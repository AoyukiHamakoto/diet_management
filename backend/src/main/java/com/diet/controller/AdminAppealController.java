package com.diet.controller;

import com.diet.common.Result;
import com.diet.entity.Appeal;
import com.diet.entity.MealPlan;
import com.diet.entity.Post;
import com.diet.entity.Recipe;
import com.diet.mapper.PostMapper;
import com.diet.service.IAppealService;
import com.diet.service.IMealPlanService;
import com.diet.service.IRecipeService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/appeals")
@RequiredArgsConstructor
public class AdminAppealController {

    private final IAppealService appealService;
    private final IMealPlanService mealPlanService;
    private final IRecipeService recipeService;
    private final PostMapper postMapper;

    @GetMapping
    public Result<Map<String, Object>> list(@RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        var q = appealService.lambdaQuery().orderByDesc(Appeal::getCreateTime);
        if (status != null && !status.isBlank()) {
            try {
                q.eq(Appeal::getStatus, Appeal.Status.valueOf(status));
            } catch (IllegalArgumentException ignored) {
            }
        }
        var p = q.page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size));
        List<Map<String, Object>> records = p.getRecords().stream()
                .map(this::toAdminAppealMap)
                .collect(Collectors.toList());
        return Result.success(Map.of(
                "records", records,
                "total", p.getTotal(),
                "pages", p.getPages(),
                "current", p.getCurrent()));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> getDetail(@PathVariable Long id) {
        Appeal a = appealService.getById(id);
        if (a == null)
            return Result.error(404, "申诉不存在");
        Map<String, Object> detail = toAdminAppealMap(a);
        detail.put("adminComment", a.getAdminComment());
        detail.put("adminId", a.getAdminId());
        detail.put("updateTime", a.getUpdateTime() != null ? a.getUpdateTime().toString() : null);
        if (a.getTargetType() == Appeal.TargetType.MEAL_PLAN) {
            MealPlan plan = mealPlanService.getById(a.getTargetId());
            if (plan != null) {
                Map<String, Object> targetDetail = new HashMap<>();
                targetDetail.put("planDate", plan.getPlanDate() != null ? plan.getPlanDate().toString() : null);
                targetDetail.put("mealType", plan.getMealType() != null ? plan.getMealType().name() : null);
                targetDetail.put("auditStatus", plan.getAuditStatus() != null ? plan.getAuditStatus().name() : null);
                targetDetail.put("auditComment", plan.getAuditComment());
                detail.put("targetDetail", targetDetail);
            }
        } else if (a.getTargetType() == Appeal.TargetType.RECIPE) {
            Recipe recipe = recipeService.getById(a.getTargetId());
            if (recipe != null) {
                Map<String, Object> targetDetail = new HashMap<>();
                targetDetail.put("title", recipe.getTitle());
                targetDetail.put("status", recipe.getStatus() != null ? recipe.getStatus().name() : null);
                targetDetail.put("rejectReason", recipe.getRejectReason());
                detail.put("targetDetail", targetDetail);
            }
        } else if (a.getTargetType() == Appeal.TargetType.POST) {
            Post post = postMapper.selectById(a.getTargetId());
            if (post != null) {
                Map<String, Object> targetDetail = new HashMap<>();
                targetDetail.put("title", post.getTitle());
                targetDetail.put("status", post.getStatus());
                targetDetail.put("rejectReason", post.getRejectReason());
                detail.put("targetDetail", targetDetail);
            }
        }
        return Result.success(detail);
    }

    @PostMapping("/{id}/resolve")
    public Result<Void> resolve(@AuthenticationPrincipal Long adminId,
            @PathVariable Long id,
            @RequestBody(required = false) ResolveAppealRequest request) {
        try {
            appealService.resolve(id, adminId, request != null ? request.getAdminComment() : null);
            return Result.success(null);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @PostMapping("/{id}/reject")
    public Result<Void> reject(@AuthenticationPrincipal Long adminId,
            @PathVariable Long id,
            @RequestBody RejectAppealRequest request) {
        if (request == null || request.getAdminComment() == null || request.getAdminComment().isBlank()) {
            return Result.error(400, "拒绝申诉必须填写处理意见");
        }
        try {
            appealService.reject(id, adminId, request.getAdminComment());
            return Result.success(null);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    private Map<String, Object> toAdminAppealMap(Appeal a) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", a.getId());
        m.put("userId", a.getUserId());
        m.put("targetType", a.getTargetType() != null ? a.getTargetType().name() : null);
        m.put("targetId", a.getTargetId());
        m.put("reason", a.getReason());
        m.put("status", a.getStatus() != null ? a.getStatus().name() : null);
        m.put("createTime", a.getCreateTime() != null ? a.getCreateTime().toString() : null);
        if (a.getTargetType() == Appeal.TargetType.MEAL_PLAN) {
            MealPlan plan = mealPlanService.getById(a.getTargetId());
            if (plan != null)
                m.put("targetSummary", "计划 " + plan.getPlanDate() + " "
                        + (plan.getMealType() != null ? plan.getMealType().name() : ""));
        } else if (a.getTargetType() == Appeal.TargetType.RECIPE) {
            Recipe recipe = recipeService.getById(a.getTargetId());
            if (recipe != null)
                m.put("targetSummary", "菜谱：" + recipe.getTitle());
        } else if (a.getTargetType() == Appeal.TargetType.POST) {
            Post post = postMapper.selectById(a.getTargetId());
            if (post != null)
                m.put("targetSummary", "帖子：" + post.getTitle());
        }
        return m;
    }

    @Data
    public static class ResolveAppealRequest {
        private String adminComment;
    }

    @Data
    public static class RejectAppealRequest {
        private String adminComment;
    }
}
