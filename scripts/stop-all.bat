@echo off
REM ============================================
REM Book System - One-click stop (Docker)
REM Stops containers but keeps data volumes.
REM ============================================
cd /d "%~dp0.."

where docker >nul 2>&1
if errorlevel 1 (
  echo [stop-all] Docker not found.
  pause
  exit /b 1
)

docker compose down
echo [stop-all] All services stopped. Data volumes kept - run scripts\start-all.bat to resume.
pause
