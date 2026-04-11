package com.diet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.diet.entity.HealthProfile;
import com.diet.entity.MealPlan;
import com.diet.entity.Recipe;
import com.diet.entity.User;
import com.diet.mapper.HealthProfileMapper;
import com.diet.mapper.MealPlanMapper;
import com.diet.mapper.RecipeMapper;
import com.diet.mapper.UserMapper;
import com.diet.service.AdminPlanService;
import com.diet.service.ISystemNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AdminPlanServiceImpl implements AdminPlanService {

    private final MealPlanMapper mealPlanMapper;
    private final UserMapper userMapper;
    private final RecipeMapper recipeMapper;
    private final HealthProfileMapper healthProfileMapper;
    private final ISystemNotificationService notificationService;
    @Value("${app.plan.audit-required:true}")
    private boolean planAuditRequired;

    @Override
    public Map<String, Object> listPlans(long page, long size, String auditStatus, Long userId, LocalDate planDate) {
        List<MealPlan> raw = mealPlanMapper.selectList(new QueryWrapper<MealPlan>()
                .eq(userId != null, "user_id", userId)
                .eq(planDate != null, "plan_date", planDate)
                .eq(auditStatus != null && !auditStatus.isBlank(), "audit_status", auditStatus)
                .orderByDesc("plan_date", "user_id", "id"));

        Map<String, PlanGroup> grouped = new LinkedHashMap<>();
        for (MealPlan plan : raw) {
            String key = plan.getUserId() + "#" + plan.getPlanDate();
            PlanGroup group = grouped.computeIfAbsent(key, k -> new PlanGroup(plan.getUserId(), plan.getPlanDate()));
            group.items.add(plan);
        }

        List<Map<String, Object>> records = grouped.values().stream()
                .map(this::toPlanListItem)
                .toList();

        long total = records.size();
        long safePage = Math.max(1, page);
        long safeSize = Math.max(1, size);
        int from = (int) Math.min((safePage - 1) * safeSize, total);
        int to = (int) Math.min(from + safeSize, total);
        List<Map<String, Object>> paged = records.subList(from, to);

        return Map.of(
                "records", paged,
                "total", total,
                "pages", (long) Math.ceil(total * 1.0 / safeSize),
                "current", safePage
        );
    }

    @Override
    public Map<String, Object> getPlanDetail(Long planId) {
        MealPlan base = mealPlanMapper.selectById(planId);
        if (base == null) return null;

        List<MealPlan> dayPlans = mealPlanMapper.selectList(new QueryWrapper<MealPlan>()
                .eq("user_id", base.getUserId())
                .eq("plan_date", base.getPlanDate())
                .orderByAsc("meal_type"));

        User user = userMapper.selectById(base.getUserId());
        HealthProfile profile = healthProfileMapper.selectOne(new QueryWrapper<HealthProfile>()
                .eq("user_id", base.getUserId()).last("LIMIT 1"));

        List<Map<String, Object>> meals = new ArrayList<>();
        double totalCalories = 0;
        for (MealPlan p : dayPlans) {
            Recipe recipe = p.getRecipeId() != null ? recipeMapper.selectById(p.getRecipeId()) : null;
            if (p.getSuggestedCalories() != null) {
                totalCalories += p.getSuggestedCalories().doubleValue();
            }
            meals.add(Map.of(
                    "plan", p,
                    "recipe", recipe
            ));
        }
        Map<String, Object> precheck = buildPrecheck(profile, dayPlans, totalCalories);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("user", user);
        detail.put("healthProfile", profile != null ? profile : Map.of());
        detail.put("planDate", base.getPlanDate());
        detail.put("auditStatus", base.getAuditStatus());
        detail.put("auditComment", base.getAuditComment() != null ? base.getAuditComment() : "");
        detail.put("auditedAt", base.getAuditedAt());
        detail.put("auditedBy", base.getAuditedBy());
        detail.put("meals", meals);
        detail.put("totalSuggestedCalories", BigDecimal.valueOf(totalCalories));
        detail.put("precheckPassed", precheck.get("passed"));
        detail.put("precheckScore", precheck.get("score"));
        detail.put("precheckIssues", precheck.get("issues"));
        return detail;
    }

    @Override
    @Transactional
    public void approvePlan(Long planId, Long adminId) {
        MealPlan base = requirePlan(planId);
        if (base.getAuditStatus() != MealPlan.AuditStatus.PENDING) {
            throw new IllegalStateException("当前计划状态不允许审核通过");
        }
        updateDayPlansAudit(base.getUserId(), base.getPlanDate(), MealPlan.AuditStatus.APPROVED, null, adminId);
        notificationService.notify(base.getUserId(),
                "您" + base.getPlanDate() + "的饮食计划已审核通过");
    }

    @Override
    @Transactional
    public void rejectPlan(Long planId, Long adminId, String reason) {
        MealPlan base = requirePlan(planId);
        if (base.getAuditStatus() != MealPlan.AuditStatus.PENDING) {
            throw new IllegalStateException("当前计划状态不允许驳回");
        }
        String msg = reason != null && !reason.isBlank() ? reason.trim() : "请调整后重新生成";
        updateDayPlansAudit(base.getUserId(), base.getPlanDate(), MealPlan.AuditStatus.REJECTED, msg, adminId);
        notificationService.notify(base.getUserId(),
                "您" + base.getPlanDate() + "的饮食计划被驳回：" + msg);
    }

    private MealPlan requirePlan(Long planId) {
        MealPlan plan = mealPlanMapper.selectById(planId);
        if (plan == null) {
            throw new IllegalArgumentException("计划不存在");
        }
        return plan;
    }

    private void updateDayPlansAudit(Long userId, LocalDate planDate, MealPlan.AuditStatus status, String comment, Long adminId) {
        List<MealPlan> dayPlans = mealPlanMapper.selectList(new QueryWrapper<MealPlan>()
                .eq("user_id", userId)
                .eq("plan_date", planDate));
        for (MealPlan p : dayPlans) {
            p.setAuditStatus(status);
            p.setAuditComment(comment);
            p.setAuditedBy(adminId);
            p.setAuditedAt(LocalDateTime.now());
            if (status == MealPlan.AuditStatus.REJECTED && planAuditRequired) {
                p.setStatus(MealPlan.PlanStatus.SKIPPED);
            }
            mealPlanMapper.updateById(p);
        }
    }

    private Map<String, Object> toPlanListItem(PlanGroup g) {
        MealPlan first = g.items.stream()
                .sorted(Comparator.comparing(MealPlan::getId))
                .findFirst()
                .orElse(null);
        if (first == null) return Map.of();

        User user = userMapper.selectById(g.userId);
        HealthProfile hp = healthProfileMapper.selectOne(new QueryWrapper<HealthProfile>()
                .eq("user_id", g.userId).last("LIMIT 1"));

        double total = g.items.stream()
                .map(MealPlan::getSuggestedCalories)
                .filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue)
                .sum();

        Map<String, Object> item = new HashMap<>();
        item.put("id", first.getId());
        item.put("userId", g.userId);
        item.put("planDate", g.planDate);
        item.put("auditStatus", first.getAuditStatus());
        item.put("auditComment", first.getAuditComment());
        item.put("mealCount", g.items.size());
        item.put("totalSuggestedCalories", BigDecimal.valueOf(total));
        item.put("nickname", user != null ? user.getNickname() : "-");
        item.put("avatar", user != null ? user.getAvatar() : null);
        item.put("target", hp != null && hp.getTarget() != null ? hp.getTarget().name() : null);
        Map<String, Object> precheck = buildPrecheck(hp, g.items, total);
        item.put("precheckPassed", precheck.get("passed"));
        item.put("precheckScore", precheck.get("score"));
        item.put("precheckIssues", precheck.get("issues"));
        return item;
    }

    /**
     * Plan precheck before manual audit:
     * - Heat target reasonable vs TDEE and target type.
     * - Basic macro balance by summing recipe nutrition.
     * - Meal completeness (at least 3 meals with recipe bound).
     */
    private Map<String, Object> buildPrecheck(HealthProfile profile, List<MealPlan> dayPlans, double totalSuggestedCalories) {
        List<String> issues = new ArrayList<>();
        double score = 100;
        if (dayPlans == null || dayPlans.isEmpty()) {
            issues.add("当日计划为空");
            return Map.of("passed", false, "score", 0, "issues", issues);
        }

        long mealWithRecipe = dayPlans.stream().filter(p -> p.getRecipeId() != null).count();
        if (mealWithRecipe < 3) {
            issues.add("已绑定菜谱餐次不足3餐");
            score -= 25;
        }

        if (profile != null && profile.getTdee() != null && totalSuggestedCalories > 0) {
            double tdee = profile.getTdee().doubleValue();
            double expectedMin = tdee;
            double expectedMax = tdee;
            if (profile.getTarget() == HealthProfile.Target.LOSE_WEIGHT) {
                expectedMin = tdee * 0.70;
                expectedMax = tdee * 0.95;
            } else if (profile.getTarget() == HealthProfile.Target.BUILD_MUSCLE) {
                expectedMin = tdee * 1.05;
                expectedMax = tdee * 1.30;
            } else {
                expectedMin = tdee * 0.90;
                expectedMax = tdee * 1.10;
            }
            if (totalSuggestedCalories < expectedMin || totalSuggestedCalories > expectedMax) {
                issues.add("总热量与目标区间偏差较大");
                score -= 30;
            }
        } else {
            issues.add("缺少TDEE或健康档案信息，无法完成热量预校验");
            score -= 20;
        }

        double p = 0, c = 0, f = 0;
        for (MealPlan mp : dayPlans) {
            if (mp.getRecipeId() == null) continue;
            Recipe r = recipeMapper.selectById(mp.getRecipeId());
            if (r == null || r.getNutritionInfo() == null) continue;
            p += getNum(r.getNutritionInfo().get("protein"));
            c += getNum(r.getNutritionInfo().get("carb"));
            f += getNum(r.getNutritionInfo().get("fat"));
        }
        double kcal = p * 4 + c * 4 + f * 9;
        if (kcal > 0) {
            double pRatio = p * 4 / kcal;
            double cRatio = c * 4 / kcal;
            double fRatio = f * 9 / kcal;
            if (pRatio < 0.12 || pRatio > 0.40) {
                issues.add("蛋白质占比异常");
                score -= 10;
            }
            if (cRatio < 0.35 || cRatio > 0.65) {
                issues.add("碳水占比异常");
                score -= 10;
            }
            if (fRatio < 0.15 || fRatio > 0.40) {
                issues.add("脂肪占比异常");
                score -= 10;
            }
        } else {
            issues.add("缺少可用营养数据，无法完成宏量预校验");
            score -= 15;
        }

        score = Math.max(0, score);
        boolean passed = score >= 70 && issues.size() <= 2;
        return Map.of(
                "passed", passed,
                "score", BigDecimal.valueOf(score).setScale(1, java.math.RoundingMode.HALF_UP),
                "issues", issues
        );
    }

    private double getNum(Object v) {
        if (v instanceof Number n) return n.doubleValue();
        if (v != null) {
            try {
                return Double.parseDouble(v.toString());
            } catch (Exception ignored) {
                return 0;
            }
        }
        return 0;
    }

    private static class PlanGroup {
        private final Long userId;
        private final LocalDate planDate;
        private final List<MealPlan> items = new ArrayList<>();

        private PlanGroup(Long userId, LocalDate planDate) {
            this.userId = userId;
            this.planDate = planDate;
        }
    }
}
