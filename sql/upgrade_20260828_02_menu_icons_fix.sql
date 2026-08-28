-- ============================================
-- 2026-08-28：修复 5 个缺失 svg 的菜单图标（book/delete/recycle 无对应资源，
-- 侧边栏渲染空白）——08 脚本只覆盖了 2070/2125 等，遗漏了这套旧 menu_id 菜单
-- （本地与 docker 的运营辅助实际是 2410 而非 2390，menu_id 漂移导致 08 的
-- 2390 定位落空）。幂等：WHERE icon='旧值' 保证重复执行无副作用
-- ============================================
UPDATE sys_menu SET icon='component' WHERE menu_id=2410 AND icon='delete'; -- 运营辅助
UPDATE sys_menu SET icon='log'      WHERE menu_id=2413 AND icon='delete'; -- 回收站
UPDATE sys_menu SET icon='list'     WHERE menu_id=2414 AND icon='recycle'; -- 回收站（孤儿）
UPDATE sys_menu SET icon='list'     WHERE menu_id=2077 AND icon='book';   -- 服务信息（已隐藏）
UPDATE sys_menu SET icon='list'     WHERE menu_id=2332 AND icon='book';   -- 服务回收站（已隐藏）