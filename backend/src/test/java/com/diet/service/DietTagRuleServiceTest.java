package com.diet.service;

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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class DietTagRuleServiceTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379).toString());
    }

    @Autowired
    private DietTagRuleService dietTagRuleService;

    @Test
    void bmiOverweightRule() {
        HealthProfile profile = new HealthProfile();
        profile.setUserId(1L);
        profile.setBmi(BigDecimal.valueOf(26));  // 24-28
        List<com.diet.entity.DietTag> tags = dietTagRuleService.generateDietTags(profile);
        assertTrue(tags.stream().anyMatch(t -> "LOW_CALORIE".equals(t.getTagName())));
    }

    /** 深度检查：BMI=26 + 目标=减脂 应生成 LOW_CALORIE 标签（由 BMI 规则触发，与目标组合仍满足） */
    @Test
    void bmi26WithLoseWeightTargetGeneratesLowCalorieTag() {
        HealthProfile profile = new HealthProfile();
        profile.setUserId(1L);
        profile.setBmi(BigDecimal.valueOf(26));
        profile.setTarget(HealthProfile.Target.LOSE_WEIGHT);
        List<com.diet.entity.DietTag> tags = dietTagRuleService.generateDietTags(profile);
        assertTrue(tags.stream().anyMatch(t -> "LOW_CALORIE".equals(t.getTagName())),
                "BMI=26 + 目标=减脂 应生成 LOW_CALORIE 标签");
        assertEquals("BMI_RULE", tags.stream().filter(t -> "LOW_CALORIE".equals(t.getTagName())).findFirst().orElseThrow().getSource());
    }

    @Test
    void muscleBuildingRule() {
        HealthProfile profile = new HealthProfile();
        profile.setUserId(1L);
        profile.setTarget(HealthProfile.Target.BUILD_MUSCLE);
        List<com.diet.entity.DietTag> tags = dietTagRuleService.generateDietTags(profile);
        assertTrue(tags.stream().anyMatch(t -> "HIGH_PROTEIN".equals(t.getTagName())));
        assertTrue(tags.stream().anyMatch(t -> "HIGH_CALORIE".equals(t.getTagName())));
    }

    @Test
    void lactoseAllergyRule() {
        HealthProfile profile = new HealthProfile();
        profile.setUserId(1L);
        profile.setAllergyTags(List.of("牛奶"));
        List<com.diet.entity.DietTag> tags = dietTagRuleService.generateDietTags(profile);
        assertTrue(tags.stream().anyMatch(t -> "DAIRY_FREE".equals(t.getTagName())));
    }

    @Test
    void fatLossHighExerciseRule() {
        HealthProfile profile = new HealthProfile();
        profile.setUserId(1L);
        profile.setTarget(HealthProfile.Target.LOSE_WEIGHT);
        profile.setExerciseFrequency(HealthProfile.ExerciseFrequency.HIGH);
        List<com.diet.entity.DietTag> tags = dietTagRuleService.generateDietTags(profile);
        assertTrue(tags.stream().anyMatch(t -> "LOW_CARB".equals(t.getTagName())));
        assertTrue(tags.stream().anyMatch(t -> "HIGH_PROTEIN".equals(t.getTagName())));
    }
}
