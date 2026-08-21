@echo off
chcp 65001 >nul
REM ============================================
REM 数智游民创新工场 · Windows 服务化卸载（与 install-services.bat 配套）
REM 用法: 以管理员身份运行 scripts\uninstall-services.bat
REM ============================================
setlocal
net session >nul 2>&1
if errorlevel 1 (
  echo [错误] 请以管理员身份运行本脚本
  pause & exit /b 1
)
if "%TOOLS_HOME%"=="" set "TOOLS_HOME=%USERPROFILE%\tools"
set "NSSM=%TOOLS_HOME%\nssm\nssm.exe"

for %%s in (wanshiwu-backend wanshiwu-redis wanshiwu-mysql) do (
  sc query %%s >nul 2>&1
  if not errorlevel 1 (
    echo 停止并卸载服务 %%s ...
    net stop %%s >nul 2>&1
    if exist "%NSSM%" (
      "%NSSM%" remove %%s confirm >nul 2>&1
    ) else (
      sc delete %%s >nul 2>&1
    )
  )
)
echo 服务化已全部卸载（前端不受影响）。
pause
exit /b 0
