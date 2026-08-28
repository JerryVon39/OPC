-- ============================================
-- 升级脚本：荐购申请（book_purchase_req 表 + 荐购管理菜单）
-- 适用：已存在的数据库（全新库由 business_init.sql 一次创建，无需本文件）
-- 幂等：可重复执行，务必带 --default-character-set=utf8mb4
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p ry-vue < sql/upgrade_20260818_purchase.sql
-- ============================================


-- ============================================
CREATE TABLE IF NOT EXISTS `book_purchase_req` (
  `req_id` bigint NOT NULL AUTO_INCREMENT COMMENT '申请ID',
  `book_name` varchar(100) NOT NULL COMMENT '书名',
  `author` varchar(60) DEFAULT NULL COMMENT '作者',
  `status` char(1) DEFAULT '0' COMMENT '状态(0待处理 1已处理 2已拒绝)',
  `remark` varchar(255) DEFAULT NULL COMMENT '读者附言（出版社/版次等）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`req_id`),
  KEY `idx_purchase_book_name` (`book_name`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='图书荐购申请';

-- 荐购管理菜单（图书业务目录下）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '荐购管理',(SELECT menu_id FROM sys_menu WHERE menu_name='图书业务'),9,'purchase','system/purchase/index',1,0,'C','0','0','system:purchase:list','shopping-cart-full','admin',NOW(),'读者荐购申请管理菜单' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='荐购管理');
-- 荐购权限点（按钮）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '荐购查询',(SELECT menu_id FROM sys_menu WHERE menu_name='荐购管理'),1,'','',1,0,'F','0','0','system:purchase:query','#','admin',NOW(),'荐购查询按钮' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='荐购查询');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '荐购处理',(SELECT menu_id FROM sys_menu WHERE menu_name='荐购管理'),2,'','',1,0,'F','0','0','system:purchase:edit','#','admin',NOW(),'荐购处理按钮(标记已处理/已拒绝)' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='荐购处理');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '荐购删除',(SELECT menu_id FROM sys_menu WHERE menu_name='荐购管理'),3,'','',1,0,'F','0','0','system:purchase:remove','#','admin',NOW(),'荐购删除按钮' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='荐购删除');
