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
DELETE FROM cms_article_history
WHERE article_id IN (SELECT article_id FROM cms_article
                     WHERE title = '《广东省支持人工智能 OPC 创新发展行动方案（2026—2028年）》'
                       AND category_id = 8);
DELETE FROM cms_article
WHERE title = '《广东省支持人工智能 OPC 创新发展行动方案（2026—2028年）》'
  AND category_id = 8;

-- ②b. 本地遗留停用菜单清理（服务管理/成员服务/合作经营：status='1' 停用，
--       仅存在于历史演化库，部署链不产生——全新库无此菜单，清理以保部署一致）
DELETE rm FROM sys_role_menu rm
WHERE rm.menu_id IN (SELECT menu_id FROM sys_menu
                     WHERE menu_name IN ('服务管理','成员服务','合作经营') AND status = '1');
DELETE FROM sys_menu
WHERE menu_name IN ('服务管理','成员服务','合作经营') AND status = '1';

-- ②c. 重复揭牌文章清理（upgrade 链插入 id=1 与快照 id=18 同标题重复——
--       本地历史 id=1 被错位文章占用，快照不含 id=1；保留较大 id 与快照一致）
DELETE a1 FROM cms_article a1
JOIN cms_article a2 ON a1.title = a2.title AND a1.article_id < a2.article_id
WHERE a1.title = '清远首个人工智能 OPC 生态社区正式揭牌运营'
  AND a1.del_flag <> '2' AND a2.del_flag <> '2';
DELETE h FROM cms_article_history h
WHERE h.article_id NOT IN (SELECT article_id FROM cms_article);

-- ③ sys_config 修复：config_id=121「页脚-联系我们」名称乱码（历史导出损坏，hex 3F3F3F2DEFBFBD...）
UPDATE sys_config SET config_name = '页脚-联系我们'
WHERE config_id = 121 AND config_key = 'site.footer.contact';

-- ④ sys_config 唯一索引（幂等守卫：已存在则跳过）
SET @has_uk := (SELECT COUNT(*) FROM information_schema.statistics
                WHERE table_schema = DATABASE() AND table_name = 'sys_config' AND index_name = 'uk_config_key');
SET @ddl := IF(@has_uk = 0, 'ALTER TABLE sys_config ADD UNIQUE KEY uk_config_key (config_key)', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 完成提示
SELECT COUNT(*) AS orphan_menu_left FROM sys_menu WHERE parent_id <> 0 AND parent_id NOT IN (SELECT menu_id FROM sys_menu);
SELECT COUNT(*) AS test_page_left FROM cms_page WHERE page_key = 'test';
