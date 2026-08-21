# 🏗️ 数智游民创新工场 · 清远 AI OPC 生态社区官网

> 基于 **RuoYi-Vue 3.9.2**（Spring Boot + Vue 2）二次开发的 OPC 生态社区官网系统
> 前台官网（8 大栏目）+ 后台管理（服务/成员/报名/候补/入驻申请/CMS/看板）+ 成员体系（注册/登录/报名/候补）· 完整业务闭环 · 学习与演示用

---

## ✨ 功能全景

### 🏙️ 官网前台（匿名可访问，`ruoyi-ui/public/` 静态页）

10 个页面 + 顶部导航 9 项（**首页 / 服务项目 / 走进社区 / 产业生态 / 政策赋能 / 入驻招商 / 人才培养 / 新闻动态 / 联系我们**），全部为真实社区信息（来源：《官网内容库》，与公开报道交叉验证）：

| 页面 | 说明 |
|---|---|
| **首页**（`shop.html`） | **全屏翻页式长滚动单页**：① Hero（25vh 轮播 + 公司整体简介双 CTA）→ ② 品牌理念（含市场数据）→ ③ 三大赋能 → ④ 产业生态（三大赛道 + 6 家入驻企业）→ ⑤ 服务概览（三大分类卡入口）→ ⑥ 新闻动态 → ⑦ 发展历程 & 联系我们（深蓝渐变模块）→ ⑧ CTA + 页脚。滚轮累积 18% 屏高平滑翻页（移动端自由滚动），模块间协调色阶交替，章节编号水印 + 左上角标题 |
| **服务项目**（`services.html`） | 全部服务列表：**顶部吸顶分类标签页**（全部服务 + 3 大分类，支持 `?type=` 直达与标签切换）+ 关键词搜索 + 排序 + 服务卡片网格（报名/满员候补按钮） |
| **服务详情**（`service.html?id=`） | 服务完整信息（主办方/合作机构/分类/费用/名额/介绍/报名须知）+ 报名/候补（乐观 UI）+ 相关服务 |
| **走进社区**（`about.html`） | 社区定位/合作模式/区位/发展历程/运营主体/OPC 法律与税务 |
| **产业生态**（`industry.html`） | 三大赛道 + 6 家首批入驻企业 + AI+ 产业融合 |
| **政策赋能**（`policy.html`） | 三大赋能体系 + 政策一览表 + **政策文件库**（CMS 动态加载 7 条政策卡片）+ 省级 78 号文解读（100/1000/10000 蓝图） |
| **入驻招商**（`join.html`） | A 类免费入驻（OPC 合伙人）/ B 类付费入驻 + 权益明细 |
| **人才培养**（`talent.html`） | 3 所高校合作 + 培训体系 + 人才生态 |
| **新闻动态**（`news.html`） | CMS 文章列表（栏目 Tab 筛选：新闻/政策/活动/故事）+ 分页加载 |
| **文章详情**（`article.html?id=`） | 新闻/政策共用详情页，政策原文链接解析为可点击（白名单协议） |

- 👤 **成员体系**：自助注册（成员编号**后端自动生成**，防伪造/占坑）、登录（**电子邮箱必填**，用于报名/候补等自动邮件通知）、会话 token（Redis 30 分钟）校验归属
- ⚡ **乐观 UI**：报名/候补点击瞬间按钮即变，出错自动回滚；锁粒度按服务拆分
- 📝 **入驻申请**：前台提交入驻意向（项目/组织名称 + 联系人 + 申请说明 + 邮箱）→ 后台处理（待处理/已处理/已拒绝，同名称防重复）
- 🎠 **轮播图管理**：后台增删改前台轮播（上传图片/排序/启用停用）
- 🎨 **视觉体系**：深空科技蓝配色（CSS 变量化，全站统一）、模块化色阶、滚动动画（`prefers-reduced-motion` 自动禁用）
- 📱 **手机自适应**：≤768px 自动切换移动版布局（首页取消强制翻页）

### 🔧 后台管理（管理员视角）

- 🧩 **服务信息管理**：增删改查、**封面图片上传**、服务编号/分类/介绍、在架/下架、名额预警标签、Excel 导入导出
- 🎚️ **上下架开关**：列表状态列拨动开关直接上下架（不用进编辑页）；有候补中/有名额候补的服务禁止下架（联动校验）
- 📥 **Excel 批量导入**：服务/成员页上传 xlsx 批量新增（模板下载、逐行校验、同名/编号判重跳过、失败行号明细；编号留空自动生成）
- 🚨 **名额预警**：后台首页自动列出剩余名额低于阈值（`book.stock.warn` 可配）的在架服务提醒，点击直达低名额列表
- 👤 **成员管理**：编号自动生成/手填、类型字典、性别/状态、停用/挂失、历史记录入口、Excel 导出
- 📋 **报名管理**：报名/完成/延期、逾期红色标记、按服务/成员筛选、**Excel 导出**
- 🪪 **挂失补办**：前台自助申请（姓名+登记手机号）+ 后台一键补办，旧编号作废、历史快照保留
- 📱 **我的信息**：前台登录后自助修改手机号/邮箱（编号+姓名校验）
- 📧 **自动邮件通知**：报名成功 / 延期成功 / 候补成功 / 候补有名额提醒 / 入驻申请处理结果 自动发邮件（QQ 邮箱 SMTP，异步"尽力而为"不阻断业务，授权码走环境变量 `MAIL_AUTH_CODE` 不进代码库）
- 📊 **运营看板**：首页业务统计 + 热门服务 Top5 图表 + 快捷入口；**报名统计**：热门服务 Top10、成员报名排行 Top10
- 🗂️ **文章管理 CMS**：新闻动态等栏目内容的后台增删改查
- 🔐 **角色权限分级**：多角色（服务/成员/运营等）+ 测试账号（RBAC 完整落地）
- 🛡️ **数据一致性**：记录快照冗余——删除成员或服务后历史记录依然完整可查；删除有未完成报名/待处理申请/候补中的服务或成员自动拦截

### 🤖 自动化（Quartz 定时任务）

| 任务 | 时间 | 行为 |
|---|---|---|
| 逾期检查 | 每天 0:00 | 自动标记逾期报名 |
| 逾期催办公告 | 每天 9:00 | 有逾期自动发布催办公告（当天只发一次） |
| 公告回收 | 完成时触发 | 逾期清结 → 公告自动删除；仍有逾期 → 重新汇总发布 |
| 候补超时释放 | 定时 | '有名额'状态超 N 天未完成报名 → 自动取消并通知下一位候补成员 |

## 📚 真实内容（官网内容库）

- **21 条 AI 服务 / 新闻**：全部为真实社区信息（清城区人工智能 OPC 生态社区，2026-08-01 揭牌，"国企引领、民企赋能"，星谷科技园/天安智谷产业园，咨询电话 0763-3391888 等），来源与口径见 [《官网内容库》](docs/官网内容库.md)
- 前台 8 栏目静态页 + 服务中心均直接引用该内容库；后台 CMS 可维护新闻动态等栏目

## 🛠️ 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Spring Boot · Spring Security · JWT · MyBatis · Redis · Quartz |
| 前端 | Vue 2 · Element UI · Axios · 官网前台原生静态页（HTML/CSS/JS） |
| 数据库 | MySQL 8 |
| 部署 | Maven 打包 · Nginx 静态托管 · Docker 一键编排（详见部署指南） |

## 🚀 一键启动（Docker，推荐新用户）

从 GitHub 克隆仓库后，**只需安装 Docker**（Windows: Docker Desktop；无需 MySQL/Redis/Java/Node）：

```bash
git clone https://github.com/JerryVon39/OPC.git
cd OPC

# Windows：双击 scripts\start-all.bat，或在命令行运行
scripts\start-all.bat
# Linux/macOS：
./scripts/start-all.sh
```

脚本会自动：检查/自动启动 Docker Desktop（未运行时自动拉起并等待就绪）→ 生成 `.env`（默认配置可直接启动）→ 检查本地 v2.0 镜像（缺失则自动用源码构建）→ 启动 MySQL/Redis/后端/前端四容器 → 等待后端就绪。

> 💡 **镜像说明**：本项目为私有项目，镜像不推 Docker Hub——`start-all.bat` 检测到本地无 v2.0 镜像时自动 `docker compose build` 构建（首次需几分钟）。

- 管理后台：`http://localhost/`（默认账号 admin / admin123，首次登录请修改密码）
- 官网前台：`http://localhost/shop.html`
- 停止：`scripts\stop-all.bat` / `./scripts/stop-all.sh`（数据保留，重跑 start 即恢复）

> 💡 可选配置：编辑 `.env` 可设置数据库/Redis 口令、JWT 密钥（`TOKEN_SECRET`，生产务必设置）、邮件通知（`MAIL_USERNAME`/`MAIL_AUTH_CODE`，QQ 邮箱 SMTP 授权码）。全部口令默认值仅面向演示，**生产部署务必修改**。
>
> 💡 需要自己改代码时，可 `docker compose up -d --build` 用本地代码重新构建镜像（compose 保留了 build 配置）。

## 🚀 快速开始（本机开发，脚本一键启动）

已有 JDK 17 / Node.js（以及本机 MySQL、Redis，或用 Docker）的开发者，推荐用脚本：

```bash
git clone https://github.com/JerryVon39/OPC.git
cd OPC

# Windows：双击 scripts\start-local.bat，或在命令行运行
scripts\start-local.bat
# Linux/macOS：
./scripts/start-local.sh
```

脚本自动完成：生成 `.env`（默认配置可直接启动）→ 启动/复用 MySQL 与 Redis（本机原生优先，无则自动用 Docker 拉起容器）→ 首次自动建库导入 `sql/` 初始化脚本、已有库自动跑增量升级 → 构建并启动后端（8080）→ 启动前端（8081）→ 打印访问地址。

- 管理后台：`http://localhost:8081/`（默认账号 admin / admin123，首次登录请修改密码）
- 官网前台：`http://localhost:8081/shop.html`
- 前端端口可在 `.env` 用 `FE_PORT` 修改；本机原生 MySQL/Redis 不在 PATH 时可设 `TOOLS_HOME` 指向其安装目录
- 停止：`scripts\stop-local.bat` / `./scripts/stop-local.sh`（数据保留，重跑 start 即恢复）
- 统一管理：`scripts\svc.bat start|stop|status|restart`（一键启动/停止/查看四服务状态，端口检测自动跳过已在运行的服务）
- 服务化注册（开机自启 + 崩溃自动重启）：管理员运行 `scripts\install-services.bat`（nssm 注册 MySQL/Redis/后端为 Windows 服务），卸载用 `scripts\uninstall-services.bat`

### 手动启动（可选）

```bash
# 1. 初始化数据库（依次导入；务必带 --default-character-set=utf8mb4，
#    否则 Windows 终端按 GBK 读脚本会把中文读坏入库（菜单乱码/登录报错））
mysql --default-character-set=utf8mb4 -uroot -p < sql/ry_20260417.sql        # 若依系统表
mysql --default-character-set=utf8mb4 -uroot -p < sql/quartz.sql             # 定时任务表
mysql --default-character-set=utf8mb4 -uroot -p < sql/business_init.sql      # 业务表+字典+菜单（全新库初始化）

# 已有旧库升级到当前版本时，business_init.sql 顶部建表语句在已有库会报"表已存在"，
# 只需额外执行增量升级脚本（幂等）：
mysql --default-character-set=utf8mb4 -uroot -p ry-vue < sql/upgrade_20260821_official.sql   # 官网化业务重映射（服务/成员/报名/候补/入驻申请）

# 2. 配置数据库连接
#    编辑 ruoyi-admin/src/main/resources/application-druid.yml（账号密码）

# 3. 启动后端
mvn -pl ruoyi-admin -am package -DskipTests
java -jar ruoyi-admin/target/ruoyi-admin.jar

# 4. 启动前端
cd ruoyi-ui
npm install
npm run dev
```

> 💡 **邮件通知**（可选）：新成员登记需填邮箱，报名/候补等自动发信。在项目根目录建 `.env` 文件写入
> `MAIL_AUTH_CODE=你的QQ邮箱授权码` 和 `MAIL_USERNAME=你的发件邮箱`（启动脚本自动读取注入）；不配则邮件不发送、业务不受影响。

- 后台地址：`http://localhost:8081/`（默认账号 admin / admin123）
- 官网前台：`http://localhost:8081/shop.html`（访客视角，无需登录）

> 💡 前端端口在 `ruoyi-ui/vue.config.js` 中配置（默认 80，本机被占用时可改端口）。

## 📁 项目结构

```
├── ruoyi-admin        # 启动模块（配置、定时任务 BorrowTask）
├── ruoyi-framework    # 框架层（安全/拦截器/切面）
├── ruoyi-system       # 业务模块（book/reader/borrow/reserve/purchase/cms 的 controller/service/mapper）
├── ruoyi-common       # 公共工具
├── ruoyi-generator    # 代码生成器
├── ruoyi-quartz       # 定时任务框架
├── ruoyi-ui           # 前端（views/system/{book,reader,borrow,reserve,purchase,cms}、public/ 官网静态页）
├── scripts            # 运维脚本（svc.bat 服务管理 / start-local 本地启动 / start-all Docker / backup-db 备份 / smoke_official 冒烟）
├── docs               # 文档（官网内容库/部署指南/代码审查报告；若依学习资料归档）
└── sql                # 数据库脚本（business_init.sql 业务初始化，幂等）
```

**核心业务代码位置**：
- 报名业务规则（报名/完成/名额联动/逾期/公告回收）：`ruoyi-system/.../service/impl/BorrowRecordServiceImpl.java`
- 入驻申请业务（校验/去重/事务）：`ruoyi-system/.../service/impl/BookPurchaseReqServiceImpl.java`
- 候补业务（队列/推进/通知）：`ruoyi-system/.../service/impl/BookReserveServiceImpl.java`
- 定时任务：`ruoyi-admin/.../quartz/task/BorrowTask.java`
- 官网前台：`ruoyi-ui/public/shop.html`（首页，全屏翻页单页）、`services.html`（服务列表）、`service.html`/`article.html`（详情页）、`about/industry/policy/join/talent/news.html`（栏目页）

## 🔐 前台成员登录会话（2026-08-17 加固）

前台成员登录后由后端签发 **短期会话 token**（存 Redis，30 分钟有效），我的报名/候补/入驻申请查询与报名/延期/取消等操作均需携带 `sessionToken` 并由后端校验归属，不再信任前端提交的成员编号，降低冒用他人编号的风险。未登录时相关入口会提示先登录。

## 🛡️ 并发与数据一致性（持续加固）

- 完成报名、前台/后台取消、删除待处理记录全部改为 **原子状态转换**（`WHERE ... AND status = 期望值`），只有状态转换成功的请求才回补名额，并发重复操作不会造成名额虚高
- 报名/候补/下单/删除成员以 **FOR UPDATE 锁成员行**（删除服务锁服务行），同一成员的并发操作串行化、检查与写入原子化；配合 **READ_COMMITTED 隔离级别** 修复并发重复报名/重复候补漏检
- 单元测试补至 **96 用例**（含并发取消失败不回补名额、加锁后成员被并发删除等场景）

## 🧪 自动化测试（双保险）

| 层级 | 怎么跑 | 覆盖 |
|---|---|---|
| 单元测试 | `mvn test`（秒级，不依赖数据库） | 报名规则/候补校验/申请校验/删除还原名额/并发取消防双回补/编号生成·查重/统计缓存/行锁路径/并发删除守卫/批量导入判重与错误收集等 |
| 集成冒烟 | `python -u scripts/smoke_official.py`（需后端已启动） | 公开接口（服务列表/数据条/字典/新闻）→ 成员登录 → 报名/重复拒绝/我的报名 → 满员候补 → 入驻申请 → 后台登录与菜单树（与新种子数据耦合，可重复执行） |

> 💡 `python` 命令若被 Windows 应用商店的 python 占位拦截，请用本机 Python 的完整路径运行，或先卸载应用商店占位。

## 🛡️ 部署与运维

- [《部署指南》](docs/部署指南.md)：本机启动 / 正式部署（前端构建 + Nginx 反向代理）/ **cpolar 内网穿透在线演示** / Docker（可选）/ 首次初始化 / 升级回滚
- [《数据库备份脚本》](scripts/backup-db.bat)：一键 mysqldump 备份，可挂 Windows 任务计划定期执行
- [《Docker 发布脚本》](scripts/publish-docker.bat)：一键重新构建 → 推送 Docker Hub → 同步 compose 版本号（`scripts\publish-docker.bat v1.2`）
- **Docker 化已实机验证**：镜像构建 → 四容器编排 → 全新库初始化 → 前后端联通全链路通过

## 📖 文档

- [《官网内容库》](docs/官网内容库.md)：官网各栏目/服务/新闻的真实内容真源
- [《部署指南》](docs/部署指南.md)：启动/部署/备份/升级全流程
- [《代码审查报告》](docs/代码审查报告.md)：早期业务系统的代码审查记录
- [《若依学习资料》](docs/若依学习资料/)：若依框架学习手册归档（《RuoYi-Vue 使用指南》《代码生成器使用流程》《测试用例》，已不随项目文档维护，保留学习价值）

## 🗂️ Git 提交轨迹

### 早期业务系统阶段（精简合并）

| 提交 | 内容 |
|---|---|
| `b5e1433` | 原始若依源码（RuoYi-Vue 3.9.2） |
| `76917c5` ~ `81d87010` | 品牌定制 + 一期业务系统：成员登记/登录、报名办理、统计看板、公告、角色权限分级（官网化后语义映射为成员/报名/候补/入驻申请业务） |
| `004dc961` ~ `e3511b54` | 候补与订单体系：候补队列、超时释放、订单号唯一约束（官网化后语义映射为候补/入驻申请业务） |
| `9e5abda5` ~ `c5569295` | 费用结算与成员自助服务：挂失补办、前台自助办理、登录态优化 |
| `6315828f` ~ `f722e174` | 数据导入与体验优化：Excel 批量导入、搜索联想、名额预警、乐观 UI、上下架开关 |

### 最近持续迭代（明细）

| 提交 | 内容 |
|---|---|
| `011ffa3f` | 审查修复（前端 4 项）：乐观锁粒度/失焦残影/开关防重/加载 catch |
| `a5a9b5fd` | 审查修复：搜索标题栏用户输入转义（修复 DOM XSS） |
| `4fb855cc` | 独立复核修复：isAsc 排序白名单 + 联想回车路径失效在途请求 |
| `936ec656` | Docker：compose 绑定 Docker Hub 线上镜像（部署方免源码一键拉起） |
| `5e028ed5` | CI 加单元测试步骤 + Docker 一键发布脚本 |
| `d4bbbe3d` | 修复 M1 并发竞态：下架与新建候补/报名共享服务行锁 |
| `6315828f` | Excel 批量导入（服务/成员，模板下载+逐行校验+判重跳过）/ 名额预警；单测 108、集成 65 |
| `e36b5fb3` | 新增导入测试 Excel（早期测试文件，已移除；官网冒烟用 smoke_official.py） |
| `f204dbd7` | 业务菜单拆分：3 个二级目录（服务管理/成员服务/合作经营），角色权限自动跟随 |
| `206b4d79` | 死代码清理 + 部署指南补 Druid 监控台安全提醒 |
| `cf34a1ca` | 前台右上角优化：次要功能收纳进"☰ 更多"下拉菜单 |
| `27f56ab8` | 删除恢复功能：删除服务/成员进快照可还原（快照表 + 后台页面 + 还原/彻底删除/清空，权限隔离，集成测试 72 项） |
| `7b74447a` | 自动邮件通知：MailUtil 异步发信（授权码走 MAIL_AUTH_CODE 环境变量）、邮箱必填（前台/后台/导入）、5 个邮件触发点（报名/延期/候补/有名额/申请结果） |
| `199b4a4b` | 邮件配置引导：启动脚本注入授权码 + 启动自检告警（防"业务正常但邮件发不出"） |
| `fa6c4541` | 一键启动脚本邮件配置：自动读取 .env 注入授权码，bat CRLF 修复，实测邮件发送成功 |
| `efd1817b` | 移除代码库中真实邮箱——发件邮箱改环境变量注入（敏感信息清理） |
| `16b4b05b` | Docker v1.1 发布：镜像推送 Docker Hub，compose 同步版本号 |
| `6e23ac0d` | 整理内置演示数据：移除冗余条目、补齐信息、新增演示成员 |
| `1c320b40` | 同步本地完整代码 + 当日审查修复：逾期公告按真实日期兜底、前台报名事务修复（自调用绕过代理）、页面跳转修复、下单锁服务行消除下架竞态 |
| `091f905e` | 二轮审查修复：记录编辑放行动态状态、取消/删除全路径 CAS 防并发双回补、订单号唯一索引落地 |
| `69cd6d49` | 四轮审查修复（名额一致性并发加固）：全流转 CAS、候补推进 CAS 不卡队、有名额候补优先完成、定时任务条件置逾期；导入行级容错/手机号校验/公告去姓名/Safari 日期解析；单测 12 类全绿 |
| `3d4a556f` | 修复 Docker 部署前台打不开：API 前缀全环境统一 /prod-api；dist 重新构建；本地重建镜像实测前台正常 |
| `354f4b7d` | 清除若依框架残留（业务化改写）：公告改社区业务内容、岗位改官网岗位结构、删若依官网菜单；投放 Docker 初始化 + 本地升级清单 |
| `0c55e79d` | Docker v1.2 发布：镜像推送 Docker Hub（含四轮审查修复 + 前台前缀修复 + 若依残留清理），compose 同步版本号 |
| `2026-08-21` | **官网化改造**：数智游民创新工场官网（8 栏目前台 + 服务/成员/报名/候补/入驻申请/CMS/运营看板后台 + 成员体系 + 21 条真实 AI 服务与新闻），业务语义全面映射为官网语义，文档/注释/注解同步清理 |
| `2026-08-21` | **前台大改版**：深空科技蓝换肤（CSS 变量化）、首页全屏翻页式长滚动单页（8 模块叙事 + 滚轮平滑翻页）、服务列表独立页（顶部吸顶分类标签页 + `?type=` 直达）、服务/文章详情页独立化（替换弹窗）、政策文件库（7 条政策 + 78 号文解读）、导航修复与「服务项目」入口、公告/数据条/搜索栏移除、死代码清理 |

## 💾 数据库数据自动同步 GitHub（git 钩子）

**你的问题**：在后台增删的成员/服务/文章只存在本地数据库，如何让 GitHub 仓库也同步？

**方案（已内置）**：仓库内置 `.githooks/pre-commit` 钩子——**每次 `git commit` 前自动把当前数据库的业务数据（13 张表：成员/服务/报名/候补/申请/文章/轮播/公告/字典）导出为 `sql/data_snapshot.sql` 并加入提交**。你正常 commit + push，数据就跟着走了；别人 clone 后导入该文件即可看到你的全部改动。

**使用**：
1. 首次配置（仓库内执行一次）：`git config core.hooksPath .githooks`
2. 日常照常 `git commit` / `git push`——数据快照自动随提交更新
3. 数据库未运行时钩子自动跳过（不阻塞提交）；也可手动运行 `scripts\export-data.bat` 刷新快照

**数据导入（对方/新机器）**：
```bash
mysql --default-character-set=utf8mb4 -uroot -p ry-vue < sql/data_snapshot.sql   # REPLACE 模式，可重复执行
```

**注意**：快照包含全部业务数据（成员邮箱等真实信息）——私密仓库适用；若转公开仓库请先脱敏。

## 📜 声明

本项目基于开源框架 **RuoYi-Vue**（[gitee.com/y_project/RuoYi-Vue](https://gitee.com/y_project/RuoYi-Vue)）二次开发，遵循 MIT 开源协议。仅用于学习交流。

---

*数智游民创新工场 · 官网系统 · 学习项目 · 2026*
