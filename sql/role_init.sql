-- ============================================
-- 数智游民创新工场 · 角色权限分级初始化（幂等，可重复执行）
-- 角色：服务运营专员 / 合作运营专员 / 运营访客
-- 参数：按成员类型的报名上限与报名期限
-- 说明：无 USE 语句，导入时通过命令行指定目标库（与 business_init.sql 一致）
-- ============================================

-- ---------- 角色 ----------
INSERT INTO sys_role (role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, create_by, create_time, remark)
SELECT '服务运营专员','librarian',2,'1','1','1','0','admin',NOW(),'官网业务日常管理（无订单/无系统管理）' WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_key='librarian');
INSERT INTO sys_role (role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, create_by, create_time, remark)
SELECT '合作运营专员','cashier',3,'1','1','1','0','admin',NOW(),'合作经营：入驻申请处理与活动报名办理' WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_key='cashier');
INSERT INTO sys_role (role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, create_by, create_time, remark)
SELECT '运营访客','viewer',4,'1','1','1','0','admin',NOW(),'只读：查看数据看板与报名统计' WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_key='viewer');

-- ---------- 角色菜单分配（按菜单名动态关联） ----------
-- 服务运营专员：服务管理目录 + 服务信息全套 + 成员服务目录 + 成员管理全套 + 活动预约 + 报名管理全套 + 报名导出
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id FROM sys_role r, sys_menu m
WHERE r.role_key='librarian' AND m.menu_name IN
('官网运营','服务管理','服务信息','服务信息查询','服务信息新增','服务信息修改','服务信息删除','服务信息导出',
 '成员服务','成员管理','成员管理查询','成员管理新增','成员管理修改','成员管理删除','成员管理导出','活动预约',
 '报名管理','报名管理查询','报名管理新增','报名管理修改','报名管理删除','报名导出')
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id=r.role_id AND rm.menu_id=m.menu_id);

-- 合作运营专员：官网运营目录 + 合作经营目录 + 活动预约 + 服务管理目录(报名/服务查看) + 成员查看
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id FROM sys_role r, sys_menu m
WHERE r.role_key='cashier' AND m.menu_name IN
('官网运营','合作经营','活动预约',
 '服务管理','报名管理','报名管理查询','报名管理新增','报名管理修改',
 '服务信息','服务信息查询','成员服务','成员管理','成员管理查询')
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id=r.role_id AND rm.menu_id=m.menu_id);

-- 运营访客：官网运营目录 + 合作经营目录 + 入驻申请（只读）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id FROM sys_role r, sys_menu m
WHERE r.role_key='viewer' AND m.menu_name IN ('官网运营','合作经营','入驻申请')
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id=r.role_id AND rm.menu_id=m.menu_id);

-- ---------- 测试用户（密码与 admin 相同 admin123，复用其 BCrypt 哈希） ----------
SET @pwd = (SELECT password FROM sys_user WHERE user_name='admin' LIMIT 1);
INSERT INTO sys_user (dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, login_ip, login_date, create_by, create_time, remark)
SELECT 101, 'librarian', '服务运营专员', '00', '', '', '0', '', @pwd, '0', '0', '127.0.0.1', NOW(), 'admin', NOW(), '服务运营角色测试账号' WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE user_name='librarian');
INSERT INTO sys_user (dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, login_ip, login_date, create_by, create_time, remark)
SELECT 104, 'cashier', '合作运营专员', '00', '', '', '0', '', @pwd, '0', '0', '127.0.0.1', NOW(), 'admin', NOW(), '合作运营角色测试账号' WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE user_name='cashier');
INSERT INTO sys_user (dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, login_ip, login_date, create_by, create_time, remark)
SELECT 102, 'viewer', '运营访客', '00', '', '', '0', '', @pwd, '0', '0', '127.0.0.1', NOW(), 'admin', NOW(), '运营访客角色测试账号' WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE user_name='viewer');

-- ---------- 用户角色关联 ----------
INSERT INTO sys_user_role (user_id, role_id)
SELECT u.user_id, r.role_id FROM sys_user u, sys_role r
WHERE u.user_name='librarian' AND r.role_key='librarian'
AND NOT EXISTS (SELECT 1 FROM sys_user_role ur WHERE ur.user_id=u.user_id AND ur.role_id=r.role_id);
INSERT INTO sys_user_role (user_id, role_id)
SELECT u.user_id, r.role_id FROM sys_user u, sys_role r
WHERE u.user_name='cashier' AND r.role_key='cashier'
AND NOT EXISTS (SELECT 1 FROM sys_user_role ur WHERE ur.user_id=u.user_id AND ur.role_id=r.role_id);
INSERT INTO sys_user_role (user_id, role_id)
SELECT u.user_id, r.role_id FROM sys_user u, sys_role r
WHERE u.user_name='viewer' AND r.role_key='viewer'
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
