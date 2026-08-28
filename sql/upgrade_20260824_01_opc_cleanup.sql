-- ============================================
-- 升级脚本：系统数据 OPC 化清理 v20260824（数智游民创新工场）
-- 清理改造后残留的：若依官网菜单、图书公司部门结构、书店岗位
-- 适用：存量库；幂等可重跑；务必带 --default-character-set=utf8mb4
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p ry-vue < sql/upgrade_20260824_opc_cleanup.sql
-- ============================================


-- ============================================
-- 1. 删除"若依官网"外链菜单（先清角色关联）
-- ============================================
DELETE FROM sys_role_menu WHERE menu_id IN (SELECT mid FROM (SELECT menu_id mid FROM sys_menu WHERE menu_name='若依官网') t);
DELETE FROM sys_menu WHERE menu_name='若依官网';

-- ============================================
-- 2. 部门：图书公司结构 → OPC 社区结构（按 dept_id 定位，幂等）
-- ============================================
UPDATE sys_dept SET dept_name='数智游民创新工场（清远）科技有限公司' WHERE dept_id=100;
UPDATE sys_dept SET dept_name='社区运营中心'   WHERE dept_id=101;
UPDATE sys_dept SET dept_name='课程培训中心'   WHERE dept_id=102;
UPDATE sys_dept SET dept_name='企业服务部'     WHERE dept_id=103;
UPDATE sys_dept SET dept_name='政策对接部'     WHERE dept_id=104;
UPDATE sys_dept SET dept_name='技术服务部'     WHERE dept_id=105;
UPDATE sys_dept SET dept_name='财务部'         WHERE dept_id=106;
UPDATE sys_dept SET dept_name='行政人事部'     WHERE dept_id=107;
UPDATE sys_dept SET dept_name='市场合作部'     WHERE dept_id=108;
UPDATE sys_dept SET dept_name='电商运营部'     WHERE dept_id=109;

-- ============================================
-- 3. 岗位：书店岗位 → OPC 社区岗位（按 post_code 定位，幂等）
-- ============================================
UPDATE sys_post SET post_name='社区总经理'   WHERE post_code='ceo';
UPDATE sys_post SET post_name='运营经理'     WHERE post_code='se';
UPDATE sys_post SET post_name='人事专员'     WHERE post_code='hr';
UPDATE sys_post SET post_name='普通员工'     WHERE post_code='user';
UPDATE sys_post SET post_name='服务采购专员' WHERE post_code='purchase';
UPDATE sys_post SET post_name='空间运营主管' WHERE post_code='store';
UPDATE sys_post SET post_name='电商运营专员' WHERE post_code='ecommerce';
UPDATE sys_post SET post_name='物资管理专员' WHERE post_code='warehouse';
UPDATE sys_post SET post_name='客户服务专员' WHERE post_code='service';
UPDATE sys_post SET post_name='财务专员'     WHERE post_code='finance';
