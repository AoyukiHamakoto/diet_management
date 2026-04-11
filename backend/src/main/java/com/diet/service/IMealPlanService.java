package com.diet.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.diet.entity.MealPlan;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface IMealPlanService extends IService<MealPlan> {

    /** Generate daily plan for user */
    List<MealPlan> generateDailyPlan(Long userId, LocalDate date);

    /** Generate 7-day plan */
    void generateWeekPlan(Long userId, LocalDate startDate);

    /** Get plan for date with recipe details */
    Map<String, Object> getDailyPlanDetail(Long userId, LocalDate date);

    /** Nutrition summary for date */
    Map<String, Object> getNutritionSummary(Long userId, LocalDate date);

    /** Days since last plan generated */
    long daysSinceLastPlan(Long userId);

    /** Proactive prompts (e.g. fix default mode, remove breakfast) */
    java.util.Map<String, Object> getProactivePrompt(Long userId);
}
