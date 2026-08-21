@echo off
chcp 65001 >nul
REM ============================================
REM 数智游民创新工场 · 服务管理脚本（方案 A：PID/端口统一管理）
REM 用法: scripts\svc.bat start | stop | status | restart
REM 说明: MySQL(3306) / Redis(6379) / 后端(8080) / 前端(8081)
REM       若 MySQL/Redis/后端已注册为 Windows 服务（install-services.bat），
REM       请改用 net start/stop 管理，本脚本 stop 会按端口杀进程（含服务进程）。
REM ============================================
setlocal
cd /d "%~dp0.."
goto :main

:check_port
netstat -an | findstr /C:":%1 " | findstr LISTENING >nul
if errorlevel 1 (exit /b 1) else (exit /b 0)

:kill_port
powershell -NoProfile -Command "Get-NetTCPConnection -LocalPort %1 -State Listen -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique | ForEach-Object { Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue }"
exit /b 0

:start
if not exist ".env" copy .env.example .env >nul
for /f "eol=# delims=" %%a in (.env) do set "%%a"
if "%DB_PASSWORD%"=="" set "DB_PASSWORD=password"
if "%FE_PORT%"=="" set "FE_PORT=8081"
if "%TOOLS_HOME%"=="" set "TOOLS_HOME=%USERPROFILE%\tools"
if not exist logs mkdir logs
set "MYSQLD=%TOOLS_HOME%\mysql-8.4.9-winx64\bin\mysqld.exe"
set "REDIS_SRV=%TOOLS_HOME%\redis\redis-server.exe"
set "BACKEND_JAR=%CD%\ruoyi-admin\target\ruoyi-admin.jar"

echo [start] 检查各服务端口...
call :check_port 3306
if errorlevel 1 (
  echo [start] MySQL 未运行，启动中...
  start "wanshiwu-mysql" "%MYSQLD%" --basedir="%TOOLS_HOME%\mysql-8.4.9-winx64" --datadir="%TOOLS_HOME%\mysql-data" --port=3306 --console
) else (
  echo [start] MySQL 已在运行
)

call :check_port 6379
if errorlevel 1 (
  echo [start] Redis 未运行，启动中...
  start "wanshiwu-redis" "%REDIS_SRV%" --port 6379
) else (
  echo [start] Redis 已在运行
)

if not exist "%BACKEND_JAR%" (
  echo [start] 后端 jar 不存在，请先构建: mvn -pl ruoyi-admin -am package -DskipTests
) else (
  call :check_port 8080
  if errorlevel 1 (
    echo [start] 后端未运行，启动中...
    start "wanshiwu-backend" cmd /k "echo [backend] 日志: logs\backend.log - 关闭此窗口即停止 & java -jar "%BACKEND_JAR%" > logs\backend.log 2>&1"
  ) else (
    echo [start] 后端已在运行
  )
)

if not exist "ruoyi-ui\node_modules" (
  echo [start] 前端依赖缺失，请先执行: cd ruoyi-ui ^&^& npm install
) else (
  call :check_port %FE_PORT%
  if errorlevel 1 (
    echo [start] 前端未运行，启动中（首次编译约 30-60 秒）...
    start "wanshiwu-frontend" cmd /k "cd /d ruoyi-ui && npm run dev -- --no-open --port=%FE_PORT% > ..\logs\frontend.log 2>&1"
  ) else (
    echo [start] 前端已在运行
  )
)

echo.
echo ============================================
echo   启动完成！请稍候 30-60 秒等待服务就绪
echo   前台官网: http://localhost:%FE_PORT%/shop.html
echo   后台管理: http://localhost:%FE_PORT%/  (admin / admin123)
echo   查看状态: scripts\svc.bat status
echo   停止服务: scripts\svc.bat stop
echo ============================================
exit /b 0

:stop
echo [stop] 停止前端(%FE_PORT%)...
call :kill_port %FE_PORT%
echo [stop] 停止后端(8080)...
call :kill_port 8080
echo [stop] 停止 Redis(6379)...
call :kill_port 6379
echo [stop] 停止 MySQL(3306)...
call :kill_port 3306
echo [stop] 全部已停止
exit /b 0

:status
echo ============================================
echo   服务状态（%date% %time%）
echo ============================================
call :check_port 3306
if errorlevel 1 (echo   MySQL  3306 : 已停止) else (echo   MySQL  3306 : 运行中)
call :check_port 6379
if errorlevel 1 (echo   Redis  6379 : 已停止) else (echo   Redis  6379 : 运行中)
call :check_port 8080
if errorlevel 1 (echo   后端   8080 : 已停止) else (echo   后端   8080 : 运行中)
call :check_port %FE_PORT%
if errorlevel 1 (echo   前端   %FE_PORT% : 已停止) else (echo   前端   %FE_PORT% : 运行中)
echo.
echo   日志目录: logs\  (backend.log / frontend.log)
exit /b 0

:main
if /i "%~1"=="start"   goto :start
if /i "%~1"=="stop"    goto :stop
if /i "%~1"=="status"  goto :status
if /i "%~1"=="restart" (call :stop & call :start & exit /b 0)
echo 用法: svc.bat start ^| stop ^| status ^| restart
exit /b 1
