package com.diet.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MealPlanSuggestionDTO {
    private int suggestedCalories;
    private String planMode;
    private String workoutTime;
    private boolean weekend;

    // Dinner-specific tuning for post-workout scenario.
    private double dinnerProteinBoostRatio = 1.0;
    private String dinnerTag;

    // Weekend maintain mode: allow a looser upper calorie bound.
    private boolean allowCheatMeal;
    private double calorieUpperMultiplier = 1.0;

    private List<String> flags = new ArrayList<>();

    public void addFlag(String flag) {
        if (flag != null && !flag.isBlank() && !flags.contains(flag)) {
            flags.add(flag);
        }
    }
}
