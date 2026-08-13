-- ============================================
-- 图书管理系统 业务初始化 SQL
-- 前置：先导入 ry_20260417.sql 和 quartz.sql
-- 说明：本文件幂等，可重复执行
-- ============================================

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `book` (
  `book_id` bigint NOT NULL AUTO_INCREMENT COMMENT '图书ID',
  `book_name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '图书名称',
  `author` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '作者',
  `book_type` varchar(10) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '图书类型(字典:book_type)',
  `publisher` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '出版社',
  `price` decimal(10,2) DEFAULT NULL COMMENT '价格(元)',
  `publish_date` date DEFAULT NULL COMMENT '出版日期',
  `stock` int DEFAULT '0' COMMENT '库存数量',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态(0在架 1下架)',
  `cover` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '封面图片',
  `isbn` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'ISBN书号',
  `intro` varchar(1000) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '图书简介',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`book_id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='图书信息表';
/*!40101 SET character_set_client = @saved_cs_client */;
INSERT INTO `book` VALUES (1,'三体','刘慈欣','1','重庆出版社',88.00,'2008-01-01',9,'0','科幻经典','admin','2026-08-12 09:40:40','',NULL),(2,'深入理解计算机系统','Randal E. Bryant','2','机械工业出版社',139.00,'2016-11-01',5,'0','计算机必读','admin','2026-08-12 09:40:40','',NULL),(3,'明朝那些事儿','当年明月','3','中国海关出版社',358.00,'2009-04-01',20,'1','通俗历史','admin','2026-08-12 09:40:40','',NULL),(4,'活着','余华','1','作家出版社',35.00,'2012-08-01',50,'0','经典文学','admin','2026-08-12 11:08:22','',NULL),(5,'哇奥','哇奥','1','哇奥',576.00,'2026-08-05',7,'0','经典文学','','2026-08-12 11:21:25','','2026-08-12 15:59:02'),(6,'百年孤独','加西亚·马尔克斯','1','南海出版公司',55.00,'2011-06-01',8,'0','魔幻现实主义','admin','2026-08-12 17:14:21','',NULL),(7,'围城','钱钟书','1','人民文学出版社',39.00,'1991-02-01',6,'0','经典讽刺小说','admin','2026-08-12 17:14:21','',NULL);
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reader` (
  `reader_id` bigint NOT NULL AUTO_INCREMENT COMMENT '读者ID',
  `reader_name` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '读者姓名',
  `phone` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '手机号码',
  `card_no` varchar(30) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '借书证号',
  `reader_type` varchar(10) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '读者类型',
  `sex` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '性别(0男 1女 2未知)',
  `birth_date` date DEFAULT NULL COMMENT '出生日期',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态(0正常 1停用)',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`reader_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='读者信息表';
/*!40101 SET character_set_client = @saved_cs_client */;
INSERT INTO `reader` VALUES (1,'哇奥','13800138000','JZ20260001','1','0',NULL,'0',NULL,'','2026-08-12 15:33:04','','2026-08-12 15:36:30'),(2,'Jerry','13937262834',NULL,'2','0',NULL,'0',NULL,'','2026-08-12 16:40:58','',NULL),(4,'前台登记测试','13700002222',NULL,'2','0',NULL,'0','通过浏览器模拟前台登记','','2026-08-12 16:48:14','',NULL),(5,'证号测试','13600003333','JS25455819','1','0',NULL,'0','','','2026-08-12 17:04:16','',NULL),(6,'test2','13424532324','JS25551661','3','0',NULL,'0',NULL,'','2026-08-12 17:05:52','','2026-08-12 17:06:17');
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `borrow_record` (
  `borrow_id` bigint NOT NULL AUTO_INCREMENT COMMENT '借阅ID',
  `reader_id` bigint DEFAULT NULL COMMENT '读者ID',
  `book_id` bigint DEFAULT NULL COMMENT '图书ID',
  `borrow_date` date DEFAULT NULL COMMENT '借出日期',
  `due_date` date DEFAULT NULL COMMENT '应还日期',
  `return_date` date DEFAULT NULL COMMENT '归还日期',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态(0借出中 1已归还 2已逾期)',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`borrow_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='借阅记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
INSERT INTO `borrow_record` VALUES (1,2,1,'2026-08-12','2026-09-11','2026-08-12','1','借阅测试','',NULL,'',NULL),(2,1,1,'2026-08-12','2026-08-01',NULL,'2','保留借出状态','',NULL,'','2026-08-12 17:27:49');

-- ============================================
-- 图书管理系统 业务初始化 SQL（幂等，可重复执行）
-- 使用：导入 ry_20260417.sql + quartz.sql 后，再导入本文件
-- 包含：业务表(book/reader/borrow_record) + 字典 + 业务菜单
-- ============================================

-- ---------- 字典：图书分类 ----------
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '图书分类','book_type','0','admin',NOW(),'图书分类' WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='book_type');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, list_class, status, create_by, create_time)
SELECT 1,'文学','1','book_type','primary','0','admin',NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='book_type' AND dict_value='1');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, list_class, status, create_by, create_time)
SELECT 2,'科技','2','book_type','success','0','admin',NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='book_type' AND dict_value='2');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, list_class, status, create_by, create_time)
SELECT 3,'历史','3','book_type','warning','0','admin',NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='book_type' AND dict_value='3');

-- ---------- 字典：读者类型 ----------
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '读者类型','reader_type','0','admin',NOW(),'读者分类' WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='reader_type');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, list_class, status, create_by, create_time)
SELECT 1,'学生','1','reader_type','primary','0','admin',NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='reader_type' AND dict_value='1');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, list_class, status, create_by, create_time)
SELECT 2,'教师','2','reader_type','success','0','admin',NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='reader_type' AND dict_value='2');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, list_class, status, create_by, create_time)
SELECT 3,'普通读者','3','reader_type','warning','0','admin',NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='reader_type' AND dict_value='3');

-- ---------- 菜单：图书业务目录 ----------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '图书业务',0,1,'business','',1,0,'M','0','0','','book','admin',NOW(),'图书业务模块' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='图书业务');

-- 图书信息
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '图书信息',(SELECT menu_id FROM sys_menu WHERE menu_name='图书业务'),1,'book','system/book/index',1,0,'C','0','0','system:book:list','book','admin',NOW(),'图书信息菜单' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='图书信息');
-- 读者管理
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '读者管理',(SELECT menu_id FROM sys_menu WHERE menu_name='图书业务'),2,'reader','system/reader/index',1,0,'C','0','0','system:reader:list','peoples','admin',NOW(),'读者管理菜单' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='读者管理');
-- 读者登记（表单构建产物页面）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '读者登记',(SELECT menu_id FROM sys_menu WHERE menu_name='图书业务'),3,'reader/form','system/reader/form',1,0,'C','0','0','system:reader:add','form','admin',NOW(),'读者登记表' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='读者登记');
-- 借阅记录
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '借阅记录',(SELECT menu_id FROM sys_menu WHERE menu_name='图书业务'),4,'borrow','system/borrow/index',1,0,'C','0','0','system:borrow:list','reading','admin',NOW(),'借阅管理菜单' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='借阅记录');
-- 借阅统计
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '借阅统计',(SELECT menu_id FROM sys_menu WHERE menu_name='图书业务'),5,'borrow/stats','system/borrow/stats',1,0,'C','0','0','system:borrow:stats','chart','admin',NOW(),'借阅统计报表' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='借阅统计');

-- ---------- 定时任务：逾期检查 ----------
INSERT INTO sys_job (job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, remark)
SELECT '逾期检查','SYSTEM','borrowTask.updateOverdueStatus()','0 0 0 * * ?','3','1','0','admin',NOW(),'每天0点自动标记逾期借阅' WHERE NOT EXISTS (SELECT 1 FROM sys_job WHERE job_name='逾期检查');

-- ============================================
-- 以下为后续版本补充（幂等）：图书封面/ISBN/简介列、购书订单表、订单菜单
-- ============================================

-- ---------- 幂等补列：book 表封面/ISBN/简介（老库自动补齐，新库跳过） ----------
SET @c1 = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='book' AND column_name='cover');
SET @s1 = IF(@c1=0, 'ALTER TABLE book ADD COLUMN cover varchar(255) DEFAULT NULL COMMENT ''封面图片''', 'SELECT 1');
PREPARE st1 FROM @s1; EXECUTE st1; DEALLOCATE PREPARE st1;
SET @c2 = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='book' AND column_name='isbn');
SET @s2 = IF(@c2=0, 'ALTER TABLE book ADD COLUMN isbn varchar(20) DEFAULT NULL COMMENT ''ISBN书号''', 'SELECT 1');
PREPARE st2 FROM @s2; EXECUTE st2; DEALLOCATE PREPARE st2;
SET @c3 = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='book' AND column_name='intro');
SET @s3 = IF(@c3=0, 'ALTER TABLE book ADD COLUMN intro varchar(1000) DEFAULT NULL COMMENT ''图书简介''', 'SELECT 1');
PREPARE st3 FROM @s3; EXECUTE st3; DEALLOCATE PREPARE st3;

-- ---------- 购书订单表 ----------
CREATE TABLE IF NOT EXISTS `shop_order` (
  `order_id` bigint NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no` varchar(30) DEFAULT NULL COMMENT '订单号',
  `reader_id` bigint DEFAULT NULL COMMENT '读者ID',
  `reader_name` varchar(50) DEFAULT NULL COMMENT '读者姓名',
  `card_no` varchar(30) DEFAULT NULL COMMENT '借书证号',
  `book_id` bigint DEFAULT NULL COMMENT '图书ID',
  `book_name` varchar(100) DEFAULT NULL COMMENT '图书名称',
  `quantity` int DEFAULT '1' COMMENT '购买数量',
  `total_price` decimal(10,2) DEFAULT NULL COMMENT '订单总价(元)',
  `status` char(1) DEFAULT '0' COMMENT '状态(0待处理 1已完成 2已取消)',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='购书订单表';

-- ---------- 菜单：订单管理（图书业务目录下） ----------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '订单管理',(SELECT menu_id FROM sys_menu WHERE menu_name='图书业务'),6,'order','system/order/index',1,0,'C','0','0','system:order:list','shopping','admin',NOW(),'购书订单管理菜单' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='订单管理');
-- 订单权限点（按钮）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '订单查询',(SELECT menu_id FROM sys_menu WHERE menu_name='订单管理'),1,'','',1,0,'F','0','0','system:order:query','#','admin',NOW(),'订单查询按钮' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='订单查询');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '订单修改',(SELECT menu_id FROM sys_menu WHERE menu_name='订单管理'),2,'','',1,0,'F','0','0','system:order:edit','#','admin',NOW(),'订单状态流转按钮' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='订单修改');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '订单删除',(SELECT menu_id FROM sys_menu WHERE menu_name='订单管理'),3,'','',1,0,'F','0','0','system:order:remove','#','admin',NOW(),'订单删除按钮' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='订单删除');

-- ---------- 清理测试残留数据（精确匹配，不影响正式数据） ----------
DELETE FROM book WHERE book_name='哇奥' AND price=576;
DELETE FROM reader WHERE reader_name='前台登记测试';
DELETE FROM reader WHERE reader_name='test2';
