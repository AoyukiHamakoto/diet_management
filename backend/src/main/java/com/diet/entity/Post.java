package com.diet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("post")
public class Post {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private String content;

    private String status; // PENDING, APPROVED, REJECTED

    private String rejectReason;

    private Long reviewerId;

    private LocalDateTime reviewTime;

    /** 点赞数（社区） */
    private Integer likeCount;

    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

