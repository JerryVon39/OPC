@echo off
REM ============================================
REM 数智游民创新工场 · Docker 镜像发布脚本（一键构建 + 推送）
REM
REM 用法: scripts\publish-docker.bat [版本号]     默认版本号 v2.1
REM 前置: 1) 已 docker login（需 Read&Write 权限的 token，只读 token 推送会被拒）
REM       2) 可访问 Docker Hub（国内网络需在 Docker Desktop 配置代理）
REM 效果: 重新构建 backend/frontend → 打 tag → 推送 Docker Hub → 同步 docker-compose.yml 版本号
REM ============================================
setlocal enabledelayedexpansion

set "VERSION=%1"
if "%VERSION%"=="" set "VERSION=v1.0"

echo ============================================
echo  [1/5] 重新构建镜像（当前版本 %VERSION%）
echo ============================================
docker build -t jerryvon/opc-backend:%VERSION% .
if errorlevel 1 goto :fail
docker build -t jerryvon/opc-frontend:%VERSION% -f ruoyi-ui/Dockerfile .
if errorlevel 1 goto :fail

echo ============================================
echo  [2/5] 推送后端镜像 jerryvon/opc-backend:%VERSION%
echo ============================================
docker push jerryvon/opc-backend:%VERSION%
if errorlevel 1 goto :fail

echo ============================================
echo  [3/5] 推送前端镜像 jerryvon/opc-frontend:%VERSION%
echo ============================================
docker push jerryvon/opc-frontend:%VERSION%
if errorlevel 1 goto :fail

echo ============================================
echo  [4/5] 同步 docker-compose.yml 版本号
echo ============================================
node -e "const fs=require('fs');const f='docker-compose.yml';const s=fs.readFileSync(f,'utf8').replace(/opc-backend:[a-zA-Z0-9._-]+/g,'opc-backend:%VERSION%').replace(/opc-frontend:[a-zA-Z0-9._-]+/g,'opc-frontend:%VERSION%');fs.writeFileSync(f,s);console.log('docker-compose.yml 已更新为 %VERSION%');"
if errorlevel 1 goto :fail

echo ============================================
echo  [5/5] 验证远程镜像存在
echo ============================================
powershell -Command "try { $r=Invoke-RestMethod -Uri 'https://hub.docker.com/v2/repositories/jerryvon/opc-backend/tags' -Headers @{'User-Agent'='Mozilla/5.0'} -ErrorAction Stop; $t=$r.results | Where-Object { $_.name -eq '%VERSION%' }; if($t){ Write-Host '[OK] 后端镜像已在 Docker Hub: jerryvon/opc-backend:%VERSION%' } else { Write-Host '[WARN] 后端镜像未查到（稍后刷新页面确认）' } } catch { Write-Host '[WARN] 验证接口不可达（镜像可能已推送成功，页面可查）' }"

echo.
echo [OK] 发布完成 %VERSION%
echo    仓库页: https://hub.docker.com/u/jerryvon
echo    注意: docker-compose.yml 已同步版本号，记得 git commit 存档
exit /b 0

:fail
echo.
echo [FAIL] 发布失败（见上方错误信息）
exit /b 1
