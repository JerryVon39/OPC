# 📚 阅海图书管理系统

> 基于 **RuoYi-Vue 3.9.2**（Spring Boot 4 + Vue 2）二次开发的图书借阅管理系统
> 前后端分离 · 业务闭环 · 面向学习与演示

---

## ✨ 功能特性

### 图书业务（核心闭环）
- 📖 **图书管理**：图书信息增删改查、分类字典（文学/科技/历史）、在架/下架状态、库存管理、Excel 导入导出
- 👤 **读者管理**：读者信息维护、类型字典（学生/教师/普通读者）、**系统自动分配借书证号**
- 🔄 **借阅管理**：借书/还书完整业务流程
  - 借书：自动校验库存 → 库存 -1 → 应还日期自动 +30 天
  - 还书：一键归还 → 库存 +1
  - 逾期：**定时任务每日自动标记**（0 点执行）+ 页面红色逾期标签
- 📊 **借阅统计**：热门图书 Top10、读者借阅排行 Top10
- 📝 **读者登记**：表单构建产物落地的自助登记页

### 书店前台（顾客视角，匿名访问）
- 🛍️ 图书展示：商品卡片式浏览（类型/状态/价格/库存标签）
- 📝 读者登记：顾客自助登记，自动分配借书证号
- 📖 我的借阅：输入借书证号查询借阅记录

### 系统能力（若依框架提供）
- 用户/角色/菜单三级权限（RBAC，按钮级）
- 操作日志/登录日志审计
- 系统监控（在线用户/定时任务/数据库 SQL/缓存）
- 代码生成器（本项目的图书、读者模块均由它生成）

## 🛠️ 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Spring Boot 4 · Spring Security · JWT · MyBatis · Redis · Quartz |
| 前端 | Vue 2 · Element UI · Axios |
| 数据库 | MySQL 8 |

## 🚀 快速开始

### 环境要求
- JDK 17、Maven 3.6+、Node.js 16+、MySQL 8、Redis

### 启动步骤

```bash
# 1. 初始化数据库（依次导入）
mysql -uroot -p < sql/ry_20260417.sql        # 若依系统表
mysql -uroot -p < sql/quartz.sql             # 定时任务表
mysql -uroot -p < sql/business_init.sql      # 业务表+字典+菜单（幂等）

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

- 后台地址：http://localhost:80 （默认 admin / admin123）
- 前台演示：http://localhost:80/shop.html （顾客视角，无需登录）

> 💡 默认端口为 80，若被占用或权限不足可改 `ruoyi-ui/vue.config.js` 中的 port。

## 📁 项目结构

```
├── ruoyi-admin        # 启动模块（配置、定时任务 BorrowTask）
├── ruoyi-framework    # 框架层（安全/拦截器/切面）
├── ruoyi-system       # 业务模块（book/reader/borrow 的 controller/service/mapper）
├── ruoyi-common       # 公共工具
├── ruoyi-generator    # 代码生成器
├── ruoyi-quartz       # 定时任务框架
├── ruoyi-ui           # 前端（views/system/{book,reader,borrow}、public/shop.html）
└── sql                # 数据库脚本（含 business_init.sql 业务初始化）
```

**核心业务代码位置**：
- 借阅业务规则（借书/还书/库存联动/逾期）：`ruoyi-system/.../service/impl/BorrowRecordServiceImpl.java`
- 逾期定时任务：`ruoyi-admin/.../quartz/task/BorrowTask.java`
- 书店前台：`ruoyi-ui/public/shop.html`

## 📖 学习文档

- [《RuoYi-Vue 使用指南》](docs/RuoYi-Vue使用指南.md)：从零学习若依框架的完整手册（17 章：框架概念/功能详解/权限模型/开发流程/前台架构）
- [《代码生成器使用流程》](docs/代码生成器使用流程.md)：10 分钟生成完整业务模块的操作手册

## 🗂️ 版本记录（Git 提交轨迹）

| 提交 | 内容 |
|---|---|
| `b5e1433` | 原始若依源码 |
| `76917c5` | 图书管理系统定制：品牌/首页/组织架构/公告 |
| `78c5445` | 一期：借阅管理模块 + 借还书业务 + 库存联动 |
| `d425212` | 二期：逾期定时任务 + 借阅统计 + 前台"我的借阅" |
| `3d64277` | 菜单重构：业务/系统/监控/工具四区分离 |

## 📜 声明

本项目基于开源框架 **RuoYi-Vue**（[gitee.com/y_project/RuoYi-Vue](https://gitee.com/y_project/RuoYi-Vue)）二次开发，遵循 MIT 开源协议。仅用于学习交流。

---

*阅海图书管理系统 · 学习项目 · 2026*
