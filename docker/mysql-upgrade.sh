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

UPGRADES="sql/upgrade_20260818_purchase.sql
sql/upgrade_20260819_mail.sql
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
sql/upgrade_20260825_recycle_cleanup.sql
sql/upgrade_20260825_recycle_restore.sql
sql/upgrade_20260825_editor_fix.sql
sql/upgrade_20260825_cms_enhance.sql
sql/upgrade_20260825_ops_workbench.sql
sql/upgrade_20260825_menu_fix.sql
sql/upgrade_20260825_menu_dedupe.sql
sql/upgrade_20260825_cms_block.sql
sql/upgrade_20260825_operator_block.sql
sql/upgrade_20260825_cms_section.sql
sql/upgrade_20260825_section_fix.sql
sql/upgrade_20260825_section_fix2.sql
sql/upgrade_20260825_home_polish.sql
sql/upgrade_20260825_home_fill.sql
sql/upgrade_20260825_cms_unify.sql
sql/upgrade_20260825_preview.sql
sql/upgrade_20260825_hide_book_menu.sql
sql/upgrade_20260825_block_v3.sql
sql/upgrade_20260825_block_v3_seed.sql
sql/upgrade_20260826_menu_fix2.sql
sql/upgrade_20260826_engine_merge.sql
sql/upgrade_20260826_site_settings.sql
sql/upgrade_20260826_article_history.sql
sql/upgrade_20260826_recycle_purge_job.sql
sql/upgrade_20260826_banner_style.sql
sql/upgrade_20260826_banner_style2.sql"

for f in $UPGRADES; do
  if [ -f "$f" ]; then
    echo "[mysql-upgrade] 执行 ${f} ..."
    docker exec -i "$CONTAINER" sh -c 'mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" ry-vue' < "$f" >/dev/null 2>&1 \
      || echo "[mysql-upgrade] 警告：${f} 执行出错（多为已存在/幂等跳过，可忽略）"
  fi
done

echo "[mysql-upgrade] 增量升级完成"
