package com.diet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.diet.entity.FoodComponent;
import com.diet.mapper.FoodComponentMapper;
import com.diet.service.IFoodComponentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FoodComponentServiceImpl extends ServiceImpl<FoodComponentMapper, FoodComponent> implements IFoodComponentService {

    @Override
    public IPage<FoodComponent> search(String keyword, String category, int page, int size) {
        LambdaQueryWrapper<FoodComponent> q = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            q.like(FoodComponent::getFoodName, keyword);
        }
        if (category != null && !category.isBlank()) {
            q.eq(FoodComponent::getFoodCategory, category);
        }
        q.orderByAsc(FoodComponent::getFoodName);
        return page(new Page<>(page, size), q);
    }

    @Override
    public List<FoodComponent> matchByName(String ingredientName) {
        if (ingredientName == null || ingredientName.isBlank()) {
            return list(new LambdaQueryWrapper<FoodComponent>().last("LIMIT 20"));
        }
        return lambdaQuery()
                .like(FoodComponent::getFoodName, ingredientName)
                .last("LIMIT 20")
                .list();
    }

    @Override
    public Map<String, Object> calculateNutrition(List<Map<String, Object>> ingredients) {
        if (ingredients == null || ingredients.isEmpty()) {
            return Map.of(
                    "totalCalories", 0,
                    "totalProtein", BigDecimal.ZERO,
                    "totalFat", BigDecimal.ZERO,
                    "totalCarbs", BigDecimal.ZERO,
                    "details", List.<Map<String, Object>>of()
            );
        }
        BigDecimal totalCal = BigDecimal.ZERO;
        BigDecimal totalP = BigDecimal.ZERO;
        BigDecimal totalF = BigDecimal.ZERO;
        BigDecimal totalC = BigDecimal.ZERO;
        List<Map<String, Object>> details = new ArrayList<>();

        for (Map<String, Object> ing : ingredients) {
            Object idObj = ing.get("foodComponentId");
            Object amountObj = ing.get("amount");
            if (idObj == null) continue;
            long id = idObj instanceof Number ? ((Number) idObj).longValue() : Long.parseLong(String.valueOf(idObj));
            double amount = amountObj != null && amountObj instanceof Number
                    ? ((Number) amountObj).doubleValue() : (amountObj != null ? Double.parseDouble(String.valueOf(amountObj)) : 0);
            if (amount <= 0) continue;

            FoodComponent fc = getById(id);
            if (fc == null) continue;

            double ratio = amount / 100.0;
            BigDecimal cal = toBigDecimal(fc.getEnergyKcal()).multiply(BigDecimal.valueOf(ratio)).setScale(0, RoundingMode.HALF_UP);
            BigDecimal p = toBigDecimal(fc.getProtein()).multiply(BigDecimal.valueOf(ratio)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal f = toBigDecimal(fc.getFat()).multiply(BigDecimal.valueOf(ratio)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal c = toBigDecimal(fc.getCarbohydrate()).multiply(BigDecimal.valueOf(ratio)).setScale(2, RoundingMode.HALF_UP);

            totalCal = totalCal.add(cal);
            totalP = totalP.add(p);
            totalF = totalF.add(f);
            totalC = totalC.add(c);

            Map<String, Object> d = new HashMap<>();
            d.put("foodComponentId", id);
            d.put("foodName", fc.getFoodName());
            d.put("amount", amount);
            d.put("calories", cal.intValue());
            d.put("protein", p);
            d.put("fat", f);
            d.put("carbohydrate", c);
            details.add(d);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalCalories", totalCal.intValue());
        result.put("totalProtein", totalP);
        result.put("totalFat", totalF);
        result.put("totalCarbs", totalC);
        result.put("details", details);
        return result;
    }

    private static BigDecimal toBigDecimal(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
