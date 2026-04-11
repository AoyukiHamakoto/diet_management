package com.diet.controller;

import com.diet.common.Result;
import com.diet.entity.MealPlan;
import com.diet.entity.Recipe;
import com.diet.service.IMealPlanService;
import com.diet.service.IRecipeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping({ "/recipe", "/recipes" })
@RequiredArgsConstructor
public class RecipeController {

    private final IRecipeService recipeService;
    private final IMealPlanService mealPlanService;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @GetMapping("/search")
    public Result<Map<String, Object>> search(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) Integer maxCalories,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) Integer maxTime,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "12") @Min(1) int size,
            @RequestParam(required = false, defaultValue = "match") String sort) {
        var pageable = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<Recipe>(page, size);
        Recipe.Category cat = (category != null && !category.isBlank())
                ? Recipe.Category.valueOf(category)
                : null;
        var result = recipeService.searchRecipes(userId, keyword, tags, maxCalories, cat, difficulty, maxTime,
                pageable, sort);
        return Result.success(Map.of(
                "records", result.getRecords(),
                "total", result.getTotal(),
                "pages", result.getPages(),
                "current", result.getCurrent()));
    }

    @GetMapping("/my")
    public Result<Map<String, Object>> myRecipes(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size,
            @RequestParam(required = false, defaultValue = "ALL") String status) {
        if (userId == null) {
            return Result.error(401, "Unauthorized");
        }
        int safeSize = Math.min(size, 50);
        var pageable = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<Recipe>(page, safeSize);
        var query = recipeService.lambdaQuery()
                .eq(Recipe::getCreatorId, userId)
                .orderByDesc(Recipe::getCreateTime);
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            Recipe.RecipeStatus s;
            try {
                s = Recipe.RecipeStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                return Result.error(400, "Invalid status value");
            }
            query.eq(Recipe::getStatus, s);
        }
        var result = query.page(pageable);
        List<RecipeSimpleDTO> records = result.getRecords().stream()
                .map(this::toSimpleDto)
                .collect(Collectors.toList());
        return Result.success(Map.of(
                "records", records,
                "total", result.getTotal(),
                "pages", result.getPages(),
                "current", result.getCurrent()));
    }

    @GetMapping("/recommend")
    public Result<List<Recipe>> recommend(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "LUNCH") String category) {
        if (userId == null) {
            return Result.error(401, "Unauthorized");
        }
        var recipes = recipeService.recommendForUser(userId, Recipe.Category.valueOf(category));
        return Result.success(recipes);
    }

    @GetMapping("/popular")
    public Result<List<Map<String, Object>>> popular(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "20") @Min(1) int limit) {
        Recipe.Category cat = parseCategory(category);
        return Result.success(recipeService.getPopularRecipes(cat, limit));
    }

    @PostMapping("/popular/refresh")
    public Result<Void> refreshPopular(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "20") @Min(1) int limit) {
        Recipe.Category cat = parseCategory(category);
        if (cat == null) {
            recipeService.refreshAllPopularRecipeCaches(limit);
        } else {
            recipeService.refreshPopularRecipeCache(cat, limit);
        }
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<Recipe> getById(@PathVariable Long id) {
        var recipe = recipeService.getById(id);
        if (recipe == null) {
            return Result.error(404, "Recipe not found");
        }
        return Result.success(recipe);
    }

    @PostMapping("/upload-image")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Long userId) {
        if (userId == null)
            return Result.error(401, "Unauthorized");
        if (file.isEmpty())
            return Result.error(400, "File is empty");

        String ext = file.getOriginalFilename() != null && file.getOriginalFilename().contains(".")
                ? file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."))
                : ".jpg";
        String filename = "recipe/" + UUID.randomUUID() + ext;

        try {
            Path basePath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path dir = basePath.resolve("recipe");
            Files.createDirectories(dir);
            Path target = dir.resolve(filename.substring("recipe/".length()));
            file.transferTo(target.toFile());
            String url = "/api/uploads/" + filename;
            return Result.success(Map.of("url", url, "filename", filename));
        } catch (IOException e) {
            return Result.error(500, "Upload failed: " + e.getMessage());
        }
    }

    @PostMapping
    public Result<Recipe> create(@AuthenticationPrincipal Long userId, @Valid @RequestBody RecipeCreateRequest req) {
        if (userId == null)
            return Result.error(401, "Unauthorized");

        Recipe recipe = new Recipe();
        recipe.setTitle(req.getTitle());
        recipe.setCoverImage(req.getCoverImage());
        recipe.setIngredients(req.getIngredients());
        recipe.setNutritionInfo(req.getNutritionInfo());
        recipe.setCategory(Recipe.Category.valueOf(req.getCategory()));
        recipe.setCookingTime(req.getCookingTime());
        recipe.setDifficulty(req.getDifficulty());
        recipe.setTags(req.getTags());
        recipe.setMatchTags(req.getMatchTags());
        recipe.setCaloriesPer100g(req.getCaloriesPer100g());
        recipe.setProteinCarbFatRatio(req.getProteinCarbFatRatio());
        recipe.setStatus(Recipe.RecipeStatus.PENDING);
        recipe.setCreatorId(userId);
        recipeService.save(recipe);
        return Result.success(recipe);
    }

    @PutMapping("/{id}")
    public Result<Recipe> updateRecipe(@AuthenticationPrincipal Long userId,
            @PathVariable Long id,
            @RequestBody RecipeUpdateRequest req) {
        if (userId == null) {
            return Result.error(401, "Unauthorized");
        }
        Recipe recipe = recipeService.getById(id);
        if (recipe == null) {
            return Result.error(404, "Recipe not found");
        }
        if (!Objects.equals(recipe.getCreatorId(), userId)) {
            return Result.error(403, "无权修改该菜谱");
        }
        if (recipe.getStatus() == Recipe.RecipeStatus.APPROVED) {
            return Result.error(409, "已通过审核的菜谱不能修改");
        }
        // Partial update: only set non-null fields from request
        if (req.getTitle() != null && !req.getTitle().isBlank()) {
            recipe.setTitle(req.getTitle().trim());
        }
        if (req.getCoverImage() != null) {
            recipe.setCoverImage(req.getCoverImage());
        }
        if (req.getIngredients() != null) {
            recipe.setIngredients(req.getIngredients());
        }
        if (req.getNutritionInfo() != null) {
            recipe.setNutritionInfo(req.getNutritionInfo());
        }
        if (req.getCategory() != null && !req.getCategory().isBlank()) {
            recipe.setCategory(Recipe.Category.valueOf(req.getCategory().toUpperCase().trim()));
        }
        if (req.getCookingTime() != null) {
            recipe.setCookingTime(req.getCookingTime());
        }
        if (req.getDifficulty() != null) {
            recipe.setDifficulty(req.getDifficulty());
        }
        if (req.getTags() != null) {
            recipe.setTags(req.getTags());
        }
        if (req.getMatchTags() != null) {
            recipe.setMatchTags(req.getMatchTags());
        }
        if (req.getCaloriesPer100g() != null) {
            recipe.setCaloriesPer100g(req.getCaloriesPer100g());
        }
        if (req.getProteinCarbFatRatio() != null) {
            recipe.setProteinCarbFatRatio(req.getProteinCarbFatRatio());
        }
        if (recipe.getStatus() == Recipe.RecipeStatus.REJECTED) {
            recipe.setStatus(Recipe.RecipeStatus.PENDING);
            recipe.setRejectReason(null);
        }
        recipe.setUpdateTime(LocalDateTime.now());
        recipeService.updateById(recipe);
        return Result.success(recipeService.getById(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteRecipe(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        if (userId == null) {
            return Result.error(401, "Unauthorized");
        }
        Recipe recipe = recipeService.getById(id);
        if (recipe == null) {
            return Result.error(404, "Recipe not found");
        }
        if (!Objects.equals(recipe.getCreatorId(), userId)) {
            return Result.error(403, "无权删除该菜谱");
        }
        recipeService.removeRecipeLogicalAndRefreshCache(recipe);
        return Result.success();
    }

    @PostMapping("/{id}/add-to-plan")
    public Result<Void> addToPlan(@AuthenticationPrincipal Long userId,
            @PathVariable Long id,
            @RequestBody AddToPlanRequest req) {
        if (userId == null)
            return Result.error(401, "Unauthorized");
        Recipe recipe = recipeService.getById(id);
        if (recipe == null)
            return Result.error(404, "Recipe not found");

        MealPlan plan = new MealPlan();
        plan.setUserId(userId);
        plan.setPlanDate(req.getPlanDate() != null ? req.getPlanDate() : LocalDate.now());
        plan.setMealType(MealPlan.MealType.valueOf(req.getMealType()));
        plan.setRecipeId(id);
        Object cal = recipe.getNutritionInfo() != null ? recipe.getNutritionInfo().get("calories") : null;
        plan.setSuggestedCalories(cal instanceof Number n ? java.math.BigDecimal.valueOf(n.doubleValue()) : null);
        plan.setStatus(MealPlan.PlanStatus.PLANNED);
        mealPlanService.save(plan);
        recipeService.refreshPopularRecipeCacheAsync(recipe.getCategory());
        recipeService.refreshPopularRecipeCacheAsync(null);
        return Result.success();
    }

    @Data
    public static class RecipeCreateRequest {
        private String title;
        private String coverImage;
        private java.util.List<java.util.Map<String, Object>> ingredients;
        private java.util.Map<String, Object> nutritionInfo;
        private String category;
        private Integer cookingTime;
        private String difficulty;
        private java.util.List<String> tags;
        private java.util.List<String> matchTags;
        private java.math.BigDecimal caloriesPer100g;
        private String proteinCarbFatRatio;
    }

    /** Partial update: all fields optional. */
    @Data
    public static class RecipeUpdateRequest {
        private String title;
        private String coverImage;
        private java.util.List<java.util.Map<String, Object>> ingredients;
        private java.util.Map<String, Object> nutritionInfo;
        private String category;
        private Integer cookingTime;
        private String difficulty;
        private java.util.List<String> tags;
        private java.util.List<String> matchTags;
        private java.math.BigDecimal caloriesPer100g;
        private String proteinCarbFatRatio;
    }

    @Data
    public static class AddToPlanRequest {
        private String mealType; // BREAKFAST, LUNCH, DINNER, SNACK
        private java.time.LocalDate planDate;
    }

    private Recipe.Category parseCategory(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        return switch (category.trim().toUpperCase()) {
            case "BREAKFAST", "早餐" -> Recipe.Category.BREAKFAST;
            case "LUNCH", "午餐" -> Recipe.Category.LUNCH;
            case "DINNER", "晚餐" -> Recipe.Category.DINNER;
            case "SNACK", "加餐" -> Recipe.Category.SNACK;
            default -> Recipe.Category.valueOf(category.trim().toUpperCase());
        };
    }

    private RecipeSimpleDTO toSimpleDto(Recipe recipe) {
        RecipeSimpleDTO dto = new RecipeSimpleDTO();
        dto.setId(recipe.getId());
        dto.setTitle(recipe.getTitle());
        dto.setCoverImage(recipe.getCoverImage());
        dto.setCategory(recipe.getCategory() != null ? recipe.getCategory().name() : null);
        dto.setCalories(extractCalories(recipe));
        dto.setStatus(recipe.getStatus() != null ? recipe.getStatus().name() : null);
        dto.setCreatedAt(recipe.getCreateTime());
        dto.setRejectReason(recipe.getStatus() == Recipe.RecipeStatus.REJECTED ? recipe.getRejectReason() : null);
        return dto;
    }

    private Integer extractCalories(Recipe recipe) {
        if (recipe.getNutritionInfo() != null) {
            Object cal = recipe.getNutritionInfo().get("calories");
            if (cal instanceof Number n) {
                return (int) Math.round(n.doubleValue());
            }
            if (cal != null) {
                try {
                    return (int) Math.round(Double.parseDouble(cal.toString()));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        BigDecimal c100 = recipe.getCaloriesPer100g();
        if (c100 != null) {
            return (int) Math.round(c100.doubleValue() * 2);
        }
        return null;
    }

    @Data
    public static class RecipeSimpleDTO {
        private Long id;
        private String title;
        private String coverImage;
        private String category;
        private Integer calories;
        private String status;
        private LocalDateTime createdAt;
        private String rejectReason;
    }
}
