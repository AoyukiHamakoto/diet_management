-- ============================================================
-- 健身饮食规划系统 - 演示数据（答辩/演示用）
-- 执行前请先建表：启动后端（Flyway）或执行 database/schema.sql（内部引用 V1__init_schema.sql）
-- 字符集：utf8mb4
-- ============================================================

USE `diet_management`;
SET NAMES utf8mb4;

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

INSERT INTO `recipe` (`title`, `cover_image`, `ingredients`, `nutrition_info`, `category`, `cooking_time`, `difficulty`, `tags`, `match_tags`, `calories_per_100g`, `protein_carb_fat_ratio`, `status`, `creator_id`, `create_time`, `update_time`) VALUES
('芝士培根三明治', NULL, '[{"name":"全麦面包","amount":"2片"},{"name":"培根","amount":"2片"},{"name":"芝士","amount":"1片"}]', '{"protein":18,"carb":28,"fat":22,"calories":380}', 'BREAKFAST', 12, 'EASY', '["早餐","高蛋白"]', '["HIGH_PROTEIN","HIGH_CALORIE"]', 127, '25:35:40', 'APPROVED', 6, NOW(), NOW()),
('皮蛋瘦肉粥', NULL, '[{"name":"大米","amount":"60g"},{"name":"猪瘦肉","amount":"50g"},{"name":"皮蛋","amount":"1个"}]', '{"protein":14,"carb":35,"fat":6,"calories":260}', 'BREAKFAST', 45, 'MEDIUM', '["早餐","养胃"]', '["LOW_CALORIE"]', 87, '28:58:14', 'APPROVED', NULL, NOW(), NOW()),
('蔬菜鸡蛋饼', NULL, '[{"name":"鸡蛋","amount":"2个"},{"name":"菠菜","amount":"40g"},{"name":"胡萝卜","amount":"30g"}]', '{"protein":12,"carb":10,"fat":14,"calories":210}', 'BREAKFAST', 15, 'EASY', '["早餐","高蛋白","蔬菜"]', '["HIGH_PROTEIN","LOW_CARB"]', 70, '35:22:43', 'APPROVED', NULL, NOW(), NOW()),
('香煎鸡胸肉配藜麦饭', NULL, '[{"name":"鸡胸肉","amount":"200g"},{"name":"藜麦","amount":"70g"},{"name":"西兰花","amount":"80g"}]', '{"protein":52,"carb":32,"fat":6,"calories":400}', 'LUNCH', 35, 'MEDIUM', '["高蛋白","低脂","健身餐"]', '["HIGH_PROTEIN","LOW_CALORIE"]', 100, '55:35:10', 'APPROVED', NULL, NOW(), NOW()),
('番茄炖牛腩', NULL, '[{"name":"牛腩","amount":"150g"},{"name":"番茄","amount":"2个"},{"name":"土豆","amount":"80g"}]', '{"protein":32,"carb":25,"fat":18,"calories":400}', 'LUNCH', 60, 'HARD', '["高蛋白","炖菜"]', '["HIGH_PROTEIN"]', 100, '45:28:27', 'APPROVED', NULL, NOW(), NOW()),
('清炒时蔬配糙米饭', NULL, '[{"name":"糙米","amount":"80g"},{"name":"油菜","amount":"100g"},{"name":"木耳","amount":"30g"}]', '{"protein":8,"carb":42,"fat":3,"calories":230}', 'LUNCH', 25, 'EASY', '["素食","低卡","粗粮"]', '["LOW_CALORIE"]', 58, '15:78:7', 'APPROVED', NULL, NOW(), NOW()),
('照烧鸡腿饭', NULL, '[{"name":"鸡腿","amount":"1只"},{"name":"米饭","amount":"120g"},{"name":"青菜","amount":"50g"}]', '{"protein":35,"carb":55,"fat":14,"calories":480}', 'LUNCH', 40, 'MEDIUM', '["高蛋白","日式","便当"]', '["HIGH_PROTEIN"]', 120, '32:50:18', 'APPROVED', NULL, NOW(), NOW()),
('酸辣土豆丝配馒头', NULL, '[{"name":"土豆","amount":"150g"},{"name":"馒头","amount":"1个"},{"name":"青椒","amount":"30g"}]', '{"protein":8,"carb":52,"fat":2,"calories":260}', 'LUNCH', 20, 'EASY', '["快手","素食"]', '["LOW_CALORIE"]', 65, '14:82:4', 'APPROVED', NULL, NOW(), NOW()),
('水煮鱼片', NULL, '[{"name":"草鱼片","amount":"200g"},{"name":"豆芽","amount":"100g"},{"name":"干辣椒","amount":"5个"}]', '{"protein":36,"carb":8,"fat":12,"calories":290}', 'DINNER', 35, 'MEDIUM', '["高蛋白","川菜","低脂"]', '["HIGH_PROTEIN"]', 73, '55:12:33', 'APPROVED', NULL, NOW(), NOW()),
('蒜蓉粉丝蒸虾', NULL, '[{"name":"大虾","amount":"12只"},{"name":"粉丝","amount":"50g"},{"name":"蒜","amount":"1头"}]', '{"protein":32,"carb":22,"fat":4,"calories":260}', 'DINNER', 25, 'MEDIUM', '["高蛋白","海鲜","清淡"]', '["HIGH_PROTEIN","LOW_CALORIE"]', 65, '55:38:7', 'APPROVED', NULL, NOW(), NOW()),
('香蕉', NULL, '[{"name":"香蕉","amount":"1根"}]', '{"protein":1,"carb":27,"fat":0,"calories":105}', 'SNACK', 0, 'EASY', '["加餐","水果","快手"]', '["LOW_CALORIE"]', 89, '5:93:2', 'APPROVED', NULL, NOW(), NOW()),
('混合坚果', NULL, '[{"name":"核桃","amount":"15g"},{"name":"杏仁","amount":"15g"},{"name":"腰果","amount":"10g"}]', '{"protein":5,"carb":8,"fat":18,"calories":200}', 'SNACK', 0, 'EASY', '["加餐","优质脂肪"]', '["HIGH_CALORIE"]', 667, '12:18:70', 'APPROVED', NULL, NOW(), NOW()),
('无糖酸奶', NULL, '[{"name":"无糖酸奶","amount":"200g"}]', '{"protein":8,"carb":12,"fat":4,"calories":120}', 'SNACK', 0, 'EASY', '["加餐","低卡","蛋白质"]', '["LOW_CALORIE","HIGH_PROTEIN"]', 60, '35:45:20', 'APPROVED', NULL, NOW(), NOW()),
('全麦饼干', NULL, '[{"name":"全麦饼干","amount":"40g"}]', '{"protein":4,"carb":28,"fat":6,"calories":180}', 'SNACK', 0, 'EASY', '["加餐","粗粮"]', '["LOW_CALORIE"]', 450, '10:68:22', 'APPROVED', NULL, NOW(), NOW()),
('水煮蛋', NULL, '[{"name":"鸡蛋","amount":"2个"}]', '{"protein":12,"carb":1,"fat":10,"calories":155}', 'SNACK', 10, 'EASY', '["加餐","高蛋白","快手"]', '["HIGH_PROTEIN","LOW_CARB"]', 155, '50:5:45', 'APPROVED', NULL, NOW(), NOW()),
('苹果', NULL, '[{"name":"苹果","amount":"1个"}]', '{"protein":0,"carb":25,"fat":0,"calories":95}', 'SNACK', 0, 'EASY', '["加餐","水果"]', '["LOW_CALORIE"]', 52, '0:100:0', 'APPROVED', NULL, NOW(), NOW()),
('蛋白棒', NULL, '[{"name":"蛋白棒","amount":"1根60g"}]', '{"protein":20,"carb":25,"fat":8,"calories":250}', 'SNACK', 0, 'EASY', '["加餐","高蛋白","健身"]', '["HIGH_PROTEIN"]', 417, '45:45:10', 'APPROVED', NULL, NOW(), NOW()),
('黄瓜条', NULL, '[{"name":"黄瓜","amount":"1根"}]', '{"protein":1,"carb":4,"fat":0,"calories":16}', 'SNACK', 0, 'EASY', '["加餐","低卡","蔬菜"]', '["LOW_CALORIE","LOW_CARB"]', 11, '25:70:5', 'APPROVED', NULL, NOW(), NOW()),
('牛奶', NULL, '[{"name":"牛奶","amount":"250ml"}]', '{"protein":8,"carb":12,"fat":8,"calories":155}', 'SNACK', 0, 'EASY', '["加餐","高蛋白","补钙"]', '["HIGH_PROTEIN"]', 62, '35:35:30', 'APPROVED', NULL, NOW(), NOW()),
('紫薯', NULL, '[{"name":"紫薯","amount":"150g"}]', '{"protein":2,"carb":35,"fat":0,"calories":140}', 'SNACK', 15, 'EASY', '["加餐","粗粮","低脂"]', '["LOW_CALORIE"]', 93, '6:92:2', 'APPROVED', NULL, NOW(), NOW());

-- 更多早餐/午餐/晚餐/加餐（简写标题，凑满 150 条；id 51-150 由自增生成）
INSERT INTO `recipe` (`title`, `cover_image`, `ingredients`, `nutrition_info`, `category`, `cooking_time`, `difficulty`, `tags`, `match_tags`, `calories_per_100g`, `protein_carb_fat_ratio`, `status`, `creator_id`, `create_time`, `update_time`) VALUES
('玉米粥', NULL, '[{"name":"玉米碴","amount":"50g"}]', '{"protein":4,"carb":42,"fat":2,"calories":210}', 'BREAKFAST', 25, 'EASY', '["早餐","粗粮"]', '["LOW_CALORIE"]', 70, '10:85:5', 'APPROVED', NULL, NOW(), NOW()),
('煎饺', NULL, '[{"name":"饺子","amount":"8个"}]', '{"protein":12,"carb":35,"fat":14,"calories":320}', 'BREAKFAST', 15, 'EASY', '["早餐","中式"]', '["HIGH_PROTEIN"]', 107, '18:52:30', 'APPROVED', NULL, NOW(), NOW()),
('葱油拌面', NULL, '[{"name":"挂面","amount":"100g"},{"name":"小葱","amount":"20g"}]', '{"protein":10,"carb":55,"fat":12,"calories":380}', 'LUNCH', 15, 'EASY', '["快手","主食"]', '["LOW_CALORIE"]', 95, '12:62:26', 'APPROVED', NULL, NOW(), NOW()),
('青椒肉丝盖饭', NULL, '[{"name":"猪里脊","amount":"80g"},{"name":"青椒","amount":"50g"},{"name":"米饭","amount":"150g"}]', '{"protein":22,"carb":55,"fat":8,"calories":380}', 'LUNCH', 25, 'EASY', '["高蛋白","下饭"]', '["HIGH_PROTEIN"]', 95, '28:62:10', 'APPROVED', NULL, NOW(), NOW()),
('麻婆豆腐', NULL, '[{"name":"豆腐","amount":"200g"},{"name":"肉末","amount":"50g"}]', '{"protein":18,"carb":8,"fat":14,"calories":240}', 'LUNCH', 20, 'MEDIUM', '["川菜","下饭"]', '["HIGH_PROTEIN"]', 60, '45:18:37', 'APPROVED', NULL, NOW(), NOW()),
('红烧茄子', NULL, '[{"name":"茄子","amount":"200g"},{"name":"蒜","amount":"适量"}]', '{"protein":3,"carb":18,"fat":12,"calories":180}', 'DINNER', 25, 'EASY', '["素食","下饭"]', '["LOW_CALORIE"]', 45, '8:45:47', 'APPROVED', NULL, NOW(), NOW()),
('冬瓜排骨汤', NULL, '[{"name":"排骨","amount":"100g"},{"name":"冬瓜","amount":"150g"}]', '{"protein":18,"carb":6,"fat":12,"calories":220}', 'DINNER', 50, 'MEDIUM', '["汤品","清淡"]', '["HIGH_PROTEIN"]', 55, '45:12:43', 'APPROVED', NULL, NOW(), NOW()),
('西红柿鸡蛋面', NULL, '[{"name":"挂面","amount":"90g"},{"name":"鸡蛋","amount":"1个"},{"name":"番茄","amount":"1个"}]', '{"protein":14,"carb":48,"fat":8,"calories":330}', 'LUNCH', 20, 'EASY', '["快手","饱腹"]', '["LOW_CALORIE"]', 83, '20:62:18', 'APPROVED', NULL, NOW(), NOW()),
('凉拌木耳', NULL, '[{"name":"木耳","amount":"80g"},{"name":"醋","amount":"10ml"}]', '{"protein":2,"carb":12,"fat":0,"calories":55}', 'LUNCH', 10, 'EASY', '["低卡","凉菜"]', '["LOW_CALORIE"]', 28, '18:78:4', 'APPROVED', NULL, NOW(), NOW()),
('蛋炒饭', NULL, '[{"name":"隔夜饭","amount":"200g"},{"name":"鸡蛋","amount":"2个"}]', '{"protein":14,"carb":55,"fat":10,"calories":380}', 'LUNCH', 12, 'EASY', '["快手","主食"]', '["HIGH_PROTEIN"]', 95, '18:62:20', 'APPROVED', NULL, NOW(), NOW()),
('西兰花炒虾仁', NULL, '[{"name":"虾仁","amount":"100g"},{"name":"西兰花","amount":"150g"}]', '{"protein":26,"carb":12,"fat":4,"calories":200}', 'DINNER', 18, 'EASY', '["高蛋白","低脂"]', '["HIGH_PROTEIN","LOW_CALORIE"]', 50, '58:28:14', 'APPROVED', NULL, NOW(), NOW()),
('番茄蛋花汤', NULL, '[{"name":"番茄","amount":"1个"},{"name":"鸡蛋","amount":"1个"}]', '{"protein":8,"carb":8,"fat":6,"calories":120}', 'DINNER', 15, 'EASY', '["汤品","快手"]', '["LOW_CALORIE"]', 40, '35:30:35', 'APPROVED', NULL, NOW(), NOW()),
('燕麦饼干', NULL, '[{"name":"燕麦","amount":"50g"},{"name":"香蕉","amount":"1根"}]', '{"protein":6,"carb":45,"fat":6,"calories":250}', 'SNACK', 25, 'EASY', '["加餐","粗粮"]', '["LOW_CALORIE"]', 83, '12:78:10', 'APPROVED', NULL, NOW(), NOW()),
('葡萄', NULL, '[{"name":"葡萄","amount":"150g"}]', '{"protein":1,"carb":27,"fat":0,"calories":105}', 'SNACK', 0, 'EASY', '["加餐","水果"]', '["LOW_CALORIE"]', 70, '5:95:0', 'APPROVED', NULL, NOW(), NOW()),
('橙子', NULL, '[{"name":"橙子","amount":"1个"}]', '{"protein":1,"carb":21,"fat":0,"calories":85}', 'SNACK', 0, 'EASY', '["加餐","维生素"]', '["LOW_CALORIE"]', 47, '5:93:2', 'APPROVED', NULL, NOW(), NOW()),
('待审菜谱A', NULL, '[{"name":"测试","amount":"100g"}]', '{"protein":10,"carb":20,"fat":5,"calories":180}', 'BREAKFAST', 10, 'EASY', '["待审"]', '["LOW_CALORIE"]', 60, '25:55:20', 'PENDING', 7, NOW(), NOW()),
('待审菜谱B', NULL, '[{"name":"测试","amount":"100g"}]', '{"protein":15,"carb":25,"fat":6,"calories":220}', 'LUNCH', 15, 'EASY', '["待审"]', '["HIGH_PROTEIN"]', 73, '35:50:15', 'PENDING', 8, NOW(), NOW()),
('待审菜谱C', NULL, '[{"name":"测试","amount":"100g"}]', '{"protein":12,"carb":15,"fat":8,"calories":200}', 'DINNER', 20, 'MEDIUM', '["待审"]', '["LOW_CARB"]', 67, '40:35:25', 'PENDING', 9, NOW(), NOW()),
('驳回菜谱A', NULL, '[{"name":"测试","amount":"100g"}]', '{"protein":5,"carb":50,"fat":20,"calories":380}', 'LUNCH', 10, 'EASY', '["驳回"]', '[]', 127, '10:55:35', 'REJECTED', 10, NOW(), NOW()),
('驳回菜谱B', NULL, '[{"name":"测试","amount":"100g"}]', '{"protein":8,"carb":60,"fat":15,"calories":420}', 'LUNCH', 15, 'EASY', '["驳回"]', '[]', 105, '10:62:28', 'REJECTED', NULL, NOW(), NOW());

-- 第4批菜谱 84 条（早20+午28+晚16+加餐20，共150条）
INSERT INTO `recipe` (`title`, `cover_image`, `ingredients`, `nutrition_info`, `category`, `cooking_time`, `difficulty`, `tags`, `match_tags`, `calories_per_100g`, `protein_carb_fat_ratio`, `status`, `creator_id`, `create_time`, `update_time`) VALUES
('红枣小米粥', NULL, '[{"name":"小米","amount":"50g"},{"name":"红枣","amount":"5颗"}]', '{"protein":4,"carb":45,"fat":2,"calories":220}', 'BREAKFAST', 30, 'EASY', '["早餐","养胃"]', '["LOW_CALORIE"]', 73, '10:85:5', 'APPROVED', NULL, NOW(), NOW()),
('鸡蛋灌饼', NULL, '[{"name":"面粉","amount":"80g"},{"name":"鸡蛋","amount":"1个"}]', '{"protein":11,"carb":42,"fat":8,"calories":280}', 'BREAKFAST', 15, 'EASY', '["早餐","中式"]', '["HIGH_PROTEIN"]', 93, '18:62:20', 'APPROVED', NULL, NOW(), NOW()),
('牛奶麦片', NULL, '[{"name":"麦片","amount":"50g"},{"name":"牛奶","amount":"200ml"}]', '{"protein":12,"carb":48,"fat":6,"calories":290}', 'BREAKFAST', 5, 'EASY', '["早餐","快手"]', '["LOW_CALORIE"]', 97, '22:68:10', 'APPROVED', NULL, NOW(), NOW()),
('紫薯粥', NULL, '[{"name":"紫薯","amount":"100g"},{"name":"大米","amount":"30g"}]', '{"protein":3,"carb":38,"fat":0,"calories":180}', 'BREAKFAST', 35, 'EASY', '["早餐","粗粮"]', '["LOW_CALORIE"]', 60, '8:90:2', 'APPROVED', NULL, NOW(), NOW()),
('菠菜蛋饼', NULL, '[{"name":"菠菜","amount":"50g"},{"name":"鸡蛋","amount":"2个"}]', '{"protein":14,"carb":6,"fat":12,"calories":190}', 'BREAKFAST', 12, 'EASY', '["早餐","高蛋白"]', '["HIGH_PROTEIN","LOW_CARB"]', 63, '35:15:50', 'APPROVED', NULL, NOW(), NOW()),
('包子两个', NULL, '[{"name":"鲜肉包","amount":"2个"}]', '{"protein":14,"carb":42,"fat":8,"calories":300}', 'BREAKFAST', 5, 'EASY', '["早餐","中式"]', '["HIGH_PROTEIN"]', 100, '22:58:20', 'APPROVED', NULL, NOW(), NOW()),
('芝麻糊', NULL, '[{"name":"黑芝麻糊","amount":"40g"}]', '{"protein":6,"carb":35,"fat":12,"calories":260}', 'BREAKFAST', 5, 'EASY', '["早餐","快手"]', '["LOW_CALORIE"]', 87, '12:58:30', 'APPROVED', NULL, NOW(), NOW()),
('玉米饼', NULL, '[{"name":"玉米面","amount":"60g"},{"name":"鸡蛋","amount":"1个"}]', '{"protein":8,"carb":38,"fat":5,"calories":230}', 'BREAKFAST', 20, 'EASY', '["早餐","粗粮"]', '["LOW_CALORIE"]', 77, '18:68:14', 'APPROVED', NULL, NOW(), NOW()),
('法式吐司', NULL, '[{"name":"吐司","amount":"2片"},{"name":"鸡蛋","amount":"1个"},{"name":"牛奶","amount":"30ml"}]', '{"protein":14,"carb":35,"fat":10,"calories":280}', 'BREAKFAST', 15, 'EASY', '["早餐","西式"]', '["HIGH_PROTEIN"]', 93, '28:52:20', 'APPROVED', NULL, NOW(), NOW()),
('山药粥', NULL, '[{"name":"山药","amount":"100g"},{"name":"大米","amount":"40g"}]', '{"protein":4,"carb":42,"fat":1,"calories":200}', 'BREAKFAST', 40, 'EASY', '["早餐","养胃"]', '["LOW_CALORIE"]', 67, '10:88:2', 'APPROVED', NULL, NOW(), NOW()),
('烧卖四个', NULL, '[{"name":"烧卖","amount":"4个"}]', '{"protein":12,"carb":40,"fat":10,"calories":320}', 'BREAKFAST', 10, 'EASY', '["早餐","中式"]', '["HIGH_PROTEIN"]', 107, '18:55:27', 'APPROVED', NULL, NOW(), NOW()),
('燕麦牛奶', NULL, '[{"name":"燕麦","amount":"45g"},{"name":"牛奶","amount":"250ml"}]', '{"protein":14,"carb":42,"fat":6,"calories":280}', 'BREAKFAST', 5, 'EASY', '["早餐","快手"]', '["LOW_CALORIE"]', 93, '25:65:10', 'APPROVED', NULL, NOW(), NOW()),
('南瓜饼', NULL, '[{"name":"南瓜","amount":"120g"},{"name":"糯米粉","amount":"40g"}]', '{"protein":3,"carb":42,"fat":2,"calories":200}', 'BREAKFAST', 25, 'EASY', '["早餐","粗粮"]', '["LOW_CALORIE"]', 67, '8:88:4', 'APPROVED', NULL, NOW(), NOW()),
('煎蛋吐司', NULL, '[{"name":"吐司","amount":"2片"},{"name":"鸡蛋","amount":"2个"}]', '{"protein":16,"carb":28,"fat":14,"calories":310}', 'BREAKFAST', 10, 'EASY', '["早餐","高蛋白"]', '["HIGH_PROTEIN"]', 103, '35:40:25', 'APPROVED', NULL, NOW(), NOW()),
('绿豆粥', NULL, '[{"name":"绿豆","amount":"50g"},{"name":"大米","amount":"20g"}]', '{"protein":6,"carb":38,"fat":1,"calories":190}', 'BREAKFAST', 50, 'EASY', '["早餐","清热"]', '["LOW_CALORIE"]', 63, '14:82:4', 'APPROVED', NULL, NOW(), NOW()),
('馒头配炒蛋', NULL, '[{"name":"馒头","amount":"1个"},{"name":"鸡蛋","amount":"1个"}]', '{"protein":14,"carb":38,"fat":6,"calories":260}', 'BREAKFAST', 12, 'EASY', '["早餐","中式"]', '["HIGH_PROTEIN"]', 87, '28:58:14', 'APPROVED', NULL, NOW(), NOW()),
('华夫饼', NULL, '[{"name":"华夫粉","amount":"60g"},{"name":"鸡蛋","amount":"1个"},{"name":"牛奶","amount":"50ml"}]', '{"protein":10,"carb":45,"fat":10,"calories":310}', 'BREAKFAST', 15, 'EASY', '["早餐","西式"]', '["LOW_CALORIE"]', 103, '18:62:20', 'APPROVED', NULL, NOW(), NOW()),
('八宝粥', NULL, '[{"name":"八宝米","amount":"60g"}]', '{"protein":6,"carb":48,"fat":2,"calories":240}', 'BREAKFAST', 60, 'EASY', '["早餐","养胃"]', '["LOW_CALORIE"]', 80, '12:78:10', 'APPROVED', NULL, NOW(), NOW()),
('酸奶香蕉杯', NULL, '[{"name":"酸奶","amount":"150g"},{"name":"香蕉","amount":"1根"}]', '{"protein":8,"carb":38,"fat":4,"calories":230}', 'BREAKFAST', 5, 'EASY', '["早餐","快手"]', '["LOW_CALORIE"]', 77, '18:70:12', 'APPROVED', NULL, NOW(), NOW()),
('葱油饼', NULL, '[{"name":"面粉","amount":"80g"},{"name":"小葱","amount":"20g"}]', '{"protein":8,"carb":42,"fat":14,"calories":320}', 'BREAKFAST', 25, 'MEDIUM', '["早餐","中式"]', '["LOW_CALORIE"]', 107, '12:55:33', 'APPROVED', NULL, NOW(), NOW()),
('豆浆油条替代', NULL, '[{"name":"豆浆","amount":"300ml"},{"name":"全麦面包","amount":"2片"}]', '{"protein":12,"carb":35,"fat":6,"calories":250}', 'BREAKFAST', 5, 'EASY', '["早餐","中式"]', '["LOW_CALORIE"]', 83, '22:58:20', 'APPROVED', NULL, NOW(), NOW()),
('鱼香肉丝饭', NULL, '[{"name":"猪里脊","amount":"80g"},{"name":"木耳","amount":"30g"},{"name":"米饭","amount":"150g"}]', '{"protein":22,"carb":55,"fat":10,"calories":400}', 'LUNCH', 25, 'MEDIUM', '["川菜","下饭"]', '["HIGH_PROTEIN"]', 100, '28:58:14', 'APPROVED', NULL, NOW(), NOW()),
('黄焖鸡米饭', NULL, '[{"name":"鸡腿","amount":"150g"},{"name":"米饭","amount":"120g"},{"name":"香菇","amount":"3朵"}]', '{"protein":38,"carb":52,"fat":14,"calories":480}', 'LUNCH', 45, 'MEDIUM', '["高蛋白","便当"]', '["HIGH_PROTEIN"]', 120, '35:45:20', 'APPROVED', NULL, NOW(), NOW()),
('番茄牛腩面', NULL, '[{"name":"牛腩","amount":"80g"},{"name":"挂面","amount":"100g"},{"name":"番茄","amount":"1个"}]', '{"protein":24,"carb":52,"fat":8,"calories":380}', 'LUNCH', 40, 'MEDIUM', '["高蛋白","面食"]', '["HIGH_PROTEIN"]', 95, '28:58:14', 'APPROVED', NULL, NOW(), NOW()),
('宫保鸡丁饭', NULL, '[{"name":"鸡胸","amount":"120g"},{"name":"花生","amount":"20g"},{"name":"米饭","amount":"130g"}]', '{"protein":32,"carb":48,"fat":14,"calories":440}', 'LUNCH', 30, 'MEDIUM', '["川菜","高蛋白"]', '["HIGH_PROTEIN"]', 110, '35:45:20', 'APPROVED', NULL, NOW(), NOW()),
('酸菜鱼', NULL, '[{"name":"草鱼","amount":"150g"},{"name":"酸菜","amount":"80g"},{"name":"豆芽","amount":"50g"}]', '{"protein":28,"carb":8,"fat":10,"calories":260}', 'LUNCH', 35, 'MEDIUM', '["川菜","高蛋白"]', '["HIGH_PROTEIN","LOW_CARB"]', 65, '55:15:30', 'APPROVED', NULL, NOW(), NOW()),
('卤肉饭', NULL, '[{"name":"五花肉","amount":"80g"},{"name":"米饭","amount":"150g"},{"name":"卤蛋","amount":"1个"}]', '{"protein":22,"carb":55,"fat":22,"calories":520}', 'LUNCH', 50, 'MEDIUM', '["高蛋白","便当"]', '["HIGH_PROTEIN","HIGH_CALORIE"]', 130, '22:45:33', 'APPROVED', NULL, NOW(), NOW()),
('蒜苔炒肉', NULL, '[{"name":"猪瘦肉","amount":"100g"},{"name":"蒜苔","amount":"100g"}]', '{"protein":26,"carb":12,"fat":8,"calories":240}', 'LUNCH', 18, 'EASY', '["高蛋白","快手"]', '["HIGH_PROTEIN"]', 60, '55:22:23', 'APPROVED', NULL, NOW(), NOW()),
('土豆炖牛肉', NULL, '[{"name":"牛腩","amount":"120g"},{"name":"土豆","amount":"100g"}]', '{"protein":28,"carb":28,"fat":10,"calories":340}', 'LUNCH', 55, 'MEDIUM', '["高蛋白","炖菜"]', '["HIGH_PROTEIN"]', 85, '45:35:20', 'APPROVED', NULL, NOW(), NOW()),
('扬州炒饭', NULL, '[{"name":"米饭","amount":"200g"},{"name":"虾仁","amount":"50g"},{"name":"鸡蛋","amount":"1个"},{"name":"火腿","amount":"30g"}]', '{"protein":18,"carb":55,"fat":10,"calories":400}', 'LUNCH', 20, 'EASY', '["炒饭","高蛋白"]', '["HIGH_PROTEIN"]', 100, '22:58:20', 'APPROVED', NULL, NOW(), NOW()),
('酸辣粉', NULL, '[{"name":"红薯粉","amount":"100g"},{"name":"花生","amount":"15g"},{"name":"醋","amount":"15ml"}]', '{"protein":6,"carb":52,"fat":8,"calories":320}', 'LUNCH', 25, 'EASY', '["小吃","快手"]', '["LOW_CALORIE"]', 80, '10:68:22', 'APPROVED', NULL, NOW(), NOW()),
('肉夹馍', NULL, '[{"name":"白吉馍","amount":"1个"},{"name":"卤肉","amount":"80g"}]', '{"protein":24,"carb":42,"fat":18,"calories":450}', 'LUNCH', 10, 'EASY', '["高蛋白","小吃"]', '["HIGH_PROTEIN","HIGH_CALORIE"]', 113, '28:42:30', 'APPROVED', NULL, NOW(), NOW()),
('番茄炖豆腐', NULL, '[{"name":"豆腐","amount":"200g"},{"name":"番茄","amount":"2个"}]', '{"protein":16,"carb":15,"fat":8,"calories":200}', 'LUNCH', 25, 'EASY', '["素食","低卡"]', '["LOW_CALORIE"]', 50, '45:35:20', 'APPROVED', NULL, NOW(), NOW()),
('咖喱鸡饭', NULL, '[{"name":"鸡腿","amount":"150g"},{"name":"土豆","amount":"80g"},{"name":"米饭","amount":"120g"}]', '{"protein":32,"carb":52,"fat":12,"calories":440}', 'LUNCH', 40, 'MEDIUM', '["高蛋白","便当"]', '["HIGH_PROTEIN"]', 110, '35:50:15', 'APPROVED', NULL, NOW(), NOW()),
('清炒芥兰', NULL, '[{"name":"芥兰","amount":"200g"},{"name":"蒜","amount":"3瓣"}]', '{"protein":4,"carb":8,"fat":2,"calories":60}', 'LUNCH', 10, 'EASY', '["素食","低卡"]', '["LOW_CALORIE","LOW_CARB"]', 15, '28:55:17', 'APPROVED', NULL, NOW(), NOW()),
('红烧肉套餐', NULL, '[{"name":"五花肉","amount":"100g"},{"name":"米饭","amount":"150g"},{"name":"青菜","amount":"80g"}]', '{"protein":20,"carb":55,"fat":25,"calories":550}', 'LUNCH', 60, 'MEDIUM', '["高蛋白","便当"]', '["HIGH_PROTEIN","HIGH_CALORIE"]', 138, '18:42:40', 'APPROVED', NULL, NOW(), NOW()),
('凉皮', NULL, '[{"name":"凉皮","amount":"200g"},{"name":"面筋","amount":"30g"},{"name":"黄瓜丝","amount":"50g"}]', '{"protein":8,"carb":48,"fat":8,"calories":300}', 'LUNCH', 15, 'EASY', '["小吃","快手"]', '["LOW_CALORIE"]', 75, '12:68:20', 'APPROVED', NULL, NOW(), NOW()),
('酸辣土豆丝', NULL, '[{"name":"土豆","amount":"200g"},{"name":"青椒","amount":"30g"}]', '{"protein":4,"carb":35,"fat":2,"calories":180}', 'LUNCH', 15, 'EASY', '["素食","快手"]', '["LOW_CALORIE"]', 45, '10:82:8', 'APPROVED', NULL, NOW(), NOW()),
('蒜香排骨', NULL, '[{"name":"排骨","amount":"200g"},{"name":"蒜","amount":"1头"}]', '{"protein":32,"carb":5,"fat":28,"calories":420}', 'LUNCH', 45, 'MEDIUM', '["高蛋白","下饭"]', '["HIGH_PROTEIN"]', 105, '35:6:59', 'APPROVED', NULL, NOW(), NOW()),
('干锅花菜', NULL, '[{"name":"花菜","amount":"250g"},{"name":"五花肉","amount":"30g"}]', '{"protein":8,"carb":12,"fat":10,"calories":180}', 'LUNCH', 25, 'EASY', '["素食","下饭"]', '["LOW_CALORIE"]', 45, '22:32:46', 'APPROVED', NULL, NOW(), NOW()),
('番茄鸡蛋盖饭', NULL, '[{"name":"鸡蛋","amount":"2个"},{"name":"番茄","amount":"2个"},{"name":"米饭","amount":"150g"}]', '{"protein":16,"carb":55,"fat":8,"calories":360}', 'LUNCH', 20, 'EASY', '["快手","饱腹"]', '["HIGH_PROTEIN"]', 90, '22:62:16', 'APPROVED', NULL, NOW(), NOW()),
('清蒸多宝鱼', NULL, '[{"name":"多宝鱼","amount":"400g"},{"name":"姜葱","amount":"适量"}]', '{"protein":45,"carb":2,"fat":6,"calories":260}', 'DINNER', 30, 'EASY', '["高蛋白","低脂","海鲜"]', '["HIGH_PROTEIN","LOW_CALORIE"]', 43, '78:5:17', 'APPROVED', NULL, NOW(), NOW()),
('冬瓜薏米汤', NULL, '[{"name":"冬瓜","amount":"200g"},{"name":"薏米","amount":"30g"}]', '{"protein":3,"carb":22,"fat":1,"calories":110}', 'DINNER', 40, 'EASY', '["汤品","清淡"]', '["LOW_CALORIE"]', 37, '12:78:10', 'APPROVED', NULL, NOW(), NOW()),
('白灼芥兰', NULL, '[{"name":"芥兰","amount":"250g"},{"name":"蚝油","amount":"10ml"}]', '{"protein":5,"carb":10,"fat":2,"calories":75}', 'DINNER', 12, 'EASY', '["素食","低卡"]', '["LOW_CALORIE","LOW_CARB"]', 15, '28:55:17', 'APPROVED', NULL, NOW(), NOW()),
('菌菇汤', NULL, '[{"name":"香菇","amount":"50g"},{"name":"金针菇","amount":"50g"},{"name":"平菇","amount":"50g"}]', '{"protein":6,"carb":12,"fat":1,"calories":80}', 'DINNER', 25, 'EASY', '["汤品","素食"]', '["LOW_CALORIE"]', 27, '35:55:10', 'APPROVED', NULL, NOW(), NOW()),
('蒜蓉生菜', NULL, '[{"name":"生菜","amount":"200g"},{"name":"蒜","amount":"4瓣"}]', '{"protein":3,"carb":6,"fat":2,"calories":55}', 'DINNER', 8, 'EASY', '["素食","快手"]', '["LOW_CALORIE"]', 18, '25:45:30', 'APPROVED', NULL, NOW(), NOW()),
('萝卜牛腩汤', NULL, '[{"name":"牛腩","amount":"100g"},{"name":"白萝卜","amount":"150g"}]', '{"protein":22,"carb":10,"fat":8,"calories":220}', 'DINNER', 55, 'MEDIUM', '["汤品","高蛋白"]', '["HIGH_PROTEIN"]', 55, '55:22:23', 'APPROVED', NULL, NOW(), NOW()),
('凉拌海带丝', NULL, '[{"name":"海带丝","amount":"150g"},{"name":"醋","amount":"10ml"}]', '{"protein":2,"carb":10,"fat":1,"calories":55}', 'DINNER', 10, 'EASY', '["凉菜","低卡"]', '["LOW_CALORIE"]', 18, '18:70:12', 'APPROVED', NULL, NOW(), NOW()),
('蒸蛋羹', NULL, '[{"name":"鸡蛋","amount":"2个"},{"name":"温水","amount":"100ml"}]', '{"protein":12,"carb":2,"fat":10,"calories":155}', 'DINNER', 15, 'EASY', '["高蛋白","清淡"]', '["HIGH_PROTEIN","LOW_CARB"]', 52, '50:8:42', 'APPROVED', NULL, NOW(), NOW()),
('菠菜豆腐汤', NULL, '[{"name":"菠菜","amount":"80g"},{"name":"嫩豆腐","amount":"100g"}]', '{"protein":10,"carb":6,"fat":4,"calories":95}', 'DINNER', 15, 'EASY', '["汤品","素食"]', '["LOW_CALORIE"]', 32, '55:28:17', 'APPROVED', NULL, NOW(), NOW()),
('烤鸡胸', NULL, '[{"name":"鸡胸肉","amount":"200g"},{"name":"黑胡椒","amount":"适量"}]', '{"protein":48,"carb":0,"fat":4,"calories":240}', 'DINNER', 25, 'EASY', '["高蛋白","低脂"]', '["HIGH_PROTEIN","LOW_CALORIE"]', 48, '88:0:12', 'APPROVED', NULL, NOW(), NOW()),
('蒜蓉空心菜', NULL, '[{"name":"空心菜","amount":"250g"},{"name":"蒜","amount":"5瓣"}]', '{"protein":4,"carb":8,"fat":2,"calories":60}', 'DINNER', 10, 'EASY', '["素食","低卡"]', '["LOW_CALORIE"]', 15, '28:55:17', 'APPROVED', NULL, NOW(), NOW()),
('莲藕排骨汤', NULL, '[{"name":"排骨","amount":"120g"},{"name":"莲藕","amount":"150g"}]', '{"protein":24,"carb":25,"fat":12,"calories":320}', 'DINNER', 60, 'MEDIUM', '["汤品","高蛋白"]', '["HIGH_PROTEIN"]', 80, '35:35:30', 'APPROVED', NULL, NOW(), NOW()),
('清炒芦笋', NULL, '[{"name":"芦笋","amount":"200g"},{"name":"蒜","amount":"3瓣"}]', '{"protein":4,"carb":8,"fat":2,"calories":65}', 'DINNER', 12, 'EASY', '["素食","低卡"]', '["LOW_CALORIE","LOW_CARB"]', 16, '28:55:17', 'APPROVED', NULL, NOW(), NOW()),
('番茄龙利鱼汤', NULL, '[{"name":"龙利鱼","amount":"180g"},{"name":"番茄","amount":"1个"}]', '{"protein":28,"carb":8,"fat":3,"calories":180}', 'DINNER', 25, 'EASY', '["高蛋白","低脂","汤品"]', '["HIGH_PROTEIN","LOW_CALORIE"]', 45, '68:22:10', 'APPROVED', NULL, NOW(), NOW()),
('草莓', NULL, '[{"name":"草莓","amount":"150g"}]', '{"protein":1,"carb":12,"fat":0,"calories":48}', 'SNACK', 0, 'EASY', '["加餐","水果"]', '["LOW_CALORIE"]', 32, '8:90:2', 'APPROVED', NULL, NOW(), NOW()),
('圣女果', NULL, '[{"name":"圣女果","amount":"10颗"}]', '{"protein":1,"carb":6,"fat":0,"calories":28}', 'SNACK', 0, 'EASY', '["加餐","低卡"]', '["LOW_CALORIE"]', 19, '15:80:5', 'APPROVED', NULL, NOW(), NOW()),
('猕猴桃', NULL, '[{"name":"猕猴桃","amount":"2个"}]', '{"protein":1,"carb":22,"fat":0,"calories":90}', 'SNACK', 0, 'EASY', '["加餐","维生素"]', '["LOW_CALORIE"]', 45, '5:92:3', 'APPROVED', NULL, NOW(), NOW()),
('火龙果', NULL, '[{"name":"火龙果","amount":"半个"}]', '{"protein":1,"carb":18,"fat":0,"calories":75}', 'SNACK', 0, 'EASY', '["加餐","水果"]', '["LOW_CALORIE"]', 50, '5:92:3', 'APPROVED', NULL, NOW(), NOW()),
('蓝莓', NULL, '[{"name":"蓝莓","amount":"100g"}]', '{"protein":1,"carb":14,"fat":0,"calories":57}', 'SNACK', 0, 'EASY', '["加餐","抗氧化"]', '["LOW_CALORIE"]', 57, '7:88:5', 'APPROVED', NULL, NOW(), NOW()),
('芒果', NULL, '[{"name":"芒果","amount":"150g"}]', '{"protein":1,"carb":28,"fat":0,"calories":112}', 'SNACK', 0, 'EASY', '["加餐","水果"]', '["LOW_CALORIE"]', 75, '4:94:2', 'APPROVED', NULL, NOW(), NOW()),
('酸奶杯', NULL, '[{"name":"酸奶","amount":"180g"},{"name":"格兰诺拉","amount":"20g"}]', '{"protein":8,"carb":28,"fat":4,"calories":180}', 'SNACK', 5, 'EASY', '["加餐","快手"]', '["LOW_CALORIE"]', 60, '22:65:13', 'APPROVED', NULL, NOW(), NOW()),
('花生', NULL, '[{"name":"花生","amount":"30g"}]', '{"protein":8,"carb":10,"fat":14,"calories":180}', 'SNACK', 0, 'EASY', '["加餐","优质脂肪"]', '["HIGH_CALORIE"]', 600, '22:25:53', 'APPROVED', NULL, NOW(), NOW()),
('核桃两个', NULL, '[{"name":"核桃","amount":"2个约30g"}]', '{"protein":4,"carb":4,"fat":18,"calories":195}', 'SNACK', 0, 'EASY', '["加餐","健脑"]', '["HIGH_CALORIE"]', 650, '10:10:80', 'APPROVED', NULL, NOW(), NOW()),
('杏仁', NULL, '[{"name":"杏仁","amount":"25g"}]', '{"protein":5,"carb":6,"fat":14,"calories":155}', 'SNACK', 0, 'EASY', '["加餐","优质脂肪"]', '["HIGH_CALORIE"]', 620, '15:18:67', 'APPROVED', NULL, NOW(), NOW()),
('黑巧克力', NULL, '[{"name":"黑巧克力","amount":"30g"}]', '{"protein":2,"carb":15,"fat":12,"calories":170}', 'SNACK', 0, 'EASY', '["加餐","抗氧化"]', '["HIGH_CALORIE"]', 567, '5:35:60', 'APPROVED', NULL, NOW(), NOW()),
('红薯干', NULL, '[{"name":"红薯干","amount":"50g"}]', '{"protein":1,"carb":42,"fat":0,"calories":170}', 'SNACK', 0, 'EASY', '["加餐","粗粮"]', '["LOW_CALORIE"]', 340, '3:95:2', 'APPROVED', NULL, NOW(), NOW()),
('煮玉米', NULL, '[{"name":"甜玉米","amount":"1根"}]', '{"protein":4,"carb":28,"fat":2,"calories":140}', 'SNACK', 15, 'EASY', '["加餐","粗粮"]', '["LOW_CALORIE"]', 70, '12:78:10', 'APPROVED', NULL, NOW(), NOW()),
('豆浆一杯', NULL, '[{"name":"无糖豆浆","amount":"300ml"}]', '{"protein":9,"carb":6,"fat":4,"calories":100}', 'SNACK', 0, 'EASY', '["加餐","植物蛋白"]', '["LOW_CALORIE","HIGH_PROTEIN"]', 33, '45:28:27', 'APPROVED', NULL, NOW(), NOW()),
('奶酪条', NULL, '[{"name":"奶酪","amount":"30g"}]', '{"protein":8,"carb":1,"fat":9,"calories":120}', 'SNACK', 0, 'EASY', '["加餐","高蛋白"]', '["HIGH_PROTEIN"]', 400, '35:5:60', 'APPROVED', NULL, NOW(), NOW()),
('海苔', NULL, '[{"name":"海苔","amount":"5g"}]', '{"protein":2,"carb":2,"fat":0,"calories":15}', 'SNACK', 0, 'EASY', '["加餐","低卡"]', '["LOW_CALORIE"]', 300, '55:35:10', 'APPROVED', NULL, NOW(), NOW()),
('西柚', NULL, '[{"name":"西柚","amount":"半个"}]', '{"protein":1,"carb":13,"fat":0,"calories":52}', 'SNACK', 0, 'EASY', '["加餐","低卡"]', '["LOW_CALORIE"]', 35, '8:88:4', 'APPROVED', NULL, NOW(), NOW());

-- ------------------------------------------------------------
-- 5. 饮食计划 meal_plan - 500 条（user_id 1-25, recipe_id 1-65, 2024-10~2025-02）
-- ------------------------------------------------------------
INSERT INTO `meal_plan` (`user_id`, `plan_date`, `meal_type`, `recipe_id`, `suggested_calories`, `status`, `audit_status`, `create_time`, `update_time`) VALUES
(1,'2024-10-01','BREAKFAST',1,420,'COMPLETED','AUTO','2024-10-01 08:00:00','2024-10-01 08:00:00'),
(1,'2024-10-01','LUNCH',6,580,'COMPLETED','AUTO','2024-10-01 12:00:00','2024-10-01 12:00:00'),
(1,'2024-10-01','DINNER',7,360,'COMPLETED','AUTO','2024-10-01 18:00:00','2024-10-01 18:00:00'),
(1,'2024-10-01','SNACK',21,100,'COMPLETED','AUTO','2024-10-01 15:00:00','2024-10-01 15:00:00'),
(1,'2024-10-02','BREAKFAST',2,440,'COMPLETED','AUTO','2024-10-02 08:00:00','2024-10-02 08:00:00'),
(1,'2024-10-02','LUNCH',8,560,'COMPLETED','AUTO','2024-10-02 12:00:00','2024-10-02 12:00:00'),
(1,'2024-10-02','DINNER',10,380,'COMPLETED','AUTO','2024-10-02 18:00:00','2024-10-02 18:00:00'),
(1,'2024-10-02','SNACK',22,95,'SKIPPED','AUTO','2024-10-02 15:00:00','2024-10-02 15:00:00'),
(1,'2024-10-03','BREAKFAST',4,430,'COMPLETED','AUTO','2024-10-03 08:00:00','2024-10-03 08:00:00'),
(1,'2024-10-03','LUNCH',14,600,'COMPLETED','AUTO','2024-10-03 12:00:00','2024-10-03 12:00:00'),
(1,'2024-10-03','DINNER',11,350,'COMPLETED','AUTO','2024-10-03 18:00:00','2024-10-03 18:00:00'),
(1,'2024-10-03','SNACK',23,105,'COMPLETED','AUTO','2024-10-03 15:00:00','2024-10-03 15:00:00'),
(1,'2024-10-04','BREAKFAST',5,450,'COMPLETED','APPROVED','2024-10-04 08:00:00','2024-10-04 08:00:00'),
(1,'2024-10-04','LUNCH',17,550,'COMPLETED','APPROVED','2024-10-04 12:00:00','2024-10-04 12:00:00'),
(1,'2024-10-04','DINNER',18,340,'COMPLETED','APPROVED','2024-10-04 18:00:00','2024-10-04 18:00:00'),
(1,'2024-10-04','SNACK',24,98,'COMPLETED','APPROVED','2024-10-04 15:00:00','2024-10-04 15:00:00'),
(1,'2024-10-05','BREAKFAST',9,440,'PLANNED','PENDING','2024-10-05 08:00:00','2024-10-05 08:00:00'),
(1,'2024-10-05','LUNCH',19,570,'PLANNED','PENDING','2024-10-05 12:00:00','2024-10-05 12:00:00'),
(1,'2024-10-05','DINNER',20,360,'PLANNED','PENDING','2024-10-05 18:00:00','2024-10-05 18:00:00'),
(1,'2024-10-05','SNACK',25,102,'PLANNED','PENDING','2024-10-05 15:00:00','2024-10-05 15:00:00'),
(2,'2024-10-06','BREAKFAST',3,410,'COMPLETED','AUTO','2024-10-06 08:00:00','2024-10-06 08:00:00'),
(2,'2024-10-06','LUNCH',12,540,'COMPLETED','AUTO','2024-10-06 12:00:00','2024-10-06 12:00:00'),
(2,'2024-10-06','DINNER',15,370,'OUT_EAT','AUTO','2024-10-06 18:00:00','2024-10-06 18:00:00'),
(2,'2024-10-06','SNACK',26,100,'COMPLETED','AUTO','2024-10-06 15:00:00','2024-10-06 15:00:00'),
(2,'2024-10-07','BREAKFAST',13,460,'COMPLETED','AUTO','2024-10-07 08:00:00','2024-10-07 08:00:00'),
(2,'2024-10-07','LUNCH',16,620,'COMPLETED','AUTO','2024-10-07 12:00:00','2024-10-07 12:00:00'),
(2,'2024-10-07','DINNER',27,400,'COMPLETED','AUTO','2024-10-07 18:00:00','2024-10-07 18:00:00'),
(2,'2024-10-07','SNACK',28,115,'SKIPPED','AUTO','2024-10-07 15:00:00','2024-10-07 15:00:00'),
(2,'2024-10-08','BREAKFAST',1,430,'COMPLETED','AUTO','2024-10-08 08:00:00','2024-10-08 08:00:00'),
(2,'2024-10-08','LUNCH',6,590,'COMPLETED','AUTO','2024-10-08 12:00:00','2024-10-08 12:00:00'),
(2,'2024-10-08','DINNER',7,355,'COMPLETED','AUTO','2024-10-08 18:00:00','2024-10-08 18:00:00'),
(2,'2024-10-08','SNACK',21,108,'COMPLETED','AUTO','2024-10-08 15:00:00','2024-10-08 15:00:00'),
(2,'2024-10-09','BREAKFAST',2,445,'COMPLETED','AUTO','2024-10-09 08:00:00','2024-10-09 08:00:00'),
(2,'2024-10-09','LUNCH',8,565,'COMPLETED','AUTO','2024-10-09 12:00:00','2024-10-09 12:00:00'),
(2,'2024-10-09','DINNER',10,375,'COMPLETED','AUTO','2024-10-09 18:00:00','2024-10-09 18:00:00'),
(2,'2024-10-09','SNACK',22,99,'COMPLETED','AUTO','2024-10-09 15:00:00','2024-10-09 15:00:00'),
(3,'2024-10-10','BREAKFAST',4,435,'COMPLETED','AUTO','2024-10-10 08:00:00','2024-10-10 08:00:00'),
(3,'2024-10-10','LUNCH',14,605,'COMPLETED','AUTO','2024-10-10 12:00:00','2024-10-10 12:00:00'),
(3,'2024-10-10','DINNER',11,345,'COMPLETED','AUTO','2024-10-10 18:00:00','2024-10-10 18:00:00'),
(3,'2024-10-10','SNACK',23,103,'COMPLETED','AUTO','2024-10-10 15:00:00','2024-10-10 15:00:00'),
(3,'2024-10-11','BREAKFAST',5,455,'COMPLETED','APPROVED','2024-10-11 08:00:00','2024-10-11 08:00:00'),
(3,'2024-10-11','LUNCH',17,555,'COMPLETED','APPROVED','2024-10-11 12:00:00','2024-10-11 12:00:00'),
(3,'2024-10-11','DINNER',18,338,'COMPLETED','APPROVED','2024-10-11 18:00:00','2024-10-11 18:00:00'),
(3,'2024-10-11','SNACK',24,97,'COMPLETED','APPROVED','2024-10-11 15:00:00','2024-10-11 15:00:00'),
(3,'2024-10-12','BREAKFAST',9,438,'COMPLETED','AUTO','2024-10-12 08:00:00','2024-10-12 08:00:00'),
(3,'2024-10-12','LUNCH',19,572,'COMPLETED','AUTO','2024-10-12 12:00:00','2024-10-12 12:00:00'),
(3,'2024-10-12','DINNER',20,358,'COMPLETED','AUTO','2024-10-12 18:00:00','2024-10-12 18:00:00'),
(3,'2024-10-12','SNACK',25,101,'SKIPPED','AUTO','2024-10-12 15:00:00','2024-10-12 15:00:00'),
(4,'2024-10-13','BREAKFAST',3,418,'COMPLETED','AUTO','2024-10-13 08:00:00','2024-10-13 08:00:00'),
(4,'2024-10-13','LUNCH',12,538,'COMPLETED','AUTO','2024-10-13 12:00:00','2024-10-13 12:00:00'),
(4,'2024-10-13','DINNER',15,365,'COMPLETED','AUTO','2024-10-13 18:00:00','2024-10-13 18:00:00'),
(4,'2024-10-13','SNACK',26,98,'COMPLETED','AUTO','2024-10-13 15:00:00','2024-10-13 15:00:00'),
(4,'2024-10-14','BREAKFAST',13,458,'COMPLETED','AUTO','2024-10-14 08:00:00','2024-10-14 08:00:00'),
(4,'2024-10-14','LUNCH',16,618,'COMPLETED','AUTO','2024-10-14 12:00:00','2024-10-14 12:00:00'),
(4,'2024-10-14','DINNER',27,398,'COMPLETED','AUTO','2024-10-14 18:00:00','2024-10-14 18:00:00'),
(4,'2024-10-14','SNACK',28,112,'COMPLETED','AUTO','2024-10-14 15:00:00','2024-10-14 15:00:00');

INSERT INTO `meal_plan` (`user_id`, `plan_date`, `meal_type`, `recipe_id`, `suggested_calories`, `status`, `audit_status`, `create_time`, `update_time`)
WITH RECURSIVE seq AS (SELECT 0 AS n UNION ALL SELECT n+1 FROM seq WHERE n < 449)
SELECT LEAST(25, 1 + (n DIV 20)), DATE_ADD('2024-10-15', INTERVAL (n DIV 4) DAY), ELT(1 + (n MOD 4), 'BREAKFAST','LUNCH','DINNER','SNACK'), 1 + (n MOD 65), (CASE (n MOD 4) WHEN 0 THEN 420 WHEN 1 THEN 560 WHEN 2 THEN 350 ELSE 100 END), ELT(1 + (n MOD 10), 'COMPLETED','COMPLETED','COMPLETED','COMPLETED','COMPLETED','COMPLETED','PLANNED','PLANNED','SKIPPED','OUT_EAT'), ELT(1 + (n MOD 10), 'AUTO','AUTO','AUTO','AUTO','AUTO','APPROVED','APPROVED','PENDING','AUTO','AUTO'), DATE_ADD('2024-10-15', INTERVAL (n DIV 4) DAY) + INTERVAL (8 + (n MOD 4)*3) HOUR, DATE_ADD('2024-10-15', INTERVAL (n DIV 4) DAY) + INTERVAL (8 + (n MOD 4)*3) HOUR FROM seq;

-- 6. 反馈 feedback - 200 条
INSERT INTO `feedback` (`user_id`, `plan_id`, `rating`, `feedback_tags`, `taste_feedback`, `body_reaction`, `comment`, `create_time`) VALUES
(1,1,5,'["PERFECT"]','PERFECT','ENERGETIC','很好吃，饱腹感强',NOW()),(1,2,4,'["PERFECT"]','PERFECT','ENERGETIC','蛋白质充足，训练有劲',NOW()),(1,3,5,'["PERFECT"]','PERFECT','ENERGETIC','很满意',NOW()),(1,4,4,'["TOO_LITTLE"]','TOO_LIGHT','ENERGETIC','稍微有点单调',NOW()),(1,5,3,'["TOO_BLAND"]','TOO_LIGHT','TIRED','希望能有更多选择',NOW()),(1,6,5,'["PERFECT"]','PERFECT','ENERGETIC','量刚好',NOW()),(1,7,4,'["PERFECT"]','PERFECT','ENERGETIC','不错',NOW()),(1,8,2,'["TOO_MUCH"]','TOO_MUCH','BLOATED','量太大了吃不完',NOW()),(1,9,5,'["PERFECT"]','PERFECT','ENERGETIC','好吃',NOW()),(1,10,4,'["PERFECT"]','PERFECT','ENERGETIC','满意',NOW()),
(2,11,5,'["PERFECT"]','PERFECT','ENERGETIC','很好吃',NOW()),(2,12,4,'["PERFECT"]','PERFECT','ENERGETIC','饱腹',NOW()),(2,13,3,'["TOO_OILY"]','TOO_OILY','BLOATED','有点油',NOW()),(2,14,5,'["PERFECT"]','PERFECT','ENERGETIC','完美',NOW()),(2,15,4,'["PERFECT"]','PERFECT','ENERGETIC','可以',NOW()),(2,16,1,'["TOO_MUCH"]','TOO_MUCH','BLOATED','对坚果过敏没注意到',NOW()),(2,17,5,'["PERFECT"]','PERFECT','ENERGETIC','好吃',NOW()),(2,18,4,'["PERFECT"]','PERFECT','ENERGETIC','不错',NOW()),(2,19,3,'["TOO_BLAND"]','TOO_LIGHT','TIRED','稍微有点单调',NOW()),(2,20,5,'["PERFECT"]','PERFECT','ENERGETIC','满意',NOW()),
(3,21,4,'["PERFECT"]','PERFECT','ENERGETIC','蛋白质充足',NOW()),(3,22,5,'["PERFECT"]','PERFECT','ENERGETIC','训练有劲',NOW()),(3,23,4,'["PERFECT"]','PERFECT','ENERGETIC','量合适',NOW()),(3,24,3,'["TOO_MUCH"]','TOO_MUCH','BLOATED','量偏大',NOW()),(3,25,5,'["PERFECT"]','PERFECT','ENERGETIC','好吃',NOW()),(4,26,5,'["PERFECT"]','PERFECT','ENERGETIC','很满意',NOW()),(4,27,4,'["PERFECT"]','PERFECT','ENERGETIC','可以',NOW()),(4,28,2,'["TOO_OILY"]','TOO_OILY','BLOATED','太油了',NOW()),(4,29,5,'["PERFECT"]','PERFECT','ENERGETIC','完美',NOW()),(4,30,4,'["PERFECT"]','PERFECT','ENERGETIC','不错',NOW());
INSERT INTO `feedback` (`user_id`, `plan_id`, `rating`, `feedback_tags`, `taste_feedback`, `body_reaction`, `comment`, `create_time`)
SELECT 1+(s.n MOD 25), 31+s.n, ELT(1+(s.n MOD 10), 5,5,5,4,4,4,4,3,3,2), '["PERFECT"]', 'PERFECT', 'ENERGETIC', CONCAT('评论', 31+s.n), NOW() FROM (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15 UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20 UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24 UNION SELECT 25 UNION SELECT 26 UNION SELECT 27 UNION SELECT 28 UNION SELECT 29 UNION SELECT 30 UNION SELECT 31 UNION SELECT 32 UNION SELECT 33 UNION SELECT 34 UNION SELECT 35 UNION SELECT 36 UNION SELECT 37 UNION SELECT 38 UNION SELECT 39 UNION SELECT 40 UNION SELECT 41 UNION SELECT 42 UNION SELECT 43 UNION SELECT 44 UNION SELECT 45 UNION SELECT 46 UNION SELECT 47 UNION SELECT 48 UNION SELECT 49 UNION SELECT 50 UNION SELECT 51 UNION SELECT 52 UNION SELECT 53 UNION SELECT 54 UNION SELECT 55 UNION SELECT 56 UNION SELECT 57 UNION SELECT 58 UNION SELECT 59 UNION SELECT 60 UNION SELECT 61 UNION SELECT 62 UNION SELECT 63 UNION SELECT 64 UNION SELECT 65 UNION SELECT 66 UNION SELECT 67 UNION SELECT 68 UNION SELECT 69 UNION SELECT 70 UNION SELECT 71 UNION SELECT 72 UNION SELECT 73 UNION SELECT 74 UNION SELECT 75 UNION SELECT 76 UNION SELECT 77 UNION SELECT 78 UNION SELECT 79 UNION SELECT 80 UNION SELECT 81 UNION SELECT 82 UNION SELECT 83 UNION SELECT 84 UNION SELECT 85 UNION SELECT 86 UNION SELECT 87 UNION SELECT 88 UNION SELECT 89 UNION SELECT 90 UNION SELECT 91 UNION SELECT 92 UNION SELECT 93 UNION SELECT 94 UNION SELECT 95 UNION SELECT 96 UNION SELECT 97 UNION SELECT 98 UNION SELECT 99 UNION SELECT 100 UNION SELECT 101 UNION SELECT 102 UNION SELECT 103 UNION SELECT 104 UNION SELECT 105 UNION SELECT 106 UNION SELECT 107 UNION SELECT 108 UNION SELECT 109 UNION SELECT 110 UNION SELECT 111 UNION SELECT 112 UNION SELECT 113 UNION SELECT 114 UNION SELECT 115 UNION SELECT 116 UNION SELECT 117 UNION SELECT 118 UNION SELECT 119 UNION SELECT 120 UNION SELECT 121 UNION SELECT 122 UNION SELECT 123 UNION SELECT 124 UNION SELECT 125 UNION SELECT 126 UNION SELECT 127 UNION SELECT 128 UNION SELECT 129 UNION SELECT 130 UNION SELECT 131 UNION SELECT 132 UNION SELECT 133 UNION SELECT 134 UNION SELECT 135 UNION SELECT 136 UNION SELECT 137 UNION SELECT 138 UNION SELECT 139 UNION SELECT 140 UNION SELECT 141 UNION SELECT 142 UNION SELECT 143 UNION SELECT 144 UNION SELECT 145 UNION SELECT 146 UNION SELECT 147 UNION SELECT 148 UNION SELECT 149 UNION SELECT 150 UNION SELECT 151 UNION SELECT 152 UNION SELECT 153 UNION SELECT 154 UNION SELECT 155 UNION SELECT 156 UNION SELECT 157 UNION SELECT 158 UNION SELECT 159 UNION SELECT 160 UNION SELECT 161 UNION SELECT 162 UNION SELECT 163 UNION SELECT 164 UNION SELECT 165 UNION SELECT 166 UNION SELECT 167 UNION SELECT 168 UNION SELECT 169) s WHERE 31+s.n <= 500;

-- 7. 体重记录 body_log - 300 条
INSERT INTO `body_log` (`user_id`, `weight`, `log_date`, `create_time`) WITH RECURSIVE seq AS (SELECT 0 AS n UNION ALL SELECT n+1 FROM seq WHERE n < 299) SELECT 1 + (n DIV 15), 52.0 + (n MOD 15)*0.2 + (n DIV 15)*1.5, DATE_ADD('2024-10-01', INTERVAL (n DIV 15)*32 + (n MOD 15)*2 DAY), NOW() FROM seq;

-- 8. 用户偏好 user_preference - 25 条
INSERT INTO `user_preference` (`user_id`, `calorie_multiplier`, `tag_weights`, `adjustment_count`, `learning_progress`, `create_time`, `update_time`) VALUES
(1,1.00,'{}',0,20,NOW(),NOW()),(2,0.95,'{}',2,35,NOW(),NOW()),(3,1.05,'{}',1,28,NOW(),NOW()),(4,1.00,'{}',0,15,NOW(),NOW()),(5,0.90,'{}',3,50,NOW(),NOW()),(6,1.00,'{}',0,10,NOW(),NOW()),(7,1.10,'{}',1,22,NOW(),NOW()),(8,0.85,'{}',4,60,NOW(),NOW()),(9,1.00,'{}',0,18,NOW(),NOW()),(10,1.15,'{}',2,40,NOW(),NOW()),(11,1.00,'{}',0,12,NOW(),NOW()),(12,0.92,'{}',2,45,NOW(),NOW()),(13,1.00,'{}',0,8,NOW(),NOW()),(14,1.08,'{}',1,30,NOW(),NOW()),(15,0.88,'{}',3,55,NOW(),NOW()),(16,1.00,'{}',0,25,NOW(),NOW()),(17,1.00,'{}',0,14,NOW(),NOW()),(18,1.02,'{}',1,38,NOW(),NOW()),(19,0.95,'{}',2,42,NOW(),NOW()),(20,1.00,'{}',0,20,NOW(),NOW()),(21,1.00,'{}',0,5,NOW(),NOW()),(22,1.00,'{}',0,16,NOW(),NOW()),(23,1.00,'{}',0,9,NOW(),NOW()),(24,1.00,'{}',0,11,NOW(),NOW()),(25,1.00,'{}',0,7,NOW(),NOW());

-- 9. 系统通知 system_notification - 100 条
INSERT INTO `system_notification` (`user_id`, `message`, `is_read`, `create_time`) VALUES
(1,'您的2025-01-15饮食计划已通过审核',0,NOW()),(1,'系统检测到您对坚果过敏，已为您添加无坚果标签',1,NOW()),(2,'您的2025-01-16饮食计划已通过审核',0,NOW()),(3,'您的计划被驳回：热量分配不合理',1,NOW()),(4,'新增50道春季轻食菜谱',0,NOW()),(5,'根据您的目标，建议增加蛋白质摄入',0,NOW()),(6,'您的2025-01-18饮食计划已通过审核',1,NOW()),(7,'春节饮食建议已更新',0,NOW()),(8,'您的计划被驳回：请减少油脂类菜品',1,NOW()),(9,'您的2025-01-20饮食计划已通过审核',0,NOW()),(10,'系统已根据反馈调整您的热量系数',1,NOW()),(1,'您的2025-01-22饮食计划已通过审核',0,NOW()),(2,'标签调整通知：已添加LOW_CARB',0,NOW()),(3,'您的2025-01-24饮食计划已通过审核',1,NOW()),(4,'您的计划被驳回：蛋白质不足',0,NOW()),(5,'您的2025-01-26饮食计划已通过审核',0,NOW()),(6,'根据您的目标，建议增加蛋白质摄入',1,NOW()),(7,'您的2025-01-28饮食计划已通过审核',0,NOW()),(8,'新增50道春季轻食菜谱',1,NOW()),(9,'您的计划被驳回：热量分配不合理',0,NOW()),(10,'您的2025-01-30饮食计划已通过审核',0,NOW());
INSERT INTO `system_notification` (`user_id`, `message`, `is_read`, `create_time`) SELECT 1+(s.n MOD 25), CONCAT('您的', DATE_FORMAT(DATE_ADD('2025-02-01', INTERVAL s.n DAY), '%Y-%m-%d'), '饮食计划已通过审核'), (s.n MOD 2), NOW() FROM (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15 UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20 UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24 UNION SELECT 25 UNION SELECT 26 UNION SELECT 27 UNION SELECT 28 UNION SELECT 29 UNION SELECT 30 UNION SELECT 31 UNION SELECT 32 UNION SELECT 33 UNION SELECT 34 UNION SELECT 35 UNION SELECT 36 UNION SELECT 37 UNION SELECT 38 UNION SELECT 39 UNION SELECT 40 UNION SELECT 41 UNION SELECT 42 UNION SELECT 43 UNION SELECT 44 UNION SELECT 45 UNION SELECT 46 UNION SELECT 47 UNION SELECT 48 UNION SELECT 49 UNION SELECT 50 UNION SELECT 51 UNION SELECT 52 UNION SELECT 53 UNION SELECT 54 UNION SELECT 55 UNION SELECT 56 UNION SELECT 57 UNION SELECT 58 UNION SELECT 59 UNION SELECT 60 UNION SELECT 61 UNION SELECT 62 UNION SELECT 63 UNION SELECT 64 UNION SELECT 65 UNION SELECT 66 UNION SELECT 67 UNION SELECT 68 UNION SELECT 69 UNION SELECT 70 UNION SELECT 71 UNION SELECT 72 UNION SELECT 73 UNION SELECT 74) s LIMIT 75;

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
