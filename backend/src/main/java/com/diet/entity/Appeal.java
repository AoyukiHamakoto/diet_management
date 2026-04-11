package com.diet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("appeal")
public class Appeal {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private TargetType targetType;
    private Long targetId;
    private String reason;
    private Status status;
    private Long adminId;
    private String adminComment;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public enum TargetType {
        MEAL_PLAN, RECIPE, POST
    }

    public enum Status {
        PENDING, PROCESSING, RESOLVED, REJECTED
    }
}
