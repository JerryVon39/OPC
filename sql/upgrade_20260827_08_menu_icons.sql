-- ============================================
-- 后台菜单补齐图标：原图标类名无对应 svg 资源（侧边栏渲染空白）
-- ⚠️ 2026-08-28 重写：原按 menu_id 定位——business_init.sql 菜单为自增插入，
--    全新库 menu_id 每次漂移（本地 2070/2125，全新库 2000/2043），固定 id 全落空
--    改按 menu_name 定位，幂等且跨库安全（与 upgrade_20260825_02 同模式）
-- ============================================
UPDATE sys_menu SET icon='dashboard'     WHERE menu_name='官网运营'  AND menu_type='M' AND icon='book';
UPDATE sys_menu SET icon='documentation' WHERE menu_name='内容运营'  AND menu_type='M' AND icon='content';
UPDATE sys_menu SET icon='upload'        WHERE menu_name='官网轮播'  AND icon='picture';
UPDATE sys_menu SET icon='list'          WHERE menu_name='报名管理'  AND icon='reading';
UPDATE sys_menu SET icon='people'        WHERE menu_name='入驻申请'  AND icon='shopping-cart-full';
UPDATE sys_menu SET icon='table'         WHERE menu_name='页面搭建'  AND icon='menu';
UPDATE sys_menu SET icon='theme'         WHERE menu_name='站点设置'  AND icon='settings';
UPDATE sys_menu SET icon='component'     WHERE menu_name='运营辅助'  AND menu_type='M' AND icon='delete';
UPDATE sys_menu SET icon='log'           WHERE menu_name='回收站'    AND icon='delete';
UPDATE sys_menu SET icon='list'          WHERE menu_name='回收站'    AND icon='recycle';
-- 隐藏的服务业务菜单（图标同样缺失，一并修复）
UPDATE sys_menu SET icon='list'          WHERE menu_name='服务信息'  AND icon='book';
UPDATE sys_menu SET icon='list'          WHERE menu_name='服务管理'  AND icon='book';
UPDATE sys_menu SET icon='list'          WHERE menu_name='服务回收站' AND icon='book';