package com.diet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("system_notification")
public class SystemNotification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String message;
    private Boolean isRead;
    private LocalDateTime createTime;
}
