# AGENTS.md

本项目由 ZCode 维护。本文件是根级指南，**常驻上下文**。子模块指南、操作流程见相应 skill / 子 AGENTS.md，按需 Read。

## 子模块（动手前 Read 对应子指南）

| 目录 | 子指南 | 技术栈 |
|------|--------|--------|
| `backend/` | [`backend/AGENTS.md`](backend/AGENTS.md) | Spring Boot 4.0.3 + JDK 17 + MyBatis |
| `frontend/` | [`frontend/AGENTS.md`](frontend/AGENTS.md) | Vue 3.5 + TS 5.6 + Element Plus 2.13 + Vite 6 |
| `app/` | [`app/AGENTS.md`](app/AGENTS.md) | uni-app + Vue 3 + Pinia（移动端） |

## ⚠️ 核心约束（违反会出 bug）

- **只改本项目文件**，workspace 其他目录（ktg-mes-ui、RuoYi-Vue 等）只读
- **编辑 Vue/XML 模板前必 Read 上下文**确认标签嵌套，闭眼改 `<el-row>`/`<el-col>` 易破坏结构
- **所有 SQL WHERE 带 `factory_id`**（MyBatis 拦截器自动注入参数值，但 SQL 仍需写 `<if>` 条件）
- **所有表有 `factory_id`**，外协 8 表额外冗余 `outsource_factory_id`（vendor/workorder/task/route_process/card_process/feedback/outsource_issue/outsource_recpt）
- **Flyway 迁移内 INSERT 必须显式写 `factory_id`**（Flyway 裸 JDBC，不走拦截器）；业务代码 INSERT **别写** `factory_id`（拦截器会注入）
- **Debug 信任用户判断** — 用户指出的方向先查透，别翻无关代码（见 `bug-fix` skill）

## 命名规范

- 后端包名 `com.qixiaoxia.mes.{domain}`，模块 `qixiaoxia-{domain}`
- 数据库表 `qxx_{domain}_{entity}`；前端路由权限 `mes:{domain}:{entity}:{action}`
- 前端页面 `src/views/mes/{domain}/{entity}/`，API `src/api/mes/{domain}/{entity}.ts`

### MES 领域缩写

| md 基础数据 | wm 仓储 | pro 生产 | qc 质量 | dv 设备 | print 打印 | tm 工具 | report 报表 |

## 后端关键机制

- **FactoryIdInterceptor** — MyBatis Interceptor 自动注入 `factory_id`，所有用户（含 admin）受拦截，仅 `@SkipFactoryId` 放行
- **Redis 分布式锁** — 库存/状态变更必须用 Redisson，**先锁后事务**（`TransactionTemplate`），禁止在加锁方法上用 `@Transactional`

> 细节（拦截规则、锁标准用法、必须加锁的 5 个场景）见 [`backend/AGENTS.md`](backend/AGENTS.md)。

## 前后端职责划分

| 职责 | 后端 | 前端 |
|------|:---:|:---:|
| 数据填充（默认值、关联、计算字段）| ✅ | ❌ |
| 跨域数据聚合 | ✅ 提供本域专用接口 | ❌ 禁止直接调其他域 API |
| 表单展示与交互 | — | ✅ |

核心原则：后端拥有数据逻辑，前端无感知。如报工物料消耗，前端禁直接调 `workorderbom/listByWorkorderId` 自拼，应由后端 `GET /mes/pro/feedback/consumeDefaults/{workorderId}` 封装。

## 代码质量约束

| 约束 | 阈值 |
|------|------|
| 后端函数长度 | ≤ 50 行（模板/配置除外）|
| 前端组件长度 | ≤ 300 行，超过必须拆 |
| 重复逻辑 | ≥ 2 次 → 抽公共方法/组件 |
| 业务状态 | 用枚举，不用字符串 |
| 魔法数字 | 必须定义为常量 |
| 依赖 | 优先用框架/项目已有工具 |

## AI 行为准则

**禁止**：改非本项目文件、跳类型检查/lint 提交、顺手重构无关代码。

**必须询问**：删除/重构已有业务代码、改 DDL/API 签名/公共组件、引入新依赖。

**自主决策**：补充缺失 `factory_id`、代码生成器覆盖的标准 CRUD。

**工作流**：Plan First — 非 trivial（≥3 步或涉架构）先进 plan mode，出错立刻停重 plan。子 Agent — 搜索/探索/多文件读取用子 agent，Grep 优先于 `find \| xargs grep`。

## 生产服务器

| 项 | 值 |
|------|------|
| 连接 | `ssh qxx`（已配免密别名 → `115.29.234.204` root）|
| 项目路径 | `/var/www/qixiaoxia-mes`（服务器）/ `/Users/huangwenhua/Company/qixiaoxiao/qixiaoxia-mes`（本机）|
| 架构 | Nginx(:80) → `dist/` 静态 + `/prod-api/` → Java(:8081)；MySQL:3307/Redis:6380/MinIO:9010 均 Docker |

> ⚠️ 服务器仅 1.8GB 内存，**禁在生产跑 `vite build`**，前端本机构建后 scp。发布用 `deploy` skill。

## 调试

```bash
TOKEN=$(python3 backend/scripts/get_token.py 2>/dev/null)
curl -s http://localhost:8081/getInfo -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
# 或：python3 backend/scripts/get_token.py --test   （依赖：pip3 install requests redis）
```

## 环境版本

JDK 17+ · Maven 3.9+ · MySQL 8.0+ · Redis 7.0+ · Node 18.20 LTS

## API 契约

- 单条 `{ code:200, msg:"操作成功", data:{...} }`；分页 `{ code:200, msg:"查询成功", rows:[...], total:100 }`
- 后端新增/改接口 → 必须同步前端 API 模块，字段命名前后端一致（camelCase）

## 参考

设计文档：[数据库设计决策](docs/设计文档/数据库设计决策.md) · [多工厂外协设计](docs/设计文档/多工厂外协设计.md) · [字符集规范](docs/设计文档/数据库字符集规范.md) · [测试约定](docs/设计文档/测试约定.md)

若依 http://doc.ruoyi.vip · Element Plus https://element-plus.org/zh-CN/ · uni-app https://uniapp.dcloud.net.cn/

---

## Skills（任务触发型工作流，自动加载）

| skill | 触发 | 用途 |
|------|------|------|
| `dev-start` | "启动开发/本地启动" | 检查 Docker → 构建后端 → 启 :8081 → 启前端 → 验证链路 |
| `deploy` | "发布/上线/部署" | 本机构建前端 → 服务器拉码/编译/重启 → scp dist → reload nginx |
| `new-mes-feature` | "新增 XX 功能/模块" | 全栈 7 步：DB 检查→Flyway→代码生成→自动编码→菜单→前端页面→API |
| `db-migration` | "加字段/建表/改表/迁移" | Flyway 规则：版本号、DML 幂等、factory_id、不可改已执行文件 |
| `run-tests` | "跑测试/test/验证" | 三层测试命令、curl≠E2E 红线、6 层上线验证清单 |
| `crt-review` | "自检/code review/提交前检查" | 三轮审查→修复→测试 + 提交前 checklist |
| `bug-fix` | 报 bug、"不工作"、报错 | 禁止猜：实际观察→数据验证→数据流跟踪→方案→修复→验证 |
