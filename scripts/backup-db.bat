@echo off
chcp 65001 >nul
rem =====================================================
rem  数智游民创新工场 · 数据库一键备份脚本
rem  用法：双击运行；配合「任务计划程序」可定时自动备份
rem  备份文件：backup\ry-vue_YYYYMMDD_HHMM.sql
rem  R14 fix: 原硬编码 C:\Users\1\tools + DB_PASS=password 换机即失效；
rem          现读取 .env（DB_PASSWORD/TOOLS_HOME），时间戳改 PowerShell 生成（无空格）
rem =====================================================
setlocal
cd /d "%~dp0.."

rem ---- 读取 .env（缺省用演示默认值） ----
if not exist ".env" copy .env.example .env >nul
for /f "eol=# delims=" %%a in (.env) do set "%%a"
if "%DB_PASSWORD%"=="" set "DB_PASSWORD=password"
if "%TOOLS_HOME%"=="" set "TOOLS_HOME=%USERPROFILE%\tools"

rem ---- 定位 mysqldump（PATH 优先，其次 TOOLS_HOME） ----
set "MYSQLDUMP="
where mysqldump >nul 2>&1
if not errorlevel 1 set "MYSQLDUMP=mysqldump"
if "%MYSQLDUMP%"=="" if exist "%TOOLS_HOME%\mysql-8.4.9-winx64\bin\mysqldump.exe" set "MYSQLDUMP=%TOOLS_HOME%\mysql-8.4.9-winx64\bin\mysqldump.exe"
if "%MYSQLDUMP%"=="" (
  echo [FAIL] mysqldump 未找到：请将 MySQL bin 加入 PATH，或在 .env 设置 TOOLS_HOME
  pause & exit /b 1
)

rem ---- 时间戳（PowerShell 生成，小时<10 也无空格） ----
for /f "delims=" %%s in ('powershell -NoProfile -Command "Get-Date -Format 'yyyyMMdd_HHmm'"') do set "STAMP=%%s"
set "BACKUP_DIR=%~dp0..\backup"
if not exist "%BACKUP_DIR%" mkdir "%BACKUP_DIR%"
set "FILE=%BACKUP_DIR%\ry-vue_%STAMP%.sql"

echo [1/2] 正在备份数据库 ry-vue ...
"%MYSQLDUMP%" -uroot -p%DB_PASSWORD% --default-character-set=utf8mb4 --single-transaction ry-vue > "%FILE%" 2>nul

if errorlevel 1 (
  echo [FAIL] 备份失败，请检查：
  echo       1. MySQL 是否正在运行
  echo       2. .env 中的 DB_PASSWORD 是否正确
  echo       3. mysqldump 版本与 MySQL 版本是否一致
  pause & exit /b 1
)

echo [OK] 备份成功：%FILE%
echo.
pause
