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
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/appeals")
@RequiredArgsConstructor
public class AppealController {

    private final IAppealService appealService;
    private final IMealPlanService mealPlanService;
    private final IRecipeService recipeService;
    private final PostMapper postMapper;

    @PostMapping
    public Result<Map<String, Object>> submit(@AuthenticationPrincipal Long userId,
            @Valid @RequestBody AppealSubmitRequest request) {
        if (userId == null)
            return Result.error(401, "Unauthorized");
        try {
            Appeal a = appealService.submit(userId, request.getTargetType(), request.getTargetId(),
                    request.getReason());
            return Result.success(toAppealMap(a, null));
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @GetMapping("/my")
    public Result<Map<String, Object>> myAppeals(@AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (userId == null)
            return Result.error(401, "Unauthorized");
        var p = appealService.lambdaQuery()
                .eq(Appeal::getUserId, userId)
                .orderByDesc(Appeal::getCreateTime)
                .page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size));
        List<Map<String, Object>> records = p.getRecords().stream()
                .map(a -> toAppealMap(a, resolveTargetSummary(a)))
                .collect(Collectors.toList());
        return Result.success(Map.of(
                "records", records,
                "total", p.getTotal(),
                "pages", p.getPages(),
                "current", p.getCurrent()));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> getDetail(@AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        if (userId == null)
            return Result.error(401, "Unauthorized");
        Appeal a = appealService.getById(id);
        if (a == null)
            return Result.error(404, "申诉不存在");
        if (!a.getUserId().equals(userId)) {
            return Result.error(403, "无权查看该申诉");
        }
        Map<String, Object> detail = toAppealMap(a, resolveTargetSummary(a));
        return Result.success(detail);
    }

    private Map<String, Object> toAppealMap(Appeal a, Map<String, Object> targetSummary) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", a.getId());
        m.put("userId", a.getUserId());
        m.put("targetType", a.getTargetType() != null ? a.getTargetType().name() : null);
        m.put("targetId", a.getTargetId());
        m.put("reason", a.getReason());
        m.put("status", a.getStatus() != null ? a.getStatus().name() : null);
        m.put("adminId", a.getAdminId());
        m.put("adminComment", a.getAdminComment());
        m.put("createTime", a.getCreateTime() != null ? a.getCreateTime().toString() : null);
        m.put("updateTime", a.getUpdateTime() != null ? a.getUpdateTime().toString() : null);
        if (targetSummary != null)
            m.put("targetSummary", targetSummary);
        return m;
    }

    private Map<String, Object> resolveTargetSummary(Appeal a) {
        Map<String, Object> sum = new HashMap<>();
        if (a.getTargetType() == Appeal.TargetType.MEAL_PLAN) {
            MealPlan plan = mealPlanService.getById(a.getTargetId());
            if (plan != null) {
                sum.put("planDate", plan.getPlanDate() != null ? plan.getPlanDate().toString() : null);
                sum.put("mealType", plan.getMealType() != null ? plan.getMealType().name() : null);
                sum.put("auditComment", plan.getAuditComment());
            }
        } else if (a.getTargetType() == Appeal.TargetType.RECIPE) {
            Recipe recipe = recipeService.getById(a.getTargetId());
            if (recipe != null) {
                sum.put("title", recipe.getTitle());
                sum.put("rejectReason", recipe.getRejectReason());
            }
        } else if (a.getTargetType() == Appeal.TargetType.POST) {
            Post post = postMapper.selectById(a.getTargetId());
            if (post != null) {
                sum.put("title", post.getTitle());
                sum.put("rejectReason", post.getRejectReason());
            }
        }
        return sum;
    }

    @Data
    public static class AppealSubmitRequest {
        @NotBlank(message = "targetType 必填")
        private String targetType; // MEAL_PLAN | RECIPE | POST
        private Long targetId;
        @NotBlank(message = "申诉理由必填")
        @Size(min = 10, max = 500, message = "申诉理由 10-500 字")
        private String reason;
    }
}
