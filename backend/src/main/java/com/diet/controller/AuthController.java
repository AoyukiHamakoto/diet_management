package com.diet.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.diet.common.Result;
import com.diet.entity.HealthProfile;
import com.diet.entity.User;
import com.diet.service.IHealthProfileService;
import com.diet.service.LoginAttemptService;
import com.diet.service.IUserService;
import com.diet.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "认证接口")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final IUserService userService;
    private final IHealthProfileService healthProfileService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final LoginAttemptService loginAttemptService;
    @Value("${auth.register-verification-code:123456}")
    private String registerVerificationCode;
    private static final java.util.regex.Pattern STRONG_PASSWORD_PATTERN =
            java.util.regex.Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,64}$");

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Result<Map<String, Object>> register(@Valid @RequestBody RegisterRequest req) {
        log.info("[业务-注册] 开始注册, phone={}", req.getPhone() != null ? req.getPhone().replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2") : null);
        // Check if phone already exists
        User existing = userService.getOne(new QueryWrapper<User>().eq("phone", req.getPhone()));
        if (existing != null) {
            log.warn("[业务-注册] 手机号已存在");
            return Result.error(400, "该手机号已注册");
        }

        if (req.getSmsCode() == null || req.getSmsCode().isBlank() || !registerVerificationCode.equals(req.getSmsCode())) {
            log.warn("[业务-注册] 验证码错误");
            return Result.error(400, "短信验证码错误");
        }

        if (!STRONG_PASSWORD_PATTERN.matcher(req.getPassword()).matches()) {
            return Result.error(400, "密码需包含大小写字母、数字和特殊字符，且长度至少8位");
        }

        // Create user
        User user = new User();
        user.setPhone(req.getPhone());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setNickname(req.getNickname() != null ? req.getNickname() : "用户" + req.getPhone().substring(7));
        user.setRole(User.Role.USER);
        user.setEnabled(true);
        user.setActivated(true);
        userService.save(user);
        log.info("[业务-注册] 用户已创建, userId={}", user.getId());

        // Create empty health profile
        HealthProfile profile = new HealthProfile();
        profile.setUserId(user.getId());
        healthProfileService.save(profile);
        log.info("[业务-注册] 健康档案已创建, userId={}", user.getId());

        return Result.success(Map.of(
                "activated", true,
                "message", "注册成功，请登录",
                "user", userToMap(user)));
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest req) {
        log.info("[业务-登录] 开始登录, phone={}", req.getPhone() != null ? req.getPhone().replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2") : null);
        if (loginAttemptService.isLocked(req.getPhone())) {
            log.warn("[业务-登录] 登录已锁定, phone 已省略");
            return Result.error(429, "登录失败次数过多，请稍后再试");
        }
        User user = userService.getOne(new QueryWrapper<User>().eq("phone", req.getPhone()));
        if (user == null) {
            log.warn("[业务-登录] 用户不存在");
            return Result.error(401, "用户不存在");
        }
        if (user.getEnabled() != null && !user.getEnabled()) {
            log.warn("[业务-登录] 账户已禁用, userId={}", user.getId());
            return Result.error(403, "账户已被禁用");
        }
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            log.warn("[业务-登录] 密码错误, userId={}", user.getId());
            loginAttemptService.recordFailure(req.getPhone());
            return Result.error(401, "密码错误");
        }

        loginAttemptService.clear(req.getPhone());
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getPhone(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getPhone(), user.getRole().name());
        log.info("[业务-登录] 登录成功, userId={}, role={}", user.getId(), user.getRole());

        return Result.success(Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken,
                "user", userToMap(user)));
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息")
    public Result<Map<String, Object>> me(@AuthenticationPrincipal Long userId) {
        if (userId == null) {
            return Result.error(401, "Unauthorized");
        }
        User user = userService.getById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        HealthProfile profile = healthProfileService.lambdaQuery()
                .eq(HealthProfile::getUserId, userId)
                .one();

        int completionRate = calculateCompletionRate(profile);

        return Result.success(Map.of(
                "user", userToMap(user),
                "healthProfile", profile != null ? profile : Map.of(),
                "completionRate", completionRate));
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新Token")
    public Result<Map<String, String>> refresh(@RequestBody RefreshRequest req) {
        if (req.getRefreshToken() == null || req.getRefreshToken().isBlank()) {
            return Result.error(400, "refreshToken is required");
        }

        try {
            if (!jwtUtil.validateToken(req.getRefreshToken())) {
                return Result.error(401, "Invalid refresh token");
            }
            var claims = jwtUtil.parseToken(req.getRefreshToken());
            String type = claims.get("type", String.class);
            if (!"refresh".equals(type)) {
                return Result.error(401, "Not a refresh token");
            }

            Long userId = Long.parseLong(claims.getSubject());
            String phone = claims.get("phone", String.class);
            String role = claims.get("role", String.class);

            User user = userService.getById(userId);
            if (user == null || (user.getEnabled() != null && !user.getEnabled())) {
                return Result.error(401, "User not found or disabled");
            }

            String newAccessToken = jwtUtil.generateAccessToken(userId, phone, role);
            String newRefreshToken = jwtUtil.generateRefreshToken(userId, phone, role);

            return Result.success(Map.of(
                    "accessToken", newAccessToken,
                    "refreshToken", newRefreshToken));
        } catch (Exception e) {
            return Result.error(401, "Invalid refresh token: " + e.getMessage());
        }
    }

    private Map<String, Object> userToMap(User user) {
        return Map.of(
                "id", user.getId(),
                "phone", user.getPhone(),
                "nickname", user.getNickname() != null ? user.getNickname() : "",
                "avatar", user.getAvatar() != null ? user.getAvatar() : "",
                "activated", user.getActivated() == null || user.getActivated(),
                "role", user.getRole().name());
    }

    private int calculateCompletionRate(HealthProfile profile) {
        if (profile == null)
            return 0;
        int total = 6; // height, weight, gender, age, target, exerciseFrequency
        int filled = 0;
        if (profile.getHeight() != null && profile.getHeight().compareTo(BigDecimal.ZERO) > 0)
            filled++;
        if (profile.getWeight() != null && profile.getWeight().compareTo(BigDecimal.ZERO) > 0)
            filled++;
        if (profile.getGender() != null)
            filled++;
        if (profile.getAge() != null && profile.getAge() > 0)
            filled++;
        if (profile.getTarget() != null)
            filled++;
        if (profile.getExerciseFrequency() != null)
            filled++;
        return (int) ((filled * 100.0) / total);
    }

    @Data
    public static class RegisterRequest {
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        private String phone;

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 64, message = "密码长度为8-64个字符")
        private String password;

        @NotBlank(message = "验证码不能为空")
        private String smsCode;
        private String nickname;
    }

    @Data
    public static class LoginRequest {
        @NotBlank(message = "手机号不能为空")
        private String phone;

        @NotBlank(message = "密码不能为空")
        private String password;
    }

    @Data
    public static class RefreshRequest {
        private String refreshToken;
    }
}
