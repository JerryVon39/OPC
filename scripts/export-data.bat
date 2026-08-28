@echo off
chcp 65001 >nul
REM ============================================
REM 数智游民创新工场 · 手动导出数据库数据快照
REM 作用：把当前数据库的业务数据（服务/文章/配置等）导出到
REM       sql\data_snapshot.sql，commit + push 到 GitHub 即同步
REM 提示：git commit 时 .githooks\pre-commit 会自动执行同样导出；
REM       本脚本用于"改了数据但不想提交代码"时手动刷新快照
REM 用法：双击运行，或命令行执行 scripts\export-data.bat
REM R15 fix: 原只有最后一条 mysqldump 检查 errorlevel，主导出失败也会
REM          组装残缺文件覆盖快照——现每步检查，失败即中止并保留现场
REM ============================================
setlocal
cd /d "%~dp0.."

if not exist ".env" copy .env.example .env >nul
for /f "eol=# delims=" %%a in (.env) do set "%%a"
if "%DB_PASSWORD%"=="" set "DB_PASSWORD=password"
if "%TOOLS_HOME%"=="" set "TOOLS_HOME=%USERPROFILE%\tools"

set "MYSQLDUMP=%TOOLS_HOME%\mysql-8.4.9-winx64\bin\mysqldump.exe"
if not exist "%MYSQLDUMP%" set "MYSQLDUMP=mysqldump"
set "OUT=sql\data_snapshot.sql"
set "TMP=%OUT%.tmp"
REM 隐私排除：reader（成员手机/邮箱）、book_purchase_req（申请人信息）与 .githooks/pre-commit 一致，不导出
set "TABLES=book borrow_record shop_order book_reserve sys_banner sys_notice sys_notice_read cms_category sys_dict_type sys_dict_data cms_block cms_page"

if exist "%TMP%" del "%TMP%"

echo [1/4] 导出业务表 ...
"%MYSQLDUMP%" --default-character-set=utf8mb4 -uroot -p%DB_PASSWORD% --no-create-info --replace --skip-comments --skip-add-locks --skip-lock-tables --set-gtid-purged=OFF ry-vue %TABLES% > "%TMP%" 2>nul
if errorlevel 1 (echo [FAIL] 业务表导出失败 - 请确认 MySQL 已启动、.env 的 DB_PASSWORD 正确 & del "%TMP%" 2>nul & pause & exit /b 1)

echo [2/4] 导出文章（过滤回收站行）...
"%MYSQLDUMP%" --default-character-set=utf8mb4 -uroot -p%DB_PASSWORD% --no-create-info --replace --skip-comments --skip-add-locks --skip-lock-tables --set-gtid-purged=OFF --where="del_flag != '2'" ry-vue cms_article >> "%TMP%" 2>nul
if errorlevel 1 (echo [FAIL] 文章导出失败 & del "%TMP%" 2>nul & pause & exit /b 1)

echo [3/4] 导出站点配置（排除隐私邮箱键）...
"%MYSQLDUMP%" --default-character-set=utf8mb4 -uroot -p%DB_PASSWORD% --no-create-info --replace --skip-comments --skip-add-locks --skip-lock-tables --set-gtid-purged=OFF --where="config_key NOT IN ('opc.apply.notify.email','site_email')" ry-vue sys_config >> "%TMP%" 2>nul
if errorlevel 1 (echo [FAIL] 配置导出失败 & del "%TMP%" 2>nul & pause & exit /b 1)

echo [4/4] 组装文件头...
(
  echo -- ============================================
  echo -- 数据快照：由 scripts\export-data.bat 手动生成（%date% %time%）
  echo -- 来源：数智游民创新工场官网数据库（ry-vue）业务数据
  echo -- 导入（可重复执行，REPLACE 模式）：
  echo --   mysql --default-character-set=utf8mb4 -uroot -p ry-vue ^< sql\data_snapshot.sql
  echo -- 提示：本文件为自动生成，请勿手工编辑
  echo -- ============================================
  echo.
  type "%TMP%"
) > "%OUT%"
if errorlevel 1 (echo [FAIL] 文件组装失败 & del "%TMP%" 2>nul & pause & exit /b 1)
del "%TMP%" 2>nul

echo [OK] 数据快照已更新: %OUT%
echo [OK] 再 commit + push 即可同步到 GitHub：
echo        git add sql\data_snapshot.sql
echo        git commit -m "data: 刷新数据快照"
echo        git push
pause
exit /b 0
