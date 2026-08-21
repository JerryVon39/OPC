-- ============================================
-- 升级脚本：政策文件库 + 政策新闻 v20260826（数智游民创新工场）
-- 内容：cms_category 新增「政策文件」栏目 + 7 篇政策文章（政策.md 全量）
--       + 3 篇政策新闻（省级 78 号文/高新区绿色通道/孵化基地免租）
-- 适用：存量库（在 upgrade_20260822_cms.sql 之后执行）；全新库直接执行
-- 幂等：可重复执行（栏目按名称判重、文章按标题判重）
-- 注意：必须指定字符集执行，否则中文乱码入库：
--   mysql --default-character-set=utf8mb4 -uroot -p ry-vue < sql/upgrade_20260826_policy.sql
-- 数据来源：C:\Users\1\Desktop\OPC\真实信息\政策.md + 省级政策 PDF（粤发改高技〔2026〕78号，OCR 提取）
-- ============================================

USE ry-vue;

-- ============================================
-- 1. 栏目：政策文件（挂一级栏目，sort=5，位于入驻故事之后）
-- ============================================
INSERT INTO cms_category (category_name, parent_id, sort, status, create_by, create_time)
SELECT '政策文件',0,5,'0','admin',NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM cms_category WHERE category_name='政策文件');

-- ============================================
-- 2. 政策文章 7 篇（幂等：按标题判重；正文纯文本，前台 textContent 渲染）
-- ============================================

-- 2.1 市级：《清远市人工智能创新发展行动方案（2026—2027年）》
INSERT INTO cms_article (category_id, title, summary, content, author, is_top, status, views, publish_time, create_by, create_time)
SELECT c.category_id,'《清远市人工智能创新发展行动方案（2026—2027年）》','清远市级人工智能纲领性文件，指导全市人工智能产业发展与 OPC 生态社区建设。','【政策名称】《清远市人工智能创新发展行动方案（2026—2027年）》
【发文级别】清远市级
【文号】清府办函〔2026〕...
【状态】已出台
【政策要点】清远市推进人工智能创新发展的纲领性文件，统筹全市人工智能产业布局，支持人工智能 OPC 生态社区建设。
【原文链接】暂无直接官方链接，可通过清远市政府官网（http://www.gdqy.gov.cn）检索获取。','数智游民创新工场','0','0',0,NOW(),'admin',NOW()
FROM cms_category c WHERE c.category_name='政策文件'
AND NOT EXISTS (SELECT 1 FROM cms_article WHERE title='《清远市人工智能创新发展行动方案（2026—2027年）》');

-- 2.2 区级：清城区人工智能 OPC 生态社区战略合作框架协议
INSERT INTO cms_article (category_id, title, summary, content, author, is_top, status, views, publish_time, create_by, create_time)
SELECT c.category_id,'清城区人工智能 OPC 生态社区战略合作框架协议','清城区政府与清远星链科技签署共建，21 天揭牌跑出"清城速度"。','【政策名称】清城区人工智能 OPC 生态社区战略合作框架协议
【发文级别】清城区级
【状态】已签署落地
【政策要点】2026 年 7 月 11 日，清城区政府与清远市星链科技有限公司签署合作框架协议，共建清城区人工智能 OPC 生态社区（数智游民创新工场）；2026 年 8 月 1 日正式揭牌运营，从签约到揭牌仅用 21 天，被誉为"清城速度"。
【媒体报道】南方日报：https://epaper.nfnews.com/nfdaily/html/202607/17/content_10175934.html；清远日报：https://www.qyrb.cn；中国日报：https://cnews.chinadaily.com.cn；新浪财经：https://finance.sina.cn','数智游民创新工场','0','0',0,NOW(),'admin',NOW()
FROM cms_category c WHERE c.category_name='政策文件'
AND NOT EXISTS (SELECT 1 FROM cms_article WHERE title='清城区人工智能 OPC 生态社区战略合作框架协议');

-- 2.3 高新区：OPC 登记注册绿色通道
INSERT INTO cms_article (category_id, title, summary, content, author, is_top, status, views, publish_time, create_by, create_time)
SELECT c.category_id,'清远高新区 AI OPC 登记注册绿色通道','一窗通办、一日办结，探索"集群注册"无需实体地址的注册模式。','【政策名称】清远高新区第十二期政企恳谈会——人工智能 OPC 登记注册绿色通道
【发文级别】清远高新区
【状态】已实施
【政策要点】设立人工智能 OPC 登记注册"明晰指引"和"绿色通道"，实行"一窗通办""一日办结"；探索"集群注册"等无需实体地址的注册模式，破解"工位注册""地址挂靠"等落地堵点。
【相关链接】https://www.qysed.cn','数智游民创新工场','0','0',0,NOW(),'admin',NOW()
FROM cms_category c WHERE c.category_name='政策文件'
AND NOT EXISTS (SELECT 1 FROM cms_article WHERE title='清远高新区 AI OPC 登记注册绿色通道');

-- 2.4 市级：省市共建创业孵化基地（免租金）
INSERT INTO cms_article (category_id, title, summary, content, author, is_top, status, views, publish_time, create_by, create_time)
SELECT c.category_id,'清远市省市共建创业孵化基地（免租金）','入驻省市共建创业孵化基地享受 3-4 年免租金；OPC 项目最高 300㎡ 3 年免费。','【政策名称】清远市省市共建创业孵化基地（免租金）
【发文级别】清远市级
【状态】已实施
【政策要点】入驻清远市省市共建创业孵化基地，享受 3 至 4 年免租金支持；对符合条件的数字游民项目，入驻市级孵化基地最高可获 300 平方米 3 年免费使用权。
【相关链接】https://www.gdqy.gov.cn','数智游民创新工场','0','0',0,NOW(),'admin',NOW()
FROM cms_category c WHERE c.category_name='政策文件'
AND NOT EXISTS (SELECT 1 FROM cms_article WHERE title='清远市省市共建创业孵化基地（免租金）');

-- 2.5 高新区：《关于促进省职教城清远高新区凤翔谷发展的若干措施（试行）》
INSERT INTO cms_article (category_id, title, summary, content, author, is_top, status, views, publish_time, create_by, create_time)
SELECT c.category_id,'《关于促进省职教城清远高新区凤翔谷发展的若干措施（试行）》','大学毕业生到高新区孵化载体创办企业，最长 24 个月场租补贴（20 元/㎡/月，最高 200㎡）。','【政策名称】《关于促进省职教城清远高新区凤翔谷发展的若干措施（试行）》
【发文级别】清远高新区
【状态】已实施
【政策要点】大学毕业生到省职教城清远高新区孵化载体创办企业，给予最长 24 个月场租补贴，标准为 20 元/平方米/月，最大补贴面积不超过 200 平方米。
【相关链接】https://www.gdqy.gov.cn','数智游民创新工场','0','0',0,NOW(),'admin',NOW()
FROM cms_category c WHERE c.category_name='政策文件'
AND NOT EXISTS (SELECT 1 FROM cms_article WHERE title='《关于促进省职教城清远高新区凤翔谷发展的若干措施（试行）》');

-- 2.6 省级：《广东省支持人工智能 OPC 创新发展行动方案（2026—2028年）》
INSERT INTO cms_article (category_id, title, summary, content, author, is_top, status, views, publish_time, create_by, create_time)
SELECT c.category_id,'《广东省支持人工智能 OPC 创新发展行动方案（2026—2028年）》','粤发改高技〔2026〕78 号：2028 年建成 100 个 OPC 生态社区、培育 1000 家标杆企业、集聚 10000 名人才。','【政策名称】《广东省支持人工智能 OPC 创新发展行动方案（2026—2028年）》
【发文级别】省级
【文号】粤发改高技〔2026〕78 号
【印发时间】2026 年 3 月 13 日
【状态】已出台（主动公开）
【总体目标】2026 年先行培育 10 个具有引领效应的人工智能 OPC 生态社区，形成一批年营业收入超千万的优质企业；到 2028 年，建成百个人工智能 OPC 生态社区，培育千家标杆企业，集聚万名创新创业人才，将广东打造成为全国领先的人工智能 OPC 发展高地。
【六大任务】一、加强基础能力保障：算力券制度、算力公共服务平台、"政府—社区—平台—OPC"联动机制、公共数据开放与授权运营、免费模型接口与安全合规检测的公共模型服务平台；二、完善空间载体配套：打造"拎脑入驻"的 OPC 生态社区并给予算力补贴、省级社区认定评估、传统孵化器转型；三、推动场景开放创新：构建短剧短视频/电子商务/知识付费/数字文创/软件开发场景池、湾区应用场景发布厅、以赛促创（琶洲算法大赛）、跨境应用场景；四、拓宽多元融资渠道：创投基金矩阵（大湾区国家创投引导基金等）、"人才贷/研发贷/成果贷/算力贷"全周期信贷产品；五、强化人才政策供给：OPC 人才纳入高层次人才认定、高校"人工智能+"交叉学科、人工智能训练师职业技能等级认定、创业导师专家库、人才安居（"一张床、一间房、一套房"）；六、健全服务保障体系：企业开办"一日办结、一网通办"、知识产权质押处置、调解/公证/仲裁特色法律服务、人工智能应用中试基地、企业出海一站式服务（ODI 备案、跨境支付）。
【原文链接】广东省政府门户网站：https://www.gd.gov.cn；广东省科技厅：https://gdstc.gd.gov.cn','数智游民创新工场','1','0',0,NOW(),'admin',NOW()
FROM cms_category c WHERE c.category_name='政策文件'
AND NOT EXISTS (SELECT 1 FROM cms_article WHERE title='《广东省支持人工智能 OPC 创新发展行动方案（2026—2028年）》');

-- 2.7 市级（酝酿中）：数字政府领域通用人工智能应用政策
INSERT INTO cms_article (category_id, title, summary, content, author, is_top, status, views, publish_time, create_by, create_time)
SELECT c.category_id,'《清远市加快数字政府领域通用人工智能应用若干政策措施》（征求意见稿）','酝酿中政策：2024 年 1 月已结束意见征集，尚未正式出台。','【政策名称】《清远市加快数字政府领域通用人工智能应用若干政策措施》（征求意见稿）
【发文级别】清远市级
【状态】酝酿中（2024 年 1 月已结束意见征集，尚未正式出台）
【政策方向】加快数字政府领域通用人工智能应用，为 AI 企业参与数字政府建设提供政策指引。
【相关报道】https://qysme.com','数智游民创新工场','0','0',0,NOW(),'admin',NOW()
FROM cms_category c WHERE c.category_name='政策文件'
AND NOT EXISTS (SELECT 1 FROM cms_article WHERE title='《清远市加快数字政府领域通用人工智能应用若干政策措施》（征求意见稿）');

-- ============================================
-- 3. 政策新闻 3 篇（新闻动态栏目，幂等：按标题判重）
-- ============================================

INSERT INTO cms_article (category_id, title, summary, content, author, is_top, status, views, publish_time, create_by, create_time)
SELECT c.category_id,'广东出台行动方案：到 2028 年建成 100 个 AI OPC 生态社区','粤发改高技〔2026〕78 号发布，培育 1000 家标杆企业、集聚 10000 名创新创业人才，算力券、人才贷、训练师认定等支持全面覆盖。','2026 年 3 月，广东省发展改革委印发《广东省支持人工智能 OPC 创新发展行动方案（2026—2028年）》（粤发改高技〔2026〕78 号）。方案提出：2026 年先行培育 10 个具有引领效应的人工智能 OPC 生态社区；到 2028 年建成百个生态社区、培育千家标杆企业、集聚万名创新创业人才。支持内容覆盖算力券补贴、"拎脑入驻"、全周期信贷（人才贷/研发贷/成果贷/算力贷）、人工智能训练师职业技能等级认定、人才安居保障等。"数智游民创新工场"作为清远市首个人工智能 OPC 生态社区，将率先对接省级政策资源，为入驻企业提供一站式政策服务。','数智游民创新工场','1','0',0,DATE_SUB(NOW(), INTERVAL 1 DAY),'admin',NOW()
FROM cms_category c WHERE c.category_name='新闻动态'
AND NOT EXISTS (SELECT 1 FROM cms_article WHERE title='广东出台行动方案：到 2028 年建成 100 个 AI OPC 生态社区');

INSERT INTO cms_article (category_id, title, summary, content, author, is_top, status, views, publish_time, create_by, create_time)
SELECT c.category_id,'清远高新区开通 AI OPC 登记注册绿色通道','"一窗通办""一日办结"，探索"集群注册"等无需实体地址的注册模式，破解工位注册、地址挂靠堵点。','清远高新区第十二期政企恳谈会明确：设立人工智能 OPC 登记注册"明晰指引"和"绿色通道"，实行"一窗通办""一日办结"；探索"集群注册"等无需实体地址的注册模式，为数字游民与一人公司创业者破解"工位注册""地址挂靠"等落地堵点。依托清远星谷科技园布局的 AI 创客试点，"拎脑入驻"正在成为现实。','数智游民创新工场','0','0',0,DATE_SUB(NOW(), INTERVAL 3 DAY),'admin',NOW()
FROM cms_category c WHERE c.category_name='新闻动态'
AND NOT EXISTS (SELECT 1 FROM cms_article WHERE title='清远高新区开通 AI OPC 登记注册绿色通道');

INSERT INTO cms_article (category_id, title, summary, content, author, is_top, status, views, publish_time, create_by, create_time)
SELECT c.category_id,'省市共建孵化基地免租 3-4 年：OPC 项目最高 300㎡ 三年免费','凤翔谷措施同步为大学毕业生创业企业提供最长 24 个月场租补贴（20 元/㎡/月，最高 200㎡）。','清远市省市共建创业孵化基地面向创业团队开放，享受 3 至 4 年免租金支持；符合条件的 OPC 项目入驻市级孵化基地，最高可获 300 平方米 3 年免费使用权。《关于促进省职教城清远高新区凤翔谷发展的若干措施（试行）》同步实施：大学毕业生到高新区孵化载体创办企业，给予最长 24 个月场租补贴，标准为 20 元/平方米/月，最大补贴面积不超过 200 平方米，进一步降低大学生 OPC 创业的场地成本。','数智游民创新工场','0','0',0,DATE_SUB(NOW(), INTERVAL 5 DAY),'admin',NOW()
FROM cms_category c WHERE c.category_name='新闻动态'
AND NOT EXISTS (SELECT 1 FROM cms_article WHERE title='省市共建孵化基地免租 3-4 年：OPC 项目最高 300㎡ 三年免费');

-- ============================================
-- 4. 完成提示
-- ============================================
SELECT CONCAT('政策文件库已就绪：政策文件栏目 ',
              (SELECT COUNT(*) FROM cms_category WHERE category_name='政策文件'),
              ' 个，政策文章 ',
              (SELECT COUNT(*) FROM cms_article a JOIN cms_category c ON a.category_id=c.category_id WHERE c.category_name='政策文件'),
              ' 篇') AS result;
