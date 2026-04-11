package com.diet.controller;

import com.diet.common.Result;
import com.diet.entity.MealPlan;
import com.diet.entity.Recipe;
import com.diet.service.IMealPlanService;
import com.diet.service.IRecipeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/plan")
@RequiredArgsConstructor
public class PlanController {

    private static final Logger log = LoggerFactory.getLogger(PlanController.class);

    private final IMealPlanService mealPlanService;
    private final IRecipeService recipeService;

    @GetMapping("/daily")
    public Result<Map<String, Object>> getDailyPlan(
            @AuthenticationPrincipal Long userId,
            @RequestParam String date) {
        log.info("[业务-饮食计划] 查询日计划, userId={}, date={}", userId, date);
        if (userId == null) return Result.error(401, "Unauthorized");
        LocalDate d = LocalDate.parse(date);
        return Result.success(mealPlanService.getDailyPlanDetail(userId, d));
    }

    @GetMapping("/reminder")
    public Result<Map<String, Object>> getReminder(@AuthenticationPrincipal Long userId) {
        if (userId == null) return Result.error(401, "Unauthorized");
        long days = mealPlanService.daysSinceLastPlan(userId);
        return Result.success(Map.of("daysSinceLastPlan", days, "needReminder", days >= 3));
    }

    @GetMapping("/proactive-prompt")
    public Result<Map<String, Object>> getProactivePrompt(@AuthenticationPrincipal Long userId) {
        if (userId == null) return Result.error(401, "Unauthorized");
        return Result.success(mealPlanService.getProactivePrompt(userId));
    }

    @PostMapping("/generate")
    @Operation(
            summary = "生成饮食计划",
            description = "基于用户健康数据和 Drools 规则引擎生成个性化饮食计划",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "成功",
                            content = @Content(schema = @Schema(implementation = MealPlanGenerateResponse.class))
                    )
            }
    )
    public Result<Map<String, Object>> generatePlan(
            @AuthenticationPrincipal Long userId,
            @RequestParam String date) {
        log.info("[业务-饮食计划] 生成日计划, userId={}, date={}", userId, date);
        if (userId == null) return Result.error(401, "Unauthorized");
        try {
            LocalDate d = LocalDate.parse(date);
            mealPlanService.generateDailyPlan(userId, d);
            log.info("[业务-饮食计划] 生成完成, userId={}, date={}", userId, date);
            return Result.success(mealPlanService.getDailyPlanDetail(userId, d));
        } catch (IllegalArgumentException e) {
            log.warn("[业务-饮食计划] 生成失败, userId={}, date={}, msg={}", userId, date, e.getMessage());
            return Result.error(400, e.getMessage());
        }
    }

    @PostMapping("/generate-week")
    public Result<String> generateWeekPlan(
            @AuthenticationPrincipal Long userId,
            @RequestParam String startDate) {
        log.info("[业务-饮食计划] 生成周计划, userId={}, startDate={}", userId, startDate);
        if (userId == null) return Result.error(401, "Unauthorized");
        try {
            LocalDate start = LocalDate.parse(startDate);
            mealPlanService.generateWeekPlan(userId, start);
            log.info("[业务-饮食计划] 周计划生成完成, userId={}", userId);
            return Result.success("本周计划已生成");
        } catch (IllegalArgumentException e) {
            log.warn("[业务-饮食计划] 周计划生成失败, userId={}, msg={}", userId, e.getMessage());
            return Result.error(400, e.getMessage());
        }
    }

    @PutMapping("/{id}/complete")
    public Result<MealPlan> complete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        log.info("[业务-饮食计划] 标记完成, userId={}, planId={}", userId, id);
        if (userId == null) return Result.error(401, "Unauthorized");
        MealPlan plan = mealPlanService.getById(id);
        if (plan == null || !plan.getUserId().equals(userId)) {
            log.warn("[业务-饮食计划] 计划不存在或无权操作, userId={}, planId={}", userId, id);
            return Result.error(404, "Plan not found");
        }
        plan.setStatus(MealPlan.PlanStatus.COMPLETED);
        mealPlanService.updateById(plan);
        log.info("[业务-饮食计划] 已完成, planId={}", id);
        if (plan.getRecipeId() != null) {
            Recipe recipe = recipeService.getById(plan.getRecipeId());
            if (recipe != null) {
                recipeService.refreshPopularRecipeCacheAsync(recipe.getCategory());
            }
        }
        recipeService.refreshPopularRecipeCacheAsync(null);
        return Result.success(plan);
    }

    @PutMapping("/{id}/skip")
    public Result<MealPlan> skip(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        log.info("[业务-饮食计划] 跳过, userId={}, planId={}", userId, id);
        if (userId == null) return Result.error(401, "Unauthorized");
        MealPlan plan = mealPlanService.getById(id);
        if (plan == null || !plan.getUserId().equals(userId)) {
            return Result.error(404, "Plan not found");
        }
        plan.setStatus(MealPlan.PlanStatus.SKIPPED);
        mealPlanService.updateById(plan);
        return Result.success(plan);
    }

    @PutMapping("/{id}/replace")
    public Result<Map<String, Object>> replace(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        log.info("[业务-饮食计划] 替换菜谱, userId={}, planId={}", userId, id);
        if (userId == null) return Result.error(401, "Unauthorized");
        MealPlan plan = mealPlanService.getById(id);
        if (plan == null || !plan.getUserId().equals(userId)) {
            return Result.error(404, "Plan not found");
        }
        Recipe oldRecipe = plan.getRecipeId() != null ? recipeService.getById(plan.getRecipeId()) : null;
        double targetCal = oldRecipe != null ? getRecipeCalories(oldRecipe) : 400;
        Recipe.Category cat = mealTypeToCategory(plan.getMealType());
        Recipe newRecipe = recipeService.findReplacement(userId, cat, plan.getRecipeId(), targetCal);
        if (newRecipe == null) {
            log.warn("[业务-饮食计划] 无替换菜谱, userId={}, planId={}", userId, id);
            return Result.error(400, "暂无可替换的菜谱");
        }
        double deviationRate = calcNutritionDeviationRate(oldRecipe, newRecipe);
        String advice = deviationRate > 0.15 ? buildCompensationAdvice(oldRecipe, newRecipe) : null;
        plan.setRecipeId(newRecipe.getId());
        plan.setSuggestedCalories(java.math.BigDecimal.valueOf(getRecipeCalories(newRecipe)));
        mealPlanService.updateById(plan);
        log.info("[业务-饮食计划] 替换完成, planId={}, newRecipeId={}, deviationRate={}", id, newRecipe.getId(), deviationRate);
        java.util.Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("plan", plan);
        data.put("recipe", newRecipe);
        data.put("nutritionDeviationRate", java.math.BigDecimal.valueOf(deviationRate).setScale(3, java.math.RoundingMode.HALF_UP));
        data.put("needCompensationAdvice", deviationRate > 0.15);
        data.put("compensationAdvice", advice);
        return Result.success(data);
    }

    @PutMapping("/{id}/out-eat")
    public Result<MealPlan> markOutEat(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        if (userId == null) return Result.error(401, "Unauthorized");
        MealPlan plan = mealPlanService.getById(id);
        if (plan == null || !plan.getUserId().equals(userId)) {
            return Result.error(404, "Plan not found");
        }
        plan.setStatus(MealPlan.PlanStatus.OUT_EAT);
        plan.setOutEatNote(body != null ? body.get("note") : null);
        mealPlanService.updateById(plan);
        return Result.success(plan);
    }

    @GetMapping("/week")
    public Result<List<Map<String, Object>>> getWeekSummary(
            @AuthenticationPrincipal Long userId,
            @RequestParam String startDate) {
        if (userId == null) return Result.error(401, "Unauthorized");
        LocalDate start = LocalDate.parse(startDate);
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate d = start.plusDays(i);
            Map<String, Object> detail = mealPlanService.getDailyPlanDetail(userId, d);
            detail.put("date", d.toString());
            result.add(detail);
        }
        return Result.success(result);
    }

    private Recipe.Category mealTypeToCategory(MealPlan.MealType mt) {
        return Recipe.Category.valueOf(mt.name());
    }

    private double getRecipeCalories(Recipe r) {
        if (r == null) return 0;
        if (r.getNutritionInfo() != null) {
            Object cal = r.getNutritionInfo().get("calories");
            if (cal instanceof Number n) return n.doubleValue();
        }
        return r.getCaloriesPer100g() != null ? r.getCaloriesPer100g().doubleValue() * 2 : 0;
    }

    private double getRecipeNutrient(Recipe r, String key) {
        if (r == null || r.getNutritionInfo() == null) return 0;
        Object val = r.getNutritionInfo().get(key);
        if (val instanceof Number n) return n.doubleValue();
        if (val != null) {
            try {
                return Double.parseDouble(val.toString());
            } catch (Exception ignored) {}
        }
        return 0;
    }

    /**
     * Average relative deviation across calories/protein/carb/fat.
     */
    private double calcNutritionDeviationRate(Recipe oldRecipe, Recipe newRecipe) {
        if (oldRecipe == null || newRecipe == null) return 0;
        double oldCal = Math.max(1, getRecipeCalories(oldRecipe));
        double oldP = Math.max(1, getRecipeNutrient(oldRecipe, "protein"));
        double oldC = Math.max(1, getRecipeNutrient(oldRecipe, "carb"));
        double oldF = Math.max(1, getRecipeNutrient(oldRecipe, "fat"));

        double newCal = getRecipeCalories(newRecipe);
        double newP = getRecipeNutrient(newRecipe, "protein");
        double newC = getRecipeNutrient(newRecipe, "carb");
        double newF = getRecipeNutrient(newRecipe, "fat");

        double dCal = Math.abs(newCal - oldCal) / oldCal;
        double dP = Math.abs(newP - oldP) / oldP;
        double dC = Math.abs(newC - oldC) / oldC;
        double dF = Math.abs(newF - oldF) / oldF;
        return (dCal + dP + dC + dF) / 4.0;
    }

    private String buildCompensationAdvice(Recipe oldRecipe, Recipe newRecipe) {
        double oldP = getRecipeNutrient(oldRecipe, "protein");
        double oldC = getRecipeNutrient(oldRecipe, "carb");
        double oldF = getRecipeNutrient(oldRecipe, "fat");
        double newP = getRecipeNutrient(newRecipe, "protein");
        double newC = getRecipeNutrient(newRecipe, "carb");
        double newF = getRecipeNutrient(newRecipe, "fat");

        java.util.List<String> tips = new java.util.ArrayList<>();
        if (newP < oldP * 0.85) {
            tips.add("蛋白质偏低，可额外补充鸡蛋/牛奶/豆制品");
        }
        if (newC < oldC * 0.85) {
            tips.add("碳水偏低，可加一份全谷主食");
        }
        if (newF > oldF * 1.15) {
            tips.add("脂肪偏高，下一餐优先清淡烹饪");
        }
        if (tips.isEmpty()) {
            tips.add("营养偏差较大，建议当天其余餐次优先高蛋白、低油、主食适量");
        }
        return String.join("；", tips);
    }

    @Schema(name = "MealPlanGenerateResponse", description = "计划生成接口响应示例")
    private static class MealPlanGenerateResponse extends Result<Map<String, Object>> {
    }
}
