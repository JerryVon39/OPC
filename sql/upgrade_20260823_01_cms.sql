-- ============================================
-- 升级脚本：CMS 文章管理模块 · 收敛/补全 v20260823
-- 数智游民创新工场"文章管理插件"：栏目表 cms_category + 文章表 cms_article
--       + 后台菜单（CMS 管理/文章管理 + 权限点）+ 角色关联（librarian）+ 演示文章
--
-- 说明（本会话历史）：早期草稿脚本曾以"旧演示结构"建过 cms_article（category 字符串栏目、
--       view_count 列、状态仅 0/1），本脚本自动识别并清理旧结构后按最终设计重建，
--       与 sql/upgrade_20260822_cms.sql 收敛为同一终态；两者可任意顺序、任意次数重复执行。
--
-- 幂等：可重复执行；表 CREATE TABLE IF NOT EXISTS，种子/菜单/角色均 INSERT...SELECT WHERE NOT EXISTS，
--       清理段 DELETE 天然幂等。
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p ry-vue < sql/upgrade_20260823_cms.sql
-- ============================================

USE ry-vue;

-- ============================================
-- 0. 清理旧演示结构（幂等）
--   0.1 旧版 cms_article（存在 view_count 列即判定为旧结构，仅含本会话演示种子，直接重建）
-- ============================================
SET @has_old = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='cms_article' AND column_name='view_count');
SET @drop_old = IF(@has_old=1, 'DROP TABLE IF EXISTS cms_article', 'SELECT 1');
PREPARE st_drop FROM @drop_old; EXECUTE st_drop; DEALLOCATE PREPARE st_drop;

-- 0.2 清理旧版"文章栏目"字典（最终设计改为 cms_category 表，字典不再使用）
DELETE FROM sys_dict_data WHERE dict_type='cms_category';
DELETE FROM sys_dict_type WHERE dict_type='cms_category';

-- 0.3 清理旧版菜单（早期草稿：'文章管理' C 菜单 + '文章管理查询/新增/修改/删除/发布' F 权限点，
--     权限前缀 system:article:*；最终设计为 'CMS 管理' 目录 + system:cms:*，此处删除旧结构避免冲突）
--     先删角色关联，再删菜单（含挂在其下可能残留的子权限点）
DELETE FROM sys_role_menu WHERE menu_id IN (
  SELECT mid FROM (
    SELECT m.menu_id AS mid FROM sys_menu m
    WHERE m.menu_name IN ('文章管理','文章管理查询','文章管理新增','文章管理修改','文章管理删除','文章管理发布')
       OR m.parent_id IN (SELECT menu_id FROM sys_menu WHERE menu_name='文章管理')
  ) t
);
DELETE FROM sys_menu WHERE menu_name IN ('文章管理查询','文章管理新增','文章管理修改','文章管理删除','文章管理发布')
   OR parent_id IN (SELECT mid FROM (SELECT menu_id AS mid FROM sys_menu WHERE menu_name='文章管理') t);
DELETE FROM sys_menu WHERE menu_name='文章管理' AND perms='system:article:list';

-- ============================================
-- 1. 栏目表 cms_category
-- ============================================
CREATE TABLE IF NOT EXISTS `cms_category` (
  `category_id` bigint NOT NULL AUTO_INCREMENT COMMENT '栏目ID',
  `category_name` varchar(50) NOT NULL COMMENT '栏目名称',
  `parent_id` bigint DEFAULT '0' COMMENT '父栏目ID',
  `sort` int DEFAULT '0' COMMENT '排序',
  `status` char(1) DEFAULT '0' COMMENT '状态(0启用 1停用)',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='CMS 文章栏目';

-- ============================================
-- 2. 文章表 cms_article
-- ============================================
CREATE TABLE IF NOT EXISTS `cms_article` (
  `article_id` bigint NOT NULL AUTO_INCREMENT COMMENT '文章ID',
  `category_id` bigint DEFAULT NULL COMMENT '栏目ID',
  `title` varchar(200) NOT NULL COMMENT '文章标题',
  `summary` varchar(500) DEFAULT NULL COMMENT '摘要',
  `content` mediumtext COMMENT '正文(BBCODE)',
  `cover` varchar(255) DEFAULT NULL COMMENT '封面图',
  `author` varchar(64) DEFAULT NULL COMMENT '作者',
  `is_top` char(1) DEFAULT '0' COMMENT '置顶(0普通 1置顶)',
  `status` char(1) DEFAULT '0' COMMENT '状态(0已发布 1草稿 2已下线)',
  `views` int DEFAULT '0' COMMENT '浏览量',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`article_id`),
  KEY `idx_cms_category` (`category_id`),
  KEY `idx_cms_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='CMS 文章表';

-- ============================================
-- 3. 栏目种子（幂等：按栏目名判重）
-- ============================================
INSERT INTO cms_category (category_name, parent_id, sort, status, create_by, create_time)
SELECT '新闻动态',0,1,'0','admin',NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM cms_category WHERE category_name='新闻动态');
INSERT INTO cms_category (category_name, parent_id, sort, status, create_by, create_time)
SELECT '政策解读',0,2,'0','admin',NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM cms_category WHERE category_name='政策解读');
INSERT INTO cms_category (category_name, parent_id, sort, status, create_by, create_time)
SELECT '活动报道',0,3,'0','admin',NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM cms_category WHERE category_name='活动报道');
INSERT INTO cms_category (category_name, parent_id, sort, status, create_by, create_time)
SELECT '入驻故事',0,4,'0','admin',NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM cms_category WHERE category_name='入驻故事');

-- ============================================
-- 4. 演示文章（幂等：按标题判重；正文用纯文本，与前台 news.html textContent 渲染一致；
--    内容与官网内容库真实报道同步，可删除或后台继续编辑）
-- ============================================
INSERT INTO cms_article (category_id, title, summary, content, author, is_top, status, views, publish_time, create_by, create_time)
SELECT c.category_id,'清远首个人工智能 OPC 生态社区正式揭牌运营','2026 年 8 月 1 日，清城区人工智能 OPC 生态社区在清远星谷科技园正式揭牌运营，从签约到揭牌仅用 21 天，被誉为"清城速度"。','2026 年 8 月 1 日，清城区人工智能 OPC 生态社区在清远星谷科技园正式揭牌运营。从 7 月 11 日签约到揭牌仅用 21 天，被誉为"清城速度"。揭牌当天，塔链人工智能科技、南京世东智脑、正经点赞（清远）媒体科技等 6 家企业签约入驻，并与广东财贸职业学院、清远职业技术学院、广东岭南职业技术学院 3 所高校共建校外实践教学基地。','数智游民创新工场','1','0',186,DATE_SUB(NOW(), INTERVAL 2 DAY),'admin',NOW()
FROM cms_category c WHERE c.category_name='新闻动态'
AND NOT EXISTS (SELECT 1 FROM cms_article WHERE title='清远首个人工智能 OPC 生态社区正式揭牌运营');
INSERT INTO cms_article (category_id, title, summary, content, author, is_top, status, views, publish_time, create_by, create_time)
SELECT c.category_id,'2026 年一季度上新微短剧中 AI 制作占比超 95%','行业数据显示，2025 年国内 AI 视频市场规模已突破 1200 亿元，AI 微短剧、超级数字人等新业态正成为数字经济的重要增长极。','行业数据显示，2025 年国内 AI 视频市场规模已突破 1200 亿元，预计 2030 年达 5800 亿元；2026 年一季度上新微短剧中，AI 制作占比已超过 95%。AI 微短剧、超级数字人、AIGC 内容创作等新业态正在成为数字经济的重要增长极。','数智游民创新工场','0','0',132,DATE_SUB(NOW(), INTERVAL 6 DAY),'admin',NOW()
FROM cms_category c WHERE c.category_name='新闻动态'
AND NOT EXISTS (SELECT 1 FROM cms_article WHERE title='2026 年一季度上新微短剧中 AI 制作占比超 95%');
INSERT INTO cms_article (category_id, title, summary, content, author, is_top, status, views, publish_time, create_by, create_time)
SELECT c.category_id,'AI 视频市场规模 1200 亿：一人公司主理人的入局机会','2025 年国内 AI 视频市场规模突破 1200 亿元，预计 2030 年达 5800 亿元。对 OPC 主理人而言，AI 内容制作正从"尝鲜"走向"主业"。','对 OPC（一人公司）主理人而言，AI 内容制作正从"尝鲜"走向"主业"：一人 + AI 即可完成剧本生成、视频制作、分发投放的全链路。社区面向主理人开放 AI 微短剧制作实战营、超级数字人实操课等课程，并提供订单对接支持，帮助个人创作者以更低门槛切入 AI 内容赛道。','数智游民创新工场','0','0',98,DATE_SUB(NOW(), INTERVAL 4 DAY),'admin',NOW()
FROM cms_category c WHERE c.category_name='政策解读'
AND NOT EXISTS (SELECT 1 FROM cms_article WHERE title='AI 视频市场规模 1200 亿：一人公司主理人的入局机会');
INSERT INTO cms_article (category_id, title, summary, content, author, is_top, status, views, publish_time, create_by, create_time)
SELECT c.category_id,'首届人工智能 OPC 创客短视频创作大赛筹备启动','社区筹办 2026 首届人工智能 OPC 创客短视频创作大赛，并成立创客基金，联动 AI 头部企业对接全国稳定内容订单。','社区筹办 2026 首届人工智能 OPC 创客短视频创作大赛，并成立创客基金。社区将全面梳理文旅推广、政务科普、城市形象等领域的 AI 文创订单需求，联动星火深智等 AI 头部企业对接全国稳定内容订单，以订单和赛事吸引产业集聚。','数智游民创新工场','0','0',143,DATE_SUB(NOW(), INTERVAL 3 DAY),'admin',NOW()
FROM cms_category c WHERE c.category_name='活动报道'
AND NOT EXISTS (SELECT 1 FROM cms_article WHERE title='首届人工智能 OPC 创客短视频创作大赛筹备启动');
INSERT INTO cms_article (category_id, title, summary, content, author, is_top, status, views, publish_time, create_by, create_time)
SELECT c.category_id,'毕业生 AI 实训营开营：本地高校学生迈出 OPC 创业第一步','面向本地高校毕业生与清远籍大学生的免费 OPC 创业实训营开营，结营学员将对接社区订单与入驻资源。','面向本地高校毕业生与清远籍大学生的免费 OPC 创业实训营正式开营。实训营覆盖一人公司注册、AI 工具矩阵、内容创作与获客实战全流程，结营学员将对接社区订单与入驻资源，迈出 OPC 创业的第一步。','北江人工智能产教融合研究院','0','0',76,DATE_SUB(NOW(), INTERVAL 1 DAY),'admin',NOW()
FROM cms_category c WHERE c.category_name='入驻故事'
AND NOT EXISTS (SELECT 1 FROM cms_article WHERE title='毕业生 AI 实训营开营：本地高校学生迈出 OPC 创业第一步');

-- ============================================
-- 5. 后台菜单：CMS 管理（M 目录，挂"官网运营"下，双操作幂等）
-- ============================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'CMS 管理',(SELECT menu_id FROM sys_menu WHERE menu_name='官网运营'),10,'cms','',1,0,'M','0','0','','documentation','admin',NOW(),'CMS 文章管理插件' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='CMS 管理');

-- 文章管理（C 页面）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '文章管理',(SELECT menu_id FROM sys_menu WHERE menu_name='CMS 管理'),1,'article','system/cms/index',1,0,'C','0','0','system:cms:list','documentation','admin',NOW(),'CMS 文章管理菜单' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='文章管理');

-- 权限点（F 按钮）：文章查询/新增/修改/删除/发布
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '文章查询',(SELECT menu_id FROM sys_menu WHERE menu_name='文章管理'),1,'','',1,0,'F','0','0','system:cms:query','#','admin',NOW(),'文章查询按钮' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='文章查询');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '文章新增',(SELECT menu_id FROM sys_menu WHERE menu_name='文章管理'),2,'','',1,0,'F','0','0','system:cms:add','#','admin',NOW(),'文章新增按钮' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='文章新增');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '文章修改',(SELECT menu_id FROM sys_menu WHERE menu_name='文章管理'),3,'','',1,0,'F','0','0','system:cms:edit','#','admin',NOW(),'文章修改按钮(含置顶/上下线)' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='文章修改');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '文章删除',(SELECT menu_id FROM sys_menu WHERE menu_name='文章管理'),4,'','',1,0,'F','0','0','system:cms:remove','#','admin',NOW(),'文章删除按钮' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='文章删除');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '文章发布',(SELECT menu_id FROM sys_menu WHERE menu_name='文章管理'),5,'','',1,0,'F','0','0','system:cms:publish','#','admin',NOW(),'文章发布按钮(草稿/下线→已发布)' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='文章发布');

-- ============================================
-- 6. 角色关联补充：内容编辑（editor）授予 CMS 全套菜单（幂等：按 role_id+menu_id 判重）
-- ============================================
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id FROM sys_role r, sys_menu m
WHERE r.role_key IN ('editor') AND m.menu_name IN
('CMS 管理','文章管理','文章查询','文章新增','文章修改','文章删除','文章发布')
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id=r.role_id AND rm.menu_id=m.menu_id);
