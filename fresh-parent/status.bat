@echo off
chcp 65001 >nul
title Fresh 生鲜团购系统 - 查看服务状态
echo.
cd /d %~dp0
docker-compose ps
echo.
echo ============================================
echo    查看实时日志:
echo      docker-compose logs -f        (所有服务)
echo      docker-compose logs -f gateway (指定服务)
echo ============================================
echo.
pause
