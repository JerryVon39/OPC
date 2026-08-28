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

REM 2. Generate .env on first run
if not exist ".env" (
  copy .env.example .env >nul
  echo [start-all] .env generated. Edit MAIL_* to enable email notifications.
)
REM Load .env (DB password needed for the incremental upgrade step below)
for /f "eol=# delims=" %%a in (.env) do set "%%a"
if "%DB_PASSWORD%"=="" set "DB_PASSWORD=%MYSQL_ROOT_PASSWORD%"

REM 2b. TOKEN_SECRET guard: backend refuses to start with the repo-default key
REM      (TokenService.checkSecretNotDefault). Auto-generate a strong random one
REM      when missing or equal to the repo default - otherwise docker compose
REM      injects the default key and the backend crash-loops (B1 fix).
set NEED_SECRET=0
if "%TOKEN_SECRET%"=="" set NEED_SECRET=1
if "%TOKEN_SECRET%"=="25a96a6099a0cc7c37fa1d412ab9712479d32e0b5d9e470e8f6f522271ab2c7c" set NEED_SECRET=1
if "%NEED_SECRET%"=="1" (
  echo [start-all] TOKEN_SECRET empty or default - generating a strong random one...
  REM N-1 fix: 原 PowerShell 生成命令含 %{ 与 ''{0:x}''（cmd % 展开破坏 + PS 语法错误，
  REM 实测 NEW_SECRET 恒空）；改用 node（项目必有）生成 64 hex，实测通过
  for /f "delims=" %%k in ('node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"') do set "NEW_SECRET=%%k"
  REM N-8 fix: 写回用 UTF-8 无 BOM（原 -Encoding ASCII 会把中文替换成 ?）；
  REM 兼容 .env 无 TOKEN_SECRET 行的情况（追加而非仅替换）
  powershell -NoProfile -Command "$p=(Join-Path (Get-Location) '.env'); $c=[IO.File]::ReadAllText($p); if ($c -match '(?m)^TOKEN_SECRET=.*$') { $c=[regex]::Replace($c,'(?m)^TOKEN_SECRET=.*$','TOKEN_SECRET=%NEW_SECRET%') } else { $c += [Environment]::NewLine + 'TOKEN_SECRET=%NEW_SECRET%' + [Environment]::NewLine }; [IO.File]::WriteAllText($p,$c,(New-Object System.Text.UTF8Encoding($false)))"
  for /f "eol=# delims=" %%a in (.env) do set "%%a"
  echo [start-all] TOKEN_SECRET generated and saved to .env
)

REM 3. Start all services (first run pulls images and initializes the database)
echo [start-all] Starting MySQL / Redis / backend / frontend ...
docker image inspect jerryvon/opc-backend:v2.3 >nul 2>&1
if errorlevel 1 (
  echo [start-all] v2.3 image not found locally - building from source, takes a few minutes...
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
REM    (I1: wildcard scan sql\upgrade_*.sql - filename order = execution order;
REM    single source is the sql/ directory itself)
REM    B2 fix: 原 \"$MYSQL_ROOT_PASSWORD\" 转义被 cmd/sh 双重扭曲导致密码带引号
REM    → 认证静默失败；改经宿主机 %DB_PASSWORD%（.env）直连并检查 errorlevel
echo [start-all] Running DB incremental upgrades for existing volume (idempotent)...
for /f "delims=" %%f in ('dir /b /on sql\upgrade_*.sql') do (
  echo   [start-all] applying sql\%%f
  type "sql\%%f" | docker exec -i opc-mysql mysql --default-character-set=utf8mb4 -uroot -p%DB_PASSWORD% ry-vue >nul 2>&1
  if errorlevel 1 (
    echo [start-all] ERROR: upgrade %%f failed - check DB_PASSWORD in .env or: docker compose logs mysql
    pause & exit /b 1
  )
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
echo   Admin:  http://localhost/index.html   (admin / admin123, 首次登录请修改)
echo   Reader: http://localhost/home.html
echo   Stop:   scripts\stop-all.bat
echo ============================================
pause
