-- ============================================
-- 升级脚本：回收站（2026-08-19）
-- 用途：防止管理员误删图书/读者，删除时快照进回收站表，支持还原/彻底删除
-- 执行方式：mysql -uroot -p --default-character-set=utf8mb4 ry-vue < upgrade_20260819_recycle.sql
-- 全部为幂等语句（IF NOT EXISTS / WHERE NOT EXISTS），可重复执行
-- ============================================

USE `ry-vue`;

CREATE TABLE IF NOT EXISTS `book_recycle` (
  `recycle_id` bigint NOT NULL AUTO_INCREMENT COMMENT '回收站ID',
  `book_id` bigint DEFAULT NULL COMMENT '原图书ID',
  `book_name` varchar(100) NOT NULL COMMENT '图书名称',
  `author` varchar(50) DEFAULT NULL COMMENT '作者',
  `book_type` varchar(10) DEFAULT NULL COMMENT '图书类型(字典:book_type)',
  `publisher` varchar(100) DEFAULT NULL COMMENT '出版社',
  `price` decimal(10,2) DEFAULT NULL COMMENT '价格(元)',
  `publish_date` date DEFAULT NULL COMMENT '出版日期',
  `stock` int DEFAULT '0' COMMENT '库存数量',
  `status` char(1) DEFAULT '0' COMMENT '状态(0在架 1下架)',
  `cover` varchar(255) DEFAULT NULL COMMENT '封面图片',
  `isbn` varchar(20) DEFAULT NULL COMMENT 'ISBN书号',
  `intro` varchar(1000) DEFAULT NULL COMMENT '图书简介',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `deleted_by` varchar(64) DEFAULT '' COMMENT '删除人',
  `deleted_time` datetime DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`recycle_id`),
  KEY `idx_book_recycle_name` (`book_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='图书回收站';

CREATE TABLE IF NOT EXISTS `reader_recycle` (
  `recycle_id` bigint NOT NULL AUTO_INCREMENT COMMENT '回收站ID',
  `reader_id` bigint DEFAULT NULL COMMENT '原读者ID',
  `reader_name` varchar(50) NOT NULL COMMENT '读者姓名',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号码',
  `card_no` varchar(30) DEFAULT NULL COMMENT '借书证号',
  `reader_type` varchar(10) DEFAULT NULL COMMENT '读者类型',
  `sex` char(1) DEFAULT '0' COMMENT '性别(0男 1女 2未知)',
  `birth_date` date DEFAULT NULL COMMENT '出生日期',
  `status` char(1) DEFAULT '0' COMMENT '状态(0正常 1停用)',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `deleted_by` varchar(64) DEFAULT '' COMMENT '删除人',
  `deleted_time` datetime DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`recycle_id`),
  KEY `idx_reader_recycle_name` (`reader_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='读者回收站';

-- 回收站菜单（图书业务下第 4 个目录）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '回收站',(SELECT menu_id FROM sys_menu WHERE menu_name='图书业务'),4,'recycle','',1,0,'M','0','0','','delete','admin',NOW(),'误删数据恢复' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='回收站');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '图书回收站',(SELECT menu_id FROM sys_menu WHERE menu_name='回收站'),1,'book','system/recycle/book',1,0,'C','0','0','system:recycle:book:list','book','admin',NOW(),'误删图书恢复' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='图书回收站');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '读者回收站',(SELECT menu_id FROM sys_menu WHERE menu_name='回收站'),2,'reader','system/recycle/reader',1,0,'C','0','0','system:recycle:reader:list','peoples','admin',NOW(),'误删读者恢复' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='读者回收站');

-- 角色关联：图书管理员可用回收站（数据恢复属管理职责，收银/访客不开放）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id FROM sys_role r, sys_menu m
WHERE r.role_key='librarian' AND m.menu_name IN ('回收站','图书回收站','读者回收站')
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id=r.role_id AND rm.menu_id=m.menu_id);