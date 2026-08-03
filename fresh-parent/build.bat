@echo off
chcp 65001 >nul
title Fresh 生鲜团购系统 - Docker 一键部署
color 0A

echo ============================================
echo    Fresh 生鲜团购系统 - Docker 一键部署
echo ============================================
echo.

REM ====== 检查环境 ======
echo [1/5] 检查环境...
where docker >nul 2>nul
if %errorlevel% neq 0 (
    echo [错误] 未检测到 Docker，请先安装 Docker Desktop
    pause
    exit /b 1
)
where docker-compose >nul 2>nul
if %errorlevel% neq 0 (
    echo [错误] 未检测到 docker-compose
    pause
    exit /b 1
)
where mvn >nul 2>nul
if %errorlevel% neq 0 (
    echo [警告] 未检测到 Maven，请确认已在本地打包好 jar 文件
    echo         如果已手动打包，按任意键继续；否则请先安装 Maven
    pause
)
echo [完成] 环境检查通过
echo.

REM ====== 编译打包 ======
echo [2/5] Maven 编译打包...
echo.

REM 先安装公共模块到本地仓库
echo    安装 fresh-common-core 到本地仓库...
cd /d %~dp0fresh-common-core
call mvn clean install -DskipTests -q
if %errorlevel% neq 0 (
    echo [错误] fresh-common-core 打包失败
    pause
    exit /b 1
)
echo    [完成] fresh-common-core

cd /d %~dp0
echo    打包所有微服务...
call mvn clean package -DskipTests -q
if %errorlevel% neq 0 (
    echo [错误] Maven 打包失败，请检查编译错误
    pause
    exit /b 1
)
echo [完成] Maven 编译打包成功
echo.

REM ====== 构建 Docker 镜像 ======
echo [3/5] 构建 Docker 镜像...
echo.

cd /d %~dp0

echo    构建 fresh-user...
docker build -t fresh/fresh-user:latest ./fresh-user
if %errorlevel% neq 0 goto :build_error

echo    构建 fresh-goods...
docker build -t fresh/fresh-goods:latest ./fresh-goods
if %errorlevel% neq 0 goto :build_error

echo    构建 fresh-order...
docker build -t fresh/fresh-order:latest ./fresh-order
if %errorlevel% neq 0 goto :build_error

echo    构建 fresh-gateway...
docker build -t fresh/fresh-gateway:latest ./fresh-gateway
if %errorlevel% neq 0 goto :build_error

echo    构建 fresh-ai...
docker build -t fresh/fresh-ai:latest ./fresh-ai
if %errorlevel% neq 0 goto :build_error

echo    构建 fresh-message...
docker build -t fresh/fresh-message:latest ./fresh-message
if %errorlevel% neq 0 goto :build_error

echo    构建 fresh-data...
docker build -t fresh/fresh-data:latest ./fresh-data
if %errorlevel% neq 0 goto :build_error

echo [完成] 所有镜像构建成功
echo.

REM ====== 启动服务 ======
echo [4/5] 启动所有服务...
echo.

docker-compose up -d
if %errorlevel% neq 0 (
    echo [错误] 服务启动失败
    pause
    exit /b 1
)

echo [完成] 所有服务已启动
echo.

REM ====== 显示状态 ======
echo [5/5] 显示服务状态...
echo.
timeout /t 5 /nobreak >nul

docker-compose ps

echo.
echo ============================================
echo    部署完成！服务访问地址：
echo ============================================
echo.
echo    API 网关:  http://localhost:8080
echo    Nacos 控制台: http://localhost:8848/nacos
echo    Redis:      localhost:6379
echo    MySQL:      localhost:3306 (root/root)
echo    RocketMQ:   localhost:9876 (NameServer)
echo.
echo    微服务端口:
echo      fresh-user:     8081
echo      fresh-goods:    8082
echo      fresh-order:    8083
echo      fresh-ai:       8084
echo      fresh-message:  8085
echo      fresh-data:     8086
echo.
echo    常用命令:
echo      查看日志:  docker-compose logs -f [服务名]
echo      停止服务:  docker-compose down
echo      重启服务:  docker-compose restart [服务名]
echo      查看状态:  docker-compose ps
echo.
echo ============================================
pause
exit /b 0

:build_error
echo.
echo [错误] Docker 镜像构建失败
echo.
pause
exit /b 1
