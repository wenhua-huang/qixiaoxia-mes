# CLAUDE.md

This file provides guidance to Claude Code when working with code in this repository.

## ⚠️ 核心约束

- **只修改本项目文件**，workspace 中其他目录（ktg-mes-ui、RuoYi-Vue 等）均为只读参考
- **编辑 Vue/XML 模板前必 Read 上下文**，确认标签嵌套。闭眼改 `<el-row>`/`<el-col>` 极易破坏结构
- **所有 SQL WHERE 必须带 `factory_id`**（由 MyBatis 拦截器自动注入，但编写时仍需确认）
- **所有表都有 `factory_id` 字段**，外协 8 张表额外冗余 `outsource_factory_id`（vendor、workorder、task、route_process、card_process、feedback、outsource_issue、outsource_recpt）
- **Debug：信任用户判断** — 用户指出问题方向时，先聚焦该方向直接修，修完验证。不要在用户说的方向还没查透时就去翻后端/DB/拦截器等无关代码。用户是实际操作者，判断优先级最高（详见 [[debug-rule-trust-user-judgment]]）

## 🗄️ 数据库迁移 (Flyway)

### 什么是 Flyway

Flyway 是数据库版本管理工具，通过**版本化的 SQL 迁移文件**管理 Schema 变更。每次应用启动时自动比对 `flyway_schema_history` 表，按版本号升序执行未跑过的迁移脚本。

**核心规则**：已执行的迁移文件**禁止修改**（checksum 校验），所有 DML **必须幂等**。

### 当前配置

| 配置项 | 值 | 说明 |
|--------|-----|------|
| 迁移文件路径 | `backend/ruoyi-admin/src/main/resources/db/migration/` | classpath 下的 SQL 迁移脚本 |
| 历史表 | `flyway_schema_history` | 记录已执行的迁移版本 |
| `baseline-on-migrate` | `true` | 已有数据库自动建立基线 |
| `baseline-version` | `32` | V1-V32 已存在于 DB，跳过执行 |
| 当前最新版本 | V39 | 新迁移从 V40 开始 |

### 命名规范

```
V{版本号}__{描述}.sql

✅ V40__add_product_bom_table.sql
✅ V41__seed_md_item_menu.sql
✅ V42__update_order_status_default.sql
❌ V40_add_bom.sql          （单下划线）
❌ V40__添加BOM表.sql        （中文文件名）
❌ 40__add_bom.sql           （缺 V 前缀）
```

- **前缀 `V`** — 版本化迁移，只执行一次，不可重复
- **版本号** — 严格递增整数，新需求取最大值 +1
- **双下划线 `__`** — 分隔符（不是单下划线）
- **描述** — 下划线分隔的英文，简明描述变更内容

### 🔧 需求涉及 SQL 时的标准流程

每次需求需要新增/修改表结构或种子数据时，必须遵循：

**Step 1 — 查版本号**
```bash
ls backend/ruoyi-admin/src/main/resources/db/migration/ | sort -V | tail -3
# 假设输出 V39，新文件用 V40
```

**Step 2 — 创建迁移文件**
```bash
touch backend/ruoyi-admin/src/main/resources/db/migration/V40__{描述}.sql
```

**Step 3 — 编写 SQL**（DDL 和 DML 可混在同一个文件，按逻辑顺序编排）

### ⚠️ 核心约束

#### 1. DML 必须幂等

Flyway 的 checksum 只防文件篡改，不防数据重复。一条 `INSERT` 如果重跑，会产生重复行。

```sql
-- ❌ 不幂等 — 每次启动都插入新行，迟早爆唯一约束
INSERT INTO sys_menu (menu_name, perms) VALUES ('物料管理', 'mes:md:item:list');

-- ✅ 幂等 — 用 WHERE NOT EXISTS 防重
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, perms, status)
SELECT '物料管理', 2000, 1, 'item', 'mes/md/item/index', 'mes:md:item:list', '0'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE perms = 'mes:md:item:list'
);
```

**所有 DML 语句（INSERT/UPDATE/DELETE）都必须做幂等判断**，不只是 INSERT。

#### 2. 已执行的迁移文件不可修改

```
V40 执行后 → flyway_schema_history 记录了 checksum
                    ↓
如果有人改 V40 的内容 → 启动时 checksum 不匹配 → 💥 报错拒绝启动
```

**需要调整 Schema？** — 新建一个更高版本号的迁移文件，在里面 `ALTER TABLE` 或 `UPDATE`，永远不修改已执行的旧文件。

#### 3. DDL + DML 混排

一个迁移文件可同时包含 DDL 和 DML，按逻辑顺序编排：

```sql
-- V40__add_product_bom.sql
-- Part 1: DDL（建表）
CREATE TABLE qxx_md_bom (
    id BIGINT NOT NULL AUTO_INCREMENT,
    item_id BIGINT NOT NULL COMMENT '物料ID',
    ...
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Part 2: DML（种子数据 — 注意幂等）
INSERT INTO sys_menu (...) SELECT ... WHERE NOT EXISTS (...);
INSERT INTO sys_dict_data (...) SELECT ... WHERE NOT EXISTS (...);
```

#### 4. `factory_id` 处理

Flyway 是裸 JDBC 执行 SQL，**不走 MyBatis 拦截器**。因此：

| SQL 类型 | factory_id 规则 |
|----------|----------------|
| `CREATE TABLE` | 必须包含 `factory_id` 字段（所有表） |
| `INSERT` | **必须显式写 `factory_id`**（拦截器不生效） |
| `UPDATE` / `DELETE` | WHERE 带 `factory_id` |

```sql
-- ✅ Flyway 迁移中的 INSERT 必须写 factory_id
INSERT INTO sys_menu (factory_id, menu_name, perms)
VALUES (1, '物料管理', 'mes:md:item:list');

-- ❌ 不要依赖拦截器自动注入（Flyway 不走拦截器）
INSERT INTO sys_menu (menu_name, perms)
VALUES ('物料管理', 'mes:md:item:list');
```

### 本地 vs 生产执行

| 场景 | 方式 |
|------|------|
| **本地开发** | 启动后端 → Flyway 自动扫描 `db/migration/` → 执行未跑过的版本 |
| **生产部署** | 同上（后端重启时自动执行）。也可手动兜底：`ssh qxx 'docker exec -i qxx-mysql mysql -uroot -pqxx123456 mes < xxx.sql'` |

> ⚠️ **只执行 SQL，不连带部署/重启等操作**（SQL 执行原则）

### 查看迁移历史

```sql
-- 查看已执行的迁移记录
SELECT version, description, installed_on, execution_time, success
FROM flyway_schema_history
ORDER BY version DESC
LIMIT 10;

-- 查看待执行的迁移
-- 对比 db/migration/ 目录 vs flyway_schema_history 表
```

## Project Overview

**企小侠文化纸盒MES系统** — 三端一体 monorepo，基于若依框架：

| 目录 | 项目 | 技术栈 | 子 CLAUDE.md |
|------|------|--------|-------------|
| `backend/` | RuoYi-Vue 后端 | Spring Boot 4.0.3 + JDK 17 + MyBatis | `backend/CLAUDE.md` |
| `frontend/` | PC 前端 | Vue 3 + TypeScript + Element Plus + Vite | `frontend/CLAUDE.md` |
| `app/` | 移动端 | uni-app + Vue 3 + Pinia | — |

## Quick Start

```bash
# 后端 (JDK 17+, MySQL 8.0+, Redis 7.0+)
cd backend
mysql -u root -p < sql/ry_20260417.sql && mysql -u root -p < sql/quartz.sql
# 修改 ruoyi-admin/src/main/resources/application-druid.yml 数据库连接
mvn clean package -pl ruoyi-admin -am -DskipTests
java -jar ruoyi-admin/target/ruoyi-admin.jar   # :8081

# PC 前端 (Node 18+)
cd frontend
yarn install && yarn dev   # :80，代理 /dev-api → localhost:8081
```

## MES 领域规划

| 领域 | 缩写 | 功能 | 领域 | 缩写 | 功能 |
|------|------|------|------|------|------|
| 基础数据 | md | 物料、BOM、工艺路线 | 仓储管理 | wm | 入库、领料、库存 |
| 生产管理 | pro | 工单、工序流转、报工 | 质量管理 | qc | IQC/IPQC/OQC |
| 设备管理 | dv | 台账、保养、OEE | 报表 | report | 生产/质量/库存报表 |
| 打印标签 | print | 标签打印、条码 | 工具管理 | tm | 工装模具 |

## 命名规范

- 后端包名: `com.ruoyi`（若依原生包名，按分层+domain 组织：Controller 在 `com.ruoyi.web.controller.mes.{domain}`，Service/Mapper/Domain 在 `com.ruoyi.system.{layer}.mes.{domain}`）。模块按若依分层: `ruoyi-admin` / `ruoyi-system` / `ruoyi-common`，不按 domain 分模块。⚠️ **是 `com.ruoyi` 不是 `com.qixiaoxia`**（IDE 配置若期望 `com.qixiaoxia` 会误挪文件）
- 数据库表: `qxx_{domain}_{entity}`（如 `qxx_wm_item_recpt`）
- 前端路由权限: `mes:{domain}:{entity}:{action}`（如 `mes:wm:itemrecpt:list`）
- 前端页面: `src/views/mes/{domain}/{entity}/`

## 关键设计决策

> 详细设计文档在 `docs/设计文档/`，Claude 按需 Read。

| 文档 | 内容 |
|------|------|
| [数据库设计决策](docs/设计文档/数据库设计决策.md) | 库存分层、SPU/SKU、BOM 约定 |
| [多工厂外协设计](docs/设计文档/多工厂外协设计.md) | factory_id、外协 8 张表 |
| [数据库字符集规范](docs/设计文档/数据库字符集规范.md) | utf8mb4 DDL 模板 |

## 后端关键机制

> 详见 `backend/CLAUDE.md`

- **FactoryId 拦截器** ✅ — MyBatis Interceptor 自动注入 `factory_id`，仅 `@SkipFactoryId` 可放行
- **Redis 分布式锁** ✅ — 库存变更必须用 Redisson，先锁后事务，禁止 `@Transactional`

## 新增 MES 功能（全栈步骤）

1. **检查 DB 字段**：`docker exec -i qxx-mysql mysql -uroot -pqxx123456 mes -e "DESCRIBE <table>"` — 缺失字段必须先 ALTER TABLE
2. **数据库迁移**：`backend/sql/` 下创建 DDL/DML 脚本
3. **代码生成**：RuoYi 代码生成器 → 导入表 → 一键生成 CRUD（标准 CRUD 优先用生成器，不手写）
4. **自动编码**：新建实体必须同步创建 `sys_auto_code_rule` + `sys_auto_code_part`（FIXCHAR + NOWDATE + SERIALNO）
5. **后端菜单**：`sys_menu` 表添加菜单记录
6. **前端页面**：`frontend/src/views/mes/{domain}/{entity}/`
7. **前端 API**：`frontend/src/api/mes/{domain}/`

**自动编码前端接入 4 处修改**：① import `genSerialCode` ② `autoGenFlag` ref ③ el-switch 模板 ④ handleAutoGenChange handler。详情见 `frontend/CLAUDE.md`。

**自动编码 SQL 模板**（`sys_auto_code_rule` + `sys_auto_code_part`）：
```sql
-- rule: 编码规则定义
INSERT INTO sys_auto_code_rule (factory_id, rule_code, rule_name, rule_desc, max_length, is_padded, padded_char, padded_method, enable_flag)
VALUES (1, '{RULE_CODE}', '{规则名称}', '{描述}', 20, 'N', '0', 'L', '1');
-- part 1: 固定前缀 (FIXCHAR)
INSERT INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, fix_character)
VALUES (1, @rid, 1, 'FIXCHAR', '{前缀编码}', '{前缀名称}', {长度}, '{前缀}');
-- part 2: 日期 (NOWDATE)
INSERT INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, date_format)
VALUES (1, @rid, 2, 'NOWDATE', 'DATE_PART', '日期', 8, 'yyyyMMdd');
-- part 3: 流水号 (SERIALNO, cycle_flag=Y cycle_method=DAY)
INSERT INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, seria_start_no, seria_step, cycle_flag, cycle_method)
VALUES (1, @rid, 3, 'SERIALNO', 'SERIAL_PART', '流水号', 3, 1, 1, 'Y', 'DAY');
```

## 测试策略

项目严格区分三层测试，各层职责不同，**禁止用 `curl` 调 API 替代任何一层的正式测试**。

> 命名规范、运行时约定、参考实现详见 [测试约定](docs/设计文档/测试约定.md)。

### 测试金字塔

| 层级 | 位置 | 框架 | 运行前提 | 测什么 | 真实接口 |
|------|------|------|---------|--------|:---:|
| **单元测试** | `backend/*/src/test/`（`*Test.java`） | JUnit 5 + Mockito | 无（不连 DB，全 Mock） | 单个 Service/Utils 方法逻辑 | ❌ |
| **组件测试** | `frontend/src/**/__tests__/`（`*.spec.ts`） | Vitest + @vue/test-utils | 无（jsdom mock） | 单个 Vue 组件渲染+交互 | ❌ |
| **集成测试** | `backend/ruoyi-admin/src/test/`（`*IT.java`） | SpringBootTest + Testcontainers | Docker（MySQL 容器 + Redis） | Controller → Service → Mapper → DB | ✅ |
| **E2E 测试** | `e2e/tests/`（`*.spec.ts`） | Playwright | ⚠️ **前后端必须同时启动** | 真实浏览器全栈业务流程 | ✅ |

### 接口调用红线

| 层级 | 规则 | 断网能跑？ |
|------|------|:---:|
| **后端单元测试** | 禁止连 DB/Redis/任何外部服务，全 Mock | ✅ 必须能 |
| **前端组件测试** | 禁止调真实后端 API，`vi.mock('@/api/xxx')` mock 所有 API 模块 | ✅ 必须能 |
| **集成测试** | 可连 Testcontainers 临时 DB，禁止连生产库 | ❌ 不能 |
| **E2E 测试** | 必须连真实前后端，禁止 mock 业务 API | ❌ 不能 |

### 运行命令

```bash
# === 单元测试（秒级，不连 DB）===
cd backend && mvn test                          # Surefire，*Test.java
cd frontend && npm test                         # Vitest

# === 集成测试（分钟级，需要 Docker）===
cd backend && mvn verify                        # Failsafe，*IT.java
# 前置：docker compose up -d redis

# === E2E 测试（分钟级，前后端必须都在运行）===
cd e2e && npx playwright test                   # 全量
cd e2e && npx playwright test tests/pur/order.spec.ts  # 单文件
```

### ⚠️ curl ≠ E2E

| 方式 | 经过前端编译渲染？ | 经过真实浏览器？ | 性质 |
|------|:---:|:---:|------|
| `curl` 调 API | ❌ | ❌ | **API 快速验证**（开发辅助），仅验证后端接口 |
| Playwright | ✅ | ✅ | **E2E 测试**（正式验证），全链路真实验证 |

curl 跳过了 Vue 编译、Axios 封装、响应拦截器解包、UI 渲染等关键环节，**禁止用 curl 结果声称"E2E 通过"**。

### 全栈功能上线前验证清单

> **测到 bug 先停**：本次引入的 → 直接修；项目既有的 → 停下告知，不偷偷修。

| # | 验证层 | 性质 | 操作 |
|---|--------|------|------|
| ① 服务可达 | 端口检查 | 开发辅助 | `curl -so /dev/null -w '%{http_code}' http://localhost:8081/ && curl -so /dev/null -w ' %{http_code}' http://localhost:5173/` |
| ② 编译 | import 解析 | 开发辅助 | `curl -s http://localhost:5173/src/views/mes/{M}/{E}/index.vue \| grep -o '/src/api/[^"]*' \| sort -u` |
| ③ API 快速验证 | curl CRUD | **冒烟，非 E2E** | `TOKEN=$(python3 backend/scripts/get_token.py); curl ...` → add → update → delete |
| ④ 取值 | 拦截器解包 | 开发辅助 | `grep -A20 'response.use' frontend/src/utils/request.ts` → 确认 `resolve(res.data)` |
| ⑤ genSerialCode | 连调 3 次 | 开发辅助 | 确认流水号递增、编码规则存在、`response.data` 取值正确 |
| ⑥ **正式 E2E** | Playwright 全链路 | **正式验证** | `cd e2e && npx playwright test`（前后端都必须启动） |

## 调试约定

```bash
TOKEN=$(python3 backend/scripts/get_token.py 2>/dev/null)
curl -s http://localhost:8081/getInfo -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
# 一步到位：python3 backend/scripts/get_token.py --test
# 依赖：pip3 install requests redis
```

## AI 行为准则

### 禁止操作
- 修改非本项目文件、跳过类型检查/lint 直接提交
- 顺手重构无关代码（即使看到烂代码，非本次任务范围就不动）

### 必须询问
- 删除/重构已有业务代码、修改 DDL、修改 API 签名、修改公共组件、引入新依赖

### 自主决策
- 补充缺失的 `factory_id`、代码生成器覆盖的标准 CRUD

### 工作流纪律

**Plan First** — 非 trivial 任务（≥3 步或涉架构决策）先进 plan mode，写出计划再执行。中途出错立刻停，重新 plan，不盲改。

**子 Agent** — 搜索/探索/多文件读取用子 agent 做，保持主上下文干净。Grep 优先于 `find | xargs grep`。

## 代码质量约束

| 约束 | 阈值 |
|------|------|
| 后端函数长度 | ≤ 50 行（模板/配置除外）|
| 前端组件长度 | ≤ 300 行，超过必须拆分 |
| 重复逻辑 | ≥ 2 次 → 抽取公共方法/组件 |
| 魔法数字 | 必须定义为常量 |
| 业务状态 | 用枚举，不用字符串 |
| 依赖 | 优先用框架/项目已有工具 |

## AI 提交前自检

- [ ] 类型检查通过（frontend: `npx vue-tsc --noEmit`，backend: `mvn compile`）
- [ ] SQL 迁移文件：命名 `V{next}__{desc}.sql`，DML 已幂等处理，INSERT 显式写 `factory_id`
- [ ] SQL: 所有 WHERE 带 `factory_id`
- [ ] 权限: Controller 有 `@PreAuthorize`
- [ ] 无硬编码魔法数字/字符串
- [ ] 新文件命名符合项目规范
- [ ] API 变更已同步更新前端 API 模块

## 生产服务器部署

### 服务器信息

| 项 | 值 |
|------|------|
| IP | `115.29.234.204` |
| 用户 | `root` |
| 密码 | `ShQxx2026@$^` |
| 连接命令 | `sshpass -p 'ShQxx2026@$^' ssh -o StrictHostKeyChecking=no root@115.29.234.204` |
| 项目路径 | `/var/www/qixiaoxia-mes` |
| 分支 | `main` |

### 服务架构

```
浏览器 → Nginx(:80) → /            → 静态文件(dist/)   前端(生产构建)
                     → /prod-api/   → Java(:8081)         后端
MySQL(:3307)  Redis(:6380)  MinIO(:9010)  均为 Docker 容器
```

> ⚠️ **服务器仅 1.8GB 内存，禁止在生产环境跑 Vite dev server 或执行 `vite build`。**
> 前端构建必须在本机完成（`cd frontend && npx vite build`），然后 scp `dist/` 到服务器。

### 发布流程

```bash
# === 前置：本机构建前端 ===
cd frontend && npx vite build

# 1. 连接服务器
sshpass -p 'ShQxx2026@$^' ssh -o StrictHostKeyChecking=no root@115.29.234.204

# 2. 拉取最新代码
cd /var/www/qixiaoxia-mes
git pull origin main

# 3. 执行 SQL 变更（如有）
docker exec -i qxx-mysql mysql -uroot -pqxx123456 mes --default-character-set=utf8mb4 < backend/sql/xxx.sql

# 4. 编译后端（必须用 JDK 17 + 跳过 checkstyle）
export JAVA_HOME=/usr/lib/jvm/java-17-alibaba-dragonwell-17.0.9.0.10.9-1.al8.x86_64
export PATH=$JAVA_HOME/bin:$PATH
cd backend
mvn clean package -pl ruoyi-admin -am -DskipTests -Dcheckstyle.skip=true -q

# 5. 重启后端
kill $(lsof -ti :8081) 2>/dev/null
sleep 2
nohup java -jar /var/www/qixiaoxia-mes/backend/ruoyi-admin/target/ruoyi-admin.jar \
  --server.port=8081 --ruoyi.profile=/var/www/qixiaoxia-mes/uploadPath \
  > /tmp/qxx-backend.log 2>&1 &

# 6. 上传前端 dist（本机执行）
sshpass -p 'ShQxx2026@$^' scp -o StrictHostKeyChecking=no -r frontend/dist/* root@115.29.234.204:/var/www/qixiaoxia-mes/frontend/dist/

# 7. 重载 Nginx
sshpass -p 'ShQxx2026@$^' ssh -o StrictHostKeyChecking=no root@115.29.234.204 'nginx -s reload'
```

### 发布后验证

```bash
TOKEN=$(curl -s -X POST http://localhost:8081/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123","code":"","uuid":""}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

# 后端直连
curl -s http://localhost:8081/mes/md/unitmeasure/list -H "Authorization: Bearer $TOKEN"

# Nginx 代理
curl -s http://localhost/prod-api/mes/md/unitmeasure/list -H "Authorization: Bearer $TOKEN"

# 前端页面
curl -s -o /dev/null -w "HTTP %{http_code}" http://localhost/
```

### 注意事项

- 服务器 **验证码已关闭**（`sys_config.captchaEnabled=false`）

## 环境版本

| 组件 | 版本 | 组件 | 版本 |
|------|------|------|------|
| JDK | 17+ | MySQL | 8.0+ |
| Maven | 3.9+ | Redis | 7.0+ |
| Node | 18.20 LTS | — | — |

## 前后端职责划分

| 职责 | 后端 | 前端 |
|------|:---:|:---:|
| 数据填充（默认值、关联数据、计算字段） | ✅ 负责 | ❌ 禁止 |
| 跨域数据聚合（如从工单BOM取消耗默认值） | ✅ 提供本域专用接口 | ❌ 禁止直接调其他域的 BOM API |
| 表单展示与交互 | — | ✅ 负责 |
| 前端主动请求后端 → 接收数据 → 渲染 | — | ✅ 接收即展示 |

**核心原则**：后端拥有数据逻辑，前端无感知。

- ❌ **禁止前端直接调跨域 API** 获取数据填充。如报工页物料消耗 → 禁止前端调 `workorderbom/listByWorkorderId` 自行拼装
- ✅ **后端提供专用接口**，封装内部数据查询逻辑。如 `GET /mes/pro/feedback/consumeDefaults/{workorderId}` → 后端内部查 BOM 表，返回 `List<ProFeedbackConsume>`
- ✅ 前端只跟本 domain 的 API 交互，不关心数据来源

## API 契约

- 单条: `{ code: 200, msg: "操作成功", data: {...} }`
- 分页: `{ code: 200, msg: "查询成功", rows: [...], total: 100 }`
- 后端新增/修改接口 → 必须同步更新前端 API 模块，字段命名前后端保持一致（camelCase）

### 🚫 禁止前端分步调用多个接口拼业务

**任何需要原子性的业务操作，必须由后端封装为一个接口，前端只调一次。**

| ❌ 错误做法 | ✅ 正确做法 |
|------------|------------|
| 前端调 `add 入库单头` → `add 入库行` → `confirm 确认收货` | 后端提供 `POST /receive`，一次接受 header+lines，内部事务完成三步 |
| 前端调 `生成领料单` → `生成缺料采购单` → `生成入库单` | 后端提供 `POST /generateDocs`，前端只传一个 `{ workorderId, flags }` |

**原因**：
1. 分步调用中间任何一步失败，数据处于半成品状态（孤儿入库单头、缺行等）
2. 网络抖动导致第 2 步没发出，第 1 步已生效，无法回滚
3. 移动端网络更不稳定，分步调用风险放大 10 倍
4. **事务必须由后端控制**，前端没有能力也不应该有

**设计新功能时，先问：这个操作需要几步？如果需要 ≥2 步，必须合并为一个接口。**

## Key References

- 若依官方: http://doc.ruoyi.vip
- Element Plus: https://element-plus.org/zh-CN/

## 🔁 新功能实现后自检工作流（CRT）⚠️ 自动执行

> 🚫 **禁止等待用户来手动启动测试或 Code Review。**
> 任何代码变更完成后，**必须立即自动执行** `/auto-crt`（或手动按顺序完成 CRT 三步）。
> **CRT 全部绿色通过 = 任务完成。CRT 未通过 = 任务未完成，禁止停止。**

### C — Code Review（三轮找 Bug）

直接对当前改动执行 `/code-review`（high effort）。每轮发现问题→修复→再 review，直到输出 `[]`（零 finding）。

```
第 1 轮：启动 /code-review → 通常 5~10 个 finding → 修复所有
第 2 轮：再次 /code-review → 通常 2~5 个 finding → 修复所有  
第 3 轮：再次 /code-review → 通常 0~2 个 finding → 修复至清零
```

**典型问题类型**：FactoryIdInterceptor 冲突（INSERT 别写 `factory_id`）、`@Transactional` 与锁顺序（先锁后事务）、并发幂等缺失（加 Redis 锁+DB 唯一约束）、mapper XML UPDATE 缺新列、N+1 查询、死代码。

### R — Repair（修复所有 Finding）

对每个 finding：读代码 → 确认根因 → 改代码 → `mvn compile -pl ruoyi-system -am -DskipTests -q` 验证编译通过。

**修复优先级**：🔴 系统不可用（SQL error/编译失败） → 🟠 数据正确性（重复/丢失） → 🟡 性能/风格。

### T — Test（三层测试全覆盖）

| 层级 | 工具 | 要求 |
|------|------|------|
| **后端单元测试** | JUnit 5 + Mockito | Service 每个 public 方法 ≥1 个测试。Mock 所有依赖，覆盖正常/边界/幂等/错误路径 |
| **后端集成测试** | SpringBootTest + Testcontainers | Controller 端点可达 + **E2E 连续流程**（如：工单→缺料看板→采购单→补货→领料单→完工→入库单） |
| **前端组件测试** | Vitest + @vue/test-utils | 新增/修改的 Vue 组件 ≥1 个测试。Mock API，验证渲染、API 调用参数、按钮行为、错误不崩溃 |

**运行命令**：
```bash
# 后端
cd backend
mvn test -pl ruoyi-system -Dtest="XxxUnitTest"  # 单元测试
mvn test -pl ruoyi-admin -Dtest="XxxIntegrationTest"  # 集成测试（需 Redis + Docker）

# 前端
cd frontend
npx vitest run src/views/mes/{domain}/{entity}/__tests__/  # 组件测试
```

**质量门禁**：三层测试全部 green + 已有回归测试未破坏 → 才能交付。
- uni-app: https://uniapp.dcloud.net.cn/
