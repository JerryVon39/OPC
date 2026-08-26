#!/bin/bash
# ============================================
# 数智游民创新工场 · 旧数据卷增量升级脚本
# 场景：docker-entrypoint-initdb.d 只在新数据卷（首次初始化）时执行；
#       已存在的 mysql-data 卷（旧镜像 v2.0 及更早部署过）不会重跑初始化，
#       库结构停留在旧版本（如缺 del_flag / reader_id 列），登录/列表会报
#       Unknown column 错误。本脚本对已有卷补跑全部幂等升级脚本。
# 用法：docker compose up -d 之后执行（由 start-all.sh/bat 自动调用；也可手动）
# 说明：
#   - 全部 upgrade 脚本幂等，可重复执行，对任意旧版本库安全
#   - 命名即顺序：upgrade_YYYYMMDD_NN_描述.sql（文件名序=执行序，I1 单一来源通配扫描）
#   - 不执行 data_snapshot.sql（业务数据快照），避免覆盖后台已修改的业务数据；
#     全新部署的数据导入由 mysql-init.sh 负责
# ============================================
set -e
cd "$(dirname "$0")/.."

CONTAINER="opc-mysql"

if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER}$"; then
  echo "[mysql-upgrade] 容器 ${CONTAINER} 未运行，跳过增量升级"
  exit 0
fi

UPGRADES="$(ls sql/upgrade_*.sql 2>/dev/null | sort)"

for f in $UPGRADES; do
  if [ -f "$f" ]; then
    echo "[mysql-upgrade] 执行 ${f} ..."
    docker exec -i "$CONTAINER" sh -c 'mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" ry-vue' < "$f" >/dev/null 2>&1 \
      || echo "[mysql-upgrade] 警告：${f} 执行出错（多为已存在/幂等跳过，可忽略）"
  fi
done

echo "[mysql-upgrade] 增量升级完成"
