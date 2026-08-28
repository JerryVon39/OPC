-- ============================================
-- 升级脚本：首页模块配置 JSON 修复 v20260825（第 2 版）
-- 问题：三条种子 config_json 正文含 ASCII 双引号，JSON_VALID=0 → 前台解析失败，
--       品牌理念/产业生态卡片与联系模块时间线空白。
-- 处理：用 JSON_OBJECT/JSON_ARRAY 构造器重写（正文引号用中文引号，JSON 由 MySQL 生成保证合法）
-- 幂等：UPDATE 按 section_key 定位
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p ry-vue < sql/upgrade_20260825_section_fix2.sql
-- ============================================


UPDATE cms_page_section SET config_json = JSON_OBJECT('cols', 2, 'cards', JSON_ARRAY(
  JSON_OBJECT('icon', '🏢', 'title', 'OPC 新内涵', 'text', '一人公司（One Person Company）不只是法律意义上的企业形态，更是 AI 时代的“生产力引擎”：以最低合规成本注册经营主体，再以 AI 深度协同放大个人产出，实现“单人成军”的轻量化创业。'),
  JSON_OBJECT('icon', '🧑💻', 'title', '什么是“数智游民”', 'text', '数智游民= 一个人 + 一台高性能 AI 电脑即可创业：不依赖场地、不依赖团队规模，只要有创意与 AI 工具，就能承接内容创作、技术开发、创意设计等订单，把想法变成事业。')
))
WHERE section_key = 'home-concept';

UPDATE cms_page_section SET config_json = JSON_OBJECT('cols', 3, 'cards', JSON_ARRAY(
  JSON_OBJECT('icon', '🎬', 'title', 'AI 内容创作', 'text', 'AI 微短剧、超级数字人、AIGC 内容创作——2026 年一季度上新微短剧中 AI 制作占比已超 95%，内容创作进入“一人成军”时代。'),
  JSON_OBJECT('icon', '⚙️', 'title', 'AI 技术应用', 'text', 'AI 获客系统、GEO（生成式引擎优化）、AI 应用开发——以 AI 工具重构获客与交付流程，让个人与小微企业也能用上企业级 AI 能力。'),
  JSON_OBJECT('icon', '🤖', 'title', 'AI 硬件与场景', 'text', 'AI 眼镜、AI 手机、AI 工牌、AI 玩具、AI 陪伴、AI 外骨骼辅助装置，以及具身智能训练场、工业/生活场景训练场——硬件长生态链正在成形。')
))
WHERE section_key = 'home-ecosystem';

UPDATE cms_page_section SET config_json = JSON_OBJECT('items', JSON_ARRAY(
  JSON_OBJECT('date', '2026-04-29', 'title', '运营公司成立', 'desc', '数智游民创新工场（清远）科技有限公司注册成立（注册资本 100 万元）。'),
  JSON_OBJECT('date', '2026-07-11', 'title', '签署合作框架协议', 'desc', '清城区政府与清远市星链科技有限公司签署协议，共建人工智能 OPC 生态社区。'),
  JSON_OBJECT('date', '2026-08-01', 'title', '正式揭牌运营', 'desc', '揭牌当天 6 家企业签约入驻、3 所本地高校共建实践基地——从签约到揭牌仅 21 天，被誉为“清城速度”。')
))
WHERE section_key = 'home-contact';

-- 完成提示：全表 JSON 有效性校验
SELECT section_key, JSON_VALID(config_json) AS valid FROM cms_page_section WHERE config_json IS NOT NULL;
