-- ============================================
-- 数智游民创新工场 · 角色权限分级初始化（幂等，可重复执行）
-- 角色：超级管理员(admin，基表预置) / 内容编辑(editor)
-- 参数：按成员类型的报名上限与报名期限
-- 说明：无 USE 语句，导入时通过命令行指定目标库（与 business_init.sql 一致）
-- 说明：仅预置 2 个角色；本文件只补「内容编辑」角色及其测试账号，
--       基表预置的 common 角色与 ry 演示账号由 upgrade_20260824_roles.sql 幂等清理
-- ============================================

-- ---------- 内容编辑角色 ----------
INSERT INTO sys_role (role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, create_by, create_time, remark)
SELECT '内容编辑','editor',2,'1','1','1','0','admin',NOW(),'官网内容编辑：文章管理/服务信息/成员/报名维护（无订单/无系统管理）' WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_key='editor');

-- ---------- 角色菜单分配（按菜单名动态关联；幂等） ----------
-- 内容编辑：内容运营/成员与报名（新目录结构）下的文章/服务/成员/报名维护。
-- 注意：新顶层目录（内容运营/成员与报名）与缺失的按钮权限菜单由
--       upgrade_20260825_editor_fix.sql 补建并授予——该脚本必须在 menu_reorg 之后执行
--       （全新库 mysql-init.sh / 旧卷 mysql-upgrade.sh / start-all.bat 均已挂载），
--       此处仅授此时已存在的菜单（角色定义语义在此文件完整声明）。
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id FROM sys_role r, sys_menu m
WHERE r.role_key='editor' AND m.menu_name IN
('服务信息','成员管理','活动预约','报名管理','报名导出')
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id=r.role_id AND rm.menu_id=m.menu_id);

-- ---------- 测试账号（内容编辑；密码与 admin 相同 admin123，复用其 BCrypt 哈希） ----------
SET @pwd = (SELECT password FROM sys_user WHERE user_name='admin' LIMIT 1);
INSERT INTO sys_user (dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, login_ip, login_date, create_by, create_time, remark)
SELECT 101, 'editor', '内容编辑', '00', '', '', '0', '', @pwd, '0', '0', '127.0.0.1', NOW(), 'admin', NOW(), '内容编辑角色测试账号' WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE user_name='editor');

-- ---------- 用户角色关联 ----------
INSERT INTO sys_user_role (user_id, role_id)
SELECT u.user_id, r.role_id FROM sys_user u, sys_role r
WHERE u.user_name='editor' AND r.role_key='editor'
AND NOT EXISTS (SELECT 1 FROM sys_user_role ur WHERE ur.user_id=u.user_id AND ur.role_id=r.role_id);

-- ---------- 按成员类型的报名规则参数（P2，config_key 不动） ----------
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '个人主理人报名上限','book.borrow.maxCount.student','5','Y','admin',NOW(),'个人主理人成员最多同时报名服务数' WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key='book.borrow.maxCount.student');
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '团队报名上限','book.borrow.maxCount.teacher','10','Y','admin',NOW(),'团队成员最多同时报名服务数' WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key='book.borrow.maxCount.teacher');
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '企业报名上限','book.borrow.maxCount.normal','3','Y','admin',NOW(),'企业成员最多同时报名服务数' WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key='book.borrow.maxCount.normal');
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '个人主理人报名期限','book.borrow.days.student','30','Y','admin',NOW(),'个人主理人成员报名期限(天)' WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key='book.borrow.days.student');
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '团队报名期限','book.borrow.days.teacher','60','Y','admin',NOW(),'团队成员报名期限(天)' WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key='book.borrow.days.teacher');
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '企业报名期限','book.borrow.days.normal','30','Y','admin',NOW(),'企业成员报名期限(天)' WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key='book.borrow.days.normal');
