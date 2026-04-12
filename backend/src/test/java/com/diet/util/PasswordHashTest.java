package com.diet.util;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 生成演示密码 123456 的 BCrypt 哈希，用于 database/demo_data.sql 中的演示用户密码字段
 */
class PasswordHashTest {

    @Test
    void printBcryptHashFor123456() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode("123456");
        System.out.println("BCrypt hash for '123456' (use in SQL):");
        System.out.println(hash);
        // 自检
        assert encoder.matches("123456", hash);
    }
}
