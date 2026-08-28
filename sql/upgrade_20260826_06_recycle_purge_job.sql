-- ============================================
-- 升级脚本：回收站 30 天自动清理定时任务 v20260826
-- 内容：注册 sys_job「回收站过期清理」——每天 3:30 调 cmsArticleServiceImpl.purgeRecycleBinExpired()
--       （此前该清理方法全仓库无任何调度方/种子，回收站无限膨胀，H11 修复）
-- 幂等：INSERT ... SELECT WHERE NOT EXISTS（job_name 判重）
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p ry-vue < sql/upgrade_20260826_recycle_purge_job.sql
-- ============================================


INSERT INTO sys_job (job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, remark)
SELECT '回收站过期清理','SYSTEM','cmsArticleServiceImpl.purgeRecycleBinExpired()','0 30 3 * * ?','3','1','0','admin',NOW(),'每天3:30自动永久删除回收站中超过30天的文章'
WHERE NOT EXISTS (SELECT 1 FROM sys_job WHERE job_name='回收站过期清理');
