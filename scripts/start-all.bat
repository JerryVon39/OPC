@echo off
REM ============================================
REM Book System - One-click start (Docker)
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
  echo [start-all] Docker is not running. Please start Docker Desktop first.
  pause
  exit /b 1
)

REM 2. Generate .env on first run (defaults work out of the box)
if not exist ".env" (
  copy .env.example .env >nul
  echo [start-all] .env generated with default values. Edit MAIL_* to enable email notifications.
)

REM 3. Start all services (first run pulls images and initializes the database)
echo [start-all] Starting MySQL / Redis / backend / frontend ...
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
