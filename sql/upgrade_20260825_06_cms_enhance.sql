-- ============================================
-- 升级脚本：CMS 文章管理增强 v20260825（第一批 · 文章管理闭环）
-- 内容：cms_article 加列（sort 排序 / attachment 附件 / keywords+description SEO / del_flag+deleted_* 软删回收站）
--       + 后台菜单（栏目管理 C 页 + F 权限点、文章回收站 C 页）+ editor 角色关联
-- 适用：存量库（在 upgrade_20260825_05_editor_fix.sql 之后执行（命名序 _25_06 早于 _26_01））；全新库直接执行
-- 幂等：可重复执行；列按 information_schema 判存补齐，菜单/角色 INSERT...SELECT WHERE NOT EXISTS，
--       序号顺延 UPDATE 带 NOT EXISTS 守卫（存在「栏目管理」时不再顺延）
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p ry-vue < sql/upgrade_20260825_cms_enhance.sql
-- ============================================


-- ============================================
-- 1. cms_article 幂等补列（与 upgrade_20260822_cms.sql 同一 information_schema 模式）
-- ============================================
SET @c1 = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='cms_article' AND column_name='sort');
SET @s1 = IF(@c1=0, 'ALTER TABLE cms_article ADD COLUMN sort int DEFAULT 0 COMMENT ''排序(越小越靠前，置顶之后生效)''', 'SELECT 1');
PREPARE st1 FROM @s1; EXECUTE st1; DEALLOCATE PREPARE st1;

SET @c2 = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='cms_article' AND column_name='attachment');
SET @s2 = IF(@c2=0, 'ALTER TABLE cms_article ADD COLUMN attachment varchar(255) DEFAULT NULL COMMENT ''附件(政策原文PDF等)''', 'SELECT 1');
PREPARE st2 FROM @s2; EXECUTE st2; DEALLOCATE PREPARE st2;

SET @c3 = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='cms_article' AND column_name='keywords');
SET @s3 = IF(@c3=0, 'ALTER TABLE cms_article ADD COLUMN keywords varchar(255) DEFAULT NULL COMMENT ''SEO关键词''', 'SELECT 1');
PREPARE st3 FROM @s3; EXECUTE st3; DEALLOCATE PREPARE st3;

SET @c4 = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='cms_article' AND column_name='description');
SET @s4 = IF(@c4=0, 'ALTER TABLE cms_article ADD COLUMN description varchar(500) DEFAULT NULL COMMENT ''SEO描述''', 'SELECT 1');
PREPARE st4 FROM @s4; EXECUTE st4; DEALLOCATE PREPARE st4;

SET @c5 = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='cms_article' AND column_name='del_flag');
SET @s5 = IF(@c5=0, 'ALTER TABLE cms_article ADD COLUMN del_flag char(1) DEFAULT ''0'' COMMENT ''删除标志(0存在 2已删除，对齐 book 两态软删)''', 'SELECT 1');
PREPARE st5 FROM @s5; EXECUTE st5; DEALLOCATE PREPARE st5;

SET @c6 = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='cms_article' AND column_name='deleted_time');
SET @s6 = IF(@c6=0, 'ALTER TABLE cms_article ADD COLUMN deleted_time datetime DEFAULT NULL COMMENT ''删除时间''', 'SELECT 1');
PREPARE st6 FROM @s6; EXECUTE st6; DEALLOCATE PREPARE st6;

SET @c7 = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='cms_article' AND column_name='deleted_by');
SET @s7 = IF(@c7=0, 'ALTER TABLE cms_article ADD COLUMN deleted_by varchar(64) DEFAULT '''' COMMENT ''删除人''', 'SELECT 1');
PREPARE st7 FROM @s7; EXECUTE st7; DEALLOCATE PREPARE st7;

-- 存量数据：既有文章 del_flag 默认 '0'，无需回写

-- ============================================
-- 2. 后台菜单：栏目管理（C 页，挂「内容运营」）
--    父级目录兜底重建（menu_reorg 已建四分组，此处幂等补建）
-- ============================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '内容运营', 0, 1, 'content', '', 1, 0, 'M', '0', '0', '', 'content', 'admin', NOW(), '前台内容管理'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='内容运营' AND menu_type='M');
SELECT menu_id INTO @content FROM sys_menu WHERE menu_name='内容运营' AND menu_type='M' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '运营辅助', 0, 3, 'ops', '', 1, 0, 'M', '0', '0', '', 'delete', 'admin', NOW(), '误删恢复等辅助功能'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='运营辅助' AND menu_type='M');
SELECT menu_id INTO @ops FROM sys_menu WHERE menu_name='运营辅助' AND menu_type='M' LIMIT 1;

-- 原 官网轮播(2)/文章管理(3)/通知公告(4) 顺延一位，为「栏目管理」腾出 order_num=2
-- 幂等守卫：栏目管理已存在则不再顺延（守卫用会话变量，规避 MySQL 1093 目标表子查询限制）
SET @cat_menu = (SELECT COUNT(*) FROM sys_menu WHERE menu_name='栏目管理');
UPDATE sys_menu SET order_num = order_num + 1
WHERE parent_id = @content AND order_num >= 2 AND @cat_menu = 0;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '栏目管理', @content, 2, 'category', 'system/cms/category', 1, 0, 'C', '0', '0', 'system:cmsCategory:list', 'tree', 'admin', NOW(), 'CMS 栏目管理（分类树）'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='栏目管理');
SELECT menu_id INTO @category FROM sys_menu WHERE menu_name='栏目管理' LIMIT 1;

-- 权限点（F 按钮）：栏目查询/新增/修改/删除
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '栏目查询', @category, 1, '', '', 1, 0, 'F', '0', '0', 'system:cmsCategory:query', '#', 'admin', NOW(), '栏目查询按钮' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='栏目查询');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '栏目新增', @category, 2, '', '', 1, 0, 'F', '0', '0', 'system:cmsCategory:add', '#', 'admin', NOW(), '栏目新增按钮' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='栏目新增');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '栏目修改', @category, 3, '', '', 1, 0, 'F', '0', '0', 'system:cmsCategory:edit', '#', 'admin', NOW(), '栏目修改按钮(含排序/停用)' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='栏目修改');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '栏目删除', @category, 4, '', '', 1, 0, 'F', '0', '0', 'system:cmsCategory:remove', '#', 'admin', NOW(), '栏目删除按钮(有文章/子栏目拒绝)' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='栏目删除');

-- ============================================
-- 3. 后台菜单：文章回收站（C 页，挂「运营辅助」，order_num=3，位于图书/读者回收站之后）
-- ============================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '文章回收站', @ops, 3, 'cms', 'system/recycle/cms', 1, 0, 'C', '0', '0', 'system:cms:remove', 'documentation', 'admin', NOW(), '误删文章恢复（两态）'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='文章回收站');

-- ============================================
-- 4. 角色关联补充：内容编辑（editor）授予 栏目管理 全套 + 文章回收站（幂等：按 role_id+menu_id 判重）
-- ============================================
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id FROM sys_role r, sys_menu m
WHERE r.role_key IN ('editor') AND m.menu_name IN
('栏目管理','栏目查询','栏目新增','栏目修改','栏目删除','文章回收站')
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id=r.role_id AND rm.menu_id=m.menu_id);

-- ============================================
-- 5. 完成提示
-- ============================================
SELECT CONCAT('CMS 增强已就绪：文章表列数 ',
              (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='cms_article'),
              '，栏目管理菜单 ',
              (SELECT COUNT(*) FROM sys_menu WHERE menu_name='栏目管理'),
              ' 个，文章回收站菜单 ',
              (SELECT COUNT(*) FROM sys_menu WHERE menu_name='文章回收站'),
              ' 个') AS result;
