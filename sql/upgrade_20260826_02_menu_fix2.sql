-- ============================================
-- 升级脚本：运营辅助菜单修复 v20260826
-- 背景：菜单整理时 2262 运营辅助 / 2277 回收站被删除重建为 2298（空目录，无子菜单），
--       导致：① 运营辅助点击空白（无子菜单→无动态路由→跳 /ops 404）
--       ② 使用帮助(2265)/回收站三个子菜单(2245/2246/2271)成为孤儿（parent 指向已删 id）
-- 处理：
--   1. 2298 运营辅助 path 改 ops-aux（避让历史 ops 命名，防路由覆盖）
--   2. 在运营辅助下重建「回收站」目录
--   3. 孤儿菜单归位：回收站三个子菜单 → 回收站目录；使用帮助 → 运营辅助
-- 幂等：会话变量守卫（MySQL 1093：UPDATE 目标表不得出现在 FROM 子查询）
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p ry-vue < sql/upgrade_20260826_menu_fix2.sql
-- ============================================

USE ry-vue;

-- 1. 运营辅助 path 修正（避让 2087 合作经营的历史 ops 命名）
UPDATE sys_menu SET path = 'ops-aux', update_by = 'admin', update_time = NOW() WHERE menu_id = 2298;

-- 2. 重建回收站目录（幂等：已有则跳过）
SET @has_rec = (SELECT COUNT(*) FROM sys_menu WHERE parent_id = 2298 AND path = 'recycle' AND menu_type = 'M');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '回收站', 2298, 1, 'recycle', '', NULL, '', 1, 0, 'M', '0', '0', '', 'recycle', 'admin', NOW(), '运营辅助-回收站（2026-08-26 菜单修复重建）'
WHERE @has_rec = 0;

-- 3. 孤儿菜单归位
SET @rec_id = (SELECT menu_id FROM sys_menu WHERE parent_id = 2298 AND path = 'recycle' AND menu_type = 'M' LIMIT 1);
UPDATE sys_menu SET parent_id = @rec_id, update_by = 'admin', update_time = NOW() WHERE menu_id IN (2245, 2246, 2271);
UPDATE sys_menu SET parent_id = 2298, update_by = 'admin', update_time = NOW() WHERE menu_id = 2265;

-- 完成提示
SELECT menu_id, menu_name, path, parent_id, menu_type, visible FROM sys_menu
WHERE menu_id IN (2298, 2265, 2245, 2246, 2271) OR (parent_id = 2298 AND menu_type = 'M')
ORDER BY parent_id, order_num;
