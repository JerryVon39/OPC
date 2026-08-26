@echo off
REM ============================================
REM Digital Nomad Innovation Works - One-click start (Docker)
REM For users who clone from GitHub: no need to
REM install MySQL/Redis/Java/Node, Docker only.
REM Usage: double-click or run scripts\start-all.bat
REM Stop:  scripts\stop-all.bat
REM ============================================
cd /d "%~dp0.."

REM 1. Check Docker
where docker >nul 2>&1
if errorlevel 1 (
  echo [start-all] Docker not found. Install it first: https://docs.docker.com/get-docker/
  pause
  exit /b 1
)
docker info >nul 2>&1
if errorlevel 1 (
  echo [start-all] Docker is not running. Trying to start Docker Desktop automatically...
  powershell -NoProfile -Command "$p1='C:\Program Files\Docker\Docker\Docker Desktop.exe'; $p2=\"$env:LOCALAPPDATA\Programs\DockerDesktop\Docker Desktop.exe\"; if(Test-Path $p1){Start-Process $p1} elseif(Test-Path $p2){Start-Process $p2} else {Write-Host 'Docker Desktop not found at default paths'}"
  set /a ok=0
  for /l %%i in (1,1,20) do (
    docker info >nul 2>&1
    if not errorlevel 1 set /a ok=1
    if %%i EQU 1 echo [start-all] Waiting for Docker Desktop, up to 60s...
    if %%i EQU 5 echo [start-all]   ... still waiting - Docker Desktop is starting...
    if %%i EQU 10 echo [start-all]   ... still waiting, check the Docker Desktop window...
    if %%i EQU 15 echo [start-all]   ... last attempts...
    ping -n 3 127.0.0.1 >nul
  )
  if not "%ok%"=="1" (
    echo [start-all] Docker Desktop did not start within ~60s. Please start it manually, then rerun this script.
    pause
    exit /b 1
  )
  echo [start-all] Docker Desktop is ready.
)

REM 2. Generate .env on first run (defaults work out of the box)
if not exist ".env" (
  copy .env.example .env >nul
  echo [start-all] .env generated with default values. Edit MAIL_* to enable email notifications.
)

REM 3. Start all services (first run pulls images and initializes the database)
echo [start-all] Starting MySQL / Redis / backend / frontend ...
docker image inspect jerryvon/opc-backend:v2.1 >nul 2>&1
if errorlevel 1 (
  echo [start-all] v2.1 image not found locally - building from source, takes a few minutes...
  docker compose build
  if errorlevel 1 (echo [start-all] Build failed & pause & exit /b 1)
)
docker compose up -d
if errorlevel 1 (
  echo [start-all] Failed to start. Check logs: docker compose logs -f
  pause
  exit /b 1
)

REM 4. Old data volume incremental upgrades (idempotent):
REM    existing mysql-data volume does NOT re-run init scripts, so upgrade
REM    scripts are re-applied to align schema with new code (del_flag/reader_id
REM    columns etc.), otherwise login/list fails with "Unknown column".
REM    !! Keep this list in sync with docker/mysql-upgrade.sh (single source:
REM    docker/mysql-init.sh for fresh volumes, mysql-upgrade.sh for old ones).
echo [start-all] Running DB incremental upgrades for existing volume (idempotent)...
for %%f in (sql\upgrade_20260818_purchase.sql sql\upgrade_20260819_mail.sql sql\upgrade_20260820_cleanup.sql sql\upgrade_20260821_official.sql sql\upgrade_20260822_realcontent.sql sql\upgrade_20260822_cms.sql sql\upgrade_20260823_cms.sql sql\upgrade_20260824_opc_cleanup.sql sql\upgrade_20260824_profile.sql sql\upgrade_20260824_two_state.sql sql\upgrade_20260824_auth.sql sql\upgrade_20260824_contest.sql sql\upgrade_20260824_roles.sql sql\upgrade_20260826_policy.sql sql\upgrade_20260824_menu_cleanup.sql sql\upgrade_20260825_recycle_menu.sql sql\upgrade_20260825_menu_reorg.sql sql\upgrade_20260825_recycle_cleanup.sql sql\upgrade_20260825_recycle_restore.sql sql\upgrade_20260825_editor_fix.sql sql\upgrade_20260825_cms_enhance.sql sql\upgrade_20260825_ops_workbench.sql sql\upgrade_20260825_menu_fix.sql sql\upgrade_20260825_menu_dedupe.sql sql\upgrade_20260825_cms_block.sql sql\upgrade_20260825_operator_block.sql sql\upgrade_20260825_cms_section.sql sql\upgrade_20260825_section_fix.sql sql\upgrade_20260825_section_fix2.sql sql\upgrade_20260825_home_polish.sql sql\upgrade_20260825_home_fill.sql sql\upgrade_20260825_cms_unify.sql sql\upgrade_20260825_preview.sql sql\upgrade_20260825_hide_book_menu.sql sql\upgrade_20260825_block_v3.sql sql\upgrade_20260825_block_v3_seed.sql sql\upgrade_20260826_menu_fix2.sql sql\upgrade_20260826_engine_merge.sql sql\upgrade_20260826_site_settings.sql sql\upgrade_20260826_article_history.sql sql\upgrade_20260826_recycle_purge_job.sql) do (
  type "%%f" | docker exec -i opc-mysql sh -c "mysql --default-character-set=utf8mb4 -uroot -p\"$MYSQL_ROOT_PASSWORD\" ry-vue" >nul 2>&1
)
echo [start-all] DB incremental upgrades done

REM 4. Wait for backend to be ready (first pull is slow, up to ~3 minutes)
echo [start-all] Waiting for backend ...
set /a ok=0
for /l %%i in (1,1,90) do (
  rem 后端不再映射宿主机 8080，改经 80 端口 nginx /prod-api 探测后端响应
  for /f "delims=" %%c in ('curl -s -o nul -w "%%{http_code}" --max-time 3 http://localhost/prod-api/') do (
    if not "%%c"=="000" if not "%%c"=="502" if not "%%c"=="503" if not "%%c"=="504" (
      set /a ok=1
      goto ready
    )
  )
  ping -n 3 127.0.0.1 >nul
)
:ready
if "%ok%"=="0" (
  echo [start-all] Backend not ready in time. Check logs: docker compose logs -f backend
  pause
  exit /b 1
)
echo [start-all] Backend is ready.

echo.
echo ============================================
echo   Startup complete!
echo   Admin:  http://localhost/index.html   (login with the randomized admin password from deployment handover)
echo   Reader: http://localhost/home.html
echo   Stop:   scripts\stop-all.bat
echo ============================================
pause
