package com.diet.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.diet.entity.*;
import com.diet.mapper.FeedbackMapper;
import com.diet.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl extends ServiceImpl<FeedbackMapper, Feedback> implements IFeedbackService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FeedbackServiceImpl.class);
    private static final Pattern ALLERGY_PATTERN = Pattern.compile("对([^过敏]+)过敏|([^，。]+)过敏");

    private final IUserPreferenceService preferenceService;
    private final IHealthProfileService healthProfileService;
    private final IMealPlanService mealPlanService;
    private final IRecipeService recipeService;
    private final ISystemNotificationService notificationService;

    @Override
    @Transactional
    public Feedback submitFeedback(Long userId, Long planId, int tasteRating, Integer satietyRating,
                                   Integer difficultyRating, List<String> tags, String comment) {
        log.info("[业务-反馈] 提交反馈, userId={}, planId={}, taste={}, satiety={}, difficulty={}", userId, planId, tasteRating, satietyRating, difficultyRating);
        MealPlan plan = mealPlanService.getById(planId);
        if (plan == null || !plan.getUserId().equals(userId)) {
            log.warn("[业务-反馈] 计划不存在或无权操作, userId={}, planId={}", userId, planId);
            throw new AccessDeniedException("计划不存在或无权对该计划提交反馈");
        }
        int satiety = normalizeScore(satietyRating != null ? satietyRating : tasteRating);
        int difficulty = normalizeScore(difficultyRating != null ? difficultyRating : tasteRating);
        int taste = normalizeScore(tasteRating);
        List<String> mergedTags = mergeTagsByComment(tags, comment);
        log.info("[业务-反馈] 归一化评分 taste={} satiety={} difficulty={}, 合并标签={}", taste, satiety, difficulty, mergedTags);
        Feedback f = new Feedback();
        f.setUserId(userId);
        f.setPlanId(planId);
        f.setRating(taste);
        f.setSatietyRating(satiety);
        f.setDifficultyRating(difficulty);
        f.setSatisfactionScore(calculateSatisfactionScore(taste, satiety, difficulty));
        f.setFeedbackTags(mergedTags);
        f.setComment(comment);
        String action = processFeedbackAndAdjust(userId, planId, taste, f.getFeedbackTags(), comment);
        f.setSystemAction(action);
        save(f);
        log.info("[业务-反馈] 反馈已保存, feedbackId={}, systemAction={}", f.getId(), action);
        if (plan.getRecipeId() != null) {
            Recipe recipe = recipeService.getById(plan.getRecipeId());
            if (recipe != null) {
                recipeService.refreshPopularRecipeCacheAsync(recipe.getCategory());
            }
        }
        recipeService.refreshPopularRecipeCacheAsync(null);
        return f;
    }

    private int normalizeScore(Integer value) {
        if (value == null) return 5;
        if (value < 1) return 1;
        return Math.min(value, 5);
    }

    private BigDecimal calculateSatisfactionScore(int taste, int satiety, int difficulty) {
        double score = taste * 0.3 + satiety * 0.3 + difficulty * 0.4;
        return BigDecimal.valueOf(score).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Lightweight NLP: map comment keywords to feedback tags.
     */
    private List<String> mergeTagsByComment(List<String> tags, String comment) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        if (tags != null) {
            merged.addAll(tags.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty()).toList());
        }
        if (comment != null) {
            String c = comment.trim();
            if (c.contains("偏咸") || c.contains("太咸")) merged.add("TOO_SALTY");
            if (c.contains("偏甜") || c.contains("太甜")) merged.add("TOO_SWEET");
            if (c.contains("偏油") || c.contains("太油") || c.contains("油腻")) merged.add("TOO_OILY");
            if (c.contains("量少") || c.contains("吃不饱")) merged.add("TOO_LITTLE");
            if (c.contains("量多") || c.contains("吃太撑") || c.contains("太多")) merged.add("TOO_MUCH");
        }
        return merged.stream().toList();
    }

    private String processFeedbackAndAdjust(Long userId, Long planId, int rating, List<String> tags, String comment) {
        List<String> actions = new ArrayList<>();
        UserPreference pref = preferenceService.getOrCreate(userId);
        HealthProfile profile = healthProfileService.lambdaQuery().eq(HealthProfile::getUserId, userId).one();

        // 1. Parse allergy from comment - "对虾过敏" -> add to allergy_tags
        if (comment != null && !comment.isBlank()) {
            String allergy = parseAllergyFromComment(comment);
            if (allergy != null && profile != null && profile.getAllergyTags() != null
                    && !profile.getAllergyTags().contains(allergy)) {
                List<String> newAllergies = new ArrayList<>(profile.getAllergyTags());
                newAllergies.add(allergy);
                profile.setAllergyTags(newAllergies);
                healthProfileService.updateById(profile);
                actions.add("已为您添加" + allergy + "过敏记录");
                notificationService.notify(userId, "已根据您的反馈添加「" + allergy + "」至过敏原列表");
            }
        }

        // 2. TOO_MUCH twice in a row -> reduce calorie 10%（下限 0.8）
        if (tags != null && tags.contains("TOO_MUCH")) {
            List<Feedback> recent = lambdaQuery().eq(Feedback::getUserId, userId)
                    .orderByDesc(Feedback::getCreateTime).last("LIMIT 3").list();
            boolean prevHadTooMuch = !recent.isEmpty() && recent.get(0).getFeedbackTags() != null
                    && recent.get(0).getFeedbackTags().contains("TOO_MUCH");
            if (prevHadTooMuch) {  // Consecutive: previous + this = 2
                double mult = pref.getCalorieMultiplier() != null ? pref.getCalorieMultiplier().doubleValue() : 1.0;
                double newMult = Math.max(0.8, mult * 0.9);  // Min 0.8 (20% reduction)
                pref.setCalorieMultiplier(BigDecimal.valueOf(newMult));
                pref.setAdjustmentCount((pref.getAdjustmentCount() != null ? pref.getAdjustmentCount() : 0) + 1);
                pref.setLearningProgress(Math.min(100, (pref.getLearningProgress() != null ? pref.getLearningProgress() : 0) + 10));
                preferenceService.updateById(pref);
                actions.add("总热量减少10%");
                notificationService.notify(userId, "基于您的反馈，已为您调整：总热量减少10%");
            }
        }

        // 2b. TOO_LITTLE 连续两次 -> 增加热量系数 10%（上限 1.2，形成闭环可恢复）
        if (tags != null && tags.contains("TOO_LITTLE")) {
            List<Feedback> recent = lambdaQuery().eq(Feedback::getUserId, userId)
                    .orderByDesc(Feedback::getCreateTime).last("LIMIT 3").list();
            boolean prevHadTooLittle = !recent.isEmpty() && recent.get(0).getFeedbackTags() != null
                    && recent.get(0).getFeedbackTags().contains("TOO_LITTLE");
            if (prevHadTooLittle) {
                double mult = pref.getCalorieMultiplier() != null ? pref.getCalorieMultiplier().doubleValue() : 1.0;
                double newMult = Math.min(1.2, mult * 1.1);  // Cap 1.2 (20% increase)
                pref.setCalorieMultiplier(BigDecimal.valueOf(newMult));
                pref.setAdjustmentCount((pref.getAdjustmentCount() != null ? pref.getAdjustmentCount() : 0) + 1);
                pref.setLearningProgress(Math.min(100, (pref.getLearningProgress() != null ? pref.getLearningProgress() : 0) + 10));
                preferenceService.updateById(pref);
                actions.add("总热量增加10%");
                notificationService.notify(userId, "基于您的反馈，已为您调整：总热量增加10%");
            }
        }

        // 3. Low score (1-2) to HIGH_PROTEIN dish 3 times -> reduce HIGH_PROTEIN weight
        if (rating <= 2) {
            MealPlan plan = mealPlanService.getById(planId);
            if (plan != null && plan.getRecipeId() != null) {
                Recipe recipe = recipeService.getById(plan.getRecipeId());
                if (recipe != null && recipe.getMatchTags() != null
                        && recipe.getMatchTags().contains("HIGH_PROTEIN")) {
                    List<Feedback> lowScoreProtein = lambdaQuery()
                            .eq(Feedback::getUserId, userId)
                            .le(Feedback::getRating, 2)
                            .ge(Feedback::getCreateTime, LocalDateTime.now().minusDays(14))
                            .list();
                    List<Long> planIds = lowScoreProtein.stream().map(Feedback::getPlanId).toList();
                    long proteinLowCount = 0;
                    for (Long pid : planIds) {
                        MealPlan p = mealPlanService.getById(pid);
                        if (p != null && p.getRecipeId() != null) {
                            Recipe r = recipeService.getById(p.getRecipeId());
                            if (r != null && r.getMatchTags() != null && r.getMatchTags().contains("HIGH_PROTEIN"))
                                proteinLowCount++;
                        }
                    }
                    if (proteinLowCount >= 3) {
                        Map<String, Double> weights = pref.getTagWeights() != null
                                ? new HashMap<>(pref.getTagWeights()) : new HashMap<>();
                        weights.put("HIGH_PROTEIN", 0.3);
                        pref.setTagWeights(weights);
                        pref.setAdjustmentCount((pref.getAdjustmentCount() != null ? pref.getAdjustmentCount() : 0) + 1);
                        pref.setLearningProgress(Math.min(100, (pref.getLearningProgress() != null ? pref.getLearningProgress() : 0) + 10));
                        preferenceService.updateById(pref);
                        actions.add("减少高蛋白菜品推荐");
                        notificationService.notify(userId, "已根据您的反馈：减少高蛋白类菜品推荐");
                    }
                }
            }
        }

        // 4. TOO_OILY -> add avoid oily preference
        if (tags != null && tags.contains("TOO_OILY")) {
            Map<String, Double> weights = pref.getTagWeights() != null
                    ? new HashMap<>(pref.getTagWeights()) : new HashMap<>();
            weights.put("AVOID_OILY", 0.2);  // Penalize oily recipes
            pref.setTagWeights(weights);
            pref.setAdjustmentCount((pref.getAdjustmentCount() != null ? pref.getAdjustmentCount() : 0) + 1);
            pref.setLearningProgress(Math.min(100, (pref.getLearningProgress() != null ? pref.getLearningProgress() : 0) + 10));
            preferenceService.updateById(pref);
            actions.add("减少油腻菜品推荐");
            notificationService.notify(userId, "已根据您的反馈：减少油腻菜品推荐");
        }

        return actions.isEmpty() ? null : String.join("；", actions);
    }


    private String parseAllergyFromComment(String comment) {
        Matcher m = ALLERGY_PATTERN.matcher(comment);
        if (m.find()) {
            String g1 = m.group(1);
            if (g1 != null && !g1.isBlank()) return g1.trim();
            String g2 = m.group(2);
            if (g2 != null && !g2.isBlank()) return g2.trim();
        }
        if (comment.contains("花生过敏")) return "花生";
        if (comment.contains("虾过敏") || comment.contains("对虾过敏")) return "海鲜";
        if (comment.contains("牛奶过敏")) return "牛奶";
        return null;
    }

    @Override
    public IPage<Map<String, Object>> getHistory(Long userId, int page, int size) {
        IPage<Feedback> p = lambdaQuery()
                .eq(Feedback::getUserId, userId)
                .orderByDesc(Feedback::getCreateTime)
                .page(new Page<>(page, size));
        List<Map<String, Object>> items = p.getRecords().stream().map(f -> {
            Map<String, Object> m = new HashMap<>();
            m.put("feedback", f);
            MealPlan plan = mealPlanService.getById(f.getPlanId());
            String dishName = "";
            if (plan != null && plan.getRecipeId() != null) {
                Recipe r = recipeService.getById(plan.getRecipeId());
                if (r != null) dishName = r.getTitle();
            }
            m.put("dishName", dishName);
            m.put("planDate", plan != null ? plan.getPlanDate() : null);
            return m;
        }).collect(Collectors.toList());
        Page<Map<String, Object>> result = new Page<>(p.getCurrent(), p.getSize(), p.getTotal());
        result.setRecords(items);
        return result;
    }

    @Override
    public boolean shouldOfferReplace(Long userId, Long planId) {
        List<Feedback> list = lambdaQuery()
                .eq(Feedback::getPlanId, planId)
                .orderByDesc(Feedback::getId)
                .last("LIMIT 1")
                .list();
        Feedback f = list.isEmpty() ? null : list.get(0);
        return f != null && f.getRating() != null && f.getRating() <= 2;
    }

    @Override
    public Map<String, Object> getExecutionFeedbackReport(Long userId) {
        Map<String, Object> out = new LinkedHashMap<>();
        LocalDate since = LocalDate.now().minusDays(60);
        List<MealPlan> plans = mealPlanService.lambdaQuery()
                .eq(MealPlan::getUserId, userId)
                .ge(MealPlan::getPlanDate, since)
                .list();
        Set<LocalDate> activeDays = plans.stream()
                .filter(p -> p.getStatus() != null && p.getStatus() != MealPlan.PlanStatus.PLANNED)
                .map(MealPlan::getPlanDate)
                .collect(Collectors.toSet());
        int tracked = activeDays.size();
        out.put("eligible", tracked >= 3);
        out.put("executionDaysTracked", tracked);
        out.put("daysRequired", 3);
        if (tracked < 3) {
            out.put("message", "需在不同日期至少记录 3 天饮食执行情况（完成/跳过/外食等）后生成简报");
            out.put("daysNeeded", 3 - tracked);
            return out;
        }

        LocalDate twoWeeksAgo = LocalDate.now().minusDays(14);
        List<MealPlan> recentPlans = mealPlanService.lambdaQuery()
                .eq(MealPlan::getUserId, userId)
                .ge(MealPlan::getPlanDate, twoWeeksAgo)
                .list();
        long totalItems = recentPlans.size();
        long completed = recentPlans.stream()
                .filter(p -> p.getStatus() == MealPlan.PlanStatus.COMPLETED)
                .count();
        double completionRate = totalItems == 0 ? 0 : (completed * 100.0 / totalItems);
        out.put("completionRateLast14DaysPercent", Math.round(completionRate * 10) / 10.0);

        List<Feedback> recentFb = lambdaQuery()
                .eq(Feedback::getUserId, userId)
                .ge(Feedback::getCreateTime, LocalDateTime.now().minusDays(14))
                .list();
        OptionalDouble avg = recentFb.stream()
                .filter(f -> f.getSatisfactionScore() != null)
                .mapToDouble(f -> f.getSatisfactionScore().doubleValue())
                .average();
        out.put("avgSatisfactionLast14Days", avg.isPresent()
                ? BigDecimal.valueOf(avg.getAsDouble()).setScale(2, java.math.RoundingMode.HALF_UP)
                : null);
        out.put("feedbackCountLast14Days", recentFb.size());
        out.put("message", "简报已生成（基于近 14 天计划与反馈数据）");
        return out;
    }
}
