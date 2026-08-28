-- ============================================
-- 升级脚本：回收站入口恢复（名称业务化）v20260825
-- 数智游民创新工场 · 运营辅助模块
-- 背景：recycle_cleanup 曾按「删除入口」理解移除回收站菜单；用户确认应保留功能、
--       仅修正残留名称：「图书回收站」→「服务回收站」、「读者回收站」→「成员回收站」。
-- 结构（与全站术语一致，扁平挂在「运营辅助」下）：
--   运营辅助 → 服务回收站（system/recycle/book，system:book:remove）
--            → 成员回收站（system/recycle/reader，system:reader:remove）
-- 幂等：INSERT 带 WHERE NOT EXISTS；若存在旧名菜单（图书回收站/读者回收站）先改名对齐。
-- 执行顺序：可重复执行；在 recycle_cleanup 之后（cleanup 先删旧名，本脚本重建新名）。
-- ============================================


-- 0. 旧名对齐（存量库若还有旧名菜单，直接改名，避免重复创建）
UPDATE sys_menu SET menu_name='服务回收站' WHERE menu_name='图书回收站';
UPDATE sys_menu SET menu_name='成员回收站' WHERE menu_name='读者回收站';

-- 1. 恢复「运营辅助」顶层分组（幂等）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '运营辅助', 0, 3, 'ops', '', 1, 0, 'M', '0', '0', '', 'delete', 'admin', NOW(), '误删恢复等辅助功能'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='运营辅助' AND menu_type='M');

-- 2. 恢复「服务回收站」（原图书回收站，两态软删恢复：del_flag='2'）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '服务回收站', p.menu_id, 1, 'book', 'system/recycle/book', 1, 0, 'C', '0', '0', 'system:book:remove', 'book', 'admin', NOW(), '误删服务恢复（两态）'
FROM sys_menu p
WHERE p.menu_name='运营辅助' AND p.menu_type='M'
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='服务回收站');

-- 3. 恢复「成员回收站」（原读者回收站）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '成员回收站', p.menu_id, 2, 'reader', 'system/recycle/reader', 1, 0, 'C', '0', '0', 'system:reader:remove', 'peoples', 'admin', NOW(), '误删成员恢复（两态）'
FROM sys_menu p
WHERE p.menu_name='运营辅助' AND p.menu_type='M'
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='成员回收站');
