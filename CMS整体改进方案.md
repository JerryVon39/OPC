# 数智游民创新工场 · CMS 整体改进方案（含全部决策）

> 制定日期：2026-08-25（首版）；v2 代码审查修订；v3 对照 `CMS改进2.md` 查漏补缺；**v4（2026-08-25）：用户拍板"首页数据条不做"——D5 定案为不做；D17 关联模块降级为候选待确认；新增后续批次完善设计（第十一节）**
> 输入材料：`CMS改进.md` + `CMS改进2.md`（两份功能分析，**建议≠最终决定，以用户拍板为准**）+ **代码审查**（全量核实）
> 说明：本方案已包含全部推荐决策（【决策】条目），无需再拍板；如需调整可在评审时提出异议。
> 目标用户：老板 / 运营（非程序员）——"后台能自己维护内容与部分前台页面，且不容易改坏"

---

## 一、目标

1. **内容管理对标经典 CMS 模式**：栏目树（左）+ 内容列表（右），栏目可自助增改（当前栏目只能靠 SQL 建）
2. **部分前台页面后台可改**：首页关键区块 + 4 个栏目页（走进社区/入驻招商/人才培养/产业生态）的文本槽，后台表单编辑，**改坏可一键回滚**
3. **全后台非程序员友好**：业务化菜单 + 运营工作台 + 防呆设计 + 使用帮助
4. **前台永不因后台改动而白屏**：静态兜底 + 渐进增强渲染

---

## 二、现状盘点（代码核实）

| 能力 | 现状 | 缺口 |
|---|---|---|
| 文章管理 | ✅ 列表/增删改/置顶/发布/下线（`views/system/cms/index.vue`） | 无封面预览、无批量操作、无排序、物理删除 |
| 栏目管理 | ⚠️ 后端 CRUD 齐备 + 文章删除守卫（`CmsCategoryServiceImpl`），前端 API 客户端已存在（`api/system/cms.js`），**无页面/无菜单** | 栏目只能 SQL 维护；**缺子栏目删除守卫** |
| 分类树联动 | ❌ 平铺列表 + 下拉筛选 | 缺左树右表布局 |
| 前台文章渲染 | ⚠️ 编辑器是 Quill（HTML 入库），`article.html` 用 `textContent` 渲染 | **Quill 富文本前台不生效**——非程序员写作体验断点 |
| 政策文件 | ✅ 7 篇政策已入 CMS | 无原文 PDF 附件 |
| 服务/大赛/报名 | ✅ `book` 体系（分类字典/上下架/回收站/报名） | 不并入 CMS（决策 D2） |
| 前台静态页 | ✅ about/join/talent/industry 结构化卡片布局（7/4/5/5 个 section） | 正文不可后台改；**整页富文本会毁掉卡片布局→只能做文本槽** |
| 首页 | ✅ 轮播已 banner 驱动（`home.html` 调 `/system/banner/publicList` + DEFAULT 兜底）、新闻已 CMS 驱动、精选服务已 book 驱动 | 剩余静态块（intro/品牌理念/三大赋能/产业生态/CTA）不可后台改 |
| 站点配置 | ✅ **`SysConfigController` 已有 `@Anonymous /configKey/{key}`** | 只差插 `sys_config` 键 + 前台 fetch，零后端 |
| 回收站 | ✅ `book` 两态模式（`del_flag='2'` + deleted_by/time，restore/purge + 回收站页 `recycle/book.vue`） | CMS 文章未接入 |
| 统计数据 | ✅ `StatisticsService`（Redis 5 分钟缓存 + 写路径主动失效；book/reader/borrow/order 四维） | 工作台需补文章维度 |
| 上传 | ✅ `MimeTypeUtils` 含 pdf、nginx `client_max_body_size 20m`、`/common/upload` 就绪 | — |
| 前台公共脚本 | ✅ `js/site.js`（`esc()`/`apiFetch`/登录态渲染） | ⚠️ **12 页各自内联复制登录/注册/找回密码弹窗 HTML**——改登录要改 12 个文件 |
| 后台菜单 | ✅ 已业务化重组（内容运营/成员与报名/运营辅助/系统设置） | 无工作台/帮助；运营角色仍见系统设置 |

---

## 三、总体设计

### 3.1 内容分层

```
模板骨架（导航/页头/页脚/布局）   → 静态 HTML，SEO 保留，非程序员不可编辑
列表内容（新闻/政策/服务）        → cms_article（增强）+ book（不动）
页面文本槽 / 首页区块            → 新表 cms_block（唯一内容模型）+ 历史版本
站点配置（联系方式等）           → 复用 sys_config + 已有匿名 configKey 接口
```

### 3.2 渲染铁律：渐进增强 + 静态兜底

- 前台页面 HTML 内**保留现有内容作为默认值**（SEO 与降级双保证；与 home 轮播 `DEFAULT_BANNERS` 同一模式）
- 页面加载时 fetch 公开接口，成功则替换文本槽内容，失败/超时/未配置则保留静态内容
- 前台**永远不白屏**；后台改坏的内容只影响"动态覆盖"，不影响页面骨架

### 3.3 技术决策（推荐即定）

| # | 决策点 | 结论 | 理由 |
|---|---|---|---|
| D1 | 多模型实现方式 | **文章单表不动；统一用 cms_block 承载"页面可编辑内容"**（不做整页富文本模型） | 见 D4 |
| D2 | 服务/大赛/招聘 | **继续用 book 体系**，不并入 CMS | 已有分类字典/上下架/回收站/报名流；招聘复用 book 报名流 |
| D3 | 政策文件 | **文章 + `attachment` 附件列**（PDF 上传），不建独立模型 | 正文文本块已承载要点，缺的只是"原文下载" |
| D4 | 前台页面可改形态 | **取消 cms_page 整页富文本；改为 cms_block 文本槽**：4 个栏目页各开放 2–3 个文本槽（hero 副标语/CTA/联系区），首页开放 8–10 个区块（intro/品牌理念/三大赋能/产业生态/CTA） | 代码审查证据：about 7 个 section、join 4、talent 5、industry 5 全是 info-card 栅格/时间线结构化布局；整页富文本会拍平布局，非程序员被迫重造卡片，更易改坏。cms_page 记为远期（未来新建自由排版页再用） |
| D5 | 首页数据条 | **不做（用户拍板，2026-08-25 定案）**。v2 因"现首页无数据条"判定不做 → v3 依 CMS改进2 §3.1 反转为 P0 → v4 用户明确否决。**最终：不做**。hero 保持"轮播（banner 驱动）+ intro 文字块"现状，intro 作为 1 个区块 | 用户决策优先于需求文档建议；`site_stat_*` 覆盖键与统计扩展一并取消 |
| D6 | 联系方式 | 进"站点配置"（`sys_config` 键 + **复用现有 `@Anonymous /configKey/{key}`**），前台 footer/联系区优先取配置，未配置走静态默认 | 零后端工作；电话 0763-3391888 现写死在 12 页 footer 中 |
| D7 | 运营角色 | **新建 role_key=`operator`（运营专员）**，边界对齐 CMS改进2 §6.3：operator = 工作台 + 成员与报名 + 运营辅助（**不含内容运营**）；editor（内容编辑）= 工作台 + 内容运营 + 运营辅助；两者均**不授系统设置** | 职责分明：内容编辑管内容、运营专员管报名/入驻审批，互不越界；doc 的三角色表与本决策一致 |
| D8 | 文章回收站 | **做**（对齐 book：`del_flag='2'` + deleted_by/time + restore/purge + `recycle/cms.vue`） | 误删可恢复，非程序员兜底刚需 |
| D9 | 历史版本 | **cms_block 每次保存自动存一版，上限 20 版/区块**，可查看/回滚 | 月度维护量小，20 版足够且存储可控；回滚是非程序员"改坏自救"的卖点 |
| D10 | 前台导航动态化 | **不做**（远期记录） | 导航横跨 12 静态页 + 汉堡菜单，改造风险高收益低 |
| D11 | 不做清单 | 运维类（在线升级/缓存/漏洞扫描/日志）、多语言、订单电商、转移/同步 | 与 CMS改进.md 判断一致 |
| D12 | 浏览量防刷 | Redis 按会话去重后 +1 | 防刷流量造假 |
| D13 | 定时发布 | publish_time 未来时间即预约，前台列表加 `publish_time <= NOW()` | 一个条件实现预约发布 |
| D14 | 栏目删除守卫 | **现有"有文章拒绝删除"守卫之外，补"有子栏目拒绝删除"** | 代码审查发现：删父栏目会留孤儿子栏目 |
| D15 | 前台弹窗收敛 | **把 12 页内联复制的登录/注册/找回密码弹窗 HTML 收敛进 `site.js` 动态注入**（第四批） | 审查发现：改登录流程要改 12 个文件，是非程序员不可维护的真实根源；`site.js` 已有公共底座 |
| D16 | 清理批次（对齐 CMS改进2 第一阶段，代码已核实） | ① 删除挂失补办（`ReaderController:566` 起整段 + 前台入口，图书遗留功能）；② 移除 book/reader 页 Excel 导入导出按钮（保留代码）；③ **保留 git hooks 数据快照**（不采纳 doc 的移除建议——快照是无成本备份，commit 自动导出，删后仅剩手动脚本）；④ 公告定时任务无需处理（doc 所述 3 个公告任务现状已不存在，仅剩业务必需的 `BorrowTask`） | 按 doc 意图精简运维面；git hooks 保留理由单独标注 |
| D17 | 关联新业务模块 | **降级为候选清单（待用户确认后才排期）**：算力资源预约（3–4 人日）、订单发布与投标（3–4 人日）、政策智能匹配（2 人日）、讨论区/工具箱/在线支付（按需）。**不纳入 CMS 批次，不默认立项**——CMS改进2 的新增建议与"数据条"同源，采纳度以用户确认为准 | 超出 CMS 范畴且用户未表态；保持候选状态，避免文档默认承诺 |
| D18 | 操作路径最短化（CMS改进2 §4.3） | 分类树节点上直接"发文章"：栏目管理页每个节点加发文章按钮 → 跳文章页并预选该栏目（query 传参） | 对齐"分类即操作入口"模式，成本约 0.2 人日 |

---

## 四、模块方案

### 🔴 P0-1 栏目管理（分类树）——对标报告 §7 核心模式

- 新增 `views/system/cms/category.vue`：`el-tree` 栏目树（新增主分类/子分类、改名、排序、停用/启用）；后端接口与前端 API 客户端**全部现成**
- 重构 `cms/index.vue` 为**左树右表**：点击栏目联动过滤（替换下拉筛选），树顶部"全部文章"节点
- 后端补**子栏目删除守卫**（D14）：`deleteCmsCategoryByCategoryIds` 先查子栏目，存在则拒绝
- 分类深度限制 ≤3 级；新增菜单"栏目管理" + `system:cmsCategory:*` 权限点 + editor/operator 角色关联

### 🔴 P0-2 文章管理增强

- 封面图缩略预览列
- 批量置顶/取消置顶、批量上架/下线（后端补 2 个批量接口）
- `cms_article` 加 `sort` 列：列表可输入序号，前台排序 = 置顶 → sort → 发布时间
- `cms_article` 加 `keywords`/`description`（SEO）与 `attachment`（政策 PDF）——**一次加列到位**
- 编辑弹窗：附件上传（政策原文 PDF）、正文 Quill 保持不变

### 🔴 P0-3 文章软删除回收站（对齐 book 两态）

- `cms_article` 加 `del_flag`/`deleted_time`/`deleted_by` 三列
- 删除 → 软删（`del_flag='2'`）；新增 `views/system/recycle/cms.vue`（还原/彻底删除，复用 `recycle/book.vue` 模式）
- 后台列表与前台公开列表查询自动过滤；回收站菜单挂"运营辅助"

### 🔴 P0-4 前台文章 HTML 渲染修复（非程序员体验的关键一步）

- `article.html` 正文从 `textContent` 改为**白名单 HTML 渲染**：允许 p/br/strong/em/h1-h4/ul/ol/li/a/img/blockquote/pre/code/span(color)/table，其余标签过滤（防 XSS；Quill 输出相对干净，前端白名单正则即可）
- 链接自动 `<a>`（保留现有逻辑）；图片加最大宽度样式；存量纯文本文章不受影响（无标签文本照常渲染）
- 验收：后台写"加粗 + 插图 + 列表"的文章，前台所见即所得

### 🔴 P0-5 政策原文 PDF 附件

- 政策类文章编辑弹窗上传 PDF（`/common/upload` 已支持，nginx 20m 已确认）
- 前台 `article.html`：有附件时显示"📄 下载政策原文"按钮
- 对应原 TODO #10

### 🟡 P1-1 区块管理（cms_block）——"后台改前台页面"核心（取代原 cms_page）

- 新表 `cms_block`（block_id/block_key/page_key/title/subtitle/content/image/link/sort/visible/version/updated_by/update_time）+ `cms_block_history`（每区块 20 版）
- 后台"区块管理"页：按页面分组，每块一个卡片表单（标题/副标题/正文/图片/链接/排序/显示开关）；**隐藏用开关不用删除**；历史版本 Tab（查看/一键回滚）
- 公开接口 `@Anonymous`：`/system/cmsBlock/publicList?pageKey=home`（仅 visible=0 区块）
- 前台接入（均带静态兜底）：
  - `home.html`：`home-intro`（标题/副标语/简介/双 CTA）、`home-concept`（品牌理念段落）、`home-feature-1/2/3`（三大赋能卡片）、`home-ecosystem`（产业生态入口）、`home-cta`（入驻 CTA）——**无数据条**（D5 用户拍板不做）
  - 4 个栏目页文本槽：`about-hero-sub` / `about-cta`、`join-hero-sub` / `join-cta`、`talent-hero-sub` / `talent-cta`、`industry-hero-sub` / `industry-cta`（hero 副标语 + 结尾 CTA 文案）
  - 联系区统一走站点配置（P1-3），不做区块
- 渲染 JS 放 `site.js`（或 `site-content.js`）：`window.CMS_BLOCKS` 钩子，页面声明槽位 id → 区块 key 映射，fetch 成功后替换 `textContent`/`innerHTML`（白名单），失败走静态兜底

### 🟡 P1-2 站点配置（原报告 P1"设置菜单"落地，零后端）

- 插入 `sys_config` 键（幂等）：`site_phone`(0763-3391888) / `site_email` / `site_address` / `site_wechat` / `site_qrcode`
- **复用现有 `@Anonymous /system/config/configKey/{key}`**——前台 footer 联系区 fetch 后替换，未配置走静态默认
- 后台用 RuoYi 原生"参数设置"页即可（键名加 `site_` 前缀 + 说明标注"前台可见"），无需新页面

### 🟡 P1-3 运营工作台 + 角色收敛 + 帮助

- 新增"运营工作台"落地页（operator/editor/admin 默认路由）：
  - 快捷入口大按钮：发文章 / 管栏目 / 改区块 / 传轮播 / 管服务 / 看报名
  - 数据卡：服务/成员/今日报名/今日订单（复用 `StatisticsService.dashboard()`）+ **补文章维度**（文章总数/今日发文/草稿数/回收站数）
  - 最近编辑记录（文章/区块各 5 条，点击直达编辑）
  - 操作指引卡（"三步发一篇新闻"）
- 角色边界（D7）：新建 `operator` = 工作台 + 成员与报名 + 运营辅助；`editor` = 工作台 + 内容运营 + 运营辅助；均**不授系统设置**
- 每个管理页顶部一行灰色使用说明 + 空状态引导
- 侧边栏"使用帮助"页（图文操作手册：发文章/改页面/传轮播/看报名/回滚）

### 🟡 P1-4 操作路径最短化（CMS改进2 §4.3"分类即操作入口"）

- 栏目管理页每个栏目节点加"发文章"快捷按钮 → 跳转文章管理页并**预选该栏目**（`/content/article?categoryId=X`，index.vue `handleAdd` 读取 query 预填）
- 文章编辑弹窗标题输入加 `maxlength=200`（对齐 DB varchar(200) 约束，CMS改进2 数据质量建议）
- 作者字段**保留自由文本**（不采纳"从用户下拉选择"——本站作者多为机构名如"数智游民创新工场/北江人工智能产教融合研究院"，下拉反增负担）

### 🟢 P2 运营增强（最后一批）

- 浏览量防刷（D12）：`publicDetail` 前查 Redis 会话标记，未标记才 +1
- 定时发布（D13）：前台 `selectPublicArticleList` 加 `publish_time <= NOW()`；后台发布弹窗可选发布时间（默认立即）
- SEO：文章 keywords/description（P0-2 已加列）→ 前台 `article.html` 动态更新 `<meta>`
- **前台弹窗收敛（D15）**：12 页内联的登录/注册/找回密码弹窗 HTML 移入 `site.js` 动态注入，各页删除内联副本（一次性大清理，需逐页回归）

---

## 五、数据模型变更汇总

```sql
-- 新增
cms_block          block_id / block_key / page_key / title / subtitle / content /
                   image / link / sort / visible(0显示 1隐藏) / version /
                   updated_by / update_time
cms_block_history  history_id / block_id / version / title / subtitle / content /
                   image / link / updated_by / update_time   -- 每区块 20 版

-- cms_article 加列（一次性）
sort int DEFAULT 0
attachment varchar(255)      -- 政策原文 PDF 等附件
keywords varchar(255)        -- SEO
description varchar(500)     -- SEO
del_flag char(1) DEFAULT '0' -- 回收站（对齐 book：'2'=已删）
deleted_time datetime / deleted_by varchar(64)

-- sys_config 预置键（幂等 INSERT）
site_phone / site_email / site_address / site_wechat / site_qrcode

-- 栏目种子：4 栏目页文本槽 + 首页区块的初始记录（与前台静态内容一致，兜底双保险）
```

升级脚本沿用 `upgrade_YYYYMMDD_*.sql` 幂等格式（CREATE IF NOT EXISTS / INSERT...SELECT WHERE NOT EXISTS / information_schema 补列）。

---

## 六、后台菜单与权限（终态）

```
├── 运营工作台（新，C 页，默认路由）
├── 内容运营
│   ├── 服务信息          （现有 book）
│   ├── 官网轮播          （现有 banner）
│   ├── 栏目管理（新）      system:cmsCategory:*
│   ├── 文章管理          system:cms:*（增强）
│   ├── 区块管理（新）      system:cmsBlock:*
│   └── 通知公告          （现有）
├── 成员与报名            （现有，不动）
├── 运营辅助
│   ├── 回收站            （现有 book/reader + 新增 cms）
│   └── 站点配置（说明标注）  system:config:list（RuoYi 原生）
└── 系统设置              （仅 admin；operator 不可见）
```

角色分配：`admin` 全量；`operator`（新）= 工作台 + 成员与报名 + 运营辅助；`editor` = 工作台 + 内容运营 + 运营辅助（D7，职责分明：内容 vs 运营审批）。

---

## 七、前台页面改造清单

| 页面 | 改造 | 兜底 |
|---|---|---|
| news.html | 不动（已数据驱动） | — |
| policy.html | 不动；政策详情走 article.html（新增附件按钮） | — |
| article.html | 正文白名单 HTML 渲染 + 附件下载按钮 + SEO meta 动态化 | 现有提示 |
| services.html | 不动 | — |
| about / join / talent / industry | 各 2 个文本槽（hero 副标语 + CTA）接 cms_block | 保留静态文本 |
| home.html | 6 类区块接 cms_block（intro/理念/赋能×3/生态/CTA）；轮播/新闻/服务已动态 | 保留静态区块（DEFAULT_BANNERS 同款模式） |
| 全部 12 页 | footer 联系区接站点配置（P1-2）；弹窗收敛进 site.js（P2） | 未配置走静态 |
| profile / service / contest | 不动 | — |

---

## 八、实施计划（4 批，每批独立可验收）

| 批次 | 内容 | 工作量 | 验收标准 |
|---|---|---|---|
| **第一批 · 文章管理闭环** ✅ 已交付 | P0-1 栏目树（含子栏目守卫）+ P0-2 文章增强（含加列）+ P0-3 回收站 + P0-4 HTML 渲染 + P0-5 PDF 附件 | ~4 人日（已完成） | 后台可建栏目树、文章封面/批量/排序、删除进回收站可恢复；前台富文本所见即所得；政策可传 PDF 并可下载 |
| **第二批 · 区块化前台** | P1-1 cms_block + 历史回滚 + 区块管理页 + home 6 类区块（无数据条，D5）+ 4 栏目页文本槽 + **P1-4 树上发文章快捷入口** | ~3.5 人日 | 后台改"三大赋能"文案，前台刷新即变；改坏可回滚；接口失败前台显示静态内容；栏目页一键发文章 |
| **第三批 · 站点配置+工作台+角色** | P1-2 sys_config 键 + footer 接入（零后端）+ P1-3 工作台/角色收敛（D7 边界）/帮助 | ~3 人日 | 改电话前台即时生效；operator 只见 工作台+成员报名+运营辅助；editor 只见 工作台+内容运营+运营辅助；三步发文章流程 |
| **第四批 · 增强与收敛** | P2 防刷/定时发布/SEO + 前台弹窗收敛（D15） | ~2 人日 | 浏览量防刷生效；预约发布生效；登录弹窗全站单源 |
| **清理批次（可选）** | D16：删除挂失补办、移除 Excel 导入导出按钮（保留代码）、git hooks 保留 | ~1 人日 | 后台无挂失/导入残留入口；快照备份机制不变 |

合计约 **13.5 人日**（含可选清理批次 1 人日）。每批完成自动 commit（push 需另行确认）；SQL 升级脚本随批交付。

**候选新业务模块（D17，待用户确认后才排期，不默认立项）**：算力资源预约（3–4 人日）、订单发布与投标（3–4 人日）、政策智能匹配（2 人日）、讨论区/工具箱/在线支付（按需）。

---

## 九、风险与兜底

1. **前台白屏风险**：所有动态渲染均"静态兜底 + 超时回退"；区块加载失败仅跳过该区块（与 home 轮播 DEFAULT_BANNERS 同一已验证模式）
2. **XSS 风险**：内容仅管理员可写（登录 + 权限点），仍按白名单过滤渲染（P0-4/P1-1）
3. **误操作风险**：删除走回收站、区块历史 20 版可回滚、区块只隐藏不删除、系统设置对 operator 不可见
4. **改坏页面风险**：非程序员只改文本槽字段（表单化），改不了布局/样式/导航；卡片栅格等结构化布局在 HTML 骨架层不可达
5. **SEO 风险**：骨架保留静态默认内容，CMS 覆盖是增强不是替换；新 SEO 字段只增不减
6. **数据迁移风险**：全部走幂等升级脚本，可重复执行；存量数据零迁移（仅加列）
7. **弹窗收敛回归风险**（第四批）：12 页逐一回归登录/注册/找回密码全流程后再提交

---

## 十、附录：与 CMS改进2.md 差异对照表（逐条审阅结论）

| CMS改进2 条目 | 类别 | 结论 | 去向 / 理由 |
|---|---|---|---|
| 挂失补办删除 | 删减 | ✅ 采纳 | D16 清理批次（`ReaderController:566` 已核实存在，图书遗留功能） |
| 回收站三态→两态 | 删减 | ✅ 已落地 | 代码已是两态（`upgrade_20260824_03_two_state.sql`），本方案 D8 沿用 |
| 公告定时任务删除 | 删减 | ⚠️ 无需处理 | 已核实 Quartz 仅存业务必需的 `BorrowTask`，doc 所述 3 个公告任务已不存在 |
| 数据快照 git hooks 移除 | 删减 | ❌ 不采纳 | D16 ③：快照是无成本备份且 commit 自动触发；删除后备份依赖手动脚本，风险大于收益 |
| Excel 导入导出后置 | 删减 | ✅ 采纳（部分） | D16 ②：移除按钮保留代码（doc 同建议）；菜单已隐藏系统工具，无二期待办 |
| 角色精简为 2 个 | 删减 | ⚠️ 调整为 3 个 | D7：doc 自身 §6.3 即三角色（管理员/内容编辑/运营专员），按后者细化边界 |
| 首页运营统计数字 | 新增 | ❌ 不采纳（**用户拍板 2026-08-25**） | D5 定案不做：hero 保持轮播+intro 现状；需求文档建议不等于最终决定 |
| 算力资源预约 | 新增 | ✅ 采纳（P0，**独立立项**） | D17：超出 CMS 范畴，单独立项 3–4 人日 |
| 订单发布与投标 | 新增 | ✅ 采纳（P0，**独立立项**） | D17：超出 CMS 范畴，单独立项 3–4 人日 |
| 政策智能匹配 | 新增 | ✅ 采纳（P1，独立立项） | D17：可后续基于政策文章 ext 字段扩展 |
| 线上讨论区 / 工具箱 / 在线支付 | 新增 | 🟢 按需 | D17：社区成熟后评估 |
| 分类树状管理 | 借鉴 | ✅ 已落地 | 第一批栏目树页（含子栏目守卫） |
| 内容类型区分（频道/列表/链接） | 借鉴 | ⚠️ 部分采纳 | 频道/列表由栏目树 + 文章/区块模型覆盖；**跳转链接**维持 D10（导航动态化不做，低成本替代：站点配置存外链供 footer/联系区渲染，随 P1-2 评估） |
| 结构化编辑（标题/副标题/简介/正文/封面） | 借鉴 | ✅ 已有 | 文章字段：title/summary/cover/content + 附件 + SEO；副标题未采纳（新闻列表不展示，Quill 正文可承载） |
| 富文本编辑器 | 借鉴 | ✅ 已落地 | Quill + 第一批白名单 HTML 渲染修复 |
| 作者从用户中选择 | 借鉴 | ❌ 不采纳 | P1-4：本站作者多为机构名，自由文本更合适 |
| 标题长度 ≤200 | 借鉴 | ✅ 采纳 | P1-4：前端 maxlength=200（DB 已约束） |
| 分类树上直接操作（最短路径） | 借鉴 | ✅ 采纳 | D18/P1-4：栏目节点"发文章"快捷入口 |
| 全选/批量排序 | 借鉴 | ✅ 已落地 | 第一批批量置顶/下线 + 行内排序 |

---

## 十一、后续批次完善设计（第二批起，可执行粒度）

### 第二批 · 区块化前台（~3.5 人日）

**数据模型**（详细）：
```sql
cms_block          block_id(自增PK) / block_key varchar(50) / page_key varchar(30) /
                   title varchar(200) / subtitle varchar(200) / content mediumtext(HTML) /
                   image varchar(255) / link varchar(255) / sort int DEFAULT 0 /
                   visible char(1) DEFAULT '0' / version int DEFAULT 1 /
                   updated_by varchar(64) / update_time datetime
                   -- 唯一键 uk(block_key)；page_key 用于后台分组展示
cms_block_history  history_id(PK) / block_id / version / title / subtitle / content /
                   image / link / updated_by / update_time   -- 每区块最多 20 版（超限删最旧）
```

**区块 key 全集**（与前台静态内容同文，兜底双保险）：
- home：`home-intro`（标题/副标语/简介/双 CTA）、`home-concept`（品牌理念段落）、`home-feature-1`/`home-feature-2`/`home-feature-3`（三大赋能）、`home-ecosystem`（产业生态标题+入口）、`home-cta`（入驻 CTA）
- 栏目页文本槽（各 2 个）：`about-hero-sub`/`about-cta`、`join-hero-sub`/`join-cta`、`talent-hero-sub`/`talent-cta`、`industry-hero-sub`/`industry-cta`
- 后台"区块管理"页按 `page_key` 分组展示，仅列表这 13 个 key（不开放新建 key——防非程序员造出前台不认识的区块）

**接口**（沿用 controller/service/mapper 模式）：
- 公开：`GET /system/cmsBlock/publicList?pageKey=home`（@Anonymous，仅 visible='0'，按 sort）
- 后台：`list`/`{id}`/`add`/`edit`/`delete`（权限 `system:cmsBlock:*`）+ `GET /history/{blockId}`（含版本列表）+ `PUT /rollback/{blockId}/{version}`（回滚=取该版写入主表并 version+1 记新历史）
- 保存时自动写历史（service 层：先插 history，再更新主表 version+1）

**前台渲染机制**（`site.js` 新增，12 页共用）：
- `window.CMS_BLOCK_SLOTS = { 'home-intro': { el: '#homeIntro', mode: 'html'|'text' }, ... }` 每页声明槽位映射
- `loadCmsBlocks(pageKey)`：fetch 成功 → 逐槽替换（text 用 textContent / html 用白名单 innerHTML，复用第一批 sanitizer 逻辑抽出为 `site.js` 公共函数）；失败/超时(3s)/空 → 保留静态内容
- home 与 4 栏目页各加一段 `loadCmsBlocks('home'|'about'|...)` 调用

**验收清单**：后台改"三大赋能"卡片文案 → 前台刷新即变；改坏 → 区块管理历史 Tab 一键回滚；断网 → 前台显示静态原文；栏目管理页节点"发文章" → 文章页预选栏目；标题输入框 maxlength=200。

### 第三批 · 站点配置 + 工作台 + 角色（~3 人日）

- `sys_config` 预置键（幂等 INSERT）：`site_phone`(0763-3391888)/`site_email`/`site_address`/`site_wechat`/`site_qrcode`
- 前台接入：12 页 footer 联系区 + home `home-contact` 区，fetch `/prod-api/system/config/configKey/site_phone` 等，未配置走静态（**零后端**，RuoYi 原生匿名端点已确认）
- 运营工作台（`views/system/ops/index.vue`，菜单"运营工作台"置于顶层 order_num=0）：
  - 快捷入口：发文章/管栏目/改区块/传轮播/管服务/看报名（按角色显隐）
  - 数据卡：复用 `StatisticsService.dashboard()`（服务/成员/今日报名/今日订单）+ 新增 `articleTotal`/`articleToday`/`draftCount`/`recycleCount`（dashboard() 补 4 个 count）
  - 最近编辑：文章/区块各 5 条（按 update_time 倒序）
  - 指引卡："三步发一篇新闻"静态文案
- 角色收敛（D7）：`operator` 新建并挂 工作台+成员与报名+运营辅助；`editor` 补挂 工作台；菜单可见性按角色（admin 全量，系统设置仅 admin）
- 帮助页：`views/system/help/index.vue` 静态图文（发文章/改区块/传轮播/审批/回滚五篇），菜单"使用帮助"挂运营辅助

### 第四批 · 增强与收敛（~2 人日）

- 浏览量防刷：`selectPublicArticleDetail` 前查 Redis `cms:view:{ip}:{articleId}`（TTL 10 分钟），未命中才 `increaseArticleViews`；Redis 不可用静默降级为自增
- 定时发布：前台 `selectPublicArticleList` 加 `and a.publish_time <= NOW()`；后台编辑弹窗发布时可选发布时间（datetime-picker，默认立即=null）；`publishArticle`/`changeArticleStatus` 置已发布时保留原 publish_time
- SEO：`article.html` 详情加载后动态写 `<meta name="keywords">` / `<meta name="description">`（取 `a.keywords`/`a.description`，空则用默认）
- 弹窗收敛（D15）：登录/注册/找回密码弹窗 HTML 从 12 页内联移入 `site.js`（`injectAuthModals()` 动态创建 DOM），12 页逐一回归全流程后提交

### 清理批次（可选，~1 人日，D16）

- 删除挂失补办：`ReaderController` 挂失/补办两端点 + `ReaderServiceImpl` 相关方法 + 前台入口 + `BizStatus` 相关常量（0.5 人日）
- 移除 Excel 导入导出：book/reader 页顶栏导入按钮与 `api/system/book.js` 的 `importData` 调用（保留后端接口与代码注释）（0.3 人日）
- git hooks 数据快照：**保留**（不采纳移除）
- 验收：后台无挂失/导入残留入口；挂失接口 404；快照机制不变
