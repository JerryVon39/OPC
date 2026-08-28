-- ============================================
-- 升级脚本：轮播图样式增强 v20260826
-- 变更：sys_banner 加 bg_color（背景纯色/渐变 CSS，留空=默认深空渐变）
--       与 text_color（文字颜色，默认 #ffffff）
-- 幂等：information_schema 补列
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p ry-vue < sql/upgrade_20260826_banner_style.sql
-- ============================================


SET @c1 = (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_banner' AND COLUMN_NAME = 'bg_color');
SET @s1 = IF(@c1 = 0, 'ALTER TABLE sys_banner ADD COLUMN bg_color varchar(100) DEFAULT NULL COMMENT ''背景样式(纯色/渐变CSS，留空=默认深空渐变)''', 'SELECT 1');
PREPARE st1 FROM @s1; EXECUTE st1; DEALLOCATE PREPARE st1;

SET @c2 = (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_banner' AND COLUMN_NAME = 'text_color');
SET @s2 = IF(@c2 = 0, 'ALTER TABLE sys_banner ADD COLUMN text_color varchar(20) DEFAULT ''#ffffff'' COMMENT ''文字颜色''', 'SELECT 1');
PREPARE st2 FROM @s2; EXECUTE st2; DEALLOCATE PREPARE st2;
