-- ============================================
-- 升级脚本：区块管理 v3（栏目页内容管理）v20260825
-- 背景：栏目页主体模块化（方案 B）——区块分为两类：
--   1) 槽位区块（template=''，默认）：绑定页面骨架固定位置（hero 副标语），走 CMS_BLOCK_SLOTS 槽位
--   2) 内容区块（template 非空）：运营可增删/排序/编辑，存 config_json，前台渲染器渲染
-- 变更：cms_block / cms_block_history 加 template、config_json 两列
-- 幂等：information_schema 补列（存在则跳过）
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p ry-vue < sql/upgrade_20260825_block_v3.sql
-- ============================================

USE ry-vue;

-- 1. cms_block 补列
SET @col1 = (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'cms_block' AND COLUMN_NAME = 'template');
SET @col2 = (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'cms_block' AND COLUMN_NAME = 'config_json');
SET @sql1 = IF(@col1 = 0, 'ALTER TABLE cms_block ADD COLUMN template varchar(20) NOT NULL DEFAULT '''' COMMENT ''模板(空=槽位区块,非空=内容区块)''', 'SELECT 1');
SET @sql2 = IF(@col2 = 0, 'ALTER TABLE cms_block ADD COLUMN config_json json NULL COMMENT ''内容区块配置(模板化区块使用)''', 'SELECT 1');
PREPARE s1 FROM @sql1; EXECUTE s1; DEALLOCATE PREPARE s1;
PREPARE s2 FROM @sql2; EXECUTE s2; DEALLOCATE PREPARE s2;

-- 2. cms_block_history 补列（历史回滚覆盖模板配置）
SET @col3 = (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'cms_block_history' AND COLUMN_NAME = 'template');
SET @col4 = (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'cms_block_history' AND COLUMN_NAME = 'config_json');
SET @sql3 = IF(@col3 = 0, 'ALTER TABLE cms_block_history ADD COLUMN template varchar(20) DEFAULT NULL', 'SELECT 1');
SET @sql4 = IF(@col4 = 0, 'ALTER TABLE cms_block_history ADD COLUMN config_json json NULL', 'SELECT 1');
PREPARE s3 FROM @sql3; EXECUTE s3; DEALLOCATE PREPARE s3;
PREPARE s4 FROM @sql4; EXECUTE s4; DEALLOCATE PREPARE s4;

-- 完成提示
SELECT COLUMN_NAME, COLUMN_TYPE FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'cms_block' AND COLUMN_NAME IN ('template', 'config_json');
