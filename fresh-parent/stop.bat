@echo off
chcp 65001 >nul
title Fresh 生鲜团购系统 - 停止所有服务
echo 正在停止所有服务...
cd /d %~dp0
docker-compose down
echo.
echo [完成] 所有服务已停止
pause
