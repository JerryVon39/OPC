@echo off
chcp 65001 >nul
REM ============================================
REM Digital Nomad Innovation Works - One-click start (local dev, no Docker required)
REM Flow (same as the proven local start-all.bat):
REM   clean ports -> Redis -> MySQL -> backend -> frontend -> print URLs
REM
REM Portable: works from any git clone. Requirements:
REM   - JDK 17+ / Node.js (npm) on PATH; Maven only needed for first build
REM   - MySQL + Redis: either native binaries (on PATH, or under TOOLS_HOME
REM     set in .env, default %USERPROFILE%\tools), or Docker Desktop
REM     (falls back to `docker compose up -d mysql redis` automatically)
REM
REM Frontend port: 8081 (override FE_PORT in .env)
REM Stop: scripts\stop-local.bat
REM ============================================
setlocal
cd /d "%~dp0.."

REM ---------- 0. Load .env (create from example on first run) ----------
if not exist ".env" copy .env.example .env >nul
for /f "eol=# delims=" %%a in (.env) do set "%%a"
if "%DB_PASSWORD%"=="" set "DB_PASSWORD=password"
if "%FE_PORT%"=="" set "FE_PORT=8081"
if "%TOOLS_HOME%"=="" set "TOOLS_HOME=%USERPROFILE%\tools"
if not exist logs mkdir logs

REM ---------- 1. Prerequisite check ----------
where java >nul 2>&1
if errorlevel 1 (echo [1/5] JDK 17+ not found in PATH. Install: https://adoptium.net/ & goto :fail)
where node >nul 2>&1
if errorlevel 1 (echo [1/5] Node.js not found in PATH. Install: https://nodejs.org/ & goto :fail)
where npm >nul 2>&1
if errorlevel 1 (echo [1/5] npm not found in PATH & goto :fail)

REM ---------- 2. Clean leftover processes on our ports (never touch Docker Desktop) ----------
echo [2/5] Cleaning ports 8080/8081/3306/6379 ...
powershell -NoProfile -Command "Get-NetTCPConnection -LocalPort 8080,8081,3306,6379 -State Listen -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique | ForEach-Object { $p = Get-CimInstance Win32_Process -Filter ('ProcessId=' + $_); if ($p -and $p.Name -notmatch 'com.docker.backend|docker-proxy|dockerd') { Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue; Write-Host ('  stopped PID ' + $_ + ' (' + $p.Name + ')') } }"
ping -n 3 127.0.0.1 >nul

REM ---------- 3. Redis (port 6379): running? -> native -> docker -> fail ----------
netstat -an | findstr /C:":6379 " | findstr LISTENING >nul
if not errorlevel 1 (echo [3/5] Redis already running & goto :redis_done)

set "REDIS_BIN="
where redis-server >nul 2>&1
if not errorlevel 1 set "REDIS_BIN=redis-server"
if "%REDIS_BIN%"=="" if exist "%TOOLS_HOME%\redis\redis-server.exe" set "REDIS_BIN=%TOOLS_HOME%\redis\redis-server.exe"
if not "%REDIS_BIN%"=="" goto :redis_native

where docker >nul 2>&1
if errorlevel 1 (echo [3/5] Redis unavailable: install Redis, or install Docker & goto :fail)
docker info >nul 2>&1
if errorlevel 1 (echo [3/5] Docker not running. Start Docker Desktop, or install Redis & goto :fail)
echo [3/5] No native Redis, starting container ...
docker compose up -d redis >nul 2>&1
if errorlevel 1 (echo [3/5] Docker redis start failed & goto :fail)
set REDIS_READY=0
for /l %%i in (1,1,10) do (
  netstat -an | findstr /C:":6379 " | findstr LISTENING >nul
  if not errorlevel 1 (set REDIS_READY=1 & goto redis_ok)
  ping -n 2 127.0.0.1 >nul
)
:redis_ok
if "%REDIS_READY%"=="0" (echo [3/5] redis container not ready in 20s & goto :fail)
echo [3/5] Redis ready - container
goto :redis_done

:redis_native
echo [3/5] Starting native Redis ...
start "wanshiwu-redis" "%REDIS_BIN%"
ping -n 3 127.0.0.1 >nul
netstat -an | findstr /C:":6379 " | findstr LISTENING >nul
if errorlevel 1 (echo [3/5] Redis failed to start & goto :fail)
echo [3/5] Redis ready

:redis_done

REM ---------- 4. MySQL (port 3306): reachable? -> native -> docker -> fail ----------
set "MYSQL_BIN="
where mysql >nul 2>&1
if not errorlevel 1 set "MYSQL_BIN=mysql"
if "%MYSQL_BIN%"=="" if exist "%TOOLS_HOME%\mysql-8.4.9-winx64\bin\mysql.exe" set "MYSQL_BIN=%TOOLS_HOME%\mysql-8.4.9-winx64\bin\mysql.exe"
if "%MYSQL_BIN%"=="" (echo [4/5] mysql client not found. Add MySQL bin to PATH or set TOOLS_HOME in .env & goto :fail)

if not "%MYSQL_BIN%"=="" (
  "%MYSQL_BIN%" --default-character-set=utf8mb4 -uroot -p%DB_PASSWORD% -e "SELECT 1" >nul 2>&1
  if not errorlevel 1 (echo [4/5] MySQL already running & goto :mysql_done)
)

set "MYSQLD="
where mysqld >nul 2>&1
if not errorlevel 1 set "MYSQLD=mysqld"
if "%MYSQLD%"=="" if exist "%TOOLS_HOME%\mysql-8.4.9-winx64\bin\mysqld.exe" set "MYSQLD=%TOOLS_HOME%\mysql-8.4.9-winx64\bin\mysqld.exe"
if not "%MYSQLD%"=="" goto :mysql_native

where docker >nul 2>&1
if errorlevel 1 (echo [4/5] MySQL unavailable: install MySQL, or install Docker & goto :fail)
docker info >nul 2>&1
if errorlevel 1 (echo [4/5] Docker not running. Start Docker Desktop, or install MySQL & goto :fail)
echo [4/5] No native MySQL, starting container - first start initializes DB ...
docker compose up -d mysql >nul 2>&1
if errorlevel 1 (echo [4/5] Docker mysql start failed & goto :fail)
goto :mysql_wait_docker

:mysql_native
echo [4/5] Starting native MySQL ...
REM if TOOLS_HOME has the mysql-data layout, point mysqld at it explicitly
REM (a bare `mysqld --console` would use its compiled-in default datadir and
REM come up empty); otherwise start with defaults.
if exist "%TOOLS_HOME%\mysql-data" (
  start "wanshiwu-mysql" %MYSQLD% --basedir="%TOOLS_HOME%\mysql-8.4.9-winx64" --datadir="%TOOLS_HOME%\mysql-data" --port=3306 --console
) else (
  start "wanshiwu-mysql" %MYSQLD% --console
)
goto :mysql_wait_native

:mysql_wait_native
REM wait for 3306, then verify root/DB_PASSWORD works (native: up to 40s)
set MYSQL_READY=0
for /l %%i in (1,1,20) do (
  netstat -an | findstr /C:":3306 " | findstr LISTENING >nul
  if not errorlevel 1 (
    "%MYSQL_BIN%" --default-character-set=utf8mb4 -uroot -p%DB_PASSWORD% -e "SELECT 1" >nul 2>&1
    if not errorlevel 1 (set MYSQL_READY=1 & goto mysql_ok)
  )
  ping -n 2 127.0.0.1 >nul
)
goto mysql_ok

:mysql_wait_docker
REM container start can be slower on first init (up to 60s)
set MYSQL_READY=0
for /l %%i in (1,1,60) do (
  netstat -an | findstr /C:":3306 " | findstr LISTENING >nul
  if not errorlevel 1 (
    "%MYSQL_BIN%" --default-character-set=utf8mb4 -uroot -p%DB_PASSWORD% -e "SELECT 1" >nul 2>&1
    if not errorlevel 1 (set MYSQL_READY=1 & goto mysql_ok)
  )
  ping -n 2 127.0.0.1 >nul
)
:mysql_ok
if "%MYSQL_READY%"=="0" (echo [4/5] MySQL not ready - check DB_PASSWORD in .env or the wanshiwu-mysql window & goto :fail)
echo [4/5] MySQL ready

:mysql_done

REM ---------- 5. Database init (fresh: full import / existing: idempotent upgrades) ----------
"%MYSQL_BIN%" --default-character-set=utf8mb4 -uroot -p%DB_PASSWORD% -N -e "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='ry-vue'" > logs\db_check.txt 2>nul
set /p DB_EXISTS=<logs\db_check.txt
set TABLE_COUNT=0
if not "%DB_EXISTS%"=="0" (
  "%MYSQL_BIN%" --default-character-set=utf8mb4 -uroot -p%DB_PASSWORD% -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='ry-vue' AND table_name IN ('sys_menu','book','shop_order')" > logs\db_check.txt 2>nul
  set /p TABLE_COUNT=<logs\db_check.txt
)
if "%DB_EXISTS%"=="0" goto :db_fresh
if not "%TABLE_COUNT%"=="3" goto :db_fresh
echo [5/5] Existing DB detected. Running idempotent upgrades...
REM 幂等升级全清单（与 docker/mysql-upgrade.sh 一致，另含 auth/recycle_menu/reorg/cleanup）：
REM   - purchase 旧脚本含旧名父菜单 INSERT，在业务化库上重跑会产生孤儿菜单，
REM     由清单末尾的 menu_cleanup 统一清理（getRouters NPE 防御）
REM   - auth 依赖 two_state 的 del_flag，必须排在 two_state 之后
REM   - recycle 旧脚本（三态快照建表）已废弃删除，不再执行
for %%f in (sql\upgrade_20260818_purchase.sql sql\upgrade_20260819_mail.sql sql\upgrade_20260819_menu.sql sql\upgrade_20260820_cleanup.sql sql\upgrade_20260821_official.sql sql\upgrade_20260822_realcontent.sql sql\upgrade_20260822_cms.sql sql\upgrade_20260823_cms.sql sql\upgrade_20260824_opc_cleanup.sql sql\upgrade_20260824_profile.sql sql\upgrade_20260824_two_state.sql sql\upgrade_20260824_auth.sql sql\upgrade_20260824_contest.sql sql\upgrade_20260824_roles.sql sql\upgrade_20260826_policy.sql sql\upgrade_20260824_menu_cleanup.sql sql\upgrade_20260825_recycle_menu.sql sql\upgrade_20260825_menu_reorg.sql sql\upgrade_20260825_recycle_cleanup.sql sql\upgrade_20260825_recycle_restore.sql upgrade_20260825_editor_fix.sql) do (
  if exist "%%f" (
    echo   executing %%f
    "%MYSQL_BIN%" --default-character-set=utf8mb4 -uroot -p%DB_PASSWORD% ry-vue < "%%f" >nul
    if errorlevel 1 (echo [5/5] upgrade %%f failed & goto :fail)
  )
)
goto :db_done

:db_fresh
echo [5/5] Fresh DB detected. Creating database and importing init scripts...
"%MYSQL_BIN%" --default-character-set=utf8mb4 -uroot -p%DB_PASSWORD% -e "CREATE DATABASE IF NOT EXISTS `ry-vue` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci"
if errorlevel 1 (echo [5/5] create database failed & goto :fail)
for %%f in (sql\ry_20260417.sql sql\quartz.sql sql\business_init.sql sql\role_init.sql) do (
  echo   importing %%f
  "%MYSQL_BIN%" --default-character-set=utf8mb4 -uroot -p%DB_PASSWORD% ry-vue < "%%f" >nul
  if errorlevel 1 (echo [5/5] import %%f failed & goto :fail)
)
REM 全新库与 Docker 首次初始化对齐：补跑全部幂等升级（del_flag/password_hash/CMS/菜单等）+ 业务数据快照
echo   applying idempotent upgrades...
for %%f in (sql\upgrade_20260818_purchase.sql sql\upgrade_20260819_mail.sql sql\upgrade_20260819_menu.sql sql\upgrade_20260820_cleanup.sql sql\upgrade_20260821_official.sql sql\upgrade_20260822_realcontent.sql sql\upgrade_20260822_cms.sql sql\upgrade_20260823_cms.sql sql\upgrade_20260824_opc_cleanup.sql sql\upgrade_20260824_profile.sql sql\upgrade_20260824_two_state.sql sql\upgrade_20260824_auth.sql sql\upgrade_20260824_contest.sql sql\upgrade_20260824_roles.sql sql\upgrade_20260826_policy.sql sql\upgrade_20260824_menu_cleanup.sql sql\upgrade_20260825_recycle_menu.sql sql\upgrade_20260825_menu_reorg.sql sql\upgrade_20260825_recycle_cleanup.sql sql\upgrade_20260825_recycle_restore.sql upgrade_20260825_editor_fix.sql) do (
  if exist "%%f" (
    echo   applying %%f
    "%MYSQL_BIN%" --default-character-set=utf8mb4 -uroot -p%DB_PASSWORD% ry-vue < "%%f" >nul
    if errorlevel 1 (echo [5/5] upgrade %%f failed & goto :fail)
  )
)
if exist "sql\data_snapshot.sql" (
  echo   applying sql\data_snapshot.sql
  "%MYSQL_BIN%" --default-character-set=utf8mb4 -uroot -p%DB_PASSWORD% ry-vue < "sql\data_snapshot.sql" >nul
  if errorlevel 1 (echo [5/5] data_snapshot failed & goto :fail)
)
:db_done

REM ---------- 6. Backend (port 8080) ----------
if exist "ruoyi-admin\target\ruoyi-admin.jar" (
  echo [6/5] Backend jar exists, skipping build
) else (
  where mvn >nul 2>&1
  if errorlevel 1 (echo [6/5] Maven not found in PATH - needed for first build & goto :fail)
  echo [6/5] Building backend - first run takes 1-2 min...
  call mvn -q -pl ruoyi-admin -am package -DskipTests
  if errorlevel 1 (echo [6/5] Build failed & goto :fail)
)
echo [6/5] Starting backend ...
start "wanshiwu-backend" cmd /k "echo [backend] running - close this window to stop & java -jar ruoyi-admin\target\ruoyi-admin.jar"
echo [6/5] Waiting for backend - up to 40s...
set BACKEND_READY=0
for /l %%i in (1,1,20) do (
  curl -s -o nul http://localhost:8080/ >nul 2>&1
  if not errorlevel 1 (set BACKEND_READY=1 & goto backend_ok)
  ping -n 2 127.0.0.1 >nul
)
:backend_ok
if "%BACKEND_READY%"=="0" (echo [6/5] Backend not ready in 40s - check the wanshiwu-backend window & goto :fail)
echo [6/5] Backend ready

REM ---------- 7. Frontend (port %FE_PORT%) ----------
if not exist "ruoyi-ui\node_modules" (
  echo [7/5] Installing frontend dependencies - first run takes 1-2 min...
  pushd ruoyi-ui
  call npm install
  if errorlevel 1 (popd & echo [7/5] npm install failed & goto :fail)
  popd
)
echo [7/5] Starting frontend - port %FE_PORT% ...
REM vue-cli 4.x: --port must use the equals form (space form is treated as webpack entry)
start "wanshiwu-frontend" cmd /k "cd /d ruoyi-ui && npm run dev -- --no-open --port=%FE_PORT%"
echo [7/5] Waiting for frontend compile - up to 120s...
set FE_URL=
for /l %%i in (1,1,30) do (
  curl -s --max-time 3 "http://localhost:%FE_PORT%/dev-api/captchaImage" | findstr /C:"code" >nul
  if not errorlevel 1 (set "FE_URL=http://localhost:%FE_PORT%" & goto frontend_ok)
  ping -n 2 127.0.0.1 >nul
)
:frontend_ok
if "%FE_URL%"=="" (echo [7/5] Frontend not ready in 120s - check the wanshiwu-frontend window & goto :fail)
echo [7/5] Frontend ready

echo.
echo ============================================
echo   All services started!
echo   Admin:  %FE_URL%/              admin / Ee606EcUQsgj�:	
echo   Reader: %FE_URL%/home.html
echo   Stop:   scripts\stop-local.bat
echo ============================================
pause
exit /b 0

:fail
echo.
echo [start-local] FAILED - see messages above.
pause
exit /b 1
