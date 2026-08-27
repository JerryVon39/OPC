-- ============================================
-- 后台菜单补齐图标：原图标类名无对应 svg 资源（侧边栏渲染空白）
-- 替换为已存在的 RuoYi svg 图标；幂等：WHERE icon='旧值' 保证重复执行无副作用
-- ============================================
UPDATE sys_menu SET icon='dashboard'    WHERE menu_id=2070 AND icon='book';   -- 官网运营（顶级）
UPDATE sys_menu SET icon='list'         WHERE menu_id=2085 AND icon='book';   -- 服务管理（旧隐藏组）
UPDATE sys_menu SET icon='documentation' WHERE menu_id=2125 AND icon='content'; -- 内容运营（顶级）
UPDATE sys_menu SET icon='upload'       WHERE menu_id=2071 AND icon='picture'; -- 官网轮播
UPDATE sys_menu SET icon='list'         WHERE menu_id=2079 AND icon='reading'; -- 报名管理
UPDATE sys_menu SET icon='people'       WHERE menu_id=2081 AND icon='shopping-cart-full'; -- 入驻申请
UPDATE sys_menu SET icon='table'        WHERE menu_id=2278 AND icon='menu';   -- 页面搭建（已隐藏）
UPDATE sys_menu SET icon='theme'        WHERE menu_id=2300 AND icon='settings';-- 站点设置
UPDATE sys_menu SET icon='component'    WHERE menu_id=2390 AND icon='delete'; -- 运营辅助（顶级）
UPDATE sys_menu SET icon='log'          WHERE menu_id=2394 AND icon='delete'; -- 回收站（运营辅助下）
UPDATE sys_menu SET icon='list'         WHERE menu_id=2395 AND icon='recycle';-- 旧回收站（孤儿菜单）