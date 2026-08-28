-- ============================================
-- 升级脚本：去重「运营工作台」菜单 v20260825
-- 背景：「工作台」菜单 component 复用登录落地页 views/index.vue（/index 内置路由），
--       与登录后首页渲染同一组件 → 侧边栏出现两个相同页面，冗余且困惑。
-- 处理：删除「运营工作台」目录 + 「工作台」子页 + 角色关联；
--       登录落地页为 RuoYi 内置 /index 路由，与菜单无关，删除后落地页不受影响。
-- 幂等：DELETE 天然幂等（按 menu_name + component 定位，不写死 menu_id）
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p ry-vue < sql/upgrade_20260825_menu_dedupe.sql
-- ============================================


-- 1. 先删角色关联（operator/editor 对该目录与子页的授权）
DELETE FROM sys_role_menu WHERE menu_id IN (
  SELECT mid FROM (
    SELECT m.menu_id AS mid FROM sys_menu m
    WHERE m.menu_name IN ('运营工作台','工作台')
       OR m.parent_id IN (SELECT menu_id FROM sys_menu WHERE menu_name='运营工作台' AND menu_type='M')
  ) t
);

-- 2. 删除子页与目录（含其下可能残留的子项）
DELETE FROM sys_menu WHERE menu_name='工作台' AND component='index';
DELETE FROM sys_menu WHERE menu_name='运营工作台' AND menu_type='M';

-- 3. 完成提示
SELECT CONCAT('去重完成：剩余运营工作台菜单 ',
              (SELECT COUNT(*) FROM sys_menu WHERE menu_name IN ('运营工作台','工作台')),
              ' 个') AS result;
