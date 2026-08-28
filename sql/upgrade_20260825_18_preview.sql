-- ============================================
-- 升级脚本：后台实时预览支持 v20260825（页面搭建/区块管理内嵌 iframe）
-- 新增：sys_config.site.front.url —— 前台站点地址（含端口不含路径）
--       页面搭建/区块管理的实时预览 iframe 用它拼前台页面地址
-- 幂等：INSERT ... SELECT WHERE NOT EXISTS
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p ry-vue < sql/upgrade_20260825_preview.sql
-- ============================================


-- 1. 前台站点地址（默认 http://localhost，对齐 docker nginx 80 端口；部署到服务器后改这里或系统参数设置）
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '前台站点地址', 'site.front.url', 'http://localhost', 'Y', 'admin', NOW(), '后台页面搭建/区块管理实时预览 iframe 使用的前台地址（含端口，不含路径）'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'site.front.url');

-- 完成提示
SELECT config_key, config_value FROM sys_config WHERE config_key = 'site.front.url';
