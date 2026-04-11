package com.diet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("diet_tag")
public class DietTag {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String tagName;  // HIGH_PROTEIN, LOW_CARB, GLUTEN_FREE, etc.
    private BigDecimal confidenceScore;
    private String source;   // Rule ID
    private LocalDateTime createTime;

    public DietTag() {}

    /** Constructor for Drools rule insert - used by diet-label-rules.drl */
    public DietTag(Long userId, String tagName, double confidenceScore, String source) {
        this.userId = userId;
        this.tagName = tagName;
        this.confidenceScore = BigDecimal.valueOf(confidenceScore);
        this.source = source;
    }
}
