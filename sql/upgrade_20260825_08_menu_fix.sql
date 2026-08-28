-- ============================================
-- 升级脚本：后台菜单修复 + 分类重排 v20260825（审查修复）
-- 内容：
--   1. 修复顶层目录 path 冲突：运营辅助 path 'ops' → 'ops-aux'（与运营工作台 'ops' 重复，
--      导致 RuoYi 动态路由 /ops 父子节点合并覆盖，新页面打不开）
--   2. 重建「回收站」目录（原目录被清理脚本删除，服务/成员回收站成 2 个孤儿菜单不可见）
--      + 服务/成员/文章 3 个回收站全部挂其下；使用帮助平挂运营辅助
--   3. 内容运营子菜单重排：文章管理(1)/栏目管理(2)/区块管理(3)/服务信息(4)/官网轮播(5)/通知公告(6)
--   4. 角色补挂「回收站」目录（RuoYi 权限菜单树要求父链完整，否则子回收站对角色不可见）
-- 幂等：可重复执行；目录/菜单 INSERT...SELECT WHERE NOT EXISTS，UPDATE 按 menu_name 定位
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p ry-vue < sql/upgrade_20260825_menu_fix.sql
-- ============================================


-- ============================================
-- 1. 顶层目录 path 冲突修复
-- ============================================
UPDATE sys_menu SET path = 'ops-aux' WHERE menu_name = '运营辅助' AND menu_type = 'M' AND path = 'ops';

-- ============================================
-- 2. 重建「回收站」目录（挂运营辅助 order=1）并归位 3 个回收站
-- ============================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '回收站', (SELECT menu_id FROM sys_menu WHERE menu_name='运营辅助' AND menu_type='M' LIMIT 1), 1, 'recycle', '', 1, 0, 'M', '0', '0', '', 'delete', 'admin', NOW(), '误删数据恢复（两态）'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name='回收站' AND menu_type='M');

-- 服务/成员回收站：孤儿父 id → 回收站目录（按菜单名+component 定位，幂等）
UPDATE sys_menu m
JOIN sys_menu p ON p.menu_name='回收站' AND p.menu_type='M'
SET m.parent_id = p.menu_id
WHERE m.menu_name IN ('服务回收站','成员回收站') AND m.component IN ('system/recycle/book','system/recycle/reader');

-- 文章回收站：运营辅助平挂 → 回收站目录 order=3
UPDATE sys_menu m
JOIN sys_menu p ON p.menu_name='回收站' AND p.menu_type='M'
SET m.parent_id = p.menu_id, m.order_num = 3
WHERE m.menu_name='文章回收站';

-- 使用帮助：平挂运营辅助 order=2（与回收站目录同级）
UPDATE sys_menu SET order_num = 2 WHERE menu_name='使用帮助';

-- ============================================
-- 3. 内容运营子菜单重排（按使用频率：文章/栏目/区块 在前）
-- ============================================
UPDATE sys_menu SET order_num = 1 WHERE menu_name='文章管理';
UPDATE sys_menu SET order_num = 2 WHERE menu_name='栏目管理';
UPDATE sys_menu SET order_num = 3 WHERE menu_name='区块管理';
UPDATE sys_menu SET order_num = 4 WHERE menu_name='服务信息';
UPDATE sys_menu SET order_num = 5 WHERE menu_name='官网轮播';
UPDATE sys_menu SET order_num = 6 WHERE menu_name='通知公告';

-- ============================================
-- 4. 角色补挂「回收站」目录（operator/editor 已有 3 个回收站子页权限，缺父目录不可见）
-- ============================================
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id FROM sys_role r, sys_menu m
WHERE r.role_key IN ('operator','editor') AND m.menu_name='回收站' AND m.menu_type='M'
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id=r.role_id AND rm.menu_id=m.menu_id);

-- ============================================
-- 5. 完成提示
-- ============================================
SELECT CONCAT('菜单修复完成：孤儿菜单 ',
              (SELECT COUNT(*) FROM sys_menu m LEFT JOIN sys_menu p ON m.parent_id=p.menu_id WHERE m.parent_id<>0 AND p.menu_id IS NULL),
              ' 个，回收站目录 ',
              (SELECT COUNT(*) FROM sys_menu WHERE menu_name='回收站' AND menu_type='M'),
              ' 个') AS result;
