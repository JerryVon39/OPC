@echo off
chcp 65001 >nul
REM ============================================
REM 数智游民创新工场 · 手动导出数据库数据快照
REM 作用：把当前数据库的业务数据（成员/服务/文章等）导出到
REM       sql\data_snapshot.sql，commit + push 后 GitHub 即同步。
REM 提示：git commit 时 .githooks\pre-commit 会自动执行同样导出，
REM       本脚本用于"改了数据但不想提交代码"时手动刷新快照。
REM 用法：双击运行，或命令行 scripts\export-data.bat
REM ============================================
setlocal
cd /d "%~dp0.."

if not exist ".env" copy .env.example .env >nul
for /f "eol=# delims=" %%a in (.env) do set "%%a"
if "%DB_PASSWORD%"=="" set "DB_PASSWORD=password"
if "%TOOLS_HOME%"=="" set "TOOLS_HOME=%USERPROFILE%\tools"

set "MYSQLDUMP=%TOOLS_HOME%\mysql-8.4.9-winx64\bin\mysqldump.exe"
set "OUT=sql\data_snapshot.sql"
set "TABLES=book reader borrow_record shop_order book_reserve book_purchase_req sys_banner sys_notice sys_notice_read cms_category cms_article sys_dict_type sys_dict_data"

"%MYSQLDUMP%" --default-character-set=utf8mb4 -uroot -p%DB_PASSWORD% --no-create-info --replace --skip-comments --skip-add-locks --skip-lock-tables --set-gtid-purged=OFF ry-vue %TABLES% > "%OUT%" 2>nul
if errorlevel 1 (
  echo [export-data] 导出失败 - 请确认 MySQL 已启动（start-local 或 Docker）
  pause
  exit /b 1
)

(
  echo -- ============================================
  echo -- 数据快照：由 scripts\export-data.bat 手动生成（%date% %time%）
  echo -- 来源：数智游民创新工场官网数据库（ry-vue）业务数据
  echo -- 导入（可重复执行，REPLACE 模式）：
  echo --   mysql --default-character-set=utf8mb4 -uroot -p ry-vue ^< sql\data_snapshot.sql
  echo -- 提示：本文件为自动生成，请勿手工编辑
  echo -- ============================================
  echo.
  type "%OUT%"
) > "%OUT%.tmp"
move /y "%OUT%.tmp" "%OUT%" >nul

echo [export-data] 数据快照已更新: %OUT%
echo [export-data] 请 commit + push 即可同步到 GitHub:
echo   git add sql\data_snapshot.sql
echo   git commit -m "data: 刷新数据快照"
echo   git push
pause
exit /b 0
