-- ============================================
-- 固化「站点设置」菜单（2026-08-27 第九轮部署对齐）
-- 背景：站点设置页（system/site/index，配置 site.nav/site.footer.* 等键）此前
--       仅在本机后台人工创建，未入任何升级脚本——docker 全新部署缺该菜单。
--       本脚本按名称幂等创建（挂「系统设置」目录下）。
-- 幂等：INSERT...SELECT WHERE NOT EXISTS
-- ============================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '站点设置', (SELECT menu_id FROM sys_menu WHERE menu_name='系统设置' AND menu_type='M' LIMIT 1), 1, 'site', 'system/site/index', 1, 0, 'C', '0', '0', 'system:config:list', 'setting', 'admin', NOW(), '前台站点配置（导航/页脚/前台地址，site.* 键）'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='站点设置' AND menu_type='C');
