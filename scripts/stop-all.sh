#!/bin/bash
# ============================================
# 万事屋图书管理系统 · 一键停止（Docker 方式）
# 停止容器但保留数据（下次 start-all.sh 启动数据仍在）
# ============================================
set -e
cd "$(dirname "$0")/.."

if ! command -v docker >/dev/null 2>&1; then
  echo "[stop-all] 未检测到 Docker"
  exit 1
fi

docker compose down
echo "[stop-all] 已停止全部服务（数据卷已保留，重新运行 scripts/start-all.sh 即可恢复）"
