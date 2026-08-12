---
name: bug-fix
description: Use when fixing any bug or defect — enforces investigate-before-fix discipline. Triggers on reports of errors, broken behavior, wrong data, exceptions, "doesn't work", regressions. Forbids guessing the cause; requires real observation, data verification, and full data-flow tracing before proposing a fix.
---

# Bug 修复流程

修复任何 bug 都必须遵循以下流程，**禁止靠推理猜测跳过实际调查**。

## 标准流程

### 1. 找到真实原因（不能靠猜）
- **实际观察**：打开页面/跑代码/查日志，亲眼看到异常现象
- **数据验证**：查数据库实际值、API 实际返回，用 `SELECT` 或浏览器 Network 面板确认
- **代码跟踪**：从 UI → API → 后端 → 数据库，完整走一遍数据流
- **排除假设**：列出所有可能原因，逐一用证据排除，保留有证据支撑的

**反例**：凭"Element UI 在值不匹配时可能显示原始值"这种推测下结论 ❌
**正例**：查数据库 `SELECT enable_flag, COUNT(*) FROM md_item GROUP BY enable_flag` 发现确实存在 '0'/'1' ✅

### 2. 出方案
- 列出可选修复方案（前端防御 / 后端修数 / 双管齐下等）
- 说明每个方案的影响面和风险
- 推荐方案并说明理由

### 3. 做计划
- 用 EnterPlanMode 进入计划模式
- 列出每一步要改什么文件、怎么改
- 计划通过后再动手

### 4. 修复
- 按计划逐项实施
- 每次改动最小化，一个 commit 做一件事
- 保持和现有代码风格一致

### 5. 验证（编译通过 ≠ 修复完成）
- 跑 lint / 编译确认无语法错误
- **后端改动必须重新打包并重启运行中的进程**：`mvn -pl ruoyi-admin -am package -DskipTests` → kill 旧进程 → `java -jar` 重启 → 等端口就绪。仅 `mvn compile` 不会更新运行中的 jar，等于没生效。
- **实测真实接口/页面**：用 token 调真正的接口（curl），或在浏览器/小程序里实际点一次，看到返回结果符合预期（报错被拦住 / 数据正确）。不能只看日志"启动成功"或只跑编译就声称修好。
- 跑全量 E2E 确认无回退
- 说明如何验证修复有效（查哪些数据、看哪些页面）

## 捷径（quick path）

1. **用户已定位**：用户明确指出问题方向和层面（如"前端阻止了提交"）→ 步骤 1 已完成，直接聚焦用户指出的方向修复，不要再扩散到后端/DB/拦截器。
2. **单文件 bug**：可跳过 Plan Mode（步骤 3）直接修复。但步骤 1（查数据）和步骤 5（验证）不可跳过。
