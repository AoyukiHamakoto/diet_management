package com.diet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("order_record")
public class OrderRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String orderNo;

    private String itemName;

    private BigDecimal amount;

    private String status; // PENDING, PAID, REFUNDED

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

