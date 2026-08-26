-- ============================================
-- 63：公告生效/失效时间（幂等补列）
-- begin_time/end_time：NULL=不限；公告条仅在时间窗内展示
-- 后台表单（通知公告）可设置；历史公告无值自动兼容
-- ============================================
SET @c1 = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='sys_notice' AND column_name='begin_time');
SET @s1 = IF(@c1=0, 'ALTER TABLE sys_notice ADD COLUMN begin_time datetime NULL COMMENT ''生效时间（NULL=立即生效）''', 'SELECT 1');
PREPARE st1 FROM @s1; EXECUTE st1; DEALLOCATE PREPARE st1;
SET @c2 = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='sys_notice' AND column_name='end_time');
SET @s2 = IF(@c2=0, 'ALTER TABLE sys_notice ADD COLUMN end_time datetime NULL COMMENT ''失效时间（NULL=长期有效）''', 'SELECT 1');
PREPARE st2 FROM @s2; EXECUTE st2; DEALLOCATE PREPARE st2;
