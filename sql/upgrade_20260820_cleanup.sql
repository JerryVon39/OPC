-- ============================================
-- 升级脚本：清除若依框架残留（品牌业务化，参考本地演进库实际填写）
-- 适用：Docker 全新初始化（mysql-init.sh 追加执行）/ 已存在的数据库（start-local 清单追加）/ 存量库手动执行
-- 幂等：可重复执行，务必带 --default-character-set=utf8mb4
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p ry-vue < sql/upgrade_20260820_cleanup.sql
-- ============================================

USE ry-vue;

-- ---------- 1. 用户：admin 个人信息业务化；ry 演示账号改名（本地同名账号的昵称/备注） ----------
-- 邮箱留空：本地为真实邮箱，公开仓库/初始化脚本不放真实联系方式（泄露教训）
UPDATE sys_user SET nick_name='系统管理员', email='', remark='管理员' WHERE user_name='admin' AND user_id=1;
UPDATE sys_user SET nick_name='演示账号', remark='测试员' WHERE user_name='ry' AND user_id=2;

-- ---------- 2. 部门：若依科技/分公司结构 → 本地业务化结构（阅海图书文化有限公司） ----------
-- leader/email 本地残留"若依"/ry@qq.com，一并清空（负责人信息按需后台再填）
UPDATE sys_dept SET dept_name='阅海图书文化有限公司', leader='', email='' WHERE dept_id=100;
UPDATE sys_dept SET dept_name='图书运营中心', leader='', email='' WHERE dept_id=101;
UPDATE sys_dept SET dept_name='综合服务中心', leader='', email='' WHERE dept_id=102;
UPDATE sys_dept SET dept_name='图书采购部',   leader='', email='' WHERE dept_id=103;
UPDATE sys_dept SET dept_name='门店管理部',   leader='', email='' WHERE dept_id=104;
UPDATE sys_dept SET dept_name='电商运营部',   leader='', email='' WHERE dept_id=105;
UPDATE sys_dept SET dept_name='财务部',       leader='', email='' WHERE dept_id=106;
UPDATE sys_dept SET dept_name='仓储物流部',   leader='', email='' WHERE dept_id=107;
UPDATE sys_dept SET dept_name='市场部',       leader='', email='' WHERE dept_id=108;
UPDATE sys_dept SET dept_name='财务部',       leader='', email='' WHERE dept_id=109;

-- ---------- 3. 公告：若依 3 条公告 → 本地业务公告（标题/类型/正文照抄本地演进库） ----------
UPDATE sys_notice SET notice_title='新版本发布公告', notice_type='2',
  notice_content='图书管理系统 v1.1 即将发布，将新增借阅登记功能，敬请期待！'
  WHERE notice_id=1;
UPDATE sys_notice SET notice_title='系统维护通知', notice_type='1',
  notice_content='图书管理系统将于本周六 02:00-04:00 进行例行维护，期间系统将暂停访问，请提前保存工作内容。'
  WHERE notice_id=2;
UPDATE sys_notice SET notice_title='图书管理系统正式上线', notice_type='2',
  notice_content='图书管理系统 v1.0 正式上线！支持图书信息增删改查、分类管理、在架/下架状态管理、Excel 导入导出等功能。'
  WHERE notice_id=3;

-- ---------- 4. 岗位：若依 3 岗（董事长/经理/员工）→ 本地书店 10 岗 ----------
UPDATE sys_post SET post_name='总经理'     WHERE post_code='ceo';
UPDATE sys_post SET post_name='运营经理'   WHERE post_code='se';
UPDATE sys_post SET post_name='人事专员'   WHERE post_code='hr';
INSERT INTO sys_post (post_code, post_name, post_sort, status, create_by, create_time, remark)
SELECT 'user',      '图书管理员',     4, '0', 'admin', NOW(), '前台借还书业务办理'
WHERE NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code='user');
INSERT INTO sys_post (post_code, post_name, post_sort, status, create_by, create_time, remark)
SELECT 'purchase',  '图书采购专员',   5, '0', 'admin', NOW(), '图书采购与荐购处理'
WHERE NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code='purchase');
INSERT INTO sys_post (post_code, post_name, post_sort, status, create_by, create_time, remark)
SELECT 'store',     '门店店长',       6, '0', 'admin', NOW(), '门店日常经营'
WHERE NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code='store');
INSERT INTO sys_post (post_code, post_name, post_sort, status, create_by, create_time, remark)
SELECT 'ecommerce', '电商运营专员',   7, '0', 'admin', NOW(), '线上购书订单运营'
WHERE NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code='ecommerce');
INSERT INTO sys_post (post_code, post_name, post_sort, status, create_by, create_time, remark)
SELECT 'warehouse', '库管专员',       8, '0', 'admin', NOW(), '仓储与库存管理'
WHERE NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code='warehouse');
INSERT INTO sys_post (post_code, post_name, post_sort, status, create_by, create_time, remark)
SELECT 'service',   '客服专员',       9, '0', 'admin', NOW(), '读者服务与咨询'
WHERE NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code='service');
INSERT INTO sys_post (post_code, post_name, post_sort, status, create_by, create_time, remark)
SELECT 'finance',   '财务专员',      10, '0', 'admin', NOW(), '收款与账务'
WHERE NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code='finance');

-- ---------- 5. 菜单：删除"若依官网"外链（先清角色关联再删菜单） ----------
DELETE FROM sys_role_menu WHERE menu_id = (SELECT menu_id FROM sys_menu WHERE menu_name='若依官网');
DELETE FROM sys_menu WHERE menu_name='若依官网';
