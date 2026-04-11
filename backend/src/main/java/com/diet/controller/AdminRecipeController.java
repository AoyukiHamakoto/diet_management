package com.diet.controller;

import com.diet.common.Result;
import com.diet.entity.Recipe;
import com.diet.service.IRecipeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/recipe")
@RequiredArgsConstructor
@Tag(name = "管理员接口")
public class AdminRecipeController {

    private final IRecipeService recipeService;

    @GetMapping("/pending")
    public Result<List<Recipe>> getPending() {
        var list = recipeService.lambdaQuery()
                .eq(Recipe::getStatus, Recipe.RecipeStatus.PENDING)
                .orderByDesc(Recipe::getCreateTime)
                .list();
        return Result.success(list);
    }

    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        var pageable = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<Recipe>(page, size);
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Recipe>()
                .orderByDesc("create_time");
        if (status != null && !status.isBlank()) {
            try {
                wrapper.eq("status", Recipe.RecipeStatus.valueOf(status));
            } catch (IllegalArgumentException ignored) {}
        }
        var result = recipeService.page(pageable, wrapper);
        return Result.success(Map.of(
                "records", result.getRecords(),
                "total", result.getTotal(),
                "pages", result.getPages(),
                "current", result.getCurrent()
        ));
    }

    @PutMapping("/{id}/approve")
    public Result<Map<String, Object>> approve(@AuthenticationPrincipal Long adminId,
                                               @PathVariable Long id,
                                               @RequestBody(required = false) ApproveRequest req) {
        Recipe r = recipeService.getById(id);
        if (r == null) return Result.error(404, "Recipe not found");
        if (adminId == null) return Result.error(401, "Unauthorized");
        if (r.getStatus() != Recipe.RecipeStatus.PENDING) {
            return Result.error(400, "当前菜谱不在待审核状态");
        }
        if (req != null && req.getCategory() != null && !req.getCategory().isBlank()) {
            try {
                r.setCategory(Recipe.Category.valueOf(req.getCategory()));
            } catch (IllegalArgumentException ignored) {}
        }
        Recipe.ReviewStage stage = r.getReviewStage() != null ? r.getReviewStage() : Recipe.ReviewStage.PENDING;
        if (stage == Recipe.ReviewStage.PENDING) {
            r.setReviewStage(Recipe.ReviewStage.FIRST_APPROVED);
            r.setFirstReviewerId(adminId);
            r.setFirstReviewTime(LocalDateTime.now());
            r.setRejectReason(null);
            recipeService.updateById(r);
            return Result.success(Map.of(
                    "recipe", r,
                    "message", "初审通过，待终审",
                    "stage", r.getReviewStage().name()
            ));
        }
        if (stage == Recipe.ReviewStage.FIRST_APPROVED) {
            if (r.getFirstReviewerId() != null && r.getFirstReviewerId().equals(adminId)) {
                return Result.error(400, "终审需由不同管理员完成");
            }
            r.setStatus(Recipe.RecipeStatus.APPROVED);
            r.setReviewStage(Recipe.ReviewStage.FINAL_APPROVED);
            r.setFinalReviewerId(adminId);
            r.setFinalReviewTime(LocalDateTime.now());
            r.setRejectReason(null);
            recipeService.updateById(r);
            return Result.success(Map.of(
                    "recipe", r,
                    "message", "终审通过，菜谱已发布",
                    "stage", r.getReviewStage().name()
            ));
        }
        if (stage == Recipe.ReviewStage.FINAL_APPROVED) {
            return Result.error(400, "该菜谱已完成终审");
        }
        if (stage == Recipe.ReviewStage.REJECTED) {
            return Result.error(400, "该菜谱已被拒绝");
        }
        recipeService.updateById(r);
        return Result.success(Map.of(
                "recipe", r,
                "message", "审核结果已更新",
                "stage", r.getReviewStage() != null ? r.getReviewStage().name() : "PENDING"
        ));
    }

    @PutMapping("/{id}/reject")
    public Result<Void> reject(@AuthenticationPrincipal Long adminId,
                               @PathVariable Long id,
                               @RequestBody RejectRequest req) {
        Recipe r = recipeService.getById(id);
        if (r == null) return Result.error(404, "Recipe not found");
        if (adminId == null) return Result.error(401, "Unauthorized");
        Recipe.ReviewStage prevStage = r.getReviewStage();
        r.setStatus(Recipe.RecipeStatus.REJECTED);
        r.setReviewStage(Recipe.ReviewStage.REJECTED);
        if (prevStage == Recipe.ReviewStage.FIRST_APPROVED) {
            r.setFinalReviewerId(adminId);
            r.setFinalReviewTime(LocalDateTime.now());
        } else if (prevStage == null || prevStage == Recipe.ReviewStage.PENDING) {
            r.setFirstReviewerId(adminId);
            r.setFirstReviewTime(LocalDateTime.now());
        }
        r.setRejectReason(req != null && req.getReason() != null ? req.getReason() : "审核未通过");
        recipeService.updateById(r);
        return Result.success(null);
    }

    /**
     * 管理员删除菜谱（逻辑删除）。提供 DELETE 与 POST 两种方式，避免部分网关/代理拦截 DELETE。
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@AuthenticationPrincipal Long adminId, @PathVariable Long id) {
        return adminDeleteRecipe(adminId, id);
    }

    @PostMapping("/{id}/delete")
    public Result<Void> deleteByPost(@AuthenticationPrincipal Long adminId, @PathVariable Long id) {
        return adminDeleteRecipe(adminId, id);
    }

    private Result<Void> adminDeleteRecipe(Long adminId, Long id) {
        if (adminId == null) {
            return Result.error(401, "Unauthorized");
        }
        Recipe recipe = recipeService.getById(id);
        if (recipe == null) {
            return Result.error(404, "菜谱不存在");
        }
        recipeService.removeRecipeLogicalAndRefreshCache(recipe);
        return Result.success(null);
    }

    @Data
    public static class ApproveRequest {
        private String category;
    }

    @Data
    public static class RejectRequest {
        private String reason;
    }
}
