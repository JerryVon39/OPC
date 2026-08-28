-- ============================================
-- 升级脚本：editor（内容编辑）角色修复 v20260825
-- 数智游民创新工场 · 后台角色
-- 问题：
--   1. upgrade_20260825_menu_reorg.sql 新建顶层目录（内容运营/成员与报名）后无任何脚本
--      授予 editor，原授予的旧目录（官网运营/服务管理/成员服务/合作经营/CMS 管理）被
--      status='1' 隐藏 → 子菜单因父级不在授权列表被 getChildPerms 丢弃，editor 侧边栏为空；
--   2. role_init.sql 授予的 15 个按钮权限菜单（服务信息查询 等）在所有 SQL 中均无创建语句，
--      INSERT..SELECT 匹配不到行静默无效 → 即使目录修好，增删改也全部 403。
-- 方案：补建缺失按钮菜单（F 级，幂等）+ 按新结构重新授予 editor，并清理旧目录授予。
-- 幂等：全部 INSERT..SELECT WHERE NOT EXISTS / DELETE 按角色+菜单名定位，可重复执行。
-- 执行顺序：必须在 upgrade_20260825_menu_reorg.sql 之后（依赖其目录结构）；
--           全新库由 docker/mysql-init.sh 调用，旧卷由 docker/mysql-upgrade.sh 调用。
-- ============================================


-- 1. 补建缺失的按钮权限菜单（menu_type='F'，幂等）
-- 1.1 服务信息按钮（挂「服务信息」下；perms 与 BookController 注解一致）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '服务信息查询', m.menu_id, 1, '', '', 1, 0, 'F', '0', '0', 'system:book:query', '#', 'admin', NOW(), '服务信息查询按钮'
FROM sys_menu m WHERE m.menu_name='服务信息'
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='服务信息查询');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '服务信息新增', m.menu_id, 2, '', '', 1, 0, 'F', '0', '0', 'system:book:add', '#', 'admin', NOW(), '服务信息新增按钮'
FROM sys_menu m WHERE m.menu_name='服务信息'
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='服务信息新增');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '服务信息修改', m.menu_id, 3, '', '', 1, 0, 'F', '0', '0', 'system:book:edit', '#', 'admin', NOW(), '服务信息修改按钮'
FROM sys_menu m WHERE m.menu_name='服务信息'
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='服务信息修改');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '服务信息删除', m.menu_id, 4, '', '', 1, 0, 'F', '0', '0', 'system:book:remove', '#', 'admin', NOW(), '服务信息删除按钮'
FROM sys_menu m WHERE m.menu_name='服务信息'
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='服务信息删除');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '服务信息导出', m.menu_id, 5, '', '', 1, 0, 'F', '0', '0', 'system:book:export', '#', 'admin', NOW(), '服务信息导出按钮'
FROM sys_menu m WHERE m.menu_name='服务信息'
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='服务信息导出');

-- 1.2 成员管理按钮（挂「成员管理」下；perms 与 ReaderController 注解一致）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '成员管理查询', m.menu_id, 1, '', '', 1, 0, 'F', '0', '0', 'system:reader:query', '#', 'admin', NOW(), '成员管理查询按钮'
FROM sys_menu m WHERE m.menu_name='成员管理'
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='成员管理查询');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '成员管理新增', m.menu_id, 2, '', '', 1, 0, 'F', '0', '0', 'system:reader:add', '#', 'admin', NOW(), '成员管理新增按钮'
FROM sys_menu m WHERE m.menu_name='成员管理'
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='成员管理新增');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '成员管理修改', m.menu_id, 3, '', '', 1, 0, 'F', '0', '0', 'system:reader:edit', '#', 'admin', NOW(), '成员管理修改按钮'
FROM sys_menu m WHERE m.menu_name='成员管理'
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='成员管理修改');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '成员管理删除', m.menu_id, 4, '', '', 1, 0, 'F', '0', '0', 'system:reader:remove', '#', 'admin', NOW(), '成员管理删除按钮'
FROM sys_menu m WHERE m.menu_name='成员管理'
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='成员管理删除');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '成员管理导出', m.menu_id, 5, '', '', 1, 0, 'F', '0', '0', 'system:reader:export', '#', 'admin', NOW(), '成员管理导出按钮'
FROM sys_menu m WHERE m.menu_name='成员管理'
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='成员管理导出');

-- 1.3 报名管理按钮（挂「报名管理」下；perms 与 BorrowRecordController 注解一致）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '报名管理查询', m.menu_id, 1, '', '', 1, 0, 'F', '0', '0', 'system:borrow:query', '#', 'admin', NOW(), '报名管理查询按钮'
FROM sys_menu m WHERE m.menu_name='报名管理'
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='报名管理查询');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '报名管理新增', m.menu_id, 2, '', '', 1, 0, 'F', '0', '0', 'system:borrow:add', '#', 'admin', NOW(), '报名管理新增按钮'
FROM sys_menu m WHERE m.menu_name='报名管理'
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='报名管理新增');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '报名管理修改', m.menu_id, 3, '', '', 1, 0, 'F', '0', '0', 'system:borrow:edit', '#', 'admin', NOW(), '报名管理修改按钮'
FROM sys_menu m WHERE m.menu_name='报名管理'
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='报名管理修改');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '报名管理删除', m.menu_id, 4, '', '', 1, 0, 'F', '0', '0', 'system:borrow:remove', '#', 'admin', NOW(), '报名管理删除按钮'
FROM sys_menu m WHERE m.menu_name='报名管理'
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='报名管理删除');

-- 2. 清理 editor 对旧隐藏目录/父级的授予（避免死授权残留）
DELETE rm FROM sys_role_menu rm
JOIN sys_role r ON rm.role_id = r.role_id
JOIN sys_menu m ON rm.menu_id = m.menu_id
WHERE r.role_key = 'editor'
  AND m.menu_name IN ('官网运营','服务管理','成员服务','合作经营','CMS 管理','CMS 分类管理');

-- 3. 按新结构重新授予 editor（内容运营 + 成员与报名 两目录及其子菜单/按钮；不含系统设置/运营辅助）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id FROM sys_role r, sys_menu m
WHERE r.role_key = 'editor'
  AND m.menu_name IN (
    '内容运营',
    '服务信息','服务信息查询','服务信息新增','服务信息修改','服务信息删除','服务信息导出',
    '文章管理','文章查询','文章新增','文章修改','文章删除','文章发布',
    '成员与报名',
    '成员管理','成员管理查询','成员管理新增','成员管理修改','成员管理删除','成员管理导出',
    '活动预约',
    '报名管理','报名管理查询','报名管理新增','报名管理修改','报名管理删除','报名导出'
  )
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = r.role_id AND rm.menu_id = m.menu_id);

-- 4. 兜底：editor 测试账号与角色关联（幂等）
INSERT INTO sys_user_role (user_id, role_id)
SELECT u.user_id, r.role_id FROM sys_user u, sys_role r
WHERE u.user_name='editor' AND r.role_key='editor'
AND NOT EXISTS (SELECT 1 FROM sys_user_role ur WHERE ur.user_id=u.user_id AND ur.role_id=r.role_id);
