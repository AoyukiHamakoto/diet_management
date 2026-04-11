-- 旧库在更早版本已创建 post / post_comment 但无 like_count 时补列（幂等）。
-- 若 Flyway 未启用，请从项目根手动执行 scripts/fix_like_count_columns.sql。

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
