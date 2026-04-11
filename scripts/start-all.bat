@echo off
chcp 65001 >nul
setlocal
set "PROJECT_ROOT=%~dp0.."
pushd "%PROJECT_ROOT%"

echo ========================================
echo  Diet Management - 一键启动
echo ========================================
echo.

if not exist "backend\pom.xml" (
  echo [错误] backend 目录缺少 pom.xml
  pause
  exit /b 1
)

where docker >nul 2>nul
if errorlevel 1 (
  echo [错误] 未检测到 Docker，请先安装 Docker Desktop
  pause
  exit /b 1
)

echo [1/4] 启动 MySQL/Redis 容器...
docker compose up -d
if errorlevel 1 (
  echo [错误] docker compose 启动失败
  pause
  exit /b 1
)

echo [2/4] 等待数据库初始化(约 15-30 秒)...
timeout /t 20 /nobreak >nul

echo [3/4] 启动后端...
rem Docker MySQL root 密码 123456，与 application.yml 一致
start "Diet-Backend" cmd /k "title Diet-Backend && cd /d ""%PROJECT_ROOT%\backend"" && set DB_USERNAME=root&& set DB_PASSWORD=123456&& mvn spring-boot:run"

echo [4/4] 启动前端...
start "Diet-Frontend" cmd /k "title Diet-Frontend && cd /d ""%PROJECT_ROOT%\frontend"" && (if not exist node_modules npm install) && npm run dev"

echo.
echo 前端: http://localhost:3000
echo 后端: http://localhost:8080/api/swagger-ui.html
echo 管理后台: http://localhost:3000/admin
echo.
echo 提示: 首次启动会执行 Flyway 自动建表，请等待后端日志显示 Started
echo.
pause
popd
