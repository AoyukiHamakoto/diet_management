-- ============================================================
-- 一键部署（服务器/宝塔）：单文件版，无 SOURCE 依赖
-- 用法（Linux/宝塔终端）：
--   mysql -u root -p < database/full_deploy.sql
-- ============================================================

DROP DATABASE IF EXISTS `diet_management`;

CREATE DATABASE IF NOT EXISTS `diet_management`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `diet_management`;
SET NAMES utf8mb4;

-- 为后端账号授权（需使用有权限的账号导入本 SQL，如 root）
CREATE USER IF NOT EXISTS 'user'@'localhost' IDENTIFIED BY '123456';
GRANT ALL PRIVILEGES ON `diet_management`.* TO 'user'@'localhost';
CREATE USER IF NOT EXISTS 'user'@'%' IDENTIFIED BY '123456';
GRANT ALL PRIVILEGES ON `diet_management`.* TO 'user'@'%';
FLUSH PRIVILEGES;

-- =============================================================================
-- 以下内联自：database/migration/V1__init_schema.sql
-- =============================================================================

-- =============================================================================
-- 统合原 V1～V22 及手工补丁：新库一条迁移即可建全表 + 种子数据。
-- 已存在旧 flyway_schema_history 的库请勿直接替换迁移文件；需 baseline 或重建库后再用。
-- =============================================================================

-- ----------------------------------------------------------------------------- user
CREATE TABLE `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `phone` VARCHAR(20) NOT NULL COMMENT 'Phone number',
    `password` VARCHAR(255) NOT NULL COMMENT 'Encrypted password',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT 'Nickname',
    `avatar` VARCHAR(500) DEFAULT NULL COMMENT 'Avatar URL',
    `role` VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT 'USER, ADMIN',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1=enabled, 0=disabled',
    `activated` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '账号是否已激活',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User table';

-- ----------------------------------------------------------------------------- health_profile
CREATE TABLE `health_profile` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `user_id` BIGINT NOT NULL COMMENT 'User ID',
    `height` DECIMAL(5,2) DEFAULT NULL COMMENT 'Height (cm)',
    `weight` DECIMAL(5,2) DEFAULT NULL COMMENT 'Weight (kg)',
    `gender` VARCHAR(10) DEFAULT NULL COMMENT 'MALE, FEMALE',
    `age` INT DEFAULT NULL COMMENT 'Age in years',
    `bmi` DECIMAL(4,2) DEFAULT NULL COMMENT 'BMI (calculated)',
    `target` VARCHAR(20) DEFAULT NULL COMMENT 'LOSE_WEIGHT, BUILD_MUSCLE, MAINTAIN',
    `allergy_tags` JSON DEFAULT NULL COMMENT 'Allergy tags JSON array',
    `exercise_frequency` VARCHAR(20) DEFAULT NULL COMMENT 'NONE, LIGHT, MODERATE, HIGH',
    `exercise_time` VARCHAR(20) DEFAULT 'NONE' COMMENT 'MORNING/AFTERNOON/EVENING/NONE',
    `tdee` DECIMAL(8,2) DEFAULT NULL COMMENT 'Total Daily Energy Expenditure (calories)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    CONSTRAINT `fk_health_profile_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Health profile table';

-- ----------------------------------------------------------------------------- body_log
CREATE TABLE `body_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `user_id` BIGINT NOT NULL COMMENT 'User ID',
    `weight` DECIMAL(5,2) NOT NULL COMMENT 'Weight (kg)',
    `log_date` DATE NOT NULL COMMENT 'Log date',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_log_date` (`user_id`, `log_date`),
    KEY `idx_user_id` (`user_id`),
    CONSTRAINT `fk_body_log_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Body weight log for chart';

-- ----------------------------------------------------------------------------- diet_tag
CREATE TABLE `diet_tag` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `user_id` BIGINT NOT NULL COMMENT 'User ID',
    `tag_name` VARCHAR(50) NOT NULL COMMENT 'HIGH_PROTEIN, LOW_CARB, GLUTEN_FREE, etc.',
    `confidence_score` DECIMAL(3,2) DEFAULT NULL COMMENT 'Confidence score 0-1',
    `source` VARCHAR(100) DEFAULT NULL COMMENT 'Rule ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    CONSTRAINT `fk_diet_tag_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Diet tag table (Drools generated)';

-- ----------------------------------------------------------------------------- recipe
CREATE TABLE `recipe` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `title` VARCHAR(200) NOT NULL COMMENT 'Recipe title',
    `cover_image` VARCHAR(500) DEFAULT NULL COMMENT 'Cover image URL',
    `ingredients` JSON DEFAULT NULL COMMENT 'Ingredients JSON',
    `nutrition_info` JSON DEFAULT NULL COMMENT 'Protein/Carb/Fat/Calories JSON',
    `category` VARCHAR(20) NOT NULL COMMENT 'BREAKFAST, LUNCH, DINNER, SNACK',
    `cooking_time` INT DEFAULT NULL COMMENT 'Cooking time (minutes)',
    `difficulty` VARCHAR(20) DEFAULT NULL COMMENT 'EASY, MEDIUM, HARD',
    `tags` JSON DEFAULT NULL COMMENT 'Tags JSON array',
    `match_tags` JSON DEFAULT NULL COMMENT 'Tags this recipe matches',
    `calories_per_100g` DECIMAL(6,2) DEFAULT NULL COMMENT 'Calories per 100g',
    `protein_carb_fat_ratio` VARCHAR(20) DEFAULT NULL COMMENT 'e.g. 30:40:30',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, APPROVED, REJECTED',
    `review_stage` VARCHAR(30) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/FIRST_APPROVED/FINAL_APPROVED/REJECTED',
    `reject_reason` VARCHAR(500) DEFAULT NULL COMMENT 'Admin reject reason',
    `first_reviewer_id` BIGINT DEFAULT NULL COMMENT '初审管理员ID',
    `first_review_time` DATETIME DEFAULT NULL COMMENT '初审时间',
    `final_reviewer_id` BIGINT DEFAULT NULL COMMENT '终审管理员ID',
    `final_review_time` DATETIME DEFAULT NULL COMMENT '终审时间',
    `creator_id` BIGINT DEFAULT NULL COMMENT 'Creator user ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '0=normal, 1=deleted',
    PRIMARY KEY (`id`),
    KEY `idx_category` (`category`),
    KEY `idx_status` (`status`),
    KEY `idx_creator_id` (`creator_id`),
    KEY `idx_recipe_deleted` (`deleted`),
    KEY `idx_recipe_creator_deleted` (`creator_id`, `deleted`),
    CONSTRAINT `fk_recipe_creator` FOREIGN KEY (`creator_id`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Recipe table';

-- ----------------------------------------------------------------------------- meal_plan
CREATE TABLE `meal_plan` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `user_id` BIGINT NOT NULL COMMENT 'User ID',
    `plan_date` DATE NOT NULL COMMENT 'Plan date',
    `meal_type` VARCHAR(20) NOT NULL COMMENT 'BREAKFAST, LUNCH, DINNER, SNACK',
    `recipe_id` BIGINT DEFAULT NULL COMMENT 'Recipe ID',
    `suggested_calories` DECIMAL(8,2) DEFAULT NULL COMMENT 'Suggested calories',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PLANNED' COMMENT 'PLANNED, COMPLETED, SKIPPED',
    `audit_status` VARCHAR(20) NOT NULL DEFAULT 'AUTO' COMMENT 'AUTO/PENDING/APPROVED/REJECTED',
    `audit_comment` TEXT NULL COMMENT 'Audit comment/reject reason',
    `audited_at` DATETIME NULL COMMENT 'Audit timestamp',
    `audited_by` BIGINT NULL COMMENT 'Admin user id',
    `out_eat_note` VARCHAR(500) DEFAULT NULL COMMENT 'Record when eating out',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    PRIMARY KEY (`id`),
    KEY `idx_user_plan_date` (`user_id`, `plan_date`),
    KEY `idx_recipe_id` (`recipe_id`),
    KEY `idx_meal_plan_audit_status` (`audit_status`),
    KEY `idx_meal_plan_user_date_audit` (`user_id`, `plan_date`, `audit_status`),
    CONSTRAINT `fk_meal_plan_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_meal_plan_recipe` FOREIGN KEY (`recipe_id`) REFERENCES `recipe` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Meal plan table';

-- ----------------------------------------------------------------------------- feedback
CREATE TABLE `feedback` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `user_id` BIGINT NOT NULL COMMENT 'User ID',
    `plan_id` BIGINT NOT NULL COMMENT 'Meal plan ID',
    `rating` TINYINT NOT NULL COMMENT 'Rating 1-5',
    `satiety_rating` TINYINT DEFAULT NULL COMMENT '饱腹感评分 1-5',
    `difficulty_rating` TINYINT DEFAULT NULL COMMENT '执行难度评分 1-5',
    `satisfaction_score` DECIMAL(4,2) DEFAULT NULL COMMENT '综合满意度',
    `feedback_tags` JSON DEFAULT NULL COMMENT 'Multi tags',
    `taste_feedback` VARCHAR(50) DEFAULT NULL COMMENT 'TOO_LIGHT, TOO_OILY, TOO_MUCH, etc.',
    `body_reaction` VARCHAR(50) DEFAULT NULL COMMENT 'ENERGETIC, BLOATED, TIRED, etc.',
    `comment` TEXT DEFAULT NULL COMMENT 'User comment',
    `system_action` VARCHAR(200) DEFAULT NULL COMMENT 'What system did based on feedback',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_plan_id` (`plan_id`),
    CONSTRAINT `fk_feedback_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_feedback_plan` FOREIGN KEY (`plan_id`) REFERENCES `meal_plan` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Feedback table';

-- ----------------------------------------------------------------------------- user_preference & system_notification
CREATE TABLE `user_preference` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `calorie_multiplier` DECIMAL(3,2) NOT NULL DEFAULT 1.00 COMMENT '0.9 = 10% reduction',
    `tag_weights` JSON DEFAULT NULL COMMENT 'Tag name -> weight 0-1',
    `adjustment_count` INT NOT NULL DEFAULT 0,
    `learning_progress` INT NOT NULL DEFAULT 0 COMMENT '0-100',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    CONSTRAINT `fk_user_preference_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `system_notification` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `message` VARCHAR(500) NOT NULL,
    `is_read` TINYINT(1) NOT NULL DEFAULT 0,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_read` (`user_id`, `is_read`),
    CONSTRAINT `fk_notification_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------- appeal
CREATE TABLE `appeal` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '申诉人ID',
    `target_type` VARCHAR(20) NOT NULL COMMENT 'MEAL_PLAN/RECIPE/POST',
    `target_id` BIGINT NOT NULL COMMENT '申诉对象ID',
    `reason` TEXT NOT NULL COMMENT '申诉理由',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PROCESSING/RESOLVED/REJECTED',
    `admin_id` BIGINT DEFAULT NULL COMMENT '处理管理员ID',
    `admin_comment` TEXT COMMENT '管理员处理意见',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_appeal_user_id` (`user_id`),
    KEY `idx_appeal_target` (`target_type`, `target_id`),
    KEY `idx_appeal_status` (`status`),
    CONSTRAINT `fk_appeal_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='申诉表';

-- ----------------------------------------------------------------------------- food_component
CREATE TABLE `food_component` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `food_code` VARCHAR(20) DEFAULT NULL COMMENT '食物编码',
    `food_name` VARCHAR(100) NOT NULL COMMENT '食物名称',
    `food_category` VARCHAR(50) DEFAULT NULL COMMENT '食物类别',
    `edible_part` DECIMAL(4,1) DEFAULT NULL COMMENT '可食部（%）',
    `energy_kcal` DECIMAL(6,2) DEFAULT NULL COMMENT '能量（kcal/100g）',
    `protein` DECIMAL(5,2) DEFAULT NULL COMMENT '蛋白质（g/100g）',
    `fat` DECIMAL(5,2) DEFAULT NULL COMMENT '脂肪（g/100g）',
    `carbohydrate` DECIMAL(5,2) DEFAULT NULL COMMENT '碳水化合物（g/100g）',
    `dietary_fiber` DECIMAL(5,2) DEFAULT NULL COMMENT '膳食纤维（g）',
    `cholesterol` DECIMAL(6,2) DEFAULT NULL COMMENT '胆固醇（mg）',
    `vitamin_a` DECIMAL(6,2) DEFAULT NULL COMMENT '维生素A',
    `calcium` DECIMAL(5,2) DEFAULT NULL COMMENT '钙（mg）',
    `iron` DECIMAL(4,2) DEFAULT NULL COMMENT '铁（mg）',
    `zinc` DECIMAL(4,2) DEFAULT NULL COMMENT '锌（mg）',
    PRIMARY KEY (`id`),
    KEY `idx_food_name` (`food_name`),
    KEY `idx_food_category` (`food_category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='食物成分表';

-- ----------------------------------------------------------------------------- order_record & post (community)
CREATE TABLE IF NOT EXISTS `order_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户id',
    `order_no` VARCHAR(40) NOT NULL COMMENT '订单号',
    `item_name` VARCHAR(120) NOT NULL COMMENT '订单内容',
    `amount` DECIMAL(10,2) NOT NULL COMMENT '金额',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PAID/REFUNDED',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_order_user_id` (`user_id`),
    KEY `idx_order_status` (`status`),
    CONSTRAINT `fk_order_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `post` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户id',
    `title` VARCHAR(200) NOT NULL COMMENT '标题',
    `content` TEXT NOT NULL COMMENT '内容',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
    `reject_reason` VARCHAR(500) DEFAULT NULL COMMENT '驳回原因',
    `reviewer_id` BIGINT DEFAULT NULL COMMENT '审核员id',
    `review_time` DATETIME DEFAULT NULL COMMENT '审核时间',
    `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标记',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_post_user_id` (`user_id`),
    KEY `idx_post_status` (`status`),
    KEY `idx_post_deleted` (`deleted`),
    CONSTRAINT `fk_post_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `post_like` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `post_id` BIGINT NOT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_post_like_user_post` (`user_id`, `post_id`),
    KEY `idx_post_like_post` (`post_id`),
    CONSTRAINT `fk_post_like_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_post_like_post` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `post_comment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `post_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `parent_id` BIGINT DEFAULT NULL,
    `content` VARCHAR(2000) NOT NULL,
    `like_count` INT NOT NULL DEFAULT 0 COMMENT '评论点赞数',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_pc_post` (`post_id`),
    KEY `idx_pc_parent` (`parent_id`),
    CONSTRAINT `fk_pc_post` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_pc_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `comment_like` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `comment_id` BIGINT NOT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_cl_user_comment` (`user_id`, `comment_id`),
    KEY `idx_cl_comment` (`comment_id`),
    CONSTRAINT `fk_cl_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_cl_comment` FOREIGN KEY (`comment_id`) REFERENCES `post_comment` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------- 幂等补丁：旧库仅有无 like_count 的 post_comment 表时补列（新库已有列则跳过）
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

-- ----------------------------------------------------------------------------- 种子：示例菜谱
INSERT INTO `recipe` (title, cover_image, ingredients, nutrition_info, category, cooking_time, difficulty, tags, match_tags, calories_per_100g, protein_carb_fat_ratio, status, review_stage, create_time, update_time) VALUES
('鸡胸肉沙拉', NULL, '[{"name":"鸡胸肉","amount":"150g"},{"name":"生菜","amount":"50g"},{"name":"圣女果","amount":"5颗"}]', '{"protein":35,"carb":8,"fat":3,"calories":210}', 'LUNCH', 15, 'EASY', '["轻食","高蛋白"]', '["HIGH_PROTEIN","LOW_CALORIE"]', 70, '70:15:15', 'APPROVED', 'FINAL_APPROVED', NOW(), NOW()),
('蒜香牛排', NULL, '[{"name":"牛排","amount":"200g"},{"name":"蒜","amount":"3瓣"},{"name":"黄油","amount":"10g"}]', '{"protein":45,"carb":2,"fat":35,"calories":480}', 'DINNER', 20, 'MEDIUM', '["西餐","高蛋白"]', '["HIGH_PROTEIN","HIGH_CALORIE"]', 240, '40:5:55', 'APPROVED', 'FINAL_APPROVED', NOW(), NOW()),
('虾仁炒饭', NULL, '[{"name":"虾仁","amount":"100g"},{"name":"米饭","amount":"200g"},{"name":"鸡蛋","amount":"1个"}]', '{"protein":25,"carb":60,"fat":8,"calories":420}', 'LUNCH', 15, 'EASY', '["炒饭","海鲜"]', '["HIGH_PROTEIN"]', 105, '25:60:15', 'APPROVED', 'FINAL_APPROVED', NOW(), NOW()),
('燕麦酸奶杯', NULL, '[{"name":"燕麦","amount":"50g"},{"name":"酸奶","amount":"100g"},{"name":"香蕉","amount":"半根"}]', '{"protein":12,"carb":45,"fat":5,"calories":280}', 'BREAKFAST', 5, 'EASY', '["早餐","轻食"]', '["LOW_CALORIE"]', 93, '20:65:15', 'APPROVED', 'FINAL_APPROVED', NOW(), NOW()),
('水煮蛋全麦三明治', NULL, '[{"name":"全麦面包","amount":"2片"},{"name":"鸡蛋","amount":"2个"},{"name":"生菜","amount":"20g"}]', '{"protein":18,"carb":30,"fat":12,"calories":300}', 'BREAKFAST', 10, 'EASY', '["早餐","高蛋白"]', '["HIGH_PROTEIN","LOW_CARB"]', 100, '35:45:20', 'APPROVED', 'FINAL_APPROVED', NOW(), NOW());

-- ----------------------------------------------------------------------------- 种子：食物成分
INSERT INTO `food_component` (food_name, food_category, edible_part, energy_kcal, protein, fat, carbohydrate, dietary_fiber) VALUES
('鸡蛋', '蛋类', 88, 144, 13.3, 8.8, 2.8, 0),
('鸡胸肉', '禽肉类', 100, 133, 31.0, 1.2, 0, 0),
('瘦牛肉', '畜肉类', 100, 125, 21.3, 3.5, 0, 0),
('牛奶', '乳类', 100, 54, 3.0, 3.2, 3.4, 0),
('大米（蒸）', '谷薯类', 100, 116, 2.6, 0.3, 25.6, 0.4),
('燕麦片', '谷薯类', 100, 389, 16.9, 6.9, 66.3, 10.6),
('土豆', '蔬菜类', 94, 77, 2.0, 0.1, 17.5, 2.2),
('番茄', '蔬菜类', 97, 18, 0.9, 0.2, 3.9, 1.2),
('黄瓜', '蔬菜类', 97, 15, 0.7, 0.1, 3.6, 0.5),
('豆腐', '豆类', 100, 76, 8.1, 4.2, 1.9, 0.4),
('香蕉', '水果类', 64, 89, 1.1, 0.3, 22.8, 2.6),
('苹果', '水果类', 85, 52, 0.3, 0.2, 13.8, 2.4),
('虾仁', '鱼虾类', 100, 99, 24.0, 0.3, 0.2, 0),
('酸奶（原味）', '乳类', 100, 59, 10.0, 0.4, 3.6, 0),
('西兰花', '蔬菜类', 91, 34, 2.8, 0.4, 6.6, 2.6),
('菠菜', '蔬菜类', 91, 23, 2.9, 0.4, 3.6, 2.2),
('胡萝卜', '蔬菜类', 90, 41, 0.9, 0.2, 9.6, 2.8),
('瘦猪肉', '畜肉类', 100, 143, 20.3, 6.2, 0, 0),
('三文鱼', '鱼虾类', 100, 139, 19.8, 6.3, 0, 0),
('糙米（煮）', '谷薯类', 100, 112, 2.6, 0.9, 23.5, 1.8);

-- ----------------------------------------------------------------------------- 菜谱营养与 match_tags 对齐（原 V20）
UPDATE `recipe` SET
    nutrition_info = '{"protein":35,"carb":8,"fat":3,"calories":210}',
    calories_per_100g = 70,
    protein_carb_fat_ratio = '70:15:15',
    match_tags = '["HIGH_PROTEIN","LOW_CALORIE","QUICK"]'
WHERE title = '鸡胸肉沙拉' AND (deleted = 0 OR deleted IS NULL);

UPDATE `recipe` SET
    nutrition_info = '{"protein":45,"carb":2,"fat":35,"calories":480}',
    calories_per_100g = 240,
    protein_carb_fat_ratio = '40:5:55',
    match_tags = '["HIGH_PROTEIN","HIGH_CALORIE"]'
WHERE title = '蒜香牛排' AND (deleted = 0 OR deleted IS NULL);

UPDATE `recipe` SET
    nutrition_info = '{"protein":25,"carb":60,"fat":8,"calories":420}',
    calories_per_100g = 105,
    protein_carb_fat_ratio = '25:60:15',
    match_tags = '["HIGH_PROTEIN","QUICK"]'
WHERE title = '虾仁炒饭' AND (deleted = 0 OR deleted IS NULL);

UPDATE `recipe` SET
    nutrition_info = '{"protein":12,"carb":45,"fat":5,"calories":280}',
    calories_per_100g = 93,
    protein_carb_fat_ratio = '20:65:15',
    match_tags = '["LOW_CALORIE","QUICK","LOW_CARB"]'
WHERE title = '燕麦酸奶杯' AND (deleted = 0 OR deleted IS NULL);

UPDATE `recipe` SET
    nutrition_info = '{"protein":18,"carb":30,"fat":12,"calories":300}',
    calories_per_100g = 100,
    protein_carb_fat_ratio = '35:45:20',
    match_tags = '["HIGH_PROTEIN","LOW_CARB","QUICK"]'
WHERE title = '水煮蛋全麦三明治' AND (deleted = 0 OR deleted IS NULL);

UPDATE `recipe`
SET nutrition_info = '{"protein":15,"carb":40,"fat":12,"calories":300}',
    calories_per_100g = COALESCE(calories_per_100g, 100),
    protein_carb_fat_ratio = COALESCE(NULLIF(TRIM(COALESCE(protein_carb_fat_ratio, '')), ''), '30:45:25')
WHERE (deleted = 0 OR deleted IS NULL)
  AND status = 'APPROVED'
  AND (nutrition_info IS NULL OR nutrition_info = '' OR nutrition_info = 'null');

-- ----------------------------------------------------------------------------- 社区示例帖（原 V19）
INSERT INTO `post` (user_id, title, content, status, deleted, like_count, create_time, update_time)
SELECT id, '减脂期早餐：燕麦碗怎么搭', '燕麦、希腊酸奶、蓝莓和一小把坚果，饱腹感强又不太高热量。', 'APPROVED', 0, 3, NOW(), NOW() FROM `user` ORDER BY id ASC LIMIT 1;

INSERT INTO `post` (user_id, title, content, status, deleted, like_count, create_time, update_time)
SELECT id, '练后晚餐加蛋白的小技巧', '晚餐把一部分碳水挪到练前，练后多一口鸡胸肉或豆腐。', 'APPROVED', 0, 5, NOW(), NOW() FROM `user` ORDER BY id ASC LIMIT 1;

INSERT INTO `post` (user_id, title, content, status, deleted, like_count, create_time, update_time)
SELECT id, '外食怎么选：火锅篇', '清汤锅底、多蔬菜、蘸料少芝麻酱，蛋白质选瘦牛肉和豆制品。', 'APPROVED', 0, 2, NOW(), NOW() FROM `user` ORDER BY id ASC LIMIT 1;

INSERT INTO `post` (user_id, title, content, status, deleted, like_count, create_time, update_time)
SELECT id, '一周备餐：周日两小时搞定', '鸡胸肉腌制分装、杂粮饭冷冻、西兰花焯水分袋，工作日省心。', 'APPROVED', 0, 8, NOW(), NOW() FROM `user` ORDER BY id ASC LIMIT 1;

INSERT INTO `post` (user_id, title, content, status, deleted, like_count, create_time, update_time)
SELECT id, '睡眠不足时食欲更旺盛？', '我会把加餐换成高蛋白酸奶，避免深夜饼干。', 'APPROVED', 0, 1, NOW(), NOW() FROM `user` ORDER BY id ASC LIMIT 1;

INSERT INTO `post` (user_id, title, content, status, deleted, like_count, create_time, update_time)
SELECT id, '快手午餐：20 分钟便当', '番茄炒蛋+凉拌黄瓜+米饭，少油版一样香。', 'APPROVED', 0, 4, NOW(), NOW() FROM `user` ORDER BY id ASC LIMIT 1;

INSERT INTO `post` (user_id, title, content, status, deleted, like_count, create_time, update_time)
SELECT id, '控糖期水果怎么吃', '莓果类优先，香蕉当练后快碳更合适。', 'APPROVED', 0, 6, NOW(), NOW() FROM `user` ORDER BY id ASC LIMIT 1;

INSERT INTO `post` (user_id, title, content, status, deleted, like_count, create_time, update_time)
SELECT id, '宿舍党无火早餐合集', '即食燕麦、牛奶、即食鸡胸，便利店也能凑一餐。', 'APPROVED', 0, 2, NOW(), NOW() FROM `user` ORDER BY id ASC LIMIT 1;

INSERT INTO `post` (user_id, title, content, status, deleted, like_count, create_time, update_time)
SELECT id, '记录热量 vs 直觉进食', '我每周只称三天，避免焦虑，但心里有数。', 'APPROVED', 0, 7, NOW(), NOW() FROM `user` ORDER BY id ASC LIMIT 1;

INSERT INTO `post` (user_id, title, content, status, deleted, like_count, create_time, update_time)
SELECT id, '平台期的一点心态', '体重不动时看围度和力量，别只盯秤。', 'APPROVED', 0, 9, NOW(), NOW() FROM `user` ORDER BY id ASC LIMIT 1;

INSERT INTO `post` (user_id, title, content, status, deleted, like_count, create_time, update_time)
SELECT id, '复训第一周饮食调整', '总热量略升一点，碳水集中在训练前后。', 'APPROVED', 0, 0, NOW(), NOW() FROM `user` ORDER BY id ASC LIMIT 1;

-- =============================================================================
-- 以下内联自：database/demo_data.sql（节选到当前仓库版本）
-- =============================================================================

-- 统一演示密码：明文 123456，此处为 BCrypt 哈希（由 PasswordHashTest 生成，与后端一致）
SET @pwd = '$2a$10$zg695u/ozyiGaZq5VBSlZew0m8IR62ULZhqZ1zyWngT09mycPmQnG';

-- ------------------------------------------------------------
-- 1. 用户表 user - 30 条（25 USER + 5 ADMIN）
-- ------------------------------------------------------------
INSERT INTO `user` (`phone`, `password`, `nickname`, `avatar`, `role`, `enabled`, `create_time`) VALUES
('13800138000', @pwd, '系统管理员', NULL, 'ADMIN', 1, '2024-01-05 10:00:00'),
('13800138001', @pwd, '审核专员', NULL, 'ADMIN', 1, '2024-01-10 10:00:00'),
('13800138002', @pwd, '运营管理员', NULL, 'ADMIN', 1, '2024-02-01 10:00:00'),
('13800138003', @pwd, '数据管理员', NULL, 'ADMIN', 1, '2024-03-01 10:00:00'),
('13800138004', @pwd, '超级管理员', NULL, 'ADMIN', 1, '2024-01-01 10:00:00'),
('13812345601', @pwd, '减脂小王', NULL, 'USER', 1, '2024-01-15 09:00:00'),
('13812345602', @pwd, '增肌达人', NULL, 'USER', 1, '2024-01-20 09:00:00'),
('13912345603', @pwd, '瑜伽Alice', NULL, 'USER', 1, '2024-02-01 09:00:00'),
('15012345604', @pwd, '跑步李四', NULL, 'USER', 1, '2024-02-10 09:00:00'),
('15112345605', @pwd, '健身教练王', NULL, 'USER', 1, '2024-02-15 09:00:00'),
('15212345606', @pwd, '轻食控小张', NULL, 'USER', 1, '2024-03-01 09:00:00'),
('15712345607', @pwd, '低碳水小明', NULL, 'USER', 1, '2024-03-10 09:00:00'),
('15812345608', @pwd, '高蛋白阿杰', NULL, 'USER', 1, '2024-03-15 09:00:00'),
('15912345609', @pwd, '维持体重小刘', NULL, 'USER', 1, '2024-04-01 09:00:00'),
('18212345610', @pwd, '沙拉爱好者', NULL, 'USER', 1, '2024-04-10 09:00:00'),
('18312345611', @pwd, '鸡胸肉达人', NULL, 'USER', 1, '2024-05-01 09:00:00'),
('18712345612', @pwd, '周末健身党', NULL, 'USER', 1, '2024-05-15 09:00:00'),
('18812345613', @pwd, '晨跑小陈', NULL, 'USER', 1, '2024-06-01 09:00:00'),
('13822345614', @pwd, '夜训小周', NULL, 'USER', 1, '2024-06-15 09:00:00'),
('13922345615', @pwd, '素食减脂', NULL, 'USER', 1, '2024-07-01 09:00:00'),
('15022345616', @pwd, '增重小马', NULL, 'USER', 1, '2024-08-01 09:00:00'),
('15122345617', @pwd, '均衡饮食赵', NULL, 'USER', 1, '2024-09-01 09:00:00'),
('15222345618', @pwd, '测试禁用1', NULL, 'USER', 0, '2024-10-01 09:00:00'),
('15722345619', @pwd, '测试禁用2', NULL, 'USER', 0, '2024-10-15 09:00:00'),
('15822345620', @pwd, '新人用户A', NULL, 'USER', 1, '2024-11-01 09:00:00'),
('15922345621', @pwd, '新人用户B', NULL, 'USER', 1, '2024-12-01 09:00:00'),
('18222345622', @pwd, '新人用户C', NULL, 'USER', 1, '2025-01-01 09:00:00'),
('18322345623', @pwd, '新人用户D', NULL, 'USER', 1, '2025-01-15 09:00:00'),
('18722345624', @pwd, '新人用户E', NULL, 'USER', 1, '2025-02-01 09:00:00'),
('18822345625', @pwd, '演示用户', NULL, 'USER', 1, '2025-02-10 09:00:00');

-- ------------------------------------------------------------
-- 2. 健康档案 health_profile - 25 条（user_id 1-25）
-- ------------------------------------------------------------
INSERT INTO `health_profile` (`user_id`, `height`, `weight`, `gender`, `age`, `bmi`, `target`, `allergy_tags`, `exercise_frequency`, `tdee`, `create_time`, `update_time`) VALUES
(1, 175.00, 72.00, 'MALE', 28, 23.45, 'MAINTAIN', NULL, 'MODERATE', 2200.00, NOW(), NOW()),
(2, 168.00, 65.00, 'FEMALE', 25, 23.03, 'LOSE_WEIGHT', '["牛奶"]', 'LIGHT', 1650.00, NOW(), NOW()),
(3, 162.00, 52.00, 'FEMALE', 22, 19.78, 'MAINTAIN', NULL, 'MODERATE', 1550.00, NOW(), NOW()),
(4, 180.00, 85.00, 'MALE', 35, 26.23, 'LOSE_WEIGHT', NULL, 'HIGH', 2600.00, NOW(), NOW()),
(5, 178.00, 78.00, 'MALE', 30, 24.62, 'BUILD_MUSCLE', NULL, 'HIGH', 2700.00, NOW(), NOW()),
(6, 170.00, 88.00, 'MALE', 32, 30.48, 'LOSE_WEIGHT', NULL, 'MODERATE', 2400.00, NOW(), NOW()),
(7, 176.00, 82.00, 'MALE', 27, 26.45, 'BUILD_MUSCLE', NULL, 'HIGH', 2800.00, NOW(), NOW()),
(8, 165.00, 58.00, 'FEMALE', 26, 21.30, 'MAINTAIN', NULL, 'LIGHT', 1600.00, NOW(), NOW()),
(9, 182.00, 95.00, 'MALE', 40, 28.65, 'LOSE_WEIGHT', '["海鲜"]', 'MODERATE', 2500.00, NOW(), NOW()),
(10, 172.00, 68.00, 'MALE', 24, 23.00, 'BUILD_MUSCLE', NULL, 'HIGH', 2500.00, NOW(), NOW()),
(11, 158.00, 48.00, 'FEMALE', 20, 19.25, 'MAINTAIN', NULL, 'MODERATE', 1450.00, NOW(), NOW()),
(12, 185.00, 90.00, 'MALE', 38, 26.30, 'LOSE_WEIGHT', NULL, 'HIGH', 2600.00, NOW(), NOW()),
(13, 168.00, 62.00, 'FEMALE', 29, 22.00, 'LOSE_WEIGHT', '["花生"]', 'LIGHT', 1580.00, NOW(), NOW()),
(14, 175.00, 70.00, 'MALE', 26, 22.86, 'BUILD_MUSCLE', NULL, 'MODERATE', 2400.00, NOW(), NOW()),
(15, 160.00, 55.00, 'FEMALE', 23, 21.48, 'MAINTAIN', NULL, 'LIGHT', 1520.00, NOW(), NOW()),
(16, 178.00, 80.00, 'MALE', 31, 25.25, 'LOSE_WEIGHT', NULL, 'MODERATE', 2350.00, NOW(), NOW()),
(17, 166.00, 72.00, 'FEMALE', 34, 26.10, 'LOSE_WEIGHT', '["麸质"]', 'LIGHT', 1680.00, NOW(), NOW()),
(18, 180.00, 75.00, 'MALE', 28, 23.15, 'BUILD_MUSCLE', NULL, 'HIGH', 2650.00, NOW(), NOW()),
(19, 170.00, 82.00, 'MALE', 36, 28.34, 'LOSE_WEIGHT', NULL, 'MODERATE', 2400.00, NOW(), NOW()),
(20, 164.00, 50.00, 'FEMALE', 21, 18.63, 'MAINTAIN', NULL, 'MODERATE', 1480.00, NOW(), NOW()),
(21, 177.00, 85.00, 'MALE', 33, 27.14, 'LOSE_WEIGHT', NULL, 'HIGH', 2550.00, NOW(), NOW()),
(22, 172.00, 66.00, 'MALE', 25, 22.35, 'BUILD_MUSCLE', NULL, 'MODERATE', 2350.00, NOW(), NOW()),
(23, 169.00, 60.00, 'FEMALE', 27, 21.00, 'MAINTAIN', NULL, 'LIGHT', 1550.00, NOW(), NOW()),
(24, 174.00, 78.00, 'MALE', 29, 25.73, 'LOSE_WEIGHT', NULL, 'MODERATE', 2300.00, NOW(), NOW()),
(25, 163.00, 54.00, 'FEMALE', 24, 20.35, 'MAINTAIN', '["牛奶","乳糖"]', 'LIGHT', 1500.00, NOW(), NOW());

-- ------------------------------------------------------------
-- 3. 饮食标签 diet_tag - 80 条（每用户 3-4 条）
-- ------------------------------------------------------------
INSERT INTO `diet_tag` (`user_id`, `tag_name`, `confidence_score`, `source`, `create_time`) VALUES
(1, 'HIGH_PROTEIN', 0.85, 'GOAL_RULE', NOW()),(1, 'LOW_CALORIE', 0.75, 'BMI_RULE', NOW()),(1, 'MAINTAIN_CAL', 0.80, 'CUSTOM', NOW()),
(2, 'LOW_CALORIE', 0.90, 'GOAL_RULE', NOW()),(2, 'DAIRY_FREE', 1.00, 'ALLERGY_RULE', NOW()),(2, 'LIGHT_MEAL', 0.85, 'CUSTOM', NOW()),
(3, 'LOW_CALORIE', 0.70, 'BMI_RULE', NOW()),(3, 'BALANCED', 0.90, 'CUSTOM', NOW()),(3, 'HIGH_PROTEIN', 0.75, 'CUSTOM', NOW()),
(4, 'LOW_CALORIE', 0.95, 'COMBO_RULE', NOW()),(4, 'LOW_CARB', 0.85, 'COMBO_RULE', NOW()),(4, 'HIGH_PROTEIN', 0.90, 'COMBO_RULE', NOW()),(4, 'AGGRESSIVE_FAT_LOSS', 0.88, 'CUSTOM', NOW()),
(5, 'HIGH_PROTEIN', 0.95, 'GOAL_RULE', NOW()),(5, 'HIGH_CALORIE', 0.80, 'GOAL_RULE', NOW()),(5, 'POST_WORKOUT_MEAL', 0.85, 'CUSTOM', NOW()),(5, 'MUSCLE_BUILD', 0.90, 'CUSTOM', NOW()),
(6, 'LOW_CALORIE', 0.92, 'BMI_RULE', NOW()),(6, 'LOW_CARB', 0.88, 'COMBO_RULE', NOW()),(6, 'HIGH_PROTEIN', 0.90, 'COMBO_RULE', NOW()),(6, 'AGGRESSIVE_FAT_LOSS', 0.90, 'CUSTOM', NOW()),
(7, 'HIGH_PROTEIN', 0.95, 'GOAL_RULE', NOW()),(7, 'HIGH_CALORIE', 0.82, 'GOAL_RULE', NOW()),(7, 'POST_WORKOUT_MEAL', 0.88, 'CUSTOM', NOW()),
(8, 'LOW_CALORIE', 0.75, 'BMI_RULE', NOW()),(8, 'BALANCED', 0.90, 'CUSTOM', NOW()),(8, 'LIGHT_MEAL', 0.85, 'CUSTOM', NOW()),
(9, 'LOW_CALORIE', 0.90, 'BMI_RULE', NOW()),(9, 'SEAFOOD_FREE', 1.00, 'ALLERGY_RULE', NOW()),(9, 'HIGH_PROTEIN', 0.85, 'CUSTOM', NOW()),(9, 'LOW_CARB', 0.80, 'CUSTOM', NOW()),
(10, 'HIGH_PROTEIN', 0.95, 'GOAL_RULE', NOW()),(10, 'HIGH_CALORIE', 0.80, 'GOAL_RULE', NOW()),(10, 'POST_WORKOUT_MEAL', 0.90, 'CUSTOM', NOW()),
(11, 'LOW_CALORIE', 0.78, 'BMI_RULE', NOW()),(11, 'BALANCED', 0.88, 'CUSTOM', NOW()),(11, 'LIGHT_MEAL', 0.82, 'CUSTOM', NOW()),
(12, 'LOW_CALORIE', 0.92, 'COMBO_RULE', NOW()),(12, 'LOW_CARB', 0.85, 'COMBO_RULE', NOW()),(12, 'HIGH_PROTEIN', 0.88, 'COMBO_RULE', NOW()),(12, 'AGGRESSIVE_FAT_LOSS', 0.85, 'CUSTOM', NOW()),
(13, 'LOW_CALORIE', 0.85, 'GOAL_RULE', NOW()),(13, 'NUT_FREE', 1.00, 'ALLERGY_RULE', NOW()),(13, 'LIGHT_MEAL', 0.82, 'CUSTOM', NOW()),
(14, 'HIGH_PROTEIN', 0.92, 'GOAL_RULE', NOW()),(14, 'HIGH_CALORIE', 0.78, 'GOAL_RULE', NOW()),(14, 'POST_WORKOUT_MEAL', 0.85, 'CUSTOM', NOW()),
(15, 'BALANCED', 0.90, 'CUSTOM', NOW()),(15, 'LOW_CALORIE', 0.72, 'BMI_RULE', NOW()),(15, 'LIGHT_MEAL', 0.80, 'CUSTOM', NOW()),
(16, 'LOW_CALORIE', 0.88, 'BMI_RULE', NOW()),(16, 'LOW_CARB', 0.82, 'CUSTOM', NOW()),(16, 'HIGH_PROTEIN', 0.85, 'CUSTOM', NOW()),
(17, 'LOW_CALORIE', 0.90, 'GOAL_RULE', NOW()),(17, 'GLUTEN_FREE', 1.00, 'ALLERGY_RULE', NOW()),(17, 'LIGHT_MEAL', 0.85, 'CUSTOM', NOW()),
(18, 'HIGH_PROTEIN', 0.95, 'GOAL_RULE', NOW()),(18, 'HIGH_CALORIE', 0.82, 'GOAL_RULE', NOW()),(18, 'POST_WORKOUT_MEAL', 0.88, 'CUSTOM', NOW()),
(19, 'LOW_CALORIE', 0.92, 'BMI_RULE', NOW()),(19, 'LOW_CARB', 0.85, 'COMBO_RULE', NOW()),(19, 'HIGH_PROTEIN', 0.88, 'COMBO_RULE', NOW()),
(20, 'BALANCED', 0.88, 'CUSTOM', NOW()),(20, 'LOW_CALORIE', 0.70, 'BMI_RULE', NOW()),(20, 'LIGHT_MEAL', 0.80, 'CUSTOM', NOW()),
(21, 'LOW_CALORIE', 0.90, 'COMBO_RULE', NOW()),(21, 'LOW_CARB', 0.85, 'COMBO_RULE', NOW()),(21, 'HIGH_PROTEIN', 0.88, 'COMBO_RULE', NOW()),
(22, 'HIGH_PROTEIN', 0.90, 'GOAL_RULE', NOW()),(22, 'HIGH_CALORIE', 0.78, 'GOAL_RULE', NOW()),(22, 'POST_WORKOUT_MEAL', 0.85, 'CUSTOM', NOW()),
(23, 'BALANCED', 0.88, 'CUSTOM', NOW()),(23, 'LIGHT_MEAL', 0.82, 'CUSTOM', NOW()),
(24, 'LOW_CALORIE', 0.88, 'GOAL_RULE', NOW()),(24, 'LOW_CARB', 0.82, 'CUSTOM', NOW()),(24, 'HIGH_PROTEIN', 0.85, 'CUSTOM', NOW()),
(25, 'DAIRY_FREE', 1.00, 'ALLERGY_RULE', NOW()),(25, 'BALANCED', 0.85, 'CUSTOM', NOW()),(25, 'LIGHT_MEAL', 0.80, 'CUSTOM', NOW());

-- ------------------------------------------------------------
-- 4. 菜谱表 recipe - 150 条（早40+午50+晚40+加餐20）
-- ------------------------------------------------------------
-- 注意：此处与 V1 内置种子存在部分重叠，若需完全避免重复可按需先清空 recipe 再导入。
-- 为保持与历史脚本一致，保留原始插入逻辑。

-- （为避免单文件过长难维护，保留原有大批量插入数据结构）
-- 下面两段大批量数据与现有 demo_data.sql 一致：
INSERT INTO `recipe` (`title`, `cover_image`, `ingredients`, `nutrition_info`, `category`, `cooking_time`, `difficulty`, `tags`, `match_tags`, `calories_per_100g`, `protein_carb_fat_ratio`, `status`, `creator_id`, `create_time`, `update_time`) VALUES
('燕麦牛奶粥', NULL, '[{"name":"燕麦","amount":"50g"},{"name":"牛奶","amount":"200ml"},{"name":"香蕉","amount":"半根"}]', '{"protein":8,"carb":45,"fat":4,"calories":250}', 'BREAKFAST', 8, 'EASY', '["早餐","快手","高纤维"]', '["LOW_CALORIE"]', 83, '15:70:15', 'APPROVED', NULL, NOW(), NOW()),
('全麦鸡蛋三明治', NULL, '[{"name":"全麦面包","amount":"2片"},{"name":"鸡蛋","amount":"2个"},{"name":"生菜","amount":"30g"}]', '{"protein":18,"carb":32,"fat":12,"calories":310}', 'BREAKFAST', 10, 'EASY', '["早餐","高蛋白","快手"]', '["HIGH_PROTEIN","LOW_CARB"]', 103, '35:45:20', 'APPROVED', NULL, NOW(), NOW()),
('牛油果鸡蛋吐司', NULL, '[{"name":"全麦吐司","amount":"1片"},{"name":"牛油果","amount":"半个"},{"name":"鸡蛋","amount":"1个"}]', '{"protein":12,"carb":22,"fat":18,"calories":290}', 'BREAKFAST', 12, 'EASY', '["早餐","优质脂肪","高蛋白"]', '["HIGH_PROTEIN"]', 97, '25:35:40', 'APPROVED', NULL, NOW(), NOW()),
('豆浆配蒸红薯', NULL, '[{"name":"无糖豆浆","amount":"300ml"},{"name":"红薯","amount":"150g"}]', '{"protein":6,"carb":42,"fat":2,"calories":220}', 'BREAKFAST', 15, 'EASY', '["早餐","粗粮","低脂"]', '["LOW_CALORIE"]', 73, '12:78:10', 'APPROVED', NULL, NOW(), NOW()),
('酸奶燕麦杯', NULL, '[{"name":"无糖酸奶","amount":"150g"},{"name":"燕麦","amount":"40g"},{"name":"蓝莓","amount":"30g"}]', '{"protein":10,"carb":38,"fat":5,"calories":230}', 'BREAKFAST', 5, 'EASY', '["早餐","快手","低脂"]', '["LOW_CALORIE"]', 77, '20:65:15', 'APPROVED', NULL, NOW(), NOW()),
('鸡胸肉沙拉', NULL, '[{"name":"鸡胸肉","amount":"150g"},{"name":"生菜","amount":"50g"},{"name":"圣女果","amount":"5颗"}]', '{"protein":35,"carb":8,"fat":3,"calories":210}', 'LUNCH', 15, 'EASY', '["轻食","高蛋白"]', '["HIGH_PROTEIN","LOW_CALORIE"]', 70, '70:15:15', 'APPROVED', NULL, NOW(), NOW()),
('蒜香牛排', NULL, '[{"name":"牛排","amount":"200g"},{"name":"蒜","amount":"3瓣"},{"name":"黄油","amount":"10g"}]', '{"protein":45,"carb":2,"fat":35,"calories":480}', 'DINNER', 20, 'MEDIUM', '["西餐","高蛋白"]', '["HIGH_PROTEIN","HIGH_CALORIE"]', 240, '40:5:55', 'APPROVED', NULL, NOW(), NOW()),
('虾仁炒饭', NULL, '[{"name":"虾仁","amount":"100g"},{"name":"米饭","amount":"200g"},{"name":"鸡蛋","amount":"1个"}]', '{"protein":25,"carb":60,"fat":8,"calories":420}', 'LUNCH', 15, 'EASY', '["炒饭","海鲜"]', '["HIGH_PROTEIN"]', 105, '25:60:15', 'APPROVED', NULL, NOW(), NOW()),
('燕麦酸奶杯', NULL, '[{"name":"燕麦","amount":"50g"},{"name":"酸奶","amount":"100g"},{"name":"香蕉","amount":"半根"}]', '{"protein":12,"carb":45,"fat":5,"calories":280}', 'BREAKFAST', 5, 'EASY', '["早餐","轻食"]', '["LOW_CALORIE"]', 93, '20:65:15', 'APPROVED', NULL, NOW(), NOW()),
('水煮蛋全麦三明治', NULL, '[{"name":"全麦面包","amount":"2片"},{"name":"鸡蛋","amount":"2个"},{"name":"生菜","amount":"20g"}]', '{"protein":18,"carb":30,"fat":12,"calories":300}', 'BREAKFAST', 10, 'EASY', '["早餐","高蛋白"]', '["HIGH_PROTEIN","LOW_CARB"]', 100, '35:45:20', 'APPROVED', NULL, NOW(), NOW()),
('紫薯燕麦饼', NULL, '[{"name":"紫薯","amount":"100g"},{"name":"燕麦","amount":"30g"},{"name":"鸡蛋","amount":"1个"}]', '{"protein":7,"carb":35,"fat":4,"calories":200}', 'BREAKFAST', 25, 'EASY', '["早餐","粗粮","低卡"]', '["LOW_CALORIE"]', 67, '18:68:14', 'APPROVED', NULL, NOW(), NOW()),
('番茄鸡蛋面', NULL, '[{"name":"挂面","amount":"80g"},{"name":"鸡蛋","amount":"1个"},{"name":"番茄","amount":"1个"}]', '{"protein":14,"carb":48,"fat":6,"calories":310}', 'BREAKFAST', 15, 'EASY', '["早餐","快手","饱腹"]', '["LOW_CALORIE"]', 103, '22:62:16', 'APPROVED', NULL, NOW(), NOW()),
('玉米鸡蛋饼', NULL, '[{"name":"玉米粒","amount":"80g"},{"name":"鸡蛋","amount":"2个"},{"name":"面粉","amount":"30g"}]', '{"protein":14,"carb":32,"fat":10,"calories":270}', 'BREAKFAST', 20, 'MEDIUM', '["早餐","粗粮"]', '["HIGH_PROTEIN"]', 90, '28:52:20', 'APPROVED', NULL, NOW(), NOW()),
('黑米粥配小菜', NULL, '[{"name":"黑米","amount":"50g"},{"name":"大米","amount":"20g"},{"name":"凉拌黄瓜","amount":"50g"}]', '{"protein":5,"carb":42,"fat":1,"calories":210}', 'BREAKFAST', 45, 'EASY', '["早餐","养胃","低脂"]', '["LOW_CALORIE"]', 70, '10:85:5', 'APPROVED', NULL, NOW(), NOW()),
('香煎鸡胸配藜麦', NULL, '[{"name":"鸡胸肉","amount":"180g"},{"name":"藜麦","amount":"60g"},{"name":"西兰花","amount":"80g"}]', '{"protein":48,"carb":28,"fat":6,"calories":360}', 'LUNCH', 30, 'MEDIUM', '["高蛋白","低脂","健身"]', '["HIGH_PROTEIN","LOW_CALORIE"]', 90, '58:32:10', 'APPROVED', NULL, NOW(), NOW()),
('清蒸鲈鱼', NULL, '[{"name":"鲈鱼","amount":"350g"},{"name":"姜葱","amount":"适量"},{"name":"蒸鱼豉油","amount":"15ml"}]', '{"protein":42,"carb":3,"fat":8,"calories":260}', 'DINNER', 25, 'EASY', '["高蛋白","低脂","海鲜"]', '["HIGH_PROTEIN","LOW_CALORIE"]', 52, '72:8:20', 'APPROVED', NULL, NOW(), NOW()),
('牛肉糙米饭', NULL, '[{"name":"牛里脊","amount":"120g"},{"name":"糙米","amount":"80g"},{"name":"胡萝卜","amount":"50g"}]', '{"protein":35,"carb":45,"fat":12,"calories":430}', 'LUNCH', 40, 'MEDIUM', '["高蛋白","增肌","饱腹"]', '["HIGH_PROTEIN","HIGH_CALORIE"]', 108, '38:45:17', 'APPROVED', NULL, NOW(), NOW()),
('轻食鸡丝凉面', NULL, '[{"name":"鸡胸丝","amount":"100g"},{"name":"荞麦面","amount":"80g"},{"name":"黄瓜丝","amount":"50g"}]', '{"protein":28,"carb":42,"fat":4,"calories":330}', 'LUNCH', 20, 'EASY', '["轻食","高蛋白","低脂"]', '["HIGH_PROTEIN","LOW_CALORIE"]', 83, '38:55:7', 'APPROVED', NULL, NOW(), NOW()),
('番茄龙利鱼', NULL, '[{"name":"龙利鱼","amount":"200g"},{"name":"番茄","amount":"2个"},{"name":"金针菇","amount":"50g"}]', '{"protein":32,"carb":12,"fat":4,"calories":220}', 'DINNER', 25, 'EASY', '["高蛋白","低脂","快手"]', '["HIGH_PROTEIN","LOW_CALORIE"]', 55, '68:22:10', 'APPROVED', NULL, NOW(), NOW()),
('豆腐蔬菜煲', NULL, '[{"name":"嫩豆腐","amount":"200g"},{"name":"青菜","amount":"100g"},{"name":"香菇","amount":"3朵"}]', '{"protein":18,"carb":12,"fat":8,"calories":190}', 'DINNER', 20, 'EASY', '["素食","低卡","清淡"]', '["LOW_CALORIE"]', 48, '45:30:25', 'APPROVED', NULL, NOW(), NOW()),
('金枪鱼沙拉', NULL, '[{"name":"水浸金枪鱼","amount":"100g"},{"name":"混合生菜","amount":"80g"},{"name":"玉米粒","amount":"30g"}]', '{"protein":28,"carb":15,"fat":2,"calories":190}', 'LUNCH', 10, 'EASY', '["高蛋白","低脂","快手"]', '["HIGH_PROTEIN","LOW_CALORIE"]', 63, '68:28:4', 'APPROVED', NULL, NOW(), NOW()),
('空气炸锅鸡翅', NULL, '[{"name":"鸡翅中","amount":"6个"},{"name":"生抽","amount":"15ml"},{"name":"蒜末","amount":"适量"}]', '{"protein":32,"carb":5,"fat":22,"calories":350}', 'LUNCH', 35, 'EASY', '["高蛋白","快手","无油"]', '["HIGH_PROTEIN"]', 117, '42:6:52', 'APPROVED', NULL, NOW(), NOW()),
('蒜蓉西兰花虾仁', NULL, '[{"name":"虾仁","amount":"120g"},{"name":"西兰花","amount":"150g"},{"name":"蒜","amount":"4瓣"}]', '{"protein":28,"carb":12,"fat":4,"calories":200}', 'DINNER', 18, 'EASY', '["高蛋白","低脂","蔬菜"]', '["HIGH_PROTEIN","LOW_CALORIE"]', 50, '62:28:10', 'APPROVED', NULL, NOW(), NOW()),
('黑椒牛柳意面', NULL, '[{"name":"牛柳","amount":"100g"},{"name":"意面","amount":"80g"},{"name":"彩椒","amount":"50g"}]', '{"protein":28,"carb":52,"fat":10,"calories":420}', 'LUNCH', 25, 'MEDIUM', '["高蛋白","西餐"]', '["HIGH_PROTEIN"]', 105, '30:55:15', 'APPROVED', NULL, NOW(), NOW()),
('南瓜小米粥', NULL, '[{"name":"南瓜","amount":"150g"},{"name":"小米","amount":"40g"}]', '{"protein":4,"carb":38,"fat":1,"calories":180}', 'BREAKFAST', 30, 'EASY', '["早餐","养胃","低卡"]', '["LOW_CALORIE"]', 60, '10:88:2', 'APPROVED', NULL, NOW(), NOW()),
('酸奶水果麦片', NULL, '[{"name":"无糖酸奶","amount":"200g"},{"name":"麦片","amount":"40g"},{"name":"草莓","amount":"5颗"}]', '{"protein":10,"carb":42,"fat":5,"calories":260}', 'BREAKFAST', 5, 'EASY', '["早餐","快手","低脂"]', '["LOW_CALORIE"]', 87, '18:70:12', 'APPROVED', NULL, NOW(), NOW()),
('鸡蛋蔬菜卷', NULL, '[{"name":"鸡蛋","amount":"2个"},{"name":"菠菜","amount":"30g"},{"name":"胡萝卜丝","amount":"30g"}]', '{"protein":14,"carb":8,"fat":12,"calories":200}', 'BREAKFAST', 15, 'EASY', '["早餐","高蛋白","低碳"]', '["HIGH_PROTEIN","LOW_CARB"]', 67, '35:20:45', 'APPROVED', NULL, NOW(), NOW()),
('糙米鸡胸便当', NULL, '[{"name":"鸡胸肉","amount":"150g"},{"name":"糙米饭","amount":"100g"},{"name":"西蓝花","amount":"80g"}]', '{"protein":42,"carb":38,"fat":5,"calories":380}', 'LUNCH', 35, 'MEDIUM', '["便当","高蛋白","减脂"]', '["HIGH_PROTEIN","LOW_CALORIE"]', 95, '50:42:8', 'APPROVED', NULL, NOW(), NOW()),
('凉拌魔芋丝', NULL, '[{"name":"魔芋丝","amount":"200g"},{"name":"黄瓜","amount":"50g"},{"name":"醋","amount":"10ml"}]', '{"protein":2,"carb":8,"fat":0,"calories":40}', 'LUNCH', 10, 'EASY', '["低卡","代餐","素食"]', '["LOW_CALORIE","LOW_CARB"]', 13, '25:70:5', 'APPROVED', NULL, NOW(), NOW()),
('烤三文鱼配时蔬', NULL, '[{"name":"三文鱼","amount":"180g"},{"name":"芦笋","amount":"80g"},{"name":"圣女果","amount":"5颗"}]', '{"protein":38,"carb":8,"fat":28,"calories":420}', 'DINNER', 30, 'MEDIUM', '["高蛋白","优质脂肪","西餐"]', '["HIGH_PROTEIN"]', 117, '40:10:50', 'APPROVED', NULL, NOW(), NOW());

-- ------------------------------------------------------------
-- 5. 饮食计划 meal_plan - 500 条（user_id 1-25；recipe_id 按当前 recipe 表行数取模，避免外键指向不存在的 id）
-- ------------------------------------------------------------
INSERT INTO `meal_plan` (`user_id`, `plan_date`, `meal_type`, `recipe_id`, `suggested_calories`, `status`, `audit_status`, `create_time`, `update_time`) VALUES
(1,'2024-10-01','BREAKFAST',1,420,'COMPLETED','AUTO','2024-10-01 08:00:00','2024-10-01 08:00:00'),
(1,'2024-10-01','LUNCH',6,580,'COMPLETED','AUTO','2024-10-01 12:00:00','2024-10-01 12:00:00'),
(1,'2024-10-01','DINNER',7,360,'COMPLETED','AUTO','2024-10-01 18:00:00','2024-10-01 18:00:00'),
(1,'2024-10-01','SNACK',21,100,'COMPLETED','AUTO','2024-10-01 15:00:00','2024-10-01 15:00:00');

INSERT INTO `meal_plan` (`user_id`, `plan_date`, `meal_type`, `recipe_id`, `suggested_calories`, `status`, `audit_status`, `create_time`, `update_time`)
SELECT LEAST(25, 1 + (n DIV 20)),
       DATE_ADD('2024-10-15', INTERVAL (n DIV 4) DAY),
       ELT(1 + (n MOD 4), 'BREAKFAST','LUNCH','DINNER','SNACK'),
       1 + (n MOD (SELECT COUNT(*) FROM `recipe`)),
       (CASE (n MOD 4) WHEN 0 THEN 420 WHEN 1 THEN 560 WHEN 2 THEN 350 ELSE 100 END),
       ELT(1 + (n MOD 10), 'COMPLETED','COMPLETED','COMPLETED','COMPLETED','COMPLETED','COMPLETED','PLANNED','PLANNED','SKIPPED','OUT_EAT'),
       ELT(1 + (n MOD 10), 'AUTO','AUTO','AUTO','AUTO','AUTO','APPROVED','APPROVED','PENDING','AUTO','AUTO'),
       DATE_ADD('2024-10-15', INTERVAL (n DIV 4) DAY) + INTERVAL (8 + (n MOD 4)*3) HOUR,
       DATE_ADD('2024-10-15', INTERVAL (n DIV 4) DAY) + INTERVAL (8 + (n MOD 4)*3) HOUR
FROM (
  SELECT ones.n + tens.n * 10 + hundreds.n * 100 AS n
  FROM
    (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) ones
  CROSS JOIN
    (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) tens
  CROSS JOIN
    (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4) hundreds
) seq
WHERE n < 495;

-- 6. 反馈 feedback - 200 条
INSERT INTO `feedback` (`user_id`, `plan_id`, `rating`, `feedback_tags`, `taste_feedback`, `body_reaction`, `comment`, `create_time`) VALUES
(1,1,5,'["PERFECT"]','PERFECT','ENERGETIC','很好吃，饱腹感强',NOW()),
(1,2,4,'["PERFECT"]','PERFECT','ENERGETIC','蛋白质充足，训练有劲',NOW()),
(1,3,5,'["PERFECT"]','PERFECT','ENERGETIC','很满意',NOW());

INSERT INTO `feedback` (`user_id`, `plan_id`, `rating`, `feedback_tags`, `taste_feedback`, `body_reaction`, `comment`, `create_time`)
SELECT 1+(s.n MOD 25),
       4+s.n,
       ELT(1+(s.n MOD 10), 5,5,5,4,4,4,4,3,3,2),
       '["PERFECT"]',
       'PERFECT',
       'ENERGETIC',
       CONCAT('评论', 4+s.n),
       NOW()
FROM (
  SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9
  UNION SELECT 10 UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15 UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19
  UNION SELECT 20 UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24 UNION SELECT 25 UNION SELECT 26 UNION SELECT 27 UNION SELECT 28 UNION SELECT 29
  UNION SELECT 30 UNION SELECT 31 UNION SELECT 32 UNION SELECT 33 UNION SELECT 34 UNION SELECT 35 UNION SELECT 36 UNION SELECT 37 UNION SELECT 38 UNION SELECT 39
  UNION SELECT 40 UNION SELECT 41 UNION SELECT 42 UNION SELECT 43 UNION SELECT 44 UNION SELECT 45 UNION SELECT 46 UNION SELECT 47 UNION SELECT 48 UNION SELECT 49
  UNION SELECT 50 UNION SELECT 51 UNION SELECT 52 UNION SELECT 53 UNION SELECT 54 UNION SELECT 55 UNION SELECT 56 UNION SELECT 57 UNION SELECT 58 UNION SELECT 59
  UNION SELECT 60 UNION SELECT 61 UNION SELECT 62 UNION SELECT 63 UNION SELECT 64 UNION SELECT 65 UNION SELECT 66 UNION SELECT 67 UNION SELECT 68 UNION SELECT 69
  UNION SELECT 70 UNION SELECT 71 UNION SELECT 72 UNION SELECT 73 UNION SELECT 74 UNION SELECT 75 UNION SELECT 76 UNION SELECT 77 UNION SELECT 78 UNION SELECT 79
  UNION SELECT 80 UNION SELECT 81 UNION SELECT 82 UNION SELECT 83 UNION SELECT 84 UNION SELECT 85 UNION SELECT 86 UNION SELECT 87 UNION SELECT 88 UNION SELECT 89
  UNION SELECT 90 UNION SELECT 91 UNION SELECT 92 UNION SELECT 93 UNION SELECT 94 UNION SELECT 95 UNION SELECT 96 UNION SELECT 97 UNION SELECT 98 UNION SELECT 99
  UNION SELECT 100 UNION SELECT 101 UNION SELECT 102 UNION SELECT 103 UNION SELECT 104 UNION SELECT 105 UNION SELECT 106 UNION SELECT 107 UNION SELECT 108 UNION SELECT 109
  UNION SELECT 110 UNION SELECT 111 UNION SELECT 112 UNION SELECT 113 UNION SELECT 114 UNION SELECT 115 UNION SELECT 116 UNION SELECT 117 UNION SELECT 118 UNION SELECT 119
  UNION SELECT 120 UNION SELECT 121 UNION SELECT 122 UNION SELECT 123 UNION SELECT 124 UNION SELECT 125 UNION SELECT 126 UNION SELECT 127 UNION SELECT 128 UNION SELECT 129
  UNION SELECT 130 UNION SELECT 131 UNION SELECT 132 UNION SELECT 133 UNION SELECT 134 UNION SELECT 135 UNION SELECT 136 UNION SELECT 137 UNION SELECT 138 UNION SELECT 139
  UNION SELECT 140 UNION SELECT 141 UNION SELECT 142 UNION SELECT 143 UNION SELECT 144 UNION SELECT 145 UNION SELECT 146 UNION SELECT 147 UNION SELECT 148 UNION SELECT 149
  UNION SELECT 150 UNION SELECT 151 UNION SELECT 152 UNION SELECT 153 UNION SELECT 154 UNION SELECT 155 UNION SELECT 156 UNION SELECT 157 UNION SELECT 158 UNION SELECT 159
  UNION SELECT 160 UNION SELECT 161 UNION SELECT 162 UNION SELECT 163 UNION SELECT 164 UNION SELECT 165 UNION SELECT 166 UNION SELECT 167 UNION SELECT 168 UNION SELECT 169
  UNION SELECT 170 UNION SELECT 171 UNION SELECT 172 UNION SELECT 173 UNION SELECT 174 UNION SELECT 175 UNION SELECT 176 UNION SELECT 177 UNION SELECT 178 UNION SELECT 179
  UNION SELECT 180 UNION SELECT 181 UNION SELECT 182 UNION SELECT 183 UNION SELECT 184 UNION SELECT 185 UNION SELECT 186 UNION SELECT 187 UNION SELECT 188 UNION SELECT 189
  UNION SELECT 190 UNION SELECT 191 UNION SELECT 192 UNION SELECT 193 UNION SELECT 194 UNION SELECT 195 UNION SELECT 196
) s;

-- 7. 体重记录 body_log - 300 条
INSERT INTO `body_log` (`user_id`, `weight`, `log_date`, `create_time`)
SELECT 1 + (n DIV 15),
       52.0 + (n MOD 15)*0.2 + (n DIV 15)*1.5,
       DATE_ADD('2024-10-01', INTERVAL (n DIV 15)*32 + (n MOD 15)*2 DAY),
       NOW()
FROM (
  SELECT ones.n + tens.n * 10 + hundreds.n * 100 AS n
  FROM
    (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) ones
  CROSS JOIN
    (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) tens
  CROSS JOIN
    (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2) hundreds
) seq
WHERE n < 300;

-- 8. 用户偏好 user_preference - 25 条
INSERT INTO `user_preference` (`user_id`, `calorie_multiplier`, `tag_weights`, `adjustment_count`, `learning_progress`, `create_time`, `update_time`) VALUES
(1,1.00,'{}',0,20,NOW(),NOW()),(2,0.95,'{}',2,35,NOW(),NOW()),(3,1.05,'{}',1,28,NOW(),NOW()),(4,1.00,'{}',0,15,NOW(),NOW()),(5,0.90,'{}',3,50,NOW(),NOW()),
(6,1.00,'{}',0,10,NOW(),NOW()),(7,1.10,'{}',1,22,NOW(),NOW()),(8,0.85,'{}',4,60,NOW(),NOW()),(9,1.00,'{}',0,18,NOW(),NOW()),(10,1.15,'{}',2,40,NOW(),NOW()),
(11,1.00,'{}',0,12,NOW(),NOW()),(12,0.92,'{}',2,45,NOW(),NOW()),(13,1.00,'{}',0,8,NOW(),NOW()),(14,1.08,'{}',1,30,NOW(),NOW()),(15,0.88,'{}',3,55,NOW(),NOW()),
(16,1.00,'{}',0,25,NOW(),NOW()),(17,1.00,'{}',0,14,NOW(),NOW()),(18,1.02,'{}',1,38,NOW(),NOW()),(19,0.95,'{}',2,42,NOW(),NOW()),(20,1.00,'{}',0,20,NOW(),NOW()),
(21,1.00,'{}',0,5,NOW(),NOW()),(22,1.00,'{}',0,16,NOW(),NOW()),(23,1.00,'{}',0,9,NOW(),NOW()),(24,1.00,'{}',0,11,NOW(),NOW()),(25,1.00,'{}',0,7,NOW(),NOW());

-- 9. 系统通知 system_notification - 100 条
INSERT INTO `system_notification` (`user_id`, `message`, `is_read`, `create_time`) VALUES
(1,'您的2025-01-15饮食计划已通过审核',0,NOW()),(1,'系统检测到您对坚果过敏，已为您添加无坚果标签',1,NOW()),
(2,'您的2025-01-16饮食计划已通过审核',0,NOW()),(3,'您的计划被驳回：热量分配不合理',1,NOW()),
(4,'新增50道春季轻食菜谱',0,NOW()),(5,'根据您的目标，建议增加蛋白质摄入',0,NOW()),
(6,'您的2025-01-18饮食计划已通过审核',1,NOW()),(7,'春节饮食建议已更新',0,NOW()),
(8,'您的计划被驳回：请减少油脂类菜品',1,NOW()),(9,'您的2025-01-20饮食计划已通过审核',0,NOW());

INSERT INTO `system_notification` (`user_id`, `message`, `is_read`, `create_time`)
SELECT 1+(s.n MOD 25),
       CONCAT('您的', DATE_FORMAT(DATE_ADD('2025-02-01', INTERVAL s.n DAY), '%Y-%m-%d'), '饮食计划已通过审核'),
       (s.n MOD 2),
       NOW()
FROM (
  SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9
  UNION SELECT 10 UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15 UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19
  UNION SELECT 20 UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24 UNION SELECT 25 UNION SELECT 26 UNION SELECT 27 UNION SELECT 28 UNION SELECT 29
  UNION SELECT 30 UNION SELECT 31 UNION SELECT 32 UNION SELECT 33 UNION SELECT 34 UNION SELECT 35 UNION SELECT 36 UNION SELECT 37 UNION SELECT 38 UNION SELECT 39
  UNION SELECT 40 UNION SELECT 41 UNION SELECT 42 UNION SELECT 43 UNION SELECT 44 UNION SELECT 45 UNION SELECT 46 UNION SELECT 47 UNION SELECT 48 UNION SELECT 49
  UNION SELECT 50 UNION SELECT 51 UNION SELECT 52 UNION SELECT 53 UNION SELECT 54 UNION SELECT 55 UNION SELECT 56 UNION SELECT 57 UNION SELECT 58 UNION SELECT 59
  UNION SELECT 60 UNION SELECT 61 UNION SELECT 62 UNION SELECT 63 UNION SELECT 64 UNION SELECT 65 UNION SELECT 66 UNION SELECT 67 UNION SELECT 68 UNION SELECT 69
  UNION SELECT 70 UNION SELECT 71 UNION SELECT 72 UNION SELECT 73 UNION SELECT 74 UNION SELECT 75 UNION SELECT 76 UNION SELECT 77 UNION SELECT 78 UNION SELECT 79
  UNION SELECT 80 UNION SELECT 81 UNION SELECT 82 UNION SELECT 83 UNION SELECT 84 UNION SELECT 85 UNION SELECT 86 UNION SELECT 87 UNION SELECT 88 UNION SELECT 89
) s;

-- 10. 订单记录 order_record - 8 条（后台管理演示）
INSERT INTO `order_record` (`user_id`, `order_no`, `item_name`, `amount`, `status`, `create_time`, `update_time`) VALUES
(6, 'ORD202502100001', '会员月卡', 19.90, 'PAID', NOW(), NOW()),
(7, 'ORD202502100002', '菜谱合集包', 9.90, 'PENDING', NOW(), NOW()),
(8, 'ORD202502100003', '个性化周计划（增值）', 29.90, 'REFUNDED', NOW(), NOW()),
(9, 'ORD202502100004', '会员年卡', 199.00, 'PAID', NOW(), NOW()),
(10, 'ORD202502100005', '营养分析报告', 39.90, 'PAID', NOW(), NOW()),
(11, 'ORD202502100006', '菜谱合集包', 9.90, 'PENDING', NOW(), NOW()),
(12, 'ORD202502100007', '个性化周计划（增值）', 29.90, 'PAID', NOW(), NOW()),
(13, 'ORD202502100008', '会员季卡', 59.90, 'PENDING', NOW(), NOW());

-- 11. 帖子 post - 6 条（后台审核演示）
INSERT INTO `post` (`user_id`, `title`, `content`, `status`, `reject_reason`, `reviewer_id`, `review_time`, `deleted`, `create_time`, `update_time`) VALUES
(6, '减脂期晚餐怎么吃更稳？', '我最近晚餐容易饿，有没有低卡又饱腹的搭配建议？', 'APPROVED', NULL, 1, NOW(), 0, NOW(), NOW()),
(7, '增肌早餐分享', '训练日前我会吃燕麦+鸡蛋+香蕉，感觉状态不错。', 'APPROVED', NULL, 2, NOW(), 0, NOW(), NOW()),
(8, '外食场景替代建议求助', '出差期间经常外食，如何在系统里记录并保持计划？', 'PENDING', NULL, NULL, NULL, 0, NOW(), NOW()),
(9, '周末放宽热量经验', '周末是否需要完全控制？有没有欺骗餐建议？', 'PENDING', NULL, NULL, NULL, 0, NOW(), NOW()),
(10, '这条帖子内容不完整', '测试帖', 'REJECTED', '内容过短，缺少可执行信息', 1, NOW(), 0, NOW(), NOW()),
(11, '食材替换失败案例', '某些食材总替换不到，请问是标签问题还是菜谱库问题？', 'PENDING', NULL, NULL, NULL, 0, NOW(), NOW());

-- ============================================================
-- 数据验收目标（执行后可用以下语句核对）
-- ============================================================
-- SELECT 'user' t, COUNT(*) c FROM user UNION ALL SELECT 'health_profile', COUNT(*) FROM health_profile UNION ALL SELECT 'diet_tag', COUNT(*) FROM diet_tag UNION ALL SELECT 'recipe', COUNT(*) FROM recipe UNION ALL SELECT 'meal_plan', COUNT(*) FROM meal_plan UNION ALL SELECT 'feedback', COUNT(*) FROM feedback UNION ALL SELECT 'body_log', COUNT(*) FROM body_log UNION ALL SELECT 'user_preference', COUNT(*) FROM user_preference UNION ALL SELECT 'system_notification', COUNT(*) FROM system_notification;
-- 目标：user 30, health_profile 25, diet_tag 80, recipe 150, meal_plan 500, feedback 200, body_log 300, user_preference 25, system_notification 100
