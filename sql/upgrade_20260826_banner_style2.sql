-- ============================================
-- 升级脚本：轮播图样式增强二 v20260826
-- 变更：sys_banner 加 image_fit（图片适配 cover/contain，默认 cover）
--       与 text_bg（文字底色 CSS，''=无底色）
-- 幂等：information_schema 补列
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p ry-vue < sql/upgrade_20260826_banner_style2.sql
-- ============================================

USE ry-vue;

SET @c1 = (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_banner' AND COLUMN_NAME = 'image_fit');
SET @s1 = IF(@c1 = 0, 'ALTER TABLE sys_banner ADD COLUMN image_fit varchar(10) DEFAULT ''cover'' COMMENT ''图片适配(cover铺满裁切/contain完整显示)''', 'SELECT 1');
PREPARE st1 FROM @s1; EXECUTE st1; DEALLOCATE PREPARE st1;

SET @c2 = (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_banner' AND COLUMN_NAME = 'text_bg');
SET @s2 = IF(@c2 = 0, 'ALTER TABLE sys_banner ADD COLUMN text_bg varchar(50) DEFAULT ''rgba(0,0,0,0.30)'' COMMENT ''文字底色CSS(空=无底色)''', 'SELECT 1');
PREPARE st2 FROM @s2; EXECUTE st2; DEALLOCATE PREPARE st2;
