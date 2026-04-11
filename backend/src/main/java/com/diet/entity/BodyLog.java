package com.diet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("body_log")
public class BodyLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private BigDecimal weight;
    private LocalDate logDate;
    private LocalDateTime createTime;
}
