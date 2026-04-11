package com.diet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.diet.entity.*;
import com.diet.mapper.FeedbackMapper;
import com.diet.mapper.MealPlanMapper;
import com.diet.mapper.RecipeMapper;
import com.diet.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecipeServiceImpl extends ServiceImpl<RecipeMapper, Recipe> implements IRecipeService {

    private static final String POPULAR_RECIPE_KEY_PREFIX = "recipe:popular:";
    private static final int POPULAR_CACHE_TTL_MINUTES = 30;
    private static final int DEFAULT_POPULAR_TOP_N = 30;
    /**
     * Per-key lock to avoid cache stampede when multiple threads miss the same key.
     */
    private static final Map<String, Object> POPULAR_CACHE_LOCKS = new ConcurrentHashMap<>();

    private final IDietTagService dietTagService;
    private final IHealthProfileService healthProfileService;
    private final IUserPreferenceService preferenceService;
    private final MealPlanMapper mealPlanMapper;
    private final FeedbackMapper feedbackMapper;
    private final RedisService redisService;

    // Allergy keyword -> ingredient keywords that trigger it
    private static final Map<String, List<String>> ALLERGY_INGREDIENT_MAP = Map.of(
            "牛奶", List.of("牛奶", "乳", "奶粉", "芝士", "奶酪", "奶油"),
            "鸡蛋", List.of("鸡蛋", "蛋", "蛋黄", "蛋清"),
            "海鲜", List.of("虾", "虾仁", "鱼", "蟹", "贝", "蚝", "鱿鱼", "海鲜"),
            "花生", List.of("花生", "花生酱"),
            "麸质", List.of("小麦", "面粉", "麸质", "面包"));

    @Override
    public IPage<Recipe> searchRecipes(Long userId, String keyword, List<String> tags, Integer maxCalories,
            Recipe.Category category, String difficulty, Integer maxCookingTime,
            Page<Recipe> page, String sort) {
        String sortMode = (sort == null || sort.isBlank()) ? "match" : sort.trim().toLowerCase();
        log.info("[业务-菜谱] 搜索, keyword={}, tags={}, category={}, maxCookingTime={}, sort={}, page={}",
                keyword, tags, category, maxCookingTime, sortMode, page.getCurrent());
        QueryWrapper<Recipe> wrapper = new QueryWrapper<>();
        wrapper.eq("status", Recipe.RecipeStatus.APPROVED.name());

        if (keyword != null && !keyword.isBlank()) {
            wrapper.like("title", keyword.trim());
        }
        if (tags != null && !tags.isEmpty()) {
            wrapper.and(w -> {
                for (String tag : tags) {
                    // MySQL 5.7+/8.0 without JSON_OVERLAPS
                    w.or().apply("JSON_CONTAINS(match_tags, JSON_QUOTE({0})) = 1", tag);
                }
            });
        }
        if (maxCalories != null && maxCalories > 0) {
            wrapper.and(w -> w.apply(
                    "COALESCE((JSON_UNQUOTE(JSON_EXTRACT(nutrition_info, '$.calories')) + 0), 9999) <= {0}",
                    maxCalories));
        }
        if (category != null) {
            wrapper.eq("category", category.name());
        }
        if (difficulty != null && !difficulty.isBlank()) {
            wrapper.eq("difficulty", difficulty);
        }
        if (maxCookingTime != null && maxCookingTime > 0) {
            wrapper.isNotNull("cooking_time");
            wrapper.le("cooking_time", maxCookingTime);
        }

        IPage<Recipe> result;
        if ("match".equals(sortMode)) {
            QueryWrapper<Recipe> listWrapper = wrapper.clone();
            listWrapper.last("LIMIT 2000");
            List<Recipe> all = list(listWrapper);
            if (userId != null) {
                List<DietTag> dietTags = dietTagService.lambdaQuery()
                        .eq(DietTag::getUserId, userId)
                        .orderByDesc(DietTag::getConfidenceScore)
                        .list();
                List<String> userTags = dietTags.stream()
                        .map(DietTag::getTagName)
                        .distinct()
                        .toList();
                Map<String, Double> tagWeights = preferenceService.getTagWeights(userId);
                double oilyPenalty = tagWeights.getOrDefault("AVOID_OILY", 1.0);
                all.sort((a, b) -> Double.compare(
                        scoreForPlan(b, userTags, tagWeights, oilyPenalty),
                        scoreForPlan(a, userTags, tagWeights, oilyPenalty)));
            } else {
                all.sort(Comparator.comparing(Recipe::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder())));
            }
            result = pageFromList(all, page);
        } else if ("newest".equals(sortMode)) {
            wrapper.orderByDesc("create_time");
            result = page(page, wrapper);
        } else if ("popular".equals(sortMode)) {
            wrapper.last(
                    "ORDER BY (SELECT COUNT(*) FROM meal_plan m WHERE m.recipe_id = recipe.id) DESC, create_time DESC");
            result = page(page, wrapper);
        } else {
            wrapper.orderByDesc("create_time");
            result = page(page, wrapper);
        }

        attachPersonalMatchTags(userId, result.getRecords());
        log.info("[业务-菜谱] 搜索完成, total={}", result.getTotal());
        return result;
    }

    private IPage<Recipe> pageFromList(List<Recipe> ordered, Page<Recipe> pageParam) {
        long current = pageParam.getCurrent();
        long size = pageParam.getSize();
        long total = ordered.size();
        Page<Recipe> out = new Page<>(current, size, total);
        int from = (int) ((current - 1) * size);
        if (from < 0 || from >= ordered.size()) {
            out.setRecords(List.of());
        } else {
            int to = (int) Math.min(from + size, ordered.size());
            out.setRecords(new ArrayList<>(ordered.subList(from, to)));
        }
        return out;
    }

    private void attachPersonalMatchTags(Long userId, List<Recipe> recipes) {
        if (recipes == null || recipes.isEmpty()) {
            return;
        }
        if (userId == null) {
            recipes.forEach(r -> r.setPersonalMatchTags(List.of()));
            return;
        }
        List<DietTag> dietTags = dietTagService.lambdaQuery()
                .eq(DietTag::getUserId, userId)
                .list();
        Set<String> userTagNames = dietTags.stream()
                .map(DietTag::getTagName)
                .collect(Collectors.toSet());
        for (Recipe r : recipes) {
            if (r.getMatchTags() == null || r.getMatchTags().isEmpty()) {
                r.setPersonalMatchTags(List.of());
                continue;
            }
            List<String> inter = r.getMatchTags().stream()
                    .filter(userTagNames::contains)
                    .distinct()
                    .toList();
            r.setPersonalMatchTags(inter);
        }
    }

    @Override
    public List<Recipe> recommendForUser(Long userId, Recipe.Category category) {
        log.info("[业务-菜谱] 个性化推荐, userId={}, category={}", userId, category);
        // 1) User tags and preference weights
        List<DietTag> dietTags = dietTagService.lambdaQuery()
                .eq(DietTag::getUserId, userId)
                .orderByDesc(DietTag::getConfidenceScore)
                .list();
        List<String> userTags = dietTags.stream()
                .map(DietTag::getTagName)
                .distinct()
                .toList();
        Map<String, Double> tagWeights = preferenceService.getTagWeights(userId);
        double oilyPenalty = tagWeights.getOrDefault("AVOID_OILY", 1.0);

        // 2) User profile
        HealthProfile profile = healthProfileService.lambdaQuery()
                .eq(HealthProfile::getUserId, userId)
                .one();
        List<String> allergies = profile != null && profile.getAllergyTags() != null
                ? profile.getAllergyTags()
                : List.of();
        BigDecimal tdee = profile != null ? profile.getTdee() : BigDecimal.valueOf(2000);
        NutritionVector userVector = buildUserDemandVector(profile, category);

        // 30% of TDEE for one meal (used as soft calorie fit term)
        double maxCalories = tdee != null ? tdee.doubleValue() * 0.3 : 600;

        // 3) Candidate recipes
        List<Recipe> candidates = lambdaQuery()
                .eq(Recipe::getStatus, Recipe.RecipeStatus.APPROVED)
                .eq(Recipe::getCategory, category)
                .isNotNull(Recipe::getMatchTags)
                .list();

        // 4) Filter out allergens
        List<Recipe> filtered = candidates.stream()
                .filter(r -> !containsAllergen(r.getIngredients(), allergies))
                .toList();

        // 5) Hybrid score:
        // final = 0.55*cosine(nutrition vector) + 0.30*ruleTagScore + 0.15*calorieFit
        // this keeps the old rule-based behavior while adding vector similarity.
        double maxCal = maxCalories;
        List<RecipeScore> ranked = filtered.stream()
                .map(r -> {
                    NutritionVector recipeVector = buildRecipeVector(r);
                    double cosine = cosineSimilarity(userVector, recipeVector);
                    double ruleScoreRaw = scoreForPlan(r, userTags, tagWeights, oilyPenalty);
                    double ruleScore = normalizeRuleScore(ruleScoreRaw);
                    double cal = getRecipeCalories(r);
                    double calorieFit = calorieFit(cal, maxCal);
                    double finalScore = 0.55 * cosine + 0.30 * ruleScore + 0.15 * calorieFit;
                    return new RecipeScore(r, finalScore);
                })
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .limit(30)
                .toList();

        List<Recipe> result = ranked.stream().map(RecipeScore::recipe).toList();
        attachPersonalMatchTags(userId, result);
        log.info("[业务-菜谱] 推荐完成, userId={}, category={}, 返回{}条", userId, category, result.size());
        return result;
    }

    @Override
    public List<Recipe> findRecipesForPlan(Long userId, Recipe.Category category, double calorieMin, double calorieMax,
            List<Long> excludeRecipeIds) {
        List<DietTag> dietTags = dietTagService.lambdaQuery()
                .eq(DietTag::getUserId, userId).orderByDesc(DietTag::getConfidenceScore).list();
        List<String> userTags = dietTags.stream().map(DietTag::getTagName).distinct().toList();
        HealthProfile profile = healthProfileService.lambdaQuery().eq(HealthProfile::getUserId, userId).one();
        List<String> allergies = profile != null && profile.getAllergyTags() != null ? profile.getAllergyTags()
                : List.of();
        Set<Long> exclude = excludeRecipeIds != null ? new HashSet<>(excludeRecipeIds) : Set.of();

        List<Recipe> candidates = lambdaQuery()
                .eq(Recipe::getStatus, Recipe.RecipeStatus.APPROVED)
                .eq(Recipe::getCategory, category)
                .notIn(!exclude.isEmpty(), Recipe::getId, exclude)
                .last("LIMIT 500")
                .list();

        Map<String, Double> tagWeights = preferenceService.getTagWeights(userId);
        double oilyPenalty = tagWeights.getOrDefault("AVOID_OILY", 1.0);

        return candidates.stream()
                .filter(r -> !containsAllergen(r.getIngredients(), allergies))
                .filter(r -> {
                    double cal = getRecipeCalories(r);
                    return cal >= calorieMin && cal <= calorieMax;
                })
                .sorted((a, b) -> {
                    double scoreA = scoreForPlan(a, userTags, tagWeights, oilyPenalty);
                    double scoreB = scoreForPlan(b, userTags, tagWeights, oilyPenalty);
                    return Double.compare(scoreB, scoreA);
                })
                .limit(10)
                .toList();
    }

    @Override
    public Recipe findAnyApprovedForMeal(Long userId, Recipe.Category category, List<Long> excludeRecipeIds) {
        HealthProfile profile = healthProfileService.lambdaQuery().eq(HealthProfile::getUserId, userId).one();
        List<String> allergies = profile != null && profile.getAllergyTags() != null ? profile.getAllergyTags()
                : List.of();
        Set<Long> exclude = excludeRecipeIds != null ? new HashSet<>(excludeRecipeIds) : Set.of();
        List<DietTag> dietTags = dietTagService.lambdaQuery()
                .eq(DietTag::getUserId, userId).orderByDesc(DietTag::getConfidenceScore).list();
        List<String> userTags = dietTags.stream().map(DietTag::getTagName).distinct().toList();
        Map<String, Double> tagWeights = preferenceService.getTagWeights(userId);
        double oilyPenalty = tagWeights.getOrDefault("AVOID_OILY", 1.0);

        List<Recipe> candidates = lambdaQuery()
                .eq(Recipe::getStatus, Recipe.RecipeStatus.APPROVED)
                .eq(Recipe::getCategory, category)
                .notIn(!exclude.isEmpty(), Recipe::getId, exclude)
                .last("LIMIT 200")
                .list();
        return candidates.stream()
                .filter(r -> !containsAllergen(r.getIngredients(), allergies))
                .max(Comparator.comparingDouble(
                        r -> scoreForPlan(r, userTags, tagWeights, oilyPenalty)))
                .orElse(null);
    }

    @Override
    public Recipe findReplacement(Long userId, Recipe.Category category, Long excludeRecipeId, double targetCalories) {
        double min = Math.max(0, targetCalories - 50);
        double max = targetCalories + 50;
        List<Long> exclude = excludeRecipeId != null ? List.of(excludeRecipeId) : List.of();
        List<Recipe> list = findRecipesForPlan(userId, category, min, max, exclude);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public List<Map<String, Object>> getPopularRecipes(Recipe.Category category, int limit) {
        int safeLimit = normalizeLimit(limit);
        String cacheKey = buildPopularCacheKey(category);
        Object lock = POPULAR_CACHE_LOCKS.computeIfAbsent(cacheKey, k -> new Object());

        synchronized (lock) {
            List<Map<String, Object>> cached = redisService.get(cacheKey);
            if (cached != null) {
                return cached.stream().limit(safeLimit).toList();
            }
            List<Map<String, Object>> fresh = queryPopularRecipesFromDb(category, DEFAULT_POPULAR_TOP_N);
            redisService.set(cacheKey, fresh, POPULAR_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            return fresh.stream().limit(safeLimit).toList();
        }
    }

    @Override
    public void refreshPopularRecipeCache(Recipe.Category category, int limit) {
        int safeLimit = normalizeLimit(limit);
        String cacheKey = buildPopularCacheKey(category);
        List<Map<String, Object>> fresh = queryPopularRecipesFromDb(category, safeLimit);
        redisService.set(cacheKey, fresh, POPULAR_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
    }

    @Override
    public void refreshAllPopularRecipeCaches(int limit) {
        refreshPopularRecipeCache(null, limit);
        for (Recipe.Category c : Recipe.Category.values()) {
            refreshPopularRecipeCache(c, limit);
        }
    }

    @Override
    public void refreshPopularRecipeCacheAsync(Recipe.Category category) {
        CompletableFuture.runAsync(() -> {
            try {
                refreshPopularRecipeCache(category, DEFAULT_POPULAR_TOP_N);
            } catch (Exception e) {
                log.warn("Failed to refresh popular recipe cache for category={}", category, e);
            }
        });
    }

    @Override
    public void removeRecipeLogicalAndRefreshCache(Recipe recipe) {
        if (recipe == null || recipe.getId() == null) {
            return;
        }
        Long id = recipe.getId();
        Recipe.Category category = recipe.getCategory();
        removeById(id);
        if (category != null) {
            refreshPopularRecipeCacheAsync(category);
        }
        refreshPopularRecipeCacheAsync(null);
    }

    private List<Map<String, Object>> queryPopularRecipesFromDb(Recipe.Category category, int limit) {
        int safeLimit = normalizeLimit(limit);

        List<Recipe> recipes = lambdaQuery()
                .eq(Recipe::getStatus, Recipe.RecipeStatus.APPROVED)
                .eq(category != null, Recipe::getCategory, category)
                .list();
        if (recipes.isEmpty()) {
            return List.of();
        }

        Set<Long> recipeIds = recipes.stream().map(Recipe::getId).collect(Collectors.toSet());

        List<MealPlan> mealPlans = mealPlanMapper.selectList(
                new QueryWrapper<MealPlan>().isNotNull("recipe_id"));
        Map<Long, Long> planCountByRecipe = mealPlans.stream()
                .filter(mp -> mp.getRecipeId() != null && recipeIds.contains(mp.getRecipeId()))
                .collect(Collectors.groupingBy(MealPlan::getRecipeId, Collectors.counting()));

        Map<Long, Long> planIdToRecipeId = mealPlans.stream()
                .filter(mp -> mp.getId() != null && mp.getRecipeId() != null && recipeIds.contains(mp.getRecipeId()))
                .collect(Collectors.toMap(MealPlan::getId, MealPlan::getRecipeId, (a, b) -> a));

        List<Feedback> feedbacks = feedbackMapper.selectList(
                new QueryWrapper<Feedback>().isNotNull("plan_id").isNotNull("rating"));
        Map<Long, long[]> ratingAggByRecipe = new HashMap<>();
        for (Feedback feedback : feedbacks) {
            Long recipeId = planIdToRecipeId.get(feedback.getPlanId());
            if (recipeId == null || feedback.getRating() == null) {
                continue;
            }
            long[] agg = ratingAggByRecipe.computeIfAbsent(recipeId, k -> new long[2]);
            agg[0] += feedback.getRating(); // sum
            agg[1] += 1; // count
        }

        List<PopularRecipeStat> stats = new ArrayList<>();
        for (Recipe recipe : recipes) {
            long planCount = planCountByRecipe.getOrDefault(recipe.getId(), 0L);
            long[] agg = ratingAggByRecipe.get(recipe.getId());
            double avgRating = (agg == null || agg[1] == 0) ? 0.0 : (double) agg[0] / agg[1];
            double hotScore = planCount + avgRating * 2;
            stats.add(new PopularRecipeStat(recipe, planCount, avgRating, hotScore));
        }

        return stats.stream()
                .sorted((a, b) -> {
                    int scoreCmp = Double.compare(b.hotScore(), a.hotScore());
                    if (scoreCmp != 0)
                        return scoreCmp;
                    int planCmp = Long.compare(b.planCount(), a.planCount());
                    if (planCmp != 0)
                        return planCmp;
                    return Double.compare(b.avgRating(), a.avgRating());
                })
                .limit(safeLimit)
                .map(this::toPopularMap)
                .toList();
    }

    private Map<String, Object> toPopularMap(PopularRecipeStat stat) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("recipeId", stat.recipe().getId());
        item.put("title", stat.recipe().getTitle());
        item.put("category", stat.recipe().getCategory() != null ? stat.recipe().getCategory().name() : null);
        item.put("coverImage", stat.recipe().getCoverImage());
        item.put("planCount", stat.planCount());
        item.put("avgRating", BigDecimal.valueOf(stat.avgRating()).setScale(2, java.math.RoundingMode.HALF_UP));
        item.put("hotScore", BigDecimal.valueOf(stat.hotScore()).setScale(2, java.math.RoundingMode.HALF_UP));
        return item;
    }

    private String buildPopularCacheKey(Recipe.Category category) {
        return category == null
                ? POPULAR_RECIPE_KEY_PREFIX + "all"
                : POPULAR_RECIPE_KEY_PREFIX + category.name().toLowerCase();
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_POPULAR_TOP_N;
        }
        return Math.min(limit, DEFAULT_POPULAR_TOP_N);
    }

    private record PopularRecipeStat(Recipe recipe, long planCount, double avgRating, double hotScore) {
    }

    private boolean containsAllergen(List<Map<String, Object>> ingredients, List<String> allergies) {
        if (ingredients == null || allergies == null || allergies.isEmpty())
            return false;
        Set<String> allergenKeywords = new HashSet<>();
        for (String allergy : allergies) {
            List<String> keywords = ALLERGY_INGREDIENT_MAP.getOrDefault(allergy, List.of(allergy));
            allergenKeywords.addAll(keywords);
        }
        for (Map<String, Object> ing : ingredients) {
            Object nameObj = ing.get("name");
            if (nameObj == null)
                continue;
            String name = nameObj.toString().toLowerCase();
            for (String kw : allergenKeywords) {
                if (name.contains(kw.toLowerCase()))
                    return true;
            }
        }
        return false;
    }

    private double scoreForPlan(Recipe r, List<String> userTags, Map<String, Double> tagWeights, double oilyPenalty) {
        if (r.getMatchTags() == null)
            return 0;
        double score = 0;
        for (String tag : r.getMatchTags()) {
            double w = tagWeights.getOrDefault(tag, 1.0);
            if (userTags.contains(tag))
                score += w;
        }
        if (oilyPenalty < 1 && isOilyRecipe(r))
            score *= oilyPenalty;
        return score;
    }

    private boolean isOilyRecipe(Recipe r) {
        if (r.getNutritionInfo() != null) {
            Object fat = r.getNutritionInfo().get("fat");
            Object cal = r.getNutritionInfo().get("calories");
            if (fat instanceof Number fn && cal instanceof Number cn && cn.doubleValue() > 0) {
                return fn.doubleValue() / cn.doubleValue() > 0.4 || fn.doubleValue() > 25;
            }
        }
        return false;
    }

    private NutritionVector buildUserDemandVector(HealthProfile profile, Recipe.Category category) {
        double tdee = profile != null && profile.getTdee() != null ? profile.getTdee().doubleValue() : 2000;
        double mealRatio = switch (category) {
            case BREAKFAST -> 0.30;
            case LUNCH -> 0.40;
            case DINNER -> 0.25;
            default -> 0.05;
        };
        double calories = tdee * mealRatio;
        double weight = profile != null && profile.getWeight() != null ? profile.getWeight().doubleValue() : 60.0;
        HealthProfile.Target target = profile != null ? profile.getTarget() : null;
        double proteinPerKg = target == HealthProfile.Target.BUILD_MUSCLE ? 2.0
                : target == HealthProfile.Target.LOSE_WEIGHT ? 1.8 : 1.6;
        double fatRatio = target == HealthProfile.Target.LOSE_WEIGHT ? 0.20 : 0.25;
        // Convert daily macro target to one-meal target by meal ratio.
        double protein = weight * proteinPerKg * mealRatio;
        double fat = (tdee * fatRatio / 9.0) * mealRatio;
        double carb = Math.max(0, (calories - protein * 4 - fat * 9) / 4.0);
        // Optional micronutrients, heuristically distributed by meal ratio.
        double fiber = 25 * mealRatio;
        double calcium = 800 * mealRatio;
        double iron = 12 * mealRatio;
        return new NutritionVector(calories, protein, carb, fat, fiber, calcium, iron);
    }

    private NutritionVector buildRecipeVector(Recipe recipe) {
        if (recipe == null || recipe.getNutritionInfo() == null) {
            return NutritionVector.zero();
        }
        Map<String, Object> n = recipe.getNutritionInfo();
        double calories = readNumber(n, "calories", "kcal", "energy");
        double protein = readNumber(n, "protein", "proteins");
        double carb = readNumber(n, "carb", "carbs", "carbohydrate");
        double fat = readNumber(n, "fat", "lipid");
        double fiber = readNumber(n, "fiber", "dietaryFiber");
        double calcium = readNumber(n, "calcium", "ca");
        double iron = readNumber(n, "iron", "fe");
        return new NutritionVector(calories, protein, carb, fat, fiber, calcium, iron);
    }

    private double readNumber(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object v = map.get(key);
            if (v instanceof Number n)
                return n.doubleValue();
            if (v != null) {
                try {
                    return Double.parseDouble(v.toString());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return 0;
    }

    private double cosineSimilarity(NutritionVector a, NutritionVector b) {
        double dot = a.calories() * b.calories()
                + a.protein() * b.protein()
                + a.carb() * b.carb()
                + a.fat() * b.fat()
                + a.fiber() * b.fiber()
                + a.calcium() * b.calcium()
                + a.iron() * b.iron();
        double normA = Math.sqrt(
                a.calories() * a.calories()
                        + a.protein() * a.protein()
                        + a.carb() * a.carb()
                        + a.fat() * a.fat()
                        + a.fiber() * a.fiber()
                        + a.calcium() * a.calcium()
                        + a.iron() * a.iron());
        double normB = Math.sqrt(
                b.calories() * b.calories()
                        + b.protein() * b.protein()
                        + b.carb() * b.carb()
                        + b.fat() * b.fat()
                        + b.fiber() * b.fiber()
                        + b.calcium() * b.calcium()
                        + b.iron() * b.iron());
        if (normA == 0 || normB == 0)
            return 0;
        return dot / (normA * normB);
    }

    private double normalizeRuleScore(double raw) {
        // Raw score usually ranges 0~5; compress into [0,1).
        return raw <= 0 ? 0 : raw / (raw + 2.0);
    }

    private double calorieFit(double recipeCalories, double targetCalories) {
        if (targetCalories <= 0 || recipeCalories <= 0)
            return 0;
        double diffRatio = Math.abs(recipeCalories - targetCalories) / targetCalories;
        return Math.max(0, 1.0 - diffRatio); // diff 0 ->1, diff >=100% ->0
    }

    private double getRecipeCalories(Recipe r) {
        if (r.getNutritionInfo() != null) {
            Object cal = r.getNutritionInfo().get("calories");
            if (cal instanceof Number n)
                return n.doubleValue();
            if (cal != null) {
                try {
                    return Double.parseDouble(cal.toString());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return r.getCaloriesPer100g() != null ? r.getCaloriesPer100g().doubleValue() * 2 : 0; // Assume 200g serving
    }

    private record NutritionVector(double calories, double protein, double carb, double fat,
            double fiber, double calcium, double iron) {
        static NutritionVector zero() {
            return new NutritionVector(0, 0, 0, 0, 0, 0, 0);
        }
    }

    private record RecipeScore(Recipe recipe, double score) {
    }
}
