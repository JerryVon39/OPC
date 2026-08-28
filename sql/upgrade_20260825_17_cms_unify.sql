-- ============================================
-- 升级脚本：首页内容管理收敛 v20260825（区块管理 → 页面搭建 统一入口）
-- 背景：首页内容此前有双入口双数据源——页面搭建模块 config 与区块管理 home-* 区块
--       都覆盖同一片 DOM（如 home-concept 模块卡片 vs home-concept 区块），互相打架。
-- 处理：
--   1. 首页 6 个区块（home-*）停用（visible='1'，数据保留可恢复，不再向前台输出）
--   2. home-hero 模块配置文案（原渲染器内联默认，现改为配置驱动）
-- 幂等：UPDATE 按 block_key/section_key 定位
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p ry-vue < sql/upgrade_20260825_cms_unify.sql
-- ============================================


-- 1. 停用首页区块（首页内容统一由页面搭建管理；栏目页文本槽不受影响）
UPDATE cms_block SET visible = '1', update_by = 'admin', update_time = NOW()
WHERE block_key IN ('home-intro','home-concept','home-feature-1','home-feature-2','home-feature-3','home-ecosystem');

-- 2. home-hero 模块配置文案（与现首页首屏一致；运营可在页面搭建直接改）
UPDATE cms_page_section SET config_json = JSON_OBJECT(
  'title', '清远市首个人工智能 OPC 生态社区',
  'subtitle', '一个人 + AI，把想法变成事业——免费工位 · 算力加持 · 订单牵引，拎脑入驻清远 AI 生态社区',
  'content', '数智游民创新工场由清城区政府与清远星链科技合作共建，以"国企引领、民企赋能"模式运营，从签约到揭牌仅用 21 天。社区为 AI 时代的超级个体与一人公司提供"拎脑入驻"的完整生态：免费工位与注册代办降低启动门槛，70B 大模型一体机与算力券让 AI 生产力触手可及，文创订单对接与创客基金让作品直接变现，省职教城 14 万人才与高校实践基地持续注入新鲜血液。'
), update_by = 'admin', update_time = NOW()
WHERE section_key = 'home-hero';

-- 完成提示
SELECT CONCAT('首页区块已停用 ',
              (SELECT COUNT(*) FROM cms_block WHERE block_key LIKE 'home-%' AND visible='1'),
              ' 个，栏目页文本槽仍启用 ',
              (SELECT COUNT(*) FROM cms_block WHERE block_key NOT LIKE 'home-%' AND visible='0'),
              ' 个') AS result;
