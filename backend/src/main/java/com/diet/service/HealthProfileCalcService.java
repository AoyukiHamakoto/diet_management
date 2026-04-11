package com.diet.service;

import com.diet.entity.HealthProfile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * BMI and TDEE calculation service (Mifflin-St Jeor formula)
 */
@Service
public class HealthProfileCalcService {

    public static final String BMI_UNDERWEIGHT = "偏瘦";
    public static final String BMI_NORMAL = "正常";
    public static final String BMI_OVERWEIGHT = "偏胖";
    public static final String BMI_OBESE = "肥胖";

    /**
     * BMI = weight(kg) / (height(m))^2
     * Precision: 1 decimal place
     */
    public BigDecimal calculateBmi(BigDecimal heightCm, BigDecimal weightKg) {
        if (heightCm == null || weightKg == null
                || heightCm.compareTo(BigDecimal.ZERO) <= 0
                || weightKg.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        BigDecimal heightM = heightCm.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return weightKg.divide(heightM.multiply(heightM), 1, RoundingMode.HALF_UP);
    }

    /**
     * BMI rating: 偏瘦(<18.5), 正常(18.5-24), 偏胖(24-28), 肥胖(>=28)
     */
    public String getBmiRating(BigDecimal bmi) {
        if (bmi == null) return null;
        if (bmi.compareTo(BigDecimal.valueOf(18.5)) < 0) return BMI_UNDERWEIGHT;
        if (bmi.compareTo(BigDecimal.valueOf(24)) < 0) return BMI_NORMAL;
        if (bmi.compareTo(BigDecimal.valueOf(28)) < 0) return BMI_OVERWEIGHT;
        return BMI_OBESE;
    }

    /**
     * Mifflin-St Jeor BMR formula:
     * Male: 10×weight + 6.25×height - 5×age + 5
     * Female: 10×weight + 6.25×height - 5×age - 161
     * TDEE = BMR × activity factor
     * Activity: NONE=1.2, LIGHT=1.375, MODERATE=1.55, HIGH=1.725
     */
    public BigDecimal calculateTdee(BigDecimal weightKg, BigDecimal heightCm, Integer age,
                                    HealthProfile.Gender gender, HealthProfile.ExerciseFrequency exerciseFrequency) {
        if (weightKg == null || heightCm == null || age == null || gender == null || exerciseFrequency == null) {
            return null;
        }
        if (age <= 0 || age > 150) {
            return null;
        }
        double bmr;
        if (gender == HealthProfile.Gender.MALE) {
            bmr = 10 * weightKg.doubleValue() + 6.25 * heightCm.doubleValue() - 5 * age + 5;
        } else {
            bmr = 10 * weightKg.doubleValue() + 6.25 * heightCm.doubleValue() - 5 * age - 161;
        }
        double factor = getActivityFactor(exerciseFrequency);
        return BigDecimal.valueOf(Math.round(bmr * factor)).setScale(0, RoundingMode.HALF_UP);
    }

    private double getActivityFactor(HealthProfile.ExerciseFrequency freq) {
        return switch (freq) {
            case NONE -> 1.2;      // 久坐
            case LIGHT -> 1.375;   // 轻度
            case MODERATE -> 1.55; // 中度
            case HIGH -> 1.725;    // 高强度
        };
    }
}
