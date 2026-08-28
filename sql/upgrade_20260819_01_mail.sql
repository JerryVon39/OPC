-- ============================================
-- 升级脚本：自动邮件通知（2026-08-19）
-- 1) 读者表加 email 列（新读者登记必填，用于借阅/预约等邮件通知）
-- 2) 回收站读者表加 email 列（还原读者时保留邮箱）
-- 执行：mysql -uroot -p --default-character-set=utf8mb4 ry-vue < upgrade_20260819_mail.sql
-- 幂等：MySQL 不支持"ADD COLUMN IF NOT EXISTS"（8.4 亦然），改用 INFORMATION_SCHEMA 判断，列不存在才 ADD，可重复执行
-- 邮件配置见 application.yml：发件邮箱 MAIL_USERNAME、授权码 MAIL_AUTH_CODE（环境变量注入，勿写进代码库）
-- ============================================


SET @has_reader_email := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='reader' AND COLUMN_NAME='email');
SET @ddl1 := IF(@has_reader_email = 0,
    'ALTER TABLE `reader` ADD COLUMN `email` varchar(50) DEFAULT NULL COMMENT ''电子邮箱（新读者登记必填，用于邮件通知）'' AFTER `phone`',
    'SELECT ''reader.email 已存在，跳过''');
PREPARE s1 FROM @ddl1; EXECUTE s1; DEALLOCATE PREPARE s1;

SET @has_recycle_table := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='reader_recycle');
SET @has_recycle_email := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='reader_recycle' AND COLUMN_NAME='email');
-- 表不存在时跳过（回收站建表在 upgrade_20260819_recycle.sql，需先于本脚本执行；任意顺序下都不报错）
SET @ddl2 := IF(@has_recycle_table = 0,
    'SELECT ''reader_recycle 表不存在，跳过（需先执行 upgrade_20260819_recycle.sql）''',
    IF(@has_recycle_email = 0,
        'ALTER TABLE `reader_recycle` ADD COLUMN `email` varchar(50) DEFAULT NULL COMMENT ''电子邮箱'' AFTER `phone`',
        'SELECT ''reader_recycle.email 已存在，跳过'''));
PREPARE s2 FROM @ddl2; EXECUTE s2; DEALLOCATE PREPARE s2;

SET @has_purchase_email := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='book_purchase_req' AND COLUMN_NAME='email');
SET @ddl3 := IF(@has_purchase_email = 0,
    'ALTER TABLE `book_purchase_req` ADD COLUMN `email` varchar(50) DEFAULT NULL COMMENT ''申请者邮箱（荐购结果通知用）'' AFTER `author`',
    'SELECT ''book_purchase_req.email 已存在，跳过''');
PREPARE s3 FROM @ddl3; EXECUTE s3; DEALLOCATE PREPARE s3;