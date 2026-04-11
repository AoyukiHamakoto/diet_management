package com.diet.controller;

import com.diet.common.Result;
import com.diet.entity.BodyLog;
import com.diet.entity.DietTag;
import com.diet.entity.HealthProfile;
import com.diet.entity.User;
import com.diet.service.DietTagRuleService;
import com.diet.service.HealthProfileCalcService;
import com.diet.service.IBodyLogService;
import com.diet.service.IDietTagService;
import com.diet.service.IHealthProfileService;
import com.diet.service.IUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private static final String BODY_LOG_RATE_KEY = "body_log:rate:";
    private static final int BODY_LOG_MAX_PER_HOUR = 3;
    private static final long RATE_LIMIT_TTL_SECONDS = 3600;

    private final IHealthProfileService healthProfileService;
    private final IBodyLogService bodyLogService;
    private final IDietTagService dietTagService;
    private final HealthProfileCalcService calcService;
    private final DietTagRuleService dietTagRuleService;
    private final com.diet.service.RedisService redisService;
    private final com.diet.service.IUserPreferenceService preferenceService;
    private final com.diet.service.ISystemNotificationService notificationService;
    private final IUserService userService;
    private final PasswordEncoder passwordEncoder;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    private static final int NICKNAME_MIN = 2;
    private static final int NICKNAME_MAX = 20;
    private static final String NICKNAME_PATTERN = "^[\\u4e00-\\u9fa5a-zA-Z0-9]+$";
    private static final long AVATAR_MAX_BYTES = 2 * 1024 * 1024;
    private static final java.util.regex.Pattern STRONG_PASSWORD_PATTERN =
            java.util.regex.Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,64}$");

    @GetMapping("/profile")
    public Result<UserProfileDTO> getProfile(@AuthenticationPrincipal Long userId) {
        if (userId == null) return Result.error(401, "Unauthorized");
        User user = userService.getById(userId);
        if (user == null) return Result.error(404, "用户不存在");
        return Result.success(toProfileDTO(user));
    }

    @PutMapping("/profile")
    public Result<UserProfileDTO> updateProfile(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        if (userId == null) return Result.error(401, "Unauthorized");
        User user = userService.getById(userId);
        if (user == null) return Result.error(404, "用户不存在");
        if (request.getNickname() != null && !request.getNickname().isBlank()) {
            String n = request.getNickname().trim();
            if (n.length() < NICKNAME_MIN || n.length() > NICKNAME_MAX) {
                return Result.error(400, "昵称长度为2-20个字符");
            }
            if (!n.matches(NICKNAME_PATTERN)) {
                return Result.error(400, "昵称仅支持中文、英文、数字");
            }
            user.setNickname(n);
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }
        userService.updateById(user);
        return Result.success(toProfileDTO(userService.getById(userId)));
    }

    @PutMapping("/password")
    public Result<Void> updatePassword(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdatePasswordRequest request) {
        if (userId == null) return Result.error(401, "Unauthorized");
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return Result.error(400, "两次输入的密码不一致");
        }
        if (request.getOldPassword().equals(request.getNewPassword())) {
            return Result.error(400, "新密码不能与旧密码相同");
        }
        if (!STRONG_PASSWORD_PATTERN.matcher(request.getNewPassword()).matches()) {
            return Result.error(400, "新密码需包含大小写字母、数字和特殊字符，且长度至少8位");
        }
        User user = userService.getById(userId);
        if (user == null) return Result.error(404, "用户不存在");
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            return Result.error(400, "当前密码不正确");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userService.updateById(user);
        return Result.success(null);
    }

    @PostMapping("/avatar")
    public Result<Map<String, String>> uploadAvatar(
            @AuthenticationPrincipal Long userId,
            @RequestParam("file") MultipartFile file) {
        if (userId == null) return Result.error(401, "Unauthorized");
        if (file == null || file.isEmpty()) return Result.error(400, "请选择图片");
        String name = file.getOriginalFilename();
        if (name == null || !name.contains(".")) return Result.error(400, "不支持的文件格式");
        String ext = name.substring(name.lastIndexOf(".")).toLowerCase();
        if (!".jpg".equals(ext) && !".jpeg".equals(ext) && !".png".equals(ext)) {
            return Result.error(400, "仅支持 jpg、png 格式");
        }
        if (file.getSize() > AVATAR_MAX_BYTES) return Result.error(400, "图片大小不能超过2MB");
        String filename = "avatar/" + UUID.randomUUID() + ext;
        try {
            Path basePath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path dir = basePath.resolve("avatar");
            Files.createDirectories(dir);
            Path target = dir.resolve(filename.substring("avatar/".length()));
            file.transferTo(target.toFile());
            String avatarUrl = "/api/uploads/" + filename;
            return Result.success(Map.of("avatarUrl", avatarUrl));
        } catch (IOException e) {
            return Result.error(500, "上传失败: " + e.getMessage());
        }
    }

    private UserProfileDTO toProfileDTO(User user) {
        UserProfileDTO dto = new UserProfileDTO();
        dto.setId(user.getId());
        dto.setPhone(maskPhone(user.getPhone()));
        dto.setNickname(user.getNickname() != null ? user.getNickname() : "");
        dto.setAvatar(user.getAvatar() != null ? user.getAvatar() : "");
        dto.setCreatedAt(user.getCreateTime() != null
                ? user.getCreateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
        return dto;
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 11) return phone != null ? phone : "";
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    @Data
    public static class UserProfileDTO {
        private Long id;
        private String phone;
        private String nickname;
        private String avatar;
        private String createdAt;
    }

    @Data
    public static class UpdateProfileRequest {
        @Size(max = 20, message = "昵称最多20个字符")
        @Pattern(regexp = "^[\\u4e00-\\u9fa5a-zA-Z0-9]*$", message = "昵称仅支持中文、英文、数字")
        private String nickname;
        private String avatar;
    }

    @Data
    public static class ClearHealthProfileRequest {
        private Boolean confirm;
    }

    @Data
    public static class UpdatePasswordRequest {
        @NotBlank(message = "当前密码不能为空")
        private String oldPassword;
        @NotBlank(message = "新密码不能为空")
        @Size(min = 8, max = 64, message = "新密码长度为8-64个字符")
        private String newPassword;
        @NotBlank(message = "确认密码不能为空")
        private String confirmPassword;
    }

    @GetMapping("/health-profile")
    public Result<HealthProfileResponse> getHealthProfile(@AuthenticationPrincipal Long userId) {
        log.info("[业务-健康档案] 查询档案, userId={}", userId);
        if (userId == null) {
            return Result.error(401, "Unauthorized");
        }
        HealthProfile profile = healthProfileService.lambdaQuery()
                .eq(HealthProfile::getUserId, userId)
                .one();
        if (profile == null) {
            log.info("[业务-健康档案] 无档案, userId={}", userId);
            return Result.success(new HealthProfileResponse());
        }
        String bmiRating = calcService.getBmiRating(profile.getBmi());
        log.info("[业务-健康档案] 查询成功, userId={}, bmi={}, tdee={}", userId, profile.getBmi(), profile.getTdee());
        return Result.success(new HealthProfileResponse(profile, bmiRating));
    }

    @PutMapping("/health-profile")
    public Result<HealthProfileResponse> updateHealthProfile(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody HealthProfileUpdateRequest request) {
        log.info("[业务-健康档案] 更新档案, userId={}, height={}, weight={}, target={}", userId, request.getHeight(), request.getWeight(), request.getTarget());
        if (userId == null) {
            return Result.error(401, "Unauthorized");
        }
        // Validation (only when provided)
        if (request.getHeight() != null) validateHeight(request.getHeight());
        if (request.getWeight() != null) validateWeight(request.getWeight());
        if (request.getTarget() != null && !isValidTarget(request.getTarget())) {
            log.warn("[业务-健康档案] 目标值非法, target={}", request.getTarget());
            return Result.error(400, "Invalid target value");
        }

        HealthProfile profile = healthProfileService.lambdaQuery()
                .eq(HealthProfile::getUserId, userId)
                .one();
        if (profile == null) {
            profile = new HealthProfile();
            profile.setUserId(userId);
            log.info("[业务-健康档案] 新建档案, userId={}", userId);
        }

        if (request.getHeight() != null) profile.setHeight(request.getHeight());
        if (request.getWeight() != null) profile.setWeight(request.getWeight());
        if (request.getGender() != null) profile.setGender(HealthProfile.Gender.valueOf(request.getGender()));
        if (request.getAge() != null) profile.setAge(request.getAge());
        if (request.getTarget() != null) profile.setTarget(HealthProfile.Target.valueOf(request.getTarget()));
        if (request.getExerciseFrequency() != null) profile.setExerciseFrequency(HealthProfile.ExerciseFrequency.valueOf(request.getExerciseFrequency()));
        if (request.getExerciseTime() != null) profile.setExerciseTime(HealthProfile.ExerciseTime.valueOf(request.getExerciseTime()));
        if (request.getAllergyTags() != null) profile.setAllergyTags(normalizeAllergies(request.getAllergyTags()));
        if (profile.getTarget() == null) {
            return Result.error(400, "运动目标必填");
        }
        if (profile.getAllergyTags() == null || profile.getAllergyTags().isEmpty()) {
            profile.setAllergyTags(List.of("无"));
        }

        // Recalculate BMI and TDEE
        BigDecimal bmi = calcService.calculateBmi(profile.getHeight(), profile.getWeight());
        profile.setBmi(bmi);
        BigDecimal tdee = calcService.calculateTdee(
                profile.getWeight(), profile.getHeight(), profile.getAge(),
                profile.getGender(), profile.getExerciseFrequency());
        profile.setTdee(tdee);
        log.info("[业务-健康档案] 重算BMI/TDEE, bmi={}, tdee={}", bmi, tdee);

        healthProfileService.saveOrUpdate(profile);
        log.info("[业务-健康档案] 档案已持久化, profileId={}", profile.getId());

        // Regenerate diet tags via Drools rules
        dietTagService.lambdaUpdate().eq(DietTag::getUserId, userId).remove();
        java.util.List<DietTag> generatedTags = dietTagRuleService.generateDietTags(profile);
        if (!generatedTags.isEmpty()) {
            dietTagService.saveBatch(generatedTags);
            log.info("[业务-健康档案] 饮食标签已再生, count={}", generatedTags.size());
        }

        String bmiRating = calcService.getBmiRating(profile.getBmi());
        log.info("[业务-健康档案] 更新完成, userId={}", userId);
        return Result.success(new HealthProfileResponse(profile, bmiRating));
    }

    @PostMapping("/health-profile/clear")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> clearHealthProfile(
            @AuthenticationPrincipal Long userId,
            @RequestBody(required = false) ClearHealthProfileRequest request) {
        if (userId == null) return Result.error(401, "Unauthorized");
        if (request == null || !Boolean.TRUE.equals(request.getConfirm())) {
            return Result.error(400, "请确认清空操作");
        }
        HealthProfile profile = healthProfileService.lambdaQuery()
                .eq(HealthProfile::getUserId, userId)
                .one();
        if (profile != null) {
            profile.setHeight(null);
            profile.setWeight(null);
            profile.setBmi(null);
            profile.setAge(null);
            profile.setGender(null);
            profile.setTarget(null);
            profile.setExerciseFrequency(null);
            profile.setExerciseTime(null);
            profile.setAllergyTags(Collections.emptyList());
            profile.setTdee(null);
            healthProfileService.updateById(profile);
        }
        dietTagService.lambdaUpdate().eq(DietTag::getUserId, userId).remove();
        preferenceService.reset(userId);
        return Result.success(null);
    }

    @PostMapping("/body-log")
    public Result<BodyLogResponse> createBodyLog(
            @AuthenticationPrincipal Long userId,
            @RequestBody Map<String, Object> request) {
        log.info("[业务-体重日志] 提交体重, userId={}", userId);
        if (userId == null) {
            return Result.error(401, "Unauthorized");
        }
        BigDecimal weight = parseWeight(request.get("weight"));
        if (weight == null) {
            return Result.error(400, "体重不能为空");
        }
        validateWeight(weight);

        // Rate limit: max 3 per hour
        String rateKey = BODY_LOG_RATE_KEY + userId;
        Object countObj = redisService.get(rateKey);
        int count = (countObj instanceof Number) ? ((Number) countObj).intValue() : 0;
        if (count >= BODY_LOG_MAX_PER_HOUR) {
            return Result.error(429, "体重更新过于频繁，请1小时后再试");
        }

        LocalDate logDate = parseLogDate(request.get("logDate"));

        BodyLog existing = bodyLogService.lambdaQuery()
                .eq(BodyLog::getUserId, userId)
                .eq(BodyLog::getLogDate, logDate)
                .one();
        if (existing != null) {
            existing.setWeight(weight);
            bodyLogService.updateById(existing);
        } else {
            BodyLog log = new BodyLog();
            log.setUserId(userId);
            log.setWeight(weight);
            log.setLogDate(logDate);
            bodyLogService.save(log);
        }

        // Update health_profile weight and recalc
        HealthProfile profile = healthProfileService.lambdaQuery()
                .eq(HealthProfile::getUserId, userId)
                .one();
        if (profile != null) {
            profile.setWeight(weight);
            profile.setBmi(calcService.calculateBmi(profile.getHeight(), weight));
            if (profile.getHeight() != null && profile.getAge() != null && profile.getGender() != null && profile.getExerciseFrequency() != null) {
                profile.setTdee(calcService.calculateTdee(
                        weight, profile.getHeight(), profile.getAge(),
                        profile.getGender(), profile.getExerciseFrequency()));
            }
            healthProfileService.updateById(profile);
        }

        // Increment rate limit
        redisService.set(rateKey, count + 1, RATE_LIMIT_TTL_SECONDS, java.util.concurrent.TimeUnit.SECONDS);

        log.info("[业务-体重日志] 保存成功, userId={}, logDate={}, weight={}", userId, logDate, weight);
        return Result.success(new BodyLogResponse(logDate, weight));
    }

    @GetMapping("/diet-tags")
    public Result<List<DietTagResponse>> getDietTags(@AuthenticationPrincipal Long userId) {
        log.info("[业务-饮食标签] 查询, userId={}", userId);
        if (userId == null) {
            return Result.error(401, "Unauthorized");
        }
        var tags = dietTagService.lambdaQuery()
                .eq(DietTag::getUserId, userId)
                .orderByDesc(DietTag::getConfidenceScore)
                .list();
        var result = tags.stream()
                .map(t -> new DietTagResponse(t.getTagName(), t.getConfidenceScore(), t.getSource()))
                .toList();
        log.info("[业务-饮食标签] 返回{}条, userId={}", result.size(), userId);
        return Result.success(result);
    }

    @GetMapping("/preferences")
    public Result<Map<String, Object>> getPreferences(@AuthenticationPrincipal Long userId) {
        if (userId == null) return Result.error(401, "Unauthorized");
        var pref = preferenceService.getOrCreate(userId);
        var tags = dietTagService.lambdaQuery().eq(DietTag::getUserId, userId)
                .orderByDesc(DietTag::getConfidenceScore).list();
        var tagList = tags.stream()
                .map(t -> Map.<String, Object>of(
                        "tag", t.getTagName(),
                        "confidence", t.getConfidenceScore() != null ? (int)(t.getConfidenceScore().doubleValue() * 100) : 0,
                        "weight", pref.getTagWeights() != null && pref.getTagWeights().containsKey(t.getTagName())
                                ? pref.getTagWeights().get(t.getTagName()) : 1.0
                ))
                .toList();
        return Result.success(Map.of(
                "adjustmentCount", pref.getAdjustmentCount() != null ? pref.getAdjustmentCount() : 0,
                "learningProgress", pref.getLearningProgress() != null ? pref.getLearningProgress() : 0,
                "calorieMultiplier", pref.getCalorieMultiplier() != null ? pref.getCalorieMultiplier() : 1.0,
                "profileTags", tagList
        ));
    }

    @PostMapping("/preferences/reset")
    public Result<String> resetPreferences(@AuthenticationPrincipal Long userId) {
        if (userId == null) return Result.error(401, "Unauthorized");
        preferenceService.reset(userId);
        return Result.success("已重置推荐偏好");
    }

    @GetMapping("/notifications")
    public Result<List<com.diet.entity.SystemNotification>> getNotifications(@AuthenticationPrincipal Long userId) {
        if (userId == null) return Result.error(401, "Unauthorized");
        return Result.success(notificationService.getUnread(userId));
    }

    @PutMapping("/notifications/{id}/read")
    public Result<Void> markNotificationRead(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        if (userId == null) return Result.error(401, "Unauthorized");
        notificationService.markRead(id, userId);
        return Result.success(null);
    }

    @GetMapping("/body-log/chart")
    public Result<List<Map<String, Object>>> getBodyLogChart(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "30") @Min(1) @Max(365) int days) {
        if (userId == null) {
            return Result.error(401, "Unauthorized");
        }
        return Result.success(bodyLogService.getChartData(userId, days));
    }

    private void validateHeight(BigDecimal height) {
        if (height != null && (height.compareTo(BigDecimal.valueOf(100)) < 0 || height.compareTo(BigDecimal.valueOf(250)) > 0)) {
            throw new IllegalArgumentException("身高范围：100-250cm");
        }
    }

    private void validateWeight(BigDecimal weight) {
        if (weight != null && (weight.compareTo(BigDecimal.valueOf(30)) < 0 || weight.compareTo(BigDecimal.valueOf(200)) > 0)) {
            throw new IllegalArgumentException("体重范围：30-200kg");
        }
    }

    private List<String> normalizeAllergies(List<String> allergies) {
        if (allergies == null) {
            return List.of("无");
        }
        List<String> cleaned = allergies.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        return cleaned.isEmpty() ? List.of("无") : cleaned;
    }

    private BigDecimal parseWeight(Object rawWeight) {
        if (rawWeight == null) return null;
        if (rawWeight instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        if (rawWeight instanceof String s && !s.isBlank()) {
            return new BigDecimal(s);
        }
        // 兼容前端误传: {"weight":{"weight":65.2}}
        if (rawWeight instanceof Map<?, ?> map) {
            Object nested = map.get("weight");
            if (nested == null) return null;
            if (nested instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
            if (nested instanceof String s && !s.isBlank()) return new BigDecimal(s);
        }
        return null;
    }

    private LocalDate parseLogDate(Object rawLogDate) {
        if (rawLogDate == null) return LocalDate.now();
        if (rawLogDate instanceof String s && !s.isBlank()) {
            try {
                return LocalDate.parse(s);
            } catch (Exception ignored) {
                return LocalDate.now();
            }
        }
        return LocalDate.now();
    }

    private boolean isValidTarget(String target) {
        try {
            HealthProfile.Target.valueOf(target);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Data
    public static class HealthProfileUpdateRequest {
        @DecimalMin("100") @DecimalMax("250")
        private BigDecimal height;
        @DecimalMin("30") @DecimalMax("200")
        private BigDecimal weight;
        @Pattern(regexp = "MALE|FEMALE")
        private String gender;
        @Min(1) @Max(150)
        private Integer age;
        @Pattern(regexp = "LOSE_WEIGHT|BUILD_MUSCLE|MAINTAIN")
        private String target;
        @Pattern(regexp = "NONE|LIGHT|MODERATE|HIGH")
        private String exerciseFrequency;
        @Pattern(regexp = "MORNING|AFTERNOON|EVENING|NONE")
        private String exerciseTime;
        private List<String> allergyTags;
    }

    @Data
    public static class HealthProfileResponse {
        private Long id;
        private BigDecimal height;
        private BigDecimal weight;
        private String gender;
        private Integer age;
        private BigDecimal bmi;
        private String bmiRating;
        private String target;
        private String exerciseFrequency;
        private String exerciseTime;
        private BigDecimal tdee;
        private List<String> allergyTags;

        public HealthProfileResponse() {}

        public HealthProfileResponse(HealthProfile p, String bmiRating) {
            this.id = p.getId();
            this.height = p.getHeight();
            this.weight = p.getWeight();
            this.gender = p.getGender() != null ? p.getGender().name() : null;
            this.age = p.getAge();
            this.bmi = p.getBmi();
            this.bmiRating = bmiRating;
            this.target = p.getTarget() != null ? p.getTarget().name() : null;
            this.exerciseFrequency = p.getExerciseFrequency() != null ? p.getExerciseFrequency().name() : null;
            this.exerciseTime = p.getExerciseTime() != null ? p.getExerciseTime().name() : null;
            this.tdee = p.getTdee();
            this.allergyTags = p.getAllergyTags();
        }
    }

    @Data
    public static class BodyLogRequest {
        @NotNull
        @DecimalMin("30") @DecimalMax("200")
        private BigDecimal weight;
        private LocalDate logDate;
    }

    @Data
    public static class DietTagResponse {
        private String tagName;
        private java.math.BigDecimal confidenceScore;
        private String source;

        public DietTagResponse(String tagName, java.math.BigDecimal confidenceScore, String source) {
            this.tagName = tagName;
            this.confidenceScore = confidenceScore;
            this.source = source;
        }
    }

    @Data
    public static class BodyLogResponse {
        private LocalDate logDate;
        private BigDecimal weight;

        public BodyLogResponse(LocalDate logDate, BigDecimal weight) {
            this.logDate = logDate;
            this.weight = weight;
        }
    }
}
