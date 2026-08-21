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
# 若依框架残留清理（业务化改写：用户/部门/公告/岗位/官网菜单，幂等）
mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" < /docker-entrypoint-initdb.d/sql/upgrade_20260820_cleanup.sql
# 官网改造（业务重映射，幂等）：菜单/字典/21 条服务/成员/新闻/轮播
mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" < /docker-entrypoint-initdb.d/sql/upgrade_20260821_official.sql
# 官网内容真实化（幂等）：21 条真实 AI 服务/真实新闻/轮播/产业赛道字典
mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" < /docker-entrypoint-initdb.d/sql/upgrade_20260822_realcontent.sql
# CMS 文章管理（幂等）：cms_category/cms_article 表/栏目/文章/菜单
mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" < /docker-entrypoint-initdb.d/sql/upgrade_20260822_cms.sql

echo "数智游民创新工场数据库初始化完成"
