package com.diet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diet.entity.*;
import com.diet.mapper.*;
import com.diet.service.IAdminService;
import com.diet.service.IHealthProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements IAdminService {

    private final UserMapper userMapper;
    private final MealPlanMapper mealPlanMapper;
    private final RecipeMapper recipeMapper;
    private final FeedbackMapper feedbackMapper;
    private final HealthProfileMapper healthProfileMapper;
    private final IHealthProfileService healthProfileService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Map<String, Object> getDashboardStats() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();

        // Today active users: users who have plan for today or generated plan today
        var distinctUsers = mealPlanMapper.selectList(
                new QueryWrapper<MealPlan>().select("user_id").apply("plan_date = {0}", today)
        );
        Set<Long> userIds = distinctUsers.stream().map(MealPlan::getUserId).collect(Collectors.toSet());
        long todayPlans = mealPlanMapper.selectCount(
                new QueryWrapper<MealPlan>().ge("create_time", todayStart)
        );
        long pendingRecipes = recipeMapper.selectCount(
                new QueryWrapper<Recipe>().eq("status", Recipe.RecipeStatus.PENDING)
        );

        var allFeedbacks = feedbackMapper.selectList(new QueryWrapper<Feedback>().select("rating"));
        double avgRating = allFeedbacks.stream()
                .filter(Objects::nonNull)
                .map(Feedback::getRating)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);

        return Map.of(
                "todayActiveUsers", userIds.size(),
                "todayPlansGenerated", todayPlans,
                "pendingRecipes", pendingRecipes,
                "avgFeedbackRating", BigDecimal.valueOf(avgRating).setScale(1, RoundingMode.HALF_UP)
        );
    }

    @Override
    public List<Map<String, Object>> getUserGrowthChart(int days) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1);
        List<Map<String, Object>> result = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            LocalDateTime dayStart = d.atStartOfDay();
            LocalDateTime dayEnd = d.plusDays(1).atStartOfDay();
            Long count = userMapper.selectCount(
                    new QueryWrapper<User>()
                            .ge("create_time", dayStart)
                            .lt("create_time", dayEnd)
            );
            result.add(Map.of(
                    "date", d.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    "count", count != null ? count : 0
            ));
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getPopularRecipes(int limit) {
        var grouped = mealPlanMapper.selectList(
                new QueryWrapper<MealPlan>().isNotNull("recipe_id")
        ).stream().collect(Collectors.groupingBy(MealPlan::getRecipeId, Collectors.counting()));
        return grouped.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .map(e -> {
                    Recipe r = recipeMapper.selectById(e.getKey());
                    return Map.<String, Object>of(
                            "recipeId", e.getKey(),
                            "title", r != null ? r.getTitle() : "未知",
                            "count", e.getValue()
                    );
                })
                .toList();
    }

    @Override
    public Map<String, Long> getTargetDistribution() {
        var list = healthProfileMapper.selectList(
                new QueryWrapper<HealthProfile>().select("target")
        );
        Map<String, Long> dist = new HashMap<>();
        for (HealthProfile p : list) {
            if (p == null) continue;
            String t = p.getTarget() != null ? p.getTarget().name() : "UNKNOWN";
            dist.merge(t, 1L, Long::sum);
        }
        return dist;
    }

    @Override
    public IPage<UserListItem> listUsers(Long page, Long size, String bmiRange, String target) {
        Page<User> userPage = new Page<>(page != null ? page : 1, size != null ? size : 10);
        QueryWrapper<User> qw = new QueryWrapper<>();
        qw.eq("role", User.Role.USER.name());

        Set<Long> filterUserIds = null;
        if (target != null && !target.isBlank() || (bmiRange != null && !bmiRange.isBlank())) {
            var allHp = healthProfileMapper.selectList(new QueryWrapper<HealthProfile>().select("user_id", "bmi", "target"));
            filterUserIds = new HashSet<>();
            for (HealthProfile hp : allHp) {
                if (hp == null) continue;
                if (target != null && !target.isBlank() && (hp.getTarget() == null || !hp.getTarget().name().equals(target)))
                    continue;
                if (bmiRange != null && !bmiRange.isBlank()) {
                    String r = bmiToRange(hp.getBmi());
                    if (!bmiRange.equals(r)) continue;
                }
                filterUserIds.add(hp.getUserId());
            }
            if (filterUserIds.isEmpty()) {
                return new Page<UserListItem>(userPage.getCurrent(), userPage.getSize(), 0)
                        .setRecords(List.of());
            }
            qw.in("id", filterUserIds);
        }

        IPage<User> up = userMapper.selectPage(userPage, qw);
        List<UserListItem> items = new ArrayList<>();
        for (User u : up.getRecords()) {
            HealthProfile hp = healthProfileService.lambdaQuery()
                    .eq(HealthProfile::getUserId, u.getId()).one();
            String bmiR = hp != null && hp.getBmi() != null ? bmiToRange(hp.getBmi()) : "-";
            String phoneMasked = maskPhone(u.getPhone());
            items.add(new UserListItem(
                    u.getId(),
                    phoneMasked,
                    u.getNickname() != null ? u.getNickname() : "-",
                    u.getCreateTime() != null ? u.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "-",
                    bmiR,
                    hp != null && hp.getTarget() != null ? hp.getTarget().name() : "-",
                    u.getEnabled() == null ? true : u.getEnabled()
            ));
        }
        Page<UserListItem> result = new Page<>(up.getCurrent(), up.getSize(), up.getTotal());
        result.setRecords(items);
        return result;
    }

    @Override
    public UserDetailVo getUserDetail(Long userId) {
        User u = userMapper.selectById(userId);
        if (u == null) return null;
        HealthProfile hp = healthProfileService.lambdaQuery()
                .eq(HealthProfile::getUserId, userId).one();
        return new UserDetailVo(
                u.getId(),
                maskPhone(u.getPhone()),
                u.getNickname(),
                u.getAvatar(),
                u.getRole() != null ? u.getRole().name() : "USER",
                u.getEnabled() == null ? true : u.getEnabled(),
                u.getCreateTime() != null ? u.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null,
                hp
        );
    }

    @Override
    public void resetUserPassword(Long operatorAdminId, Long userId, String newPassword) {
        if (operatorAdminId != null && operatorAdminId.equals(userId)) {
            throw new IllegalArgumentException("不能对当前管理员账号执行此操作");
        }
        User u = userMapper.selectById(userId);
        if (u == null) throw new IllegalArgumentException("User not found");
        u.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(u);
    }

    @Override
    public void disableUser(Long operatorAdminId, Long userId) {
        if (operatorAdminId != null && operatorAdminId.equals(userId)) {
            throw new IllegalArgumentException("不能禁用当前管理员账号");
        }
        User u = userMapper.selectById(userId);
        if (u == null) throw new IllegalArgumentException("User not found");
        if (u.getRole() == User.Role.ADMIN) {
            throw new IllegalArgumentException("管理员账号不可禁用");
        }
        u.setEnabled(false);
        userMapper.updateById(u);
    }

    @Override
    public void enableUser(Long operatorAdminId, Long userId) {
        if (operatorAdminId != null && operatorAdminId.equals(userId)) {
            throw new IllegalArgumentException("不能操作当前管理员账号");
        }
        User u = userMapper.selectById(userId);
        if (u == null) throw new IllegalArgumentException("User not found");
        u.setEnabled(true);
        userMapper.updateById(u);
    }

    @Override
    public Map<String, Double> getRetentionRates() {
        var users = userMapper.selectList(new QueryWrapper<User>().select("id", "create_time"));
        if (users.isEmpty()) return Map.of("nextDay", 0.0, "day7", 0.0, "day30", 0.0);

        LocalDate now = LocalDate.now();
        long baseNext = 0, base7 = 0, base30 = 0;
        long retNext = 0, ret7 = 0, ret30 = 0;
        for (User u : users) {
            if (u.getCreateTime() == null) continue;
            LocalDate regDate = u.getCreateTime().toLocalDate();
            long daysSince = java.time.temporal.ChronoUnit.DAYS.between(regDate, now);
            long planCount = mealPlanMapper.selectCount(new QueryWrapper<MealPlan>().eq("user_id", u.getId()));
            boolean hasPlan = planCount > 0;
            if (daysSince >= 1) { baseNext++; if (hasPlan) retNext++; }
            if (daysSince >= 7) { base7++; if (hasPlan) ret7++; }
            if (daysSince >= 30) { base30++; if (hasPlan) ret30++; }
        }
        return Map.of(
                "nextDay", baseNext > 0 ? retNext * 100.0 / baseNext : 0.0,
                "day7", base7 > 0 ? ret7 * 100.0 / base7 : 0.0,
                "day30", base30 > 0 ? ret30 * 100.0 / base30 : 0.0
        );
    }

    @Override
    public double getPlanCompletionRate(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        var plans = mealPlanMapper.selectList(
                new QueryWrapper<MealPlan>().ge("create_time", since)
        );
        if (plans.isEmpty()) return 0;
        long completed = plans.stream().filter(p -> p.getStatus() == MealPlan.PlanStatus.COMPLETED).count();
        return plans.size() > 0 ? completed * 100.0 / plans.size() : 0;
    }

    @Override
    public List<Map<String, Object>> getPopularAllergies(int limit) {
        var list = healthProfileMapper.selectList(
                new QueryWrapper<HealthProfile>().select("allergy_tags").isNotNull("allergy_tags")
        );
        Map<String, Long> count = new HashMap<>();
        for (HealthProfile hp : list) {
            if (hp == null) continue;
            if (hp.getAllergyTags() != null) {
                for (String tag : hp.getAllergyTags()) {
                    if (tag != null && !tag.isBlank()) {
                        count.merge(tag.trim(), 1L, Long::sum);
                    }
                }
            }
        }
        return count.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .map(e -> Map.<String, Object>of("tag", e.getKey(), "count", e.getValue()))
                .toList();
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return "***";
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String bmiToRange(BigDecimal bmi) {
        if (bmi == null) return null;
        double v = bmi.doubleValue();
        if (v < 18.5) return "偏瘦";
        if (v < 24) return "正常";
        if (v < 28) return "偏胖";
        return "肥胖";
    }
}
