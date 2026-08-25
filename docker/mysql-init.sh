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
# 系统角色收敛（幂等）：仅预置 超级管理员/内容编辑，清理 common/cashier/viewer 及测试账号
mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" < /docker-entrypoint-initdb.d/sql/upgrade_20260824_roles.sql
# 若依框架残留清理（业务化改写：用户/部门/公告/岗位/官网菜单，幂等）
mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" < /docker-entrypoint-initdb.d/sql/upgrade_20260820_cleanup.sql
# 官网改造（业务重映射，幂等）：菜单/字典/21 条服务/成员/新闻/轮播
mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" < /docker-entrypoint-initdb.d/sql/upgrade_20260821_official.sql
# 官网内容真实化（幂等）：21 条真实 AI 服务/真实新闻/轮播/产业赛道字典
mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" < /docker-entrypoint-initdb.d/sql/upgrade_20260822_realcontent.sql
# CMS 文章管理（幂等）：cms_category/cms_article 表/栏目/文章/菜单
mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" < /docker-entrypoint-initdb.d/sql/upgrade_20260822_cms.sql
# 系统数据 OPC 化（幂等）：若依官网菜单/部门/岗位
mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" < /docker-entrypoint-initdb.d/sql/upgrade_20260824_opc_cleanup.sql
mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" < /docker-entrypoint-initdb.d/sql/upgrade_20260824_profile.sql
# 回收站三态→两态（幂等）：book/reader 加 del_flag/deleted_by/deleted_time 软删列。
# 后端起 book/reader 查询普遍依赖 del_flag，全新库必须执行本脚本，否则报 Unknown column 'del_flag'
mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" < /docker-entrypoint-initdb.d/sql/upgrade_20260824_two_state.sql
# 读者认证与邮件（幂等）：reader 加 password_hash/pwd_set/email_verified/phone_verified/last_login_time，
# reader_login_log/mail_config/mail_template 建表、uk_email 唯一索引、登录日志/邮件通知后台菜单。
# 依赖 two_state（内部引用 del_flag），必须排在 two_state 之后；缺它全新库读者登录/注册直接 500
mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" < /docker-entrypoint-initdb.d/sql/upgrade_20260824_auth.sql
# 创客大赛报名服务条目（book_id=23，contest.html 报名入口固定引用，幂等）
mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" < /docker-entrypoint-initdb.d/sql/upgrade_20260824_contest.sql
mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" < /docker-entrypoint-initdb.d/sql/upgrade_20260826_policy.sql
# 回收站菜单恢复（幂等）：回收站页已改接两态接口（del_flag 软删），恢复菜单可见性并兜底重建
mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" < /docker-entrypoint-initdb.d/sql/upgrade_20260825_recycle_menu.sql
mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" < /docker-entrypoint-initdb.d/sql/upgrade_20260825_menu_reorg.sql
mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" < /docker-entrypoint-initdb.d/sql/upgrade_20260825_recycle_cleanup.sql
mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" < /docker-entrypoint-initdb.d/sql/upgrade_20260825_recycle_restore.sql
# editor 角色修复（幂等）：补建缺失按钮权限菜单 + 按新目录结构重新授予（依赖 menu_reorg，必须在其后）
mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" < /docker-entrypoint-initdb.d/sql/upgrade_20260825_editor_fix.sql
# 业务数据快照（幂等 REPLACE）：最新一次 commit 时的前台服务/文章/轮播/公告/字典等业务数据，
# 随部署打包进库——本地改后台数据后 commit，部署方拉起即有最新内容（与 .githooks/pre-commit 联动）
mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" < /docker-entrypoint-initdb.d/sql/data_snapshot.sql

echo "数智游民创新工场数据库初始化完成"
