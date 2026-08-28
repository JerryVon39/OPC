-- ============================================
-- 升级脚本：清理孤儿菜单（parent_id 为 NULL）v20260824
-- 数智游民创新工场 · 菜单树完整性修复
-- 背景：upgrade_20260818_purchase.sql 等旧脚本在存量库执行时，若父菜单
--       子查询未匹配到（旧库父菜单名不同/缺失），会插入 parent_id=NULL 的
--       菜单行；后端 getRouters 构建菜单树时对 NULL parentId 调 longValue()
--       抛 NPE → 后台登录后无法进入。
--       同时该脚本存在幂等破缺（按 menu_name 判重未覆盖父级缺失场景），
--       可能与后续脚本插入的完整菜单重复。
-- 处理：删除全部 parent_id IS NULL 的孤儿菜单及其子级（含角色关联）。
--       根菜单 parent_id=0 不受影响；正常菜单的父级必然存在。
-- 幂等：可重复执行；对任意版本库安全。
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p ry-vue < sql/upgrade_20260824_menu_cleanup.sql
-- ============================================


-- 1. 删孤儿菜单的角色关联
DELETE rm FROM sys_role_menu rm
JOIN sys_menu m ON m.menu_id = rm.menu_id
WHERE m.parent_id IS NULL;

-- 2. 删孤儿菜单的直接子级（父级即将删除）
DELETE FROM sys_menu
WHERE parent_id IN (SELECT menu_id FROM (SELECT menu_id FROM sys_menu WHERE parent_id IS NULL) t);

-- 3. 删孤儿菜单本身
DELETE FROM sys_menu WHERE parent_id IS NULL;
