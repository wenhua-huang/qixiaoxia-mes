# CLAUDE.md

This file provides guidance to Claude Code when working with code in this repository.

## ⚠️ 核心约束

- **只修改本项目文件**，workspace 中其他目录（ktg-mes-ui、RuoYi-Vue 等）均为只读参考
- **编辑 Vue/XML 模板前必 Read 上下文**，确认标签嵌套。闭眼改 `<el-row>`/`<el-col>` 极易破坏结构
- **所有 SQL WHERE 必须带 `factory_id`**（由 MyBatis 拦截器自动注入，但编写时仍需确认）
- **所有表都有 `factory_id` 字段**，外协 8 张表额外冗余 `outsource_factory_id`（vendor、workorder、task、route_process、card_process、feedback、outsource_issue、outsource_recpt）
- **Debug：信任用户判断** — 用户指出问题方向时，先聚焦该方向直接修，修完验证。不要在用户说的方向还没查透时就去翻后端/DB/拦截器等无关代码。用户是实际操作者，判断优先级最高（详见 [[debug-rule-trust-user-judgment]]）

## 🗄️ SQL 执行

- 命令：`ssh qxx 'docker exec -i qxx-mysql mysql -uroot -pqxx123456 mes < xxx.sql'`
- 只执行 SQL，不连带部署/重启等操作

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

- 后端包名: `com.qixiaoxia.mes.{domain}`，模块: `qixiaoxia-{domain}`
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

## Key References

- 若依官方: http://doc.ruoyi.vip
- Element Plus: https://element-plus.org/zh-CN/
- uni-app: https://uniapp.dcloud.net.cn/
