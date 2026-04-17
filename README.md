# 健身饮食规划系统 (Fitness Diet Planning System)

基于 Spring Boot + Vue3 的智能健身饮食规划 Web 应用，支持用户健康档案管理、个性化饮食标签生成、智能菜谱推荐、每日饮食计划以及反馈驱动的推荐优化。

## 技术栈

### 后端

| 技术            | 版本   | 说明                               |
| --------------- | ------ | ---------------------------------- |
| Spring Boot     | 3.2.x  | 基础框架                           |
| MyBatis-Plus    | 3.5.5  | ORM                                |
| Spring Security | -      | 安全框架                           |
| JWT (jjwt)      | 0.12.5 | 无状态认证                         |
| Drools          | 9.44.0 | 规则引擎（饮食标签生成）           |
| Redis           | -      | 缓存 (Spring Data Redis + Lettuce) |
| MySQL           | 8.0    | 数据库 (InnoDB)                    |
| Flyway          | -      | 数据库版本迁移                     |
| SpringDoc       | -      | OpenAPI 3.0 / Swagger              |

### 前端

| 技术            | 版本   | 说明       |
| --------------- | ------ | ---------- |
| Vue             | 3.4.x  | 前端框架   |
| Vite            | 5.1.x  | 构建工具   |
| Element Plus    | 2.6.x  | UI 组件库  |
| Pinia           | 2.1.x  | 状态管理   |
| Vue Router      | 4.x    | 路由       |
| ECharts         | 5.5.x  | 数据可视化 |
| vite-plugin-pwa | 0.20.x | PWA 支持   |

## 服务器部署参数（宝塔）

已按当前服务器信息提供生产配置（见 `frontend/.env.production`）：

- 前端访问地址：`http://43.164.2.245:3000`
- 后端 API 地址：`http://43.164.2.245:8080/api`
- 管理后台地址：`http://43.164.2.245:3000/admin`

若后续更换服务器 IP 或端口，只需更新 `frontend/.env.production` 的 `VITE_API_BASE` 并重新执行前端构建。

## 项目结构

```
diet_management/
├── backend/                     # Spring Boot 后端
│   ├── pom.xml
│   ├── rules/                   # Drools 规则热加载目录（可选）
│   └── src/main/java/com/diet/
├── frontend/                    # Vue3 前端
│   ├── public/
│   │   └── favicon.svg         # PWA 图标
│   ├── src/
│   │   ├── api/                # API 接口
│   │   │   ├── admin.js
│   │   │   ├── auth.js
│   │   │   ├── feedback.js
│   │   │   ├── plan.js
│   │   │   ├── recipe.js
│   │   │   ├── request.js      # Axios 拦截器
│   │   │   └── user.js
│   │   ├── components/         # 公共组件
│   │   │   ├── PlanSkeleton.vue
│   │   │   ├── PullToRefresh.vue
│   │   │   ├── PwaInstallBanner.vue
│   │   │   └── RecipeGridSkeleton.vue
│   │   ├── composables/
│   │   │   ├── useLongPress.js
│   │   │   └── useSwipe.js
│   │   ├── directives/
│   │   │   └── vLazy.js        # 图片懒加载
│   │   ├── layouts/
│   │   │   ├── AdminLayout.vue # 管理后台布局
│   │   │   └── MainLayout.vue  # 用户端布局
│   │   ├── router/
│   │   │   └── index.js
│   │   ├── stores/
│   │   │   └── user.js
│   │   ├── utils/
│   │   │   └── vibrate.js
│   │   ├── views/
│   │   │   ├── admin/          # 管理端页面
│   │   │   │   ├── AdminDashboardView.vue
│   │   │   │   ├── AdminRecipeAuditView.vue
│   │   │   │   ├── AdminRulesView.vue
│   │   │   │   ├── AdminStatisticsView.vue
│   │   │   │   └── AdminUsersView.vue
│   │   │   ├── AdminRecipesView.vue
│   │   │   ├── FeedbackHistoryView.vue
│   │   │   ├── LoginView.vue
│   │   │   ├── MineView.vue
│   │   │   ├── PlanView.vue
│   │   │   ├── PreferencesView.vue
│   │   │   ├── ProfileSetupView.vue
│   │   │   ├── ProfileView.vue
│   │   │   ├── RecipeDetailView.vue
│   │   │   ├── RecipesView.vue
│   │   │   └── RecordView.vue
│   │   ├── App.vue
│   │   └── main.js
│   ├── index.html
│   ├── package.json
│   └── vite.config.js
│
├── database/                    # 本项目全部 SQL 统一放此目录
│   ├── migration/               # Flyway 版本脚本（唯一 DDL 来源；backend 打包时并入 classpath）
│   │   ├── V1__init_schema.sql
│   │   └── V2__ensure_post_like_comment_like_count.sql
│   ├── schema.sql               # 手工：建库并 SOURCE migration/V1（项目根执行）
│   ├── init-db.sql              # 仅建库
│   ├── fix_like_count_columns.sql  # 旧库补 like_count（Flyway 未开时，幂等）
│   └── demo_data.sql            # 演示数据，可选
│
├── backend/src/main/java/com/diet/
│   ├── common/
│   │   ├── GlobalExceptionHandler.java
│   │   └── Result.java
│   ├── config/
│   │   ├── DroolsConfig.java
│   │   ├── RedisConfig.java
│   │   ├── SecurityConfig.java
│   │   └── WebMvcConfig.java
│   ├── controller/
│   │   ├── AdminController.java
│   │   ├── AdminRecipeController.java
│   │   ├── AuthController.java
│   │   ├── FeedbackController.java
│   │   ├── PlanController.java
│   │   ├── RecipeController.java
│   │   └── UserController.java
│   ├── entity/
│   │   ├── BodyLog.java
│   │   ├── DietTag.java
│   │   ├── Feedback.java
│   │   ├── HealthProfile.java
│   │   ├── MealPlan.java
│   │   ├── Recipe.java
│   │   ├── SystemNotification.java
│   │   ├── User.java
│   │   └── UserPreference.java
│   ├── mapper/
│   │   ├── BodyLogMapper.java
│   │   ├── DietTagMapper.java
│   │   ├── FeedbackMapper.java
│   │   ├── HealthProfileMapper.java
│   │   ├── MealPlanMapper.java
│   │   ├── RecipeMapper.java
│   │   ├── SystemNotificationMapper.java
│   │   ├── UserMapper.java
│   │   └── UserPreferenceMapper.java
│   ├── security/
│   │   └── JwtAuthenticationFilter.java
│   ├── service/
│   │   ├── DietTagRuleService.java
│   │   ├── HealthProfileCalcService.java
│   │   ├── RedisService.java
│   │   ├── IAdminService.java
│   │   ├── IBodyLogService.java
│   │   ├── IDietTagService.java
│   │   ├── IFeedbackService.java
│   │   ├── IHealthProfileService.java
│   │   ├── IMealPlanService.java
│   │   ├── IRecipeService.java
│   │   ├── ISystemNotificationService.java
│   │   ├── IUserPreferenceService.java
│   │   ├── IUserService.java
│   │   └── impl/
│   ├── util/
│   │   └── JwtUtil.java
│   └── DietManagementApplication.java
│
├── backend/src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   ├── META-INF/
│   │   └── kmodule.xml         # Drools 配置
│   └── rules/
│       └── diet-label-rules.drl
│
├── scripts/
│   ├── start-all.bat           # Windows 一键启动
│   └── stop-all.bat
├── docker-compose.yml          # MySQL + Redis
└── README.md
```

## 核心功能模块

### 1. 用户认证

- 手机号 + 密码登录/注册
- JWT (access_token + refresh_token) 无状态认证
- 角色区分：USER / ADMIN
- BCrypt 密码加密

### 2. 健康档案

- 身高、体重、性别、年龄
- BMI 自动计算与评级（偏瘦/正常/偏胖/肥胖）
- TDEE 计算（Mifflin-St Jeor 公式 + 活动系数）
- 体重记录与变化曲线

### 3. Drools 规则引擎

- 根据 BMI、健身目标、过敏原生成饮食标签
- 规则：BMI 超重 → 低卡；增肌目标 → 高蛋白高卡；乳糖过敏 → 无乳制品；减脂+高强度运动 → 低碳高蛋白

### 4. 菜谱管理

- 菜谱 CRUD、图片上传
- 标签匹配、智能推荐（排除过敏原）
- 管理员审核（待审核/通过/拒绝）

### 5. 饮食计划

- 智能生成每日计划（早餐/午餐/晚餐/加餐）
- 热量分配（如 30%/40%/30%）
- 替换菜品、跳过某餐、标记外食
- 营养统计（蛋白质/碳水/脂肪）

### 6. 反馈与优化

- 餐后评价（五星、快捷标签、备注）
- 反馈驱动的推荐调整
- 偏好设置、反馈历史

### 7. 管理后台

- 仪表盘（用户数、计划数、待审核、平均评分）
- 用户管理（脱敏、禁用、重置密码）
- 菜谱审核中心
- 规则引擎管理
- 数据统计

### 8. PWA 与移动端

- PWA 配置（manifest、Service Worker）
- 响应式布局（Mobile / Tablet / Desktop）
- 手势：左右滑动、下拉刷新、长按菜单
- 图片懒加载、骨架屏、震动反馈、Web Share API

## 数据库设计

| 表名                | 说明                                          |
| ------------------- | --------------------------------------------- |
| user                | 用户（手机号、密码、角色）                    |
| health_profile      | 健康档案（身高体重、BMI、TDEE、目标、过敏原） |
| diet_tag            | 饮食标签（Drools 生成）                       |
| recipe              | 菜谱                                          |
| meal_plan           | 饮食计划                                      |
| feedback            | 反馈记录                                      |
| body_log            | 体重日志                                      |
| user_preference     | 用户偏好                                      |
| system_notification | 系统通知                                      |

## 数据库结构

- 使用 Flyway 进行版本管理；表结构唯一来源为 `database/migration/V1__init_schema.sql`（打包时由 Maven 映射到 `classpath:db/migration`）
- 手工一键建表可用 `database/schema.sql`（建库后 `SOURCE database/migration/V1__init_schema.sql`，须在项目根目录执行）
- 示例数据脚本见 `database/demo_data.sql`（可选，用于答辩演示）

## Redis 使用说明

- 用途1：接口限流（如体重记录频率限制）
- 用途2：热门菜谱缓存（Key: `recipe:popular:{category}`，过期 30 分钟）
- 缓存接口：
  - `GET /recipe/popular`
  - `POST /recipe/popular/refresh`
- 配置见 `application.yml` 中 `spring.data.redis`

## Drools 规则引擎

- 规则文件位置：`backend/src/main/resources/rules/`
- 饮食标签规则：`diet-label-rules.drl`
- 计划生成规则：`meal-plan-rules.drl`
- 调用方式：
  - `DietTagRuleService`（生成饮食标签）
  - `MealPlanRuleService`（输出计划建议：热量、模式、标记）

## 管理员功能

- 用户管理：查看、禁用、重置密码
- 菜谱审核：待审核列表、通过/拒绝
- 计划审核：按用户+日期审核计划，支持通过/驳回并通知用户
- 数据统计：用户增长、热门菜谱、目标分布

## 项目使用方法（完整指南）

### 零配置推荐启动（新增）

如果你要快速跑通“用户注册→健康档案→计划生成→反馈→管理员审核”全流程，推荐直接使用：

```bash
scripts\start-all.bat
```

该脚本会自动：

1. 通过 `docker compose` 启动 MySQL 和 Redis；
2. 启动后端（Spring Boot + Flyway 自动建表）；
3. 启动前端（Vite）；
4. 打开本地访问地址。

停止 Docker 依赖可执行：

```bash
scripts\stop-all.bat
```

默认连接参数已内置在 `application.yml` 的环境变量默认值中（可覆盖）：

- `DB_HOST`/`DB_PORT`/`DB_NAME`
- `DB_USERNAME`/`DB_PASSWORD`
- `REDIS_HOST`/`REDIS_PORT`/`REDIS_PASSWORD`

### 一、环境要求

| 环境    | 版本要求   | 说明                                   |
| ------- | ---------- | -------------------------------------- |
| JDK     | 17+        | 后端运行环境                           |
| Maven   | 3.6+       | 后端构建（或使用 IDE 内置）            |
| MySQL   | 8.0+       | 数据库，必须提前安装并启动             |
| Redis   | 任意稳定版 | 缓存，可选；未启动时部分缓存功能不可用 |
| Node.js | 18+        | 前端运行与构建                         |
| npm     | 9+         | 随 Node 安装即可                       |

### 二、首次使用：数据库准备

**方式 A：使用 Flyway 自动建表（推荐）**

1. 先创建空库（仅建库，表由 Flyway 创建）：
   ```bash
   mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS diet_management CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
   ```
2. 启动后端时 Flyway 会自动执行 `database/migration/` 下的脚本（打包进 `classpath:db/migration`），完成表结构创建。
3. 若需**演示/答辩用示例数据**，再执行：
   ```bash
   mysql -u root -p diet_management < database/demo_data.sql
   ```
   （需先有一次后端启动或从项目根执行过 `mysql … < database/schema.sql`，保证表已存在。）

**方式 B：完全手动初始化**

```bash
# 1. 建库 + 建表（在项目根目录；脚本会加载 database/migration/V1__init_schema.sql）
mysql -u root -p < database/schema.sql

# 2. （可选）导入演示数据，约 30 用户、150 菜谱、500 计划等
mysql -u root -p diet_management < database/demo_data.sql
```

- 脚本路径说明：
  - `database/schema.sql`：仅建库并导入 `database/migration/V1__init_schema.sql`（与 Flyway 一致）。
  - `database/demo_data.sql`：大量示例数据（用户、健康档案、菜谱、计划、反馈、体重、通知等），适合演示与答辩。

### 三、配置修改

编辑 `backend/src/main/resources/application.yml`，按本机环境修改：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/diet_management?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: 123456

  data:
    redis:
      host: localhost
      port: 6379
      password: # 无密码留空即可
```

- 若未安装 Redis，可暂时不改；后端会启动，仅 Redis 相关功能不可用。
- 若你使用 `database/schema.sql` 手工建表，建议保持
  `FLYWAY_ENABLED=false`（默认即 false），避免 Flyway 再次执行同一套 DDL。
- 若你想走 Flyway 自动迁移，请使用空库并设置 `FLYWAY_ENABLED=true`。

### 四、启动项目

**方式 1：使用一键启动脚本（Windows）**

在项目根目录执行：

```bash
scripts\start-all.bat
```

脚本会依次：

1. 启动 Docker 中的 MySQL 与 Redis（若已安装 Docker）。
2. 在新窗口启动后端（backend 目录，Spring Boot，端口 8080）。
3. 在新窗口安装前端依赖（若未安装）并启动前端开发服务器（端口 3000）。

两个窗口请保持打开，关闭窗口即停止对应服务。

**方式 2：手动分步启动**

1. **启动后端**（在 backend 目录）：

   ```bash
   cd backend
   mvn spring-boot:run
   ```

   - 成功后可访问：http://localhost:8080/api/swagger-ui.html

2. **启动前端**（新开一个终端）：

   ```bash
   cd frontend
   npm install
   npm run dev
   ```

   - 前端地址：http://localhost:3000

### 五、访问地址与演示账号

| 用途                   | 地址                                      |
| ---------------------- | ----------------------------------------- |
| 用户端（含移动端适配） | http://localhost:3000                     |
| 管理后台               | http://localhost:3000/admin               |
| API 文档（Swagger）    | http://localhost:8080/api/swagger-ui.html |

**若已导入 `database/demo_data.sql`，可使用以下演示账号：**

| 角色     | 手机号      | 密码   | 说明                           |
| -------- | ----------- | ------ | ------------------------------ |
| 管理员   | 13800138000 | 123456 | 系统管理员                     |
| 管理员   | 13800138001 | 123456 | 审核专员                       |
| 普通用户 | 13812345601 | 123456 | 减脂小王等（见脚本内更多用户） |

- 管理员登录后可从用户端右上角进入「管理后台」，或直接访问 `/admin`。

### 六、常用开发命令

```bash
# 后端（需先 cd backend）
mvn spring-boot:run          # 启动
mvn test                     # 单元测试
mvn package -DskipTests      # 打包

# 前端（需先 cd frontend）
npm install                  # 安装依赖
npm run dev                  # 开发模式
npm run build                # 生产构建
npm run preview              # 预览生产构建
```

### 七、常见问题

1. **后端启动报错：无法连接 MySQL**
   - 确认 MySQL 已启动，且 `application.yml` 中用户名、密码、库名正确。
   - 若本机 MySQL 非 3306 端口，请修改 `url` 中的端口。

2. **后端报错：Redis 连接失败**
   - 若未安装 Redis，可先注释或移除配置中的 `spring.data.redis`，或安装并启动 Redis。

3. **前端页面能打开但接口 404 / 401**
   - 确认后端已成功启动（http://localhost:8080/api/swagger-ui.html 能打开）。
   - 前端通过 Vite 代理将 `/api` 转发到 `http://localhost:8080`，请勿直接改后端 context-path。

4. **导入 demo_data.sql 报错**
   - 必须先有表结构：先在项目根执行 `database/schema.sql`，或先启动一次后端（Flyway 建表），再执行 `database/demo_data.sql`。
   - 示例数据需要 MySQL 8.0+（脚本中使用了 `WITH RECURSIVE`）。

5. **接口报错 `Unknown column 'like_count' in 'field list'`（post / post_comment）**
   - 原因：本地库是旧版手工建表或 **Flyway 默认关闭**（`FLYWAY_ENABLED` 未打开），表结构未随代码升级；`CREATE TABLE IF NOT EXISTS` 也不会给已有表加列。这是**库与后端实体不一致**，不是 Mapper 写错列名。
   - 处理：在目标库执行 `database/fix_like_count_columns.sql`（幂等），或设置 `FLYWAY_ENABLED=true` 后重启后端让 `database/migration/V2__ensure_post_like_comment_like_count.sql` 自动执行。Windows 下请用 **cmd** 执行 `mysql ... < database\fix_like_count_columns.sql`，勿用 PowerShell 管道导入（易损坏 SQL 引号）。

6. **端口被占用**
   - 后端端口在 `application.yml` 的 `server.port`（默认 8080）。
   - 前端端口在 `frontend/vite.config.js` 的 `server.port`（默认 3000）。修改后需重启对应服务。

---

## 快速开始（简要）

### 环境要求

- JDK 17+
- MySQL 8.0+
- Redis（可选）
- Node.js 18+

### 1. 数据库

```bash
# 创建数据库（或使用 Flyway 自动创建）
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS diet_management CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
# 表结构：首次启动后端由 Flyway 创建，或在项目根手动执行：
mysql -u root -p < database/schema.sql
# 演示数据（可选）：
mysql -u root -p diet_management < database/demo_data.sql
```

### 2. 配置

修改 `backend/src/main/resources/application.yml` 中的数据库连接（用户名、密码等），或通过环境变量覆盖（见根目录 `env.example`）。本地/服务器多环境说明见 **[docs/环境配置-本地与服务器.md](docs/环境配置-本地与服务器.md)**。

### 3. 启动

- **本机已装 MySQL/Redis**：双击 `scripts\一键启动.bat`（打开两个窗口分别启动后端与前端）。
- **使用 Docker 起数据库**：`scripts\start-all.bat`（需安装 Docker Desktop）。
- 或手动：先 `cd backend && mvn spring-boot:run`，再在 `frontend` 目录执行 `npm install` 与 `npm run dev`。

### 4. 访问

- 用户端：http://localhost:3000
- 管理后台：http://localhost:3000/admin
- API 文档：http://localhost:8080/api/swagger-ui.html

## API 接口概览

| 路径                           | 说明         |
| ------------------------------ | ------------ |
| POST /auth/register            | 注册         |
| POST /auth/login               | 登录         |
| GET /auth/me                   | 当前用户     |
| POST /auth/refresh             | 刷新 Token   |
| GET /user/health-profile       | 健康档案     |
| PUT /user/health-profile       | 更新健康档案 |
| POST /user/body-log            | 记录体重     |
| GET /user/body-log/chart       | 体重曲线     |
| GET /recipe/search             | 菜谱搜索     |
| POST /recipe/upload-image      | 图片上传     |
| GET /plan                      | 获取计划     |
| POST /plan/generate            | 生成计划     |
| POST /feedback                 | 提交反馈     |
| GET /admin/dashboard           | 仪表盘数据   |
| GET /admin/users               | 用户列表     |
| GET /admin/recipe/pending      | 待审核菜谱   |
| PUT /admin/recipe/{id}/approve | 审核通过     |
| PUT /admin/recipe/{id}/reject  | 审核拒绝     |

## 响应式断点

| 断点    | 宽度           | 说明                  |
| ------- | -------------- | --------------------- |
| Mobile  | < 768px        | 底部 Tabbar、卡片列表 |
| Tablet  | 768px - 1024px | -                     |
| Desktop | > 1024px       | 侧边栏、网格布局      |

## 许可证

MIT
