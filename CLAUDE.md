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

## Project Overview

**企小侠文化纸盒MES系统** (Qixiaoxia Cultural Paper Box MES) — 定制化制造执行系统，专为文化纸盒生产行业设计。

本项目是一个三端一体 monorepo，基于若依 (RuoYi) 开源框架构建：

| 目录 | 项目 | 技术栈 | 来源 |
|------|------|--------|------|
| `backend/` | RuoYi-Vue 后端 | Spring Boot 4.0.3 + JDK 17 + MyBatis | [Gitee](https://gitee.com/y_project/RuoYi-Vue) |
| `frontend/` | RuoYi-Vue3-TypeScript PC前端 | Vue 3 + TypeScript + Element Plus + Vite | [Gitcode](https://gitcode.com/yangzongzhuan/RuoYi-Vue3) (typescript 分支，官方源) |
| `app/` | RuoYi-App-Vue3 移动端 | uni-app + Vue 3 + Pinia | [Gitee](https://gitee.com/y_project/RuoYi-App) (vue3 分支) |

每个子项目有独立的 `CLAUDE.md`，包含详细的架构说明和开发指南。

## Quick Start

### 1. 后端 (backend/)

```bash
# 环境要求: JDK 17+, Maven 3.9+, MySQL 8.0+, Redis 7.0+
cd backend

# 1. 初始化数据库
mysql -u root -p < sql/ry_20260417.sql
mysql -u root -p < sql/quartz.sql

# 2. 修改 ruoyi-admin/src/main/resources/application-druid.yml 中数据库连接

# 3. 启动 (IDE 中运行 RuoYiApplication.main() 或命令行)
mvn clean package -pl ruoyi-admin -am -DskipTests
java -jar ruoyi-admin/target/ruoyi-admin.jar
# 默认端口: 8080
```

### 2. PC前端 (frontend/)

```bash
# 环境要求: Node 18+
cd frontend
yarn install   # 或 npm install
yarn dev       # 默认端口 80, 代理 /dev-api → localhost:8080
```

### 3. 移动端 (app/)

```bash
cd app
npm install
# 用 HBuilder X 打开，运行到浏览器/小程序/真机
```

## Repository Source Map

| 子项目 | 源仓库 | 分支 | 平台 |
|--------|--------|------|------|
| backend | `y_project/RuoYi-Vue` | master (Spring Boot 4.x) | Gitee |
| frontend | `yangzongzhuan/RuoYi-Vue3` | typescript (Vue3+TS) | Gitcode |
| app | `y_project/RuoYi-App` | vue3 (Vue3+Pinia) | Gitee |

> **说明**：RuoYi-Vue3-TypeScript 官方由 `yangzongzhuan` 维护在 Gitcode，y_project 在 Gitee 上无此仓库。后端和 App 均为 y_project 官方 Gitee 仓库。

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

### Adding a New MES Feature (Full Stack)

1. **数据库**: `backend/sql/` 下创建迁移脚本
2. **后端实体/Service/Mapper**: `backend/ruoyi-system/` 或新模块
3. **后端 Controller**: `backend/ruoyi-admin/src/main/java/com/qixiaoxia/mes/{domain}/controller/`
4. **后端菜单**: 在 `sys_menu` 表中添加菜单记录
5. **PC前端页面**: `frontend/src/views/mes/{domain}/{entity}/`
6. **PC前端 API**: `frontend/src/api/mes/{domain}/`
7. **移动端页面**: `app/pages/mes/{domain}/`(如需要)

## Environment Versions

| 组件 | 版本 | 备注 |
|------|------|------|
| JDK | 17+ | LTS, Oracle / Amazon Corretto |
| Maven | 3.9+ | 配置阿里云镜像加速 |
| MySQL | 8.0+ | utf8mb4 字符集 |
| Redis | 7.0+ | 缓存 & Session |
| Node | 18.20 LTS | 前端构建 |
| IDE | IntelliJ IDEA / VS Code | 后端/前端开发 |

## 数据库字符集规范 (Database Charset Rules)

> ⚠️ **曾因 `character_set_client=latin1` 导入 SQL 导致全库中文乱码。请严格遵守以下规范。**

### 新表 DDL 模板

```sql
CREATE TABLE qxx_xxx_xxx (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    name VARCHAR(100) NOT NULL COMMENT '名称',
    ...
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='表注释';
```

### SQL 迁移文件规范

每个 SQL 文件**开头必须加**：
```sql
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
```

### JDBC 连接字符串

`application-druid.yml` 中必须使用 `characterEncoding=utf8mb4`（不是 `utf8`）：
```yaml
url: jdbc:mysql://localhost:3307/mes?useUnicode=true&characterEncoding=utf8mb4&...
```

### MySQL 客户端导入

```bash
# 正确：指定字符集
docker exec qxx-mysql mysql -u root -p --default-character-set=utf8mb4 < xxx.sql

# 错误：未指定字符集（会继承客户端默认 latin1）
docker exec qxx-mysql mysql -u root -p < xxx.sql
```

### 乱码排查

```sql
-- 检查存储的原始字节：中文 UTF-8 编码以 Ex 开头（如 E7 A0 94 = 研）
SELECT column_name, HEX(column_name) FROM table_name WHERE condition;

-- 如果 hex 以 C3/C2/E2 等 2-3 字节开头 → 说明是双重编码乱码
-- 正确 utf8mb4 中文：E7A094 (=研, 3字节, E7开头)
-- 双重编码乱码：    C3A7C2A0... (=ç  , 被当成拉丁字符再次编码)
```

### 修复工具

如果确认是双重编码，使用修复脚本：
```bash
# 预览
python3 backend/scripts/fix_encoding.py --dry-run
# 执行修复
python3 backend/scripts/fix_encoding.py
```

## 调试约定 (Debugging Convention)

### API 调试前置：自动获取 Token

> **规则**：当需要调试后端 API（调用 `/getInfo`、`/getRouters` 等受保护接口）时，**必须先通过 `get_token.py` 脚本获取 token**，不要手动走 captcha→Redis→login 流程。

```bash
# 基础：获取 token 并存入变量
TOKEN=$(python3 backend/scripts/get_token.py 2>/dev/null)

# curl 中使用
curl -s http://localhost:8081/getInfo -H "Authorization: Bearer $TOKEN" | python3 -m json.tool

# 一步到位：获取 token 并直接测试 API
python3 backend/scripts/get_token.py --test

# 输出 header 格式，直接给 curl 用
python3 backend/scripts/get_token.py --print-header
# → Authorization: Bearer eyJhbGciOi...
```

**依赖**：`pip3 install requests redis`（已安装）

**配置**：可通过环境变量覆盖默认值：
- `QXX_API_URL` — 后端地址（默认 `http://localhost:8081`）
- `QXX_REDIS_HOST` / `QXX_REDIS_PORT` / `QXX_REDIS_PASSWORD` — Redis 配置

### curl 调试模板

```bash
# 标准模板：获取 token → 调用 API
TOKEN=$(python3 backend/scripts/get_token.py 2>/dev/null)
curl -s http://localhost:8081/接口路径 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"key":"value"}' | python3 -m json.tool
```

## Testing Conventions (测试约定)

### Test Hierarchy (测试金字塔)

| 层级 | 位置 | 框架 | 运行速度 | 范围 |
|------|------|------|---------|------|
| Unit | `backend/*/src/test/` | JUnit 5 + Mockito | 秒级 | 单个 Service/Utils 方法 |
| Integration | `backend/ruoyi-admin/src/test/` | SpringBootTest + Testcontainers | 分钟级 | Controller → DB 全链路 |
| Component | `frontend/src/**/__tests__/` | Vitest + @vue/test-utils | 秒级 | 单个 Vue 组件 |
| E2E | `e2e/tests/` | Playwright + TypeScript | 分钟级 | 全栈业务流程 |

### Database Rules for Tests

1. **单元测试**不连接数据库（Mock 所有 Mapper）
2. **集成测试**使用 Testcontainers MySQL 8.0.33，不连本地/开发数据库
3. 测试数据库名：`mes_integration_test`，字符集 utf8mb4
4. Schema 通过 Spring `sql.init` 从 `backend/sql/ry_20260417.sql` 自动初始化
5. 每个测试类在 `@BeforeEach` 中清理目标表
6. 生产库 `mes`，测试库 `mes_integration_test`，严禁交叉

### Naming & Directory Conventions

**Backend**:
- Unit test: `src/test/java/{包路径}/{类名}Test.java` → 由 Surefire 执行 (`mvn test`)
- Integration test: `src/test/java/{包路径}/{类名}IT.java` → 由 Failsafe 执行 (`mvn verify`)
- Method naming: `should_{expected}_when_{condition}` (snake_case 英文)
- `@DisplayName` 中文描述

**Frontend**:
- Test files: `__tests__/{组件名}.spec.ts`（与组件同目录）
- `data-testid` 属性用于元素定位（优先于 CSS class）

**E2E**:
- Files: `e2e/tests/{模块}/{用例}.spec.ts`
- Selector 优先级: `data-testid` > `aria-label` > CSS class

### Runtime Conventions

**Pre-commit** (must pass):
```bash
cd backend && mvn test          # 单元测试 <30s
cd frontend && npm test          # 组件测试 <10s
```

**Pre-PR** (must pass):
```bash
cd backend && mvn verify         # 单元 + 集成测试
cd frontend && npm test          # 组件测试
```

**合并后 / 夜间**: E2E 测试自动运行（不阻塞提交）

### Prompt Hints for Claude

写测试时遵循以下模式：

**Backend unit test**: `@ExtendWith(MockitoExtension.class)`, Mock 所有依赖, 覆盖正常+边界+异常
**Backend integration test**: 继承 `BaseIntegrationTest`, 用 `RestTemplate`, DB 断言
**Frontend component test**: `vi.mock('@/api/xxx')`, mock Router/Pinia, 用 `data-testid`
**E2E test**: 复用 `storageState`, `waitForResponse` 等 API 返回, 不用 `sleep`

### 参考实现

- 单元测试示例: `backend/ruoyi-system/src/test/java/com/ruoyi/system/service/impl/SysConfigServiceImplTest.java`
- 集成测试示例: `backend/ruoyi-admin/src/test/java/com/ruoyi/web/controller/system/SysConfigControllerIT.java`
- 集成测试基类: `backend/ruoyi-admin/src/test/java/com/ruoyi/BaseIntegrationTest.java`
- 组件测试示例: `frontend/src/views/__tests__/login.spec.ts`
- E2E 示例: `e2e/tests/auth/login.spec.ts`

## Key References

- 若依官方文档: http://doc.ruoyi.vip
- RuoYi-Vue 文档: http://doc.ruoyi.vip/ruoyi-vue/
- RuoYi-App 文档: http://doc.ruoyi.vip/ruoyi-app/
- Element Plus 文档: https://element-plus.org/zh-CN/
- uni-app 文档: https://uniapp.dcloud.net.cn/
