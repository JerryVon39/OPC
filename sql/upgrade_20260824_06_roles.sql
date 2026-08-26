-- ============================================
-- 升级脚本：角色精简为仅 2 个预置角色（2026-08-24）
-- 数智游民创新工场 · 系统角色收敛
-- 目标：预置角色只保留「超级管理员(admin)」+「内容编辑(editor)」
--       存量库 role_init.sql 曾预置 librarian/cashier/viewer 三角色及测试账号，
--       一并收敛：librarian 更名为 editor（保留 role_id 与其已授权的内容菜单），
--       cashier/viewer 及若依基础角色 common 与对应测试账号（ry/cashier/viewer）清理
-- 幂等：可重复执行；全新库（mysql-init.sh 含本脚本）与存量库均适用
-- 安全：admin/admin123 登录不受影响（步骤 5 幂等保障）；注册已关闭（sys.account.registerUser=false）
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p ry-vue < sql/upgrade_20260824_roles.sql
-- ============================================

USE `ry-vue`;

-- ---------- 1. 内容编辑角色：librarian → editor（保留 role_id 与已授权菜单） ----------
-- 存量库：librarian(role_id=100) 已授予 CMS/服务/成员等全套内容菜单，直接更名，
--         sys_role_menu、sys_user_role 按 role_id 关联，不受 role_key 变更影响。
UPDATE sys_role
SET role_name='内容编辑', role_key='editor',
    remark='官网内容编辑：文章管理/服务信息/成员/报名维护（无订单/无系统管理）',
    update_by='admin', update_time=NOW()
WHERE role_key='librarian';

-- 兜底：role_key='editor' 不存在则补建（全新库由 role_init.sql 已建，此处幂等）
INSERT INTO sys_role (role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, create_by, create_time, remark)
SELECT '内容编辑','editor',2,'1','1','1','0','admin',NOW(),'官网内容编辑：文章管理/服务信息/成员/报名维护（无订单/无系统管理）'
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_key='editor');

-- ---------- 2. 内容编辑测试账号：librarian → editor（密码与 admin 相同，复用其 BCrypt 哈希） ----------
UPDATE sys_user
SET user_name='editor', nick_name='内容编辑', remark='内容编辑角色测试账号',
    update_by='admin', update_time=NOW()
WHERE user_name='librarian';

SET @pwd = (SELECT password FROM sys_user WHERE user_name='admin' LIMIT 1);
INSERT INTO sys_user (dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, login_ip, login_date, create_by, create_time, remark)
SELECT 101, 'editor', '内容编辑', '00', '', '', '0', '', @pwd, '0', '0', '127.0.0.1', NOW(), 'admin', NOW(), '内容编辑角色测试账号'
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE user_name='editor');

-- ---------- 3. 清理多余角色（cashier/viewer/common）及其关联 ----------
-- 3.1 先删用户-角色绑定（按 role_key 关联）
DELETE ur FROM sys_user_role ur
JOIN sys_role r ON r.role_id=ur.role_id
WHERE r.role_key IN ('cashier','viewer','common');

-- 3.2 删角色-菜单绑定
DELETE rm FROM sys_role_menu rm
JOIN sys_role r ON r.role_id=rm.role_id
WHERE r.role_key IN ('cashier','viewer','common');

-- 3.3 删角色本身
DELETE FROM sys_role WHERE role_key IN ('cashier','viewer','common');

-- ---------- 4. 清理多余测试账号（cashier/viewer/ry） ----------
-- 4.1 先删用户-岗位绑定
DELETE up FROM sys_user_post up
JOIN sys_user u ON u.user_id=up.user_id
WHERE u.user_name IN ('cashier','viewer','ry');

-- 4.2 再删用户-角色绑定（按 user_name 兜底）
DELETE ur FROM sys_user_role ur
JOIN sys_user u ON u.user_id=ur.user_id
WHERE u.user_name IN ('cashier','viewer','ry');

-- 4.3 删用户本身
DELETE FROM sys_user WHERE user_name IN ('cashier','viewer','ry');

-- ---------- 5. 一致性保障：超级管理员(admin)与 admin 绑定必须完好（幂等保护） ----------
INSERT INTO sys_role (role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, create_by, create_time, remark)
SELECT '超级管理员','admin',1,'1','1','1','0','admin',NOW(),'超级管理员'
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_key='admin');

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.user_id, r.role_id FROM sys_user u, sys_role r
WHERE u.user_name='admin' AND r.role_key='admin'
AND NOT EXISTS (SELECT 1 FROM sys_user_role ur WHERE ur.user_id=u.user_id AND ur.role_id=r.role_id);

-- ---------- 6. 角色菜单保障：内容编辑必须拥有核心内容菜单（幂等） ----------
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id FROM sys_role r, sys_menu m
WHERE r.role_key='editor' AND m.menu_name IN
('官网运营','服务管理','服务信息','服务信息查询','服务信息新增','服务信息修改','服务信息删除','服务信息导出',
 '成员服务','成员管理','成员管理查询','成员管理新增','成员管理修改','成员管理删除','成员管理导出','活动预约',
 '报名管理','报名管理查询','报名管理新增','报名管理修改','报名管理删除','报名导出',
 'CMS 管理','文章管理','文章查询','文章新增','文章修改','文章删除','文章发布')
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id=r.role_id AND rm.menu_id=m.menu_id);