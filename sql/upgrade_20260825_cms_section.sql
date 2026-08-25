-- ============================================
-- 升级脚本：首页模块化搭建（方案 B）v20260825
-- 内容：cms_page_section 表（首页模块配置，模板化）
--       + 8 条种子（与当前 home.html 静态模块一致，兜底双保险）
--       + 后台菜单（页面搭建 C 页 + F 权限点）+ editor/operator 角色授权
-- 模板库（9 种，由前端渲染器实现）：hero/cards/tags/news/timeline/contact/cta/text/banner_text
-- 幂等：表 CREATE TABLE IF NOT EXISTS，种子/菜单 INSERT...SELECT WHERE NOT EXISTS，
--       序号顺延 UPDATE 带会话变量守卫（规避 MySQL 1093）
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p ry-vue < sql/upgrade_20260825_cms_section.sql
-- ============================================

USE ry-vue;

-- ============================================
-- 1. 首页模块表 cms_page_section
-- ============================================
CREATE TABLE IF NOT EXISTS `cms_page_section` (
  `section_id` bigint NOT NULL AUTO_INCREMENT COMMENT '模块ID',
  `page_key` varchar(30) NOT NULL DEFAULT 'home' COMMENT '页面键(当前仅 home)',
  `section_key` varchar(50) NOT NULL COMMENT '模块键(唯一)',
  `template` varchar(30) NOT NULL COMMENT '模板类型(hero/cards/tags/news/timeline/contact/cta/text/banner_text)',
  `title` varchar(200) DEFAULT NULL COMMENT '模块标题(展示用)',
  `config_json` mediumtext COMMENT '模板配置 JSON(卡片/标签/按钮/时间线等)',
  `sort` int DEFAULT '0' COMMENT '排序(越小越靠前，前台按此渲染)',
  `visible` char(1) DEFAULT '0' COMMENT '显示(0显示 1隐藏)',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`section_id`),
  UNIQUE KEY `uk_cms_section_key` (`section_key`),
  KEY `idx_cms_section_page` (`page_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='CMS 首页模块（模板化搭建）';

-- ============================================
-- 2. 种子（8 条，与当前 home.html 静态模块内容一致；config_json 为模板配置）
-- ============================================
INSERT INTO cms_page_section (page_key, section_key, template, title, config_json, sort, visible, update_by, update_time)
SELECT 'home','home-hero','hero','首页首屏（轮播+文案）', NULL, 1, '0', 'admin', NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM cms_page_section WHERE section_key='home-hero');

INSERT INTO cms_page_section (page_key, section_key, template, title, config_json, sort, visible, update_by, update_time)
SELECT 'home','home-concept','cards','品牌理念',
'{"cols":2,"cards":[{"icon":"🏢","title":"OPC 新内涵","text":"一人公司（One Person Company）不只是法律意义上的企业形态，更是 AI 时代的"生产力引擎"：以最低合规成本注册经营主体，再以 AI 深度协同放大个人产出，实现"单人成军"的轻量化创业。"},{"icon":"🧑💻","title":"什么是"数智游民"","text":"数智游民= 一个人 + 一台高性能 AI 电脑即可创业：不依赖场地、不依赖团队规模，只要有创意与 AI 工具，就能承接内容创作、技术开发、创意设计等订单，把想法变成事业。"}]}',
2, '0', 'admin', NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM cms_page_section WHERE section_key='home-concept');

INSERT INTO cms_page_section (page_key, section_key, template, title, config_json, sort, visible, update_by, update_time)
SELECT 'home','home-empower','cards','三大赋能体系',
'{"cols":3,"cards":[{"icon":"💻","title":"算力与技术支持","text":"与阿里巴巴达成深度合作意向，核心 AI 生态体系清城区私有化部署；同步部署智谱等开源大模型开发支撑平台；加速 SD-WAN 网络接入国内外优质大模型；入驻企业可申请试用 70B 以内大模型一体机，并享省级算力券补贴。"},{"icon":"🏦","title":"政策与金融服务","text":"联动金融机构开发创客特色贷款（财政贴息、低息）；数字游民最高 2 万元资金奖励、80 万元担保贷款、3 万元运营激励；市级孵化基地最高 300㎡ 三年免费；发改/工信/人社等多部门一站式政策对接。"},{"icon":"📦","title":"订单与市场牵引","text":"全面梳理文旅推广、政务科普、城市形象等文创订单需求；联动星火深智等 AI 头部企业对接全国稳定内容订单；筹备 2026 首届人工智能 OPC 创客短视频创作大赛；成立创客基金，以订单和赛事吸引产业集聚。"}]}',
3, '0', 'admin', NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM cms_page_section WHERE section_key='home-empower');

INSERT INTO cms_page_section (page_key, section_key, template, title, config_json, sort, visible, update_by, update_time)
SELECT 'home','home-ecosystem','cards','产业生态',
'{"cols":3,"cards":[{"icon":"🎬","title":"AI 内容创作","text":"AI 微短剧、超级数字人、AIGC 内容创作——2026 年一季度上新微短剧中 AI 制作占比已超 95%，内容创作进入"一人成军"时代。"},{"icon":"⚙️","title":"AI 技术应用","text":"AI 获客系统、GEO（生成式引擎优化）、AI 应用开发——以 AI 工具重构获客与交付流程，让个人与小微企业也能用上企业级 AI 能力。"},{"icon":"🤖","title":"AI 硬件与场景","text":"AI 眼镜、AI 手机、AI 工牌、AI 玩具、AI 陪伴、AI 外骨骼辅助装置，以及具身智能训练场、工业/生活场景训练场——硬件长生态链正在成形。"}]}',
4, '0', 'admin', NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM cms_page_section WHERE section_key='home-ecosystem');

INSERT INTO cms_page_section (page_key, section_key, template, title, config_json, sort, visible, update_by, update_time)
SELECT 'home','home-companies','tags','入驻企业 & AI+ 融合',
'{"groups":[{"title":"首批入驻企业","tags":["塔链人工智能科技","南京世东智脑","正经点赞（清远）媒体科技","北江人工智能产教融合研究院","光年制造工作室","彗星互娱工作室"]},{"title":"AI + 产业融合","tags":["AI + 文旅","AI + 制造","AI + 现代服务业"]}]}',
5, '0', 'admin', NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM cms_page_section WHERE section_key='home-companies');

INSERT INTO cms_page_section (page_key, section_key, template, title, config_json, sort, visible, update_by, update_time)
SELECT 'home','home-news','news','新闻动态','{"count":6}', 6, '0', 'admin', NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM cms_page_section WHERE section_key='home-news');

INSERT INTO cms_page_section (page_key, section_key, template, title, config_json, sort, visible, update_by, update_time)
SELECT 'home','home-contact','contact','发展历程 & 联系我们',
'{"items":[{"date":"2026-04-29","title":"运营公司成立","desc":"数智游民创新工场（清远）科技有限公司注册成立（注册资本 100 万元）。"},{"date":"2026-07-11","title":"签署合作框架协议","desc":"清城区政府与清远市星链科技有限公司签署协议，共建人工智能 OPC 生态社区。"},{"date":"2026-08-01","title":"正式揭牌运营","desc":"揭牌当天 6 家企业签约入驻、3 所本地高校共建实践基地——从签约到揭牌仅 21 天，被誉为"清城速度"。"}]}',
7, '0', 'admin', NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM cms_page_section WHERE section_key='home-contact');

INSERT INTO cms_page_section (page_key, section_key, template, title, config_json, sort, visible, update_by, update_time)
SELECT 'home','home-cta','cta','入驻 CTA 横幅',
'{"title":"一个人 + AI，开启你的 OPC 事业","text":"免费工位 · 政策赋能 · 订单对接 ｜ 咨询电话：0763-3391888","btnText":"立即入驻","btnLink":"join.html"}',
8, '0', 'admin', NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM cms_page_section WHERE section_key='home-cta');

-- ============================================
-- 3. 后台菜单：页面搭建（C 页，挂「内容运营」，位于区块管理之后）
-- ============================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '内容运营', 0, 1, 'content', '', 1, 0, 'M', '0', '0', '', 'content', 'admin', NOW(), '前台内容管理'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='内容运营' AND menu_type='M');
SELECT menu_id INTO @content FROM sys_menu WHERE menu_name='内容运营' AND menu_type='M' LIMIT 1;

-- 服务信息(4)/官网轮播(5)/通知公告(6) 顺延一位，为「页面搭建」腾出 order_num=4（幂等守卫）
SET @sec_menu = (SELECT COUNT(*) FROM sys_menu WHERE menu_name='页面搭建');
UPDATE sys_menu SET order_num = order_num + 1
WHERE parent_id = @content AND order_num >= 4 AND @sec_menu = 0;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '页面搭建', @content, 4, 'section', 'system/cms/section', 1, 0, 'C', '0', '0', 'system:cmsSection:list', 'menu', 'admin', NOW(), '首页模块化搭建（增删/排序/模板）'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='页面搭建');
SELECT menu_id INTO @section FROM sys_menu WHERE menu_name='页面搭建' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '搭建查询', @section, 1, '', '', 1, 0, 'F', '0', '0', 'system:cmsSection:query', '#', 'admin', NOW(), '搭建查询按钮' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='搭建查询');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '搭建新增', @section, 2, '', '', 1, 0, 'F', '0', '0', 'system:cmsSection:add', '#', 'admin', NOW(), '搭建新增按钮' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='搭建新增');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '搭建修改', @section, 3, '', '', 1, 0, 'F', '0', '0', 'system:cmsSection:edit', '#', 'admin', NOW(), '搭建修改按钮' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='搭建修改');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '搭建删除', @section, 4, '', '', 1, 0, 'F', '0', '0', 'system:cmsSection:remove', '#', 'admin', NOW(), '搭建删除按钮' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='搭建删除');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '搭建排序', @section, 5, '', '', 1, 0, 'F', '0', '0', 'system:cmsSection:sort', '#', 'admin', NOW(), '模块上移下移' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='搭建排序');

-- ============================================
-- 4. 角色授权：editor + operator 全套「页面搭建」（运营核心能力）
-- ============================================
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id FROM sys_role r, sys_menu m
WHERE r.role_key IN ('editor','operator') AND m.menu_name IN
('页面搭建','搭建查询','搭建新增','搭建修改','搭建删除','搭建排序')
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id=r.role_id AND rm.menu_id=m.menu_id);

-- ============================================
-- 5. 完成提示
-- ============================================
SELECT CONCAT('页面搭建就绪：首页模块 ',
              (SELECT COUNT(*) FROM cms_page_section),
              ' 个，页面搭建菜单 ',
              (SELECT COUNT(*) FROM sys_menu WHERE menu_name='页面搭建'),
              ' 个') AS result;
