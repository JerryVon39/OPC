#!/bin/bash
# ============================================
# 万事屋 · MySQL 首次初始化脚本
# 按依赖顺序导入 SQL（docker-entrypoint 自动执行 .sh）
# 顺序：若依系统表 → 定时任务表 → 业务表 → 角色权限
# 注意：business_init.sql 幂等，可重复执行
# ============================================
set -e

mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" < /docker-entrypoint-initdb.d/sql/ry_20260417.sql
mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" < /docker-entrypoint-initdb.d/sql/quartz.sql
mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" < /docker-entrypoint-initdb.d/sql/business_init.sql
mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" < /docker-entrypoint-initdb.d/sql/role_init.sql

echo "万事屋数据库初始化完成"
