package com.diet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("meal_plan")
public class MealPlan {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private LocalDate planDate;
    private MealType mealType;
    private Long recipeId;
    private BigDecimal suggestedCalories;
    private PlanStatus status;
    private AuditStatus auditStatus;
    private String auditComment;
    private LocalDateTime auditedAt;
    private Long auditedBy;
    private String outEatNote;  // Record when eating out
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public enum MealType {
        BREAKFAST, LUNCH, DINNER, SNACK
    }

    public enum PlanStatus {
        PLANNED, COMPLETED, SKIPPED, OUT_EAT  // OUT_EAT = marked as eating out
    }

    public enum AuditStatus {
        AUTO, PENDING, APPROVED, REJECTED
    }
}
