-- ============================================
-- 2026-08-28 终态清理（幂等，可重复执行）
-- 背景（问题报告核查确认）：
-- ① 孤儿菜单兜底：旧版 upgrade 脚本按 menu_id 硬编码曾产生 parent 不存在的孤儿菜单
--    （26_02 已重写按名称定位并自愈，本脚本兜底清除任何残余孤儿，防其他脚本残留）
-- ② 快照脏数据：cms_page 有 page_key='test' 测试页、cms_block 有 4 只 pb-* 测试区块、
--    cms_article id=1 标题为省级政策文件但正文是揭牌运营报道（与正式揭牌文章重复，
--    内容错位；政策原文待素材补充后另行建文）——测试/错位数据随快照进全新部署库
-- ③ sys_config 无 config_key 唯一索引：快照 REPLACE 依赖自增 id 对齐，增删漂移会
--    产生重复 config_key → selectConfigByKey 抛 TooManyResultsException（前台 500）
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p ry-vue < sql/upgrade_20260828_04_cleanup.sql
-- ============================================

-- ① 孤儿菜单兜底清除（parent 不存在的非顶层菜单；先清授权再删菜单，JOIN 避开 1093）
DELETE rm FROM sys_role_menu rm
WHERE rm.menu_id NOT IN (SELECT menu_id FROM sys_menu);
DELETE m FROM sys_menu m
LEFT JOIN sys_menu p ON p.menu_id = m.parent_id
WHERE m.parent_id <> 0 AND p.menu_id IS NULL;

-- ② 快照脏数据：测试页及其测试区块、内容错位文章（按内容特征，任何库序安全）
DELETE FROM cms_block WHERE page_key = 'test';
DELETE FROM cms_page WHERE page_key = 'test';
-- R-N5 fix: 删除加 content 守卫——错位文正文是揭牌报道（content 含「揭牌运营」），
-- 真政策正文是【政策名称】…粤发改高技…；不加守卫会在存量库日后发布同标题真政策时误删
DELETE FROM cms_article_history
WHERE article_id IN (SELECT article_id FROM cms_article
                     WHERE title = '《广东省支持人工智能 OPC 创新发展行动方案（2026—2028年）》'
                       AND category_id = 8 AND content LIKE '%揭牌运营%');
DELETE FROM cms_article
WHERE title = '《广东省支持人工智能 OPC 创新发展行动方案（2026—2028年）》'
  AND category_id = 8 AND content LIKE '%揭牌运营%';

-- ②b. 本地遗留停用菜单清理（服务管理/成员服务/合作经营：status='1' 停用，
--       仅存在于历史演化库，部署链不产生——全新库无此菜单，清理以保部署一致）
DELETE rm FROM sys_role_menu rm
WHERE rm.menu_id IN (SELECT menu_id FROM sys_menu
                     WHERE menu_name IN ('服务管理','成员服务','合作经营') AND status = '1');
DELETE FROM sys_menu
WHERE menu_name IN ('服务管理','成员服务','合作经营') AND status = '1';

-- ②c. 升级链种子遗留的重复揭牌文章（N-2 回归修复：upgrade 链在「新闻动态」cat=1 插入的
--       揭牌报道，与快照「社区要闻」cat=7 的正式揭牌文章同标题——本脚本执行于快照导入前，
--       当时无第二行可 JOIN（旧版 JOIN 去重失效，实测全新库终态 16 条重复）；
--       改按「标题+栏目」精确删除种子行，快照的 cat=7 文章不受影响）
DELETE FROM cms_article_history
WHERE article_id IN (SELECT article_id FROM cms_article
                     WHERE title = '清远首个人工智能 OPC 生态社区正式揭牌运营' AND category_id = 1);
DELETE FROM cms_article
WHERE title = '清远首个人工智能 OPC 生态社区正式揭牌运营' AND category_id = 1;

-- ②d. editor 补授「运营辅助」父级（R7：原只授「回收站」目录未授父级，buildMenuTree
--       把回收站当顶层渲染，editor 侧边栏树结构走样——补父级后回收站正确挂运营辅助下）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id FROM sys_role r, sys_menu m
WHERE r.role_key='editor' AND m.menu_name='运营辅助' AND m.menu_type='M'
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id=r.role_id AND rm.menu_id=m.menu_id);

-- ③ sys_config 修复：config_id=121「页脚-联系我们」名称乱码（历史导出损坏，hex 3F3F3F2DEFBFBD...）
UPDATE sys_config SET config_name = '页脚-联系我们'
WHERE config_id = 121 AND config_key = 'site.footer.contact';

-- ④ sys_config 唯一索引（R-N6 fix: 加去重仲裁——存量库若已有重复 config_key，
--    ADD UNIQUE 会报 Duplicate entry 中断整条升级链；先删重复（保留最小 id）再加索引）
DELETE c1 FROM sys_config c1
JOIN sys_config c2 ON c1.config_key = c2.config_key AND c1.config_id > c2.config_id;
SET @has_uk := (SELECT COUNT(*) FROM information_schema.statistics
                WHERE table_schema = DATABASE() AND table_name = 'sys_config' AND index_name = 'uk_config_key');
SET @ddl := IF(@has_uk = 0, 'ALTER TABLE sys_config ADD UNIQUE KEY uk_config_key (config_key)', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 完成提示
SELECT COUNT(*) AS orphan_menu_left FROM sys_menu WHERE parent_id <> 0 AND parent_id NOT IN (SELECT menu_id FROM sys_menu);
SELECT COUNT(*) AS test_page_left FROM cms_page WHERE page_key = 'test';
