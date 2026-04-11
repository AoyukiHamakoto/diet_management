package com.diet.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class RedisServiceTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379).toString());
    }

    @Autowired
    private RedisService redisService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void redisConnectionWorks() {
        assertNotNull(redisTemplate);
        assertNotNull(redisService);
    }

    @Test
    void redisSetAndGet() {
        String key = "test:key:" + System.currentTimeMillis();
        String value = "test-value";
        redisService.set(key, value);
        Object result = redisService.get(key);
        assertEquals(value, result);
        redisService.delete(key);
    }

    @Test
    void redisExpire() {
        String key = "test:expire:" + System.currentTimeMillis();
        redisService.set(key, "value", 10, java.util.concurrent.TimeUnit.SECONDS);
        assertTrue(redisService.hasKey(key));
        redisService.delete(key);
    }
}
