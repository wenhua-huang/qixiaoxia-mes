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
- **默认端口**: 8080 (配置在 `ruoyi-admin/src/main/resources/application.yml`)
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

```
backend/
├── pom.xml                    # 父 POM，依赖管理 (SB 4.0.3, MyBatis 4.0.1, Druid 1.2.28)
├── ruoyi-admin/               # 🔑 Web 入口模块 — Controller 层
│   └── src/main/java/com/ruoyi/
│       ├── RuoYiApplication.java       # Spring Boot 启动类
│       └── web/controller/             # REST Controllers
│           ├── system/                 # 系统管理 (用户/角色/菜单/部门/岗位/字典/配置/通知)
│           └── monitor/                # 监控 (在线用户/操作日志/登录日志/缓存/服务器)
├── ruoyi-common/               # 通用模块 — 工具类、异常、枚举、注解、过滤器
├── ruoyi-framework/            # 框架模块 — Security、JWT、AOP、数据权限、配置
├── ruoyi-system/               # 系统业务模块 — Service + Mapper 层
├── ruoyi-generator/            # 代码生成器模块 — Velocity 模板引擎
├── ruoyi-quartz/               # 定时任务模块 — Quartz 调度
├── ruoyi-ui/                   # 前端 (Vue 2 版，本项目不使用此目录，前端在 ../frontend/)
├── sql/                        # 数据库初始化脚本
│   ├── ry_20260417.sql         # 核心表结构 + 初始化数据
│   └── quartz.sql              # 定时任务表
└── bin/                        # 部署脚本
```

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
- **注解驱动**: `@PreAuthorize` + 自定义 `@RequiresPermissions` / `@RequiresRoles`

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

### MES Domain Modules (规划)

| 模块 | 包名 | 功能 |
|------|------|------|
| 基础数据 (md) | `com.qixiaoxia.mes.md` | 物料/工艺路线/BOM/工作中心 |
| 仓储管理 (wm) | `com.qixiaoxia.mes.wm` | 入库/出库/库存/盘点 |
| 生产管理 (pro) | `com.qixiaoxia.mes.pro` | 工单/报工/流转 |
| 质量管理 (qc) | `com.qixiaoxia.mes.qc` | 检验/缺陷/不良品 |
| 设备管理 (dv) | `com.qixiaoxia.mes.dv` | 设备台账/保养/维修 |

### Adding a New MES Module

1. 在根 `pom.xml` 中添加 `<module>qixiaoxia-{domain}</module>`
2. 在 `ruoyi-admin/pom.xml` 中添加依赖
3. Controller 放在 `ruoyi-admin/src/main/java/com/qixiaoxia/mes/{domain}/controller/`
4. Service/Mapper 放在新模块中
5. 数据库表建在 `sql/` 目录下，命名 `qxx_{domain}_*.sql`

## Testing

### Unit Tests (JUnit 5 + Mockito)

位于各模块 `src/test/java/` 下。示例：`ruoyi-system/src/test/java/.../SysConfigServiceImplTest.java`

- `@ExtendWith(MockitoExtension.class)` — 不启动 Spring 容器
- `@Mock` 所有依赖（Mapper、RedisCache 等），`@InjectMocks` 待测类
- 命名：`should_{expected}_when_{condition}`
- `@DisplayName` 中文描述
- 覆盖：正常路径、边界值（null/空/零）、异常路径

### Integration Tests (SpringBootTest + Testcontainers)

位于 `ruoyi-admin/src/test/java/`。示例：`SysConfigControllerIT`

- 继承 `BaseIntegrationTest`（Testcontainers MySQL 8.0.33）
- `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `@TestInstance(PER_CLASS)`
- 数据源由 `@DynamicPropertySource` 动态注入
- Schema 通过 `spring.sql.init` 从 `../sql/ry_20260417.sql` 自动初始化
- 使用 `RestTemplate` 发 HTTP 请求
- `@BeforeEach` 清理测试数据
- 断言 HTTP 响应 + 数据库最终状态（通过 Mapper）

### Prerequisites for Integration Tests

```bash
# 必须启动本地 Redis
docker compose up -d redis
```

### Running Tests

```bash
# 单元测试（快速）
mvn test

# 特定模块
mvn test -pl ruoyi-system

# 单元 + 集成测试
mvn verify

# 特定集成测试
mvn test -pl ruoyi-admin -Dtest="*IT" -Dsurefire.failIfNoSpecifiedTests=false
```

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

### 推荐静态检查

待配置 `maven-checkstyle-plugin`，AI 生成代码后先 `mvn compile` 验证。

## DTO/Entity/VO 分层规范

### 核心原则

> RuoYi 代码生成器不分 DTO/VO，模板只有 `domain.java.vm`（Entity）。**生成器产出的 CRUD 沿用 RuoYi 模式（Entity 一把梭），不加 DTO/VO。**

### 什么时候需要 VO/Body（仅手写复杂业务时）

| 场景 | 用什么 | 触发条件 |
|------|--------|----------|
| 标准 CRUD（生成器） | Entity 贯穿 | 默认，不建 DTO/VO |
| 多表组合查询结果 | VO | 返回字段来自 ≥2 张表，无现成 Entity |
| 请求体字段与 Entity 差异大 | Body | 前端提交字段 ≠ Entity 字段（含临时字段、验证码） |
| 敏感字段需隐藏 | VO | Entity 含 password 等不应暴露的字段 |

### 什么时候不需要

- 查询参数多几个筛选条件 → 直接用 Entity，别建 Query
- 返回字段 = Entity 的 80% 以上 → 直接用 Entity，别建 VO
- 「以后可能会变」→ 别提前建，等实际需要再说

### 转换方式（仅手写 VO/Body 时）

- **优先用静态工厂方法**：`XxxVO.from(entity)` — 零依赖
- 字段 > 10 或嵌套多时考虑 MapStruct（需先加依赖）
- **禁止**：手写逐字段 setter、`BeanUtils.copyProperties`

## 分层开发模板

### Controller（`ruoyi-admin` 的 controller 目录）

```java
@RestController
@RequestMapping("/mes/{domain}/{entity}")
public class XxxController extends BaseController {

    @Autowired
    private IXxxService xxxService;

    @PreAuthorize("@ss.hasPermi('mes:{domain}:{entity}:list')")
    @GetMapping("/list")
    public TableDataInfo list(Xxx xxx) {
        startPage();
        List<Xxx> list = xxxService.selectXxxList(xxx);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:{domain}:{entity}:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(xxxService.selectXxxById(id));
    }

    @PreAuthorize("@ss.hasPermi('mes:{domain}:{entity}:add')")
    @Log(title = "新增XXX", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Xxx xxx) {
        xxx.setCreateBy(SecurityUtils.getUsername());
        return toAjax(xxxService.insertXxx(xxx));
    }
}
```

### Service（业务模块中）

```java
@Service
public class XxxServiceImpl implements IXxxService {

    @Autowired
    private XxxMapper xxxMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertXxx(Xxx xxx) {
        // 业务校验 → Mapper
        return xxxMapper.insertXxx(xxx);
    }
}
```

### Mapper XML

```java
// Java 接口
public interface XxxMapper {
    List<Xxx> selectXxxList(Xxx xxx);
    Xxx selectXxxById(Long id);
    int insertXxx(Xxx xxx);
    int updateXxx(Xxx xxx);
}
```

```xml
<!-- XML 中 SELECT 必须带 factory_id -->
<select id="selectXxxList" resultType="Xxx">
    SELECT * FROM qxx_{domain}_{entity}
    <where>
        AND factory_id = #{factoryId}
        <if test="name != null and name != ''">AND name LIKE CONCAT('%', #{name}, '%')</if>
    </where>
</select>
```

## 并发控制 & 数据一致性

> ⚠️ RuoYi 框架**无内置分布式锁/乐观锁工具**。`@RepeatSubmit` 只防重复提交（HTTP 幂等），`@RateLimiter` 只做限流。以下为设计规范，**具体工具类待后续完善**。

### 必须加锁的 MES 场景

| 场景 | 并发冲突 | 后果 |
|------|---------|------|
| 库存扣减（领料/入库/盘点） | 两人同时扣减同一批次库存 | 库存超扣/负数 |
| 工单状态变更（开工/暂停/完工/关闭） | 两人同时操作同一工单 | 状态机混乱 |
| 质检判定提交（IQC/IPQC/OQC） | 两人同时提交同一检验单 | 判定结果被覆盖 |
| 外协收料/发料确认 | 两人同时确认同一外协单 | 重复入库 |
| 设备状态变更（维修→正常、报废） | 两人同时更新同一设备 | 状态跳跃 |

### 推荐方案

| 方案 | 适用场景 | 实现思路 |
|------|---------|---------|
| **乐观锁（推荐首选）** | 更新频率低、冲突概率小 | 表加 `version` 字段，UPDATE 时 `WHERE version = ?`，不匹配则重试 |
| **Redis 分布式锁** | 更新频率高、需跨服务锁定 | `SET lock:key value NX EX 30`，操作完释放 |
| **SELECT FOR UPDATE** | 事务内必须保证读取一致性 | Mapper XML 中手写，事务提交后自动释放 |

### 乐观锁模板（待工具化）

```sql
-- 1. DDL 加 version 字段
ALTER TABLE qxx_wm_item_inventory ADD COLUMN version INT DEFAULT 0 NOT NULL COMMENT '乐观锁版本号';

-- 2. UPDATE 带 version + 条件约束
UPDATE qxx_wm_item_inventory
SET quantity = quantity - #{quantity}, version = version + 1
WHERE id = #{id} AND version = #{version} AND quantity >= #{quantity};
-- affected rows = 0 → 版本冲突，重试或返回错误
```

### AI 开发并发检查

- 涉及上述 5 类场景的 UPDATE → 必须加锁措施
- Code Review 必须检查并发安全

## 异常处理与日志规范

### 异常处理

- Service 层抛出 `ServiceException("业务描述")`，不吞异常
- Controller 层由 `GlobalExceptionHandler` 统一捕获，不手写 try-catch
- 不要 `catch (Exception e) { e.printStackTrace(); return null; }`

### 日志级别

| 级别 | 使用场景 |
|------|---------|
| `info` | 业务流程节点（开工、完工、入库、出库） |
| `warn` | 可恢复异常（库存不足、状态不允许、参数越界） |
| `error` | 不可恢复异常（数据库连接失败、外部系统调用失败） |
| `debug` | 调试信息（SQL 参数、中间结果），生产关闭 |

### 日志格式

```java
log.info("工单开工, workorderId={}, operator={}", workorderId, SecurityUtils.getUsername());
log.warn("库存不足, itemId={}, required={}, available={}", itemId, qty, stock);
log.error("入库失败, recptCode={}", recptCode, exception);  // 异常作最后一个参数
```

## 测试覆盖率底线

- Service 层单元测试覆盖率 ≥ 80%
- Mapper 集成测试覆盖核心 SQL（至少含 factory_id 过滤、分页查询）
- 生成器的标准 CRUD 可不测（框架保证），手写业务逻辑必须测

## 环境与依赖管理

- Maven BOM（父 POM `<dependencyManagement>`）统一管理版本
- 新增依赖：优先查 `ruoyi-common` 是否已有工具 → 其次用 Spring Boot 自带 → 最后才引入三方库
- 引入新依赖需在父 POM 声明版本，不得在子模块直接写 version
