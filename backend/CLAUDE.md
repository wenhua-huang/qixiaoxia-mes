# CLAUDE.md

This file provides guidance to Claude Code when working with code in this repository.

## Project Overview

RuoYi-Vue backend (v3.9.2), Spring Boot 4.x + JDK 17. This is the backend for the **企小侠文化纸盒MES系统** (Qixiaoxia Cultural Paper Box MES). It provides REST APIs consumed by the `frontend/` (RuoYi-Vue3-TypeScript) and `app/` (RuoYi-App-Vue3).

**Source**: Cloned from https://gitee.com/y_project/RuoYi-Vue (master branch, Spring Boot 4.x).

## Build & Run

**运行时环境**: JDK 17+, Maven 3.9+, MySQL 8.0+, Redis 7.0+

```bash
# 开发环境启动 (默认 profile: druid)
cd backend
mvn clean package -pl ruoyi-admin -am -DskipTests
java -jar ruoyi-admin/target/ruoyi-admin.jar

# 或直接在 IDE (IntelliJ IDEA) 中运行 RuoYiApplication.main()
```

- **应用入口**: `ruoyi-admin/src/main/java/com/ruoyi/RuoYiApplication.java`
- **Servlet 初始化器**: `RuoYiServletInitializer.java` (支持 war 部署)
- **默认端口**: 8081 (配置在 `ruoyi-admin/src/main/resources/application.yml`)
- **数据库配置**: `application-druid.yml` (Druid 连接池，MySQL)
- **日志配置**: `logback.xml`
- **Swagger/OpenAPI**: 已集成 springdoc-openapi (v3.0.2)，访问 `/swagger-ui.html`

### 数据库初始化

```bash
# 1. 创建 MySQL 数据库 (推荐 utf8mb4)
# 2. 执行 SQL 脚本
mysql> source sql/ry_20260417.sql
mysql> source sql/quartz.sql
# 3. 修改 application-druid.yml 中的数据库连接信息
```

## Project Structure

| 模块 | 职责 |
|------|------|
| `ruoyi-admin/` | Web 入口 — Controller、启动类、`application.yml` |
| `ruoyi-framework/` | Security、JWT、AOP、数据权限、FactoryId 拦截器 |
| `ruoyi-system/` | 业务层 — Service + Mapper |
| `ruoyi-common/` | 工具类、异常、枚举、注解 |
| `ruoyi-generator/` | 代码生成器（Velocity 模板） |
| `ruoyi-quartz/` | 定时任务（Quartz） |
| `sql/` | 数据库初始化脚本（`ry_20260417.sql`、`quartz.sql`） |

> `ruoyi-ui/` 目录不使用，前端在 `../frontend/`。

## Architecture & Key Patterns

### Module Dependency

```
ruoyi-admin → ruoyi-framework → ruoyi-system → ruoyi-common
            → ruoyi-generator
            → ruoyi-quartz
```

### Layered Architecture

```
Controller (ruoyi-admin) → Service (ruoyi-system) → Mapper (ruoyi-system)
         ↕                        ↕
    ruoyi-framework          ruoyi-common
   (Security/JWT/AOP)       (utils/enums/annotation)
```

### Security

- **认证**: Spring Security + JWT (无状态)，`ruoyi-framework` 中配置
- **权限**: RBAC 模型 — 用户 → 角色 → 菜单/权限
- **数据权限**: `@DataScope` 注解，按部门数据隔离
- **工厂隔离**: `FactoryIdInterceptor` MyBatis 拦截器，自动注入 `factory_id`
- **注解驱动**: `@PreAuthorize` + 自定义 `@RequiresPermissions` / `@RequiresRoles`

### FactoryIdInterceptor ✅

> MyBatis Interceptor 自动注入 `factory_id`，所有用户（含 admin）都受拦截，仅 `@SkipFactoryId` 可放行。

**实现文件**：
- `ruoyi-framework/.../interceptor/FactoryIdInterceptor.java` — 拦截器核心逻辑（~120 行）
- `ruoyi-common/.../annotation/SkipFactoryId.java` — 放行注解
- `ruoyi-framework/.../config/MyBatisConfig.java` — 注册拦截器

**拦截规则**：

| 场景 | 行为 |
|------|------|
| INSERT/UPDATE/DELETE/SELECT | 参数对象 `factoryId` 为 null → 自动注入 `SecurityUtils.getFactoryId()` |
| 外协表 | `outsourceFactoryId` 已设值 → 跳过（用外协工厂 ID 替代） |
| @SkipFactoryId | Mapper 方法有注解 → 跳过所有拦截 |
| 参数为 null | 跳过 |

**为什么不选 AOP**：AOP 只拦 Service 方法，Mapper 直接调用、动态 SQL 拼接都拦不到。MyBatis Interceptor 在 SQL 执行层，100% 覆盖。

### Request Lifecycle

```
Client → JwtAuthenticationTokenFilter → SecurityConfig
       → Controller → Service → Mapper → MySQL/Redis
       → Response (统一格式: AjaxResult)
```

### Key Dependencies (from pom.xml)

| Dependency | Version | Purpose |
|-----------|---------|---------|
| Spring Boot | 4.0.3 | Framework |
| MyBatis Spring Boot | 4.0.1 | ORM |
| Druid | 1.2.28 | Connection pool |
| Springdoc OpenAPI | 3.0.2 | API documentation |
| JWT | 0.9.1 | Token-based auth |
| Fastjson | 2.0.61 | JSON serialization |
| PageHelper | 2.1.1 | MyBatis pagination |
| OSHI | 6.10.0 | System monitoring |
| Apache POI | 4.1.2 | Excel import/export |
| Velocity | 2.3 | Code generation templates |
| Kaptcha | 2.3.3 | Captcha generation |

### API Response Format

All controllers return `AjaxResult`:
```json
{ "code": 200, "msg": "操作成功", "data": {...} }
```
Table queries return `TableDataInfo`:
```json
{ "code": 200, "msg": "查询成功", "rows": [...], "total": 100 }
```

### Configuration Files

- `application.yml` — 主配置 (端口、profile、MyBatis、Springdoc 等)
- `application-druid.yml` — 数据源配置 (master/slave)
- `logback.xml` — 日志配置
- `i18n/messages.properties` — 国际化资源

## Customization for 企小侠 MES

### Package Naming Convention

所有企小侠 MES 定制代码应放在新模块中，命名规范：
- 基础包: `com.qixiaoxia.mes`
- 模块命名: `qixiaoxia-{domain}` (如 `qixiaoxia-wm` 仓储管理)
- 数据库表前缀: `qxx_` (区别于 RuoYi 原生的 `sys_`)

### Adding a New MES Module

1. 在根 `pom.xml` 中添加 `<module>qixiaoxia-{domain}</module>`
2. 在 `ruoyi-admin/pom.xml` 中添加依赖
3. Controller 放在 `ruoyi-admin/src/main/java/com/qixiaoxia/mes/{domain}/controller/`
4. Service/Mapper 放在新模块中
5. 数据库表建在 `sql/` 目录下，命名 `qxx_{domain}_*.sql`

## Testing

### Unit Tests（JUnit 5 + Mockito）

`@ExtendWith(MockitoExtension.class)` + `@Mock`/`@InjectMocks`，不启动 Spring 容器。命名 `should_{expected}_when_{condition}`，`@DisplayName` 中文描述。覆盖正常/边界/异常路径。

### Integration Tests（SpringBootTest + Testcontainers）

继承 `BaseIntegrationTest`，`@SpringBootTest(webEnvironment = RANDOM_PORT)`，`RestTemplate` 发请求，断言 HTTP 响应 + DB 最终状态。

前置：`docker compose up -d redis`（Redis 必须启动）。**改 `ruoyi-system` 的代码/XML 后必须先 `mvn install -pl ruoyi-system -am -DskipTests`，否则测试从 `~/.m2` 加载旧 jar**。

```bash
mvn install -pl ruoyi-system -am -DskipTests    # 改依赖模块后必须先 install
mvn test                          # 单元测试
mvn test -pl ruoyi-system         # 特定模块
mvn verify                        # 单元 + 集成
mvn failsafe:integration-test -pl ruoyi-admin -Dit.test="XxxControllerIT"  # 单跑一个集成测试
```

调试 SQL：临时在 `application-test.yml` 设 `logging.level.com.ruoyi.system.mapper.mes: debug`。

## RuoYi 框架工具清单

> ⚠️ RuoYi `ruoyi-common` 已封装大量工具类和注解，**优先使用，禁止重复造轮子或另引三方库**。

### 工具类（`com.ruoyi.common.utils`）

| 工具 | 用途 | 禁止代替方案 |
|------|------|-------------|
| `StringUtils` | 字符串判空、格式化、驼峰/下划线互转 | ❌ Apache Commons / 手写 |
| `DateUtils` | 日期格式化、解析、计算 | ❌ `SimpleDateFormat` 手写 |
| `SecurityUtils` | 获取当前用户、角色、权限、`factoryId` | ❌ 从 HttpSession 手动取 |
| `ServletUtils` | 获取 Request/Response、请求参数 | ❌ 直接注入 `HttpServletRequest` |
| `DictUtils` | 字典数据读取（自带缓存） | ❌ 每次查数据库 |
| `ExceptionUtil` | 异常堆栈转字符串 | ❌ 手写 `printStackTrace` |
| `Arith` | 精确浮点运算 | ❌ `double` 直接计算 |
| `MessageUtils` | i18n 国际化消息 | ❌ 硬编码中文提示 |
| `LogUtils` | 操作日志记录 | ❌ 手写日志插入逻辑 |
| `PageUtils` | 分页数据包装 | ❌ 手写分页包装 |

### 关键注解（`com.ruoyi.common.annotation`）

| 注解 | 功能 | 位置 |
|------|------|------|
| `@Log` | 自动记录操作日志入库 | 方法级 |
| `@DataScope` | 数据权限部门隔离 | Mapper 方法 |
| `@Excel` | Entity 字段标注 → 自动导出 | Entity 字段 |
| `@RepeatSubmit` | 防重复提交（仅 HTTP 幂等，非并发锁） | Controller 方法 |
| `@RateLimiter` | 接口限流（Redis 令牌桶） | Controller 方法 |
| `@Anonymous` | 跳过认证 | Controller 方法 |
| `@Sensitive` | 字段脱敏（日志/返回时） | Entity 字段 |

### 框架基类

| 类 | 提供能力 |
|----|---------|
| `BaseEntity` | `createBy`、`createTime`、`updateBy`、`updateTime`、`remark` |
| `BaseController` | `startPage()`、`getDataTable()`、`success()`、`toAjax()`、`error()` |
| `TreeEntity` | 树形表通用字段（`parentId`、`ancestors`） |

## 代码风格与静态检查

### Java 编码约定

- **import 顺序**: `com.qixiaoxia` → `com.ruoyi` → 三方库 → `java.*` → `jakarta.*`
- **包结构**: Controller 放 `ruoyi-admin/module/controller/{domain}/`，Service/Mapper 放业务模块
- **命名**: 类名 PascalCase，方法/变量 camelCase，常量 UPPER_SNAKE_CASE
- 所有 `@RequestMapping` 路径使用 kebab-case（`/wm/item-receipt`）
- **函数长度 ≤ 50 行**（模板/配置除外），超过必须拆分

### 推荐静态检查

待配置 `maven-checkstyle-plugin`，AI 生成代码后先 `mvn compile` 验证。

## DTO/Entity/VO 分层规范

> RuoYi 代码生成器不分 DTO/VO，**标准 CRUD 用 Entity 贯穿，不建 DTO/VO**。

**仅手写复杂业务时建 VO/Body**：多表组合查询结果、请求体与 Entity 差异大、敏感字段需隐藏。返回字段 ≥ 80% 匹配 Entity 则直接用 Entity，不提前建"以后可能用"的 VO。

转换优先用静态工厂方法 `XxxVO.from(entity)`，禁止逐字段 setter 和 `BeanUtils.copyProperties`。

## 分层开发模板

> 标准 CRUD 用 **RuoYi 代码生成器**（`ruoyi-generator`）一键生成，零手写。以下仅记关键规则。

- **Controller**：继承 `BaseController`，方法加 `@PreAuthorize`，用 `startPage()` / `getDataTable()` / `success()` / `toAjax()`
- **Service**：`@Transactional(rollbackFor = Exception.class)`，通过 `SecurityUtils.getUsername()` 取操作人。**库存变更等需加锁的方法禁止 @Transactional，改用 TransactionTemplate（见 Redis 分布式锁节）**
- **Mapper XML**：**所有 SELECT/UPDATE/DELETE WHERE 必须带 `factory_id = #{factoryId}`**（拦截器自动注入参数值，但 SQL 需有 `<if test>` 条件）

### 🚫 禁止让前端分步调接口拼业务

**任何多步原子操作，必须封装为一个后端接口，在一个事务中完成。** 绝对不允许：
```java
// ❌ 暴露 3 个独立接口给前端串行调用
POST /mes/wm/item_recpt          // 创建头
POST /mes/wm/item_recpt_line      // 创建行
PUT  /mes/wm/item_recpt/confirm   // 确认收货
```
正确做法：
```java
// ✅ 一个接口完成全部操作，内部 @Transactional 保证原子性
POST /mes/wm/item_recpt/receive   // 请求体: { header: {...}, lines: [...] }
```
**判断标准**：如果前端要调 ≥2 个接口才能完成一个用户操作 → 合并。

## Redis 分布式锁 ✅

> 库存变更（入库/出库/调拨/退货）**必须用 Redisson 分布式锁**，禁止 `synchronized`（仅限单 JVM）。

**实现文件**：
- `ruoyi-common/.../redis/RedisLockTemplate.java` — 锁模板（~50 行）
- `ruoyi-common/.../enums/TransactionTypeEnum.java` — 库存事务类型枚举
- `ruoyi-framework/.../config/RedissonConfig.java` — 手动创建 `RedissonClient` Bean（~35 行）
- `ruoyi-common/pom.xml` — `redisson` 3.27.2（**非** starter，避免 Spring Boot 4.0 自动配置冲突）

**强制规则**：
- **先锁后事务**：`lockTemplate.execute(lockKey, () → txTemplate.execute(status → doWork()))`，锁内 lambda 第一行就是开事务
- **禁止** `@Transactional` 注解 — 事务在方法入口就 `setAutoCommit(false)`，实际先于锁
- 锁内禁止远程调用/耗时逻辑

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

**设计要点**：

| 要点 | 说明 |
|------|------|
| Watchdog 自动续期 | `tryLock(waitTime)` 不设 leaseTime，Redisson 每 10s 续期 |
| 锁粒度 | 与 `material_stock.uk_stock` 对齐：`item:batch:warehouse:vendor:workorder:quality` |
| `isHeldByCurrentThread()` | finally 中先判断再 unlock，防止锁过期抛异常 |
| `tryLock` 超时 | 最多等 5s，失败抛 `ServiceException` |

**为什么 Redisson 而非手写 RedisLock**：

| 对比 | 手写 SET NX EX | Redisson RLock |
|------|---------------|----------------|
| 锁过期 | 固定 TTL，DB 卡了锁会丢 ❌ | Watchdog 自动续期 ✅ |
| 可重入 | 需自己实现计数器 | 内置支持 |
| 锁释放安全 | 需 Lua 脚本保证原子性 | `isHeldByCurrentThread()` |

### 必须加锁的 MES 场景

| 场景 | 并发冲突 | 后果 |
|------|---------|------|
| 库存扣减（领料/入库/盘点） | 两人同时扣减同一批次库存 | 库存超扣/负数 |
| 工单状态变更（开工/暂停/完工/关闭） | 两人同时操作同一工单 | 状态机混乱 |
| 质检判定提交（IQC/IPQC/OQC） | 两人同时提交同一检验单 | 判定结果被覆盖 |
| 外协收料/发料确认 | 两人同时确认同一外协单 | 重复入库 |
| 设备状态变更（维修→正常、报废） | 两人同时更新同一设备 | 状态跳跃 |

## 异常处理与日志

- Service 抛 `ServiceException("描述")`，不吞异常。Controller 由 `GlobalExceptionHandler` 统一捕获。
- 日志级别：`info` 业务节点 → `warn` 可恢复异常 → `error` 不可恢复异常。格式用占位符，异常作最后参数：`log.error("入库失败, recptCode={}", code, ex)`。

## 测试覆盖率底线

- Service 层单元测试覆盖率 ≥ 80%
- Mapper 集成测试覆盖核心 SQL（至少含 factory_id 过滤、分页查询）
- 生成器的标准 CRUD 可不测（框架保证），手写业务逻辑必须测

## 环境与依赖管理

- Maven BOM（父 POM `<dependencyManagement>`）统一管理版本
- 新增依赖：优先查 `ruoyi-common` 是否已有工具 → 其次用 Spring Boot 自带 → 最后才引入三方库
- 引入新依赖需在父 POM 声明版本，不得在子模块直接写 version
