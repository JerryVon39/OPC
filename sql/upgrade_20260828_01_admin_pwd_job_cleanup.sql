-- ============================================
-- 2026-08-28 收尾修复（2026-08-28 扩展：①admin 口令统一 ②editor 口令统一
--   ——editor 原按 role_init 复制 admin 初始哈希（Ee606EcUQsgj），admin 被本脚本
--   改为 admin123 后 editor 停留在旧值，与文档 editor/0y9JbGS4Whl7 不符（全新部署
--   按文档登录 editor 必失败，实测复现）；现统一为 admin123 与文档一致。
--   ③停用冗余「回收站过期清理」任务（原 job_id=102 硬编码——全新库 job_id 自增
--   漂移会落空，改按 job_name 定位）
-- 幂等：均带条件更新，重复执行无副作用
-- ============================================

-- ① admin 密码统一为 admin123（本地库当前哈希；Ee606EcUQsgj 为旧基础库值不再使用）
UPDATE sys_user SET password = '$2a$10$xbykThJtBwk4YS0TW4J3ieKbSGHFopyfFv97wQUaYW/CHrUAgUyT.'
WHERE user_name = 'admin' AND password <> '$2a$10$xbykThJtBwk4YS0TW4J3ieKbSGHFopyfFv97wQUaYW/CHrUAgUyT.';

-- ② editor 口令统一为 admin123（与 admin 一致，文档 editor/admin123 为准）
UPDATE sys_user SET password = '$2a$10$xbykThJtBwk4YS0TW4J3ieKbSGHFopyfFv97wQUaYW/CHrUAgUyT.'
WHERE user_name = 'editor' AND password <> '$2a$10$xbykThJtBwk4YS0TW4J3ieKbSGHFopyfFv97wQUaYW/CHrUAgUyT.';

-- ③ 停用冗余回收站清理任务（DEFAULT 组与 SYSTEM 组同方法；按名称定位，保留 SYSTEM 组 103）
UPDATE sys_job SET status = '1'
WHERE job_name = '回收站自动清理' AND status = '0'
  AND invoke_target LIKE '%purgeRecycleBinExpired%'
  AND job_group <> 'SYSTEM';
