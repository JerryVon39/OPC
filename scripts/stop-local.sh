#!/usr/bin/env bash
# ============================================
# Book System - One-click stop (local dev)
# Stops backend/frontend started by start-local.sh.
# MySQL/Redis (native or containers) are left running -
# use scripts/stop-all.sh to stop the Docker stack.
# ============================================
cd "$(dirname "$0")/.."

# backend: java -jar ruoyi-admin
pkill -f "ruoyi-admin/target/ruoyi-admin.jar" 2>/dev/null && echo "backend stopped" || echo "backend not running"
# frontend: vue-cli-service dev server
pkill -f "vue-cli-service" 2>/dev/null && echo "frontend stopped" || echo "frontend not running"

echo
echo "Done. Database data kept - run scripts/start-local.sh to resume."
