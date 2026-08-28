-- ============================================
-- 升级脚本：万事屋图书系统 → 数智游民创新工场官网（业务重映射，v20260821）
-- 适用：已存在的数据库（存量库升级路径，start-local 清单追加；全新库由改造后的 business_init.sql 直接初始化）
-- 幂等：可重复执行，务必带 --default-character-set=utf8mb4
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p ry-vue < sql/upgrade_20260821_official.sql
--
-- 边界说明：Java 类名/表名/API URL/Redis key 一律不变（Book→服务、Reader→成员、
-- BorrowRecord→报名 等均为展示层语义），本脚本只改：菜单名/停用、字典标签、参数名、示例数据。
-- 读者类型字典保持 3 值（ConfigUtil 按 1/2/3 映射 student/teacher/normal 参数键，加值会回退默认）。
-- ============================================


-- ============================================
-- 1. 菜单重命名（C/M 菜单；F 型按钮权限点随父菜单自动改名，此处一并处理）
--    模式：UPDATE 旧名→新名（存量库）；business_init.sql 负责全新库 INSERT 新名。
--    注意：重跑本脚本时 UPDATE 已无匹配行，天然幂等。
-- ============================================

-- 1.1 父目录与二级目录
UPDATE sys_menu SET menu_name='官网运营' WHERE menu_name='图书业务';
UPDATE sys_menu SET menu_name='服务管理' WHERE menu_name='图书管理';
UPDATE sys_menu SET menu_name='成员服务' WHERE menu_name='读者服务';
UPDATE sys_menu SET menu_name='合作经营' WHERE menu_name='经营管理';

-- 1.2 业务子菜单
UPDATE sys_menu SET menu_name='服务信息' WHERE menu_name='图书信息';
UPDATE sys_menu SET menu_name='报名管理' WHERE menu_name='借阅记录';
UPDATE sys_menu SET menu_name='官网轮播' WHERE menu_name='轮播图管理';
UPDATE sys_menu SET menu_name='成员管理' WHERE menu_name='读者管理';
UPDATE sys_menu SET menu_name='活动预约' WHERE menu_name='预约管理';
UPDATE sys_menu SET menu_name='入驻申请' WHERE menu_name='荐购管理';

-- 1.3 按钮权限点（F 型）
UPDATE sys_menu SET menu_name='报名导出' WHERE menu_name='借阅导出';
UPDATE sys_menu SET menu_name='入驻申请查询' WHERE menu_name='荐购查询';
UPDATE sys_menu SET menu_name='入驻申请处理' WHERE menu_name='荐购处理';
UPDATE sys_menu SET menu_name='入驻申请删除' WHERE menu_name='荐购删除';

-- ============================================
-- 2. 菜单停用（status='1'：selectMenuTreeAll 过滤 status=0，直接 URL 访问也被权限点拒绝）
--    停用：订单（含权限点）/借阅统计/回收站三菜单/读者登记（前台自助注册替代）
-- ============================================
UPDATE sys_menu SET status='1' WHERE menu_name IN (
  '订单管理','订单查询','订单修改','订单删除',
  '借阅统计',
  '回收站','图书回收站','读者回收站',
  '读者登记'
);

-- ============================================
-- 3. 字典改造（值不变，仅改名称/标签；增量库 UPDATE，全新库由 business_init 直接插新标签）
-- ============================================
-- 3.1 服务分类（book_type）：1=AI与数字服务 2=创意设计 3=本地生活与创业
UPDATE sys_dict_type SET dict_name='服务分类', remark='服务分类' WHERE dict_type='book_type';
UPDATE sys_dict_data SET dict_label='AI与数字服务', list_class='primary' WHERE dict_type='book_type' AND dict_value='1';
UPDATE sys_dict_data SET dict_label='创意设计',      list_class='success' WHERE dict_type='book_type' AND dict_value='2';
UPDATE sys_dict_data SET dict_label='本地生活与创业',list_class='warning' WHERE dict_type='book_type' AND dict_value='3';
-- 3.2 成员类型（reader_type）：1=个人主理人 2=团队 3=企业
UPDATE sys_dict_type SET dict_name='成员类型', remark='成员分类' WHERE dict_type='reader_type';
UPDATE sys_dict_data SET dict_label='个人主理人', list_class='primary' WHERE dict_type='reader_type' AND dict_value='1';
UPDATE sys_dict_data SET dict_label='团队',       list_class='success' WHERE dict_type='reader_type' AND dict_value='2';
UPDATE sys_dict_data SET dict_label='企业',       list_class='warning' WHERE dict_type='reader_type' AND dict_value='3';
-- 3.3 公告类型（sys_notice_type）：1=通知→新闻动态 2=公告（框架字典，键值在 ry_20260417.sql）
UPDATE sys_dict_data SET dict_label='新闻动态' WHERE dict_type='sys_notice_type' AND dict_value='1';

-- ============================================
-- 4. 参数名语义化（config_key 一律不动——ConfigUtil/前端按 key 读取）
-- ============================================
UPDATE sys_config SET config_name='名额预警阈值', remark='剩余名额低于或等于该值时，前后台显示名额紧张标签' WHERE config_key='book.stock.warn';
UPDATE sys_config SET config_name='个人主理人报名上限' WHERE config_key='book.borrow.maxCount.student';
UPDATE sys_config SET config_name='团队报名上限'       WHERE config_key='book.borrow.maxCount.teacher';
UPDATE sys_config SET config_name='企业报名上限'       WHERE config_key='book.borrow.maxCount.normal';
UPDATE sys_config SET config_name='个人主理人报名期限' WHERE config_key='book.borrow.days.student';
UPDATE sys_config SET config_name='团队报名期限'       WHERE config_key='book.borrow.days.teacher';
UPDATE sys_config SET config_name='企业报名期限'       WHERE config_key='book.borrow.days.normal';
UPDATE sys_config SET config_name='报名续期次数上限'   WHERE config_key='book.borrow.renewLimit';
UPDATE sys_config SET config_name='候补名额保留天数'   WHERE config_key='book.reserve.expireDays';
-- 罚款体系停用但参数保留在后台参数页，名称中性化
UPDATE sys_config SET config_name='截止逾期费用(元/天)' WHERE config_key='book.fine.perDay';
UPDATE sys_config SET config_name='截止逾期免计天数'     WHERE config_key='book.fine.graceDays';

-- ============================================
-- 5. 定时任务名语义化（invoke_target/cron 不动）
-- ============================================
UPDATE sys_job SET job_name='报名截止检查' WHERE job_name='逾期检查';
UPDATE sys_job SET job_name='候补超时检查' WHERE job_name='预约超时检查';

-- ============================================
-- 6. 示例数据：21 本书 → 21 条服务（按 ISBN 定位，幂等；覆盖全部演示场景）
--    演示覆盖：20 条招募中 + 1 条已结束(id3)；满员候补(id15/id16 stock=0)；
--    名额紧张(id7/id20 stock<=3)；新服务角标(id8/id16/id22 create_time 近 7 天)；分类覆盖 1/2/3
-- ============================================
UPDATE book SET book_name='AI 一人公司实战营',   author='数智游民创新工场',   book_type='1', publisher='清远高新区管委会',  price=199.00, publish_date='2026-08-01', stock=9,  status='0',
  intro='从 0 到 1 打造一人公司：AI 工具矩阵、获客、交付全流程实操训练营。适合想用 AI 开启独立事业的个人主理人。'
  WHERE isbn='9787536692930';
UPDATE book SET book_name='提示词工程入门课',     author='林晓川',             book_type='1', publisher='网易有道',        price=99.00,  publish_date='2026-07-15', stock=3,  status='0',
  intro='提示词是 AI 时代的敲门砖。本课程从基础结构到高级技巧，带你掌握与大模型高效对话的方法论。'
  WHERE isbn='9787111544937';
UPDATE book SET book_name='共享工位月租计划',     author='数智游民创新工场',   book_type='3', publisher='清远智慧谷',      price=500.00, publish_date='2026-06-01', stock=20, status='1',
  intro='（已结束）共享办公工位月租计划，曾支持 20+ 位主理人入驻办公。'
  WHERE isbn='9787505732534';
UPDATE book SET book_name='AI 短视频代运营服务',  author='陈晓工作室',         book_type='1', publisher='清远融媒体中心',  price=0.00,   publish_date='2026-07-20', stock=50, status='0',
  intro='免费体验：AI 辅助短视频策划、拍摄、剪辑全链路代运营，本地商家优先。'
  WHERE isbn='9787506365437';
UPDATE book SET book_name='一人公司法律咨询包',   author='李律师团队',         book_type='1', publisher='广东观澜律师事务所',price=299.00, publish_date='2026-07-10', stock=8,  status='0',
  intro='面向一人公司与小微团队：股权架构、合同审查、合规咨询一站式服务包。'
  WHERE isbn='9787544253994';
UPDATE book SET book_name='数字游民共居空间',     author='清远青年社区',       book_type='3', publisher='碧桂园清远',      price=800.00, publish_date='2026-07-25', stock=2,  status='0',
  intro='共居 + 共创：按月租入住共居空间，含共享工位与社区活动，长住优惠。'
  WHERE isbn='9787020029532';
UPDATE book SET book_name='AI 数字分身工作坊',    author='刘洋',               book_type='1', publisher='腾讯云',          price=149.00, publish_date='2026-08-19', stock=30, status='0',
  intro='新服务：手把手搭建你的 AI 数字分身——形象定制、语音克隆、直播带货实操。'
  WHERE isbn='9787530216781';
UPDATE book SET book_name='一人公司财税合规指南', author='王会计工作室',       book_type='3', publisher='清远税务学会',    price=0.00,   publish_date='2026-06-28', stock=25, status='0',
  intro='个体户注册、小规模纳税申报、发票管理全流程指南，财税小白也能听得懂。'
  WHERE isbn='9787020002207';
UPDATE book SET book_name='AI 写作与内容创作营',  author='张敏',               book_type='1', publisher='知乎',            price=129.00, publish_date='2026-07-08', stock=28, status='0',
  intro='公众号 / 知乎 / 小红书多平台 AI 内容生产实战：选题、提纲、成稿、分发。'
  WHERE isbn='9787020008735';
UPDATE book SET book_name='零代码 AI 应用搭建',   author='赵宇',               book_type='1', publisher='飞书',            price=169.00, publish_date='2026-07-12', stock=26, status='0',
  intro='不写代码也能搭 AI 应用：表单、知识库、自动化工作流的零代码方案。'
  WHERE isbn='9787020008728';
UPDATE book SET book_name='数字游民保险方案咨询', author='平安保险清远',       book_type='3', publisher='中国平安',        price=0.00,   publish_date='2026-07-05', stock=24, status='0',
  intro='灵活就业社保、补充商业险、意外险——数字游民的保障方案一对一咨询。'
  WHERE isbn='9787020008759';
UPDATE book SET book_name='小红书 AI 运营训练营', author='孙悦',               book_type='1', publisher='小红书',          price=88.00,  publish_date='2026-06-20', stock=40, status='0',
  intro='[b]AI 时代的内容运营课[/b]：爆款选题、AI 图文生成、笔记优化、涨粉变现全链路。
[quote]内容力 = AI 提效 × 真实人设[/quote]
[color=#c65d43]往期学员 200+[/color]，[url=#]查看学员案例 →[/url]'
  WHERE isbn='9787020042494';
UPDATE book SET book_name='视频号 AI 剪辑速成',   author='周涛',               book_type='1', publisher='微信视频号',      price=0.00,   publish_date='2026-07-18', stock=35, status='0',
  intro='剪映 + AI 工具快速出片：从素材管理到成片发布，一天学会日更节奏。'
  WHERE isbn='9787532748662';
UPDATE book SET book_name='社区共创空间预约',     author='数智游民创新工场',   book_type='3', publisher='清远图书馆',      price=0.00,   publish_date='2026-06-15', stock=0,  status='0',
  intro='（满员可候补）社区共创空间按场次预约：路演厅、直播间、洽谈室。'
  WHERE isbn='9787544270878';
UPDATE book SET book_name='AI 心理陪伴体验',      author='心光工作室',         book_type='1', publisher='广东工业大学清远校区',price=0.00, publish_date='2026-08-18', stock=0, status='0',
  intro='新服务：AI 心理陪伴对话体验 + 真人倾听服务，关注数字游民的心理健康。'
  WHERE isbn='9787544270879';
UPDATE book SET book_name='一人公司品牌设计',     author='绘境设计',           book_type='2', publisher='站酷',            price=129.00, publish_date='2026-07-22', stock=30, status='0',
  intro='logo / VI / 包装设计一条龙，AI 辅助出稿、设计师精修，适合初创一人公司。'
  WHERE isbn='9787020008742';
UPDATE book SET book_name='AI 绘画与设计基础',    author='阿杰',               book_type='2', publisher='花瓣',            price=0.00,   publish_date='2026-06-18', stock=32, status='0',
  intro='Midjourney / Stable Diffusion 入门：从提示词到商业级出图的工作流。'
  WHERE isbn='9787537812249';
UPDATE book SET book_name='本地生活探店 AI 写作', author='清远探店团',         book_type='3', publisher='抖音生活服务',    price=0.00,   publish_date='2026-07-28', stock=27, status='0',
  intro='本地商家探店内容 AI 化：短视频脚本、点评文案、直播话术模板库。'
  WHERE isbn='9787020009626';
UPDATE book SET book_name='低代码小程序开发课',   author='小码匠',             book_type='2', publisher='微信开放平台',    price=128.00, publish_date='2026-07-30', stock=2,  status='0',
  intro='名额紧张：低代码 + AI 辅助开发微信小程序，一人也能接外包项目。'
  WHERE isbn='9787121022982';
UPDATE book SET book_name='AI 算法与模型微调',    author='清远 AI 实验室',     book_type='1', publisher='华为云',          price=128.00, publish_date='2026-08-05', stock=10, status='0',
  intro='面向中小企业：开源模型私有化部署与微调实战，数据不出域。'
  WHERE isbn='9787111407010';
UPDATE book SET book_name='清远非遗文创 AI 化',   author='岭南文创社',         book_type='2', publisher='清远市文化馆',    price=0.00,   publish_date='2026-08-20', stock=20, status='0',
  intro='新服务：清远非遗 IP 数字化共创——AI 文创设计、数字藏品、研学课程。'
  WHERE isbn='9787101054033';

-- 新服务角标：近 7 天 create_time（幂等：只对未设过的时间覆盖一次）
UPDATE book SET create_time=DATE_SUB(NOW(), INTERVAL 2 DAY) WHERE isbn='9787530216781' AND create_time < DATE_SUB(NOW(), INTERVAL 7 DAY);
UPDATE book SET create_time=DATE_SUB(NOW(), INTERVAL 3 DAY) WHERE isbn='9787544270879' AND create_time < DATE_SUB(NOW(), INTERVAL 7 DAY);
UPDATE book SET create_time=DATE_SUB(NOW(), INTERVAL 1 DAY) WHERE isbn='9787101054033' AND create_time < DATE_SUB(NOW(), INTERVAL 7 DAY);

-- ============================================
-- 7. 示例数据：7 个读者 → 社区成员（证号不变——前端登录凭证，后端 Java 生成）
--    演示覆盖：3 类型成员、1 停用成员（前台登录被拒）、2 邮件通知成员
-- ============================================
UPDATE reader SET reader_name='周舟', reader_type='1', remark='个人主理人-演示报名与候补' WHERE card_no='JS20260001';
UPDATE reader SET reader_name='李想', reader_type='2', remark='团队-演示长借期报名'       WHERE card_no='JS20260002';
UPDATE reader SET reader_name='王梅', reader_type='3', remark='企业-演示报名三态'          WHERE card_no='JS20260003';
UPDATE reader SET reader_name='吴挂', reader_type='1', remark='停用成员-演示前台登录被拒'   WHERE card_no='JS20260004';
UPDATE reader SET reader_name='Jerry',reader_type='2', remark='项目作者账号'                WHERE card_no='DK';
UPDATE reader SET reader_name='赵一', reader_type='1', remark='演示前台注册/修改资料/看板统计全链路' WHERE card_no='JS20260005';
UPDATE reader SET reader_name='钱枫', reader_type='3', remark='演示邮件通知（报名/候补/申请结果）'   WHERE card_no='JS20260006';

-- ============================================
-- 8. 报名记录快照同步（borrow_record 语义：0进行中/1已完成/2已截止；快照列随成员/服务改名）
--    日期为历史数据保留不动；borrow_id=2 的截止日期已过 → 看板"已截止报名"演示
-- ============================================
UPDATE borrow_record SET reader_name='周舟', book_name='AI 一人公司实战营'      WHERE borrow_id=1;
UPDATE borrow_record SET reader_name='周舟', book_name='一人公司财税合规指南'    WHERE borrow_id=2;
UPDATE borrow_record SET reader_name='李想', book_name='提示词工程入门课'        WHERE borrow_id=3;
UPDATE borrow_record SET reader_name='王梅', book_name='AI 短视频代运营服务'     WHERE borrow_id=4;
UPDATE borrow_record SET reader_name='王梅', book_name='小红书 AI 运营训练营'    WHERE borrow_id=5;
UPDATE borrow_record SET reader_name='吴挂', book_name='AI 绘画与设计基础'       WHERE borrow_id=6;

-- ============================================
-- 9. 候补记录快照同步（book_reserve：0候补中/1有名额/2已完成/3已取消；挂靠 book_id=15 社区共创空间预约）
-- ============================================
UPDATE book_reserve SET reader_name='李想', book_name='社区共创空间预约' WHERE book_id=15 AND card_no='JS20260002';
UPDATE book_reserve SET reader_name='周舟', book_name='社区共创空间预约' WHERE book_id=15 AND card_no='JS20260001';
UPDATE book_reserve SET reader_name='王梅', book_name='社区共创空间预约' WHERE book_id=15 AND card_no='JS20260003';
UPDATE book_reserve SET reader_name='Jerry',book_name='社区共创空间预约' WHERE book_id=15 AND card_no='DK';
-- 满员：与"候补中/有名额"状态链自洽（原脚本把库存置 1 对应"可借"，现统一置 0 演示候补入口）
UPDATE book SET stock=0 WHERE book_id=15;

-- ============================================
-- 10. 入驻/合作申请示例（原荐购表；0待审核/1已通过/2已婉拒；按 book_name 判重幂等）
-- ============================================
INSERT INTO book_purchase_req (book_name, author, email, status, remark, create_by, create_time)
SELECT 'AI 电商代运营团队招募', '郑浩', 'zhenghao@qq.com', '0', '希望入驻社区并招募 3 人 AI 代运营团队，需要共享工位与政策对接', 'admin', DATE_SUB(NOW(), INTERVAL 1 DAY) FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM book_purchase_req WHERE book_name='AI 电商代运营团队招募');
INSERT INTO book_purchase_req (book_name, author, email, status, remark, create_by, create_time)
SELECT '数字游民签证与出海服务', '陈立', 'chenli@qq.com', '1', '提供数字游民签证咨询与跨境财税服务，寻求社区合作挂牌', 'admin', DATE_SUB(NOW(), INTERVAL 3 DAY) FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM book_purchase_req WHERE book_name='数字游民签证与出海服务');
INSERT INTO book_purchase_req (book_name, author, email, status, remark, create_by, create_time)
SELECT '本地 AI 茶饮品牌', '何雨', 'heyu@qq.com', '2', '计划用 AI 运营一家茶饮店，申请入驻被婉拒（店铺资质待补）', 'admin', DATE_SUB(NOW(), INTERVAL 5 DAY) FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM book_purchase_req WHERE book_name='本地 AI 茶饮品牌');

-- ============================================
-- 11. 清理演示订单（订单模块已停用，历史演示订单删除）
-- ============================================
DELETE FROM shop_order WHERE order_no LIKE 'WSW%';

-- ============================================
-- 12. 轮播图 → 品牌首屏轮播（表无唯一索引，用 UPDATE 不用 INSERT）
-- ============================================
UPDATE sys_banner SET title='数智游民创新工场', subtitle='清远首个 AI 一人公司生态社区 ｜ 一个人，也可以是一家公司', link='', sort=1 WHERE title='万事屋';
UPDATE sys_banner SET title='AI 课程与服务', subtitle='AI 技能课程 / 共享工位 / 孵化服务，一站式支持 OPC 成长', link='', sort=2 WHERE title='图书预约';
UPDATE sys_banner SET title='欢迎入驻', subtitle='一个人 + AI，在清远开启你的数智游民之旅', link='', sort=3 WHERE title='新书上架';

-- ============================================
-- 13. 公告 → 新闻动态（3 条；正文用纯文本，规避前台 textContent 渲染的富文本降级）
-- ============================================
UPDATE sys_notice SET notice_title='数智游民创新工场正式启幕', notice_type='2',
  notice_content='清远市首个人工智能 OPC（一人公司）生态社区「数智游民创新工场」正式启幕。社区提供 AI 技能课程、共享工位、孵化服务与政策对接，支持每一位"一个人 + AI"的创业者。'
  WHERE notice_id=1;
UPDATE sys_notice SET notice_title='首期 AI 一人公司实战营开放报名', notice_type='1',
  notice_content='首期「AI 一人公司实战营」现已开放报名：AI 工具矩阵、获客、交付全流程实操，限额 20 席，报满即止。'
  WHERE notice_id=2;
UPDATE sys_notice SET notice_title='社区共创空间开放预约', notice_type='1',
  notice_content='路演厅、直播间、洽谈室等共创空间已开放预约。名额有限，可先预约排队，释放名额后自动通知。'
  WHERE notice_id=3;

-- ============================================
-- 14. 孤儿菜单自愈（幂等）：start-local 升级清单曾包含旧脚本（purchase/recycle），
--     在官网改造后的库上按旧名反查父级失败会插入 parent_id 为 NULL 的孤儿菜单，
--     导致登录后菜单树构建 NPE（Cannot invoke Long.longValue() because getParentId() is null）。
--     本段清理：① 挂在孤儿目录下的子菜单 ② 孤儿目录本身 ③ 悬空引用与角色关联。
--     自愈后正确的官网菜单树（官网运营→服务管理/成员服务/合作经营/CMS 管理）不受影响。
-- ============================================
DELETE FROM sys_menu WHERE parent_id IN (
  SELECT mid FROM (SELECT m.menu_id mid FROM sys_menu m JOIN sys_menu p ON p.menu_id=m.parent_id
    WHERE p.parent_id IS NULL AND p.menu_type='M' AND p.menu_id>1) t
);
DELETE FROM sys_menu WHERE parent_id IS NULL AND menu_type='M' AND menu_id>1;
-- 悬空菜单/角色关联清理（循环：物化子查询基于删除前快照，连锁悬空需多轮）
-- ⚠️ 必须带 parent_id>0 条件：parent_id=0 是合法顶级菜单，NOT IN 会误删
DELETE FROM sys_menu WHERE parent_id > 0 AND parent_id NOT IN (SELECT mid FROM (SELECT menu_id mid FROM sys_menu) t);
DELETE FROM sys_menu WHERE parent_id > 0 AND parent_id NOT IN (SELECT mid FROM (SELECT menu_id mid FROM sys_menu) t);
DELETE FROM sys_menu WHERE parent_id > 0 AND parent_id NOT IN (SELECT mid FROM (SELECT menu_id mid FROM sys_menu) t);
DELETE FROM sys_menu WHERE parent_id > 0 AND parent_id NOT IN (SELECT mid FROM (SELECT menu_id mid FROM sys_menu) t);
DELETE FROM sys_menu WHERE parent_id > 0 AND parent_id NOT IN (SELECT mid FROM (SELECT menu_id mid FROM sys_menu) t);
DELETE FROM sys_role_menu WHERE menu_id NOT IN (SELECT mid FROM (SELECT menu_id mid FROM sys_menu) t);
