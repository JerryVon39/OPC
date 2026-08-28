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

# 2. 生成 .env（仅首次；邮件通知需填写 MAIL_* 后重启）
if [ ! -f .env ]; then
  cp .env.example .env
  echo "[start-all] 已生成 .env（如需邮件通知请编辑 .env 的 MAIL_* 后重跑）"
fi
# 2b. TOKEN_SECRET 守卫：后端拒绝用仓库默认密钥启动（TokenService.checkSecretNotDefault），
#     缺失/默认时自动生成强随机值写入 .env（B1 修复）
set -a; . ./.env; set +a
if [ -z "$TOKEN_SECRET" ] || [ "$TOKEN_SECRET" = "25a96a6099a0cc7c37fa1d412ab9712479d32e0b5d9e470e8f6f522271ab2c7c" ]; then
  echo "[start-all] TOKEN_SECRET 缺失或为默认值，自动生成强随机密钥..."
  TOKEN_SECRET=$(openssl rand -hex 32 2>/dev/null || head -c 32 /dev/urandom | od -An -tx1 | tr -d ' \n')
  sed -i "s/^TOKEN_SECRET=.*/TOKEN_SECRET=$TOKEN_SECRET/" .env
  set -a; . ./.env; set +a
  echo "[start-all] TOKEN_SECRET 已生成并写入 .env"
fi

# 3. 检查本地镜像；缺失（或代码更新后 tag 提升）则用源码构建
#    镜像已推送 Docker Hub（jerryvon/opc-backend/opc-frontend:v2.4+），
#    本地无对应 tag 时 compose 可直接拉取；强制构建用 docker compose build
if ! docker image inspect jerryvon/opc-backend:v2.4 >/dev/null 2>&1; then
  echo "[start-all] 未找到本地 v2.4 镜像，尝试从 Docker Hub 拉取..."
  docker pull jerryvon/opc-backend:v2.4 || { echo "[start-all] 拉取失败（网络受限？），改用源码构建（首次需几分钟）..."; docker compose build; }
fi

# 4. 启动全部服务（首次自动初始化数据库）
echo "[start-all] 正在启动 MySQL / Redis / 后端 / 前端 ..."
docker compose up -d

# 5. 旧数据卷增量升级（幂等）：已存在的数据卷不会重跑初始化脚本，
#    补跑 upgrade 脚本使库结构与新代码一致（如 del_flag/reader_id 列），
#    否则登录/列表报 Unknown column
bash docker/mysql-upgrade.sh

# 4. 等待后端就绪（首次拉镜像 + 初始化数据库较慢，最多约 3 分钟）
command -v curl >/dev/null 2>&1 || { echo "[start-all] 未检测到 curl，无法探测后端就绪状态"; exit 1; }
echo "[start-all] 等待后端启动中 ..."
READY=0
for ((i=1; i<=90; i++)); do
  # 后端不再映射宿主机 8080（nginx 反代 /prod-api 到 backend），改经 80 端口探测后端是否已响应
  code=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:${FE_PORT:-80}/prod-api/ 2>/dev/null)
  if [ "$code" != "000" ] && [ "$code" != "502" ] && [ "$code" != "503" ] && [ "$code" != "504" ]; then
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
