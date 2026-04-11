@echo off
chcp 65001 >nul
setlocal
set "PROJECT_ROOT=%~dp0.."
pushd "%PROJECT_ROOT%"

echo 正在停止 Docker 依赖服务...
docker compose down

echo.
echo 已停止 MySQL/Redis 容器。
echo 如需完全停止项目，请同时关闭 Diet-Backend 和 Diet-Frontend 窗口。
echo.
pause
popd
