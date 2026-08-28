-- ============================================
-- 升级脚本：回收站菜单恢复（两态）v20260825
-- 数智游民创新工场 · 误删数据恢复入口
-- 背景：upgrade_20260821_official.sql 曾将「回收站/图书回收站/读者回收站」三菜单
--       停用（status='1'），且旧页面依赖从未写入数据的 book_recycle/reader_recycle
--       快照表（回收站功能整体空转）。2026-08-25 前端回收站页已改接两态接口
--       （/system/book|reader/deletedList|restore|purge，软删 del_flag='2' 即入回收站），
--       本脚本恢复菜单可见性并把子菜单权限点对齐两态接口所需权限。
-- 幂等：UPDATE 天然幂等；INSERT ... WHERE NOT EXISTS 兜底「菜单已被 menu_cleanup 清理」场景。
-- 执行顺序：必须在 upgrade_20260824_menu_cleanup.sql 之后（防孤儿清理先于重建）。
-- ============================================


-- 1. 恢复停用的三菜单（若存在）
UPDATE sys_menu SET status='0' WHERE menu_name='回收站' AND menu_type='M';
UPDATE sys_menu SET status='0', perms='system:book:remove' WHERE menu_name='图书回收站';
UPDATE sys_menu SET status='0', perms='system:reader:remove' WHERE menu_name='读者回收站';

-- 2. 兜底重建（菜单已被清理时）：父菜单「官网运营」（原「图书业务」改名，menu_id 不变）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '回收站', m.menu_id, 10, 'recycle', '', 1, 0, 'M', '0', '0', '', 'delete', 'admin', NOW(), '误删数据恢复（两态）'
FROM sys_menu m
WHERE m.menu_name='官网运营' AND m.menu_type='M'
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='回收站');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '图书回收站', p.menu_id, 1, 'book', 'system/recycle/book', 1, 0, 'C', '0', '0', 'system:book:remove', 'book', 'admin', NOW(), '误删服务恢复（两态）'
FROM sys_menu p
WHERE p.menu_name='回收站' AND p.menu_type='M'
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='图书回收站');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '读者回收站', p.menu_id, 2, 'reader', 'system/recycle/reader', 1, 0, 'C', '0', '0', 'system:reader:remove', 'peoples', 'admin', NOW(), '误删成员恢复（两态）'
FROM sys_menu p
WHERE p.menu_name='回收站' AND p.menu_type='M'
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='读者回收站');
