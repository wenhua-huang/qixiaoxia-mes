# 自动 CRT（Code Review → Repair → Test）

**触发条件**：任何代码变更完成后**必须立即执行**，禁止等待用户手动触发。

## 核心原则

> 写完代码 ≠ 任务完成。CRT 全部绿色通过 = 任务完成。

## 流程（严格按序执行）

### Step 1 — 检测变更范围

```bash
git diff --name-only HEAD 2>/dev/null
```

识别变更文件类型：
- 后端 Java → 需要单元测试 + 集成测试
- 后端 XML (Mapper) → 需要集成测试
- 前端 Vue/TS → 需要组件测试 + vue-tsc
- Flyway SQL → 执行验证

### Step 2 — 补齐测试代码

| 变更类型 | 测试类型 | 框架 | 关键约束 |
|---------|---------|------|---------|
| 后端 Service | 单元测试 | JUnit 5 + Mockito | 全 Mock，不连 DB/Redis |
| 后端 Controller | 集成测试 | SpringBootTest + Testcontainers | 真实 DB 容器 |
| 前端 Vue 组件 | 组件测试 | Vitest + @vue/test-utils | mock 所有 API 模块 |
| 前端 API/工具 | 组件测试 | Vitest | mock axios |

**每个新增/修改的 public 方法 ≥1 个测试**，覆盖：
1. 正常路径（happy path）
2. 边界条件（null/空/零值）
3. 异常路径（ServiceException/网络错误）
4. 幂等性（重复调用）

### Step 3 — 运行测试

```bash
# 后端单元测试
cd backend && mvn test -pl ruoyi-system -Dtest="XxxUnitTest" -DfailIfNoTests=false 2>&1

# 后端集成测试（需 Docker Redis）
cd backend && mvn test -pl ruoyi-admin -Dtest="XxxIT" -DfailIfNoTests=false 2>&1

# 前端组件测试
cd frontend && npx vitest run --reporter=verbose 2>&1
```

所有测试必须绿色（0 failures, 0 errors）。

### Step 4 — 修复失败

- 测试红灯 → 读错误信息 → 修复代码或测试 → 重新运行直到全绿
- 禁止跳过失败的测试或加 `@Disabled`
- 如果测试本身写错了（非代码 bug），修正测试逻辑

### Step 5 — Code Review（三轮清零）

对当前变更执行 `/code-review`（使用 `Skill` 工具调用 `code-review`）：

```
第 1 轮 → 修复所有 finding → 第 2 轮 → 修复所有 finding → 第 3 轮 → 必须输出 []
```

**每轮修复后逐条确认 finding 已解决，不允许跳过或标记为 "won't fix"。**

### Step 6 — 类型检查

```bash
cd frontend && npx vue-tsc --noEmit 2>&1 | grep -v "node_modules"  # 前端：0 error
cd backend && mvn compile -pl ruoyi-system -am -DskipTests -q 2>&1   # 后端：无报错
```

## 最终门禁

全部满足才能视为任务完成：

- [ ] 后端单元测试全绿
- [ ] 后端集成测试全绿（如涉及）
- [ ] 前端组件测试全绿
- [ ] e2e测试全绿
- [ ] Code Review 零 finding（三轮确认）
- [ ] `vue-tsc --noEmit` 零新增类型错误
- [ ] `mvn compile` 无报错
- [ ] Flyway SQL 已在目标 DB 验证通过（如涉及）
- [ ] 启动前后端 输出端口

**任何一项不通过 → 修复 → 从 Step 3 重新开始，直到全部通过。**
