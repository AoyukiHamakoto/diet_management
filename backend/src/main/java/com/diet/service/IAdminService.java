package com.diet.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.diet.entity.HealthProfile;

import java.util.List;
import java.util.Map;

public interface IAdminService {

    /** Dashboard stats: today active users, plans generated, pending recipes, avg feedback rating */
    Map<String, Object> getDashboardStats();

    /** User growth in last 7 days */
    List<Map<String, Object>> getUserGrowthChart(int days);

    /** Top 10 popular recipes (by meal_plan count) */
    List<Map<String, Object>> getPopularRecipes(int limit);

    /** User target distribution pie chart */
    Map<String, Long> getTargetDistribution();

    /** List users with filters */
    IPage<UserListItem> listUsers(Long page, Long size, String bmiRange, String target);

    /** Get user detail with health profile (phone masked) */
    UserDetailVo getUserDetail(Long userId);

    /** Reset user password */
    void resetUserPassword(Long operatorAdminId, Long userId, String newPassword);

    /** Disable user account */
    void disableUser(Long operatorAdminId, Long userId);

    /** Enable user account */
    void enableUser(Long operatorAdminId, Long userId);

    /** Retention: next-day, 7-day, 30-day */
    Map<String, Double> getRetentionRates();

    /** Plan completion rate */
    double getPlanCompletionRate(int days);

    /** Popular allergy tags count */
    List<Map<String, Object>> getPopularAllergies(int limit);

    record UserListItem(Long id, String phoneMasked, String nickname, String createTime,
                        String bmiRange, String target, Boolean enabled) {}

    record UserDetailVo(Long id, String phoneMasked, String nickname, String avatar, String role,
                        Boolean enabled, String createTime, HealthProfile healthProfile) {}
}
