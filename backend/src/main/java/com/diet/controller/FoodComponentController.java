package com.diet.controller;

import com.diet.common.Result;
import com.diet.entity.FoodComponent;
import com.diet.service.IFoodComponentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/food-components")
@RequiredArgsConstructor
public class FoodComponentController {

    private final IFoodComponentService foodComponentService;

    @GetMapping("/search")
    public Result<Map<String, Object>> search(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (userId == null) return Result.error(401, "Unauthorized");
        var p = foodComponentService.search(keyword, category, page, size);
        return Result.success(Map.of(
                "records", p.getRecords(),
                "total", p.getTotal(),
                "pages", p.getPages(),
                "current", p.getCurrent()
        ));
    }

    @GetMapping("/match")
    public Result<List<FoodComponent>> match(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String ingredientName) {
        if (userId == null) return Result.error(401, "Unauthorized");
        List<FoodComponent> list = foodComponentService.matchByName(ingredientName != null ? ingredientName.trim() : null);
        return Result.success(list);
    }

    @GetMapping("/{id}")
    public Result<FoodComponent> getById(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        if (userId == null) return Result.error(401, "Unauthorized");
        FoodComponent fc = foodComponentService.getById(id);
        if (fc == null) return Result.error(404, "食物不存在");
        return Result.success(fc);
    }

    @PostMapping("/calculate-nutrition")
    public Result<Map<String, Object>> calculateNutrition(
            @AuthenticationPrincipal Long userId,
            @RequestBody Map<String, Object> request) {
        if (userId == null) return Result.error(401, "Unauthorized");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> ingredients = (List<Map<String, Object>>) request.get("ingredients");
        Map<String, Object> result = foodComponentService.calculateNutrition(ingredients != null ? ingredients : List.of());
        return Result.success(result);
    }
}
