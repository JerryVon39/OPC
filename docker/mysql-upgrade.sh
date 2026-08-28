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
    # R8 fix: 原 >/dev/null 吞输出 + 任意错误都继续——全部 upgrade 均幂等且带守卫，
    # 正常执行不会报错，失败=结构性问题，应停止并暴露错误（与 start-local.sh 的 || exit 1 语义一致）
    if ! docker exec -i "$CONTAINER" sh -c 'mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" ry-vue' < "$f"; then
      echo "[mysql-upgrade] 错误：${f} 执行失败（upgrade 脚本均幂等，失败=结构性问题，请检查上方错误信息）"
      exit 1
    fi
  fi
done

echo "[mysql-upgrade] 增量升级完成"
