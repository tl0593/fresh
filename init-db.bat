@echo off
chcp 65001 >nul
echo ========================================
echo  Fresh 生鲜团购 - 数据库初始化脚本
echo ========================================
echo.
echo 默认连接: localhost:3307  用户: root  密码: root
echo 如使用本地 MySQL(3306)，请修改下方 MYSQL_HOST 和 MYSQL_PORT
echo.

set MYSQL_HOST=127.0.0.1
set MYSQL_PORT=3307
set MYSQL_USER=root
set MYSQL_PASSWORD=root

set SQL_DIR=%~dp0SQL

where mysql >nul 2>&1
if errorlevel 1 (
    echo [错误] 未找到 mysql 客户端，请先安装 MySQL 或将 mysql 加入 PATH
    echo.
    echo 也可使用 Docker 自动初始化:
    echo   cd fresh-parent
    echo   docker compose up -d mysql
    echo Docker 会将 SQL 目录挂载到 /docker-entrypoint-initdb.d 自动执行
    pause
    exit /b 1
)

echo 正在初始化数据库...
for %%f in (
    00-nacos-init.sql
    01-fresh-user-service.sql
    02-fresh-goods-service.sql
    03-fresh-order-service.sql
    04-fresh-ai-service.sql
    05-fresh-message-service.sql
    06-fresh-data-service.sql
    07-fresh-gateway-service.sql
    08-fresh-common-core.sql
    09-fresh-comment.sql
    10-fresh-promotion.sql
    99-seed-data.sql
) do (
    echo   执行 %%f ...
    mysql -h%MYSQL_HOST% -P%MYSQL_PORT% -u%MYSQL_USER% -p%MYSQL_PASSWORD% < "%SQL_DIR%\%%f"
    if errorlevel 1 (
        echo [失败] %%f 执行出错，请检查 MySQL 是否已启动、账号密码是否正确
        pause
        exit /b 1
    )
)

echo.
echo [完成] 全部 12 个数据库初始化成功！
echo.
echo 各服务数据库:
echo   fresh_user / fresh_goods / fresh_order / fresh_ai
echo   fresh_message / fresh_data / fresh_gateway / fresh_common
echo   fresh_comment / fresh_promotion / nacos_config
echo.
pause
