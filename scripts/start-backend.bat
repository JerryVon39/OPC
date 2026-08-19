@echo off
REM ============================================
REM 一键启动后端（含邮件授权码注入）
REM 用法：
REM   1) 首次使用：在本文件同目录建 .env 文件，写入
REM        MAIL_AUTH_CODE=你的QQ邮箱授权码
REM        （可选）MAIL_ENABLED=false 关闭邮件通知
REM   2) 双击本脚本即可启动
REM 邮件发不出 99% 是漏配授权码：SMTP 认证失败会被吞掉，
REM 但业务（借书/预约等）照常成功，日志会打 WARN。
REM ============================================
cd /d "%~dp0.."

REM 从 .env 读取环境变量（若存在；# 开头为注释行）
if exist ".env" for /f "usebackq eol=# delims=" %%a in (".env") do set "%%a"

if "%MAIL_AUTH_CODE%"=="" (
  echo [start-backend] 警告: 未检测到 MAIL_AUTH_CODE, 邮件通知将发送失败! 请编辑 .env
) else (
  echo [start-backend] 已注入 MAIL_AUTH_CODE, 邮件通知可用
)
if exist "ruoyi-admin\target\ruoyi-admin.jar" (
  java -jar ruoyi-admin\target\ruoyi-admin.jar
) else (
  echo [start-backend] 未找到 jar, 请先运行: mvn -B package -pl ruoyi-admin -am -DskipTests
  pause
)
