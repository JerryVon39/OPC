-- ============================================
-- 升级脚本：隐藏服务管理入口 v20260825（前台服务业务已关停）
-- 背景：前台服务入口已全部移除（service.html 无引用、services.html/contest.html 已删），
--       后台"服务信息 /content/book"与"服务回收站 /content/recycle/book"失去业务用途。
-- 处理：菜单停用（visible='1'，数据保留可随时恢复，不改代码不删表）
-- 幂等：UPDATE 按 menu_id 定位
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p ry-vue < sql/upgrade_20260825_hide_book_menu.sql
-- ============================================

USE ry-vue;

-- 1. 停用服务业务菜单（数据保留可随时恢复）：
--    服务信息（内容运营下）、服务回收站（回收站目录下）
--    活动预约 / 报名管理 / 入驻申请（成员与报名下，管理 book 系业务数据）
-- 修复（2026-08-27）：原按硬编码 menu_id 定位——全新库 menu_id 自增起点不同导致错杀/漏杀
--（docker 全新部署曾把"区块修改/区块删除"等隐藏、漏掉服务业务菜单）；改按 menu_name 定位，幂等且移植安全。
UPDATE sys_menu SET visible = '1', update_by = 'admin', update_time = NOW()
WHERE menu_name IN ('服务信息', '活动预约', '报名管理', '入驻申请', '服务回收站');

-- 完成提示
SELECT menu_id, menu_name, visible FROM sys_menu WHERE menu_name IN ('服务信息', '活动预约', '报名管理', '入驻申请', '服务回收站') ORDER BY menu_id;
