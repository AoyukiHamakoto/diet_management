package com.diet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "health_profile", autoResultMap = true)
public class HealthProfile {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private BigDecimal height;
    private BigDecimal weight;
    private Gender gender;
    private Integer age;
    private BigDecimal bmi;
    private Target target;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> allergyTags;
    private ExerciseFrequency exerciseFrequency;
    /** 运动时段偏好：用户自主选择，供规则引擎（如练后餐）使用 */
    private ExerciseTime exerciseTime;
    private BigDecimal tdee;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public enum Gender {
        MALE, FEMALE
    }

    public enum Target {
        LOSE_WEIGHT, BUILD_MUSCLE, MAINTAIN
    }

    public enum ExerciseFrequency {
        NONE, LIGHT, MODERATE, HIGH
    }

    /** 运动时段偏好：早晨/下午/晚上/无 */
    public enum ExerciseTime {
        MORNING,   // 6:00-12:00
        AFTERNOON, // 12:00-18:00
        EVENING,   // 18:00-23:00
        NONE       // 不固定/暂无
    }
}
