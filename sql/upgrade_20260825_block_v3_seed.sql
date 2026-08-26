-- ============================================
-- 升级脚本：区块管理 v3 种子迁移 v20260825（栏目页主体模块化）
-- 内容：4 个栏目页 20 个内容区块（素材 100% 来自现有静态页面 content-section，未编造）
--   1. 内容区块（template 非空，sort 排序，前台渲染器渲染）
--   2. 原 cta 槽位区块（about-cta/talent-cta/industry-cta）停用，由 cta 模板区块承担
-- 幂等：INSERT ... SELECT WHERE NOT EXISTS（按 block_key）
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p ry-vue < sql/upgrade_20260825_block_v3_seed.sql
-- ============================================

USE ry-vue;

-- ==================== about 走进社区 ====================
INSERT INTO cms_block (block_key, page_key, title, template, config_json, sort, visible, version, update_by, update_time)
SELECT 'about-loc', 'about', '📍 社区定位', 'cards',
JSON_OBJECT('cols', 2, 'cards', JSON_ARRAY(
  JSON_OBJECT('icon', '🏢', 'title', 'OPC 新内涵', 'text', '一人公司（One Person Company）不只是法律意义上的"一人公司"载体，更是 AI 时代的"生产力引擎"——以最低合规成本注册经营主体，再以 AI 深度协同放大个人产出。'),
  JSON_OBJECT('icon', '🧑‍💻', 'title', '什么是"数智游民"', 'text', '"数智游民"= 一个人 + 一台高性能 AI 电脑即可创业：不依赖场地、不依赖团队规模，只要有创意与 AI 工具，就能承接内容创作、技术开发、创意设计等订单。')
)), 1, '0', 1, 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM cms_block WHERE block_key = 'about-loc');

INSERT INTO cms_block (block_key, page_key, title, template, config_json, sort, visible, version, update_by, update_time)
SELECT 'about-coop', 'about', '🤝 合作模式："国企引领、民企赋能"', 'cards',
JSON_OBJECT('cols', 3, 'subtitle', '清城区政府 × 清远市星链科技有限公司 合作共建清城区人工智能 OPC 生态社区，双方优势互补、共建共享。', 'cards', JSON_ARRAY(
  JSON_OBJECT('icon', '🏛️', 'title', '政府方 · 区属国企顺拓集团', 'text', '提供场地、硬件、后勤保障与本地资源，为创业者托底基础设施，让创业者拎包入驻、轻装上阵。'),
  JSON_OBJECT('icon', '🚀', 'title', '企业方 · 清远星链科技', 'text', '负责课程搭建、师资组建、订单对接、工位招商，把 AI 课程体系、产业订单与运营能力带进社区。'),
  JSON_OBJECT('icon', '🤝', 'title', '合资运营 · 星拓公司', 'text', '双方合资组建星拓公司负责社区日常运营，"国企引领、民企赋能"，充分发挥国资资源与民企活力的双重优势。')
)), 2, '0', 1, 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM cms_block WHERE block_key = 'about-coop');

INSERT INTO cms_block (block_key, page_key, title, template, config_json, sort, visible, version, update_by, update_time)
SELECT 'about-location', 'about', '🏞️ 区位优势', 'cards',
JSON_OBJECT('cols', 3, 'cards', JSON_ARRAY(
  JSON_OBJECT('icon', '📍', 'title', '中心城区区位', 'text', '清城区是清远市中心城区，城市配套成熟、交通便利，是承接 AI 创业与数字游民落地的主阵地。'),
  JSON_OBJECT('icon', '🎭', 'title', '文旅资源与人才储备', 'text', '丰厚的文旅资源为 AI 内容创作提供天然素材；省职教城约 14 万师生，构建起庞大的人才蓄水池。'),
  JSON_OBJECT('icon', '📊', 'title', '产业场景与内需市场', 'text', '多元产业应用场景与广阔内需市场，为 AI 应用落地与订单对接提供真实场景与稳定需求。')
)), 3, '0', 1, 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM cms_block WHERE block_key = 'about-location');

INSERT INTO cms_block (block_key, page_key, title, template, config_json, sort, visible, version, update_by, update_time)
SELECT 'about-milestones', 'about', '📅 发展历程', 'timeline',
JSON_OBJECT('items', JSON_ARRAY(
  JSON_OBJECT('date', '2026-04-29', 'title', '运营公司成立', 'desc', '数智游民创新工场（清远）科技有限公司注册成立（注册资本 100 万元）。'),
  JSON_OBJECT('date', '2026-07-11', 'title', '签署合作框架协议', 'desc', '清城区政府与清远市星链科技有限公司签署合作框架协议，共建人工智能 OPC 生态社区。'),
  JSON_OBJECT('date', '2026-08-01', 'title', '正式揭牌运营', 'desc', '揭牌仪式在清远星谷科技园举行。从签约到揭牌仅 21 天，跑出"清城速度"。')
)), 4, '0', 1, 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM cms_block WHERE block_key = 'about-milestones');

INSERT INTO cms_block (block_key, page_key, title, template, config_json, sort, visible, version, update_by, update_time)
SELECT 'about-operator', 'about', '🏭 运营主体', 'text',
JSON_OBJECT('text', '<b>数智游民创新工场（清远）科技有限公司</b>：2026-04-29 成立，注册资本 100 万元，负责清城区人工智能 OPC 生态社区的日常运营与入驻招商服务。'),
5, '0', 1, 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM cms_block WHERE block_key = 'about-operator');

INSERT INTO cms_block (block_key, page_key, title, template, config_json, sort, visible, version, update_by, update_time)
SELECT 'about-legal', 'about', '⚖️ OPC 的法律与税务', 'cards',
JSON_OBJECT('cols', 3, 'cards', JSON_ARRAY(
  JSON_OBJECT('icon', '🛡️', 'title', '法律优势', 'text', '<b>有限责任</b>：股东仅以出资额为限对公司债务承担责任，个人房产、存款等家庭财产受法律保护；<b>独立法人地位</b>：商业合作中比个体户更具公信力；<b>决策高效</b>：内部治理结构简单，能快速响应市场。'),
  JSON_OBJECT('icon', '📋', 'title', '法律边界', 'text', '<b>举证责任倒置</b>：一旦涉诉，股东需自己证明公司财产独立于个人财产，否则承担连带责任；<b>一人一公司</b>：一个自然人只能投资设立一家一人有限责任公司；<b>强制审计</b>：每年度终了须编制财务会计报告并依法审计。'),
  JSON_OBJECT('icon', '🧾', 'title', '税务结构', 'text', 'OPC 需缴纳<b>企业所得税</b>（一般 25%，小微企业有优惠）加<b>个人所得税</b>（分红时缴 20%）；相比个体户 5%-35% 超额累进税率存在"双重征税"，但利润做大后综合税负可能更有优势。社区提供注册代办与纳税申报服务。')
)), 6, '0', 1, 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM cms_block WHERE block_key = 'about-legal');

INSERT INTO cms_block (block_key, page_key, title, template, config_json, sort, visible, version, update_by, update_time)
SELECT 'about-cta-banner', 'about', '想成为社区一员？', 'cta',
JSON_OBJECT('text', '免费工位、政策赋能、订单对接，一个人 + AI 即刻启程 ｜ 咨询电话：0763-3391888', 'btnText', '前往入驻招商', 'btnLink', 'join.html'),
7, '0', 1, 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM cms_block WHERE block_key = 'about-cta-banner');

-- ==================== join 入驻招商 ====================
INSERT INTO cms_block (block_key, page_key, title, template, config_json, sort, visible, version, update_by, update_time)
SELECT 'join-ways', 'join', '🏠 两种入驻方式', 'cards',
JSON_OBJECT('cols', 2, 'cards', JSON_ARRAY(
  JSON_OBJECT('icon', '🌟', 'title', 'A 类：免费入驻 · 成为 OPC 合伙人', 'text', '<b>条件：</b>获得社区老板认可<br><b>合伙人权益：</b><ul><li>房租、水电、高速宽带全免</li><li>注册代办、纳税申报服务</li><li>本地 Token 免费使用</li><li>可申请试用 70B 以内大模型一体机</li><li>超高规格办公装修</li><li>多间高规格会议室</li><li>工会和党建公共空间</li></ul>'),
  JSON_OBJECT('icon', '🪑', 'title', 'B 类：付费入驻 · 普通成员', 'text', '<b>适用：</b>暂未获社区认可、或不愿受股权约束的创业者<br><b>费用：</b>支付小额租金，分摊水电网络费<br><b>成员权益：</b><ul><li>共享社区工位与高速网络</li><li>使用公共会议室与公共空间</li><li>参与社区课程、赛事与订单对接</li><li>可随时申请转为 OPC 合伙人</li></ul>')
)), 1, '0', 1, 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM cms_block WHERE block_key = 'join-ways');

INSERT INTO cms_block (block_key, page_key, title, template, config_json, sort, visible, version, update_by, update_time)
SELECT 'join-companies', 'join', '🏢 首批入驻企业（简列）', 'text',
JSON_OBJECT('text', '塔链人工智能科技（广州）有限公司 · AI 技术研发 ｜ 南京世东智脑人工智能有限公司 · AI 技术研发 ｜ 正经点赞（清远）媒体科技有限公司 · 数字内容创作<br>北江人工智能产教融合研究院 · AI 产教融合 ｜ 光年制造工作室 · 创意设计 ｜ 彗星互娱工作室 · 创意文娱设计<br><b>2026-08-01 揭牌当日签约入驻。</b>'),
2, '0', 1, 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM cms_block WHERE block_key = 'join-companies');

INSERT INTO cms_block (block_key, page_key, title, template, config_json, sort, visible, version, update_by, update_time)
SELECT 'join-form', 'join', '📝 入驻申请', 'form', NULL, 3, '0', 1, 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM cms_block WHERE block_key = 'join-form');

INSERT INTO cms_block (block_key, page_key, title, template, config_json, sort, visible, version, update_by, update_time)
SELECT 'join-contact', 'join', '📮 咨询信息', 'list',
JSON_OBJECT('items', JSON_ARRAY(
  JSON_OBJECT('title', '咨询电话', 'desc', '0763-3391888'),
  JSON_OBJECT('title', '社区地址', 'desc', '清远国家高新技术产业开发区天安智谷产业园 B6 栋、T1 栋 1105')
)), 4, '0', 1, 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM cms_block WHERE block_key = 'join-contact');

-- ==================== talent 人才培养 ====================
INSERT INTO cms_block (block_key, page_key, title, template, config_json, sort, visible, version, update_by, update_time)
SELECT 'talent-schools', 'talent', '🎓 高校合作 · 校外实践教学基地', 'cards',
JSON_OBJECT('cols', 3, 'cards', JSON_ARRAY(
  JSON_OBJECT('icon', '🏫', 'title', '广东财贸职业学院', 'text', '共建校外实践教学基地、AI 实训基地，联合培养应用型 AI 人才'),
  JSON_OBJECT('icon', '🏫', 'title', '清远职业技术学院', 'text', '共建校外实践教学基地、AI 实训基地，联合培养应用型 AI 人才'),
  JSON_OBJECT('icon', '🏫', 'title', '广东岭南职业技术学院', 'text', '共建校外实践教学基地、AI 实训基地，联合培养应用型 AI 人才')
)), 1, '0', 1, 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM cms_block WHERE block_key = 'talent-schools');

INSERT INTO cms_block (block_key, page_key, title, template, config_json, sort, visible, version, update_by, update_time)
SELECT 'talent-training', 'talent', '🧑‍🏫 培训体系', 'cards',
JSON_OBJECT('cols', 2, 'cards', JSON_ARRAY(
  JSON_OBJECT('icon', '🎬', 'title', '常态化 AI 技能培训', 'text', 'AI 短视频创作、智能内容生产技能培训常态化开展，零基础也能快速上手。'),
  JSON_OBJECT('icon', '🚀', 'title', 'OPC 创客训练营', 'text', '已举办多期，手把手教你从 0 到 1 开一家"一人公司"。'),
  JSON_OBJECT('icon', '🧑‍🎓', 'title', '高校毕业生专属实训营', 'text', '面向本地高校毕业生，衔接就业与创业，学完即可接单。'),
  JSON_OBJECT('icon', '🌱', 'title', '清远籍大学生免费训练营', 'text', '清远籍大学生可免费参加 OPC 创业训练营，回家乡也能创未来。')
)), 2, '0', 1, 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM cms_block WHERE block_key = 'talent-training');

INSERT INTO cms_block (block_key, page_key, title, template, config_json, sort, visible, version, update_by, update_time)
SELECT 'talent-tools', 'talent', '🛠️ 教学工具', 'text',
JSON_OBJECT('text', '依托塔链人工智能科技（广州）有限公司研发迭代 <b>"无限画布"</b> 与 <b>"AgentSkill"</b> 教学工具，让 AI 技能学习更直观、上手更快。'),
3, '0', 1, 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM cms_block WHERE block_key = 'talent-tools');

INSERT INTO cms_block (block_key, page_key, title, template, config_json, sort, visible, version, update_by, update_time)
SELECT 'talent-ecosystem', 'talent', '🌐 人才生态', 'stats',
JSON_OBJECT('items', JSON_ARRAY(
  JSON_OBJECT('value', '10 所', 'label', '省职教城高校数量'),
  JSON_OBJECT('value', '14 万+', 'label', '省职教城师生人才储备')
), 'text', '联动<b>"百万英才汇南粤"</b>、<b>"青雁归清"</b>计划，让清远籍人才回得来、留得住、创得成。'),
4, '0', 1, 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM cms_block WHERE block_key = 'talent-ecosystem');

INSERT INTO cms_block (block_key, page_key, title, template, config_json, sort, visible, version, update_by, update_time)
SELECT 'talent-cta-banner', 'talent', '报名训练营，解锁 AI 新技能', 'cta',
JSON_OBJECT('text', 'OPC 创客训练营 / 毕业生 AI 实训营正在招募中 ｜ 咨询电话：0763-3391888', 'btnText', '前往首页报名', 'btnLink', 'home.html#contact'),
5, '0', 1, 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM cms_block WHERE block_key = 'talent-cta-banner');

-- ==================== industry 产业生态 ====================
INSERT INTO cms_block (block_key, page_key, title, template, config_json, sort, visible, version, update_by, update_time)
SELECT 'industry-stats', 'industry', '📊 行业背景数据', 'stats',
JSON_OBJECT('items', JSON_ARRAY(
  JSON_OBJECT('value', '1200 亿+', 'label', '2025 年国内 AI 视频市场规模突破'),
  JSON_OBJECT('value', '5800 亿', 'label', '预计 2030 年市场规模达'),
  JSON_OBJECT('value', '95%+', 'label', '2026 年 Q1 上新微短剧 AI 制作占比'),
  JSON_OBJECT('value', '400 亿', 'label', '2026 年 AI 短剧市场规模预计冲击')
)), 1, '0', 1, 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM cms_block WHERE block_key = 'industry-stats');

INSERT INTO cms_block (block_key, page_key, title, template, config_json, sort, visible, version, update_by, update_time)
SELECT 'industry-tracks', 'industry', '🎯 三大赛道', 'cards',
JSON_OBJECT('cols', 3, 'cards', JSON_ARRAY(
  JSON_OBJECT('icon', '🎬', 'title', 'AI 内容创作', 'text', '聚焦 AI 微短剧、超级数字人、AIGC 内容创作。全国首部 AIGC 黄梅戏微短剧《打猪草》已采用仿真数字人与动作捕捉技术，为内容创作注入 AI 新表达。<ul><li>AI 微短剧</li><li>超级数字人</li><li>AIGC 内容创作</li></ul>'),
  JSON_OBJECT('icon', '🤖', 'title', 'AI 技术应用', 'text', '聚焦本土自研 AI 获客系统、GEO（生成式引擎优化）与 AI 应用开发，让 AI 能力真正落地为获客与增长工具。<ul><li>本土自研 AI 获客系统</li><li>GEO（生成式引擎优化）</li><li>AI 应用开发</li></ul>'),
  JSON_OBJECT('icon', '🕶️', 'title', 'AI 硬件与场景生态链', 'text', '覆盖 AI 眼镜、AI 手机、AI 工牌、AI 玩具、AI 陪伴设备、AI 外骨骼辅助装置，以及多个 AI 场景训练场，构建硬件 + 场景的完整生态链。<ul><li>AI 眼镜 / AI 手机 / AI 工牌 / AI 玩具</li><li>AI 陪伴设备 / AI 外骨骼辅助装置</li><li>AI 具身智能训练场</li><li>AI 工业场景训练场 / AI 生活场景训练场</li></ul>')
)), 2, '0', 1, 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM cms_block WHERE block_key = 'industry-tracks');

INSERT INTO cms_block (block_key, page_key, title, template, config_json, sort, visible, version, update_by, update_time)
SELECT 'industry-companies', 'industry', '🏢 首批入驻企业（2026-08-01 揭牌签约）', 'cards',
JSON_OBJECT('cols', 3, 'cards', JSON_ARRAY(
  JSON_OBJECT('icon', '🔬', 'title', '塔链人工智能科技（广州）有限公司', 'text', 'AI 技术研发 —— 研发迭代"无限画布""AgentSkill"教学工具'),
  JSON_OBJECT('icon', '🧠', 'title', '南京世东智脑人工智能有限公司', 'text', 'AI 技术研发'),
  JSON_OBJECT('icon', '🎬', 'title', '正经点赞（清远）媒体科技有限公司', 'text', '数字内容创作'),
  JSON_OBJECT('icon', '🎓', 'title', '北江人工智能产教融合研究院', 'text', 'AI 产教融合'),
  JSON_OBJECT('icon', '🎨', 'title', '光年制造工作室', 'text', '创意设计'),
  JSON_OBJECT('icon', '🎮', 'title', '彗星互娱工作室', 'text', '创意文娱设计')
)), 3, '0', 1, 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM cms_block WHERE block_key = 'industry-companies');

INSERT INTO cms_block (block_key, page_key, title, template, config_json, sort, visible, version, update_by, update_time)
SELECT 'industry-merge', 'industry', '🔗 产业融合', 'cards',
JSON_OBJECT('cols', 3, 'cards', JSON_ARRAY(
  JSON_OBJECT('icon', '🏞️', 'title', 'AI + 文旅', 'text', '用 AIGC 短剧、数字人讲解与文创内容，为清远文旅资源注入 AI 新表达。'),
  JSON_OBJECT('icon', '🏭', 'title', 'AI + 制造', 'text', 'AI 视觉检测、智能排产与工业场景训练，助力本地制造业智能化升级。'),
  JSON_OBJECT('icon', '🛍️', 'title', 'AI + 现代服务业', 'text', '数字人直播、智能客服与个性化推荐，重塑本地生活与现代服务体验。')
)), 4, '0', 1, 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM cms_block WHERE block_key = 'industry-merge');

-- ==================== 停用原 cta 槽位区块（由 cta 模板承担，数据保留）====================
UPDATE cms_block SET visible = '1', update_by = 'admin', update_time = NOW()
WHERE block_key IN ('about-cta', 'talent-cta', 'industry-cta');

-- 完成提示
SELECT page_key, template, COUNT(*) AS cnt FROM cms_block
WHERE template IS NOT NULL AND template != '' GROUP BY page_key, template ORDER BY page_key, template;
