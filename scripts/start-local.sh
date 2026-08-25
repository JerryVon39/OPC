#!/usr/bin/env bash
# ============================================
# Digital Nomad Innovation Works - One-click start (local dev, no Docker required)
# Flow (same as the proven local start-all.bat):
#   Redis -> MySQL -> backend -> frontend -> print URLs
#
# Portable: works from any git clone. Requirements:
#   - JDK 17+ / Node.js (npm) on PATH; Maven only needed for first build
#   - MySQL + Redis: either already running (system services), or Docker
#     (falls back to `docker compose up -d mysql redis` automatically)
#
# Frontend port: 8081 (override FE_PORT in .env)
# Stop: scripts/stop-local.sh
# ============================================
set -euo pipefail
cd "$(dirname "$0")/.."

# ---------- 0. Load .env (create from example on first run) ----------
[ -f .env ] || cp .env.example .env
set -a; . ./.env; set +a
DB_PASSWORD=${DB_PASSWORD:-password}
FE_PORT=${FE_PORT:-8081}
# 上传目录：Linux 默认家目录下（application.yml 的 Windows 盘符默认值在 Linux 会落到 <cwd>/D:/ruoyi/uploadPath）
RUOYI_PROFILE=${RUOYI_PROFILE:-$HOME/ruoyi/uploadPath}
mkdir -p "$RUOYI_PROFILE"
mkdir -p logs

# ---------- 1. Prerequisite check ----------
command -v java >/dev/null 2>&1 || { echo "[1/5] JDK 17+ not found in PATH. Install: https://adoptium.net/"; exit 1; }
command -v node >/dev/null 2>&1 || { echo "[1/5] Node.js not found in PATH. Install: https://nodejs.org/"; exit 1; }
command -v npm >/dev/null 2>&1 || { echo "[1/5] npm not found in PATH"; exit 1; }

mysql_ready() { mysql -uroot -p"$DB_PASSWORD" -e "SELECT 1" >/dev/null 2>&1; }

# ---------- 2. Redis (port 6379): running? -> native -> docker -> fail ----------
if redis-cli ping 2>/dev/null | grep -q PONG; then
  echo "[2/5] Redis already running"
elif command -v redis-server >/dev/null 2>&1; then
  echo "[2/5] Starting native Redis ..."
  redis-server --daemonize yes
  sleep 1
elif command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1; then
  echo "[2/5] No native Redis, starting container ..."
  docker compose up -d redis >/dev/null
  sleep 2
else
  echo "[2/5] Redis unavailable: install redis-server or Docker"; exit 1
fi
redis-cli ping 2>/dev/null | grep -q PONG || { echo "[2/5] Redis not ready in time"; exit 1; }
echo "[2/5] Redis ready"

# ---------- 3. MySQL (port 3306): reachable? -> native -> docker -> fail ----------
if mysql_ready; then
  echo "[3/5] MySQL already running"
elif command -v mysqld >/dev/null 2>&1; then
  echo "[3/5] Starting native MySQL ..."
  mysqld --daemonize 2>/dev/null || true
  echo "      if mysqld did not start, start it manually (systemctl start mysql)"
  for _ in $(seq 1 20); do mysql_ready && break; sleep 2; done
elif command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1; then
  echo "[3/5] No native MySQL, starting container (first start initializes DB) ..."
  docker compose up -d mysql >/dev/null
  for _ in $(seq 1 30); do mysql_ready && break; sleep 2; done
else
  echo "[3/5] MySQL unavailable: install MySQL or Docker"; exit 1
fi
mysql_ready || { echo "[3/5] MySQL not ready - check DB_PASSWORD in .env"; exit 1; }
echo "[3/5] MySQL ready"

# ---------- 4. Database init (fresh: full import / existing: idempotent upgrades) ----------
# 幂等升级脚本全清单（与 docker/mysql-upgrade.sh 一致，另含 auth）：
#   - purchase/recycle 旧脚本含旧名父菜单 INSERT，在业务化库上重跑会产生孤儿菜单，
#     由清单末尾的 menu_cleanup 统一清理（getRouters NPE 防御）
#   - auth 依赖 two_state 的 del_flag，必须排在 two_state 之后
UPGRADES="sql/upgrade_20260818_purchase.sql
sql/upgrade_20260819_mail.sql
sql/upgrade_20260819_menu.sql
sql/upgrade_20260820_cleanup.sql
sql/upgrade_20260821_official.sql
sql/upgrade_20260822_realcontent.sql
sql/upgrade_20260822_cms.sql
sql/upgrade_20260823_cms.sql
sql/upgrade_20260824_opc_cleanup.sql
sql/upgrade_20260824_profile.sql
sql/upgrade_20260824_two_state.sql
sql/upgrade_20260824_auth.sql
sql/upgrade_20260824_contest.sql
sql/upgrade_20260824_roles.sql
sql/upgrade_20260826_policy.sql
sql/upgrade_20260824_menu_cleanup.sql
sql/upgrade_20260825_recycle_menu.sql
sql/upgrade_20260825_menu_reorg.sql
sql/upgrade_20260825_recycle_cleanup.sql"

DB_EXISTS=$(mysql -uroot -p"$DB_PASSWORD" -N -e "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='ry-vue'" 2>/dev/null || echo 0)
TABLE_COUNT=0
if [ "$DB_EXISTS" != "0" ]; then
  TABLE_COUNT=$(mysql -uroot -p"$DB_PASSWORD" -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='ry-vue' AND table_name IN ('sys_menu','book','shop_order')" 2>/dev/null || echo 0)
fi
if [ "$DB_EXISTS" != "0" ] && [ "$TABLE_COUNT" = "3" ]; then
  echo "[4/5] Existing DB detected. Running idempotent upgrades..."
  for f in $UPGRADES; do
    if [ -f "$f" ]; then
      echo "      executing $f"
      mysql -uroot -p"$DB_PASSWORD" ry-vue < "$f" || { echo "[4/5] upgrade $f failed"; exit 1; }
    fi
  done
else
  echo "[4/5] Fresh DB detected. Creating database and importing init scripts..."
  mysql -uroot -p"$DB_PASSWORD" -e "CREATE DATABASE IF NOT EXISTS \`ry-vue\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci" || exit 1
  for f in sql/ry_20260417.sql sql/quartz.sql sql/business_init.sql sql/role_init.sql; do
    echo "      importing $f"
    mysql -uroot -p"$DB_PASSWORD" ry-vue < "$f" || { echo "[4/5] import $f failed"; exit 1; }
  done
  # 全新库与 Docker 首次初始化对齐：补跑全部幂等升级（del_flag/password_hash/CMS/菜单等）+ 业务数据快照
  echo "      applying idempotent upgrades..."
  for f in $UPGRADES; do
    if [ -f "$f" ]; then
      echo "      applying $f"
      mysql -uroot -p"$DB_PASSWORD" ry-vue < "$f" || { echo "[4/5] upgrade $f failed"; exit 1; }
    fi
  done
  if [ -f sql/data_snapshot.sql ]; then
    echo "      applying sql/data_snapshot.sql"
    mysql -uroot -p"$DB_PASSWORD" ry-vue < sql/data_snapshot.sql || { echo "[4/5] data_snapshot failed"; exit 1; }
  fi
fi

# ---------- 5. Backend (port 8080) ----------
if [ -f ruoyi-admin/target/ruoyi-admin.jar ]; then
  echo "[5/5] Backend jar exists, skipping build"
else
  command -v mvn >/dev/null 2>&1 || { echo "[5/5] Maven not found in PATH - needed for first build"; exit 1; }
  echo "[5/5] Building backend (first run, 1-2 min)..."
  mvn -q -pl ruoyi-admin -am package -DskipTests || { echo "[5/5] Build failed"; exit 1; }
fi
echo "[5/5] Starting backend ..."
(cd ruoyi-admin && nohup java -jar target/ruoyi-admin.jar > ../logs/backend.log 2>&1 &)
for _ in $(seq 1 20); do curl -s -o /dev/null http://localhost:8080/ && break; sleep 2; done
curl -s -o /dev/null http://localhost:8080/ || { echo "[5/5] Backend not ready in 40s - check logs/backend.log"; exit 1; }
echo "[5/5] Backend ready"

# ---------- 6. Frontend (port $FE_PORT) ----------
if [ ! -d ruoyi-ui/node_modules ]; then
  echo "[6/5] Installing frontend dependencies (first run, 1-2 min)..."
  (cd ruoyi-ui && npm install) || { echo "[6/5] npm install failed"; exit 1; }
fi
echo "[6/5] Starting frontend (port $FE_PORT) ..."
(cd ruoyi-ui && nohup npm run dev -- --no-open --port="$FE_PORT" > ../logs/frontend.log 2>&1 &)
for _ in $(seq 1 30); do curl -s --max-time 3 "http://localhost:$FE_PORT/dev-api/captchaImage" | grep -q code && break; sleep 2; done
curl -s --max-time 3 "http://localhost:$FE_PORT/dev-api/captchaImage" | grep -q code || { echo "[6/5] Frontend not ready in 60s - check logs/frontend.log"; exit 1; }
echo "[6/5] Frontend ready"

echo
echo "============================================"
echo "  All services started!"
echo "  Admin:  http://localhost:$FE_PORT/              admin / admin123"
echo "  Reader: http://localhost:$FE_PORT/home.html"
echo "  Stop:   scripts/stop-local.sh"
echo "============================================"
