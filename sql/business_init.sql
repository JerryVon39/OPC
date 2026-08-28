-- ============================================
-- 数智游民创新工场 业务初始化 SQL
-- 前置：先导入 ry_20260417.sql 和 quartz.sql
-- 说明：本文件幂等，可重复执行
-- 注意：必须指定字符集执行，否则中文会乱码入库（Windows 默认 GBK 会把 UTF-8 读坏）：
--   mysql --default-character-set=utf8mb4 -uroot -p ry-vue < business_init.sql
-- 语义边界：Java 类名/表名/API URL 一律不变（Book→服务、Reader→成员、BorrowRecord→报名
-- 等均为展示层语义），本文件只改：菜单名、字典标签、参数名、示例数据。
-- ============================================

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `book` (
  `book_id` bigint NOT NULL AUTO_INCREMENT COMMENT '服务ID',
  `book_name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '服务名称',
  `author` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '主办方',
  `book_type` varchar(10) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '服务分类(字典:book_type)',
  `publisher` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '合作机构',
  `price` decimal(10,2) DEFAULT NULL COMMENT '费用(元)',
  `publish_date` date DEFAULT NULL COMMENT '上线时间',
  `stock` int DEFAULT '0' COMMENT '剩余名额',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态(0招募中 1已结束)',
  `cover` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '封面图片',
  `isbn` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '服务编号',
  `intro` varchar(1000) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '服务介绍',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`book_id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='服务信息表';
/*!40101 SET character_set_client = @saved_cs_client */;
-- 初始 6 条服务（覆盖：AI数字服务/创意设计/本地生活与创业 三类、已结束演示、免费体验、名额紧张；幂等按服务编号判重）
INSERT INTO `book` (book_id, book_name, author, book_type, publisher, price, publish_date, stock, status, isbn, intro, create_by, create_time, update_by, update_time)
SELECT 1,'AI 一人公司实战营','数智游民创新工场','1','清远高新区管委会',199.00,'2026-08-01',9,'0','9787536692930','从 0 到 1 打造一人公司：AI 工具矩阵、获客、交付全流程实操训练营。适合想用 AI 开启独立事业的个人主理人。','admin','2026-08-12 09:40:40','',NULL FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM book WHERE isbn='9787536692930');
INSERT INTO `book` (book_id, book_name, author, book_type, publisher, price, publish_date, stock, status, isbn, intro, create_by, create_time, update_by, update_time)
SELECT 2,'提示词工程入门课','林晓川','1','网易有道',99.00,'2026-07-15',3,'0','9787111544937','提示词是 AI 时代的敲门砖。本课程从基础结构到高级技巧，带你掌握与大模型高效对话的方法论。','admin','2026-08-12 09:40:40','',NULL FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM book WHERE isbn='9787111544937');
INSERT INTO `book` (book_id, book_name, author, book_type, publisher, price, publish_date, stock, status, isbn, intro, create_by, create_time, update_by, update_time)
SELECT 3,'共享工位月租计划','数智游民创新工场','3','清远智慧谷',500.00,'2026-06-01',20,'1','9787505732534','（已结束）共享办公工位月租计划，曾支持 20+ 位主理人入驻办公。','admin','2026-08-12 09:40:40','',NULL FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM book WHERE isbn='9787505732534');
INSERT INTO `book` (book_id, book_name, author, book_type, publisher, price, publish_date, stock, status, isbn, intro, create_by, create_time, update_by, update_time)
SELECT 4,'AI 短视频代运营服务','陈晓工作室','1','清远融媒体中心',0.00,'2026-07-20',50,'0','9787506365437','免费体验：AI 辅助短视频策划、拍摄、剪辑全链路代运营，本地商家优先。','admin','2026-08-12 11:08:22','',NULL FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM book WHERE isbn='9787506365437');
INSERT INTO `book` (book_id, book_name, author, book_type, publisher, price, publish_date, stock, status, isbn, intro, create_by, create_time, update_by, update_time)
SELECT 6,'一人公司法律咨询包','李律师团队','1','广东观澜律师事务所',299.00,'2026-07-10',8,'0','9787544253994','面向一人公司与小微团队：股权架构、合同审查、合规咨询一站式服务包。','admin','2026-08-12 17:14:21','',NULL FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM book WHERE isbn='9787544253994');
INSERT INTO `book` (book_id, book_name, author, book_type, publisher, price, publish_date, stock, status, isbn, intro, create_by, create_time, update_by, update_time)
SELECT 7,'数字游民共居空间','清远青年社区','3','碧桂园清远',800.00,'2026-07-25',2,'0','9787020029532','共居 + 共创：按月租入住共居空间，含共享工位与社区活动，长住优惠。','admin','2026-08-12 17:14:21','',NULL FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM book WHERE isbn='9787020029532');
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `reader` (
  `reader_id` bigint NOT NULL AUTO_INCREMENT COMMENT '成员ID',
  `reader_name` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '成员姓名',
  `phone` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '手机号码',
  `email` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '电子邮箱（新成员登记必填，用于邮件通知）',
  `card_no` varchar(30) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '成员证号',
  `reader_type` varchar(10) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '成员类型',
  `sex` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '性别(0男 1女 2未知)',
  `birth_date` date DEFAULT NULL COMMENT '出生日期',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态(0正常 1停用)',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`reader_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='成员信息表';
/*!40101 SET character_set_client = @saved_cs_client */;
-- 测试成员（名字即用途说明：个人主理人/团队/企业 各类型，对应差异化报名规则、停用与邮件通知演示；幂等按成员ID判重）
INSERT INTO `reader` (reader_id, reader_name, phone, email, card_no, reader_type, sex, status, remark, create_by, create_time)
SELECT 1, '周舟', '13800000001', 'stu_test@qq.com', 'JS20260001', '1', '1', '0', '个人主理人-演示报名与候补', 'admin', NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM reader WHERE reader_id=1);
INSERT INTO `reader` (reader_id, reader_name, phone, email, card_no, reader_type, sex, status, remark, create_by, create_time)
SELECT 2, '李想', '13800000002', 'tea_test@qq.com', 'JS20260002', '2', '0', '0', '团队-演示长借期报名', 'admin', NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM reader WHERE reader_id=2);
INSERT INTO `reader` (reader_id, reader_name, phone, email, card_no, reader_type, sex, status, remark, create_by, create_time)
SELECT 3, '王梅', '13800000003', 'gen_test@qq.com', 'JS20260003', '3', '0', '0', '企业-演示报名三态', 'admin', NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM reader WHERE reader_id=3);
INSERT INTO `reader` (reader_id, reader_name, phone, email, card_no, reader_type, sex, status, remark, create_by, create_time)
SELECT 4, '吴挂', '13800000004', 'gua_test@qq.com', 'JS20260004', '1', '0', '1', '停用成员-演示前台登录被拒', 'admin', NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM reader WHERE reader_id=4);
INSERT INTO `reader` (reader_id, reader_name, phone, email, card_no, reader_type, sex, status, remark, create_by, create_time)
SELECT 5, 'Jerry', '12345678901', 'jerry@qq.com', 'DK', '2', '0', '0', '项目作者账号', 'admin', NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM reader WHERE reader_id=5);
INSERT INTO `reader` (reader_id, reader_name, phone, email, card_no, reader_type, sex, status, remark, create_by, create_time)
SELECT 6, '赵一', '13800008888', 'zhengti@qq.com', 'JS20260005', '1', '1', '0', '演示前台注册/修改资料/看板统计全链路', 'admin', NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM reader WHERE reader_id=6);
INSERT INTO `reader` (reader_id, reader_name, phone, email, card_no, reader_type, sex, status, remark, create_by, create_time)
SELECT 7, '钱枫', '13877776666', 'mail_test@qq.com', 'JS20260006', '3', '2', '0', '演示邮件通知（报名/候补/申请结果）', 'admin', NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM reader WHERE reader_id=7);
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `borrow_record` (
  `borrow_id` bigint NOT NULL AUTO_INCREMENT COMMENT '报名ID',
  `reader_id` bigint DEFAULT NULL COMMENT '成员ID',
  `book_id` bigint DEFAULT NULL COMMENT '服务ID',
  `borrow_date` date DEFAULT NULL COMMENT '报名日期',
  `due_date` date DEFAULT NULL COMMENT '截止日期',
  `return_date` date DEFAULT NULL COMMENT '完成日期',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态(0进行中 1已完成 2已截止)',
  `reader_name` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '成员姓名(快照)',
  `card_no` varchar(30) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '成员证号(快照)',
  `book_name` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '服务名称(快照)',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`borrow_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='报名记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
-- 测试报名记录（覆盖：进行中/即将截止/团队长期限/已完成/已截止 五种状态；幂等按报名ID判重）
INSERT INTO borrow_record (borrow_id, reader_id, book_id, borrow_date, due_date, return_date, status, reader_name, card_no, book_name, create_by, create_time)
SELECT 1, 1, 1, '2026-08-13', '2026-09-12', NULL, '0', '周舟', 'JS20260001', 'AI 一人公司实战营', 'system', NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM borrow_record WHERE borrow_id=1);
INSERT INTO borrow_record (borrow_id, reader_id, book_id, borrow_date, due_date, return_date, status, reader_name, card_no, book_name, create_by, create_time)
SELECT 2, 1, 9, '2026-07-21', '2026-08-20', NULL, '0', '周舟', 'JS20260001', '一人公司财税合规指南', 'system', NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM borrow_record WHERE borrow_id=2);
INSERT INTO borrow_record (borrow_id, reader_id, book_id, borrow_date, due_date, return_date, status, reader_name, card_no, book_name, create_by, create_time)
SELECT 3, 2, 2, '2026-08-13', '2026-10-12', NULL, '0', '李想', 'JS20260002', '提示词工程入门课', 'system', NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM borrow_record WHERE borrow_id=3);
INSERT INTO borrow_record (borrow_id, reader_id, book_id, borrow_date, due_date, return_date, status, reader_name, card_no, book_name, create_by, create_time)
SELECT 4, 3, 4, '2026-07-01', '2026-07-31', '2026-07-20', '1', '王梅', 'JS20260003', 'AI 短视频代运营服务', 'system', NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM borrow_record WHERE borrow_id=4);
INSERT INTO borrow_record (borrow_id, reader_id, book_id, borrow_date, due_date, return_date, status, reader_name, card_no, book_name, create_by, create_time)
SELECT 5, 3, 13, '2026-07-02', '2026-08-01', NULL, '2', '王梅', 'JS20260003', '小红书 AI 运营训练营', 'system', NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM borrow_record WHERE borrow_id=5);
-- ---------- 购书订单表（建表保留，订单模块已停用；全新库不插入演示订单） ----------
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

-- ---------- 服务候补表 ----------
CREATE TABLE IF NOT EXISTS `book_reserve` (
  `reserve_id` bigint NOT NULL AUTO_INCREMENT COMMENT '候补ID',
  `book_id` bigint DEFAULT NULL COMMENT '服务ID',
  `reader_id` bigint DEFAULT NULL COMMENT '成员ID',
  `reader_name` varchar(50) DEFAULT NULL COMMENT '成员姓名(快照)',
  `card_no` varchar(30) DEFAULT NULL COMMENT '成员证号(快照)',
  `book_name` varchar(100) DEFAULT NULL COMMENT '服务名称(快照)',
  `reserve_date` datetime DEFAULT NULL COMMENT '候补时间',
  `status` char(1) DEFAULT '0' COMMENT '状态(0候补中 1有名额 2已完成 3已取消)',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`reserve_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='服务候补表';

-- ---------- 前台轮播图（品牌首屏轮播） ----------
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
-- 注：轮播种子数据已移除——由 data_snapshot.sql（快照）全量提供，避免与快照 REPLACE 合并产生重复行

-- ---------- 公告 → 新闻动态（3 条；正文用纯文本，规避前台 textContent 渲染的富文本降级） ----------
UPDATE sys_dict_data SET dict_label='新闻动态' WHERE dict_type='sys_notice_type' AND dict_value='1';
UPDATE sys_notice SET notice_title='数智游民创新工场正式启幕', notice_type='2',
  notice_content='清远市首个人工智能 OPC（一人公司）生态社区「数智游民创新工场」正式启幕。社区提供 AI 技能课程、共享工位、孵化服务与政策对接，支持每一位"一个人 + AI"的创业者。'
  WHERE notice_id=1;
UPDATE sys_notice SET notice_title='首期 AI 一人公司实战营开放报名', notice_type='1',
  notice_content='首期「AI 一人公司实战营」现已开放报名：AI 工具矩阵、获客、交付全流程实操，限额 20 席，报满即止。'
  WHERE notice_id=2;
UPDATE sys_notice SET notice_title='社区共创空间开放预约', notice_type='1',
  notice_content='路演厅、直播间、洽谈室等共创空间已开放预约。名额有限，可先预约排队，释放名额后自动通知。'
  WHERE notice_id=3;

-- ---------- 菜单：官网运营目录（必须先于所有引用它的子菜单插入，否则 parent_id 为 NULL 导致登录菜单树 NPE） ----------
-- 双操作幂等：UPDATE 旧名→新名（存量库兜底，无匹配行则无害）+ INSERT 新名（全新库）
UPDATE sys_menu SET menu_name='官网运营' WHERE menu_name='图书业务';
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '官网运营',0,1,'business','',1,0,'M','0','0','','book','admin',NOW(),'官网运营模块' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='官网运营');

-- 官网轮播菜单与权限点
UPDATE sys_menu SET menu_name='官网轮播' WHERE menu_name='轮播图管理';
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '官网轮播',(SELECT menu_id FROM sys_menu WHERE menu_name='官网运营'),8,'banner','system/banner/index',1,0,'C','0','0','system:banner:list','picture','admin',NOW(),'官网轮播图管理菜单' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='官网轮播');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '轮播图查询',(SELECT menu_id FROM sys_menu WHERE menu_name='官网轮播'),1,'','',1,0,'F','0','0','system:banner:query','#','admin',NOW(),'轮播图查询' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='轮播图查询');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '轮播图新增',(SELECT menu_id FROM sys_menu WHERE menu_name='官网轮播'),2,'','',1,0,'F','0','0','system:banner:add','#','admin',NOW(),'轮播图新增' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='轮播图新增');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '轮播图修改',(SELECT menu_id FROM sys_menu WHERE menu_name='官网轮播'),3,'','',1,0,'F','0','0','system:banner:edit','#','admin',NOW(),'轮播图修改' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='轮播图修改');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '轮播图删除',(SELECT menu_id FROM sys_menu WHERE menu_name='官网轮播'),4,'','',1,0,'F','0','0','system:banner:remove','#','admin',NOW(),'轮播图删除' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='轮播图删除');

-- ---------- 续期次数字段与参数 ----------
SET @rc = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='borrow_record' AND column_name='renew_count');
SET @rs = IF(@rc=0, 'ALTER TABLE borrow_record ADD COLUMN renew_count int DEFAULT 0 COMMENT ''已续期次数''', 'SELECT 1');
PREPARE rst FROM @rs; EXECUTE rst; DEALLOCATE PREPARE rst;
UPDATE sys_config SET config_name='报名续期次数上限', remark='每条服务最多可续期次数' WHERE config_key='book.borrow.renewLimit';
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '报名续期次数上限','book.borrow.renewLimit','1','Y','admin',NOW(),'每条服务最多可续期次数' WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key='book.borrow.renewLimit');

-- BBCODE 演示：小红书 AI 运营训练营简介展示富文本效果
UPDATE book SET intro = '[b]AI 时代的内容运营课[/b]：爆款选题、AI 图文生成、笔记优化、涨粉变现全链路。
[quote]内容力 = AI 提效 × 真实人设[/quote]
[color=#c65d43]往期学员 200+[/color]，[url=#]查看学员案例 →[/url]' WHERE book_name='小红书 AI 运营训练营';

-- 活动预约菜单
UPDATE sys_menu SET menu_name='活动预约' WHERE menu_name='预约管理';
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '活动预约',(SELECT menu_id FROM sys_menu WHERE menu_name='官网运营'),7,'reserve','system/reserve/index',1,0,'C','0','0','system:borrow:list','date','admin',NOW(),'活动预约管理菜单' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='活动预约');

-- 候补参数与定时任务
UPDATE sys_config SET config_name='候补名额保留天数', remark='有名额状态超过该天数未确认自动取消' WHERE config_key='book.reserve.expireDays';
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '候补名额保留天数','book.reserve.expireDays','2','Y','admin',NOW(),'有名额状态超过该天数未确认自动取消' WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key='book.reserve.expireDays');
UPDATE sys_job SET job_name='候补超时检查' WHERE job_name='预约超时检查';
INSERT INTO sys_job (job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, remark)
SELECT '候补超时检查','SYSTEM','borrowTask.reserveExpireCheck()','0 0 8 * * ?','3','1','0','admin',NOW(),'每天8点自动取消超时未确认的候补名额并通知下一位' WHERE NOT EXISTS (SELECT 1 FROM sys_job WHERE job_name='候补超时检查');

-- 候补演示数据（《社区共创空间预约》完整状态链：有名额/候补中/已完成/已取消，打开"我的候补"即见；幂等按成员+服务判重）
INSERT INTO book_reserve (book_id, reader_id, reader_name, card_no, book_name, reserve_date, status, create_by, create_time)
SELECT 15, 2, '李想', 'JS20260002', '社区共创空间预约', DATE_SUB(NOW(), INTERVAL 2 DAY), '1', '李想', NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM book_reserve WHERE book_id=15 AND reader_id=2);
INSERT INTO book_reserve (book_id, reader_id, reader_name, card_no, book_name, reserve_date, status, create_by, create_time)
SELECT 15, 1, '周舟', 'JS20260001', '社区共创空间预约', DATE_SUB(NOW(), INTERVAL 1 DAY), '0', '周舟', NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM book_reserve WHERE book_id=15 AND reader_id=1);
INSERT INTO book_reserve (book_id, reader_id, reader_name, card_no, book_name, reserve_date, status, create_by, create_time)
SELECT 15, 3, '王梅', 'JS20260003', '社区共创空间预约', DATE_SUB(NOW(), INTERVAL 5 DAY), '2', '王梅', NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM book_reserve WHERE book_id=15 AND reader_id=3);
INSERT INTO book_reserve (book_id, reader_id, reader_name, card_no, book_name, reserve_date, status, create_by, create_time)
SELECT 15, 5, 'Jerry', 'DK', '社区共创空间预约', DATE_SUB(NOW(), INTERVAL 7 DAY), '3', 'Jerry', NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM book_reserve WHERE book_id=15 AND reader_id=5);
-- 社区共创空间预约初始剩余名额置 0（满员可候补）：下方种子 INSERT 已直接写 stock=0，此处无需重复 UPDATE

-- 停用成员历史报名（已完成，演示资料保留；幂等按成员+服务+报名日期判重）
INSERT INTO borrow_record (reader_id, book_id, borrow_date, due_date, return_date, status, reader_name, card_no, book_name, create_by, create_time)
SELECT 4, 18, '2026-07-10', '2026-08-09', '2026-07-25', '1', '吴挂', 'JS20260004', 'AI 绘画与设计基础', 'system', NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM borrow_record WHERE reader_id=4 AND book_id=18 AND borrow_date='2026-07-10');

-- ============================================
-- 数智游民创新工场 业务初始化 SQL（幂等，可重复执行）
-- 使用：导入 ry_20260417.sql + quartz.sql 后，再导入本文件
-- 包含：业务表(book/reader/borrow_record) + 字典 + 业务菜单
-- ============================================

-- ---------- 字典：服务分类 ----------
UPDATE sys_dict_type SET dict_name='服务分类', remark='服务分类' WHERE dict_type='book_type';
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '服务分类','book_type','0','admin',NOW(),'服务分类' WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='book_type');
UPDATE sys_dict_data SET dict_label='AI与数字服务', list_class='primary' WHERE dict_type='book_type' AND dict_value='1';
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, list_class, status, create_by, create_time)
SELECT 1,'AI与数字服务','1','book_type','primary','0','admin',NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='book_type' AND dict_value='1');
UPDATE sys_dict_data SET dict_label='创意设计', list_class='success' WHERE dict_type='book_type' AND dict_value='2';
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, list_class, status, create_by, create_time)
SELECT 2,'创意设计','2','book_type','success','0','admin',NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='book_type' AND dict_value='2');
UPDATE sys_dict_data SET dict_label='本地生活与创业', list_class='warning' WHERE dict_type='book_type' AND dict_value='3';
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, list_class, status, create_by, create_time)
SELECT 3,'本地生活与创业','3','book_type','warning','0','admin',NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='book_type' AND dict_value='3');

-- ---------- 字典：成员类型 ----------
UPDATE sys_dict_type SET dict_name='成员类型', remark='成员分类' WHERE dict_type='reader_type';
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '成员类型','reader_type','0','admin',NOW(),'成员分类' WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='reader_type');
UPDATE sys_dict_data SET dict_label='个人主理人', list_class='primary' WHERE dict_type='reader_type' AND dict_value='1';
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, list_class, status, create_by, create_time)
SELECT 1,'个人主理人','1','reader_type','primary','0','admin',NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='reader_type' AND dict_value='1');
UPDATE sys_dict_data SET dict_label='团队', list_class='success' WHERE dict_type='reader_type' AND dict_value='2';
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, list_class, status, create_by, create_time)
SELECT 2,'团队','2','reader_type','success','0','admin',NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='reader_type' AND dict_value='2');
UPDATE sys_dict_data SET dict_label='企业', list_class='warning' WHERE dict_type='reader_type' AND dict_value='3';
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, list_class, status, create_by, create_time)
SELECT 3,'企业','3','reader_type','warning','0','admin',NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='reader_type' AND dict_value='3');

-- 服务信息
UPDATE sys_menu SET menu_name='服务信息' WHERE menu_name='图书信息';
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '服务信息',(SELECT menu_id FROM sys_menu WHERE menu_name='官网运营'),1,'book','system/book/index',1,0,'C','0','0','system:book:list','book','admin',NOW(),'服务信息菜单' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='服务信息');
-- 成员管理
UPDATE sys_menu SET menu_name='成员管理' WHERE menu_name='读者管理';
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '成员管理',(SELECT menu_id FROM sys_menu WHERE menu_name='官网运营'),2,'reader','system/reader/index',1,0,'C','0','0','system:reader:list','peoples','admin',NOW(),'成员管理菜单' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='成员管理');
-- 报名管理
UPDATE sys_menu SET menu_name='报名管理' WHERE menu_name='借阅记录';
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '报名管理',(SELECT menu_id FROM sys_menu WHERE menu_name='官网运营'),4,'borrow','system/borrow/index',1,0,'C','0','0','system:borrow:list','reading','admin',NOW(),'报名管理菜单' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='报名管理');
-- 报名导出（按钮权限点）
UPDATE sys_menu SET menu_name='报名导出' WHERE menu_name='借阅导出';
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '报名导出',(SELECT menu_id FROM sys_menu WHERE menu_name='报名管理'),6,'','',1,0,'F','0','0','system:borrow:export','#','admin',NOW(),'报名记录导出按钮' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='报名导出');

-- ---------- 定时任务：报名截止检查 ----------
UPDATE sys_job SET job_name='报名截止检查' WHERE job_name='逾期检查';
-- 已移除：公告类「报名截止检查」任务（borrowTask.updateOverdueStatus，逾期语义为图书系统遗留，社区官网不需要）

-- ============================================
-- 以下为后续版本补充（幂等）：服务封面/编号/介绍列、购书订单表
-- ============================================

-- ---------- 幂等补列：book 表封面/编号/介绍（老库自动补齐，新库跳过） ----------
SET @c1 = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='book' AND column_name='cover');
SET @s1 = IF(@c1=0, 'ALTER TABLE book ADD COLUMN cover varchar(255) DEFAULT NULL COMMENT ''封面图片''', 'SELECT 1');
PREPARE st1 FROM @s1; EXECUTE st1; DEALLOCATE PREPARE st1;
SET @c2 = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='book' AND column_name='isbn');
SET @s2 = IF(@c2=0, 'ALTER TABLE book ADD COLUMN isbn varchar(20) DEFAULT NULL COMMENT ''服务编号''', 'SELECT 1');
PREPARE st2 FROM @s2; EXECUTE st2; DEALLOCATE PREPARE st2;
SET @c3 = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='book' AND column_name='intro');
SET @s3 = IF(@c3=0, 'ALTER TABLE book ADD COLUMN intro varchar(1000) DEFAULT NULL COMMENT ''服务介绍''', 'SELECT 1');
PREPARE st3 FROM @s3; EXECUTE st3; DEALLOCATE PREPARE st3;

-- ---------- 清理测试残留数据（精确匹配，不影响正式数据） ----------
DELETE FROM book WHERE book_name='哇奥' AND price=576;
DELETE FROM reader WHERE reader_name='前台登记测试';
DELETE FROM reader WHERE reader_name='test2';

-- ============================================
-- 以下为后续版本补充（幂等）：证号唯一索引、名额预警参数、演示服务
-- ============================================

-- ---------- 成员证号唯一索引 ----------
SET @idx = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='reader' AND index_name='uk_card_no');
SET @sql_idx = IF(@idx=0, 'ALTER TABLE reader ADD UNIQUE INDEX uk_card_no (card_no)', 'SELECT 1');
PREPARE st_idx FROM @sql_idx; EXECUTE st_idx; DEALLOCATE PREPARE st_idx;

-- ---------- 报名记录快照列（老库自动补齐：成员/服务删除后历史记录仍完整） ----------
SET @bc1 = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='borrow_record' AND column_name='reader_name');
SET @bs1 = IF(@bc1=0, 'ALTER TABLE borrow_record ADD COLUMN reader_name varchar(50) DEFAULT NULL COMMENT ''成员姓名(快照)''', 'SELECT 1');
PREPARE bst1 FROM @bs1; EXECUTE bst1; DEALLOCATE PREPARE bst1;
SET @bc2 = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='borrow_record' AND column_name='card_no');
SET @bs2 = IF(@bc2=0, 'ALTER TABLE borrow_record ADD COLUMN card_no varchar(30) DEFAULT NULL COMMENT ''成员证号(快照)''', 'SELECT 1');
PREPARE bst2 FROM @bs2; EXECUTE bst2; DEALLOCATE PREPARE bst2;
SET @bc3 = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='borrow_record' AND column_name='book_name');
SET @bs3 = IF(@bc3=0, 'ALTER TABLE borrow_record ADD COLUMN book_name varchar(100) DEFAULT NULL COMMENT ''服务名称(快照)''', 'SELECT 1');
PREPARE bst3 FROM @bs3; EXECUTE bst3; DEALLOCATE PREPARE bst3;

-- ---------- 截止逾期字段（老库自动补齐） ----------
SET @fc1 = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='borrow_record' AND column_name='fine_amount');
SET @fs1 = IF(@fc1=0, 'ALTER TABLE borrow_record ADD COLUMN fine_amount decimal(10,2) DEFAULT 0.00 COMMENT ''截止逾期费用(元)''', 'SELECT 1');
PREPARE fst1 FROM @fs1; EXECUTE fst1; DEALLOCATE PREPARE fst1;
SET @fc2 = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='borrow_record' AND column_name='fine_paid');
SET @fs2 = IF(@fc2=0, 'ALTER TABLE borrow_record ADD COLUMN fine_paid char(1) DEFAULT ''0'' COMMENT ''费用是否已缴(0未缴 1已缴)''', 'SELECT 1');
PREPARE fst2 FROM @fs2; EXECUTE fst2; DEALLOCATE PREPARE fst2;
-- 截止逾期费用参数
UPDATE sys_config SET config_name='截止逾期费用(元/天)', remark='截止逾期后每天产生的费用(元)' WHERE config_key='book.fine.perDay';
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '截止逾期费用(元/天)','book.fine.perDay','0.10','Y','admin',NOW(),'截止逾期后每天产生的费用(元)' WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key='book.fine.perDay');
UPDATE sys_config SET config_name='截止逾期免计天数', remark='截止逾期超过该天数才计费' WHERE config_key='book.fine.graceDays';
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '截止逾期免计天数','book.fine.graceDays','0','Y','admin',NOW(),'截止逾期超过该天数才计费' WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key='book.fine.graceDays');

-- ---------- 名额预警阈值参数 ----------
UPDATE sys_config SET config_name='名额预警阈值', remark='剩余名额低于或等于该值时，前后台显示名额紧张标签' WHERE config_key='book.stock.warn';
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '名额预警阈值','book.stock.warn','3','Y','admin',NOW(),'剩余名额低于或等于该值时，前后台显示名额紧张标签' WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key='book.stock.warn');

-- ---------- 演示服务扩充（幂等：按服务编号判重；与升级脚本 upgrade_20260821_official.sql 第 6 节数据一致） ----------
INSERT INTO book (book_name, author, book_type, publisher, price, publish_date, stock, status, isbn, intro, create_by, create_time)
SELECT 'AI 数字分身工作坊','刘洋','1','腾讯云',149.00,'2026-08-19',30,'0','9787530216781','新服务：手把手搭建你的 AI 数字分身——形象定制、语音克隆、直播带货实操。','admin',NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM book WHERE isbn='9787530216781');
INSERT INTO book (book_name, author, book_type, publisher, price, publish_date, stock, status, isbn, intro, create_by, create_time)
SELECT '一人公司财税合规指南','王会计工作室','3','清远税务学会',0.00,'2026-06-28',25,'0','9787020002207','个体户注册、小规模纳税申报、发票管理全流程指南，财税小白也能听得懂。','admin',NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM book WHERE isbn='9787020002207');
INSERT INTO book (book_name, author, book_type, publisher, price, publish_date, stock, status, isbn, intro, create_by, create_time)
SELECT 'AI 写作与内容创作营','张敏','1','知乎',129.00,'2026-07-08',28,'0','9787020008735','公众号 / 知乎 / 小红书多平台 AI 内容生产实战：选题、提纲、成稿、分发。','admin',NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM book WHERE isbn='9787020008735');
INSERT INTO book (book_name, author, book_type, publisher, price, publish_date, stock, status, isbn, intro, create_by, create_time)
SELECT '零代码 AI 应用搭建','赵宇','1','飞书',169.00,'2026-07-12',26,'0','9787020008728','不写代码也能搭 AI 应用：表单、知识库、自动化工作流的零代码方案。','admin',NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM book WHERE isbn='9787020008728');
INSERT INTO book (book_name, author, book_type, publisher, price, publish_date, stock, status, isbn, intro, create_by, create_time)
SELECT '数字游民保险方案咨询','平安保险清远','3','中国平安',0.00,'2026-07-05',24,'0','9787020008759','灵活就业社保、补充商业险、意外险——数字游民的保障方案一对一咨询。','admin',NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM book WHERE isbn='9787020008759');
INSERT INTO book (book_name, author, book_type, publisher, price, publish_date, stock, status, isbn, intro, create_by, create_time)
SELECT '小红书 AI 运营训练营','孙悦','1','小红书',88.00,'2026-06-20',40,'0','9787020042494','[b]AI 时代的内容运营课[/b]：爆款选题、AI 图文生成、笔记优化、涨粉变现全链路。
[quote]内容力 = AI 提效 × 真实人设[/quote]
[color=#c65d43]往期学员 200+[/color]，[url=#]查看学员案例 →[/url]','admin',NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM book WHERE isbn='9787020042494');
INSERT INTO book (book_name, author, book_type, publisher, price, publish_date, stock, status, isbn, intro, create_by, create_time)
SELECT '视频号 AI 剪辑速成','周涛','1','微信视频号',0.00,'2026-07-18',35,'0','9787532748662','剪映 + AI 工具快速出片：从素材管理到成片发布，一天学会日更节奏。','admin',NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM book WHERE isbn='9787532748662');
INSERT INTO book (book_name, author, book_type, publisher, price, publish_date, stock, status, isbn, intro, create_by, create_time)
SELECT '社区共创空间预约','数智游民创新工场','3','清远图书馆',0.00,'2026-06-15',0,'0','9787544270878','（满员可候补）社区共创空间按场次预约：路演厅、直播间、洽谈室。','admin',NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM book WHERE isbn='9787544270878');
INSERT INTO book (book_name, author, book_type, publisher, price, publish_date, stock, status, isbn, intro, create_by, create_time)
SELECT 'AI 心理陪伴体验','心光工作室','1','广东工业大学清远校区',0.00,'2026-08-18',0,'0','9787544270879','新服务：AI 心理陪伴对话体验 + 真人倾听服务，关注数字游民的心理健康。','admin',NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM book WHERE isbn='9787544270879');
INSERT INTO book (book_name, author, book_type, publisher, price, publish_date, stock, status, isbn, intro, create_by, create_time)
SELECT '一人公司品牌设计','绘境设计','2','站酷',129.00,'2026-07-22',30,'0','9787020008742','logo / VI / 包装设计一条龙，AI 辅助出稿、设计师精修，适合初创一人公司。','admin',NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM book WHERE isbn='9787020008742');
INSERT INTO book (book_name, author, book_type, publisher, price, publish_date, stock, status, isbn, intro, create_by, create_time)
SELECT 'AI 绘画与设计基础','阿杰','2','花瓣',0.00,'2026-06-18',32,'0','9787537812249','Midjourney / Stable Diffusion 入门：从提示词到商业级出图的工作流。','admin',NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM book WHERE isbn='9787537812249');
INSERT INTO book (book_name, author, book_type, publisher, price, publish_date, stock, status, isbn, intro, create_by, create_time)
SELECT '本地生活探店 AI 写作','清远探店团','3','抖音生活服务',0.00,'2026-07-28',27,'0','9787020009626','本地商家探店内容 AI 化：短视频脚本、点评文案、直播话术模板库。','admin',NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM book WHERE isbn='9787020009626');
INSERT INTO book (book_name, author, book_type, publisher, price, publish_date, stock, status, isbn, intro, create_by, create_time)
SELECT '低代码小程序开发课','小码匠','2','微信开放平台',128.00,'2026-07-30',2,'0','9787121022982','名额紧张：低代码 + AI 辅助开发微信小程序，一人也能接外包项目。','admin',NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM book WHERE isbn='9787121022982');
INSERT INTO book (book_name, author, book_type, publisher, price, publish_date, stock, status, isbn, intro, create_by, create_time)
SELECT 'AI 算法与模型微调','清远 AI 实验室','1','华为云',128.00,'2026-08-05',10,'0','9787111407010','面向中小企业：开源模型私有化部署与微调实战，数据不出域。','admin',NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM book WHERE isbn='9787111407010');
INSERT INTO book (book_name, author, book_type, publisher, price, publish_date, stock, status, isbn, intro, create_by, create_time)
SELECT '清远非遗文创 AI 化','岭南文创社','2','清远市文化馆',0.00,'2026-08-20',20,'0','9787101054033','新服务：清远非遗 IP 数字化共创——AI 文创设计、数字藏品、研学课程。','admin',NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM book WHERE isbn='9787101054033');

-- 分散演示服务上线日期（前台"新服务"角标只显示最近上线的3条，其余为老服务）
-- 注意：必须位于全部服务 INSERT 之后（含上方扩充段），否则 UPDATE 匹配不到刚插入的行
UPDATE book SET create_time='2026-06-15 10:00:00' WHERE book_name IN ('AI 一人公司实战营','提示词工程入门课','共享工位月租计划','AI 短视频代运营服务','一人公司法律咨询包','数字游民共居空间');
UPDATE book SET create_time='2026-07-01 10:00:00' WHERE book_name IN ('一人公司财税合规指南','AI 写作与内容创作营','零代码 AI 应用搭建','数字游民保险方案咨询','小红书 AI 运营训练营','视频号 AI 剪辑速成','社区共创空间预约','一人公司品牌设计','AI 绘画与设计基础','本地生活探店 AI 写作','低代码小程序开发课','AI 算法与模型微调');
-- 新服务角标：近 7 天上线（幂等：只对未设过的时间覆盖一次）
UPDATE book SET create_time=DATE_SUB(NOW(), INTERVAL 2 DAY) WHERE isbn='9787530216781' AND create_time < DATE_SUB(NOW(), INTERVAL 7 DAY);
UPDATE book SET create_time=DATE_SUB(NOW(), INTERVAL 3 DAY) WHERE isbn='9787544270879' AND create_time < DATE_SUB(NOW(), INTERVAL 7 DAY);
UPDATE book SET create_time=DATE_SUB(NOW(), INTERVAL 1 DAY) WHERE isbn='9787101054033' AND create_time < DATE_SUB(NOW(), INTERVAL 7 DAY);

-- ============================================
-- 查询性能索引（幂等）：报名/订单/候补按常用查询条件加索引，数据量增长后避免全表扫描
-- ============================================

-- ---------- 报名记录：按成员查（报名校验/我的报名）、按服务查（完成候补联动/重复报名校验） ----------
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

-- ---------- 服务候补：按证号查（前台"我的候补"）、按服务查（完成后找最早候补） ----------
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
UPDATE sys_menu m JOIN sys_menu p ON p.menu_name='官网运营' SET m.parent_id=p.menu_id
WHERE m.menu_name='活动预约' AND m.parent_id IS NULL;

-- ============================================
-- 入驻/合作申请（前端搜索无结果 → "申请入驻"收集合作意向，后台审核处理）
-- ============================================
CREATE TABLE IF NOT EXISTS `book_purchase_req` (
  `req_id` bigint NOT NULL AUTO_INCREMENT COMMENT '申请ID',
  `book_name` varchar(100) NOT NULL COMMENT '服务名称',
  `author` varchar(60) DEFAULT NULL COMMENT '申请人/团队',
  `email` varchar(50) DEFAULT NULL COMMENT '申请者邮箱（审核结果通知用）',
  `status` char(1) DEFAULT '0' COMMENT '状态(0待审核 1已通过 2已婉拒)',
  `remark` varchar(255) DEFAULT NULL COMMENT '申请人附言（合作意向说明等）',
  `reader_id` bigint DEFAULT NULL COMMENT '申请成员ID（前台登录后提交时关联；匿名历史数据为空）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`req_id`),
  KEY `idx_purchase_book_name` (`book_name`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='服务入驻申请';

-- 入驻申请菜单（官网运营目录下）
UPDATE sys_menu SET menu_name='入驻申请' WHERE menu_name='荐购管理';
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '入驻申请',(SELECT menu_id FROM sys_menu WHERE menu_name='官网运营'),9,'purchase','system/purchase/index',1,0,'C','0','0','system:purchase:list','shopping-cart-full','admin',NOW(),'入驻合作申请管理菜单' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='入驻申请');
-- 入驻申请权限点（按钮）
UPDATE sys_menu SET menu_name='入驻申请查询' WHERE menu_name='荐购查询';
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '入驻申请查询',(SELECT menu_id FROM sys_menu WHERE menu_name='入驻申请'),1,'','',1,0,'F','0','0','system:purchase:query','#','admin',NOW(),'入驻申请查询按钮' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='入驻申请查询');
UPDATE sys_menu SET menu_name='入驻申请处理' WHERE menu_name='荐购处理';
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '入驻申请处理',(SELECT menu_id FROM sys_menu WHERE menu_name='入驻申请'),2,'','',1,0,'F','0','0','system:purchase:edit','#','admin',NOW(),'入驻申请处理按钮(标记已通过/已婉拒)' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='入驻申请处理');
UPDATE sys_menu SET menu_name='入驻申请删除' WHERE menu_name='荐购删除';
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '入驻申请删除',(SELECT menu_id FROM sys_menu WHERE menu_name='入驻申请'),3,'','',1,0,'F','0','0','system:purchase:remove','#','admin',NOW(),'入驻申请删除按钮' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='入驻申请删除');

-- 入驻/合作申请示例（0待审核/1已通过/2已婉拒；按 book_name 判重幂等）
INSERT INTO book_purchase_req (book_name, author, email, status, remark, create_by, create_time)
SELECT 'AI 电商代运营团队招募', '郑浩', 'zhenghao@qq.com', '0', '希望入驻社区并招募 3 人 AI 代运营团队，需要共享工位与政策对接', 'admin', DATE_SUB(NOW(), INTERVAL 1 DAY) FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM book_purchase_req WHERE book_name='AI 电商代运营团队招募');
INSERT INTO book_purchase_req (book_name, author, email, status, remark, create_by, create_time)
SELECT '数字游民签证与出海服务', '陈立', 'chenli@qq.com', '1', '提供数字游民签证咨询与跨境财税服务，寻求社区合作挂牌', 'admin', DATE_SUB(NOW(), INTERVAL 3 DAY) FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM book_purchase_req WHERE book_name='数字游民签证与出海服务');
INSERT INTO book_purchase_req (book_name, author, email, status, remark, create_by, create_time)
SELECT '本地 AI 茶饮品牌', '何雨', 'heyu@qq.com', '2', '计划用 AI 运营一家茶饮店，申请入驻被婉拒（店铺资质待补）', 'admin', DATE_SUB(NOW(), INTERVAL 5 DAY) FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM book_purchase_req WHERE book_name='本地 AI 茶饮品牌');

-- ============================================
-- 菜单重组（幂等）：官网运营 → 3 个二级目录（服务管理/成员服务/合作经营），
-- 便于管理员按业务域整理。角色-菜单绑定（sys_role_menu）按 menu_id 关联，
-- 层级变化自动跟随，无需迁移；角色目录可见性由 role_init.sql 补充
-- ============================================
-- 1) 插入 3 个二级目录（C 类型，挂在官网运营下）
UPDATE sys_menu SET menu_name='服务管理' WHERE menu_name='图书管理';
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '服务管理',(SELECT menu_id FROM sys_menu WHERE menu_name='官网运营'),1,'book-mgmt','',1,0,'M','0','0','','book','admin',NOW(),'服务管理：服务信息/报名管理/官网轮播' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='服务管理');
UPDATE sys_menu SET menu_name='成员服务' WHERE menu_name='读者服务';
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '成员服务',(SELECT menu_id FROM sys_menu WHERE menu_name='官网运营'),2,'reader-mgmt','',1,0,'M','0','0','','peoples','admin',NOW(),'成员服务：成员管理/活动预约' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='成员服务');
UPDATE sys_menu SET menu_name='合作经营' WHERE menu_name='经营管理';
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '合作经营',(SELECT menu_id FROM sys_menu WHERE menu_name='官网运营'),3,'ops','',1,0,'M','0','0','','shopping','admin',NOW(),'合作经营：入驻申请' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='合作经营');

-- 2) 子菜单改挂新目录（按 menu_name 定位；重复执行结果相同，天然幂等）
UPDATE sys_menu SET parent_id=(SELECT menu_id FROM (SELECT menu_id FROM sys_menu WHERE menu_name='服务管理') tmp), order_num=1 WHERE menu_name='服务信息';
UPDATE sys_menu SET parent_id=(SELECT menu_id FROM (SELECT menu_id FROM sys_menu WHERE menu_name='服务管理') tmp), order_num=2 WHERE menu_name='报名管理';
UPDATE sys_menu SET parent_id=(SELECT menu_id FROM (SELECT menu_id FROM sys_menu WHERE menu_name='服务管理') tmp), order_num=3 WHERE menu_name='官网轮播';
UPDATE sys_menu SET parent_id=(SELECT menu_id FROM (SELECT menu_id FROM sys_menu WHERE menu_name='成员服务') tmp), order_num=1 WHERE menu_name='成员管理';
UPDATE sys_menu SET parent_id=(SELECT menu_id FROM (SELECT menu_id FROM sys_menu WHERE menu_name='成员服务') tmp), order_num=2 WHERE menu_name='活动预约';
UPDATE sys_menu SET parent_id=(SELECT menu_id FROM (SELECT menu_id FROM sys_menu WHERE menu_name='合作经营') tmp), order_num=1 WHERE menu_name='入驻申请';

-- ---------- 幂等补索引：shop_order 订单号唯一（README 承诺的唯一约束，老库补齐） ----------
SET @so1 = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='shop_order' AND index_name='uk_order_no');
SET @so2 = IF(@so1=0, 'ALTER TABLE shop_order ADD UNIQUE INDEX uk_order_no (order_no)', 'SELECT 1');
PREPARE so_stmt FROM @so2; EXECUTE so_stmt; DEALLOCATE PREPARE so_stmt;
