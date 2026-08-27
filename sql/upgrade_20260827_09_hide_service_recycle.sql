-- ============================================
-- 隐藏「服务回收站」菜单（服务业务已关停）
-- 背景：upgrade_20260825_19_hide_book_menu.sql 注释声称隐藏"服务回收站"，
--       但 UPDATE 列表漏掉该项，导致本地已执行过该脚本的库中"服务回收站"仍显示。
-- 处理：菜单停用（visible='1'，数据保留可随时恢复，与"服务信息"等一致，不改代码不删表）
-- 幂等：WHERE menu_name='服务回收站' AND visible='0' 保证重复执行无副作用
-- ============================================
UPDATE sys_menu SET visible = '1', update_by = 'admin', update_time = NOW()
WHERE menu_name = '服务回收站' AND visible = '0';