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
    if not errorlevel 1 (set /a ok=1 & goto docker_ready)
    if %%i EQU 1  echo [start-all] Waiting for Docker Desktop (up to 60s)...
    if %%i EQU 5  echo [start-all]   ... still waiting (Docker Desktop is starting)...
    if %%i EQU 10 echo [start-all]   ... still waiting, check the Docker Desktop window...
    if %%i EQU 15 echo [start-all]   ... last attempts...
    ping -n 3 127.0.0.1 >nul
  )
  :docker_ready
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
docker image inspect jerryvon/book-system-backend:v2.0 >nul 2>&1
if errorlevel 1 (
  echo [start-all] v2.0 image not found locally - building from source (takes a few minutes)...
  docker compose build
  if errorlevel 1 (echo [start-all] Build failed & pause & exit /b 1)
)
docker compose up -d
if errorlevel 1 (
  echo [start-all] Failed to start. Check logs: docker compose logs -f
  pause
  exit /b 1
)

REM 4. Wait for backend to be ready (first pull is slow, up to ~3 minutes)
echo [start-all] Waiting for backend ...
set /a ok=0
for /l %%i in (1,1,90) do (
  curl -s -o nul http://localhost:8080/ >nul 2>&1
  if not errorlevel 1 (
    set /a ok=1
    goto ready
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
echo   Admin:  http://localhost/          admin / admin123 (change password on first login)
echo   Reader: http://localhost/shop.html
echo   Stop:   scripts\stop-all.bat
echo ============================================
pause
