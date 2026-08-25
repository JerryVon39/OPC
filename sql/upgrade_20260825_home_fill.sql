-- ============================================
-- 升级脚本：首页模块填充 v20260825（充实"太空"模块，素材均来自官网内容库真实信息）
-- 内容：
--   1. 品牌理念：cards 模板新增 steps 支持 → 补"OPC 三步走"步骤条
--   2. 入驻企业区：tags 增加「合作共建方」分组（清城区政府/星链科技/顺拓集团/星拓公司）
-- 幂等：UPDATE 按 section_key 定位
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p ry-vue < sql/upgrade_20260825_home_fill.sql
-- ============================================

USE ry-vue;

-- 1. 品牌理念：2 卡 + 三步走步骤条（真实流程：注册 OPC → 搭建 AI 工具矩阵 → 承接订单变现）
UPDATE cms_page_section SET config_json = JSON_OBJECT('cols', 2, 'cards', JSON_ARRAY(
  JSON_OBJECT('icon', '🏢', 'title', 'OPC 新内涵', 'text', '一人公司（One Person Company）不只是法律意义上的企业形态，更是 AI 时代的"生产力引擎"：以最低合规成本注册经营主体，再以 AI 深度协同放大个人产出，实现"单人成军"的轻量化创业——一个人，就是一家公司。'),
  JSON_OBJECT('icon', '🧑💻', 'title', '什么是"数智游民"', 'text', '不依赖场地、不依赖团队规模，一台高性能 AI 电脑即可创业。内容创作、技术开发、创意设计……只要有创意与 AI 工具，就能承接订单、把想法变成事业。社区提供工位、算力与订单，让游民安心扎根。')
), 'steps', JSON_ARRAY(
  JSON_OBJECT('title', '注册 OPC 一人公司', 'desc', '社区提供注册代办与绿色通道，一人一公司轻松落地'),
  JSON_OBJECT('title', '搭建 AI 工具矩阵', 'desc', '大模型平台 / 70B 一体机 / 算力券，工具链一步到位'),
  JSON_OBJECT('title', '承接订单变现', 'desc', '文创订单 / 创客基金 / 大赛，以订单牵引创业起步')
))
WHERE section_key = 'home-concept';

-- 2. 入驻企业区：新增「合作共建方」分组（真实合作方）
UPDATE cms_page_section SET config_json = JSON_OBJECT('groups', JSON_ARRAY(
  JSON_OBJECT('title', '首批入驻企业', 'tags', JSON_ARRAY('塔链人工智能科技', '南京世东智脑', '正经点赞（清远）媒体科技', '北江人工智能产教融合研究院', '光年制造工作室', '彗星互娱工作室')),
  JSON_OBJECT('title', 'AI + 产业融合', 'tags', JSON_ARRAY('AI + 文旅', 'AI + 制造', 'AI + 现代服务业')),
  JSON_OBJECT('title', '合作共建方', 'tags', JSON_ARRAY('清城区政府', '清远星链科技', '顺拓集团', '星拓公司'))
))
WHERE section_key = 'home-companies';

-- 完成提示
SELECT section_key, JSON_VALID(config_json) AS valid FROM cms_page_section WHERE config_json IS NOT NULL;
