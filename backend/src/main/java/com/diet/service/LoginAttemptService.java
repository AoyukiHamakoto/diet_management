package com.diet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 登录失败计数与短时锁定，满足「防暴力破解」类非功能需求。
 * 依赖 Redis；与项目其余缓存一致，需在运行环境启动 Redis。
 */
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private static final String FAIL_PREFIX = "login:fail:";
    private static final String LOCK_PREFIX = "login:lock:";

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${auth.login.max-attempts:5}")
    private int maxAttempts;

    @Value("${auth.login.window-minutes:15}")
    private int windowMinutes;

    @Value("${auth.login.lock-minutes:15}")
    private int lockMinutes;

    public boolean isLocked(String phone) {
        if (phone == null || phone.isBlank()) {
            return false;
        }
        Boolean locked = stringRedisTemplate.hasKey(LOCK_PREFIX + phone);
        return Boolean.TRUE.equals(locked);
    }

    /**
     * 密码错误时调用：累计失败次数，达到阈值则锁定。
     */
    public void recordFailure(String phone) {
        if (phone == null || phone.isBlank()) {
            return;
        }
        String failKey = FAIL_PREFIX + phone;
        Long count = stringRedisTemplate.opsForValue().increment(failKey);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(failKey, Duration.ofMinutes(windowMinutes));
        }
        if (count != null && count >= maxAttempts) {
            stringRedisTemplate.opsForValue().set(LOCK_PREFIX + phone, "1", Duration.ofMinutes(lockMinutes));
            stringRedisTemplate.delete(failKey);
        }
    }

    /**
     * 登录成功时清空失败计数与锁定。
     */
    public void clear(String phone) {
        if (phone == null || phone.isBlank()) {
            return;
        }
        stringRedisTemplate.delete(FAIL_PREFIX + phone);
        stringRedisTemplate.delete(LOCK_PREFIX + phone);
    }
}
