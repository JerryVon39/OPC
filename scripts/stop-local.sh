#!/usr/bin/env bash
# ============================================
# Digital Nomad Innovation Works - One-click stop (local dev)
# Stops backend/frontend started by start-local.sh.
# MySQL/Redis (native or containers) are left running -
# use scripts/stop-all.sh to stop the Docker stack.
# ============================================
cd "$(dirname "$0")/.."

# backend: java -jar ruoyi-admin（B5 fix: 实际 cmdline 是 target/ruoyi-admin.jar，
# 旧模式 "ruoyi-admin/target/..." 匹配不到，后端停不掉、8080 残留）
pkill -f "ruoyi-admin.jar" 2>/dev/null && echo "backend stopped" || echo "backend not running"
# frontend: vue-cli-service dev server
pkill -f "vue-cli-service" 2>/dev/null && echo "frontend stopped" || echo "frontend not running"

echo
echo "Done. Database data kept - run scripts/start-local.sh to resume."
