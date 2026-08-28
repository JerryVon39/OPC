@echo off
chcp 65001 >nul
REM ============================================
REM 数智游民创新工场 · 手动导出数据库数�?�?�?
REM 作用：把当前数据库的业务数据（成�?/服务/文章等）导出�?
REM       sql\data_snapshot.sql，commit + push �? GitHub 即同步�??
REM 提示：git commit �? .githooks\pre-commit 会自动执行同样�?�出�?
REM       �?脚本用于"改了数据但不想提交代�?"时手动刷新快照�??
REM 用法：双击运行，或命令�?? scripts\export-data.bat
REM ============================================
setlocal
cd /d "%~dp0.."

if not exist ".env" copy .env.example .env >nul
for /f "eol=# delims=" %%a in (.env) do set "%%a"
if "%DB_PASSWORD%"=="" set "DB_PASSWORD=password"
if "%TOOLS_HOME%"=="" set "TOOLS_HOME=%USERPROFILE%\tools"

set "MYSQLDUMP=%TOOLS_HOME%\mysql-8.4.9-winx64\bin\mysqldump.exe"
set "OUT=sql\data_snapshot.sql"
REM 隐�?�排除：reader（成员手�?/�?箱）�? book_purchase_req（申请人信息）与 .githooks/pre-commit �?致，不�?�出（H4 �?复）
set "TABLES=book borrow_record shop_order book_reserve sys_banner sys_notice sys_notice_read cms_category sys_dict_type sys_dict_data cms_block cms_page"

"%MYSQLDUMP%" --default-character-set=utf8mb4 -uroot -p%DB_PASSWORD% --no-create-info --replace --skip-comments --skip-add-locks --skip-lock-tables --set-gtid-purged=OFF ry-vue %TABLES% > "%OUT%" 2>nul

"%MYSQLDUMP%" --default-character-set=utf8mb4 -uroot -p%DB_PASSWORD% --no-create-info --replace --skip-comments --skip-add-locks --skip-lock-tables --set-gtid-purged=OFF --where="del_flag != '2'" ry-vue cms_article >> "%OUT%" 2>nul

"%MYSQLDUMP%" --default-character-set=utf8mb4 -uroot -p%DB_PASSWORD% --no-create-info --replace --skip-comments --skip-add-locks --skip-lock-tables --set-gtid-purged=OFF --where="config_key NOT IN ('opc.apply.notify.email','site_email')" ry-vue sys_config >> "%OUT%" 2>nul
if errorlevel 1 (
  echo [export-data] 导出失败 - 请确�? MySQL 已启�?（start-local �? Docker�?
  pause
  exit /b 1
)

(
  echo -- ============================================
  echo -- 数据�?照：�? scripts\export-data.bat 手动生成�?%date% %time%�?
  echo -- 来源：数智游民创新工场官网数�?库（ry-vue）业务数�?
  echo -- 导入（可重�?�执行，REPLACE 模式）：
  echo --   mysql --default-character-set=utf8mb4 -uroot -p ry-vue ^< sql\data_snapshot.sql
  echo -- 提示：本文件为自动生成，请勿手工编辑
  echo -- ============================================
  echo.
  type "%OUT%"
) > "%OUT%.tmp"
move /y "%OUT%.tmp" "%OUT%" >nul

echo [export-data] 数据�?照已更新: %OUT%
echo [export-data] �? commit + push 即可同�?�到 GitHub:
echo   git add sql\data_snapshot.sql
echo   git commit -m "data: 刷新数据�?�?"
echo   git push
pause
exit /b 0
