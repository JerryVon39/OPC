-- ============================================
-- 入驻申请挂账号：book_purchase_req 加 reader_id（可空，兼容匿名历史数据）
-- 个人主页「我的入驻申请」按此列关联成员
-- ============================================
ALTER TABLE `book_purchase_req`
    ADD COLUMN `reader_id` bigint DEFAULT NULL COMMENT '申请成员ID（前台登录后提交时关联；匿名历史数据为空）' AFTER `remark`;
