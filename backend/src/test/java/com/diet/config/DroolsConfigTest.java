package com.diet.config;

import com.diet.entity.HealthProfile;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class DroolsConfigTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379).toString());
    }

    @Autowired
    private KieContainer kieContainer;

    @Test
    void droolsEngineLoads() {
        assertNotNull(kieContainer);
    }

    @Test
    void canCreateKieSessionAndFireRules() {
        KieSession kieSession = kieContainer.newKieSession();
        assertNotNull(kieSession);

        HealthProfile profile = new HealthProfile();
        profile.setUserId(1L);
        profile.setTarget(HealthProfile.Target.BUILD_MUSCLE);
        profile.setHeight(new BigDecimal("170"));
        profile.setWeight(new BigDecimal("70"));

        kieSession.insert(profile);
        int fired = kieSession.fireAllRules();
        kieSession.dispose();

        assertTrue(fired >= 0);
    }
}
