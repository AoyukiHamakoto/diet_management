package com.diet.service;

import com.diet.entity.HealthProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class HealthProfileCalcServiceTest {

    private HealthProfileCalcService calcService;

    @BeforeEach
    void setUp() {
        calcService = new HealthProfileCalcService();
    }

    @Test
    void bmiCalculation() {
        BigDecimal bmi = calcService.calculateBmi(BigDecimal.valueOf(180), BigDecimal.valueOf(75));
        assertEquals(23.1, bmi.doubleValue(), 0.1);
    }

    @Test
    void tdeeMaleOfficeWorker() {
        // 180cm, 75kg, male, 25yo, office work (NONE=1.2)
        // BMR = 10*75 + 6.25*180 - 5*25 + 5 = 750 + 1125 - 125 + 5 = 1755; TDEE = 1755*1.2 = 2106
        BigDecimal tdee = calcService.calculateTdee(
                BigDecimal.valueOf(75),
                BigDecimal.valueOf(180),
                25,
                HealthProfile.Gender.MALE,
                HealthProfile.ExerciseFrequency.NONE);
        assertNotNull(tdee);
        assertEquals(2106, tdee.intValue());
    }

    @Test
    void tdeeFemaleAndActivityFactors() {
        // 165cm, 60kg, female, 30yo -> BMR = 10*60 + 6.25*165 - 5*30 - 161 = 600 + 1031.25 - 150 - 161 = 1320.25
        // NONE 1.2 -> 1584, LIGHT 1.375 -> 1815, MODERATE 1.55 -> 2046, HIGH 1.725 -> 2277
        BigDecimal tdeeFemale = calcService.calculateTdee(
                BigDecimal.valueOf(60), BigDecimal.valueOf(165), 30,
                HealthProfile.Gender.FEMALE, HealthProfile.ExerciseFrequency.NONE);
        assertNotNull(tdeeFemale);
        assertEquals(1584, tdeeFemale.intValue());
        BigDecimal tdeeHigh = calcService.calculateTdee(
                BigDecimal.valueOf(60), BigDecimal.valueOf(165), 30,
                HealthProfile.Gender.FEMALE, HealthProfile.ExerciseFrequency.HIGH);
        assertEquals(2277, tdeeHigh.intValue());
    }

    @Test
    void tdeeAgeZeroOrNegativeReturnsNull() {
        assertNull(calcService.calculateTdee(
                BigDecimal.valueOf(70), BigDecimal.valueOf(170), 0,
                HealthProfile.Gender.MALE, HealthProfile.ExerciseFrequency.NONE));
        assertNull(calcService.calculateTdee(
                BigDecimal.valueOf(70), BigDecimal.valueOf(170), -1,
                HealthProfile.Gender.MALE, HealthProfile.ExerciseFrequency.NONE));
    }

    @Test
    void bmiRating() {
        assertEquals(HealthProfileCalcService.BMI_NORMAL, calcService.getBmiRating(BigDecimal.valueOf(22)));
        assertEquals(HealthProfileCalcService.BMI_UNDERWEIGHT, calcService.getBmiRating(BigDecimal.valueOf(18)));
        assertEquals(HealthProfileCalcService.BMI_OVERWEIGHT, calcService.getBmiRating(BigDecimal.valueOf(26)));
        assertEquals(HealthProfileCalcService.BMI_OBESE, calcService.getBmiRating(BigDecimal.valueOf(30)));
    }
}
