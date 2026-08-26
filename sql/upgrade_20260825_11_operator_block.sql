-- ============================================
-- 升级脚本：运营专员（operator）授权「区块管理」v20260825
-- 背景：运营需要管理首页不同模块（首屏/品牌理念/三大赋能/产业生态等）。
--       区块管理挂在「内容运营」分组下，operator 原角色不含该分组 → 不可见。
-- 处理：仅授予 内容运营目录 + 区块管理页 + 区块查询/修改权限点（含历史回滚），
--       不授 区块新增/删除 —— 运营只能改文案，不能新建/删除区块（防呆边界）。
-- 幂等：INSERT...SELECT WHERE NOT EXISTS，按 role_id+menu_id 判重
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p ry-vue < sql/upgrade_20260825_operator_block.sql
-- ============================================

USE ry-vue;

-- 1. operator 授「内容运营」目录（父链完整，菜单树才能显示其下有权限的子项）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id FROM sys_role r, sys_menu m
WHERE r.role_key = 'operator' AND m.menu_name = '内容运营' AND m.menu_type = 'M'
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id=r.role_id AND rm.menu_id=m.menu_id);

-- 2. operator 授「区块管理」页 + 查询/修改权限点（修改含历史回滚；不授新增/删除）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id FROM sys_role r, sys_menu m
WHERE r.role_key = 'operator' AND m.menu_name IN ('区块管理','区块查询','区块修改')
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id=r.role_id AND rm.menu_id=m.menu_id);

-- 3. 完成提示
SELECT CONCAT('operator 区块管理授权：菜单 ',
              (SELECT COUNT(*) FROM sys_role_menu rm JOIN sys_role r ON r.role_id=rm.role_id
                JOIN sys_menu m ON m.menu_id=rm.menu_id
                WHERE r.role_key='operator' AND m.menu_name IN ('内容运营','区块管理','区块查询','区块修改')),
              ' 项') AS result;
