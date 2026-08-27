-- ============================================
-- 隐藏「页面搭建」菜单（第九轮审查 P0-1）
-- 背景：内容引擎化后首页已切 cms_block 数据源（site.js loadHomeSections），
--       cms_page_section 数据不再被前台渲染。菜单仍可进可改 → 运营改完
--       发现前台无变化，误判"系统坏了/有缓存"。隐藏菜单（数据保留可恢复）。
-- 幂等：UPDATE 可重复执行
-- ============================================
UPDATE sys_menu SET visible = '1'
WHERE menu_name = '页面搭建' AND menu_type = 'C' AND component = 'system/cms/section';
