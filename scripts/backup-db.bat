@echo off
rem =====================================================
rem  万事屋 数据库一键备份脚本
rem  用法：双击运行（会弹出窗口，备份完按任意键关闭）
rem  配合"任务计划程序"可定时自动备份
rem  备份文件：backup\ry-vue_YYYYMMDD_HHMM.sql
rem =====================================================

rem ---- 配置区（按实际环境修改）----
set MYSQL_BIN=C:\Users\1\tools\mysql-8.4.9-winx64\bin
set DB_USER=root
set DB_PASS=password
set DB_NAME=ry-vue
rem 备份目录（脚本所在目录的上一级的 backup 文件夹）
set BACKUP_DIR=%~dp0..\backup
rem ---------------------------------

if not exist "%BACKUP_DIR%" mkdir "%BACKUP_DIR%"

rem 生成日期时间戳：20260813_0945
set STAMP=%date:~0,4%%date:~5,2%%date:~8,2%_%time:~0,2%%time:~3,2%
set FILE=%BACKUP_DIR%\ry-vue_%STAMP%.sql

echo [1/2] 正在备份数据库 %DB_NAME% ...
"%MYSQL_BIN%\mysqldump.exe" -u%DB_USER% -p%DB_PASS% --default-character-set=utf8mb4 --single-transaction %DB_NAME% > "%FILE%"

if %errorlevel%==0 (
  echo [OK] 备份成功：%FILE%
) else (
  echo [FAIL] 备份失败！请检查：
  echo       1. MySQL 服务是否已启动
  echo       2. 上方配置区的账号密码是否正确
)

echo.
pause
