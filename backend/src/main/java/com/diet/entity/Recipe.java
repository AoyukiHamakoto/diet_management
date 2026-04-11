package com.diet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@TableName(value = "recipe", autoResultMap = true)
public class Recipe {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String coverImage;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> ingredients;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> nutritionInfo;
    private Category category;
    private Integer cookingTime;
    private String difficulty; // EASY, MEDIUM, HARD
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> tags;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> matchTags; // HIGH_PROTEIN, LOW_CARB, etc.
    /** Intersection of matchTags and current user's diet tags (not persisted). */
    @TableField(exist = false)
    private List<String> personalMatchTags;
    @TableField("calories_per_100g")
    private java.math.BigDecimal caloriesPer100g;
    private String proteinCarbFatRatio; // e.g. "30:40:30"
    private RecipeStatus status;
    private ReviewStage reviewStage;
    private Long firstReviewerId;
    private LocalDateTime firstReviewTime;
    private Long finalReviewerId;
    private LocalDateTime finalReviewTime;
    private String rejectReason;
    private Long creatorId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    /** 逻辑删除：与 application.yml 中 mybatis-plus.global-config.db-config 一致 */
    @TableLogic
    private Boolean deleted;

    public enum Category {
        BREAKFAST, LUNCH, DINNER, SNACK
    }

    public enum RecipeStatus {
        PENDING, APPROVED, REJECTED
    }

    public enum ReviewStage {
        PENDING, FIRST_APPROVED, FINAL_APPROVED, REJECTED
    }
}
