-- ============================================
-- 2026-08-28：清理重复轮播——business_init.sql 的旧 banner 行被快照 REPLACE
-- 覆盖成"三大赋能体系"后，与快照新插入的同内容行重复（全新库 4 条 vs 本地 3 条）
-- 幂等：仅当"三大赋能体系"存在多条时，删除非最小 id 的重复行
-- ============================================
DELETE b1 FROM sys_banner b1
JOIN sys_banner b2 ON b1.title = b2.title AND b1.subtitle = b2.subtitle
WHERE b1.title = '三大赋能体系' AND b1.banner_id > b2.banner_id;