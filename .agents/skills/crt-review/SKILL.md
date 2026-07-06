---
name: crt-review
description: Use after completing a new feature, bug fix, or any non-trivial change — runs the CRT self-check (Code Review → Repair → Test). Triggers on "自检/code review/审查一下/提交前检查/ready to ship/检查改动/review 一下". Does 3 rounds of review until zero findings, repairs each by severity, then verifies the 3 test tiers. Also enforces the pre-commit checklist (type check, factory_id in SQL, @PreAuthorize, etc).
---

# CRT 自检工作流（Code Review → Repair → Test）

> 每完成一个新功能 / bug 修复 / 非平凡改动后，必须按序执行三步，禁止省略或跳步。

## C — Code Review（三轮找 Bug）

对当前改动逐轮审查，每轮发现 → 修复 → 再审，直到输出零 finding。

```
第 1 轮：审查当前改动 → 通常 5~10 个 finding → 修复所有
第 2 轮：再次审查    → 通常 2~5 个 finding → 修复所有
第 3 轮：再次审查    → 通常 0~2 个 finding → 修复至清零
```

### 高频问题清单（重点查）

| 类型 | 典型问题 |
|------|---------|
| **FactoryIdInterceptor 冲突** | INSERT 时手写了 `factory_id`（拦截器会再注入一次，冲突）→ INSERT **别写** `factory_id`；Flyway 迁移里的 INSERT 才必须显式写 |
| **`@Transactional` 与锁顺序** | 需加锁的方法用了 `@Transactional`（事务先于锁）→ 改用 `TransactionTemplate`，先锁后事务 |
| **并发幂等缺失** | 两人同时操作同一资源 → 加 Redis 分布式锁 + DB 唯一约束 |
| **mapper XML UPDATE 缺新列** | 加了字段但 UPDATE 语句没加 → 新值存不进去 |
| **N+1 查询** | 循环里查 DB → 批量查或 JOIN |
| **死代码** | 注释掉的代码块 / 未调用的方法 → 删 |

### 审查方法

1. `git diff` 看全部改动
2. 逐文件分析：是否引入上述问题
3. 查跨文件一致性（Controller 改了签名 → 前端 API 是否同步）

## R — Repair（修复所有 Finding）

每个 finding：
1. 读代码确认根因（不靠猜，见 `bug-fix` skill）
2. 改代码
3. 验证编译：`mvn compile -pl ruoyi-system -am -DskipTests -q`（后端）/ `npx vue-tsc --noEmit`（前端）

**修复优先级**：

| 优先级 | 类型 | 示例 |
|--------|------|------|
| 🔴 立即修 | 系统不可用 | SQL error、编译失败、启动报错 |
| 🟠 必须修 | 数据正确性 | 重复/丢失、并发冲突、状态机错乱 |
| 🟡 建议修 | 性能/风格 | N+1、命名、死代码 |

## T — Test（三层测试全覆盖）

| 层级 | 工具 | 要求 |
|------|------|------|
| **后端单元** | JUnit5 + Mockito | Service 每个 public 方法 ≥1 测试。Mock 所有依赖，覆盖正常/边界/幂等/错误路径 |
| **后端集成** | SpringBootTest + Testcontainers | Controller 端点可达 + **E2E 连续流程**（如：工单→缺料看板→采购单→补货→领料单→完工→入库单）|
| **前端组件** | Vitest + @vue/test-utils | 新增/修改的 Vue 组件 ≥1 测试。Mock API，验证渲染、API 调用参数、按钮行为、错误不崩溃 |

运行命令见 `run-tests` skill。

**质量门禁**：三层测试全部 green + 已有回归测试未破坏 → 才能交付。

## 提交前自检清单

- [ ] 类型检查通过（frontend: `npx vue-tsc --noEmit`，backend: `mvn compile`）
- [ ] SQL 迁移：命名 `V{next}__{desc}.sql`，DML 已幂等，INSERT 显式写 `factory_id`（仅 Flyway 迁移内）
- [ ] SQL：所有 WHERE 带 `factory_id`（业务 SQL，非 Flyway）
- [ ] 权限：Controller 有 `@PreAuthorize`
- [ ] 无硬编码魔法数字/字符串
- [ ] 新文件命名符合项目规范（`qxx_{domain}_{entity}` / `mes:{domain}:{entity}:{action}`）
- [ ] API 变更已同步更新前端 API 模块（`frontend/src/api/mes/{domain}/`），字段命名前后端一致（camelCase）
