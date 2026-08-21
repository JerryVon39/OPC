-- ============================================
-- 升级脚本：官网内容真实化 v20260822（数智游民创新工场真实信息落库）
-- 内容来源：docs/官网内容库.md（老板文档 + 公开报道交叉验证）
-- 适用：存量库（在 upgrade_20260821_official.sql 之后执行）；全新库由 business_init.sql 更新后直接含真实内容
-- 幂等：可重复执行，务必带 --default-character-set=utf8mb4
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p ry-vue < sql/upgrade_20260822_realcontent.sql
-- ============================================

USE ry-vue;

-- ============================================
-- 1. 服务分类字典 → 三大产业赛道（值 1/2/3 不变，仅标签）
-- ============================================
UPDATE sys_dict_type SET dict_name='产业赛道', remark='服务/项目所属产业赛道' WHERE dict_type='book_type';
UPDATE sys_dict_data SET dict_label='AI 内容创作',       list_class='primary' WHERE dict_type='book_type' AND dict_value='1';
UPDATE sys_dict_data SET dict_label='AI 技术应用',       list_class='success' WHERE dict_type='book_type' AND dict_value='2';
UPDATE sys_dict_data SET dict_label='AI 硬件与场景',     list_class='warning' WHERE dict_type='book_type' AND dict_value='3';

-- ============================================
-- 2. 21 条服务 → 真实 AI 项目（按 isbn 定位，幂等）
--    演示覆盖保留：20 招募中 + 1 已结束(id3)；满员候补(id15/id16)；名额紧张(id2/id7/id20)；
--    新服务角标(id8/id16/id22)；分类覆盖三大赛道；报名/候补历史快照同步（见第 3 节）
-- ============================================
UPDATE book SET book_name='AI 微短剧制作实战营',    author='星链科技',             book_type='1', publisher='清远星链科技',      price=199.00, publish_date='2026-08-01', stock=9,  status='0',
  intro='[b]AI 微短剧全流程制作[/b]：剧本生成、AI 视频生成、数字人出镜、剪辑调色到平台投放，[quote]一个人 + AI = 一条微短剧生产线[/quote]2026 年一季度上新微短剧中 AI 制作占比已超 [color=#c65d43]95%[/color]，[url=#]了解行业报告 →[/url]'
  WHERE isbn='9787536692930';
UPDATE book SET book_name='超级数字人实操课',        author='塔链人工智能科技',     book_type='1', publisher='塔链科技',          price=99.00,  publish_date='2026-07-15', stock=3,  status='0',
  intro='本土自研数字人平台实操：形象定制、语音克隆、直播带货、短视频口播全场景落地。名额紧张，手慢无。'
  WHERE isbn='9787111544937';
UPDATE book SET book_name='共享工位月租计划',        author='数智游民创新工场',     book_type='3', publisher='天安智谷产业园',    price=500.00, publish_date='2026-06-01', stock=20, status='1',
  intro='（已满租）B 类付费入驻：小额租金 + 分摊水电网络费，工位注册 / 地址挂靠一站解决。下一批工位开放敬请关注。'
  WHERE isbn='9787505732534';
UPDATE book SET book_name='AI 短视频代运营服务',     author='正经点赞（清远）媒体科技', book_type='1', publisher='正经点赞',        price=0.00,   publish_date='2026-07-20', stock=50, status='0',
  intro='免费体验：文旅推广、政务科普、城市形象类 AI 短视频全链路代运营，订单由社区统一对接分发。'
  WHERE isbn='9787506365437';
UPDATE book SET book_name='一人公司法律咨询包',      author='李律师团队',           book_type='2', publisher='广东观澜律师事务所',price=299.00, publish_date='2026-07-10', stock=8,  status='0',
  intro='OPC 专属：一人公司注册、股权架构、合同审查、合规咨询一站式服务包。'
  WHERE isbn='9787544253994';
UPDATE book SET book_name='数字游民共居空间',        author='清远青年社区',         book_type='3', publisher='碧桂园清远',        price=800.00, publish_date='2026-07-25', stock=2,  status='0',
  intro='共居 + 共创：按月租入住共居空间，含共享工位与社区活动，适合数字游民长期扎根清远。'
  WHERE isbn='9787020029532';
UPDATE book SET book_name='AIGC 内容创作营',         author='正经点赞（清远）媒体科技', book_type='1', publisher='正经点赞',        price=149.00, publish_date='2026-08-19', stock=30, status='0',
  intro='新服务：AIGC 图文/视频/多平台内容生产实训，从创意到商业化的完整链路。'
  WHERE isbn='9787530216781';
UPDATE book SET book_name='一人公司财税合规指南',    author='王会计工作室',         book_type='2', publisher='清远税务学会',      price=0.00,   publish_date='2026-06-28', stock=25, status='0',
  intro='OPC 必学：一人公司企业所得税/分红个税/年度审计要求，与个体户税负对比，财税小白听得懂。'
  WHERE isbn='9787020002207';
UPDATE book SET book_name='AI 获客系统实战营',       author='星火深智',             book_type='2', publisher='星火深智',          price=169.00, publish_date='2026-07-08', stock=28, status='0',
  intro='本土自研 AI 获客系统：线索挖掘、智能触达、客户培育全流程自动化，一人公司获客不再难。'
  WHERE isbn='9787020008735';
UPDATE book SET book_name='GEO 生成式引擎优化课',    author='清远 AI 实验室',       book_type='2', publisher='清远 AI 实验室',    price=169.00, publish_date='2026-07-12', stock=26, status='0',
  intro='GEO（Generative Engine Optimization）：让你的品牌与产品在大模型回答中被优先推荐。'
  WHERE isbn='9787020008728';
UPDATE book SET book_name='数字游民保险方案咨询',    author='平安保险清远',         book_type='3', publisher='中国平安',          price=0.00,   publish_date='2026-07-05', stock=24, status='0',
  intro='灵活就业社保、补充商业险、意外险——数字游民与一人公司主理人的保障方案一对一咨询。'
  WHERE isbn='9787020008759';
UPDATE book SET book_name='AI 短视频运营训练营',     author='孙悦',                 book_type='1', publisher='抖音生活服务',      price=88.00,  publish_date='2026-06-20', stock=40, status='0',
  intro='爆款选题、AI 图文生成、笔记优化、涨粉变现全链路，往期学员 200+。'
  WHERE isbn='9787020042494';
UPDATE book SET book_name='视频号 AI 剪辑速成',      author='周涛',                 book_type='1', publisher='微信视频号',        price=0.00,   publish_date='2026-07-18', stock=35, status='0',
  intro='剪映 + AI 工具快速出片：从素材管理到成片发布，一天学会日更节奏。'
  WHERE isbn='9787532748662';
UPDATE book SET book_name='具身智能训练场开放日',    author='数智游民创新工场',     book_type='3', publisher='天安智谷产业园',    price=0.00,   publish_date='2026-06-15', stock=0,  status='0',
  intro='（满员可候补）具身智能训练场开放日：机器人调试、场景演练、行业交流，名额释放自动通知。'
  WHERE isbn='9787544270878';
UPDATE book SET book_name='AI 生活场景训练场体验',   author='数智游民创新工场',     book_type='3', publisher='天安智谷产业园',    price=0.00,   publish_date='2026-08-18', stock=0,  status='0',
  intro='新服务：AI 生活场景训练场体验——智能家居、AI 陪伴、健康监测场景演示与试用。'
  WHERE isbn='9787544270879';
UPDATE book SET book_name='AI 工牌场景试用',          author='数智游民创新工场',     book_type='3', publisher='星链科技',          price=0.00,   publish_date='2026-07-22', stock=30, status='0',
  intro='AI 工牌：会议纪要与客户画像自动生成，一人公司商务场景提效神器，限时免费试用。'
  WHERE isbn='9787020008742';
UPDATE book SET book_name='AI 绘画与设计基础',        author='光年制造工作室',       book_type='2', publisher='光年制造',          price=0.00,   publish_date='2026-06-18', stock=32, status='0',
  intro='Midjourney / Stable Diffusion 入门：从提示词到商业级出图的工作流，本土设计工作室带练。'
  WHERE isbn='9787537812249';
UPDATE book SET book_name='AI 眼镜体验与评测',        author='数智游民创新工场',     book_type='3', publisher='天安智谷产业园',    price=0.00,   publish_date='2026-07-28', stock=27, status='0',
  intro='AI 眼镜实机体验与评测：第一视角 AI 助手、实时翻译、拍照识物，长生态链硬件尝鲜。'
  WHERE isbn='9787020009626';
UPDATE book SET book_name='OPC 创客训练营',           author='星链科技',             book_type='2', publisher='清远星链科技',      price=0.00,   publish_date='2026-07-30', stock=2,  status='0',
  intro='名额紧张：社区王牌训练营（已举办多期）——一人公司从 0 到 1：注册、税务、AI 工具矩阵、获客、订单全流程。'
  WHERE isbn='9787121022982';
UPDATE book SET book_name='AI 应用开发与微调课',      author='塔链人工智能科技',     book_type='2', publisher='塔链科技',          price=128.00, publish_date='2026-08-05', stock=10, status='0',
  intro='开源模型私有化部署与微调实战（70B 以内一体机可用），数据不出域，一人公司也能玩转模型。'
  WHERE isbn='9787111407010';
UPDATE book SET book_name='毕业生 AI 实训营',          author='北江人工智能产教融合研究院', book_type='2', publisher='北江产教融合研究院', price=0.00, publish_date='2026-08-20', stock=20, status='0',
  intro='新服务：面向本地高校毕业生与清远籍大学生的免费 OPC 创业实训营，结营对接社区订单。'
  WHERE isbn='9787101054033';

-- ============================================
-- 3. 报名/候补记录快照同步（borrow_record：0进行中/1已完成/2已截止；book_reserve：0候补中）
-- ============================================
UPDATE borrow_record SET reader_name='周舟', book_name='AI 微短剧制作实战营'      WHERE borrow_id=1;
UPDATE borrow_record SET reader_name='周舟', book_name='一人公司财税合规指南'      WHERE borrow_id=2;
UPDATE borrow_record SET reader_name='李想', book_name='超级数字人实操课'          WHERE borrow_id=3;
UPDATE borrow_record SET reader_name='王梅', book_name='AI 短视频代运营服务'       WHERE borrow_id=4;
UPDATE borrow_record SET reader_name='王梅', book_name='AI 短视频运营训练营'       WHERE borrow_id=5;
UPDATE borrow_record SET reader_name='吴挂', book_name='AI 绘画与设计基础'         WHERE borrow_id=6;
UPDATE book_reserve SET reader_name='李想', book_name='具身智能训练场开放日' WHERE book_id=15 AND card_no='JS20260002';
UPDATE book_reserve SET reader_name='周舟', book_name='具身智能训练场开放日' WHERE book_id=15 AND card_no='JS20260001';
UPDATE book_reserve SET reader_name='王梅', book_name='具身智能训练场开放日' WHERE book_id=15 AND card_no='JS20260003';
UPDATE book_reserve SET reader_name='Jerry',book_name='具身智能训练场开放日' WHERE book_id=15 AND card_no='DK';

-- ============================================
-- 4. 轮播 → 真实品牌文案（表无唯一索引，用 UPDATE）
-- ============================================
UPDATE sys_banner SET title='数智游民创新工场', subtitle='清远市首个人工智能 OPC 生态社区 ｜ 一个人 + AI，就是一家公司', link='', sort=1 WHERE title='数智游民创新工场';
UPDATE sys_banner SET title='三大赋能体系', subtitle='算力支持 · 政策金融 · 订单牵引，低门槛 AI 创业生态', link='', sort=2 WHERE title='AI 课程与服务';
UPDATE sys_banner SET title='欢迎入驻', subtitle='A 类免费合伙人 · B 类付费成员 ｜ 工位注册 / 地址挂靠一站解决', link='', sort=3 WHERE title='欢迎入驻';

-- ============================================
-- 5. 新闻 → 真实报道（正文纯文本；notice_id 1-3）
-- ============================================
UPDATE sys_notice SET notice_title='清远首个人工智能 OPC 生态社区正式揭牌运营', notice_type='2',
  notice_content='2026 年 8 月 1 日，清城区人工智能 OPC 生态社区在清远星谷科技园正式揭牌运营。从 7 月 11 日签约到揭牌仅用 21 天，被誉为"清城速度"。揭牌当天，塔链人工智能科技、南京世东智脑、正经点赞（清远）媒体科技等 6 家企业签约入驻，并与广东财贸职业学院、清远职业技术学院、广东岭南职业技术学院 3 所高校共建校外实践教学基地。'
  WHERE notice_id=1;
UPDATE sys_notice SET notice_title='首届人工智能 OPC 创客短视频创作大赛筹备启动', notice_type='1',
  notice_content='社区筹办 2026 首届人工智能 OPC 创客短视频创作大赛，并成立创客基金。社区将全面梳理文旅推广、政务科普、城市形象等领域的 AI 文创订单需求，联动星火深智等 AI 头部企业对接全国稳定内容订单，以订单和赛事吸引产业集聚。'
  WHERE notice_id=2;
UPDATE sys_notice SET notice_title='2026 年一季度上新微短剧中 AI 制作占比超 95%', notice_type='1',
  notice_content='行业数据显示，2025 年国内 AI 视频市场规模已突破 1200 亿元，预计 2030 年达 5800 亿元；2026 年一季度上新微短剧中，AI 制作占比已超过 95%。AI 微短剧、超级数字人、AIGC 内容创作等新业态正在成为数字经济的重要增长极。'
  WHERE notice_id=3;
