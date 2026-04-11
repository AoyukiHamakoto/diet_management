package com.diet.service;

import com.diet.dto.MealPlanSuggestionDTO;
import com.diet.entity.DietTag;
import com.diet.entity.HealthProfile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@SuppressWarnings("resource")
class MealPlanRuleServiceTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379).toString());
    }

    @Autowired
    private MealPlanRuleService mealPlanRuleService;

    @Test
    void aggressiveFatLossRuleShouldAdjustCalories() {
        HealthProfile profile = new HealthProfile();
        profile.setUserId(1L);
        profile.setTarget(HealthProfile.Target.LOSE_WEIGHT);
        profile.setBmi(BigDecimal.valueOf(29));
        profile.setExerciseFrequency(HealthProfile.ExerciseFrequency.HIGH);
        profile.setTdee(BigDecimal.valueOf(2400));

        MealPlanSuggestionDTO suggestion = mealPlanRuleService.getSuggestion(
                profile, List.of(), LocalDate.of(2026, 2, 23), "EVENING", 2400);

        assertEquals(1800, suggestion.getSuggestedCalories());
        assertEquals("AGGRESSIVE_FAT_LOSS", suggestion.getPlanMode());
        assertTrue(suggestion.getFlags().contains("激进减脂模式"));
    }

    @Test
    void eveningWorkoutWithHighProteinTagShouldBoostDinner() {
        HealthProfile profile = new HealthProfile();
        profile.setUserId(2L);
        profile.setTarget(HealthProfile.Target.BUILD_MUSCLE);
        profile.setTdee(BigDecimal.valueOf(2300));

        DietTag highProtein = new DietTag();
        highProtein.setUserId(2L);
        highProtein.setTagName("HIGH_PROTEIN");

        MealPlanSuggestionDTO suggestion = mealPlanRuleService.getSuggestion(
                profile, List.of(highProtein), LocalDate.of(2026, 2, 24), "EVENING", 2700);

        assertEquals(1.20, suggestion.getDinnerProteinBoostRatio(), 0.001);
        assertEquals("POST_WORKOUT_MEAL", suggestion.getDinnerTag());
        assertTrue(suggestion.getFlags().contains("晚餐蛋白增强（练后餐）"));
    }

    @Test
    void weekendMaintainShouldAllowCheatMeal() {
        HealthProfile profile = new HealthProfile();
        profile.setUserId(3L);
        profile.setTarget(HealthProfile.Target.MAINTAIN);
        profile.setTdee(BigDecimal.valueOf(2000));

        MealPlanSuggestionDTO suggestion = mealPlanRuleService.getSuggestion(
                profile, List.of(), LocalDate.of(2026, 2, 22), "UNKNOWN", 2000);

        assertTrue(suggestion.isAllowCheatMeal());
        assertEquals(1.10, suggestion.getCalorieUpperMultiplier(), 0.001);
        assertTrue(suggestion.getFlags().contains("周末可放宽热量上限10%"));
    }
}
