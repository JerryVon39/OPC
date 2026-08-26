-- ============================================
-- 升级脚本：运营工作台 + 站点配置 + 角色收敛 v20260825（第三批）
-- 内容：sys_config 预置站点键（联系方式，前台公开可读）
--       + 后台菜单（运营工作台 顶层目录+子页、使用帮助）
--       + 新角色 运营专员（operator）= 工作台 + 成员与报名 + 运营辅助
--       + 内容编辑（editor）补挂 工作台 + 使用帮助
-- 适用：存量库（在 upgrade_20260825_cms_block.sql 之后执行）；全新库直接执行
-- 幂等：可重复执行；键/菜单/角色 INSERT...SELECT WHERE NOT EXISTS，角色菜单按 role_id+menu_id 判重
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p ry-vue < sql/upgrade_20260825_ops_workbench.sql
-- ============================================

USE ry-vue;

-- ============================================
-- 1. 站点配置键（前台 footer/联系区通过 /system/config/configKey/{key} 匿名读取）
-- ============================================
INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time)
SELECT '站点电话（前台展示）', 'site_phone', '0763-3391888', 'Y', '前台 footer/联系区展示，修改后前台即时生效', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key='site_phone');
INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time)
SELECT '站点邮箱', 'site_email', '', 'Y', '前台备用展示（当前页面未展示可留空）', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key='site_email');
INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time)
SELECT '站点地址（前台展示）', 'site_address', '清远国家高新技术产业开发区天安智谷产业园 B6 栋、T1 栋 1105', 'Y', '前台 footer 地址展示', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key='site_address');
INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time)
SELECT '公众号/视频号（前台展示）', 'site_wechat', '互动世界 ｜ 视频号：互动AI世界', 'Y', '前台 footer 公众号展示', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key='site_wechat');
INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time)
SELECT '公众号二维码图（预留）', 'site_qrcode', '', 'Y', '预留：二维码图片路径，当前页面未展示', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key='site_qrcode');

-- ============================================
-- 2. 后台菜单：运营工作台（顶层目录 order_num=0 + 子页，登录后默认落地）
-- ============================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '运营工作台', 0, 0, 'ops', '', 1, 0, 'M', '0', '0', '', 'dashboard', 'admin', NOW(), '运营人员首页（登录默认落地）'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='运营工作台' AND menu_type='M');
SELECT menu_id INTO @ops_dir FROM sys_menu WHERE menu_name='运营工作台' AND menu_type='M' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '工作台', @ops_dir, 1, 'index', 'index', 1, 0, 'C', '0', '0', '', 'dashboard', 'admin', NOW(), '运营工作台（复用登录落地页 views/index.vue，增强文章数据卡与最近编辑）'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='工作台');

-- ============================================
-- 3. 后台菜单：使用帮助（C 页，挂「运营辅助」order_num=4）
-- ============================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '运营辅助', 0, 3, 'ops', '', 1, 0, 'M', '0', '0', '', 'delete', 'admin', NOW(), '误删恢复等辅助功能'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='运营辅助' AND menu_type='M');
SELECT menu_id INTO @ops_aux FROM sys_menu WHERE menu_name='运营辅助' AND menu_type='M' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '使用帮助', @ops_aux, 4, 'help', 'system/help/index', 1, 0, 'C', '0', '0', '', 'question', 'admin', NOW(), '图文操作手册'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='使用帮助');

-- ============================================
-- 4. 新角色：运营专员（operator）= 工作台 + 成员与报名 + 运营辅助（不含内容运营/系统设置）
-- ============================================
INSERT INTO sys_role (role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, create_by, create_time, remark)
SELECT '运营专员', 'operator', 3, '1', '1', '1', '0', 'admin', NOW(), '报名/入驻审批与数据恢复（不含内容与系统设置）'
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_key='operator');
SELECT role_id INTO @op_role FROM sys_role WHERE role_key='operator' LIMIT 1;

-- 4.1 operator 菜单：运营工作台目录+子页、成员与报名目录+4 子页、运营辅助目录+3 回收站+使用帮助
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT @op_role, m.menu_id FROM sys_menu m
WHERE m.menu_name IN ('运营工作台','工作台',
  '成员与报名','成员管理','活动预约','报名管理','入驻申请',
  '运营辅助','图书回收站','读者回收站','文章回收站','使用帮助')
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id=@op_role AND rm.menu_id=m.menu_id);

-- 4.2 editor 补挂：运营工作台目录+子页、使用帮助（内容编辑仍只见内容运营侧）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id FROM sys_role r, sys_menu m
WHERE r.role_key IN ('editor') AND m.menu_name IN ('运营工作台','工作台','使用帮助')
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id=r.role_id AND rm.menu_id=m.menu_id);

-- ============================================
-- 5. 完成提示
-- ============================================
SELECT CONCAT('第三批就绪：站点配置键 ',
              (SELECT COUNT(*) FROM sys_config WHERE config_key LIKE 'site_%'),
              ' 个，运营专员角色 ',
              (SELECT COUNT(*) FROM sys_role WHERE role_key='operator'),
              ' 个，工作台菜单 ',
              (SELECT COUNT(*) FROM sys_menu WHERE menu_name='工作台'),
              ' 个') AS result;
