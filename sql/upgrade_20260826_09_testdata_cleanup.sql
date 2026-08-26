-- ============================================
-- 幂等清理：删除开发期测试数据（M6 第七轮审查）
-- 1. cms_article 测试/乱码/回收站行：id 16/17/20（回收站测试行）、22（"test" 下线行）
-- 2. cms_article_history 中上述文章的历史版本
-- 3. 本文件设计为可重复执行（第二次执行无匹配行即空操作）
-- 注：sys_notice 无乱码/测试行（已核实，不处理）
-- ============================================
DELETE FROM cms_article_history WHERE article_id IN (16, 17, 20, 22);
DELETE FROM cms_article WHERE article_id IN (16, 17, 20, 22);
