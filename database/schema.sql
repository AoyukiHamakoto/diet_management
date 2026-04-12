-- ============================================================
-- 手工初始化：建库后加载与 Flyway 相同的表结构。
-- 唯一 DDL 与种子来源：database/migration/V1__init_schema.sql
--
-- 请在项目根目录执行（SOURCE 路径相对 mysql 客户端当前工作目录）：
--   mysql -u root -p < database/schema.sql
-- 勿与 Flyway 对同一库重复执行完整迁移（见 README 中 FLYWAY_ENABLED 说明）。
-- ============================================================

CREATE DATABASE IF NOT EXISTS `diet_management`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `diet_management`;

SET NAMES utf8mb4;

SOURCE database/migration/V1__init_schema.sql;
