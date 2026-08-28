@echo off
chcp 65001 >nul
REM ============================================
REM 数智游民创新工场 · Windows 服务化安装脚本（基于 nssm）
REM 把 MySQL / Redis / 后端注册为 Windows 服务
REM   - 随系统自启（SERVICE_AUTO_START）
REM   - 崩溃自动拉起（AppExit Default Restart）
REM   - 日志写到 logs\
REM 用法: 以管理员身份运行 scripts\install-services.bat
REM 卸载: scripts\uninstall-services.bat（同样需管理员）
REM 管理: net start/stop wanshiwu-mysql|wanshiwu-redis|wanshiwu-backend
REM R13 fix: ①MySQL 服务启动前先初始化 datadir（mysqld --initialize-insecure），
REM          原脚本直接指向可能为空的 datadir 导致服务起不来
REM          ②后端服务注入 DB/Redis/JWT 环境变量（原脚本不注入，
REM          改 .env 密码后服务仍用 jar 默认值连不上）
REM ============================================
setlocal
cd /d "%~dp0.."

REM ---------- 管理员权限检查 ----------
net session >nul 2>&1
if errorlevel 1 (
  echo [错误] 需要管理员权限：请右键本脚本 - 以管理员身份运行
  pause & exit /b 1
)

if not exist ".env" copy .env.example .env >nul
for /f "eol=# delims=" %%a in (.env) do set "%%a"
if "%DB_PASSWORD%"=="" set "DB_PASSWORD=%MYSQL_ROOT_PASSWORD%"
if "%DB_PASSWORD%"=="" set "DB_PASSWORD=password"
if "%TOOLS_HOME%"=="" set "TOOLS_HOME=%USERPROFILE%\tools"
if not exist logs mkdir logs

REM R-N7 fix: TOKEN_SECRET 缺失/默认时生成（否则后端服务注入空密钥被守卫拒启）
if "%TOKEN_SECRET%"=="" set NEED_SECRET=1
if "%TOKEN_SECRET%"=="25a96a6099a0cc7c37fa1d412ab9712479d32e0b5d9e470e8f6f522271ab2c7c" set NEED_SECRET=1
if "%NEED_SECRET%"=="1" (
  echo [init] TOKEN_SECRET empty or default - generating...
  for /f "delims=" %%k in ('node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"') do set "NEW_SECRET=%%k"
  powershell -NoProfile -Command "$p=(Join-Path (Get-Location) '.env'); $c=[IO.File]::ReadAllText($p); if ($c -match '(?m)^TOKEN_SECRET=.*$') { $c=[regex]::Replace($c,'(?m)^TOKEN_SECRET=.*$','TOKEN_SECRET=%NEW_SECRET%') } else { $c += [Environment]::NewLine + 'TOKEN_SECRET=%NEW_SECRET%' + [Environment]::NewLine }; [IO.File]::WriteAllText($p,$c,(New-Object System.Text.UTF8Encoding($false)))"
  for /f "eol=# delims=" %%a in (.env) do set "%%a"
)

set "NSSM_DIR=%TOOLS_HOME%\nssm"
set "NSSM=%NSSM_DIR%\nssm.exe"
set "ROOT=%CD%"

REM ---------- 准备 nssm（缺失则自动下载解压） ----------
if not exist "%NSSM%" (
  echo [1/4] nssm 未找到，自动下载 nssm 2.24 ...
  powershell -NoProfile -Command "Invoke-WebRequest -Uri 'https://nssm.cc/release/nssm-2.24.zip' -OutFile '%TOOLS_HOME%\nssm.zip' -UseBasicParsing"
  if errorlevel 1 (echo [错误] nssm 下载失败，请手动下载 https://nssm.cc/release/nssm-2.24.zip 解压到 %TOOLS_HOME%\nssm\ & pause & exit /b 1)
  powershell -NoProfile -Command "Expand-Archive -Path '%TOOLS_HOME%\nssm.zip' -DestinationPath '%TOOLS_HOME%' -Force; New-Item -ItemType Directory -Force -Path '%NSSM_DIR%' | Out-Null; Move-Item '%TOOLS_HOME%\nssm-2.24\win64\nssm.exe' '%NSSM%' -Force; Remove-Item '%TOOLS_HOME%\nssm.zip' -Force; Remove-Item '%TOOLS_HOME%\nssm-2.24' -Recurse -Force -ErrorAction SilentlyContinue"
  if errorlevel 1 (echo [错误] nssm 解压失败 & pause & exit /b 1)
)
if not exist "%NSSM%" (echo [错误] nssm.exe 未找到：%NSSM% & pause & exit /b 1)
echo [1/4] nssm 就绪: %NSSM%

REM ---------- 服务冲突检查 ----------
sc query wanshiwu-mysql >nul 2>&1 && (echo [错误] 服务 wanshiwu-mysql 已存在，请先卸载 & pause & exit /b 1)
sc query wanshiwu-redis >nul 2>&1 && (echo [错误] 服务 wanshiwu-redis 已存在，请先卸载 & pause & exit /b 1)
sc query wanshiwu-backend >nul 2>&1 && (echo [错误] 服务 wanshiwu-backend 已存在，请先卸载 & pause & exit /b 1)

REM ---------- 1. MySQL 服务 ----------
echo [2/4] 注册 MySQL 服务 ...
REM R13 fix: datadir 未初始化则先 initialize-insecure（root 空密码，后续用 DB_PASSWORD 建库）
if not exist "%TOOLS_HOME%\mysql-data" (
  echo       初始化 MySQL 数据目录 %TOOLS_HOME%\mysql-data ...
  "%TOOLS_HOME%\mysql-8.4.9-winx64\bin\mysqld.exe" --initialize-insecure --basedir="%TOOLS_HOME%\mysql-8.4.9-winx64" --datadir="%TOOLS_HOME%\mysql-data"
  if errorlevel 1 (echo [错误] MySQL datadir 初始化失败 & pause & exit /b 1)
)
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
"%NSSM%" set wanshiwu-backend DisplayName "wanshiwu Backend (数智游民创新工场)"
"%NSSM%" set wanshiwu-backend Description "数智游民创新工场官网后端 Spring Boot"
"%NSSM%" set wanshiwu-backend AppStdout "%ROOT%\logs\backend.log"
"%NSSM%" set wanshiwu-backend AppStderr "%ROOT%\logs\backend.err.log"
"%NSSM%" set wanshiwu-backend AppRotateFiles 1
"%NSSM%" set wanshiwu-backend AppRotateBytes 10485760
"%NSSM%" set wanshiwu-backend AppExit Default Restart
"%NSSM%" set wanshiwu-backend Start SERVICE_AUTO_START
REM R13 fix: 注入 DB/Redis/JWT 环境变量（与 docker-compose 等价），改 .env 后服务自动生效
"%NSSM%" set wanshiwu-backend AppEnvironmentExtra DB_HOST=localhost DB_PORT=3306 DB_USERNAME=root DB_PASSWORD=%DB_PASSWORD% REDIS_HOST=localhost REDIS_PORT=6379 REDIS_PASSWORD=%REDIS_PASSWORD% TOKEN_SECRET=%TOKEN_SECRET% RUOYI_PROFILE=%ROOT%\uploadPath

REM ---------- 启动 ----------
echo 启动服务 ...
net start wanshiwu-mysql >nul 2>&1
net start wanshiwu-redis >nul 2>&1
net start wanshiwu-backend >nul 2>&1

echo.
echo [完成] 三个服务已注册并启动：
echo   wanshiwu-mysql / wanshiwu-redis / wanshiwu-backend
echo   管理: net start/stop wanshiwu-xxx；卸载: scripts\uninstall-services.bat
echo   注意: 首次启动 MySQL 后需执行 scripts\start-local.bat 初始化数据库（建库+导入）
pause
