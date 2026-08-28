-- ============================================
-- 升级脚本：内容引擎化 A-数据迁移 v20260826（首页模块 → cms_block 统一）
-- 背景：cms_page_section（首页模块）与 cms_block（栏目页区块）两套内容表合并为一套，
--       后台统一入口「区块管理」（页面 Tab：首页/走进社区/入驻招商/人才培养/产业生态）。
-- 处理：
--   1. 首页 8 模块迁入 cms_block（block_key = section_key，page_key='home'）
--      - 同名停用区块（home-concept/home-ecosystem）ON DUPLICATE 更新启用
--      - sort 重新编号 1-8（原 sort 有重复）
--   2. 旧 home-* 停用区块（home-intro/home-feature-1/2/3）保持停用（数据保留）
--   3. cms_page_section 数据保留不动（接口兼容，前台已切数据源）
-- 幂等：ON DUPLICATE KEY UPDATE（block_key 唯一索引）
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p ry-vue < sql/upgrade_20260826_engine_merge.sql
-- ============================================


-- 1. 首页模块迁入 cms_block（按 sort 重新编号）
SET @rn := 0;
INSERT INTO cms_block (block_key, page_key, title, template, config_json, sort, visible, version, update_by, update_time)
SELECT section_key, 'home', title, template, config_json, @rn := @rn + 1, '0', 1, 'admin', NOW()
FROM (SELECT section_id, section_key, title, template, config_json, sort
      FROM cms_page_section WHERE page_key = 'home' ORDER BY sort, section_id) t
ON DUPLICATE KEY UPDATE
  page_key = 'home',
  title = VALUES(title),
  template = VALUES(template),
  config_json = VALUES(config_json),
  sort = VALUES(sort),
  visible = '0',
  version = version + 1,
  update_by = 'admin',
  update_time = NOW();

-- 2. 完成提示：首页区块清单
SELECT block_id, block_key, template, sort, visible FROM cms_block
WHERE page_key = 'home' AND template IS NOT NULL AND template != '' ORDER BY sort;
