# CLAUDE.md

This file provides guidance to Claude Code when working with code in this repository.

## ⚠️ Workspace 约束

> **只允许修改本项目的文件！** VS Code workspace 中其他目录均为只读参考，**严禁修改**。

## ⚠️ 编辑前必须 Read

> **Vue 模板 / XML / HTML 等嵌套结构，编辑前务必 Read 上下文！** 
> 闭着眼睛盲改 `<el-row>` / `<el-col>` / `<template>` 等嵌套标签极易破坏结构，
> 导致 "Element is missing end tag" 等编译错误。
> 
> **规则**：
> 1. 编辑 Vue 模板前，先 Read 目标区域 + 前后各 10 行确认标签嵌套
> 2. 用最小化 diff — 增加/删除字段时优先改一行而非替换整个块
> 3. 编辑后 grep 检查 `</el-row>` / `</template>` 等闭合标签数量是否平衡
> 4. Vite HMR 编译错误会即时反馈 — 看到红色报错立刻修，别继续堆代码

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

### FactoryId MyBatis 拦截器 ✅ 已实现

> MyBatis Interceptor 自动注入 `factory_id`，Code Review 规则 ①②③ 由拦截器自动保证。

**实现文件**：
- `ruoyi-framework/.../interceptor/FactoryIdInterceptor.java` — 拦截器核心逻辑（~120 行）
- `ruoyi-common/.../annotation/SkipFactoryId.java` — 放行注解
- `ruoyi-framework/.../config/MyBatisConfig.java` — 注册拦截器

**拦截规则**：

| 场景 | 行为 |
|------|------|
| **INSERT/UPDATE** | 参数对象有 `factoryId` 字段且为 null → 自动注入 `SecurityUtils.getFactoryId()` |
| **SELECT** | 同上（参数对象自动注入 factoryId，SQL 通过 `<if test="factoryId != null">` 过滤） |
| **外协表** | 参数对象有 `outsourceFactoryId` 且已设值 → 跳过（用外协工厂 ID 替代） |
| **@SkipFactoryId** | Mapper 方法上有此注解 → 跳过所有拦截 |
| **参数为 null** | 跳过 |

> ⚠️ **所有用户（含 admin）都受拦截**，不区分角色。仅 `@SkipFactoryId` 可放行。外协不是放行——外协是已知业务规则，用 `outsource_factory_id` 替代 `factory_id` 做隔离。

**为什么不选 AOP**：AOP 只拦 Service 方法，Mapper 直接调用、动态 SQL 拼接都拦不到。MyBatis Interceptor 在 SQL 执行层，100% 覆盖。

### Redis 分布式锁 ✅ 已实现

> 凡是需要保证**跨实例库存一致性**的操作（入库/出库/调拨/退货），必须使用 Redisson 分布式锁，**禁止用 `synchronized`**（仅限单 JVM，无法水平扩展）。

**实现文件**：
- `ruoyi-common/.../redis/RedisLockTemplate.java` — 锁模板，封装 tryLock/finally/unlock 样板（~50 行）
- `ruoyi-common/.../enums/TransactionTypeEnum.java` — 库存事务类型枚举（8 种）
- `ruoyi-framework/.../config/RedissonConfig.java` — 手动创建 `RedissonClient` Bean（~35 行，读 `spring.data.redis.*`）
- `ruoyi-common/pom.xml` — `redisson` 3.27.2（**非** starter，避免与 Spring Boot 4.0 自动配置冲突）

**强制规则**：
- 库存变更**必须先锁后事务**：`lockTemplate.execute(lockKey, () -> txTemplate.execute(status -> doWork()))`，锁内的 lambda 第一行就是开事务。**禁止**用 `@Transactional` 注解（`@Transactional` 在方法入口就 `setAutoCommit(false)`，导致事务先于锁）
- TX Bean 新增类型**必须实现 `TxBean` 接口**，否则 `copyCommonFields` 静默跳过导致数据丢失
- TX Bean 的 `vendorId`/`workorderId` 等维度字段**必须正确传递**，禁止硬编码 0L

**为什么用 plain redisson 而非 redisson-spring-boot-starter**：
Spring Boot 4.0 移除了 `org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration`，starter 的 `RedissonAutoConfigurationV2` 引用该类导致启动报 `ClassNotFoundException`。手动 `@Bean` 配置绕过此问题。

**标准用法**：

```java
@Autowired private RedisLockTemplate lockTemplate;
@Autowired private PlatformTransactionManager txManager;

public WmTransaction processStock(WmTransaction tx) {
    TransactionTemplate tt = new TransactionTemplate(txManager);
    tt.setTimeout(30);
    return lockTemplate.execute("wm:stock:lock:" + itemId,
        () -> tt.execute(status -> doProcessTransaction(tx)));  // 🔒先锁 → 再开事务
}
```

关键：`lockTemplate.execute()` 的 lambda **第一行**就是 `tt.execute()`，确保 DB 事务在锁之后才开始。

**设计要点**：

| 要点 | 说明 |
|------|------|
| **Watchdog 自动续期** | `tryLock(waitTime, TimeUnit)` 不设 leaseTime，Redisson 每 10s 自动续期，DB 卡再久锁也不丢 |
| **锁粒度** | 与 `material_stock.uk_stock` 唯一键对齐：`item:batch:warehouse:vendor:workorder:quality`，不同库存记录可并发 |
| **`isHeldByCurrentThread()`** | finally 中先判断再 unlock，防止锁已过期时抛 `IllegalMonitorStateException` |
| **`tryLock` 超时** | 等待最多 5s，避免请求堆积；获取失败抛 `ServiceException` 让调用方决定重试 |

**`@Transactional` 与锁的顺序**（必须遵守：**先锁后事务**）：

```
processTransaction(tx)
  validate() / initStock()          ← 纯内存
  lockTemplate.execute(lockKey, () -> {     ← 🔒 先拿锁
      txTemplate.execute(status -> {        ← 再显式开事务（status = new TX）
          loadMaterialStock()               ← 快照在锁内产生
          updateStock() / insertTx()
          return result;
      });                                   ← COMMIT
  });                                       ← 🔓 放锁
```

**为什么不用 `@Transactional` 声明式事务？`@Transactional` 在方法入口就 `setAutoCommit(false)`，实际先于锁。** 改用 `TransactionTemplate` 显式控制：`new TransactionTemplate(transactionManager).execute(status -> ...)`，事务在锁内才开始，彻底消除 REPEATABLE_READ 快照时序隐患。

```java
@Autowired
private PlatformTransactionManager transactionManager;

public WmTransaction processTransaction(WmTransaction tx) {
    TransactionTemplate tt = new TransactionTemplate(transactionManager);
    return lockTemplate.execute(lockKey,
        () -> tt.execute(status -> doProcessTransaction(tx, stock)));
}
```

**强制规则**：
- 库存变更**必须**先锁后事务，用 `TransactionTemplate` 显式开 TX，**禁止**用 `@Transactional` 注解
- 锁内除了 DB 操作外不能有远程调用/耗时逻辑
- `lockTemplate.execute` 的 lambda 内第一行就是 `txTemplate.execute`

**选择 Redisson 而非手写 RedisLock 的原因**：

| 对比 | 手写 SET NX EX | Redisson RLock |
|------|---------------|----------------|
| 锁过期 | 固定 TTL，DB 卡了锁会丢 ❌ | Watchdog 自动续期 ✅ |
| 可重入 | 需自己实现计数器 | 内置支持 |
| 锁释放安全 | 需 Lua 脚本保证原子性 | `isHeldByCurrentThread()` |
| 代码量 | ~90 行 | 10 行 |
| 额外依赖 | 0 | redisson.jar ~5MB，Netty 已通过 lettuce 存在 |

### Adding a New MES Feature (Full Stack)

> ⚠️ **做计划前必须检查 DB 字段是否齐备。** 任何新功能计划的第一步是：
> ```bash
> docker exec -i qxx-mysql mysql -uroot -pqxx123456 mes -e "DESCRIBE <table>"
> ```
> 对照 DDL 设计文档确认目标字段是否存在。缺失字段必须在计划中列为独立的 DDL Step（ALTER TABLE），不能假设字段存在就跳过。**这是 AI 做计划时最容易漏的环节。**

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
7. **自动编码规则**: 任一需要编码的实体/单据，**必须**同步创建 `sys_auto_code_rule` + `sys_auto_code_part`（FIXCHAR前缀 + NOWDATE日期 + SERIALNO流水号），并在前端表单中加 `autoGenFlag` 开关调用 `genSerialCode`

### 自动编码接入步骤

任一新增 MES 实体（物料/客户/供应商/仓库/库区/单据等）必须有自动编码功能：

**Step 1 — 创建编码规则** (SQL)：
```sql
INSERT INTO sys_auto_code_rule (factory_id, rule_code, rule_name, rule_desc, max_length, is_padded, padded_char, padded_method, enable_flag)
VALUES (1, '{RULE_CODE}', '{规则名称}', '{描述}', 20, 'N', '0', 'L', '1');

-- 三部分：固定前缀 + 日期 + 流水号
INSERT INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, fix_character)
VALUES (1, @rid, 1, 'FIXCHAR', 'PREFIX', '{前缀}', {前缀长度}, '{前缀}');
INSERT INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, date_format)
VALUES (1, @rid, 2, 'NOWDATE', 'DATE_PART', '日期', 8, 'yyyyMMdd');
INSERT INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, seria_start_no, seria_step, cycle_flag, cycle_method)
VALUES (1, @rid, 3, 'SERIALNO', 'SERIAL_PART', '流水号', 3, 1, 1, 'Y', 'DAY');
```

**Step 2 — 前端 Vue 页面** (必须包含 4 处修改)：
```typescript
// ① import
import { genSerialCode } from '@/api/mes/sys/autocoderule'

// ② ref
const autoGenFlag = ref(false)

// ③ 模板 switch（放在编码 input 旁边）
<el-col :span="8">
  <el-form-item label-width="70" v-if="!form.xxxId">
    <el-switch v-model="autoGenFlag" active-color="#13ce66" active-text="自动生成" @change="handleAutoGenChange" />
  </el-form-item>
</el-col>

// ④ handler + reset 中重置 autoGenFlag
function handleAutoGenChange(flag: boolean) {
  if (flag) genSerialCode('RULE_CODE').then((r: any) => { form.value.xxxCode = r.data })
  else form.value.xxxCode = ''
}
function reset() { autoGenFlag.value = false; form.value = {} as ...; ... }
```

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

## AI 提交前自检清单

每完成一个开发任务，AI 必须自查：

- [ ] 类型检查通过（frontend: `npx vue-tsc --noEmit`，backend: `mvn compile`）
- [ ] 测试通过（前端 `npm test`，后端 `mvn test`）
- [ ] SQL: 所有 WHERE 条件带 `factory_id`
- [ ] 权限: Controller 有 `@PreAuthorize`，前端页面有权限指令
- [ ] 无硬编码魔法数字/字符串
- [ ] 新文件命名符合项目规范
- [ ] API 变更已同步更新前端 API 模块

### 前端开发规范

> 📁 **详见 `frontend/CLAUDE.md`** — Vue 3 + TS + Element Plus 编码规范、组件模式、表单校验、自动编码前端接入、提交前自检清单、label 间距、日期格式等。

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
浏览器 → Nginx(:80) → /            → Vite(:5173)   前端
                     → /prod-api/   → Java(:8081)    后端
MySQL(:3307)  Redis(:6380)  MinIO(:9010)  均为 Docker 容器
```

### 发布流程

```bash
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

# 6. 前端（Vite HMR 自动热更新，通常无需重启）
# 如需重启：kill $(lsof -ti :5173) && cd frontend && npx vite --host 0.0.0.0 --port 5173
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
- 服务器 JDK：Java 11（Maven 默认）+ Java 17（Alibaba Dragonwell，后端运行时）
- `mvn` 默认用 Java 11，**必须设 `JAVA_HOME` 指向 JDK 17 才能编译**
- `checkstyle.xml` 不在服务器上，需加 `-Dcheckstyle.skip=true`
- 前端用 Vite dev 模式（端口 5173），Nginx 代理到 80

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

## API 契约规范

### 后端响应格式（统一）

- 单条: `{ code: 200, msg: "操作成功", data: {...} }`
- 分页: `{ code: 200, msg: "查询成功", rows: [...], total: 100 }`

### 前端 API 调用

> 📁 **详见 `frontend/CLAUDE.md`** — API 模块封装规范、类型定义、函数命名。

### 前后端同步规则

- 后端新增/修改接口 → 必须同步创建/修改前端 API 模块
- 字段命名: 后端 Java camelCase，前端 TypeScript 保持一致
