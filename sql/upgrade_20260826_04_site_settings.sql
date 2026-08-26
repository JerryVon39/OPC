-- ============================================
-- 升级脚本：站点级内容进后台 v20260826（导航/页脚后台化）
-- 新增 sys_config 4 键：
--   site.nav           导航菜单 JSON：[{"name":"首页","link":"home.html"},...]
--   site.footer.about  页脚「关于我们」栏（HTML）
--   site.footer.contact 页脚「联系我们」栏（HTML）
--   site.footer.join   页脚「入驻与合作」栏（HTML）
-- 种子值 = 当前静态页面内容（前台渲染失败保留静态，配置为空不覆盖）
-- 幂等：INSERT ... SELECT WHERE NOT EXISTS
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p ry-vue < sql/upgrade_20260826_site_settings.sql
-- ============================================

USE ry-vue;

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '前台导航菜单', 'site.nav',
'[{"name":"首页","link":"home.html"},{"name":"走进社区","link":"about.html"},{"name":"产业生态","link":"industry.html"},{"name":"政策赋能","link":"policy.html"},{"name":"入驻招商","link":"join.html"},{"name":"人才培养","link":"talent.html"},{"name":"新闻动态","link":"news.html"},{"name":"联系我们","link":"home.html#contact"}]',
'Y', 'admin', NOW(), '前台顶部导航（站点设置维护，留空=页面静态导航）'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'site.nav');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '页脚-关于我们', 'site.footer.about',
CONCAT('数智游民创新工场（清远 AI 一人公司生态社区）', CHAR(10), '清远市首个人工智能 OPC 生态社区 ｜ 让"一个人 + AI"成为可能'),
'Y', 'admin', NOW(), '页脚第一栏（站点设置维护，留空=页面静态内容）'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'site.footer.about');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '页脚-联系我们', 'site.footer.contact',
CONCAT('地址：清远国家高新技术产业开发区天安智谷产业园 B6 栋、T1 栋 1105', CHAR(10), '电话：0763-3391888', CHAR(10), '公众号：互动世界 ｜ 视频号：互动AI世界'),
'Y', 'admin', NOW(), '页脚第二栏（站点设置维护，留空=页面静态内容）'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'site.footer.contact');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '页脚-入驻与合作', 'site.footer.join',
CONCAT('欢迎 AI 数字服务 / 创意设计 / 本地生活创业者入驻', CHAR(10), '入驻咨询：0763-3391888'),
'Y', 'admin', NOW(), '页脚第三栏（站点设置维护，留空=页面静态内容）'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'site.footer.join');

SELECT config_key, config_value FROM sys_config WHERE config_key IN ('site.nav', 'site.footer.about', 'site.footer.contact', 'site.footer.join');
