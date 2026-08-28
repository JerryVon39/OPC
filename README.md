# 🏗️ 数智游民创新工场 · 清远 AI OPC 生态社区官网

> 基于 **RuoYi-Vue 3.9.2**（Spring Boot + Vue 2）二次开发的 OPC 生态社区官网系统
> 前台官网（8 大栏目静态页 + CMS 动态内容）+ 后台管理（**CMS 内容运营为核心**：文章/栏目/区块搭建/公告/轮播/报表 + 成员管理 + 系统设置）· 面向非程序员运营 · 私有项目（仅老板与团队可见）

---

## ✨ 功能全景

### 🏙️ 官网前台（匿名可访问，`ruoyi-ui/public/` 静态页）

前台为原生静态页 + `js/site.js` 调后端公开接口渲染，页面内容（首页模块/各栏目页区块）由后台**区块管理**搭建维护：

| 页面 | 说明 |
|---|---|
| **首页**（`home.html`） | 模块化长滚动单页：轮播 + 公司简介 → 品牌理念 → 三大赋能 → 产业生态 → 新闻动态 → 发展历程 & 联系我们 → CTA；**区块由后台「区块管理」搭建**（模板驱动 + 实时预览） |
| **走进社区**（`about.html`） | 社区定位/合作模式/区位/发展历程/运营主体/法律与税务（区块管理维护） |
| **产业生态**（`industry.html`） | 三大赛道 + 首批入驻企业 + AI+ 产业融合（区块管理维护） |
| **政策赋能**（`policy.html`） | 政策文件库 + 政策解读（CMS 文章动态加载，栏目归属页可配置） |
| **入驻招商**（`join.html`） | A/B 类入驻 + 权益明细 + **入驻申请表单**（匿名提交 → 后台处理 + 邮件通知运营者） |
| **人才培养**（`talent.html`） | 高校合作 + 培训体系（区块管理维护） |
| **新闻动态**（`news.html`） | CMS 文章列表（**两级栏目导航**：一级顶层栏目 Tab + 二级子栏目条，父栏目自动汇总子孙文章；搜索 + 服务端分页） |
| **文章详情**（`article.html?id=`） | 新闻/政策共用详情页：浏览量防刷计数、上一篇/下一篇、**OG 分享卡片**、政策原文附件 |

- 👤 **成员体系**：注册（邮箱验证码 + 编号自动生成）、登录（证号/手机号/邮箱 + BCrypt，三层频控）、14 天滑动续期会话、个人主页（资料/入驻申请）、忘记密码找回
- 📢 **公告条**：后台发布公告（生效/失效时间窗，过期自动下架），首页顶部展示，点击看全文，关闭按公告记忆
- 🎠 **轮播图**：后台管理（样式增强：背景/文字色/渐变/图片适配）
- 🎨 **视觉体系**：深空科技蓝（CSS 变量化全站统一）、模块化色阶、移动端自适应
- ✅ **自定义前台页面**：后台「页面管理」创建新分页（page.html?key=），可配置入口位置（导航栏/更多菜单）与页头，用区块模板搭建内容
- ♻️ **回到顶部 / 图片懒加载 / 全文搜索**：全站体验细节（滚动回顶按钮、原生 lazy 加载、news/policy 标题+正文搜索）

### 🔧 后台管理（管理员视角，内容运营为核心）

- 🗂️ **文章管理 CMS**：增删改查 / **一键复制** / 批量置顶 / 批量下线 / **批量移动栏目** / 回收站还原；**预约发布与定时下线**（到点自动隐藏）；版本历史回滚；草稿自动保存；摘要/关键词 SEO
- 🧩 **区块管理（页面搭建核心）**：20 种模板（schema 驱动，非程序员可视化填内容）+ 实时预览/高亮定位 + 未保存拦截 + 放大编辑 + 预览设备档位（手机/平板/桌面）+ 列表项排序 + 一键复制跨页复用 + 历史版本回滚；编辑区可拖拽调宽（偏好记忆）
- 🗂️ **栏目管理**：树形 3 级（超深阻止）、文章数统计、搜索、同级上移下移、**前台归属页面可配置**（资讯/政策页，替代名称前缀硬编码）、发文章直达编辑页
- 📢 **公告运营**：生效/失效时间窗（过期自动下架）、后台过期标记、时间窗校验
- 🖼️ **官网轮播**：样式增强、CSS 白名单校验、实时效果预览
- 📊 **内容统计报表**：浏览量 Top20、栏目分布占比、近 30 天趋势（日粒度累计）
- ⚙️ **图片上传自动压缩**：>2MB 且宽>2000px 自动等比压缩（JPEG 0.8，白底防透明变黑）
- 🔗 **OG 分享卡片 + sitemap.xml**：文章详情动态 og 标签（微信分享预览）、匿名 sitemap 供搜索引擎收录
- 👤 **成员管理**：成员列表/状态/角色（前台注册成员管理与统计）
- 📥 **入驻申请管理**：待处理/已处理/已拒绝流转，新申请**邮件通知运营者**（`opc.apply.notify.email` 可配）
- ♻️ **回收站（两态）**：文章/成员软删除 → 可恢复/彻底删除；定时清理（服务回收站已随服务业务停用隐藏）
- 🔐 **角色权限分级**：预置双角色（超级管理员 / 内容编辑 editor，按钮级权限）
- 🛡️ **数据一致性**：删除有内容的栏目自动拦截；并发状态转换原子化（CAS）；历史版本冗余

> ⏸️ **已停用业务**：原「服务业务」模块（服务信息/报名/候补/订单/挂失补办等）已随产品方向调整**整体停用**——前台页面（services/service/contest.html）已删除，后台菜单（服务信息/服务回收站等）幂等隐藏（`upgrade_20260825_19_hide_book_menu.sql` + `upgrade_20260827_09`），代码/数据/接口保留可逆恢复。

### 🤖 自动化（Quartz 定时任务）

| 任务 | 时间 | 行为 |
|---|---|---|
| 回收站定时清理 | 每日 | 回收站超期文章自动彻底删除（`upgrade_20260826_06` 配置） |

（原服务业务「候补超时释放」任务随业务停用保留未启）

## 📚 真实内容（官网内容库）

- 全部前台内容为真实社区信息（清城区人工智能 OPC 生态社区，2026-08-01 揭牌，"国企引领、民企赋能"，星谷科技园/天安智谷产业园，咨询电话 0763-3391888），来源与口径见 [《官网内容库》](docs/官网内容库.md)

## 🛠️ 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Spring Boot · Spring Security · JWT · MyBatis · Redis · Quartz |
| 前端 | Vue 2 · Element UI · Axios · 官网前台原生静态页（HTML/CSS/JS） |
| 数据库 | MySQL 8 |
| 部署 | Maven 打包 · Nginx 静态托管 · Docker 一键编排（详见部署指南） |

## 🚀 一键启动（Docker，推荐）

```bash
git clone https://github.com/JerryVon39/OPC.git
cd OPC
scripts\start-all.bat        # Windows（Linux/macOS: ./scripts/start-all.sh）
```

脚本自动：拉起 Docker Desktop → 生成 `.env` → 本地无镜像则 `docker compose build`（首次几分钟）→ 启动 MySQL/Redis/后端/前端四容器 → 全新库自动执行**全部幂等升级脚本 + 业务数据快照**。

- 管理后台：`http://localhost/index.html`
- 官网前台：`http://localhost/home.html`
- ⚠️ **admin 初始口令**：本地与 Docker 全新部署统一为 `admin123`（`upgrade_20260828_01` 幂等统一，方便部署方使用）。**首次登录请立即修改**（登录验证码默认关闭——快照配置 `sys.account.captchaEnabled=false`，可在系统设置开启）
- 停止：`scripts\stop-all.bat`（数据保留，重跑即恢复）

> 🐳 **镜像版本**：当前 `jerryvon/opc-backend:v2.5` + `jerryvon/opc-frontend:v2.5`（已推送 Docker Hub；`scripts\publish-docker.bat` 发布新版本并同步 compose）。
> ✅ **全新部署一致性已验证（2026-08-28）**：用全新数据卷模拟完整部署，10 张核心表与本地逐项一致（cms_page 7 / cms_block 43 / cms_article 15 / sys_menu 153 / sys_role_menu 68 等），菜单图标零缺失、hero/admin 口令正常——业务数据全部经快照继承，菜单/角色由幂等升级脚本按名称定位（不依赖 menu_id，跨库安全）。

> 💡 可选配置（`.env`）：数据库口令、邮件（`MAIL_USERNAME`/`MAIL_AUTH_CODE`，QQ 邮箱 SMTP 授权码）。`TOKEN_SECRET`（JWT 密钥）留空时 `start-all`/`start-local` 脚本自动生成强随机值（后端拒绝使用仓库默认密钥启动）。生产部署务必修改默认口令。

## 🚀 快速开始（本机开发）

```bash
scripts\start-local.bat      # Windows（Linux/macOS: ./scripts/start-local.sh）
```

脚本自动：启动/复用本机 MySQL 与 Redis → 全新库自动初始化（`sql/` 全量 + 升级脚本通配执行）→ 启动后端（8080）→ 前端 dev server（8081）。

**前置环境**（`scripts\check-env.bat` 可一键自检）：JDK 17、Maven 3.8+（首次构建）、MySQL 8（root 口令与 `.env` 的 `DB_PASSWORD` 一致）、Redis 5+、Node.js **16 或 18**（Vue CLI 4/webpack4 在 Node 17+ 报 `ERR_OSSL_EVP_UNSUPPORTED`；`ruoyi-ui/.nvmrc` 已固定 16，`nvm use` 即可）。不想装环境直接用上面的 Docker 一键路径。

- 管理后台：`http://localhost:8081/index.html`（admin 口令同上说明）
- 官网前台：`http://localhost:8081/`（根路径直达首页）
- 停止：`scripts\stop-local.bat`；服务管理：`scripts\svc.bat start|stop|status|restart`
- 手动初始化（可选）：按 `docker/mysql-init.sh` 的顺序执行 `sql/ry_20260417.sql` → `quartz.sql` → `business_init.sql` → `role_init.sql` → **全部 `upgrade_*.sql`**（通配扫描，文件名序=执行序，幂等）→ `data_snapshot.sql`

> 💡 **部署链单一来源**：升级脚本统一由通配扫描执行（`docker/mysql-init.sh` / `start-local.bat` / `start-all.bat` 同源），命名 `upgrade_YYYYMMDD_NN_描述.sql`，NN 保证同日内依赖顺序；**不要在后台/脚本外手工建菜单或改配置**——人工改动无法随部署同步（详见「部署对齐」节）。

## 🔧 首次使用 / 故障排查（U-8）

| 现象 | 处理 |
|---|---|
| Docker 一键 80 端口被占（IIS/Skype 等） | `.env` 设 `FE_PORT=8080` 后重跑 `start-all`（compose 与就绪探测已联动） |
| 本地上传图片后前台不显示 | Windows 无 D: 盘时检查 `%USERPROFILE%\ruoyi\uploadPath`（start-local 已自动注入 `RUOYI_PROFILE`） |
| 数据库口令对不上 | `.env` 只改 `MYSQL_ROOT_PASSWORD` 即可——`DB_PASSWORD` 留空自动跟随（compose/备份脚本均已联动） |
| 前端 `npm run dev` 报 `ERR_OSSL_EVP_UNSUPPORTED` | Node 版本过高：用 Node 16/18（`.nvmrc`）或 `NODE_OPTIONS=--openssl-legacy-provider` |
| Maven/npm 拉依赖极慢或失败 | 海外/内网换源：Maven 改 settings.xml 镜像；前端 `docker build --build-arg NPM_REGISTRY=https://registry.npmjs.org` |
| 公网部署 | **必须启用 HTTPS**（nginx.conf 443 段取消注释/外部 TLS 终结）——否则管理员 JWT、读者会话、口令明文过网 |

## 📁 项目结构

```
├── ruoyi-admin        # 启动模块（配置、定时任务）
├── ruoyi-framework    # 框架层（安全/拦截器/切面）
├── ruoyi-system       # 业务模块（cms/cmsBlock/cmsCategory/notice/banner/reader/purchase 的 controller/service/mapper）
├── ruoyi-common       # 公共工具（含图片压缩 ImageUtils）
├── ruoyi-generator    # 代码生成器
├── ruoyi-quartz       # 定时任务框架
├── ruoyi-ui           # 前端（views/system/cms 内容运营、public/ 官网静态页）
├── scripts            # 运维脚本（svc / start-local / start-all / backup-db / export-data）
├── docs               # 文档（开发文档/部署指南/官网内容库/审查记录）
└── sql                # 数据库脚本（business_init.sql + 幂等升级链 upgrade_*.sql + 数据快照）
```

**核心代码位置**：
- CMS 文章/区块/栏目：`ruoyi-system/.../controller/CmsArticleController.java`、`CmsBlockController.java`、`CmsCategoryController.java`
- 前台渲染器（页面模块/导航/公告/成员登录）：`ruoyi-ui/public/js/site.js`
- 前台静态页：`ruoyi-ui/public/*.html`
- 区块模板注册表（schema 驱动）：`ruoyi-ui/src/views/system/cms/blockTemplates.js`

## 🛡️ 并发与数据一致性（持续加固）

- 状态流转全部**原子 CAS**（`WHERE ... AND status = 期望值`），并发操作不产生脏状态
- 删除有内容/子项的栏目自动拦截；回收站两态（软删 + 恢复/彻底删除）
- 前台浏览量防刷（同 IP + 文章 10 分钟窗口）+ 日粒度报表数据（`article_views_daily`）

## 🧪 自动化测试

| 层级 | 怎么跑 | 覆盖 |
|---|---|---|
| 单元测试 | `mvn test`（秒级，不依赖数据库） | 申请校验、CAS 并发、编号生成、统计缓存、借阅规则等 114 用例 |

## 🛡️ 部署与运维

- [《部署指南》](docs/部署指南.md)：本机启动 / 正式部署 / Docker / 初始化 / **升级回滚**（含 docker 旧卷升级陷阱说明）
- [《数据库备份脚本》](scripts/backup-db.bat)：一键 mysqldump 备份
- [《开发文档》](docs/开发文档.md)：技术指南 + 红线清单/命名映射/自检（第十五节）
- [《开发文档》](docs/开发文档.md)：面向接手开发者的完整指南
- [《审查记录》](docs/审查记录.md)：历史审查问题与修复状态

## 💾 数据库数据自动同步 GitHub（git 钩子）

**机制**：`.githooks/pre-commit` 钩子在每次 `git commit` 前自动导出业务数据为 `sql/data_snapshot.sql` 并加入提交：

- **12 张表**：服务/报名/候补/文章/**区块内容（cms_block）**/轮播/公告/栏目/字典 + **站点配置（sys_config）**
- **PII 排除**：成员（reader 手机/邮箱）、入驻申请（申请人信息）不进快照；`sys_config` 排除测试发信邮箱键（`opc.apply.notify.email`/`site_email`）
- 使用：`git config core.hooksPath .githooks`（仓库内执行一次）→ 日常照常 commit/push
- Docker 全新部署自动导入快照 → 部署方拉起即含最新业务数据

**部署对齐原则**（2026-08-27 确立）：
- 业务数据（区块/配置/文章等）→ 走**快照**
- 菜单/权限 → 一律**固化为幂等升级脚本**（人工在后台改菜单无法同步，必须补脚本；`upgrade_20260827_04` 即固化实例）
- 已踩坑记录：`sys_menu` 不能进快照（menu_id 跨库漂移导致 REPLACE 重复行）；升级脚本注释勿用 `--（` 等无空格注释（SQL 语法错误）

## 📜 声明

本项目基于开源框架 **RuoYi-Vue**（[gitee.com/y_project/RuoYi-Vue](https://gitee.com/y_project/RuoYi-Vue)）二次开发，遵循 MIT 开源协议。**私有项目，仅限内部使用，请勿公开分发。**

---

*数智游民创新工场 · 官网系统 · 2026*
