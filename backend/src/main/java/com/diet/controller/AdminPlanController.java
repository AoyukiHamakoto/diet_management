package com.diet.controller;

import com.diet.common.Result;
import com.diet.service.AdminPlanService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/admin/plans")
@RequiredArgsConstructor
@Tag(name = "管理员计划审核接口")
public class AdminPlanController {

    private final AdminPlanService adminPlanService;

    @GetMapping
    public Result<Map<String, Object>> listPlans(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String auditStatus,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String planDate) {
        LocalDate date = (planDate != null && !planDate.isBlank()) ? LocalDate.parse(planDate) : null;
        return Result.success(adminPlanService.listPlans(page, size, auditStatus, userId, date));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> planDetail(@PathVariable Long id) {
        Map<String, Object> detail = adminPlanService.getPlanDetail(id);
        if (detail == null) {
            return Result.error(404, "Plan not found");
        }
        return Result.success(detail);
    }

    @PostMapping("/{id}/approve")
    public Result<Void> approve(
            @PathVariable Long id,
            @AuthenticationPrincipal Long adminId) {
        if (adminId == null) return Result.error(401, "Unauthorized");
        try {
            adminPlanService.approvePlan(id, adminId);
        } catch (IllegalArgumentException e) {
            return Result.error(404, e.getMessage());
        } catch (IllegalStateException e) {
            return Result.error(400, e.getMessage());
        }
        return Result.success();
    }

    @PostMapping("/{id}/reject")
    public Result<Void> reject(
            @PathVariable Long id,
            @AuthenticationPrincipal Long adminId,
            @RequestBody RejectRequest request) {
        if (adminId == null) return Result.error(401, "Unauthorized");
        try {
            adminPlanService.rejectPlan(id, adminId, request != null ? request.getReason() : null);
        } catch (IllegalArgumentException e) {
            return Result.error(404, e.getMessage());
        } catch (IllegalStateException e) {
            return Result.error(400, e.getMessage());
        }
        return Result.success();
    }

    @Data
    public static class RejectRequest {
        private String reason;
    }
}
