@echo off
setlocal
set "PROJECT_ROOT=%~dp0.."
pushd "%PROJECT_ROOT%"

echo ========================================
echo   Diet Management - local dev (2 windows)
echo ========================================
echo   Backend :8080 /api   Frontend :3000
echo   Start MySQL and Redis before running.
echo ========================================
echo.

if not exist "backend\pom.xml" (
  echo [ERROR] Missing backend\pom.xml. Run from scripts folder inside project.
  pause
  popd
  exit /b 1
)
if not exist "frontend\package.json" (
  echo [ERROR] Missing frontend\package.json
  pause
  popd
  exit /b 1
)

where mvn >nul 2>nul
if errorlevel 1 (
  echo [ERROR] mvn not found. Install Maven and add to PATH.
  pause
  popd
  exit /b 1
)
where node >nul 2>nul
if errorlevel 1 (
  echo [ERROR] node not found. Install Node.js LTS.
  pause
  popd
  exit /b 1
)

echo [1/2] Starting Spring Boot backend...
rem MySQL: root / 123456 (same as application.yml and docker-compose)
start "Diet-Backend" cmd /k "title Diet-Backend && cd /d ""%CD%\backend"" && set DB_USERNAME=root&& set DB_PASSWORD=123456&& mvn spring-boot:run"

timeout /t 6 /nobreak >nul

echo [2/2] Starting Vite frontend...
start "Diet-Frontend" cmd /k "title Diet-Frontend && cd /d ""%CD%\frontend"" && (if not exist node_modules npm install) && npm run dev"

echo.
echo Done. Close each window to stop that service.
echo   App:      http://localhost:3000
echo   Admin:    http://localhost:3000/admin
echo   Swagger:  http://localhost:8080/api/swagger-ui.html
echo.
echo For Docker MySQL/Redis use: scripts\start-all.bat
echo.
pause
popd
