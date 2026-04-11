package com.diet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("food_component")
public class FoodComponent {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String foodCode;
    private String foodName;
    private String foodCategory;
    private BigDecimal ediblePart;
    private BigDecimal energyKcal;
    private BigDecimal protein;
    private BigDecimal fat;
    private BigDecimal carbohydrate;
    private BigDecimal dietaryFiber;
    private BigDecimal cholesterol;
    private BigDecimal vitaminA;
    private BigDecimal calcium;
    private BigDecimal iron;
    private BigDecimal zinc;
}
