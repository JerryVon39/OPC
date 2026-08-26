-- ============================================
-- 升级脚本：CMS 区块管理 v20260825（第二批 · 区块化前台）
-- 内容：cms_block（前台可编辑文本槽/区块）+ cms_block_history（每区块 20 版历史）
--       + 12 个区块种子（content 留空 = 前台不覆盖静态内容，运营填写后才生效）
--       + 后台菜单（区块管理 C 页 + F 权限点）+ editor 角色关联
-- 适用：存量库（在 upgrade_20260825_cms_enhance.sql 之后执行）；全新库直接执行
-- 幂等：可重复执行；表 CREATE TABLE IF NOT EXISTS，种子/菜单 INSERT...SELECT WHERE NOT EXISTS，
--       序号顺延 UPDATE 带 NOT EXISTS 守卫
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p ry-vue < sql/upgrade_20260825_cms_block.sql
-- ============================================

USE ry-vue;

-- ============================================
-- 1. 区块表 cms_block
-- ============================================
CREATE TABLE IF NOT EXISTS `cms_block` (
  `block_id` bigint NOT NULL AUTO_INCREMENT COMMENT '区块ID',
  `block_key` varchar(50) NOT NULL COMMENT '区块键(唯一，前台槽位映射)',
  `page_key` varchar(30) NOT NULL COMMENT '页面键(home/about/join/talent/industry)',
  `title` varchar(200) DEFAULT NULL COMMENT '标题(文本槽时对应元素)',
  `subtitle` varchar(200) DEFAULT NULL COMMENT '副标题',
  `content` mediumtext COMMENT '内容(文本槽=纯文本；html 槽=白名单过滤后的 HTML)',
  `image` varchar(255) DEFAULT NULL COMMENT '图片(预留，当前槽位未使用)',
  `link` varchar(255) DEFAULT NULL COMMENT '链接(预留)',
  `sort` int DEFAULT '0' COMMENT '排序',
  `visible` char(1) DEFAULT '0' COMMENT '显示(0显示 1隐藏)',
  `version` int DEFAULT '1' COMMENT '当前版本',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`block_id`),
  UNIQUE KEY `uk_cms_block_key` (`block_key`),
  KEY `idx_cms_block_page` (`page_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='CMS 区块（前台可编辑文本槽/首页区块）';

-- ============================================
-- 2. 区块历史表 cms_block_history（每区块最多 20 版，超限删最旧）
-- ============================================
CREATE TABLE IF NOT EXISTS `cms_block_history` (
  `history_id` bigint NOT NULL AUTO_INCREMENT COMMENT '历史ID',
  `block_id` bigint NOT NULL COMMENT '区块ID',
  `version` int NOT NULL COMMENT '版本号',
  `title` varchar(200) DEFAULT NULL,
  `subtitle` varchar(200) DEFAULT NULL,
  `content` mediumtext,
  `image` varchar(255) DEFAULT NULL,
  `link` varchar(255) DEFAULT NULL,
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`history_id`),
  KEY `idx_cms_block_hist` (`block_id`, `version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='CMS 区块历史版本';

-- ============================================
-- 3. 区块种子（12 个；content 留空=前台保持静态内容，运营填写后才覆盖）
-- ============================================
INSERT INTO cms_block (block_key, page_key, title, sort, visible, version, update_by, update_time)
SELECT 'home-intro', 'home', '首页首屏文案', 1, '0', 1, 'admin', NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM cms_block WHERE block_key='home-intro');
INSERT INTO cms_block (block_key, page_key, title, sort, visible, version, update_by, update_time)
SELECT 'home-concept', 'home', '品牌理念', 2, '0', 1, 'admin', NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM cms_block WHERE block_key='home-concept');
INSERT INTO cms_block (block_key, page_key, title, sort, visible, version, update_by, update_time)
SELECT 'home-feature-1', 'home', '三大赋能 · 算力与技术支持', 3, '0', 1, 'admin', NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM cms_block WHERE block_key='home-feature-1');
INSERT INTO cms_block (block_key, page_key, title, sort, visible, version, update_by, update_time)
SELECT 'home-feature-2', 'home', '三大赋能 · 政策与金融服务', 4, '0', 1, 'admin', NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM cms_block WHERE block_key='home-feature-2');
INSERT INTO cms_block (block_key, page_key, title, sort, visible, version, update_by, update_time)
SELECT 'home-feature-3', 'home', '三大赋能 · 订单与市场牵引', 5, '0', 1, 'admin', NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM cms_block WHERE block_key='home-feature-3');
INSERT INTO cms_block (block_key, page_key, title, sort, visible, version, update_by, update_time)
SELECT 'home-ecosystem', 'home', '产业生态', 6, '0', 1, 'admin', NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM cms_block WHERE block_key='home-ecosystem');
INSERT INTO cms_block (block_key, page_key, title, sort, visible, version, update_by, update_time)
SELECT 'about-hero-sub', 'about', '走进社区 · 首屏副标语', 1, '0', 1, 'admin', NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM cms_block WHERE block_key='about-hero-sub');
INSERT INTO cms_block (block_key, page_key, title, sort, visible, version, update_by, update_time)
SELECT 'about-cta', 'about', '走进社区 · 结尾引导语', 2, '0', 1, 'admin', NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM cms_block WHERE block_key='about-cta');
INSERT INTO cms_block (block_key, page_key, title, sort, visible, version, update_by, update_time)
SELECT 'join-hero-sub', 'join', '入驻招商 · 首屏副标语', 1, '0', 1, 'admin', NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM cms_block WHERE block_key='join-hero-sub');
INSERT INTO cms_block (block_key, page_key, title, sort, visible, version, update_by, update_time)
SELECT 'talent-hero-sub', 'talent', '人才培养 · 首屏副标语', 1, '0', 1, 'admin', NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM cms_block WHERE block_key='talent-hero-sub');
INSERT INTO cms_block (block_key, page_key, title, sort, visible, version, update_by, update_time)
SELECT 'talent-cta', 'talent', '人才培养 · 结尾引导语', 2, '0', 1, 'admin', NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM cms_block WHERE block_key='talent-cta');
INSERT INTO cms_block (block_key, page_key, title, sort, visible, version, update_by, update_time)
SELECT 'industry-hero-sub', 'industry', '产业生态 · 首屏副标语', 1, '0', 1, 'admin', NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM cms_block WHERE block_key='industry-hero-sub');
INSERT INTO cms_block (block_key, page_key, title, sort, visible, version, update_by, update_time)
SELECT 'industry-cta', 'industry', '产业生态 · 结尾引导语', 2, '0', 1, 'admin', NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM cms_block WHERE block_key='industry-cta');

-- ============================================
-- 4. 后台菜单：区块管理（C 页，挂「内容运营」order_num=3，其后菜单顺延）
-- ============================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '内容运营', 0, 1, 'content', '', 1, 0, 'M', '0', '0', '', 'content', 'admin', NOW(), '前台内容管理'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='内容运营' AND menu_type='M');
SELECT menu_id INTO @content FROM sys_menu WHERE menu_name='内容运营' AND menu_type='M' LIMIT 1;

-- 官网轮播(3)/文章管理(4)/通知公告(5) 顺延一位，为「区块管理」腾出 order_num=3
-- 幂等守卫：区块管理已存在则不再顺延（守卫用会话变量，规避 MySQL 1093 目标表子查询限制）
SET @block_menu = (SELECT COUNT(*) FROM sys_menu WHERE menu_name='区块管理');
UPDATE sys_menu SET order_num = order_num + 1
WHERE parent_id = @content AND order_num >= 3 AND @block_menu = 0;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '区块管理', @content, 3, 'block', 'system/cms/block', 1, 0, 'C', '0', '0', 'system:cmsBlock:list', 'edit', 'admin', NOW(), '前台页面文本槽/区块管理'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='区块管理');
SELECT menu_id INTO @block FROM sys_menu WHERE menu_name='区块管理' LIMIT 1;

-- 权限点（F 按钮）：区块查询/新增/修改/删除
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '区块查询', @block, 1, '', '', 1, 0, 'F', '0', '0', 'system:cmsBlock:query', '#', 'admin', NOW(), '区块查询按钮' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='区块查询');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '区块新增', @block, 2, '', '', 1, 0, 'F', '0', '0', 'system:cmsBlock:add', '#', 'admin', NOW(), '区块新增按钮' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='区块新增');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '区块修改', @block, 3, '', '', 1, 0, 'F', '0', '0', 'system:cmsBlock:edit', '#', 'admin', NOW(), '区块修改按钮(含历史回滚)' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='区块修改');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '区块删除', @block, 4, '', '', 1, 0, 'F', '0', '0', 'system:cmsBlock:remove', '#', 'admin', NOW(), '区块删除按钮(隐藏用开关，删除慎用)' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='区块删除');

-- ============================================
-- 5. 角色关联：内容编辑（editor）授予 区块管理 全套（幂等）
-- ============================================
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id FROM sys_role r, sys_menu m
WHERE r.role_key IN ('editor') AND m.menu_name IN
('区块管理','区块查询','区块新增','区块修改','区块删除')
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id=r.role_id AND rm.menu_id=m.menu_id);

-- ============================================
-- 6. 完成提示
-- ============================================
SELECT CONCAT('区块管理已就绪：区块 ',
              (SELECT COUNT(*) FROM cms_block),
              ' 个，区块管理菜单 ',
              (SELECT COUNT(*) FROM sys_menu WHERE menu_name='区块管理'),
              ' 个') AS result;
