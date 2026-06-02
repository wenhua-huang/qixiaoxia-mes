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
