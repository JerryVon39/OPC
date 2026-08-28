-- ============================================
-- 升级脚本：运营辅助菜单修复 v20260826（2026-08-28 重写：按名称定位）
-- 背景：菜单整理时运营辅助/回收站被删除重建，旧版脚本按 menu_id 硬编码定位
--       （2298/2265/2245/2246/2271），全新库 menu_id 自增漂移 → 产生 parent 不存在的
--       孤儿「回收站」目录（SysMenuServiceImpl.buildMenuTree 把父不存在的节点当顶层
--       渲染 → admin 侧边栏多出空顶层「回收站」，点击 /recycle 白屏，实测复现）。
-- 重写：全部按 menu_name 定位（跨库安全，不依赖 menu_id）；对旧版已产生的孤儿
--       菜单同样幂等归位/清理（可重复执行）。
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p ry-vue < sql/upgrade_20260826_02_menu_fix2.sql
-- ============================================

-- 1. 定位「运营辅助」（按名称；不存在则补建，防极端缺库）
SET @ops_id := (SELECT menu_id FROM sys_menu WHERE menu_name='运营辅助' AND menu_type='M' LIMIT 1);
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '运营辅助', 0, 3, 'ops-aux', '', NULL, '', 1, 0, 'M', '0', '0', '', 'delete', 'admin', NOW(), '误删恢复等辅助功能'
WHERE @ops_id IS NULL;
SET @ops_id := (SELECT menu_id FROM sys_menu WHERE menu_name='运营辅助' AND menu_type='M' LIMIT 1);

-- 2. 运营辅助 path 修正（避让历史 ops 命名，防路由覆盖）
UPDATE sys_menu SET path='ops-aux', update_by='admin', update_time=NOW()
WHERE menu_id=@ops_id AND path <> 'ops-aux';

-- 3. 在运营辅助下重建「回收站」目录（幂等：已有则跳过）
SET @has_rec := (SELECT COUNT(*) FROM sys_menu WHERE parent_id=@ops_id AND path='recycle' AND menu_type='M');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '回收站', @ops_id, 1, 'recycle', '', NULL, '', 1, 0, 'M', '0', '0', '', 'recycle', 'admin', NOW(), '运营辅助-回收站（2026-08-26 菜单修复重建）'
WHERE @has_rec = 0;

-- 4. 孤儿归位：parent 不存在的菜单（旧版按已删除 id 硬编码产生的孤儿）
--    4.1 孤儿「使用帮助」→ 运营辅助（JOIN 判孤儿，避开 MySQL 1093）
UPDATE sys_menu m
LEFT JOIN sys_menu p ON p.menu_id = m.parent_id
SET m.parent_id = @ops_id, m.update_by = 'admin', m.update_time = NOW()
WHERE m.menu_name = '使用帮助' AND m.menu_type = 'C' AND m.parent_id <> 0 AND p.menu_id IS NULL;
--    4.2 其余孤儿（服务回收站/成员回收站等回收站系子菜单）→ 合法回收站目录
--        （孤儿「回收站」目录自身不归位——第 5 步直接删除）
SET @rec_id := (SELECT menu_id FROM sys_menu WHERE parent_id=@ops_id AND path='recycle' AND menu_type='M' LIMIT 1);
UPDATE sys_menu m
LEFT JOIN sys_menu p ON p.menu_id = m.parent_id
SET m.parent_id = @rec_id, m.update_by = 'admin', m.update_time = NOW()
WHERE m.parent_id <> 0 AND p.menu_id IS NULL
  AND m.menu_name <> '使用帮助'
  AND NOT (m.menu_name = '回收站' AND m.menu_type = 'M');

-- 5. 删除重复「回收站」目录：①parent 不存在的孤儿目录（旧版 INSERT 的父 id 漂移空目录）
--    ②已挂在回收站目录下的回收站目录（旧版归位产生，如 2434→2433 场景）；子级已在 4.2 归位
DELETE m FROM sys_menu m
LEFT JOIN sys_menu p ON p.menu_id = m.parent_id
WHERE m.menu_name = '回收站' AND m.menu_type = 'M' AND m.parent_id <> 0
  AND (p.menu_id IS NULL OR p.menu_name = '回收站');

-- 6. 清理孤儿菜单的残留角色授权
DELETE FROM sys_role_menu WHERE menu_id NOT IN (SELECT menu_id FROM sys_menu);

-- 完成提示
SELECT menu_id, menu_name, path, parent_id, menu_type, visible FROM sys_menu
WHERE parent_id=@ops_id OR menu_id=@ops_id
ORDER BY parent_id, order_num;
