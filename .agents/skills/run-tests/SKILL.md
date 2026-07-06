---
name: run-tests
description: Use when running or writing tests for this project. Triggers on "跑测试/test/验证一下/mvn test/vitest/playwright/单元测试/集成测试/E2E". Routes to the correct tier (backend unit / frontend component / backend integration / E2E) and enforces the red lines: unit/component tests must pass offline with everything mocked; integration/E2E must hit real services. Reminds that curl ≠ E2E. Also covers the pre-deploy 6-layer verification checklist.
---

# 测试策略

项目严格三层测试，各层职责不同，**禁止用 `curl` 调 API 替代任何一层的正式测试**。命名规范、参考实现详见 [`docs/设计文档/测试约定.md`](../../../docs/设计文档/测试约定.md)。

## 测试金字塔

| 层级 | 位置 | 框架 | 运行前提 | 测什么 | 真实接口 |
|------|------|------|---------|--------|:---:|
| **后端单元** | `backend/*/src/test/`（`*Test.java`）| JUnit5 + Mockito | 无（不连 DB，全 Mock）| 单个 Service/Utils 方法 | ❌ |
| **前端组件** | `frontend/src/**/__tests__/`（`*.spec.ts`）| Vitest + @vue/test-utils | 无（jsdom mock）| 单个 Vue 组件渲染+交互 | ❌ |
| **后端集成** | `backend/ruoyi-admin/src/test/`（`*IT.java`）| SpringBootTest + Testcontainers | Docker（MySQL + Redis）| Controller → Service → Mapper → DB | ✅ |
| **E2E** | `e2e/tests/`（`*.spec.ts`）| Playwright | ⚠️ **前后端必须同时启动** | 真实浏览器全栈业务流程 | ✅ |

## 接口调用红线

| 层级 | 规则 | 断网能跑？ |
|------|------|:---:|
| 后端单元 | 禁止连 DB/Redis/任何外部服务，全 Mock | ✅ 必须 |
| 前端组件 | 禁止调真实后端 API，`vi.mock('@/api/xxx')` mock 所有 API | ✅ 必须 |
| 集成测试 | 可连 Testcontainers 临时 DB，禁止连生产库 | ❌ |
| E2E | 必须连真实前后端，禁止 mock 业务 API | ❌ |

## 运行命令

```bash
# === 单元测试（秒级，不连 DB）===
cd backend && mvn test                          # Surefire，*Test.java
cd frontend && npm test                         # Vitest

# === 集成测试（分钟级，需要 Docker）===
cd backend && mvn verify                        # Failsafe，*IT.java
# 前置：docker compose up -d redis
# ⚠️ 改 ruoyi-system 代码/XML 后必须先：mvn install -pl ruoyi-system -am -DskipTests

# === E2E 测试（分钟级，前后端必须都在运行）===
cd e2e && npx playwright test                   # 全量
cd e2e && npx playwright test tests/pur/order.spec.ts  # 单文件
```

### 后端单跑

```bash
mvn test -pl ruoyi-system -Dtest="XxxUnitTest"                          # 单个单元测试
mvn failsafe:integration-test -pl ruoyi-admin -Dit.test="XxxControllerIT" # 单个集成测试
```

调试 SQL：临时在 `application-test.yml` 设 `logging.level.com.ruoyi.system.mapper.mes: debug`。

### 前端单跑

```bash
npx vitest run src/views/mes/wm/__tests__/itemrecpt.spec.ts   # 单文件
npm run test:watch                                            # watch 模式
```

配置：`vitest.config.ts`（jsdom）、`src/__tests__/setup.ts`、`src/__tests__/helpers/`（mockRouter/mockStore）。

## curl ≠ E2E

| 方式 | 经过前端编译渲染？ | 经过真实浏览器？ | 性质 |
|------|:---:|:---:|------|
| `curl` 调 API | ❌ | ❌ | **API 快速验证**（开发辅助），仅验证后端接口 |
| Playwright | ✅ | ✅ | **E2E 测试**（正式验证），全链路真实验证 |

curl 跳过了 Vue 编译、Axios 封装、响应拦截器解包、UI 渲染等关键环节，**禁止用 curl 结果声称"E2E 通过"**。

## 全栈功能上线前 6 层验证清单

> 逐层过，不可跳。**测到 bug 先停**：本次引入的 → 直接修；项目既有的 → 停下告知，不偷偷修。

| # | 验证层 | 性质 | 操作 |
|---|--------|------|------|
| ① 服务可达 | 端口检查 | 开发辅助 | `curl -so /dev/null -w '%{http_code}' http://localhost:8081/ && curl -so /dev/null -w ' %{http_code}' http://localhost:80/` |
| ② 编译 | import 解析 | 开发辅助 | `curl -s http://localhost:80/src/views/mes/{M}/{E}/index.vue \| grep -o '/src/api/[^"]*' \| sort -u` |
| ③ API 快速验证 | curl CRUD | **冒烟，非 E2E** | `TOKEN=$(python3 backend/scripts/get_token.py); curl ...` → add → update → delete |
| ④ 取值 | 拦截器解包 | 开发辅助 | `grep -A20 'response.use' frontend/src/utils/request.ts` → 确认 `resolve(res.data)` |
| ⑤ genSerialCode | 连调 3 次 | 开发辅助 | 确认流水号递增、编码规则存在、`response.data` 取值正确 |
| ⑥ **正式 E2E** | Playwright 全链路 | **正式验证** | `cd e2e && npx playwright test`（前后端都必须启动）|

## 覆盖率底线

- 后端 Service 层单元测试 ≥ 80%；生成器 CRUD 可不测，手写业务必须测
- 前端关键页面（登录、CRUD 核心）组件测试 ≥ 60%；手写业务组件必须测
- Mapper 集成测试覆盖核心 SQL（至少含 factory_id 过滤、分页）
