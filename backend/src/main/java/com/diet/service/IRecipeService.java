package com.diet.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.diet.entity.Recipe;

import java.util.List;

public interface IRecipeService extends IService<Recipe> {

    IPage<Recipe> searchRecipes(Long userId, String keyword, List<String> tags, Integer maxCalories,
            Recipe.Category category, String difficulty, Integer maxCookingTime,
            Page<Recipe> page, String sort);

    List<Recipe> recommendForUser(Long userId, Recipe.Category category);

    /** For plan generation: filter by calorie range and exclude certain recipes */
    List<Recipe> findRecipesForPlan(Long userId, Recipe.Category category, double calorieMin, double calorieMax,
            List<Long> excludeRecipeIds);

    /**
     * 当热量区间无匹配时：同分类下任意已通过菜谱（过敏过滤），保证每餐有菜。
     */
    Recipe findAnyApprovedForMeal(Long userId, Recipe.Category category, List<Long> excludeRecipeIds);

    /** Find replacement: same category, similar calories (±50), matching tags */
    Recipe findReplacement(Long userId, Recipe.Category category, Long excludeRecipeId, double targetCalories);

    /**
     * Get popular recipes by category (or all when category is null), with Redis
     * cache.
     */
    List<java.util.Map<String, Object>> getPopularRecipes(Recipe.Category category, int limit);

    /**
     * Refresh popular recipe cache for one category (or all when category is null).
     */
    void refreshPopularRecipeCache(Recipe.Category category, int limit);

    /** Refresh popular recipe cache for all categories + all. */
    void refreshAllPopularRecipeCaches(int limit);

    /** Trigger async cache refresh when popularity-related events happen. */
    void refreshPopularRecipeCacheAsync(Recipe.Category category);

    /**
     * 逻辑删除菜谱（需调用方已确认实体存在且有权删除），并刷新热门菜谱缓存。
     */
    void removeRecipeLogicalAndRefreshCache(Recipe recipe);
}
