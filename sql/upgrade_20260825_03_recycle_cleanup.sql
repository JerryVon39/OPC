-- ============================================
-- 升级脚本：回收站快照死代码清理 + 白屏修复 v20260825
-- 数智游民创新工场 · 运营辅助模块整理
-- 背景：
--   1) book_recycle/reader_recycle 快照表为三态回收站残余——从未写入、从未读取，
--      前端回收站页已改接两态接口（del_flag 软删），后端 RecycleController 已删除；
--   2) 「回收站」M 菜单（path=recycle）因无子级被若依渲染为可点击路由，路由无组件
--      → 后台点击「回收站」白屏。修复：删除该目录菜单，图书/读者回收站直接挂
--      「运营辅助」下（扁平结构）。
-- 幂等：DROP TABLE IF EXISTS / DELETE 天然幂等；sys_role_menu 关联一并清理。
-- ============================================


-- 1. 删除三态快照死表（无任何代码引用；若需恢复三态快照可重新建表）
DROP TABLE IF EXISTS `book_recycle`;
DROP TABLE IF EXISTS `reader_recycle`;

-- 2. 删除「回收站」目录菜单（白屏根因）及其角色关联；
--    图书回收站/读者回收站已直接挂在「运营辅助」下（reorg 脚本 3.3 段），无需移动
DELETE rm FROM sys_role_menu rm
JOIN sys_menu m ON m.menu_id = rm.menu_id
WHERE m.menu_name = '回收站' AND m.menu_type = 'M';
DELETE FROM sys_menu WHERE menu_name = '回收站' AND menu_type = 'M';

-- 3. 清理已删除权限点的残留菜单权限串（快照接口权限点 system:recycle:* 已随接口删除）
DELETE FROM sys_role_menu WHERE menu_id IN (
  SELECT menu_id FROM sys_menu WHERE perms LIKE 'system:recycle:%'
);
DELETE FROM sys_menu WHERE perms LIKE 'system:recycle:%';

-- 4. 回收站入口整体移除（2026-08-25 用户确认）：删除 图书回收站/读者回收站 菜单及
--    「运营辅助」空壳（M 菜单无子级会被若依渲染为可点击路由 → 白屏，须一并删除）。
--    页面代码（views/system/recycle/*、api/system/recycle.js）保留，随时可恢复入口。
DELETE rm FROM sys_role_menu rm
JOIN sys_menu m ON m.menu_id = rm.menu_id
WHERE m.menu_name IN ('图书回收站','读者回收站','运营辅助');
DELETE FROM sys_menu WHERE menu_name IN ('图书回收站','读者回收站','运营辅助');
