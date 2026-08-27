-- ============================================
-- CMS 功能增强（定时下线 / 日浏览量 / 报表菜单）
-- 1. cms_article 加 offline_time（定时下线：NULL=不限，到点后前台自动隐藏）
-- 2. article_views_daily 日浏览量表（报表趋势数据，随前台访问累加）
-- 3. 「内容统计」报表菜单（挂内容运营下，order_num=7）
-- 幂等：可重复执行
-- ============================================

-- 1. offline_time 补列
SET @c1 = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='cms_article' AND column_name='offline_time');
SET @s1 = IF(@c1=0, 'ALTER TABLE cms_article ADD COLUMN offline_time datetime NULL COMMENT ''定时下线时间（NULL=长期有效，到点前台自动隐藏）''', 'SELECT 1');
PREPARE st1 FROM @s1; EXECUTE st1; DEALLOCATE PREPARE st1;

-- 2. 日浏览量表
CREATE TABLE IF NOT EXISTS article_views_daily (
  article_id bigint NOT NULL COMMENT '文章ID',
  view_date date NOT NULL COMMENT '日期',
  view_count int NOT NULL DEFAULT 0 COMMENT '当日浏览量',
  PRIMARY KEY (article_id, view_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章日浏览量（报表趋势数据）';

-- 3. 内容统计菜单（内容运营目录下，order_num=7；幂等）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '内容统计', (SELECT menu_id FROM sys_menu WHERE menu_name='内容运营' AND menu_type='M' LIMIT 1), 7, 'report', 'system/cms/report', 1, 0, 'C', '0', '0', 'system:cms:list', 'chart', 'admin', NOW(), '文章浏览量统计报表'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='内容统计' AND menu_type='C');
