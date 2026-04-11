package com.diet.controller;

import com.diet.common.Result;
import com.diet.service.IAdminService;
import com.diet.service.IAdminService.UserDetailVo;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "管理员接口")
public class AdminController {

    private final IAdminService adminService;

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        return Result.success(adminService.getDashboardStats());
    }

    @GetMapping("/dashboard/chart/user-growth")
    public Result<List<Map<String, Object>>> userGrowthChart(@RequestParam(defaultValue = "7") int days) {
        return Result.success(adminService.getUserGrowthChart(days));
    }

    @GetMapping("/dashboard/chart/popular-recipes")
    public Result<List<Map<String, Object>>> popularRecipes(@RequestParam(defaultValue = "10") int limit) {
        return Result.success(adminService.getPopularRecipes(limit));
    }

    @GetMapping("/dashboard/chart/target-distribution")
    public Result<Map<String, Long>> targetDistribution() {
        return Result.success(adminService.getTargetDistribution());
    }

    @GetMapping("/users")
    public Result<Map<String, Object>> listUsers(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String bmiRange,
            @RequestParam(required = false) String target) {
        var p = adminService.listUsers(page, size, bmiRange, target);
        return Result.success(Map.of(
                "records", p.getRecords(),
                "total", p.getTotal(),
                "pages", p.getPages(),
                "current", p.getCurrent()
        ));
    }

    @GetMapping("/users/{id}")
    public Result<UserDetailVo> getUserDetail(@PathVariable Long id) {
        UserDetailVo vo = adminService.getUserDetail(id);
        if (vo == null) {
            return Result.error(404, "User not found");
        }
        return Result.success(vo);
    }

    @PutMapping("/users/{id}/reset-password")
    public Result<Void> resetPassword(@AuthenticationPrincipal Long adminId,
                                      @PathVariable Long id,
                                      @RequestBody ResetPasswordRequest req) {
        if (adminId == null) return Result.error(401, "Unauthorized");
        if (req.getNewPassword() == null || req.getNewPassword().length() < 6) {
            return Result.error(400, "Password must be at least 6 characters");
        }
        try {
            adminService.resetUserPassword(adminId, id, req.getNewPassword());
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
        return Result.success(null);
    }

    @PutMapping("/users/{id}/disable")
    public Result<Void> disableUser(@AuthenticationPrincipal Long adminId, @PathVariable Long id) {
        if (adminId == null) return Result.error(401, "Unauthorized");
        try {
            adminService.disableUser(adminId, id);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
        return Result.success(null);
    }

    @PutMapping("/users/{id}/enable")
    public Result<Void> enableUser(@AuthenticationPrincipal Long adminId, @PathVariable Long id) {
        if (adminId == null) return Result.error(401, "Unauthorized");
        try {
            adminService.enableUser(adminId, id);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
        return Result.success(null);
    }

    @GetMapping("/statistics/retention")
    public Result<Map<String, Double>> retentionRates() {
        return Result.success(adminService.getRetentionRates());
    }

    @GetMapping("/statistics/plan-completion")
    public Result<Map<String, Object>> planCompletion(@RequestParam(defaultValue = "30") int days) {
        double rate = adminService.getPlanCompletionRate(days);
        return Result.success(Map.of("completionRate", rate, "days", days));
    }

    @GetMapping("/statistics/popular-allergies")
    public Result<List<Map<String, Object>>> popularAllergies(@RequestParam(defaultValue = "10") int limit) {
        return Result.success(adminService.getPopularAllergies(limit));
    }

    @Data
    public static class ResetPasswordRequest {
        private String newPassword;
    }
}
