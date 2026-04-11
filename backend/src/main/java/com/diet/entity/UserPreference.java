package com.diet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName(value = "user_preference", autoResultMap = true)
public class UserPreference {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private BigDecimal calorieMultiplier;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Double> tagWeights;
    private Integer adjustmentCount;
    private Integer learningProgress;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
