package com.diet.service;

import com.diet.config.KieContainerHolder;
import com.diet.dto.MealPlanSuggestionDTO;
import com.diet.entity.DietTag;
import com.diet.entity.HealthProfile;
import org.kie.api.runtime.KieSession;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Service
public class MealPlanRuleService {

    private final KieContainerHolder kieContainerHolder;

    public MealPlanRuleService(KieContainerHolder kieContainerHolder) {
        this.kieContainerHolder = kieContainerHolder;
    }

    public MealPlanSuggestionDTO getSuggestion(HealthProfile profile,
                                               List<DietTag> dietTags,
                                               LocalDate date,
                                               String workoutTime,
                                               int baseCalories) {
        MealPlanSuggestionDTO suggestion = new MealPlanSuggestionDTO();
        suggestion.setSuggestedCalories(baseCalories);
        suggestion.setPlanMode("STANDARD");
        suggestion.setWorkoutTime(workoutTime != null ? workoutTime.toUpperCase() : "UNKNOWN");
        suggestion.setWeekend(isWeekend(date));

        KieSession session = null;
        try {
            session = kieContainerHolder.get().newKieSession("mealPlanSession");
            session.insert(profile);
            session.insert(suggestion);
            if (dietTags != null) {
                for (DietTag tag : dietTags) {
                    session.insert(tag);
                }
            }
            int fired = session.fireAllRules(200);
            if (fired >= 200) {
                throw new IllegalStateException("规则执行异常：可能存在循环触发，请检查 meal-plan-rules.drl");
            }
            // 第二层 Drools：优化过滤（与论文「营养计算 + 优化过滤」双层规则库对应）
            KieSession opt = null;
            try {
                opt = kieContainerHolder.get().newKieSession("nutritionOptimizeSession");
                opt.insert(profile);
                opt.insert(suggestion);
                int optFired = opt.fireAllRules(100);
                if (optFired >= 100) {
                    throw new IllegalStateException("规则执行异常：请检查 nutrition-optimize-rules.drl");
                }
            } finally {
                if (opt != null) {
                    opt.dispose();
                }
            }
            return suggestion;
        } finally {
            if (session != null) {
                session.dispose();
            }
        }
    }

    private boolean isWeekend(LocalDate date) {
        LocalDate d = date != null ? date : LocalDate.now();
        DayOfWeek day = d.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }
}
