-- 仅创建数据库；表结构由 Flyway 单文件迁移 backend/src/main/resources/db/migration/V1__init_schema.sql 提供
-- 本地约定：应用连接 root / 123456，库名 diet_management（与 application.yml、docker-compose 一致）
CREATE DATABASE IF NOT EXISTS diet_management CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
