-- ============================================
-- 入驻申请挂账号：book_purchase_req 加 reader_id（可空，兼容匿名历史数据）
-- 个人主页「我的入驻申请」按此列关联成员
-- 幂等：列已存在则跳过（business_init.sql 建表已含该列，老库靠本脚本补）
-- ============================================
-- 注意：DATABASE() 依赖已选库，手动执行请带库名或先 USE（docker 初始化已通过 "$MYSQL_DATABASE" 传入）
USE ry-vue;
SET @exist_reader_id := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'book_purchase_req'
      AND COLUMN_NAME = 'reader_id'
);
SET @sql_reader_id := IF(@exist_reader_id = 0,
    'ALTER TABLE `book_purchase_req` ADD COLUMN `reader_id` bigint DEFAULT NULL COMMENT ''申请成员ID（前台登录后提交时关联；匿名历史数据为空）'' AFTER `remark`',
    'SELECT 1');
PREPARE stmt_reader_id FROM @sql_reader_id;
EXECUTE stmt_reader_id;
DEALLOCATE PREPARE stmt_reader_id;