package com.diet.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.diet.dto.MealPlanSuggestionDTO;
import com.diet.entity.*;
import com.diet.mapper.MealPlanMapper;
import com.diet.service.IDietTagService;
import com.diet.service.IHealthProfileService;
import com.diet.service.IMealPlanService;
import com.diet.service.IRecipeService;
import com.diet.service.IUserPreferenceService;
import com.diet.service.IFeedbackService;
import com.diet.service.MealPlanRuleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MealPlanServiceImpl extends ServiceImpl<MealPlanMapper, MealPlan> implements IMealPlanService {

    // Calorie distribution: breakfast 30%, lunch 40%, dinner 25%, snack 5%
    private static final double[] MEAL_RATIOS = { 0.30, 0.40, 0.25, 0.05 };
    private static final MealPlan.MealType[] MEAL_TYPES = {
            MealPlan.MealType.BREAKFAST, MealPlan.MealType.LUNCH,
            MealPlan.MealType.DINNER, MealPlan.MealType.SNACK
    };
    private static final int MIN_INGREDIENTS_PER_MEAL = 3;
    private static final int MAX_INGREDIENTS_PER_MEAL = 5;

    private final IRecipeService recipeService;
    private final IHealthProfileService healthProfileService;
    private final IUserPreferenceService preferenceService;
    private final IFeedbackService feedbackService;
    private final IDietTagService dietTagService;
    private final MealPlanRuleService mealPlanRuleService;
    @Value("${app.plan.audit-required:true}")
    private boolean planAuditRequired;

    public MealPlanServiceImpl(IRecipeService recipeService, IHealthProfileService healthProfileService,
            IUserPreferenceService preferenceService, @Lazy IFeedbackService feedbackService,
            IDietTagService dietTagService, MealPlanRuleService mealPlanRuleService) {
        this.recipeService = recipeService;
        this.healthProfileService = healthProfileService;
        this.preferenceService = preferenceService;
        this.feedbackService = feedbackService;
        this.dietTagService = dietTagService;
        this.mealPlanRuleService = mealPlanRuleService;
    }

    @Override
    @Transactional
    public List<MealPlan> generateDailyPlan(Long userId, LocalDate date) {
        log.info("[业务-饮食计划] 生成日计划开始, userId={}, date={}", userId, date);
        return generateDailyPlanInternal(userId, date, Set.of(), Set.of());
    }

    @Override
    public void generateWeekPlan(Long userId, LocalDate startDate) {
        long startMs = System.currentTimeMillis();
        log.info("开始生成周计划, userId={}, startDate={}", userId, startDate);
        Set<Long> prevDayRecipeIds = new HashSet<>();
        Set<String> weekIngredientPool = new HashSet<>();
        for (int i = 0; i < 7; i++) {
            LocalDate d = startDate.plusDays(i);
            long dayStart = System.currentTimeMillis();
            List<MealPlan> dayPlans = generateDailyPlanInternal(userId, d, prevDayRecipeIds, weekIngredientPool);
            prevDayRecipeIds = dayPlans.stream()
                    .map(MealPlan::getRecipeId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            for (MealPlan p : dayPlans) {
                if (p.getRecipeId() != null) {
                    Recipe r = recipeService.getById(p.getRecipeId());
                    if (r != null) {
                        weekIngredientPool.addAll(extractIngredientNames(r));
                    }
                }
            }
            log.info("周计划-第{}天生成完成, date={}, elapsedMs={}", i + 1, d, System.currentTimeMillis() - dayStart);
        }
        if (weekIngredientPool.size() < 40) {
            log.warn("周计划食材多样性不足：uniqueIngredients={} (<40), userId={}, startDate={}",
                    weekIngredientPool.size(), userId, startDate);
        }
        log.info("周计划生成完成, userId={}, startDate={}, elapsedMs={}", userId, startDate,
                System.currentTimeMillis() - startMs);
    }

    private List<MealPlan> generateDailyPlanInternal(Long userId, LocalDate date, Set<Long> avoidRecipeIds,
            Set<String> weekIngredientPool) {
        long startMs = System.currentTimeMillis();
        HealthProfile profile = healthProfileService.lambdaQuery()
                .eq(HealthProfile::getUserId, userId).one();
        if (profile == null || profile.getTdee() == null) {
            throw new IllegalArgumentException("请先完善健康档案");
        }
        double tdee = profile.getTdee().doubleValue();
        double baseCalories = profile.getTarget() == HealthProfile.Target.LOSE_WEIGHT ? tdee
                : profile.getTarget() == HealthProfile.Target.BUILD_MUSCLE ? tdee + 400 : tdee;
        double mult = preferenceService.getCalorieMultiplier(userId);
        int baseTotalCalories = (int) Math.round(baseCalories * mult);
        List<DietTag> dietTags = dietTagService.lambdaQuery()
                .eq(DietTag::getUserId, userId)
                .list();
        String workoutTime = resolveWorkoutTime(profile);
        MealPlanSuggestionDTO suggestion = mealPlanRuleService.getSuggestion(
                profile, dietTags, date, workoutTime, baseTotalCalories);
        double totalCalories = suggestion.getSuggestedCalories();
        log.info("规则建议热量：{}，模式：{}，标记：{}",
                suggestion.getSuggestedCalories(), suggestion.getPlanMode(), suggestion.getFlags());

        long removeStart = System.currentTimeMillis();
        lambdaUpdate().eq(MealPlan::getUserId, userId).eq(MealPlan::getPlanDate, date).remove();
        log.info("生成日计划-清理旧数据完成, userId={}, date={}, elapsedMs={}",
                userId, date, System.currentTimeMillis() - removeStart);

        List<MealPlan> plans = new ArrayList<>();
        List<Long> usedRecipeIds = new ArrayList<>(avoidRecipeIds);
        Set<String> usedIngredientsToday = new HashSet<>();
        Recipe.Category[] categories = { Recipe.Category.BREAKFAST, Recipe.Category.LUNCH,
                Recipe.Category.DINNER, Recipe.Category.SNACK };

        for (int i = 0; i < 4; i++) {
            double mealCal = totalCalories * MEAL_RATIOS[i];
            if (categories[i] == Recipe.Category.DINNER && suggestion.getDinnerProteinBoostRatio() > 1.0) {
                mealCal = mealCal * suggestion.getDinnerProteinBoostRatio();
            }
            double minCal = Math.max(50, mealCal * 0.95);
            double maxCal = mealCal * 1.05;
            if (suggestion.isAllowCheatMeal()) {
                maxCal = maxCal * suggestion.getCalorieUpperMultiplier();
            }

            List<Recipe> candidates = recipeService.findRecipesForPlan(
                    userId, categories[i], minCal, maxCal, usedRecipeIds);
            if (candidates.isEmpty()) {
                // Relax to ±15% if strict ±5% has no match.
                minCal = Math.max(50, mealCal * 0.85);
                maxCal = mealCal * 1.15;
                if (suggestion.isAllowCheatMeal()) {
                    maxCal = maxCal * suggestion.getCalorieUpperMultiplier();
                }
                candidates = recipeService.findRecipesForPlan(userId, categories[i], minCal, maxCal, usedRecipeIds);
            }
            if (candidates.isEmpty()) {
                // Final fallback
                minCal = Math.max(50, mealCal * 0.5);
                maxCal = mealCal * 1.5;
                if (suggestion.isAllowCheatMeal()) {
                    maxCal = maxCal * suggestion.getCalorieUpperMultiplier();
                }
                candidates = recipeService.findRecipesForPlan(userId, categories[i], minCal, maxCal, usedRecipeIds);
            }
            if (categories[i] == Recipe.Category.DINNER
                    && "POST_WORKOUT_MEAL".equals(suggestion.getDinnerTag())) {
                candidates = candidates.stream()
                        .sorted((a, b) -> Double.compare(getRecipeProtein(b), getRecipeProtein(a)))
                        .toList();
            }
            Recipe chosen = chooseRecipeCandidate(candidates, mealCal, usedIngredientsToday, weekIngredientPool);
            if (chosen == null) {
                chosen = recipeService.findAnyApprovedForMeal(userId, categories[i], usedRecipeIds);
                if (chosen != null) {
                    log.warn("[业务-饮食计划] 热量区间无匹配，已回退到同分类可用菜谱, userId={}, date={}, meal={}, recipeId={}",
                            userId, date, categories[i], chosen.getId());
                }
            }
            if (chosen != null)
                usedRecipeIds.add(chosen.getId());
            if (chosen != null) {
                usedIngredientsToday.addAll(extractIngredientNames(chosen));
            }

            MealPlan plan = new MealPlan();
            plan.setUserId(userId);
            plan.setPlanDate(date);
            plan.setMealType(MEAL_TYPES[i]);
            plan.setRecipeId(chosen != null ? chosen.getId() : null);
            plan.setSuggestedCalories(BigDecimal.valueOf(mealCal));
            plan.setStatus(MealPlan.PlanStatus.PLANNED);
            plan.setAuditStatus(planAuditRequired ? MealPlan.AuditStatus.PENDING : MealPlan.AuditStatus.AUTO);
            plans.add(plan);
        }
        long saveStart = System.currentTimeMillis();
        saveBatch(plans);
        log.info("生成日计划-保存4餐完成, userId={}, date={}, elapsedMs={}",
                userId, date, System.currentTimeMillis() - saveStart);
        log.info("生成日计划完成, userId={}, date={}, elapsedMs={}",
                userId, date, System.currentTimeMillis() - startMs);
        return plans;
    }

    @Override
    public Map<String, Object> getDailyPlanDetail(Long userId, LocalDate date) {
        List<MealPlan> plans = lambdaQuery()
                .eq(MealPlan::getUserId, userId)
                .eq(MealPlan::getPlanDate, date)
                .orderByAsc(MealPlan::getMealType)
                .list();

        List<Map<String, Object>> meals = new ArrayList<>();
        double totalTarget = 0;
        double totalConsumed = 0;
        double protein = 0, carb = 0, fat = 0;
        Set<String> ingredientPool = new HashSet<>();
        int ingredientSlots = 0;

        for (MealPlan p : plans) {
            Recipe recipe = p.getRecipeId() != null ? recipeService.getById(p.getRecipeId()) : null;
            double cal = recipe != null ? getRecipeCalories(recipe)
                    : (p.getSuggestedCalories() != null ? p.getSuggestedCalories().doubleValue() : 0);
            if (p.getSuggestedCalories() != null)
                totalTarget += p.getSuggestedCalories().doubleValue();
            if (p.getStatus() == MealPlan.PlanStatus.COMPLETED || p.getStatus() == MealPlan.PlanStatus.OUT_EAT) {
                totalConsumed += cal;
                if (recipe != null && recipe.getNutritionInfo() != null) {
                    protein += getNum(recipe.getNutritionInfo().get("protein"));
                    carb += getNum(recipe.getNutritionInfo().get("carb"));
                    fat += getNum(recipe.getNutritionInfo().get("fat"));
                }
            }
            if (recipe != null) {
                List<String> ing = extractIngredientNames(recipe);
                ingredientPool.addAll(ing);
                ingredientSlots += ing.size();
            }

            Map<String, Object> m = new HashMap<>();
            m.put("plan", p);
            m.put("recipe", recipe);
            m.put("calories", cal);
            m.put("recipeDeleted", recipe == null && p.getRecipeId() != null);
            m.put("recordedCalories", p.getSuggestedCalories() != null ? p.getSuggestedCalories().doubleValue() : null);
            m.put("matchReasons", recipe != null && recipe.getMatchTags() != null
                    ? recipe.getMatchTags().stream().map(this::tagToReason).toList()
                    : List.of());
            meals.add(m);
        }

        Map<String, Object> result = new HashMap<>();
        HealthProfile profile = healthProfileService.lambdaQuery().eq(HealthProfile::getUserId, userId).one();
        MacroTarget macroTarget = calculateMacroTarget(profile, totalTarget);
        result.put("meals", meals);
        result.put("totalTargetCalories", totalTarget);
        result.put("totalConsumedCalories", totalConsumed);
        result.put("consumedProtein", protein);
        result.put("consumedCarb", carb);
        result.put("consumedFat", fat);
        result.put("targetProtein", macroTarget.proteinGram);
        result.put("targetCarb", macroTarget.carbGram);
        result.put("targetFat", macroTarget.fatGram);
        result.put("ingredientUniqueCount", ingredientPool.size());
        result.put("ingredientTotalCount", ingredientSlots);
        result.put("ingredientDiversityRate", ingredientSlots == 0 ? 0
                : BigDecimal.valueOf((double) ingredientPool.size() / ingredientSlots)
                        .setScale(3, java.math.RoundingMode.HALF_UP).doubleValue());
        return result;
    }

    @Override
    public Map<String, Object> getNutritionSummary(Long userId, LocalDate date) {
        return getDailyPlanDetail(userId, date);
    }

    @Override
    public long daysSinceLastPlan(Long userId) {
        MealPlan last = lambdaQuery().eq(MealPlan::getUserId, userId)
                .orderByDesc(MealPlan::getPlanDate).last("LIMIT 1").one();
        if (last == null)
            return 999;
        return java.time.temporal.ChronoUnit.DAYS.between(last.getPlanDate(), LocalDate.now());
    }

    @Override
    public Map<String, Object> getProactivePrompt(Long userId) {
        Map<String, Object> result = new HashMap<>();
        result.put("show", false);
        result.put("type", null);
        result.put("message", null);
        LocalDate today = LocalDate.now();

        // Check: 3 days completed with good ratings
        int completedGood = 0;
        for (int i = 0; i < 3; i++) {
            LocalDate d = today.minusDays(i);
            List<MealPlan> plans = lambdaQuery().eq(MealPlan::getUserId, userId)
                    .eq(MealPlan::getPlanDate, d).eq(MealPlan::getStatus, MealPlan.PlanStatus.COMPLETED).list();
            if (plans.size() >= 3) {
                var feedbacks = feedbackService.lambdaQuery().eq(com.diet.entity.Feedback::getUserId, userId)
                        .ge(com.diet.entity.Feedback::getCreateTime, d.atStartOfDay())
                        .le(com.diet.entity.Feedback::getCreateTime, d.plusDays(1).atStartOfDay())
                        .list();
                boolean allGood = feedbacks.stream().allMatch(f -> f.getRating() != null && f.getRating() >= 4);
                if (allGood && !feedbacks.isEmpty())
                    completedGood++;
            }
        }
        if (completedGood >= 3) {
            result.put("show", true);
            result.put("type", "FIX_DEFAULT_MODE");
            result.put("message", "检测到您很喜欢最近的口味，是否要将此类推荐固定为默认模式？");
            return result;
        }

        // Check: skip same meal 3 days in a row
        for (MealPlan.MealType mt : MealPlan.MealType.values()) {
            int skipCount = 0;
            for (int i = 0; i < 3; i++) {
                LocalDate d = today.minusDays(i);
                List<MealPlan> slotPlans = lambdaQuery().eq(MealPlan::getUserId, userId)
                        .eq(MealPlan::getPlanDate, d).eq(MealPlan::getMealType, mt).list();
                boolean skipped = slotPlans.stream()
                        .anyMatch(p -> p.getStatus() == MealPlan.PlanStatus.SKIPPED);
                if (skipped)
                    skipCount++;
            }
            if (skipCount >= 3) {
                String meal = switch (mt) {
                    case BREAKFAST -> "早餐";
                    case LUNCH -> "午餐";
                    case DINNER -> "晚餐";
                    default -> "加餐";
                };
                result.put("show", true);
                result.put("type", "REMOVE_MEAL");
                result.put("message", "检测到您经常不吃" + meal + "，是否将" + meal + "从计划中移除？");
                result.put("mealType", mt.name());
                return result;
            }
        }
        return result;
    }

    private String tagToReason(String tag) {
        return switch (tag) {
            case "HIGH_PROTEIN" -> "高蛋白";
            case "LOW_CALORIE" -> "适合减脂";
            case "LOW_CARB" -> "低碳水";
            case "DAIRY_FREE" -> "无乳制品";
            default -> tag;
        };
    }

    private double getRecipeCalories(Recipe r) {
        if (r == null)
            return 0;
        if (r.getNutritionInfo() != null) {
            Object cal = r.getNutritionInfo().get("calories");
            if (cal instanceof Number n)
                return n.doubleValue();
        }
        return r.getCaloriesPer100g() != null ? r.getCaloriesPer100g().doubleValue() * 2 : 0;
    }

    private double getNum(Object o) {
        if (o instanceof Number n)
            return n.doubleValue();
        if (o != null)
            try {
                return Double.parseDouble(o.toString());
            } catch (Exception ignored) {
            }
        return 0;
    }

    private double getRecipeProtein(Recipe recipe) {
        if (recipe == null || recipe.getNutritionInfo() == null) {
            return 0;
        }
        return getNum(recipe.getNutritionInfo().get("protein"));
    }

    private Recipe chooseRecipeCandidate(List<Recipe> candidates, double mealCalTarget,
            Set<String> usedIngredientsToday, Set<String> weekIngredientPool) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        return candidates.stream()
                .sorted((a, b) -> {
                    double scoreA = candidateScore(a, mealCalTarget, usedIngredientsToday, weekIngredientPool);
                    double scoreB = candidateScore(b, mealCalTarget, usedIngredientsToday, weekIngredientPool);
                    return Double.compare(scoreB, scoreA);
                })
                .findFirst()
                .orElse(candidates.get(0));
    }

    /**
     * Higher score is better:
     * 1) Prefer 3-5 ingredients,
     * 2) Closer calories to target,
     * 3) Lower overlap with today's and week's ingredients.
     */
    private double candidateScore(Recipe recipe, double mealCalTarget, Set<String> usedIngredientsToday,
            Set<String> weekIngredientPool) {
        if (recipe == null) {
            return -999;
        }
        double score = 0;
        List<String> ing = extractIngredientNames(recipe);
        int count = ing.size();
        if (count >= MIN_INGREDIENTS_PER_MEAL && count <= MAX_INGREDIENTS_PER_MEAL) {
            score += 100;
        } else if (count > 0) {
            score += Math.max(0, 80 - Math.abs(4 - count) * 10);
        }
        double calDiff = Math.abs(getRecipeCalories(recipe) - mealCalTarget);
        score += Math.max(0, 100 - calDiff); // within 100 kcal still contributes

        long overlapToday = ing.stream().filter(usedIngredientsToday::contains).count();
        long overlapWeek = ing.stream().filter(weekIngredientPool::contains).count();
        score -= overlapToday * 10;
        score -= overlapWeek * 3;
        return score;
    }

    private List<String> extractIngredientNames(Recipe recipe) {
        if (recipe == null || recipe.getIngredients() == null) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        for (Map<String, Object> item : recipe.getIngredients()) {
            if (item == null)
                continue;
            Object raw = item.get("name");
            if (raw == null)
                raw = item.get("ingredientName");
            if (raw == null)
                raw = item.get("foodName");
            if (raw == null)
                raw = item.get("ingredient");
            if (raw != null) {
                String n = raw.toString().trim();
                if (!n.isEmpty()) {
                    names.add(n);
                }
            }
        }
        return names.stream().distinct().toList();
    }

    private MacroTarget calculateMacroTarget(HealthProfile profile, double totalCalories) {
        if (totalCalories <= 0) {
            return new MacroTarget(0, 0, 0);
        }
        double weight = profile != null && profile.getWeight() != null ? profile.getWeight().doubleValue() : 60.0;
        HealthProfile.Target target = profile != null ? profile.getTarget() : null;
        double proteinPerKg;
        double fatRatio;
        if (target == HealthProfile.Target.BUILD_MUSCLE) {
            proteinPerKg = 2.0;
            fatRatio = 0.25;
        } else if (target == HealthProfile.Target.LOSE_WEIGHT) {
            proteinPerKg = 1.8;
            fatRatio = 0.20;
        } else {
            proteinPerKg = 1.6;
            fatRatio = 0.25;
        }
        double proteinGram = weight * proteinPerKg;
        double fatGram = totalCalories * fatRatio / 9.0;
        double carbCalories = totalCalories - proteinGram * 4 - fatGram * 9;
        double carbGram = Math.max(0, carbCalories / 4.0);
        return new MacroTarget(round1(proteinGram), round1(carbGram), round1(fatGram));
    }

    private double round1(double v) {
        return BigDecimal.valueOf(v).setScale(1, java.math.RoundingMode.HALF_UP).doubleValue();
    }

    private record MacroTarget(double proteinGram, double carbGram, double fatGram) {
    }

    /** 优先使用用户设置的运动时段偏好；未设置或NONE时不触发时段相关规则。 */
    private String resolveWorkoutTime(HealthProfile profile) {
        if (profile == null)
            return "NONE";
        HealthProfile.ExerciseTime userTime = profile.getExerciseTime();
        if (userTime != null && userTime != HealthProfile.ExerciseTime.NONE) {
            return userTime.name();
        }
        return "NONE";
    }
}
