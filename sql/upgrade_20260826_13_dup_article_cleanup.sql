-- ============================================
-- 清理：删除标题带「（测试2）（测试3）」后缀、与正式文章完全重复的测试残留
--       （M6 清理遗漏项，2026-08-26 补；2026-08-28 重写：原按 article_id=11 硬删，
--       id 在全新库自增漂移会误删真实文章（旧卷/真实运营库 id 序列不同），
--       改按标题特征删除，任何库序安全）
-- 硬删（含历史版本），幂等：第二次执行无匹配行即空操作
-- ============================================
DELETE FROM cms_article_history
WHERE article_id IN (SELECT article_id FROM cms_article
                     WHERE title LIKE '%（测试2）%' OR title LIKE '%（测试3）%');
DELETE FROM cms_article
WHERE title LIKE '%（测试2）%' OR title LIKE '%（测试3）%';
