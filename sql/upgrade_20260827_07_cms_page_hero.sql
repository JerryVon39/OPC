-- ============================================
-- 页面页头（hero）可自定义：cms_page 加 hero 三列 + 内置栏目页注册进 cms_page
-- hero_title / hero_subtitle / hero_bg（背景：图片 URL 或 CSS 色值，空=默认）
-- 幂等：补列 + INSERT...WHERE NOT EXISTS
-- ============================================
SET @c1 = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='cms_page' AND column_name='hero_title');
SET @s1 = IF(@c1=0, 'ALTER TABLE cms_page ADD COLUMN hero_title varchar(100) DEFAULT NULL COMMENT ''页头大标题（空=不显示页头）''', 'SELECT 1');
PREPARE st1 FROM @s1; EXECUTE st1; DEALLOCATE PREPARE st1;
SET @c2 = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='cms_page' AND column_name='hero_subtitle');
SET @s2 = IF(@c2=0, 'ALTER TABLE cms_page ADD COLUMN hero_subtitle varchar(200) DEFAULT NULL COMMENT ''页头副标题''', 'SELECT 1');
PREPARE st2 FROM @s2; EXECUTE st2; DEALLOCATE PREPARE st2;
SET @c3 = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='cms_page' AND column_name='hero_bg');
SET @s3 = IF(@c3=0, 'ALTER TABLE cms_page ADD COLUMN hero_bg varchar(255) DEFAULT NULL COMMENT ''页头背景（图片URL或CSS色值，空=默认深蓝渐变）''', 'SELECT 1');
PREPARE st3 FROM @s3; EXECUTE st3; DEALLOCATE PREPARE st3;

INSERT INTO cms_page (page_key, page_name, sort, status, menu_pos, hero_title, hero_subtitle, create_by, create_time)
SELECT 'about', '走进社区', 1, '0', 'nav', '走进社区', '清远市首个人工智能 OPC（一人公司）生态社区 —— 让"一个人 + AI"也能轻松创业', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM cms_page WHERE page_key='about');
INSERT INTO cms_page (page_key, page_name, sort, status, menu_pos, hero_title, hero_subtitle, create_by, create_time)
SELECT 'join', '入驻招商', 2, '0', 'nav', '入驻招商', '两种入驻方式，零门槛起步 —— 无论你是"一个人 + AI"的数字游民，还是创业团队，都能在这里安家', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM cms_page WHERE page_key='join');
INSERT INTO cms_page (page_key, page_name, sort, status, menu_pos, hero_title, hero_subtitle, create_by, create_time)
SELECT 'talent', '人才培养', 3, '0', 'nav', '人才培养', '从高校到实战，从技能培训到创业训练营，构建覆盖全周期的 AI 人才培养体系', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM cms_page WHERE page_key='talent');
INSERT INTO cms_page (page_key, page_name, sort, status, menu_pos, hero_title, hero_subtitle, create_by, create_time)
SELECT 'industry', '产业生态', 4, '0', 'nav', '产业生态', '聚焦 AI 内容创作、AI 技术应用、AI 硬件与场景生态链三大赛道，构建"平台 + 培训 + 订单 + 运营"一体化产业生态', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM cms_page WHERE page_key='industry');
INSERT INTO cms_page (page_key, page_name, sort, status, menu_pos, hero_title, hero_subtitle, create_by, create_time)
SELECT 'policy', '政策赋能', 5, '0', 'nav', '政策赋能', '算力、政策、订单三大赋能体系，为入驻企业与数字游民创业全程护航', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM cms_page WHERE page_key='policy');
INSERT INTO cms_page (page_key, page_name, sort, status, menu_pos, hero_title, hero_subtitle, create_by, create_time)
SELECT 'news', '新闻动态', 6, '0', 'nav', '新闻动态', '社区要闻 · 政策解读 · 活动报道 · 入驻故事 —— 数智游民创新工场的一手动态', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM cms_page WHERE page_key='news');

-- 迁移旧「页头副标语」槽位数据 → cms_page.hero_subtitle（仅未配置时），并移除旧槽位（hero 统一由注册表渲染；历史在 cms_block_history 可回滚）
UPDATE cms_page p JOIN cms_block b ON b.page_key='about' AND b.block_key='about-hero-sub'
SET p.hero_subtitle = b.content WHERE p.page_key='about' AND (p.hero_subtitle IS NULL OR p.hero_subtitle='');
DELETE FROM cms_block WHERE block_key='about-hero-sub';
