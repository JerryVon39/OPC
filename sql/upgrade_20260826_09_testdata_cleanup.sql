-- ============================================
-- 幂等清理：删除开发期测试数据（M6 第七轮审查；2026-08-28 重写：按内容特征删除）
-- 1. cms_article 测试行（原按 id 16/17/20/22 硬删——id 在全新库自增漂移，
--    硬编码 id 可能误删真实文章，改按标题特征定位，任何库序安全）
-- 2. cms_article_history 中上述文章的历史版本
-- 3. 本文件设计为可重复执行（第二次执行无匹配行即空操作）
-- 注：sys_notice 无乱码/测试行（已核实，不处理）
-- ============================================
DELETE FROM cms_article_history
WHERE article_id IN (SELECT article_id FROM cms_article
                     WHERE (title LIKE '%test%' OR title LIKE '%（测试%') AND del_flag = '2');
DELETE FROM cms_article
WHERE (title LIKE '%test%' OR title LIKE '%（测试%') AND del_flag = '2';
