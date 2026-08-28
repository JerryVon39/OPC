@echo off
chcp 65001 >nul
REM ============================================
REM 数智游民创新工场 · 环境自检脚本（U-6）
REM 检测本机环境是否满足运行要求，逐项打勾并给出缺失指引
REM 用法：scripts\check-env.bat（Windows）/ scripts\check-env.sh（Linux/macOS）
REM ============================================
setlocal
set BAD=0

echo ============================================
echo  数智游民创新工场 · 环境自检
echo ============================================

REM ---------- Docker（一键部署首选） ----------
where docker >nul 2>&1
if not errorlevel 1 (
  docker info >nul 2>&1
  if not errorlevel 1 (echo  [OK] Docker 运行中) else (echo  [!] Docker 已安装但未运行，请打开 Docker Desktop)
) else (
  echo  [--] 未安装 Docker（可选：安装后用 scripts\start-all.bat 一键启动）
)

REM ---------- 本机开发环境 ----------
where java >nul 2>&1
if not errorlevel 1 (
  for /f "delims=" %%v in ('java -version 2^>^&1 ^| findstr /r "version"') do echo  [OK] Java: %%v
) else (echo  [X] JDK 17 未安装：https://adoptium.net/ & set /a BAD+=1)

where mvn >nul 2>&1
if not errorlevel 1 (echo  [OK] Maven 已安装) else (echo  [X] Maven 未安装（首次构建需要）：https://maven.apache.org/download.cgi & set /a BAD+=1)

where node >nul 2>&1
if not errorlevel 1 (
  for /f "delims=" %%v in ('node -v 2^>^&1') do echo  [OK] Node: %%v ^(前端需 16/18，17+ 会报 md4 错误^)
) else (echo  [X] Node.js 未安装：https://nodejs.org/ & set /a BAD+=1)

where npm >nul 2>&1
if not errorlevel 1 (echo  [OK] npm 已安装) else (echo  [X] npm 未安装（随 Node 一起装）& set /a BAD+=1)

where mysql >nul 2>&1
if not errorlevel 1 (echo  [OK] MySQL 客户端已安装（版本需 8.x）) else (echo  [X] MySQL 8 未安装（或用 Docker 一键路径）& set /a BAD+=1)

where redis-server >nul 2>&1
if not errorlevel 1 (echo  [OK] Redis 已安装) else (echo  [--] Redis 未安装（或用 Docker 一键路径）)

echo ============================================
if %BAD%==0 (echo  环境检查通过！可运行 scripts\start-local.bat 或 scripts\start-all.bat) else (echo  有 %BAD% 项缺失，见上方 [X] 提示；也可直接用 Docker：scripts\start-all.bat)
echo ============================================
pause
