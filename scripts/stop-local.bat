@echo off
REM ============================================
REM Book System - One-click stop (local dev)
REM Kills the windows started by start-local.bat (backend/frontend/native
REM MySQL/Redis), then cleans any leftovers on our ports.
REM Docker containers are NOT touched - use scripts\stop-all.bat for those.
REM ============================================
cd /d "%~dp0.."
title Stop Book System services

echo Stopping Book System services ...
REM stop windows started by start-local.bat (exact window titles)
taskkill /FI "WINDOWTITLE eq wanshiwu-backend*" /T /F >nul 2>&1
taskkill /FI "WINDOWTITLE eq wanshiwu-frontend*" /T /F >nul 2>&1
taskkill /FI "WINDOWTITLE eq wanshiwu-mysql*" /T /F >nul 2>&1
taskkill /FI "WINDOWTITLE eq wanshiwu-redis*" /T /F >nul 2>&1
REM let the process trees die before scanning ports (avoids a race where a
REM listener is mid-teardown and the scan below misses it)
ping -n 3 127.0.0.1 >nul
REM cleanup leftovers on our ports (never touch Docker Desktop)
powershell -NoProfile -Command "Get-NetTCPConnection -LocalPort 8080,8081,3306,6379 -State Listen -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique | ForEach-Object { $p = Get-CimInstance Win32_Process -Filter ('ProcessId=' + $_); if ($p -and $p.Name -notmatch 'com.docker.backend|docker-proxy|dockerd') { Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue; Write-Host ('  stopped PID ' + $_ + ' (' + $p.Name + ')') } }"
echo.
echo Done. Database data kept - run scripts\start-local.bat to resume.
pause
