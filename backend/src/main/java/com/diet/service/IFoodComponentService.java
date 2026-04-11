package com.diet.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.diet.entity.FoodComponent;

import java.util.List;
import java.util.Map;

public interface IFoodComponentService extends IService<FoodComponent> {

    IPage<FoodComponent> search(String keyword, String category, int page, int size);

    List<FoodComponent> matchByName(String ingredientName);

    Map<String, Object> calculateNutrition(List<Map<String, Object>> ingredients);
}
