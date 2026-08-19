-- ============================================
-- 万事屋 · 角色权限分级初始化（幂等，可重复执行）
-- 角色：图书管理员 / 收银专员 / 统计访客
-- 参数：按读者类型的借阅上限与借期
-- ============================================
USE ry-vue;

-- ---------- 角色 ----------
INSERT INTO sys_role (role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, create_by, create_time, remark)
SELECT '图书管理员','librarian',2,'1','1','1','0','admin',NOW(),'图书业务日常管理（无订单/无系统管理）' WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_key='librarian');
INSERT INTO sys_role (role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, create_by, create_time, remark)
SELECT '收银专员','cashier',3,'1','1','1','0','admin',NOW(),'门店收银：订单处理与借还书办理' WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_key='cashier');
INSERT INTO sys_role (role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, create_by, create_time, remark)
SELECT '统计访客','viewer',4,'1','1','1','0','admin',NOW(),'只读：查看数据看板与借阅统计' WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_key='viewer');

-- ---------- 角色菜单分配（按菜单名动态关联） ----------
-- 图书管理员：图书管理目录 + 图书信息全套 + 读者服务目录 + 读者管理全套 + 读者登记 + 借阅记录全套 + 借阅导出 + 借阅统计
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id FROM sys_role r, sys_menu m
WHERE r.role_key='librarian' AND m.menu_name IN
('图书业务','图书管理','图书信息','图书信息查询','图书信息新增','图书信息修改','图书信息删除','图书信息导出',
 '读者服务','读者管理','读者管理查询','读者管理新增','读者管理修改','读者管理删除','读者管理导出','读者登记',
 '借阅记录','借阅记录查询','借阅记录新增','借阅记录修改','借阅记录删除','借阅导出','借阅统计')
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id=r.role_id AND rm.menu_id=m.menu_id);

-- 收银专员：图书业务目录 + 经营管理目录(订单全套) + 图书管理目录(图书/借阅查看) + 读者查看
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id FROM sys_role r, sys_menu m
WHERE r.role_key='cashier' AND m.menu_name IN
('图书业务','经营管理','订单管理','订单查询','订单修改','订单删除',
 '图书管理','借阅记录','借阅记录查询','借阅记录新增','借阅记录修改',
 '图书信息','图书信息查询','读者服务','读者管理','读者管理查询')
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id=r.role_id AND rm.menu_id=m.menu_id);

-- 统计访客：图书业务目录 + 经营管理目录 + 借阅统计（只读）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id FROM sys_role r, sys_menu m
WHERE r.role_key='viewer' AND m.menu_name IN ('图书业务','经营管理','借阅统计')
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id=r.role_id AND rm.menu_id=m.menu_id);

-- ---------- 测试用户（密码与 admin 相同 admin123，复用其 BCrypt 哈希） ----------
SET @pwd = (SELECT password FROM sys_user WHERE user_name='admin' LIMIT 1);
INSERT INTO sys_user (dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, login_ip, login_date, create_by, create_time, remark)
SELECT 101, 'librarian', '图书管理员', '00', '', '', '0', '', @pwd, '0', '0', '127.0.0.1', NOW(), 'admin', NOW(), '图书管理角色测试账号' WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE user_name='librarian');
INSERT INTO sys_user (dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, login_ip, login_date, create_by, create_time, remark)
SELECT 104, 'cashier', '收银专员', '00', '', '', '0', '', @pwd, '0', '0', '127.0.0.1', NOW(), 'admin', NOW(), '收银角色测试账号' WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE user_name='cashier');
INSERT INTO sys_user (dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, login_ip, login_date, create_by, create_time, remark)
SELECT 102, 'viewer', '统计访客', '00', '', '', '0', '', @pwd, '0', '0', '127.0.0.1', NOW(), 'admin', NOW(), '统计只读角色测试账号' WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE user_name='viewer');

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

-- ---------- 按读者类型的借阅规则参数（P2） ----------
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '学生借阅上限','book.borrow.maxCount.student','5','Y','admin',NOW(),'学生读者最多同时借阅本数' WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key='book.borrow.maxCount.student');
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '教师借阅上限','book.borrow.maxCount.teacher','10','Y','admin',NOW(),'教师读者最多同时借阅本数' WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key='book.borrow.maxCount.teacher');
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '普通读者借阅上限','book.borrow.maxCount.normal','3','Y','admin',NOW(),'普通读者最多同时借阅本数' WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key='book.borrow.maxCount.normal');
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '学生借期天数','book.borrow.days.student','30','Y','admin',NOW(),'学生读者借期天数' WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key='book.borrow.days.student');
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '教师借期天数','book.borrow.days.teacher','60','Y','admin',NOW(),'教师读者借期天数' WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key='book.borrow.days.teacher');
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '普通读者借期天数','book.borrow.days.normal','30','Y','admin',NOW(),'普通读者借期天数' WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key='book.borrow.days.normal');
