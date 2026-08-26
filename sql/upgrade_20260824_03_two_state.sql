-- ============================================
-- 升级脚本：回收站三态 → 两态（软删除 + 恢复/永久删除），v20260824
-- 适用：存量库升级路径；幂等可重复执行；务必带 --default-character-set=utf8mb4
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p ry-vue < sql/upgrade_20260824_two_state.sql
--
-- 背景：原回收站为「物理删除 + 快照进 book_recycle/reader_recycle 表」的三态流转。
-- 本次改为两态：删除 → book/reader 主表软删除（del_flag='2'），对前台/列表不可见；
-- 后台提供「恢复」（del_flag 置 '0'）与「永久删除」（物理 DELETE）两个操作。
-- 说明：book_recycle/reader_recycle 表与相关旧接口不再被业务调用（保留表结构，不丢数据）。
-- ============================================

USE ry-vue;

-- 1. book 主表增加软删除标记字段（幂等：列已存在则跳过）
SET @has := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='ry-vue' AND TABLE_NAME='book' AND COLUMN_NAME='del_flag');
SET @ddl := IF(@has=0,
  'ALTER TABLE `book`
     ADD COLUMN `del_flag` char(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标志（0存在 2删除）'' AFTER `status`,
     ADD COLUMN `deleted_by` varchar(64) DEFAULT '''' COMMENT ''删除人'' AFTER `del_flag`,
     ADD COLUMN `deleted_time` datetime DEFAULT NULL COMMENT ''删除时间'' AFTER `deleted_by`',
  'SELECT ''book.del_flag 已存在，跳过''');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. reader 主表同样增加软删除标记字段
SET @has := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='ry-vue' AND TABLE_NAME='reader' AND COLUMN_NAME='del_flag');
SET @ddl := IF(@has=0,
  'ALTER TABLE `reader`
     ADD COLUMN `del_flag` char(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标志（0存在 2删除）'' AFTER `status`,
     ADD COLUMN `deleted_by` varchar(64) DEFAULT '''' COMMENT ''删除人'' AFTER `del_flag`,
     ADD COLUMN `deleted_time` datetime DEFAULT NULL COMMENT ''删除时间'' AFTER `deleted_by`',
  'SELECT ''reader.del_flag 已存在，跳过''');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. 数据迁移（幂等 + 表存在性保护，当前库回收站为空，本段为安全兜底）：
--    若回收站表里已有快照数据，回写主表并标记 del_flag='2'，避免改用软删除后这些快照成为孤儿。
--    注意：book_recycle/reader_recycle 三态快照表已被 upgrade_20260825_recycle_cleanup 删除，
--    本脚本在 start-local.bat 每次启动都会重跑，表不存在时报 1146，故先判存在再迁移。
SET @has_book_recycle := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='ry-vue' AND TABLE_NAME='book_recycle');
SET @migrate_book := IF(@has_book_recycle > 0,
  'INSERT INTO book (book_name, author, book_type, publisher, price, publish_date, stock, status, cover, isbn, intro, remark, create_by, create_time, update_by, update_time, del_flag, deleted_by, deleted_time)
   SELECT r.book_name, r.author, r.book_type, r.publisher, r.price, r.publish_date, r.stock, r.status, r.cover, r.isbn, r.intro, r.remark, r.create_by, r.create_time, r.update_by, r.update_time, ''2'', r.deleted_by, r.deleted_time
   FROM book_recycle r WHERE NOT EXISTS (SELECT 1 FROM book b WHERE b.book_id = r.book_id)',
  'SELECT ''book_recycle 不存在（已被 recycle_cleanup 清理），跳过迁移''');
PREPARE stmt FROM @migrate_book; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_reader_recycle := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='ry-vue' AND TABLE_NAME='reader_recycle');
SET @migrate_reader := IF(@has_reader_recycle > 0,
  'INSERT INTO reader (reader_name, phone, email, card_no, reader_type, sex, birth_date, status, remark, create_by, create_time, update_by, update_time, del_flag, deleted_by, deleted_time)
   SELECT r.reader_name, r.phone, r.email, r.card_no, r.reader_type, r.sex, r.birth_date, r.status, r.remark, r.create_by, r.create_time, r.update_by, r.update_time, ''2'', r.deleted_by, r.deleted_time
   FROM reader_recycle r WHERE NOT EXISTS (SELECT 1 FROM reader b WHERE b.reader_id = r.reader_id)',
  'SELECT ''reader_recycle 不存在（已被 recycle_cleanup 清理），跳过迁移''');
PREPARE stmt FROM @migrate_reader; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT '软删除两态改造完成：book/reader 已具备 del_flag/deleted_by/deleted_time' AS result;