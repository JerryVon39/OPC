# 数智游民创新工场 · CMS 整体改进方案（含全部决策）

> 制定日期：2026-08-25
> 输入材料：`CMS改进.md`（对标"智慧融合管理平台"功能分析）+ 现有代码盘点（CMS 文章/栏目、book 服务、前台静态页）
> 说明：本方案已包含全部推荐决策（各节【决策】条目），无需再拍板；如需调整可在评审时提出异议。
> 目标用户：老板 / 运营（非程序员）——"后台能自己维护内容与部分前台页面，且不容易改坏"

---

## 一、目标

1. **内容管理对标经典 CMS 模式**：栏目树（左）+ 内容列表（右），栏目可自助增改（当前栏目只能靠 SQL 建）
2. **部分前台页面后台可改**：走进社区/入驻招商/人才培养/产业生态 4 个静态页的正文区 + 首页关键区块，后台富文本编辑，**改坏可一键回滚**
3. **全后台非程序员友好**：业务化菜单 + 运营工作台 + 防呆设计 + 使用帮助
4. **前台永不因后台改动而白屏**：静态兜底 + 渐进增强渲染

---

## 二、现状盘点（已具备 vs 缺口）

| 能力 | 现状 | 缺口 |
|---|---|---|
| 文章管理 | ✅ 列表/增删改/置顶/发布/下线（`views/system/cms/index.vue`） | 无封面预览、无批量操作、无排序 |
| 栏目管理 | ⚠️ 后端 CRUD 齐备（`CmsCategoryController`+删除守卫），**前端无页面** | 栏目只能 SQL 维护 |
| 分类树联动 | ❌ 平铺列表 + 下拉筛选 | 缺左树右表布局 |
| 前台文章渲染 | ⚠️ `article.html` 用 `textContent` 纯文本渲染 | **Quill 富文本（加粗/图片/列表）前台不生效**——非程序员写作体验断点 |
| 政策文件 | ✅ 7 篇政策已入 CMS（`upgrade_20260826_policy.sql`） | 正文是"【字段】文本"块；无原文 PDF 附件 |
| 服务/大赛/报名 | ✅ 走 `book` 体系（分类/上下架/回收站/报名） | 不并入 CMS（见决策 D2） |
| 单网页 | ❌ 4 个静态页（about/join/talent/industry）纯 HTML | 后台不可改 |
| 首页 | ❌ `home.html` 纯静态 | 关键区块后台不可改 |
| 站点信息（电话/邮箱/地址） | ❌ 写死在 HTML | 改联系方式要动代码 |
| 回收站 | ⚠️ `book`/`reader` 已有两态软删先例 | CMS 文章是物理删除 |
| 浏览量 | ✅ 自增 | 无防刷、无定时发布、无 SEO 元数据 |
| 后台菜单 | ✅ 已业务化重组（内容运营/成员与报名/运营辅助/系统设置） | 运营角色仍可看到系统设置；无工作台/帮助 |

---

## 三、总体设计

### 3.1 内容分层

```
模板骨架（导航/页头/页脚/布局）   → 静态 HTML，SEO 保留，非程序员不可编辑
列表内容（新闻/政策/服务）        → cms_article（增强）+ book（不动）
页面内容（单网页）               → 新表 cms_page + 历史版本
首页关键区块                     → 新表 cms_block
站点配置（联系方式等）           → 复用 RuoYi sys_config + 匿名白名单接口
```

### 3.2 渲染铁律：渐进增强 + 静态兜底

- 前台页面 HTML 内**保留现有内容作为默认值**（SEO 与降级双保证）
- 页面加载时 fetch 公开接口，成功则替换内容区，失败/超时/未配置则保留静态内容
- 前台**永远不白屏**；后台改坏的内容只影响"动态覆盖"，不影响页面骨架

### 3.3 技术决策（推荐即定）

| # | 决策点 | 结论 | 理由 |
|---|---|---|---|
| D1 | 多模型实现方式 | **文章单表不动；单网页/区块独立建表**（不分 type 混用） | 页面需要版本历史与回滚、区块有结构化字段（副标题/图片/链接），混表会让 mapper/前端复杂度陡增，对非程序员无益 |
| D2 | 服务/大赛/招聘 | **继续用 book 体系**，不并入 CMS | 已有分类/上下架/回收站/报名流；招聘复用 book 报名流（原 TODO #12 同款） |
| D3 | 政策文件 | **文章 + `attachment` 附件列**（PDF 上传），不建独立模型 | 正文文本块已能承载要点，缺的只是"原文下载" |
| D4 | 单网页 | **CMS 化**（cms_page），前台渐进增强接入 4 页 | 用户明确要求"后台改前台页面"，这是最低风险形态 |
| D5 | 首页数据条数字 | 自动统计 + 手动覆盖（覆盖值存 sys_config） | 服务总数/成员数/今日报名自动算；老板想写"入驻企业 6 家"这类自定义数字时可直接覆盖 |
| D6 | 联系方式 | 进"站点配置"（sys_config 白名单公开接口） | 电话/邮箱/地址/公众号后台可改，前台即时生效，成本低 |
| D7 | 运营角色 | **新建 role_key=`operator`（运营专员）**，授予 工作台/内容运营/成员与报名/运营辅助，**不授系统设置** | 与 editor 分离，明确排除系统设置防误操作；后续可按人细分 |
| D8 | 文章回收站 | **做**（复用 book/reader 两态模式：del_flag + recycle 记录） | 误删可恢复，非程序员兜底刚需 |
| D9 | 历史版本 | 页面每次保存自动存一版，**上限 20 版**，可查看/回滚 | 月度维护量一年约 12 版，20 版足够且存储可控 |
| D10 | 前台导航动态化 | **不做**（远期记录） | 导航横跨 12 个静态页 + 移动端汉堡菜单，动态化收益低、风险高；"添加外部链接"价值不足以覆盖改造量 |
| D11 | 不做清单 | 运维类（在线升级/缓存/漏洞扫描/日志）、多语言、订单电商、转移/同步 | 与 CMS改进.md 判断一致，与本项目定位无关 |
| D12 | 浏览量防刷 | Redis 按会话去重后 +1 | 防刷流量造假 |
| D13 | 定时发布 | publish_time 未来时间即预约，前台列表加 `publish_time <= NOW()` | 一个条件实现"预约发布"，成本≈0 |

---

## 四、模块方案

### 🔴 P0-1 栏目管理（分类树）——对标报告 §7 核心模式

- 新增 `views/system/cms/category.vue`：`el-tree` 栏目树（新增主分类/子分类、改名、排序、停用/启用），后端接口全部现成
- 重构 `cms/index.vue` 为**左树右表**：点击栏目联动过滤（替换下拉筛选），树顶部"全部文章"节点
- 栏目下有文章时删除被拒（后端 `countCmsArticleByCategoryId` 已实现），前端补友好提示
- 分类深度限制 ≤3 级（报告避坑 2）
- 新增菜单"栏目管理"，复用 `system:cmsCategory:*` 权限点

### 🔴 P0-2 文章管理增强

- 封面图缩略预览列
- 批量置顶/取消置顶、批量上架/下线（后端补 2 个批量接口）
- `cms_article` 加 `sort` 列：列表可输入序号，前台排序 = 置顶 → sort → 发布时间
- `cms_article` 加 `keywords`/`description`（SEO 元数据，P2 使用）与 `attachment`（政策 PDF，P0-5 使用）——**一次加列到位**
- 编辑弹窗：附件上传（政策原文 PDF）、正文 Quill 保持不变

### 🔴 P0-3 文章软删除回收站

- `cms_article` 加 `del_flag`/`recycle_id`/`deleted_time`/`deleted_by` 四列（对齐 `book` 先例）
- 删除 → 软删入回收站；新增 `views/system/recycle/cms.vue`（恢复/永久删除）
- 公开列表查询自动过滤已删；回收站菜单挂"运营辅助"

### 🔴 P0-4 前台文章 HTML 渲染修复（非程序员体验的关键一步）

- `article.html` 正文从 `textContent` 改为**白名单 HTML 渲染**：允许 p/br/strong/em/h1-h4/ul/ol/li/a/img/blockquote/pre/code/span(color)/table，其余标签过滤（防 XSS，Quill 输出相对干净，前端白名单正则即可）
- 链接自动 `<a>`（保留现有逻辑）；图片加最大宽度样式
- 验收：后台写"加粗 + 插图 + 列表"的文章，前台所见即所得

### 🔴 P0-5 政策原文 PDF 附件

- 政策类文章在编辑弹窗上传 PDF（`common/upload` 已支持，nginx 20MB）
- 前台 `article.html`：有附件时显示"📄 下载政策原文"按钮
- 对应原 TODO #10

### 🟡 P1-1 页面管理（单网页 + 历史回滚）——"后台改前台页面"核心

- 新表 `cms_page`（page_key 唯一/title/content/status(0草稿 1发布)/version/updated_by/update_time）+ `cms_page_history`（最多 20 版）
- 后台"页面管理"页：页面列表 + 编辑弹窗（标题 + Quill + 图片上传）+「预览前台」按钮（新窗口打开对应前台 URL）+ 历史版本 Tab（查看/一键回滚）
- 公开接口 `@Anonymous`：`/system/cmsPage/publicByKey/{pageKey}`（仅发布态）
- 前台接入 4 页：about / join / talent / industry——页面 HTML 保留现有正文为默认，JS fetch 成功后替换正文容器；失败走静态兜底
- 页面 key 约定：`about-main` / `join-main` / `talent-main` / `industry-main`

### 🟡 P1-2 首页区块管理

- 新表 `cms_block`（block_key/page_key/title/subtitle/content/image/link/sort/visible）
- 后台"区块管理"页：按页面分组，每块一个卡片表单（标题/副标题/正文/图片/链接/排序/显示开关）；**隐藏用开关不用删除**（误点不丢内容）
- `home.html` 接入区块（含静态兜底）：
  - `home-hero`：首屏标语 + 副标语 + CTA 文案
  - `home-stats`：数据条（数字自动统计，`overrides` 覆盖值存 sys_config：`site_stat_service` / `site_stat_member` / `site_stat_apply` / `site_stat_company`）
  - `home-feature-1/2/3`：三大赋能卡片（算力与技术支持 / 政策与金融 / 人才与教育）
  - `home-ecosystem`：产业生态入口（标题 + 链接）
  - `home-cta`：入驻 CTA 区块
  - `home-contact`：联系区（电话/地址，优先取站点配置）

### 🟡 P1-3 站点配置（报告 P1"设置菜单"落地）

- 复用 RuoYi 原生 `sys_config` + 后台"参数设置"页（已有，只需把键值说明汉化业务化）
- 新增 `@Anonymous` 白名单公开接口：`/system/config/publicList?keys=site_phone,site_email,site_address,site_wechat,site_qrcode,site_stat_*`
- 预置键：site_phone(0763-3391888) / site_email / site_address / site_wechat / site_qrcode(公众号二维码) / site_stat_*
- 前台 footer 联系区与 home-contact 区块优先取配置，未配置走静态默认

### 🟡 P1-4 运营工作台 + 角色收敛 + 帮助

- 新增"运营工作台"落地页（operator/editor/admin 登录后默认路由）：
  - 快捷入口大按钮：发文章 / 改页面 / 传轮播 / 管服务 / 看报名
  - 今日数据卡：新增文章 / 新增成员 / 今日报名 / 待处理入驻申请
  - 最近编辑记录（文章/页面各 5 条，点击直达编辑）
  - 操作指引卡（"三步发一篇新闻"）
- 新建 `operator` 角色（D7）：菜单 = 运营工作台 + 内容运营 + 成员与报名 + 运营辅助；**不授系统设置**
- 每个管理页顶部一行灰色使用说明 + 空状态引导（"还没有文章，点这里发布第一篇"）
- 侧边栏"使用帮助"页（图文操作手册：发文章/改页面/传轮播/看报名/回滚）

### 🟢 P2 运营增强（按需，最后一批）

- 浏览量防刷（D12）：`publicDetail` 前查 Redis 会话标记，未标记才 +1
- 定时发布（D13）：前台 `selectPublicArticleList` 加 `publish_time <= NOW()`，后台发布时可选发布时间（默认立即）
- SEO：文章 keywords/description（P0-2 已加列）→ 前台 `article.html` 动态更新 `<meta>`
- 政策正文结构化（可选）：政策文章正文按"【字段】"块解析为可折叠卡片，需时再做

---

## 五、数据模型变更汇总

```sql
-- 新增
cms_page           page_key(uk) / title / content / status / version / updated_by / update_time
cms_page_history   history_id / page_id / version / title / content / updated_by / update_time
cms_block          block_id / page_key / block_key / title / subtitle / content /
                   image / link / sort / visible / update_time

-- cms_article 加列（一次性）
sort int DEFAULT 0
attachment varchar(255)      -- 政策原文 PDF 等附件
keywords varchar(255)        -- SEO
description varchar(500)     -- SEO
del_flag char(1) DEFAULT '0' -- 回收站（对齐 book）
recycle_id bigint / deleted_time datetime / deleted_by varchar(64)

-- sys_config 预置键（幂等 INSERT）
site_phone / site_email / site_address / site_wechat / site_qrcode / site_stat_service / site_stat_member / site_stat_apply / site_stat_company
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
│   ├── 页面管理（新）      system:cmsPage:*
│   ├── 区块管理（新）      system:cmsBlock:*
│   └── 通知公告          （现有）
├── 成员与报名            （现有，不动）
├── 运营辅助
│   ├── 回收站            （现有 book/reader + 新增 cms）
│   └── 站点配置（新入口）   system:config:list（业务化说明）
└── 系统设置              （仅 admin；operator 不可见）
```

角色分配：`admin` 全量；`operator`（新）= 工作台 + 内容运营 + 成员与报名 + 运营辅助；`editor` = 内容运营 + 运营辅助（不含工作台，如需可并入）。

---

## 七、前台页面改造清单

| 页面 | 改造 | 兜底 |
|---|---|---|
| news.html | 不动（已数据驱动） | — |
| policy.html | 不动；政策详情走 article.html（新增附件按钮） | — |
| article.html | 正文改白名单 HTML 渲染 + 附件下载按钮 + SEO meta 动态化 | 现有"加载中/不存在"提示 |
| services.html | 不动 | — |
| about / join / talent / industry | 正文容器接 cms_page（渐进增强） | 保留静态正文 |
| home.html | hero/数据条/三大赋能/产业入口/CTA/联系 接 cms_block + 站点配置 | 保留静态区块 |
| profile / service / contest | 不动 | — |

---

## 八、实施计划（4 批，每批独立可验收）

| 批次 | 内容 | 工作量 | 验收标准 |
|---|---|---|---|
| **第一批 · 文章管理闭环** | P0-1 栏目树 + P0-2 文章增强（含加列）+ P0-3 回收站 + P0-4 HTML 渲染 + P0-5 PDF 附件 | ~4 人日 | 后台可建栏目树、文章封面/批量/排序、删除进回收站可恢复；前台富文本所见即所得；政策可传 PDF 并可下载 |
| **第二批 · 页面可编辑** | P1-1 cms_page + 历史回滚 + 4 页接入 | ~3.5 人日 | 后台改"走进社区"正文，前台刷新即变；改坏可回滚；断网/接口失败前台显示静态内容 |
| **第三批 · 首页与站点配置** | P1-2 cms_block + home 接入 + P1-3 站点配置 | ~2.5 人日 | 首页六大区块后台可编辑可隐藏；联系方式后台改前台即时生效；数据条支持覆盖 |
| **第四批 · 运维体验与增强** | P1-4 工作台/角色/帮助 + P2 防刷/定时/SEO | ~3 人日 | operator 登录只见运营菜单；三步发文章流程；浏览量防刷生效；预约发布生效 |

合计约 **13 人日**。每批完成自动 commit（push 需另行确认）；SQL 升级脚本随批交付。

---

## 九、风险与兜底

1. **前台白屏风险**：所有动态渲染均"静态兜底 + 超时回退"；区块加载失败仅跳过该区块
2. **XSS 风险**：内容仅管理员可写（登录 + 权限点），仍按白名单过滤渲染（P0-4）
3. **误操作风险**：删除走回收站、页面历史 20 版可回滚、区块只隐藏不删除、系统设置对 operator 不可见
4. **改坏页面风险**：非程序员只改内容字段（表单化），改不了布局/样式/导航；骨架层静态不可达
5. **SEO 风险**：骨架保留静态默认内容，CMS 覆盖是增强不是替换；新 SEO 字段只增不减
6. **数据迁移风险**：全部走幂等升级脚本，可重复执行；存量数据零迁移（仅加列）
