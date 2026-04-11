-- 在「未启用 Flyway」或迁移未跑到补列逻辑时，手工为旧表补 like_count（幂等，可重复执行）。
-- 用法（示例）：在项目根目录执行
--   mysql -u root -p diet_management < scripts/fix_like_count_columns.sql
-- Windows 请用 cmd 重定向；勿用 PowerShell 管道喂给 mysql，易破坏引号导致语法错误。

SET @sql := (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'post_comment' AND COLUMN_NAME = 'like_count') = 0,
    'ALTER TABLE `post_comment` ADD COLUMN `like_count` INT NOT NULL DEFAULT 0 AFTER `content`',
    'SELECT 1'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql2 := (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'post' AND COLUMN_NAME = 'like_count') = 0,
    'ALTER TABLE `post` ADD COLUMN `like_count` INT NOT NULL DEFAULT 0 AFTER `review_time`',
    'SELECT 1'
  )
);
PREPARE stmt2 FROM @sql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;
