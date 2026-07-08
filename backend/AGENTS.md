# backend/AGENTS.md

RuoYi-Vue 后端（v3.9.2），Spring Boot 4.0.3 + JDK 17 + MyBatis。为 `frontend/`（PC）和 `app/`（移动端）提供 REST API。源 https://gitee.com/y_project/RuoYi-Vue（Spring Boot 4.x 分支）。

> 根级约束（factory_id、Flyway、命名、测试策略）见 [`../AGENTS.md`](../AGENTS.md)，本文不重复。

## Build & Run

```bash
cd backend
mvn clean package -pl ruoyi-admin -am -DskipTests
java -jar ruoyi-admin/target/ruoyi-admin.jar     # :8081
# 或 IDE 运行 com.ruoyi.RuoYiApplication
```

- 应用入口 `ruoyi-admin/.../RuoYiApplication.java`，默认端口 8081（`application.yml`）
- 数据源 `application-druid.yml`（Druid + MySQL），日志 `logback.xml`
- Swagger `/swagger-ui.html`（springdoc-openapi 3.0.2）
- DB 初始化：`mysql> source sql/ry_20260417.sql; source sql/quartz.sql;` 后改 `application-druid.yml`

## Project Structure

| 模块 | 职责 |
|------|------|
| `ruoyi-admin/` | Web 入口 — Controller、启动类、配置 |
| `ruoyi-framework/` | Security、JWT、AOP、数据权限、FactoryId 拦截器 |
| `ruoyi-system/` | 业务层 — Service + Mapper |
| `ruoyi-common/` | 工具类、异常、枚举、注解 |
| `ruoyi-generator/` | 代码生成器（Velocity 模板）|
| `ruoyi-quartz/` | 定时任务 |

模块依赖：`ruoyi-admin → ruoyi-framework → ruoyi-system → ruoyi-common`，另 `→ ruoyi-generator / ruoyi-quartz`。`ruoyi-ui/` 不用，前端在 `../frontend/`。

## FactoryIdInterceptor ✅

MyBatis Interceptor 自动注入 `factory_id`，所有用户（含 admin）都受拦截，仅 `@SkipFactoryId` 放行。

- 实现：`ruoyi-framework/.../interceptor/FactoryIdInterceptor.java`（~120 行）
- 注解：`ruoyi-common/.../annotation/SkipFactoryId.java`
- 注册：`ruoyi-framework/.../config/MyBatisConfig.java`

| 场景 | 行为 |
|------|------|
| INSERT/UPDATE/DELETE/SELECT | 参数对象 `factoryId` 为 null → 注入 `SecurityUtils.getFactoryId()` |
| 外协表 | `outsourceFactoryId` 已设值 → 跳过（用外协工厂 ID）|
| `@SkipFactoryId` | 跳过所有拦截 |
| 参数为 null | 跳过 |

> 为什么不用 AOP：AOP 只拦 Service 方法，Mapper 直接调用、动态 SQL 拼接都拦不到。MyBatis Interceptor 在 SQL 执行层，100% 覆盖。

## Redis 分布式锁 ✅

库存/状态变更**必须用 Redisson 分布式锁**，禁止 `synchronized`（仅限单 JVM）。

- `ruoyi-common/.../redis/RedisLockTemplate.java`（锁模板，~50 行）
- `ruoyi-common/.../enums/TransactionTypeEnum.java`
- `ruoyi-framework/.../config/RedissonConfig.java`（手动建 `RedissonClient` Bean，~35 行，**非** starter，避免 Spring Boot 4.0 自动配置冲突）
- `ruoyi-common/pom.xml`：`redisson` 3.27.2

**强制规则**：
- **先锁后事务**：锁内 lambda 第一行开事务
- **禁止** 在加锁方法上用 `@Transactional`（注解在方法入口就 `setAutoCommit(false)`，实际先于锁）
- 锁内禁止远程调用/耗时逻辑

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

| 要点 | 说明 |
|------|------|
| Watchdog 自动续期 | `tryLock(waitTime)` 不设 leaseTime，Redisson 每 10s 续期 |
| 锁粒度 | 对齐 `material_stock.uk_stock`：`item:batch:warehouse:vendor:workorder:quality` |
| 释放安全 | finally 中先 `isHeldByCurrentThread()` 再 unlock |
| `tryLock` 超时 | 最多等 5s，失败抛 `ServiceException` |

### 必须加锁的 MES 场景

| 场景 | 冲突后果 |
|------|---------|
| 库存扣减（领料/入库/盘点）| 库存超扣/负数 |
| 工单状态变更（开工/暂停/完工/关闭）| 状态机混乱 |
| 质检判定提交（IQC/IPQC/OQC）| 判定结果被覆盖 |
| 外协收料/发料确认 | 重复入库 |
| 设备状态变更 | 状态跳跃 |

## Security & Request Lifecycle

- 认证 Spring Security + JWT（无状态）；权限 RBAC：用户 → 角色 → 菜单/权限
- 数据权限 `@DataScope`（按部门隔离）；注解 `@PreAuthorize` + `@RequiresPermissions`/`@RequiresRoles`
- 生命周期：`Client → JwtAuthenticationTokenFilter → SecurityConfig → Controller → Service → Mapper → MySQL/Redis → AjaxResult`

### 关键依赖（pom.xml）

Spring Boot 4.0.3 · MyBatis Spring Boot 4.0.1 · Druid 1.2.28 · PageHelper 2.1.1 · JWT 0.9.1 · Fastjson 2.0.61 · Apache POI 4.1.2 · Velocity 2.3（代码生成）· OSHI 6.10.0

## RuoYi 框架工具（禁止重复造轮子）

> `ruoyi-common` 已封装大量工具/注解，**优先使用，禁止另引三方库**。

**工具类**（`com.ruoyi.common.utils`）：`StringUtils`、`DateUtils`、`SecurityUtils`（取当前用户/角色/权限/`factoryId`）、`ServletUtils`、`DictUtils`（带缓存）、`ExceptionUtil`、`Arith`（精确浮点）、`MessageUtils`（i18n）、`LogUtils`、`PageUtils`。

**注解**（`com.ruoyi.common.annotation`）：`@Log`（操作日志入库）、`@DataScope`（数据权限）、`@Excel`（自动导入导出）、`@RepeatSubmit`（防重复提交，仅 HTTP 幂等非并发锁）、`@RateLimiter`（Redis 令牌桶）、`@Anonymous`（跳过认证）、`@Sensitive`（脱敏）。

**基类**：`BaseEntity`（createBy/createTime/updateBy/updateTime/remark）、`BaseController`（`startPage()`/`getDataTable()`/`success()`/`toAjax()`/`error()`）、`TreeEntity`（parentId/ancestors）。

## 分层开发模板

> 标准 CRUD 用 **RuoYi 代码生成器**（`ruoyi-generator`）一键生成，零手写。仅记关键规则：

- **Controller**：继承 `BaseController`，方法加 `@PreAuthorize`，用 `startPage()`/`getDataTable()`/`success()`/`toAjax()`
- **Service**：`@Transactional(rollbackFor = Exception.class)`，操作人取 `SecurityUtils.getUsername()`。**需加锁的方法禁止 `@Transactional`，改用 `TransactionTemplate`**（见上）
- **Mapper XML**：**所有 SELECT/UPDATE/DELETE 的 WHERE 必须带 `factory_id = #{factoryId}`**（拦截器注入参数值，SQL 需有 `<if test>`）

### DTO/Entity/VO

代码生成器不分 DTO/VO，**标准 CRUD 用 Entity 贯穿，不建 DTO/VO**。仅手写复杂业务时建 VO/Body（多表组合查询结果、请求体与 Entity 差异大、敏感字段隐藏）。返回字段 ≥80% 匹配 Entity 就直接用 Entity。转换优先用静态工厂 `XxxVO.from(entity)`，禁止逐字段 setter 和 `BeanUtils.copyProperties`。

### 异常与日志

Service 抛 `ServiceException("描述")`，不吞异常；Controller 由 `GlobalExceptionHandler` 统一捕获。日志 `info` 业务节点 → `warn` 可恢复 → `error` 不可恢复；占位符 + 异常末参：`log.error("入库失败, recptCode={}", code, ex)`。

## 代码风格

- import 顺序：`com.qixiaoxia` → `com.ruoyi` → 三方库 → `java.*` → `jakarta.*`
- Controller 放 `ruoyi-admin/module/controller/{domain}/`，Service/Mapper 放业务模块
- 类名 PascalCase，方法/变量 camelCase，常量 UPPER_SNAKE_CASE
- `@RequestMapping` 路径 kebab-case（`/wm/item-receipt`）
- **函数 ≤ 50 行**（模板/配置除外）

## Testing

**单元测试**（JUnit5 + Mockito）：`@ExtendWith(MockitoExtension.class)` + `@Mock`/`@InjectMocks`，不启动 Spring。命名 `should_{expected}_when_{condition}`，`@DisplayName` 中文。覆盖正常/边界/异常。

**集成测试**（SpringBootTest + Testcontainers）：继承 `BaseIntegrationTest`，`@SpringBootTest(webEnvironment=RANDOM_PORT)`，`RestTemplate` 发请求，断言 HTTP + DB 最终状态。前置 `docker compose up -d redis`。

> ⚠️ **改 `ruoyi-system` 的代码/XML 后必须先 `mvn install -pl ruoyi-system -am -DskipTests`，否则测试从 `~/.m2` 加载旧 jar**。

```bash
mvn install -pl ruoyi-system -am -DskipTests                              # 改依赖模块后必跑
mvn test                                                                  # 单元
mvn test -pl ruoyi-system                                                 # 特定模块
mvn verify                                                                # 单元 + 集成
mvn failsafe:integration-test -pl ruoyi-admin -Dit.test="XxxControllerIT" # 单跑集成
```

调试 SQL：临时在 `application-test.yml` 设 `logging.level.com.ruoyi.system.mapper.mes: debug`。覆盖率底线：Service 层 ≥80%；生成器 CRUD 可不测，手写业务必须测。

## 依赖管理

Maven BOM（父 POM `<dependencyManagement>`）统一版本。新增依赖优先级：查 `ruoyi-common` → 用 Spring Boot 自带 → 最后才引三方库。子模块不得直接写 version，必须父 POM 声明。

## 新增 MES 模块

1. 根 `pom.xml` 加 `<module>qixiaoxia-{domain}</module>`
2. `ruoyi-admin/pom.xml` 加依赖
3. Controller 放 `ruoyi-admin/.../com/qixiaoxia/mes/{domain}/controller/`，Service/Mapper 放新模块
4. 表建 `sql/`，命名 `qxx_{domain}_*.sql`（Flyway 迁移见根 AGENTS.md）
