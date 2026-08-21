#!/bin/bash
# ============================================
# 数智游民创新工场官网 · 一键启动（Docker 方式）
# 适合从 GitHub 克隆仓库的新用户：无需安装 MySQL/Redis/Java/Node，
# 只需 Docker（https://docs.docker.com/get-docker/）
# 用法：./scripts/start-all.sh       停止：./scripts/stop-all.sh
# ============================================
set -e
cd "$(dirname "$0")/.."

# 1. 检查 Docker
if ! command -v docker >/dev/null 2>&1; then
  echo "[start-all] 未检测到 Docker，请先安装：https://docs.docker.com/get-docker/"
  exit 1
fi
if ! docker info >/dev/null 2>&1; then
  echo "[start-all] Docker 服务未运行，请先启动 Docker（Windows 用户请打开 Docker Desktop）"
  exit 1
fi

# 2. 生成 .env（仅首次；默认值可直接启动，邮件通知需填写 MAIL_* 后重启）
if [ ! -f .env ]; then
  cp .env.example .env
  echo "[start-all] 已生成 .env（默认配置可直接启动；如需邮件通知请编辑 .env 的 MAIL_* 后重跑）"
fi

# 3. 启动全部服务（首次自动拉取镜像并初始化数据库）
echo "[start-all] 正在启动 MySQL / Redis / 后端 / 前端 ..."
docker compose up -d

# 4. 等待后端就绪（首次拉镜像 + 初始化数据库较慢，最多约 3 分钟）
echo "[start-all] 等待后端启动中 ..."
READY=0
for i in $(seq 1 90); do
  if curl -s -o /dev/null http://localhost:8080/ 2>/dev/null; then
    READY=1
    break
  fi
  sleep 2
done
if [ "$READY" != "1" ]; then
  echo "[start-all] 后端未在预期时间内就绪，请查看日志：docker compose logs -f backend"
  exit 1
fi
echo "[start-all] 后端已就绪 ✅"

echo ""
echo "============================================"
echo "  启动完成！访问地址："
echo "  管理后台：http://localhost/           （默认账号 admin / admin123，首次登录请修改密码）"
echo "  读者前台：http://localhost/home.html"
echo "  停止服务：./scripts/stop-all.sh"
echo "============================================"
