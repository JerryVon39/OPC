-- ============================================
-- 升级脚本：图书业务菜单重组（3 个二级目录）
-- 适用：已存在的数据库（全新库由 business_init.sql + role_init.sql 一次完成）
-- 幂等：可重复执行，务必带 --default-character-set=utf8mb4
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p ry-vue < sql/upgrade_20260819_menu.sql
-- ============================================


-- ---------- 1. 插入 3 个二级目录（图书管理/读者服务/经营管理） ----------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '图书管理',(SELECT menu_id FROM sys_menu WHERE menu_name='图书业务'),1,'book-mgmt','',1,0,'M','0','0','','book','admin',NOW(),'馆藏与借阅：图书信息/借阅记录/轮播图' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='图书管理');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '读者服务',(SELECT menu_id FROM sys_menu WHERE menu_name='图书业务'),2,'reader-mgmt','',1,0,'M','0','0','','peoples','admin',NOW(),'读者业务：读者管理/登记/预约' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='读者服务');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '经营管理',(SELECT menu_id FROM sys_menu WHERE menu_name='图书业务'),3,'ops','',1,0,'M','0','0','','shopping','admin',NOW(),'经营业务：订单/荐购/统计' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='经营管理');

-- ---------- 2. 原 9 个子菜单改挂新目录 ----------
-- 注意：MySQL 不允许 UPDATE 同表时直接子查询引用，用派生表 (SELECT ...) tmp 包装
UPDATE sys_menu SET parent_id=(SELECT menu_id FROM (SELECT menu_id FROM sys_menu WHERE menu_name='图书管理') tmp), order_num=1 WHERE menu_name='图书信息';
UPDATE sys_menu SET parent_id=(SELECT menu_id FROM (SELECT menu_id FROM sys_menu WHERE menu_name='图书管理') tmp), order_num=2 WHERE menu_name='借阅记录';
UPDATE sys_menu SET parent_id=(SELECT menu_id FROM (SELECT menu_id FROM sys_menu WHERE menu_name='图书管理') tmp), order_num=3 WHERE menu_name='轮播图管理';
UPDATE sys_menu SET parent_id=(SELECT menu_id FROM (SELECT menu_id FROM sys_menu WHERE menu_name='读者服务') tmp), order_num=1 WHERE menu_name='读者管理';
UPDATE sys_menu SET parent_id=(SELECT menu_id FROM (SELECT menu_id FROM sys_menu WHERE menu_name='读者服务') tmp), order_num=2 WHERE menu_name='读者登记';
UPDATE sys_menu SET parent_id=(SELECT menu_id FROM (SELECT menu_id FROM sys_menu WHERE menu_name='读者服务') tmp), order_num=3 WHERE menu_name='预约管理';
UPDATE sys_menu SET parent_id=(SELECT menu_id FROM (SELECT menu_id FROM sys_menu WHERE menu_name='经营管理') tmp), order_num=1 WHERE menu_name='订单管理';
UPDATE sys_menu SET parent_id=(SELECT menu_id FROM (SELECT menu_id FROM sys_menu WHERE menu_name='经营管理') tmp), order_num=2 WHERE menu_name='荐购管理';
UPDATE sys_menu SET parent_id=(SELECT menu_id FROM (SELECT menu_id FROM sys_menu WHERE menu_name='经营管理') tmp), order_num=3 WHERE menu_name='借阅统计';

-- ---------- 3. 三角色补目录关联（角色可见范围跟随新层级） ----------
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id FROM sys_role r, sys_menu m
WHERE r.role_key='librarian' AND m.menu_name IN ('图书管理','读者服务')
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id=r.role_id AND rm.menu_id=m.menu_id);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id FROM sys_role r, sys_menu m
WHERE r.role_key='cashier' AND m.menu_name IN ('图书管理','经营管理')
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id=r.role_id AND rm.menu_id=m.menu_id);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id FROM sys_role r, sys_menu m
WHERE r.role_key='viewer' AND m.menu_name IN ('经营管理')
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id=r.role_id AND rm.menu_id=m.menu_id);
