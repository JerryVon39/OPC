-- ============================================
-- 75 增强：自定义页入口位置可配置（menu_pos）
-- more=「☰ 更多」菜单（默认）/ nav=页面顶部主导航
-- 幂等：information_schema 补列
-- ============================================
SET @c1 = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='cms_page' AND column_name='menu_pos');
SET @s1 = IF(@c1=0, 'ALTER TABLE cms_page ADD COLUMN menu_pos varchar(10) NOT NULL DEFAULT ''more'' COMMENT ''前台入口位置：more=更多菜单 nav=顶部主导航''', 'SELECT 1');
PREPARE st1 FROM @s1; EXECUTE st1; DEALLOCATE PREPARE st1;
