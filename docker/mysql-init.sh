#!/bin/bash
# ============================================
# 数智游民创新工场 · MySQL 首次初始化脚本
# 按依赖顺序导入 SQL（docker-entrypoint 自动执行 .sh）
# 顺序：若依系统表 → 定时任务表 → 业务表 → 角色权限
# 注意：business_init.sql 幂等，可重复执行
# ============================================
set -e

mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" < /docker-entrypoint-initdb.d/sql/ry_20260417.sql
mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" < /docker-entrypoint-initdb.d/sql/quartz.sql
mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" < /docker-entrypoint-initdb.d/sql/business_init.sql
mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" < /docker-entrypoint-initdb.d/sql/role_init.sql
# ---- 幂等升级链（I1 单一来源：通配扫描，文件名序=执行序：upgrade_YYYYMMDD_NN_描述.sql）----
# 全部 upgrade 脚本幂等可重复执行；同日内依赖顺序由 NN 序号保证
# （two_state 先于 auth、menu_reorg 先于 editor_fix、menu_cleanup 垫后清理遗留孤儿菜单）
for f in /docker-entrypoint-initdb.d/sql/upgrade_*.sql; do
  mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" < "$f"
done
# 业务数据快照（幂等 REPLACE）：最新一次 commit 时的前台服务/文章/轮播/公告/字典等业务数据，
# 随部署打包进库——本地改后台数据后 commit，部署方拉起即有最新内容（与 .githooks/pre-commit 联动）
mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" < /docker-entrypoint-initdb.d/sql/data_snapshot.sql

echo "数智游民创新工场数据库初始化完成"
