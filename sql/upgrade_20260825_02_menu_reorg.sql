-- ============================================
-- 升级脚本：后台菜单重新分类（业务导向四分组）v20260825
-- 数智游民创新工场 · 后台功能整理
-- 背景：原菜单沿用若依默认结构（系统管理/系统监控/系统工具）+ 官网运营，
--       混杂大量日常用不上的开发/监控功能；订单管理为图书系统残留（零数据、未业务化）。
-- 目标结构：
--   内容运营   ：服务信息 / 官网轮播 / 文章管理 / 通知公告
--   成员与报名 ：成员管理 / 活动预约 / 报名管理 / 入驻申请
--   运营辅助   ：回收站（图书回收站 / 读者回收站）
--   系统设置   ：用户 / 角色 / 菜单 / 字典 / 参数 / 邮件通知 / 定时任务 / 操作日志 / 登录日志
-- 隐藏（status='1'，代码与数据保留，可随时恢复）：
--   部门管理 / 岗位管理 / 在线用户 / 数据监控 / 服务监控 / 缓存监控 / 缓存列表
--   表单构建 / 代码生成 / 系统接口 / 系统监控 / 系统工具
--   原「官网运营」空分组（服务管理/成员服务/合作经营/CMS管理）
-- 幂等：INSERT 带 WHERE NOT EXISTS，UPDATE 天然幂等；父级 id 用会话变量取（规避
--       MySQL 1093「UPDATE 目标表不能出现在 FROM 子查询」），可重复执行。
-- 执行顺序：可与 upgrade_20260825_recycle_menu.sql 任意先后（本脚本自包含回收站恢复逻辑）。
-- ============================================


-- 1. 新建三个业务顶层菜单（幂等）并缓存父级 id
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '内容运营', 0, 1, 'content', '', 1, 0, 'M', '0', '0', '', 'content', 'admin', NOW(), '前台内容管理'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='内容运营' AND menu_type='M');
SELECT menu_id INTO @content FROM sys_menu WHERE menu_name='内容运营' AND menu_type='M' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '成员与报名', 0, 2, 'member', '', 1, 0, 'M', '0', '0', '', 'peoples', 'admin', NOW(), '成员与报名管理'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='成员与报名' AND menu_type='M');
SELECT menu_id INTO @member FROM sys_menu WHERE menu_name='成员与报名' AND menu_type='M' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '运营辅助', 0, 3, 'ops', '', 1, 0, 'M', '0', '0', '', 'delete', 'admin', NOW(), '误删恢复等辅助功能'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='运营辅助' AND menu_type='M');
SELECT menu_id INTO @ops FROM sys_menu WHERE menu_name='运营辅助' AND menu_type='M' LIMIT 1;

-- 2. 「系统管理」更名「系统设置」并后置（order_num 4），缓存父级 id
UPDATE sys_menu SET menu_name='系统设置', order_num=4 WHERE menu_name='系统管理' AND menu_type='M';
SELECT menu_id INTO @settings FROM sys_menu WHERE menu_name='系统设置' AND menu_type='M' LIMIT 1;

-- 3. 子菜单归位（按名称动态定位，父级 id 来自会话变量）
-- 3.1 → 内容运营
UPDATE sys_menu SET parent_id=@content, order_num=1 WHERE menu_name='服务信息';
UPDATE sys_menu SET parent_id=@content, order_num=2 WHERE menu_name='官网轮播';
UPDATE sys_menu SET parent_id=@content, order_num=3 WHERE menu_name='文章管理';
UPDATE sys_menu SET parent_id=@content, order_num=4 WHERE menu_name='通知公告';
-- 3.2 → 成员与报名
UPDATE sys_menu SET parent_id=@member, order_num=1 WHERE menu_name='成员管理';
UPDATE sys_menu SET parent_id=@member, order_num=2 WHERE menu_name='活动预约';
UPDATE sys_menu SET parent_id=@member, order_num=3 WHERE menu_name='报名管理';
UPDATE sys_menu SET parent_id=@member, order_num=4 WHERE menu_name='入驻申请';
-- 3.3 → 运营辅助：回收站（含兜底重建，逻辑与 upgrade_20260825_recycle_menu.sql 一致）
UPDATE sys_menu SET status='0' WHERE menu_name='回收站' AND menu_type='M';
UPDATE sys_menu SET status='0', perms='system:book:remove' WHERE menu_name='图书回收站';
UPDATE sys_menu SET status='0', perms='system:reader:remove' WHERE menu_name='读者回收站';
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '回收站', @content, 1, 'recycle', '', 1, 0, 'M', '0', '0', '', 'delete', 'admin', NOW(), '误删数据恢复（两态）'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='回收站');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '图书回收站', @ops, 1, 'book', 'system/recycle/book', 1, 0, 'C', '0', '0', 'system:book:remove', 'book', 'admin', NOW(), '误删服务恢复（两态）'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='图书回收站');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '读者回收站', @ops, 2, 'reader', 'system/recycle/reader', 1, 0, 'C', '0', '0', 'system:reader:remove', 'peoples', 'admin', NOW(), '误删成员恢复（两态）'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='读者回收站');
UPDATE sys_menu SET parent_id=@ops WHERE menu_name IN ('回收站','图书回收站','读者回收站');
-- 3.4 → 系统设置：定时任务（从系统监控移入）
UPDATE sys_menu SET parent_id=@settings, order_num=11 WHERE menu_name='定时任务';

-- 4. 隐藏不必要功能（代码与数据保留，随时可恢复）
UPDATE sys_menu SET status='1' WHERE menu_name IN (
  '部门管理','岗位管理',
  '在线用户','数据监控','服务监控','缓存监控','缓存列表',
  '表单构建','代码生成','系统接口',
  '系统监控','系统工具',
  '服务管理','成员服务','合作经营','CMS 管理'
);

-- 5. 原「官网运营」空壳隐藏（子菜单已全部迁出）
UPDATE sys_menu SET status='1' WHERE menu_name='官网运营' AND menu_type='M';
