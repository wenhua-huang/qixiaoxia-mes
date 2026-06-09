# CLAUDE.md

This file provides guidance to Claude Code when working with code in this repository.

## ⚠️ Workspace 约束

> **只允许修改本项目的文件！** VS Code workspace 中其他目录均为只读参考，**严禁修改**。

| 项目 | 路径 | 说明 |
|------|------|------|
| **qixiaoxia-mes** (当前) | `/Users/huangwenhua/company/self/qixiaoxia-mes/` | ✅ 唯一开发项目，可修改 |
| ktg-mes-ui | `/Users/huangwenhua/company/self/ktg-mes-ui/` | ❌ 仅作参考 |
| ktg-mes | `/Users/huangwenhua/company/self/ktg-mes/` | ❌ 仅作参考 |
| RuoYi-Vue | `/Users/huangwenhua/company/self/RuoYi-Vue/` | ❌ 仅作参考 |
| qixiaoxia-mes (旧) | `/Users/huangwenhua/qixiaoxia-mes/` | ❌ 仅作参考 |
| cyp | `/Users/huangwenhua/company/cyp/` | ❌ 仅作参考 |
| MES-SpringBoot | `/Users/huangwenhua/company/self/MES-SpringBoot/` | ❌ 仅作参考 — 黑科MES，Spring Boot 2.1 + Layui |
| mes (Qcadoo MES) | `/Users/huangwenhua/company/self/mes/` | ❌ 仅作参考 — Qcadoo Framework，插件化MES架构 |

## Project Overview

**企小侠文化纸盒MES系统** — 定制化制造执行系统，专为文化纸盒生产行业设计。

三端一体 monorepo，基于若依 (RuoYi) 开源框架：

| 目录 | 项目 | 技术栈 |
|------|------|--------|
| `backend/` | RuoYi-Vue 后端 | Spring Boot 4.0.3 + JDK 17 + MyBatis |
| `frontend/` | RuoYi-Vue3-TypeScript PC前端 | Vue 3 + TypeScript + Element Plus + Vite |
| `app/` | RuoYi-App-Vue3 移动端 | uni-app + Vue 3 + Pinia |

每个子项目有独立的 `CLAUDE.md`。

## Quick Start

```bash
# 后端 (JDK 17+, Maven 3.9+, MySQL 8.0+, Redis 7.0+)
cd backend
mysql -u root -p < sql/ry_20260417.sql
mysql -u root -p < sql/quartz.sql
# 修改 ruoyi-admin/src/main/resources/application-druid.yml 中数据库连接
mvn clean package -pl ruoyi-admin -am -DskipTests
java -jar ruoyi-admin/target/ruoyi-admin.jar   # 默认端口 8080

# PC前端 (Node 18+)
cd frontend
yarn install && yarn dev   # 默认端口 80, 代理 /dev-api → localhost:8080

# 移动端
cd app && npm install
# 用 HBuilder X 打开，运行到浏览器/小程序/真机
```

## MES Domain Planning

| 领域 | 缩写 | 功能描述 |
|------|------|---------|
| 基础数据 | md | 物料主数据、BOM、工艺路线、工作中心、纸盒规格 |
| 仓储管理 | wm | 原材料入库、生产领料、成品入库、库存盘点、库位管理 |
| 生产管理 | pro | 生产工单、工序流转、报工、产量统计、生产排程 |
| 质量管理 | qc | 来料检验(IQC)、过程检验(IPQC)、成品检验(OQC)、不良品处理 |
| 设备管理 | dv | 设备台账、保养计划、维修记录、OEE分析 |
| 报表分析 | report | 生产报表、质量报表、库存报表、设备报表 |
| 打印标签 | print | 纸盒标签打印、条码生成、装箱标签 |

## 设计决策

> 详细设计文档在 `docs/设计文档/` 下，Claude 按需 Read。此处仅列索引。

| 文档 | 内容 |
|------|------|
| [数据库设计决策](docs/设计文档/数据库设计决策.md) | 库存分层、SPU/SKU (parent_id)、BOM 约定、表结构约定、复卷明细 |
| [多工厂外协设计](docs/设计文档/多工厂外协设计.md) | factory_id（全量表）、outsource_factory_id（8张表）、外协登录与报工流程 |
| [Qcadoo架构分析](docs/设计文档/Qcadoo架构分析.md) | Qcadoo MES 架构思想、表设计对比、qixiaoxia-mes 落地方案 |
| [数据库字符集规范](docs/设计文档/数据库字符集规范.md) | utf8mb4 DDL 模板、导入规范、乱码排查与修复 |
| [测试约定](docs/设计文档/测试约定.md) | 测试金字塔、命名规范、Pre-commit/Pre-PR 流程 |

核心原则速查：

- **所有表都有 `factory_id`**，DDL 模板：`factory_id bigint(20) not null comment '工厂ID'` + `key idx_factory_id`
- **外协8张表冗余 `outsource_factory_id`**：vendor、workorder、task、route_process、card_process、feedback、outsource_issue、outsource_recpt
- **`sys_user.factory_id`** not null，用户直接归属工厂

## Development Conventions

### Git Workflow

- 主分支: `master`
- 功能分支: `feature/{domain}-{feature-name}`
- 修复分支: `fix/{issue-description}`

### Naming Conventions

- **后端包名**: `com.qixiaoxia.mes.{domain}`
- **后端模块**: `qixiaoxia-{domain}` (如 `qixiaoxia-wm`)
- **数据库表**: `qxx_{domain}_{entity}` (如 `qxx_wm_item_recpt`)
- **前端路由权限**: `mes:{domain}:{entity}:{action}` (如 `mes:wm:itemrecpt:list`)
- **前端页面路径**: `src/views/mes/{domain}/{entity}/`

### SQL 编写约定

> ⚠️ **Code Review 必检项。所有 SQL 必须遵守，违反即为 bug。**

**① 所有表 — WHERE 必须带 `factory_id`**

```sql
-- ✅ 正确
SELECT * FROM qxx_wm_item_recpt WHERE factory_id = ? AND recpt_id = ?;
-- ❌ 错误：漏了 factory_id
SELECT * FROM qxx_wm_item_recpt WHERE recpt_id = ?;
```

**② 外协相关表 — WHERE 区分场景**

| 场景 | WHERE 条件 |
|------|-----------|
| 本工厂自己的数据 | `WHERE factory_id = ?` |
| 本工厂作为接单方的外协任务 | `WHERE outsource_factory_id = ?` |
| 本工厂发包给某个供应商 | `WHERE factory_id = ? AND vendor_id = ?` |

**③ INSERT — 必须写入 `factory_id`**

```sql
-- ✅ 正确
INSERT INTO qxx_wm_item_recpt (factory_id, recpt_code, ...) VALUES (?, ?, ...);
```

### FactoryId MyBatis 拦截器 ⚠️ 待实现

> 以下为设计，尚未实现。实现后 Code Review 规则 ①②③ 由拦截器自动保证。

**方案**：MyBatis Interceptor 拦截 `Executor.update()` 和 `Executor.query()`。

- **INSERT/UPDATE**：参数对象有 `factoryId` 字段 → 自动注入 `SecurityUtils.getFactoryId()`
- **SELECT**：涉及的表有 `factory_id` 列 → SQL 自动追加 `AND factory_id = ?`
  - 涉及的表有 `outsource_factory_id` 列且当前请求为外协视角 → 追加 `AND outsource_factory_id = ?`（不追加 `factory_id`）

**放行**：方法上加 `@SkipFactoryId` 注解跳过所有拦截。仅用于平台管理员全局查询等极少数场景。**外协不是放行**——外协是已知业务规则，用 `outsource_factory_id` 替代 `factory_id` 做隔离，不是无限制跳过。

**为什么不选 AOP**：AOP 只拦 Service 方法，Mapper 直接调用、动态 SQL 拼接都拦不到。MyBatis Interceptor 在 SQL 执行层，100% 覆盖。

```java
// 核心代码 ~80 行，位于 ruoyi-common 模块
@Intercepts({
    @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
    @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})
})
public class FactoryIdInterceptor implements Interceptor { ... }
```

### Adding a New MES Feature (Full Stack)

**标准 CRUD 操作优先用 RuoYi 代码生成器**，不要手写。

代码生成器位于 `ruoyi-generator` 模块。在数据库中建好表后：
```bash
# 访问 http://localhost:8080 → 系统工具 → 代码生成 → 导入表 → 生成
# 生成内容：Entity、Mapper(Java+XML)、Service、Controller、前端 Vue 页面、前端 API
```

生成器自动处理：BaseController 继承、@PreAuthorize 权限注解、分页、Excel 导出、el-table 模板。零 bug，零遗漏。

**以下情况 Claude 手写**：复杂业务逻辑（外协报工、批次匹配）、MES 特有流程（工单审批流）、生成器无法覆盖的自定义查询。

**全栈开发步骤**：
1. **数据库**: `backend/sql/` 下创建迁移脚本
2. **代码生成**: RuoYi 代码生成器 → 导入表 → 一键生成 CRUD
3. **后端菜单**: 在 `sys_menu` 表中添加菜单记录
4. **PC前端页面**: `frontend/src/views/mes/{domain}/{entity}/`
5. **PC前端 API**: `frontend/src/api/mes/{domain}/`
6. **移动端页面**: `app/pages/mes/{domain}/`(如需要)

## Environment Versions

| 组件 | 版本 | 备注 |
|------|------|------|
| JDK | 17+ | LTS, Oracle / Amazon Corretto |
| Maven | 3.9+ | 配置阿里云镜像加速 |
| MySQL | 8.0+ | utf8mb4 字符集 |
| Redis | 7.0+ | 缓存 & Session |
| Node | 18.20 LTS | 前端构建 |

## 调试约定

> 调试后端 API 时，**必须先通过 `get_token.py` 获取 token**，不要手动走 captcha→Redis→login 流程。

```bash
TOKEN=$(python3 backend/scripts/get_token.py 2>/dev/null)
curl -s http://localhost:8081/getInfo -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
# 一步到位：python3 backend/scripts/get_token.py --test
```

**依赖**：`pip3 install requests redis`。可通过环境变量覆盖 `QXX_API_URL`、`QXX_REDIS_HOST` 等配置。

## Key References

- 若依官方文档: http://doc.ruoyi.vip
- Element Plus 文档: https://element-plus.org/zh-CN/
- uni-app 文档: https://uniapp.dcloud.net.cn/

## AI 工程纪律

### 编码中 — 最小化变更，不越界

- **用最简单方案实现**：优先选择改动最少、影响面最小的方案
- **不做过度设计**：只解决当前需求，不为"可能"的未来需求加抽象层
- **不做未来优化**：性能优化、代码抽象等只在实际需要时做
- **只改必要代码**：不顺手改拼写、不顺手调整格式、不顺手"优化"
- **不影响其他模块**：修改前确认影响范围，不改公共模块除非需求明确要求
- **不重构无关代码**：即使看到烂代码，除非本次任务涉及，否则不动

### 编码后 — 确保可交付

- **每次任务必须有明确完成标准**：任务开始前列出 Done 条件，完成时逐条对照
- **输出必须可验证**：每个产出都能被客观验证（测试通过 / 页面可操作 / 接口返回正确）

## 代码质量约束

> ⚠️ SRP/DRY/KISS 等通用原则 AI 天生就会，此处只列项目特定阈值和 AI 容易违反的规则。

### 禁止顺手重构（AI 高频违规）

- 即使看到命名不规范、代码重复、可以优化，只要不是本次任务目标，就不动
- 例外：修复本次任务直接导致的编译错误/类型错误

### 禁止重复（项目特定阈值）

- 同一逻辑出现 ≥2 次 → 抽取公共方法/组件
- 例外：类型定义、配置文件、模板代码不在此限

### 函数/组件长度上限

- 后端：函数 ≤ 50 行（模板/配置除外）
- 前端：组件 ≤ 300 行，超过必须拆分

### 魔法数字与硬编码

- 魔法数字必须定义为常量
- 业务状态用枚举，不用字符串

### 最小化依赖

- 优先使用框架/项目已有工具（详见 backend/CLAUDE.md 中 RuoYi 工具清单）
- 新增依赖前确认项目是否已有同类工具

## AI 行为准则

### 自主决策（无需询问）

- 补充缺失的 `factory_id` 过滤条件
- 代码生成器覆盖的标准 CRUD

### 必须询问

- 删除/重构已有业务代码
- 修改数据库表结构（DDL）
- 修改 API 接口签名
- 修改公共组件/工具函数
- 引入新依赖

### 禁止操作

- 修改非本项目文件（参考 workspace 约束）
- 跳过类型检查/lint 直接提交代码

## AI 提交前自检清单

每完成一个开发任务，AI 必须自查：

- [ ] 类型检查通过（frontend: `npx vue-tsc --noEmit`，backend: `mvn compile`）
- [ ] 测试通过（前端 `npm test`，后端 `mvn test`）
- [ ] SQL: 所有 WHERE 条件带 `factory_id`
- [ ] 权限: Controller 有 `@PreAuthorize`，前端页面有权限指令
- [ ] 无硬编码魔法数字/字符串
- [ ] 新文件命名符合项目规范
- [ ] API 变更已同步更新前端 API 模块

## API 契约规范

### 后端响应格式（统一）

- 单条: `{ code: 200, msg: "操作成功", data: {...} }`
- 分页: `{ code: 200, msg: "查询成功", rows: [...], total: 100 }`

### 前端 API 调用

- 封装在 `src/api/mes/{domain}/{entity}.ts`
- 函数命名: `listXxx`, `getXxx`, `addXxx`, `updateXxx`, `delXxx`
- 参数类型必须在 `src/types/` 定义

### 前后端同步规则

- 后端新增/修改接口 → 必须同步创建/修改前端 API 模块
- 字段命名: 后端 Java camelCase，前端 TypeScript 保持一致
