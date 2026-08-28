@echo off
REM ============================================
REM Shuzhi Youmin Innovation Workspace - service manager (plan A)
REM Usage: scripts\svc.bat start | stop | status | restart
REM Manages: MySQL(3306) / Redis(6379) / backend(8080) / frontend(8081)
REM Note: if MySQL/Redis/backend are registered as Windows services
REM (install-services.bat), use `net start/stop` instead - this script
REM kills by port including service processes.
REM ============================================
setlocal
cd /d "%~dp0.."
goto :main

:check_port
netstat -an | findstr /C:":%1 " | findstr LISTENING >nul
if errorlevel 1 (exit /b 1) else (exit /b 0)

:kill_port
REM G-5 fix: 进程名白名单（mysqld/redis-server/java/node）——保留「停本项目服务」语义，
REM 避免误杀占用同端口的其他业务进程
powershell -NoProfile -Command "Get-NetTCPConnection -LocalPort %1 -State Listen -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique | ForEach-Object { $p = Get-CimInstance Win32_Process -Filter ('ProcessId=' + $_); if ($p -and $p.Name -match 'mysqld|redis-server|java|node') { Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue; Write-Host ('  stopped PID ' + $_ + ' (' + $p.Name + ')') } }"
exit /b 0

:env
if not exist ".env" copy .env.example .env >nul
for /f "eol=# delims=" %%a in (.env) do set "%%a"
if "%FE_PORT%"=="" set "FE_PORT=8081"
if "%TOOLS_HOME%"=="" set "TOOLS_HOME=%USERPROFILE%\tools"
if not exist logs mkdir logs
REM R-N7 fix: TOKEN_SECRET 缺失/默认时生成（后端守卫拒启；node 实测通过），
REM 生成后重新加载 .env 使后端进程继承
if "%TOKEN_SECRET%"=="" set NEED_SECRET=1
if "%TOKEN_SECRET%"=="25a96a6099a0cc7c37fa1d412ab9712479d32e0b5d9e470e8f6f522271ab2c7c" set NEED_SECRET=1
if "%NEED_SECRET%"=="1" (
  echo [svc] TOKEN_SECRET empty or default - generating...
  for /f "delims=" %%k in ('node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"') do set "NEW_SECRET=%%k"
  powershell -NoProfile -Command "$p=(Join-Path (Get-Location) '.env'); $c=[IO.File]::ReadAllText($p); if ($c -match '(?m)^TOKEN_SECRET=.*$') { $c=[regex]::Replace($c,'(?m)^TOKEN_SECRET=.*$','TOKEN_SECRET=%NEW_SECRET%') } else { $c += [Environment]::NewLine + 'TOKEN_SECRET=%NEW_SECRET%' + [Environment]::NewLine }; [IO.File]::WriteAllText($p,$c,(New-Object System.Text.UTF8Encoding($false)))"
  for /f "eol=# delims=" %%a in (.env) do set "%%a"
)
goto :eof

:start
call :env
set "MYSQLD=%TOOLS_HOME%\mysql-8.4.9-winx64\bin\mysqld.exe"
set "REDIS_SRV=%TOOLS_HOME%\redis\redis-server.exe"
set "BACKEND_JAR=%CD%\ruoyi-admin\target\ruoyi-admin.jar"

echo [start] checking ports...
call :check_port 3306
if errorlevel 1 (
  echo [start] starting MySQL...
  start "wanshiwu-mysql" "%MYSQLD%" --basedir="%TOOLS_HOME%\mysql-8.4.9-winx64" --datadir="%TOOLS_HOME%\mysql-data" --port=3306 --console
) else (
  echo [start] MySQL already running
)

call :check_port 6379
if errorlevel 1 (
  echo [start] starting Redis...
  start "wanshiwu-redis" "%REDIS_SRV%" --port 6379
) else (
  echo [start] Redis already running
)

if not exist "%BACKEND_JAR%" (
  echo [start] backend jar missing - build first: mvn -pl ruoyi-admin -am package -DskipTests
) else (
  call :check_port 8080
  if errorlevel 1 (
    echo [start] starting backend...
    start "wanshiwu-backend" cmd /k "java -jar ruoyi-admin\target\ruoyi-admin.jar > logs\backend.log 2>&1"
  ) else (
    echo [start] backend already running
  )
)

if not exist "ruoyi-ui\node_modules" (
  echo [start] frontend deps missing - run: cd ruoyi-ui ^&^& npm install
) else (
  call :check_port %FE_PORT%
  if errorlevel 1 (
    echo [start] starting frontend, first compile 30-60s...
    start "wanshiwu-frontend" cmd /k "cd /d ruoyi-ui && npm run dev -- --no-open --port=%FE_PORT% > ..\logs\frontend.log 2>&1"
  ) else (
    echo [start] frontend already running
  )
)

echo.
echo ============================================
echo   All services starting - wait 30-60s
echo   Frontend: http://localhost:%FE_PORT%/home.html
echo   Admin:    http://localhost:%FE_PORT%/  (login with the randomized admin password)
echo   Status:   scripts\svc.bat status
echo   Stop:     scripts\svc.bat stop
echo ============================================
goto :eof

:stop
echo [stop] stopping frontend(%FE_PORT%)...
call :kill_port %FE_PORT%
echo [stop] stopping backend(8080)...
call :kill_port 8080
echo [stop] stopping Redis(6379)...
call :kill_port 6379
echo [stop] stopping MySQL(3306)...
call :kill_port 3306
echo [stop] all stopped
goto :eof

:status
echo ============================================
echo   Service status  (%date% %time%)
echo ============================================
call :check_port 3306
if errorlevel 1 (echo   MySQL  3306 : stopped) else (echo   MySQL  3306 : running)
call :check_port 6379
if errorlevel 1 (echo   Redis  6379 : stopped) else (echo   Redis  6379 : running)
call :check_port 8080
if errorlevel 1 (echo   Backend 8080 : stopped) else (echo   Backend 8080 : running)
call :check_port %FE_PORT%
if errorlevel 1 (echo   Frontend %FE_PORT% : stopped) else (echo   Frontend %FE_PORT% : running)
echo.
echo   Logs: logs\  (backend.log / frontend.log)
goto :eof

:main
call :env
if /i "%~1"=="start"   goto :start
if /i "%~1"=="stop"    goto :stop
if /i "%~1"=="status"  goto :status
if /i "%~1"=="restart" (call :stop & call :start & goto :eof)
echo Usage: svc.bat start ^| stop ^| status ^| restart
exit /b 1
