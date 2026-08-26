-- ============================================
-- 升级脚本：首页内容升级 v20260825（文案丰富化）
-- 内容：cms_block.home-intro 首屏文案升级（subtitle/content 填充，title 留空不覆盖标题）
--       + 品牌理念/三大赋能/产业生态/CTA 四条 config_json 文案润色
-- 幂等：UPDATE 按 key 定位
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p ry-vue < sql/upgrade_20260825_home_polish.sql
-- ============================================

USE ry-vue;

-- 1. 首屏文案（区块 home-intro：副标语 + 简介升级；标题留空保持原样）
UPDATE cms_block SET subtitle = '一个人 + AI，把想法变成事业——免费工位 · 算力加持 · 订单牵引，拎脑入驻清远 AI 生态社区', content = '数智游民创新工场由清城区政府与清远星链科技合作共建，以"国企引领、民企赋能"模式运营，从签约到揭牌仅用 21 天。社区为 AI 时代的超级个体与一人公司提供"拎脑入驻"的完整生态：免费工位与注册代办降低启动门槛，70B 大模型一体机与算力券让 AI 生产力触手可及，文创订单对接与创客基金让作品直接变现，省职教城 14 万人才与高校实践基地持续注入新鲜血液。', update_by = 'admin', update_time = NOW()
WHERE block_key = 'home-intro';

-- 2. 品牌理念（2 卡文案润色）
UPDATE cms_page_section SET config_json = JSON_OBJECT('cols', 2, 'cards', JSON_ARRAY(
  JSON_OBJECT('icon', '🏢', 'title', 'OPC 新内涵', 'text', '一人公司（One Person Company）不只是法律意义上的企业形态，更是 AI 时代的"生产力引擎"：以最低合规成本注册经营主体，再以 AI 深度协同放大个人产出，实现"单人成军"的轻量化创业——一个人，就是一家公司。'),
  JSON_OBJECT('icon', '🧑💻', 'title', '什么是"数智游民"', 'text', '不依赖场地、不依赖团队规模，一台高性能 AI 电脑即可创业。内容创作、技术开发、创意设计……只要有创意与 AI 工具，就能承接订单、把想法变成事业。社区提供工位、算力与订单，让游民安心扎根。')
))
WHERE section_key = 'home-concept';

-- 3. 三大赋能（3 卡文案升级）
UPDATE cms_page_section SET config_json = JSON_OBJECT('cols', 3, 'cards', JSON_ARRAY(
  JSON_OBJECT('icon', '💻', 'title', '算力与技术支持', 'text', '与阿里巴巴深度合作，核心 AI 生态体系清城区私有化部署；智谱等开源大模型开发支撑平台随时调用；SD-WAN 网络直连国内外优质大模型。入驻即可申请试用 70B 以内大模型一体机，并享省级算力券补贴——让每个人的 AI 生产力都用得起。'),
  JSON_OBJECT('icon', '🏦', 'title', '政策与金融服务', 'text', '创客特色贷款财政贴息、数字游民最高 2 万元资金奖励、80 万元担保贷款、3 万元运营激励、市级孵化基地最高 300㎡ 三年免费；发改、工信、人社等多部门一站式政策对接，帮你把每一分政策红利都落进口袋。'),
  JSON_OBJECT('icon', '📦', 'title', '订单与市场牵引', 'text', '全面梳理文旅推广、政务科普、城市形象等文创订单需求，联动星火深智等 AI 头部企业对接全国稳定内容订单；2026 首届 OPC 创客短视频创作大赛与创客基金同步就位，以订单和赛事牵引产业集聚——入驻即有活干。')
))
WHERE section_key = 'home-empower';

-- 4. 产业生态（3 卡文案升级）
UPDATE cms_page_section SET config_json = JSON_OBJECT('cols', 3, 'cards', JSON_ARRAY(
  JSON_OBJECT('icon', '🎬', 'title', 'AI 内容创作', 'text', 'AI 微短剧、超级数字人、AIGC 内容创作——2026 年一季度上新微短剧中 AI 制作占比已超 95%，内容创作全面进入"一人成军"时代。社区提供制作实战营与订单对接，让创作直接变成收入。'),
  JSON_OBJECT('icon', '⚙️', 'title', 'AI 技术应用', 'text', 'AI 获客系统、GEO（生成式引擎优化）、AI 应用开发——以 AI 工具重构获客与交付流程，让个人与小微企业也能用上企业级 AI 能力。技术赋能 + 场景落地，AI 不再是概念而是生产力。'),
  JSON_OBJECT('icon', '🤖', 'title', 'AI 硬件与场景', 'text', 'AI 眼镜、AI 手机、AI 工牌、AI 玩具、AI 陪伴、AI 外骨骼辅助装置，以及具身智能训练场、工业/生活场景训练场——从硬件长生态链到真实落地场景，OPC 生态正在向万物智能延伸。')
))
WHERE section_key = 'home-ecosystem';

-- 5. CTA 横幅文案升级
UPDATE cms_page_section SET config_json = JSON_OBJECT('title', '一个人 + AI，开启你的 OPC 事业', 'text', '免费工位 · 政策赋能 · 订单对接 · 算力加持 ｜ 入驻咨询：0763-3391888', 'btnText', '立即入驻', 'btnLink', 'join.html')
WHERE section_key = 'home-cta';

-- 完成提示
SELECT section_key, JSON_VALID(config_json) AS valid FROM cms_page_section WHERE config_json IS NOT NULL;
