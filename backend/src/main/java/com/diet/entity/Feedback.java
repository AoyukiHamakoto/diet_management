package com.diet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "feedback", autoResultMap = true)
public class Feedback {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long planId;
    private Integer rating; // 1-5
    private Integer satietyRating; // 1-5
    private Integer difficultyRating; // 1-5 (higher = easier)
    private java.math.BigDecimal satisfactionScore; // taste*0.3 + satiety*0.3 + difficulty*0.4
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> feedbackTags; // TOO_OILY, TOO_MUCH, TASTE_GOOD, etc.
    private TasteFeedback tasteFeedback; // legacy
    private BodyReaction bodyReaction;
    private String comment;
    private String systemAction; // What system did based on this feedback
    private LocalDateTime createTime;

    public enum TasteFeedback {
        TOO_LIGHT, TOO_OILY, TOO_MUCH, TOO_SALTY, TOO_SWEET, JUST_RIGHT
    }

    public enum BodyReaction {
        ENERGETIC, BLOATED, TIRED, SATISFIED, HUNGRY, NAUSEOUS
    }
}
