@echo off
chcp 65001 >nul
REM ============================================
REM 数智游民创新工场 · Windows 服务化安装（方案 B：nssm）
REM 把 MySQL / Redis / 后端注册为 Windows 服务：
REM   - 开机自启（SERVICE_AUTO_START）
REM   - 崩溃自动重启（AppExit Default Restart）
REM   - 独立于终端会话，日志写入 logs\
REM 用法: 以管理员身份运行 scripts\install-services.bat
REM 卸载: scripts\uninstall-services.bat（同样需管理员）
REM 管理: net start/stop wanshiwu-mysql|wanshiwu-redis|wanshiwu-backend
REM ============================================
setlocal
cd /d "%~dp0.."

REM ---------- 管理员权限检查 ----------
net session >nul 2>&1
if errorlevel 1 (
  echo [错误] 请以管理员身份运行本脚本（右键 - 以管理员身份运行）
  pause & exit /b 1
)

if not exist ".env" copy .env.example .env >nul
for /f "eol=# delims=" %%a in (.env) do set "%%a"
if "%TOOLS_HOME%"=="" set "TOOLS_HOME=%USERPROFILE%\tools"
if not exist logs mkdir logs

set "NSSM_DIR=%TOOLS_HOME%\nssm"
set "NSSM=%NSSM_DIR%\nssm.exe"
set "ROOT=%CD%"

REM ---------- 准备 nssm（不存在则自动下载解压） ----------
if not exist "%NSSM%" (
  echo [1/4] nssm 未找到，自动下载 nssm 2.24 ...
  powershell -NoProfile -Command "Invoke-WebRequest -Uri 'https://nssm.cc/release/nssm-2.24.zip' -OutFile '%TOOLS_HOME%\nssm.zip' -UseBasicParsing"
  if errorlevel 1 (echo [错误] nssm 下载失败，请手动下载 https://nssm.cc/release/nssm-2.24.zip 解压到 %TOOLS_HOME%\nssm\ & pause & exit /b 1)
  powershell -NoProfile -Command "Expand-Archive -Path '%TOOLS_HOME%\nssm.zip' -DestinationPath '%TOOLS_HOME%' -Force; New-Item -ItemType Directory -Force -Path '%NSSM_DIR%' | Out-Null; Move-Item '%TOOLS_HOME%\nssm-2.24\win64\nssm.exe' '%NSSM%' -Force; Remove-Item '%TOOLS_HOME%\nssm.zip' -Force; Remove-Item '%TOOLS_HOME%\nssm-2.24' -Recurse -Force -ErrorAction SilentlyContinue"
  if errorlevel 1 (echo [错误] nssm 解压失败 & pause & exit /b 1)
)
if not exist "%NSSM%" (echo [错误] nssm.exe 不存在于 %NSSM% & pause & exit /b 1)
echo [1/4] nssm 就绪: %NSSM%

REM ---------- 服务名冲突检查 ----------
sc query wanshiwu-mysql >nul 2>&1 && (echo [错误] 服务 wanshiwu-mysql 已存在，请先卸载 & pause & exit /b 1)
sc query wanshiwu-redis >nul 2>&1 && (echo [错误] 服务 wanshiwu-redis 已存在，请先卸载 & pause & exit /b 1)
sc query wanshiwu-backend >nul 2>&1 && (echo [错误] 服务 wanshiwu-backend 已存在，请先卸载 & pause & exit /b 1)

REM ---------- 1. MySQL 服务 ----------
echo [2/4] 注册 MySQL 服务 ...
"%NSSM%" install wanshiwu-mysql "%TOOLS_HOME%\mysql-8.4.9-winx64\bin\mysqld.exe" --basedir="%TOOLS_HOME%\mysql-8.4.9-winx64" --datadir="%TOOLS_HOME%\mysql-data" --port=3306
"%NSSM%" set wanshiwu-mysql AppDirectory "%TOOLS_HOME%\mysql-8.4.9-winx64\bin"
"%NSSM%" set wanshiwu-mysql DisplayName "wanshiwu MySQL (数智游民创新工场)"
"%NSSM%" set wanshiwu-mysql Description "数智游民创新工场官网数据库 MySQL 8"
"%NSSM%" set wanshiwu-mysql AppStdout "%ROOT%\logs\mysql.log"
"%NSSM%" set wanshiwu-mysql AppStderr "%ROOT%\logs\mysql.err.log"
"%NSSM%" set wanshiwu-mysql AppRotateFiles 1
"%NSSM%" set wanshiwu-mysql AppRotateBytes 10485760
"%NSSM%" set wanshiwu-mysql AppExit Default Restart
"%NSSM%" set wanshiwu-mysql Start SERVICE_AUTO_START

REM ---------- 2. Redis 服务 ----------
echo [3/4] 注册 Redis 服务 ...
"%NSSM%" install wanshiwu-redis "%TOOLS_HOME%\redis\redis-server.exe" --port 6379
"%NSSM%" set wanshiwu-redis AppDirectory "%TOOLS_HOME%\redis"
"%NSSM%" set wanshiwu-redis DisplayName "wanshiwu Redis (数智游民创新工场)"
"%NSSM%" set wanshiwu-redis Description "数智游民创新工场官网缓存 Redis"
"%NSSM%" set wanshiwu-redis AppStdout "%ROOT%\logs\redis.log"
"%NSSM%" set wanshiwu-redis AppStderr "%ROOT%\logs\redis.err.log"
"%NSSM%" set wanshiwu-redis AppRotateFiles 1
"%NSSM%" set wanshiwu-redis AppRotateBytes 10485760
"%NSSM%" set wanshiwu-redis AppExit Default Restart
"%NSSM%" set wanshiwu-redis Start SERVICE_AUTO_START

REM ---------- 3. 后端服务 ----------
echo [4/4] 注册后端服务 ...
if not exist "%ROOT%\ruoyi-admin\target\ruoyi-admin.jar" (
  echo [警告] ruoyi-admin.jar 不存在，请先构建: mvn -pl ruoyi-admin -am package -DskipTests
)
"%NSSM%" install wanshiwu-backend "java" -jar "%ROOT%\ruoyi-admin\target\ruoyi-admin.jar"
"%NSSM%" set wanshiwu-backend AppDirectory "%ROOT%"
"%NSSM%" set wanshiwu-backend DisplayName "wanshiwu Backend (数智游民创新工场官网后端)"
"%NSSM%" set wanshiwu-backend Description "数智游民创新工场官网后端 Spring Boot"
"%NSSM%" set wanshiwu-backend AppStdout "%ROOT%\logs\backend.log"
"%NSSM%" set wanshiwu-backend AppStderr "%ROOT%\logs\backend.err.log"
"%NSSM%" set wanshiwu-backend AppRotateFiles 1
"%NSSM%" set wanshiwu-backend AppRotateBytes 10485760
"%NSSM%" set wanshiwu-backend AppExit Default Restart
"%NSSM%" set wanshiwu-backend Start SERVICE_AUTO_START

REM ---------- 启动服务 ----------
echo 启动服务中...
net start wanshiwu-mysql >nul 2>&1
net start wanshiwu-redis >nul 2>&1
net start wanshiwu-backend >nul 2>&1

echo.
echo ============================================
echo   服务化安装完成！
echo   已注册并启动:
echo     wanshiwu-mysql    (MySQL 3306)
echo     wanshiwu-redis    (Redis 6379)
echo     wanshiwu-backend  (后端 8080, 开机自启)
echo.
echo   常用管理:
echo     net start/stop wanshiwu-mysql^|wanshiwu-redis^|wanshiwu-backend
echo     服务管理器: services.msc 搜索 wanshiwu
echo   前端仍用开发模式: scripts\svc.bat start
echo   卸载服务: scripts\uninstall-services.bat
echo ============================================
pause
exit /b 0
