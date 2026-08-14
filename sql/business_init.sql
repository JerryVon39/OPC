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
-- 测试读者（名字即用途说明：学生/教师/普通/挂失 四种类型，对应差异化借阅规则与补办演示）
INSERT INTO `reader` (reader_id, reader_name, phone, card_no, reader_type, sex, status, remark, create_by, create_time) VALUES
(1, '学生测试', '13800000001', 'JS20260001', '1', '1', '0', '测试数据-学生读者(借阅上限5本/借期30天)', 'admin', NOW()),
(2, '教师测试', '13800000002', 'JS20260002', '2', '0', '0', '测试数据-教师读者(借阅上限10本/借期60天)', 'admin', NOW()),
(3, '普通测试', '13800000003', 'JS20260003', '3', '0', '0', '测试数据-普通读者(借阅上限3本/借期30天)', 'admin', NOW()),
(4, '挂失测试', '13800000004', 'JS20260004', '3', '0', '1', '测试数据-挂失读者(可演示前台申请补办)', 'admin', NOW()),
(5, 'Jerry', '12345678901', 'DK', '2', '0', '0', '测试数据-项目作者账号(教师)', 'admin', NOW());
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
  `reader_name` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '读者姓名(快照)',
  `card_no` varchar(30) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '借书证号(快照)',
  `book_name` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '图书名称(快照)',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`borrow_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='借阅记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
-- 测试借阅记录（覆盖：借出中/即将到期/教师60天借期/已归还/已逾期 五种状态）
INSERT INTO borrow_record (borrow_id, reader_id, book_id, borrow_date, due_date, return_date, status, reader_name, card_no, book_name, create_by, create_time) VALUES
(1, 1, 1, '2026-08-13', '2026-09-12', NULL, '0', '学生测试', 'JS20260001', '三体', 'system', NOW()),
(2, 1, 9, '2026-07-21', '2026-08-20', NULL, '0', '学生测试', 'JS20260001', '红楼梦', 'system', NOW()),
(3, 2, 2, '2026-08-13', '2026-10-12', NULL, '0', '教师测试', 'JS20260002', '深入理解计算机系统', 'system', NOW()),
(4, 3, 4, '2026-07-01', '2026-07-31', '2026-07-20', '1', '普通测试', 'JS20260003', '活着', 'system', NOW()),
(5, 3, 13, '2026-07-02', '2026-08-01', NULL, '2', '普通测试', 'JS20260003', '小王子', 'system', NOW());
-- 测试订单（四种状态齐全：待付款/已收款/已完成/已取消）
INSERT INTO shop_order (order_no, reader_id, reader_name, card_no, book_id, book_name, quantity, total_price, status, create_by, create_time) VALUES
('WSW20260813001', 1, '学生测试', 'JS20260001', 13, '小王子', 1, 22.00, '0', '学生测试', NOW()),
('WSW20260810001', 1, '学生测试', 'JS20260001', 15, '白夜行', 1, 59.60, '3', '学生测试', DATE_SUB(NOW(), INTERVAL 3 DAY)),
('WSW20260812001', 2, '教师测试', 'JS20260002', 21, '算法导论（第3版）', 1, 128.00, '1', '教师测试', DATE_SUB(NOW(), INTERVAL 1 DAY)),
('WSW20260811001', 3, '普通测试', 'JS20260003', 7, '围城', 2, 78.00, '2', '普通测试', DATE_SUB(NOW(), INTERVAL 2 DAY));
-- ---------- 图书预约表 ----------
CREATE TABLE IF NOT EXISTS `book_reserve` (
  `reserve_id` bigint NOT NULL AUTO_INCREMENT COMMENT '预约ID',
  `book_id` bigint DEFAULT NULL COMMENT '图书ID',
  `reader_id` bigint DEFAULT NULL COMMENT '读者ID',
  `reader_name` varchar(50) DEFAULT NULL COMMENT '读者姓名(快照)',
  `card_no` varchar(30) DEFAULT NULL COMMENT '借书证号(快照)',
  `book_name` varchar(100) DEFAULT NULL COMMENT '图书名称(快照)',
  `reserve_date` datetime DEFAULT NULL COMMENT '预约时间',
  `status` char(1) DEFAULT '0' COMMENT '状态(0预约中 1可借 2已完成 3已取消)',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`reserve_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='图书预约表';

-- 分散演示书入库日期（前台"新"角标只显示最近入库的3本，其余为旧书）
UPDATE book SET create_time='2026-06-15 10:00:00' WHERE book_name IN ('三体','深入理解计算机系统','明朝那些事儿','活着','百年孤独','围城');
UPDATE book SET create_time='2026-07-01 10:00:00' WHERE book_name IN ('平凡的世界','红楼梦','西游记','三国演义','水浒传','小王子','老人与海','呐喊','边城','骆驼祥子','代码大全（第2版）','万历十五年');

-- ---------- 前台轮播图 ----------
CREATE TABLE IF NOT EXISTS `sys_banner` (
  `banner_id` bigint NOT NULL AUTO_INCREMENT COMMENT '轮播ID',
  `title` varchar(100) DEFAULT NULL COMMENT '标题',
  `subtitle` varchar(200) DEFAULT NULL COMMENT '副标题',
  `image` varchar(255) DEFAULT NULL COMMENT '图片地址(可为空,空则渐变背景)',
  `link` varchar(255) DEFAULT NULL COMMENT '跳转链接',
  `sort` int DEFAULT '0' COMMENT '排序',
  `status` char(1) DEFAULT '0' COMMENT '状态(0启用 1停用)',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`banner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='前台轮播图';
INSERT INTO sys_banner (title, subtitle, link, sort, status, create_by, create_time) VALUES
('万事屋', '万事屋，万事皆可办 ｜ 借书自助，还书请到服务台', '', 1, '0', 'admin', NOW()),
('图书预约', '书被借光？一键预约，归还后自动通知您来借', '/shop.html', 2, '0', 'admin', NOW()),
('新书上架', '藏书持续更新，文学 / 科技 / 历史任你挑选', '', 3, '0', 'admin', NOW())
ON DUPLICATE KEY UPDATE title=VALUES(title);

-- 轮播图管理菜单与权限点
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '轮播图管理',(SELECT menu_id FROM sys_menu WHERE menu_name='图书业务'),8,'banner','system/banner/index',1,0,'C','0','0','system:banner:list','picture','admin',NOW(),'前台轮播图管理菜单' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='轮播图管理');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '轮播图查询',(SELECT menu_id FROM sys_menu WHERE menu_name='轮播图管理'),1,'','',1,0,'F','0','0','system:banner:query','#','admin',NOW(),'轮播图查询' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='轮播图查询');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '轮播图新增',(SELECT menu_id FROM sys_menu WHERE menu_name='轮播图管理'),2,'','',1,0,'F','0','0','system:banner:add','#','admin',NOW(),'轮播图新增' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='轮播图新增');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '轮播图修改',(SELECT menu_id FROM sys_menu WHERE menu_name='轮播图管理'),3,'','',1,0,'F','0','0','system:banner:edit','#','admin',NOW(),'轮播图修改' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='轮播图修改');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '轮播图删除',(SELECT menu_id FROM sys_menu WHERE menu_name='轮播图管理'),4,'','',1,0,'F','0','0','system:banner:remove','#','admin',NOW(),'轮播图删除' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='轮播图删除');

-- ---------- 续借次数字段与参数 ----------
SET @rc = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='borrow_record' AND column_name='renew_count');
SET @rs = IF(@rc=0, 'ALTER TABLE borrow_record ADD COLUMN renew_count int DEFAULT 0 COMMENT ''已续借次数''', 'SELECT 1');
PREPARE rst FROM @rs; EXECUTE rst; DEALLOCATE PREPARE rst;
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '续借次数上限','book.borrow.renewLimit','1','Y','admin',NOW(),'每本图书最多可续借次数' WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key='book.borrow.renewLimit');

-- BBCODE 演示：小王子简介展示富文本效果
UPDATE book SET intro = '写给大人的[b]童话[/b]，关于爱与责任的寓言。
[quote]只有用心才能看得清，真正重要的东西用眼睛是看不见的。[/quote]
[color=#c65d43]全球销量超 2 亿册[/color]，[url=https://baike.baidu.com/item/小王子]了解更多 →[/url]' WHERE book_name='小王子';

-- 预约管理菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '预约管理',(SELECT menu_id FROM sys_menu WHERE menu_name='图书业务'),7,'reserve','system/reserve/index',1,0,'C','0','0','system:borrow:list','date','admin',NOW(),'图书预约管理菜单' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='预约管理');

-- 预约参数与定时任务
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '预约可借保留天数','book.reserve.expireDays','2','Y','admin',NOW(),'可借状态超过该天数未到馆借阅自动取消' WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key='book.reserve.expireDays');
INSERT INTO sys_job (job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, remark)
SELECT '预约超时检查','SYSTEM','borrowTask.reserveExpireCheck()','0 0 8 * * ?','3','1','0','admin',NOW(),'每天8点自动取消超时未取的可借预约并通知下一位' WHERE NOT EXISTS (SELECT 1 FROM sys_job WHERE job_name='预约超时检查');

-- 预约演示数据（《白夜行》完整状态链：可借/预约中/已完成/已取消，打开"我的预约"即见）
INSERT INTO book_reserve (book_id, reader_id, reader_name, card_no, book_name, reserve_date, status, create_by, create_time) VALUES
(15, 2, '教师测试', 'JS20260002', '白夜行', DATE_SUB(NOW(), INTERVAL 2 DAY), '1', '教师测试', NOW()),
(15, 1, '学生测试', 'JS20260001', '白夜行', DATE_SUB(NOW(), INTERVAL 1 DAY), '0', '学生测试', NOW()),
(15, 3, '普通测试', 'JS20260003', '白夜行', DATE_SUB(NOW(), INTERVAL 5 DAY), '2', '普通测试', NOW()),
(15, 5, 'Jerry', 'DK', '白夜行', DATE_SUB(NOW(), INTERVAL 7 DAY), '3', 'Jerry', NOW());
-- 白夜行库存置 1（对应"可借"那本，与预约状态自洽）
UPDATE book SET stock=1 WHERE book_id=15 AND stock=18;

-- 挂失测试历史借阅（已归还，演示补办后历史保留）
INSERT INTO borrow_record (reader_id, book_id, borrow_date, due_date, return_date, status, reader_name, card_no, book_name, create_by, create_time) VALUES
(4, 18, '2026-07-10', '2026-08-09', '2026-07-25', '1', '挂失测试', 'JS20260004', '边城', 'system', NOW());

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
-- 借阅导出（按钮权限点）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '借阅导出',(SELECT menu_id FROM sys_menu WHERE menu_name='借阅记录'),6,'','',1,0,'F','0','0','system:borrow:export','#','admin',NOW(),'借阅记录导出按钮' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='借阅导出');
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

-- ============================================
-- 以下为后续版本补充（幂等）：证号唯一索引、库存预警参数、演示图书
-- ============================================

-- ---------- 读者证号唯一索引 ----------
SET @idx = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='reader' AND index_name='uk_card_no');
SET @sql_idx = IF(@idx=0, 'ALTER TABLE reader ADD UNIQUE INDEX uk_card_no (card_no)', 'SELECT 1');
PREPARE st_idx FROM @sql_idx; EXECUTE st_idx; DEALLOCATE PREPARE st_idx;

-- ---------- 借阅记录快照列（老库自动补齐：读者/图书删除后历史记录仍完整） ----------
SET @bc1 = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='borrow_record' AND column_name='reader_name');
SET @bs1 = IF(@bc1=0, 'ALTER TABLE borrow_record ADD COLUMN reader_name varchar(50) DEFAULT NULL COMMENT ''读者姓名(快照)''', 'SELECT 1');
PREPARE bst1 FROM @bs1; EXECUTE bst1; DEALLOCATE PREPARE bst1;
SET @bc2 = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='borrow_record' AND column_name='card_no');
SET @bs2 = IF(@bc2=0, 'ALTER TABLE borrow_record ADD COLUMN card_no varchar(30) DEFAULT NULL COMMENT ''借书证号(快照)''', 'SELECT 1');
PREPARE bst2 FROM @bs2; EXECUTE bst2; DEALLOCATE PREPARE bst2;
SET @bc3 = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='borrow_record' AND column_name='book_name');
SET @bs3 = IF(@bc3=0, 'ALTER TABLE borrow_record ADD COLUMN book_name varchar(100) DEFAULT NULL COMMENT ''图书名称(快照)''', 'SELECT 1');
PREPARE bst3 FROM @bs3; EXECUTE bst3; DEALLOCATE PREPARE bst3;

-- ---------- 逾期罚款字段（老库自动补齐） ----------
SET @fc1 = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='borrow_record' AND column_name='fine_amount');
SET @fs1 = IF(@fc1=0, 'ALTER TABLE borrow_record ADD COLUMN fine_amount decimal(10,2) DEFAULT 0.00 COMMENT ''逾期罚款金额(元)''', 'SELECT 1');
PREPARE fst1 FROM @fs1; EXECUTE fst1; DEALLOCATE PREPARE fst1;
SET @fc2 = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='borrow_record' AND column_name='fine_paid');
SET @fs2 = IF(@fc2=0, 'ALTER TABLE borrow_record ADD COLUMN fine_paid char(1) DEFAULT ''0'' COMMENT ''罚款是否已缴(0未缴 1已缴)''', 'SELECT 1');
PREPARE fst2 FROM @fs2; EXECUTE fst2; DEALLOCATE PREPARE fst2;
-- 罚款参数
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '逾期罚款单价','book.fine.perDay','0.10','Y','admin',NOW(),'逾期每天罚款金额(元)' WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key='book.fine.perDay');
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '罚款免罚天数','book.fine.graceDays','0','Y','admin',NOW(),'逾期超过该天数才计罚' WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key='book.fine.graceDays');

-- ---------- 库存预警阈值参数 ----------
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '库存预警阈值','book.stock.warn','3','Y','admin',NOW(),'库存低于或等于该值时，前后台显示库存预警标签' WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key='book.stock.warn');

-- ---------- 演示图书扩充（幂等：按 ISBN 判重） ----------
INSERT INTO book (book_name, author, book_type, publisher, price, publish_date, stock, status, isbn, intro, create_by, create_time)
SELECT '平凡的世界','路遥','1','北京十月文艺出版社',79.60,'2017-06-01',30,'0','9787530216781','全景式展现中国当代城乡社会生活，茅盾文学奖获奖作品','admin',NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM book WHERE isbn='9787530216781');
INSERT INTO book (book_name, author, book_type, publisher, price, publish_date, stock, status, isbn, intro, create_by, create_time)
SELECT '红楼梦','曹雪芹','1','人民文学出版社',59.70,'1996-12-01',25,'0','9787020002207','中国古典四大名著之首，封建社会的百科全书','admin',NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM book WHERE isbn='9787020002207');
INSERT INTO book (book_name, author, book_type, publisher, price, publish_date, stock, status, isbn, intro, create_by, create_time)
SELECT '西游记','吴承恩','1','人民文学出版社',47.20,'1980-05-01',28,'0','9787020008735','中国古典神魔小说巅峰，唐僧师徒西天取经','admin',NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM book WHERE isbn='9787020008735');
INSERT INTO book (book_name, author, book_type, publisher, price, publish_date, stock, status, isbn, intro, create_by, create_time)
SELECT '三国演义','罗贯中','1','人民文学出版社',39.50,'1992-06-01',26,'0','9787020008728','中国第一部长篇章回体历史演义小说','admin',NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM book WHERE isbn='9787020008728');
INSERT INTO book (book_name, author, book_type, publisher, price, publish_date, stock, status, isbn, intro, create_by, create_time)
SELECT '水浒传','施耐庵','1','人民文学出版社',50.60,'1997-01-01',24,'0','9787020008759','一百零八将聚义梁山，中国古典英雄传奇','admin',NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM book WHERE isbn='9787020008759');
INSERT INTO book (book_name, author, book_type, publisher, price, publish_date, stock, status, isbn, intro, create_by, create_time)
SELECT '小王子','圣埃克苏佩里','1','人民文学出版社',22.00,'2003-08-01',40,'0','9787020042494','写给大人的童话，关于爱与责任的寓言','admin',NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM book WHERE isbn='9787020042494');
INSERT INTO book (book_name, author, book_type, publisher, price, publish_date, stock, status, isbn, intro, create_by, create_time)
SELECT '老人与海','海明威','1','上海译文出版社',25.00,'2009-07-01',35,'0','9787532748662','硬汉文学经典，人可以被毁灭但不能被打败','admin',NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM book WHERE isbn='9787532748662');
INSERT INTO book (book_name, author, book_type, publisher, price, publish_date, stock, status, isbn, intro, create_by, create_time)
SELECT '白夜行','东野圭吾','2','南海出版公司',59.60,'2013-01-01',18,'0','9787544270878','东野圭吾巅峰之作，绝望与救赎的悲歌','admin',NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM book WHERE isbn='9787544270878');
INSERT INTO book (book_name, author, book_type, publisher, price, publish_date, stock, status, isbn, intro, create_by, create_time)
SELECT '解忧杂货店','东野圭吾','2','南海出版公司',39.50,'2014-05-01',22,'0','9787544270879','温暖治愈的推理小说，穿越时空的回信','admin',NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM book WHERE isbn='9787544270879');
INSERT INTO book (book_name, author, book_type, publisher, price, publish_date, stock, status, isbn, intro, create_by, create_time)
SELECT '呐喊','鲁迅','1','人民文学出版社',22.00,'1973-03-01',30,'0','9787020008742','中国现代小说奠基之作，唤醒沉睡的灵魂','admin',NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM book WHERE isbn='9787020008742');
INSERT INTO book (book_name, author, book_type, publisher, price, publish_date, stock, status, isbn, intro, create_by, create_time)
SELECT '边城','沈从文','1','北岳文艺出版社',18.00,'2002-04-01',32,'0','9787537812249','湘西田园牧歌，翠翠与傩送的爱情','admin',NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM book WHERE isbn='9787537812249');
INSERT INTO book (book_name, author, book_type, publisher, price, publish_date, stock, status, isbn, intro, create_by, create_time)
SELECT '骆驼祥子','老舍','1','人民文学出版社',25.00,'1962-11-01',27,'0','9787020009626','旧社会人力车夫的命运，现实主义经典','admin',NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM book WHERE isbn='9787020009626');
INSERT INTO book (book_name, author, book_type, publisher, price, publish_date, stock, status, isbn, intro, create_by, create_time)
SELECT '代码大全（第2版）','Steve McConnell','2','电子工业出版社',128.00,'2006-03-01',12,'0','9787121022982','软件构建的百科全书，程序员必读','admin',NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM book WHERE isbn='9787121022982');
INSERT INTO book (book_name, author, book_type, publisher, price, publish_date, stock, status, isbn, intro, create_by, create_time)
SELECT '算法导论（第3版）','Thomas H. Cormen','2','机械工业出版社',128.00,'2012-12-01',10,'0','9787111407010','算法领域的经典教材','admin',NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM book WHERE isbn='9787111407010');
INSERT INTO book (book_name, author, book_type, publisher, price, publish_date, stock, status, isbn, intro, create_by, create_time)
SELECT '万历十五年','黄仁宇','3','中华书局',18.00,'2007-01-01',20,'0','9787101054033','大历史观代表作，以小事见大时代','admin',NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM book WHERE isbn='9787101054033');

-- ============================================
-- 查询性能索引（幂等）：借阅/订单/预约按常用查询条件加索引，数据量增长后避免全表扫描
-- ============================================

-- ---------- 借阅记录：按读者查（借书校验/我的借阅）、按图书查（归还后预约联动/重复借校验） ----------
SET @i1 = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='borrow_record' AND index_name='idx_br_reader');
SET @si1 = IF(@i1=0, 'ALTER TABLE borrow_record ADD INDEX idx_br_reader (reader_id)', 'SELECT 1');
PREPARE st1 FROM @si1; EXECUTE st1; DEALLOCATE PREPARE st1;
SET @i2 = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='borrow_record' AND index_name='idx_br_book');
SET @si2 = IF(@i2=0, 'ALTER TABLE borrow_record ADD INDEX idx_br_book (book_id)', 'SELECT 1');
PREPARE st2 FROM @si2; EXECUTE st2; DEALLOCATE PREPARE st2;

-- ---------- 购书订单：按证号查（前台"我的订单"） ----------
SET @i3 = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='shop_order' AND index_name='idx_so_card');
SET @si3 = IF(@i3=0, 'ALTER TABLE shop_order ADD INDEX idx_so_card (card_no)', 'SELECT 1');
PREPARE st3 FROM @si3; EXECUTE st3; DEALLOCATE PREPARE st3;

-- ---------- 图书预约：按证号查（前台"我的预约"）、按图书查（还书后找最早预约） ----------
SET @i4 = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='book_reserve' AND index_name='idx_res_card');
SET @si4 = IF(@i4=0, 'ALTER TABLE book_reserve ADD INDEX idx_res_card (card_no)', 'SELECT 1');
PREPARE st4 FROM @si4; EXECUTE st4; DEALLOCATE PREPARE st4;
SET @i5 = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='book_reserve' AND index_name='idx_res_book');
SET @si5 = IF(@i5=0, 'ALTER TABLE book_reserve ADD INDEX idx_res_book (book_id)', 'SELECT 1');
PREPARE st5 FROM @si5; EXECUTE st5; DEALLOCATE PREPARE st5;

-- ============================================
-- 孤儿菜单自愈（幂等）：历史脚本顺序问题可能让菜单 parent_id 为 NULL，
-- 会导致登录后路由构建 NPE（Cannot invoke getParentId().longValue()）
-- ============================================
UPDATE sys_menu m JOIN sys_menu p ON p.menu_name='图书业务' SET m.parent_id=p.menu_id
WHERE m.menu_name='预约管理' AND m.parent_id IS NULL;
UPDATE sys_menu m JOIN sys_menu p ON p.menu_name='订单管理' SET m.parent_id=p.menu_id
WHERE m.menu_name='订单删除' AND m.parent_id IS NULL;
