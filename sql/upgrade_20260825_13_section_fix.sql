-- ============================================
-- 升级脚本：首页模块化修复 v20260825
-- 内容：
--   1. 区块种子 title 清空：种子 title 原为后台展示名（如"首页首屏文案"），
--      前台文本槽映射会把该字段覆盖到页面标题元素 → 前台标题被后台展示名污染。
--      修复：13 个种子区块 title 置 NULL（留空 = 前台保持原样；运营填写后才覆盖）。
--   2. 删除页面搭建测试残留模块（section_key 以 sec- 开头，空配置卡片组）
-- 幂等：UPDATE/DELETE 天然幂等（按 block_key/section_key 定位）
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p ry-vue < sql/upgrade_20260825_section_fix.sql
-- ============================================

USE ry-vue;

-- 1. 区块种子 title 清空（仅种子 key；运营后续填写的内容不受影响）
UPDATE cms_block SET title = NULL, update_by = 'admin', update_time = NOW()
WHERE block_key IN (
  'home-intro','home-concept','home-feature-1','home-feature-2','home-feature-3','home-ecosystem',
  'about-hero-sub','about-cta','join-hero-sub','talent-hero-sub','talent-cta','industry-hero-sub','industry-cta'
);

-- 2. 删除页面搭建测试残留（搭建页新建时自动生成 sec- 前缀 key）
DELETE FROM cms_page_section WHERE section_key LIKE 'sec-%';

-- 3. 完成提示
SELECT CONCAT('修复完成：区块种子 title 清空 ',
              (SELECT COUNT(*) FROM cms_block WHERE title IS NULL),
              ' 个，页面模块 ',
              (SELECT COUNT(*) FROM cms_page_section),
              ' 个') AS result;
