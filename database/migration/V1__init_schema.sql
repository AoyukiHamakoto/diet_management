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
