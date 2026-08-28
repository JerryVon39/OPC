-- ============================================
-- 2026-08-28 收尾修复：
-- ① admin 密码统一为 admin123（与本地开发库一致，方便部署方使用）
-- ② 停用冗余「回收站过期清理」任务（job_id=102 与 103 调用同一方法，保留 SYSTEM 组 103）
-- 幂等：均带条件更新，重复执行无副作用
-- ============================================

-- ① admin 密码统一为 admin123（本地库当前哈希；Ee606EcUQsgj 为旧基础库值不再使用）
UPDATE sys_user SET password = '$2a$10$xbykThJtBwk4YS0TW4J3ieKbSGHFopyfFv97wQUaYW/CHrUAgUyT.'
WHERE user_name = 'admin' AND password <> '$2a$10$xbykThJtBwk4YS0TW4J3ieKbSGHFopyfFv97wQUaYW/CHrUAgUyT.';

-- ② 停用冗余回收站清理任务（102 DEFAULT 组与 103 SYSTEM 组同方法；保留 103）
UPDATE sys_job SET status = '1' WHERE job_id = 102 AND status = '0';