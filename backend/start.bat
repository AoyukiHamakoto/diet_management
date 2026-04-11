@echo off
chcp 65001 >nul
title 健身饮食规划系统 - 启动
echo.
echo ========================================
echo   健身饮食规划系统 - 快捷启动
echo ========================================
echo.

:: 检查是否在项目根目录（存在 pom.xml 和 frontend 目录）
if not exist "pom.xml" (
    echo [错误] 请在项目根目录执行 start.bat（需存在 pom.xml）
    pause
    exit /b 1
)
if not exist "frontend\package.json" (
    echo [错误] 未找到 frontend\package.json，请确认项目结构完整
    pause
    exit /b 1
)

:: 1. 启动后端（新窗口）
echo [1/3] 正在启动后端（Spring Boot :8080）...
start "Diet-Backend" cmd /k "title Diet-Backend && mvn spring-boot:run"
echo       后端窗口已打开，请勿关闭。
echo.

:: 等待后端部分启动后再启动前端
echo [2/3] 等待后端启动（约 15 秒）...
timeout /t 15 /nobreak >nul
echo.

:: 2. 启动前端（新窗口）
echo [3/3] 正在启动前端（Vite :3000）...
start "Diet-Frontend" cmd /k "title Diet-Frontend && cd /d "%~dp0frontend" && (if not exist node_modules npm install) && npm run dev"
echo       前端窗口已打开，请勿关闭。
echo.

:: 等待前端服务就绪后打开浏览器
echo 等待前端服务就绪（约 10 秒）...
timeout /t 10 /nobreak >nul
start "" "http://localhost:3000"
echo.
echo ========================================
echo   已尝试打开浏览器：http://localhost:3000
echo   管理后台：http://localhost:3000/admin
echo   API 文档：http://localhost:8080/api/swagger-ui.html
echo ========================================
echo   关闭后端/前端窗口即可停止对应服务。
echo.
pause
