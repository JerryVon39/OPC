-- ============================================
-- 升级脚本：认证与邮件整体改造（2026-08-24）
-- 对应 docs/整体改造方案-认证与邮件.md（M1 邮件基础设施 + 认证 DDL 一次建齐）
-- 内容：
--   1) mail_config 表（SMTP 配置，单行，授权码 AES 加密）
--   2) mail_template 表（10 个场景模板 + 初始数据，含认证验证码/注册模板）
--   3) reader 表加认证列（password_hash/pwd_set/email_verified/phone_verified/last_login_time）
--   4) reader_login_log 表（读者端登录审计）
--   5) reader.email 唯一索引（先清重复：重复邮箱仅保留 reader_id 最小者，其余置空）
--   6) sys_menu：邮件通知（系统管理下）+ 成员重置密码按钮
--   7) sys_config：reader.session.minutes（14 天滑动会话）、sms.enabled（短信通道预留开关）
-- 幂等：可重复执行；执行：mysql -uroot -p --default-character-set=utf8mb4 ry-vue < upgrade_20260824_auth.sql
-- 安全：SMTP 授权码由后端 AES-GCM 加密入库（密钥 MAIL_SECRET_KEY 环境变量，未设置则明文+页面警告）
-- ============================================


-- ---------- 1. mail_config（单行 SMTP 配置） ----------
CREATE TABLE IF NOT EXISTS `mail_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键（固定单行 id=1）',
  `enabled` char(1) NOT NULL DEFAULT '1' COMMENT '邮件总开关(0关 1开)',
  `host` varchar(100) NOT NULL DEFAULT 'smtp.qq.com' COMMENT 'SMTP 主机',
  `port` int NOT NULL DEFAULT 465 COMMENT 'SMTP 端口',
  `username` varchar(100) NOT NULL DEFAULT '' COMMENT '发件邮箱',
  `auth_code` varchar(500) NOT NULL DEFAULT '' COMMENT 'SMTP 授权码（AES-GCM 加密后存储，前缀 enc:）',
  `from_name` varchar(100) NOT NULL DEFAULT '' COMMENT '发件人昵称（可选）',
  `update_by` varchar(64) DEFAULT '' COMMENT '最后修改人',
  `update_time` datetime DEFAULT NULL COMMENT '最后修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮件SMTP配置（单行，后台可改，改完即时生效）';

-- 单行保障：默认行不存在则插入（幂等）
INSERT INTO `mail_config` (id, enabled, host, port, username, auth_code, from_name, update_time)
SELECT 1, '1', 'smtp.qq.com', 465, '', '', '', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `mail_config`);

-- ---------- 2. mail_template（场景模板） ----------
CREATE TABLE IF NOT EXISTS `mail_template` (
  `code` varchar(50) NOT NULL COMMENT '模板编码（业务场景唯一键）',
  `name` varchar(100) NOT NULL COMMENT '模板名称',
  `subject` varchar(200) NOT NULL COMMENT '邮件主题',
  `content` text NOT NULL COMMENT 'HTML 正文（{占位符} 替换，如 {readerName}）',
  `status` char(1) NOT NULL DEFAULT '1' COMMENT '0停用 1启用（停用回退内置默认模板）',
  `remark` varchar(500) NOT NULL DEFAULT '' COMMENT '可用占位符说明',
  `update_by` varchar(64) DEFAULT '' COMMENT '最后修改人',
  `update_time` datetime DEFAULT NULL COMMENT '最后修改时间',
  PRIMARY KEY (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮件场景模板（后台可编辑，缺失时代码内置默认兜底）';

-- 初始模板数据：INSERT 仅在 code 不存在时生效（幂等，管理员改过的模板不被覆盖）
INSERT INTO `mail_template` (`code`, `name`, `subject`, `content`, `status`, `remark`, `update_by`, `update_time`) VALUES
('register.success', '注册成功（发证号）', '【数智游民创新工场】欢迎加入，您的成员编号已生成',
 '<p>您好，{readerName}：</p><p>欢迎加入数智游民创新工场！您的成员编号为 <b>{cardNo}</b>。</p><p>请使用「成员编号 + 密码」登录官网，妥善保管编号，丢失可在前台补办。</p><p>感谢支持数智游民创新工场！</p>',
 '1', '占位符：{readerName} 姓名、{cardNo} 成员编号。注意：本邮件只发编号，绝不可包含密码明文。', 'admin', NOW()),
('reissue.notify', '补办证号通知', '【数智游民创新工场】您的成员编号已更新',
 '<p>您好，{readerName}：</p><p>您申请补办的成员编号已生效，新编号为 <b>{cardNo}</b>，旧编号已作废。</p><p>请使用新编号 + 原密码登录。感谢支持数智游民创新工场！</p>',
 '1', '占位符：{readerName} 姓名、{cardNo} 新成员编号。', 'admin', NOW()),
('auth.code', '安全验证码', '【数智游民创新工场】安全验证码',
 '<p>您好：</p><p>您的验证码为 <b>{code}</b>，{minutes} 分钟内有效，请勿泄露给他人。</p><p>如非本人操作，请忽略本邮件。感谢支持数智游民创新工场！</p>',
 '1', '占位符：{code} 6 位验证码、{minutes} 有效分钟数。注册/找回密码/修改邮箱共用。', 'admin', NOW()),
('borrow.success', '报名成功通知', '【服务报名】报名成功',
 '<p>您好，{readerName}：</p><p>您已成功报名《{bookName}》，截止日期为 {dueDate}。</p><p>请按时完成，逾期将影响后续报名。感谢支持数智游民创新工场！</p>',
 '1', '占位符：{readerName} 姓名、{bookName} 服务名称、{dueDate} 截止日期(yyyy-MM-dd)。', 'admin', NOW()),
('renew.success', '续期成功通知', '【服务报名】续期成功',
 '<p>您好，{readerName}：</p><p>您已成功续期《{bookName}》，新的应还日期为 {dueDate}。</p><p>如再次需续期或有其他问题，请联系服务台。感谢支持数智游民创新工场！</p>',
 '1', '占位符：{readerName} 姓名、{bookName} 服务名称、{dueDate} 新截止日期(yyyy-MM-dd)。', 'admin', NOW()),
('reserve.success', '候补成功通知', '【服务候补】候补成功',
 '<p>您好，{readerName}：</p><p>您已成功预约《{bookName}》。该服务当前无名额，已进入候补队列；</p><p>一旦有名额释放，我们会通过邮件通知您。感谢支持数智游民创新工场！</p>',
 '1', '占位符：{readerName} 姓名、{bookName} 服务名称。', 'admin', NOW()),
('reserve.available', '候补有名额通知', '【服务候补】您候补的服务有名额了',
 '<p>您好，{readerName}：</p><p>您候补的《{bookName}》已有名额，现可前来报名。</p><p>请尽早在 {days} 天内办理报名，逾期未办将视为放弃。感谢支持数智游民创新工场！</p>',
 '1', '占位符：{readerName} 姓名、{bookName} 服务名称、{days} 有效天数（自动取系统配置 book.reserve.expireDays）。', 'admin', NOW()),
('reserve.cancel', '候补超时取消通知', '【服务候补】候补名额已释放',
 '<p>您好，{readerName}：</p><p>您候补的《{bookName}》名额已释放，候补状态已自动取消。您仍可重新报名或继续候补。</p><p>感谢支持数智游民创新工场！</p>',
 '1', '占位符：{readerName} 姓名、{bookName} 服务名称。', 'admin', NOW()),
('purchase.pass', '入驻申请通过通知', '【入驻申请】您的入驻申请已通过',
 '<p>您好：</p><p>您的申请《{applyName}》已通过审核，运营团队将尽快与您联系办理入驻。欢迎加入数智游民创新工场！</p>',
 '1', '占位符：{applyName} 申请（项目/组织）名称。', 'admin', NOW()),
('purchase.reject', '入驻申请未通过通知', '【入驻申请】您的入驻申请未通过',
 '<p>您好：</p><p>很遗憾，您的申请《{applyName}》暂未通过审核。我们会持续关注您的需求，感谢支持！</p>',
 '1', '占位符：{applyName} 申请（项目/组织）名称。', 'admin', NOW())
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `remark` = VALUES(`remark`);

-- ---------- 3. reader 加认证列（幂等，INFORMATION_SCHEMA 判断） ----------
SET @has_password_hash := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='reader' AND COLUMN_NAME='password_hash');
SET @ddl_a1 := IF(@has_password_hash = 0,
    'ALTER TABLE `reader` ADD COLUMN `password_hash` varchar(100) DEFAULT NULL COMMENT ''BCrypt 密码哈希（NULL=未设置密码）'' AFTER `email`',
    'SELECT ''reader.password_hash 已存在，跳过''');
PREPARE s FROM @ddl_a1; EXECUTE s; DEALLOCATE PREPARE s;

SET @has_pwd_set := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='reader' AND COLUMN_NAME='pwd_set');
SET @ddl_a2 := IF(@has_pwd_set = 0,
    'ALTER TABLE `reader` ADD COLUMN `pwd_set` char(1) NOT NULL DEFAULT ''0'' COMMENT ''是否已设置密码(0未设置 1已设置)'' AFTER `password_hash`',
    'SELECT ''reader.pwd_set 已存在，跳过''');
PREPARE s FROM @ddl_a2; EXECUTE s; DEALLOCATE PREPARE s;

SET @has_email_verified := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='reader' AND COLUMN_NAME='email_verified');
SET @ddl_a3 := IF(@has_email_verified = 0,
    'ALTER TABLE `reader` ADD COLUMN `email_verified` char(1) NOT NULL DEFAULT ''0'' COMMENT ''邮箱已验证(0未验证 1已验证)'' AFTER `pwd_set`',
    'SELECT ''reader.email_verified 已存在，跳过''');
PREPARE s FROM @ddl_a3; EXECUTE s; DEALLOCATE PREPARE s;

SET @has_phone_verified := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='reader' AND COLUMN_NAME='phone_verified');
SET @ddl_a4 := IF(@has_phone_verified = 0,
    'ALTER TABLE `reader` ADD COLUMN `phone_verified` char(1) NOT NULL DEFAULT ''0'' COMMENT ''手机已验证(0未验证 1已验证，短信通道预留)'' AFTER `email_verified`',
    'SELECT ''reader.phone_verified 已存在，跳过''');
PREPARE s FROM @ddl_a4; EXECUTE s; DEALLOCATE PREPARE s;

SET @has_last_login := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='reader' AND COLUMN_NAME='last_login_time');
SET @ddl_a5 := IF(@has_last_login = 0,
    'ALTER TABLE `reader` ADD COLUMN `last_login_time` datetime DEFAULT NULL COMMENT ''最近登录时间（个人主页展示）'' AFTER `status`',
    'SELECT ''reader.last_login_time 已存在，跳过''');
PREPARE s FROM @ddl_a5; EXECUTE s; DEALLOCATE PREPARE s;

-- ---------- 4. reader_login_log（读者端登录审计） ----------
CREATE TABLE IF NOT EXISTS `reader_login_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `reader_id` bigint DEFAULT NULL COMMENT '成员ID（登录成功时必填）',
  `card_no` varchar(30) DEFAULT '' COMMENT '成员编号（失败时尽力记录）',
  `ip` varchar(64) DEFAULT '' COMMENT '来源 IP',
  `event` varchar(30) NOT NULL COMMENT '事件：login/logout/login_fail/change_pwd/reset_pwd/change_email/register',
  `result` char(1) NOT NULL DEFAULT '1' COMMENT '0失败 1成功',
  `msg` varchar(200) DEFAULT '' COMMENT '备注（失败原因等）',
  `create_time` datetime DEFAULT NULL COMMENT '发生时间',
  PRIMARY KEY (`id`),
  KEY `idx_reader_id` (`reader_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='读者端登录/安全事件审计（后台可查）';

-- ---------- 5. reader.email 唯一索引（先清重复再建，幂等） ----------
-- 重复邮箱仅保留 reader_id 最小者，其余置 email=NULL（不删记录，不破坏报名/候补关联，管理员可手工补录）
UPDATE `reader` r1
JOIN (
    SELECT `email`, MIN(`reader_id`) AS keep_id FROM `reader`
    WHERE `email` IS NOT NULL AND `email` <> '' AND `del_flag` = '0'
    GROUP BY `email` HAVING COUNT(1) > 1
) dup ON r1.`email` = dup.`email` AND r1.`reader_id` <> dup.keep_id
SET r1.`email` = NULL, r1.`update_time` = NOW();

SET @has_uk_email := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='reader' AND INDEX_NAME='uk_email');
SET @ddl_a6 := IF(@has_uk_email = 0,
    'ALTER TABLE `reader` ADD UNIQUE INDEX `uk_email` (`email`)',
    'SELECT ''reader.uk_email 已存在，跳过''');
PREPARE s FROM @ddl_a6; EXECUTE s; DEALLOCATE PREPARE s;

-- ---------- 6. sys_menu：邮件通知（系统管理下）+ 成员重置密码按钮（幂等） ----------
INSERT INTO `sys_menu` (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '邮件通知', (SELECT menu_id FROM sys_menu WHERE menu_name='系统管理' LIMIT 1), 20, 'mail', 'system/mail/index', 1, 0, 'C', '0', '0', 'system:mail:config', 'email', 'admin', NOW(), '邮件 SMTP 配置与场景模板管理（改完即时生效）'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms='system:mail:config');

INSERT INTO `sys_menu` (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '邮件模板管理', (SELECT menu_id FROM sys_menu WHERE perms='system:mail:config' LIMIT 1), 1, '', '', 1, 0, 'F', '0', '0', 'system:mail:template', '#', 'admin', NOW(), '邮件场景模板编辑权限'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms='system:mail:template');

INSERT INTO `sys_menu` (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '成员重置密码', (SELECT menu_id FROM sys_menu WHERE menu_name IN ('成员管理','读者管理') ORDER BY menu_id LIMIT 1), 8, '', '', 1, 0, 'F', '0', '0', 'system:reader:resetPwd', '#', 'admin', NOW(), '向成员发送设置密码/重置密码邀请邮件'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms='system:reader:resetPwd');

-- 超管 admin 角色自动获得新菜单权限（admin 角色菜单不落库校验：若依 admin 拥有全部权限，无需插入；此处仅为幂等兜底）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id FROM sys_role r, sys_menu m
WHERE r.role_key='admin' AND m.perms IN ('system:mail:config','system:mail:template','system:reader:resetPwd')
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id=r.role_id AND rm.menu_id=m.menu_id);

-- ---------- 7. sys_config：会话时长与短信通道开关 ----------
INSERT INTO `sys_config` (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '读者端会话时长（分钟）', 'reader.session.minutes', '20160', 'Y', 'admin', NOW(), '前台读者会话滑动有效期（默认 20160=14 天，每次请求自动续期）'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key='reader.session.minutes');

INSERT INTO `sys_config` (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '短信验证码通道开关', 'sms.enabled', 'false', 'Y', 'admin', NOW(), '预留：接入短信服务商后置 true，验证码自动支持手机通道'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key='sms.enabled');
