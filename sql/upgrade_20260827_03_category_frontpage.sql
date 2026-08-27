-- ============================================
-- I：栏目前台归属页面可配置（替代名称前缀"政策"硬编码）
-- 1. cms_category 加 front_page 列（'news' 资讯动态页 / 'policy' 政策赋能页，默认 news）
-- 2. 存量数据按名称前缀迁移：以"政策"开头 → policy，其余 → news
-- 幂等：可重复执行
-- ============================================

SET @c1 = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='cms_category' AND column_name='front_page');
SET @s1 = IF(@c1=0, 'ALTER TABLE cms_category ADD COLUMN front_page varchar(10) NOT NULL DEFAULT ''news'' COMMENT ''前台归属页面：news=资讯动态页 policy=政策赋能页''', 'SELECT 1');
PREPARE st1 FROM @s1; EXECUTE st1; DEALLOCATE PREPARE st1;

-- 存量迁移：名称以"政策"开头的栏目归政策页（与旧硬编码行为一致）
UPDATE cms_category SET front_page = 'policy' WHERE category_name LIKE '政策%' AND front_page = 'news';
