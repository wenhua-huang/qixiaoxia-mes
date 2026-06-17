# CLAUDE.md

This file provides guidance to Claude Code when working with code in this repository.

## Project Overview

RuoYi-Vue3-TypeScript frontend (v3.9.2), the PC web frontend for the **企小侠文化纸盒MES系统** (Qixiaoxia Cultural Paper Box MES). Built with Vue 3 + TypeScript + Element Plus + Vite.

**Source**: Cloned from https://gitcode.com/yangzongzhuan/RuoYi-Vue3 (typescript 分支，官方源)。

## Build & Run

**本机环境**: Node 18+ (推荐 18.20 LTS), yarn/pnpm/npm

```bash
# 安装依赖 (推荐 yarn)
yarn install
# 或
npm install

# 开发服务器 (默认端口 80)
yarn dev

# 生产构建
yarn build:prod

# 预发布构建
yarn build:stage

# 预览构建结果
yarn preview

# TypeScript 类型检查
npx vue-tsc --noEmit
```

- **开发服务器代理**: 默认代理 `/dev-api` → `http://localhost:8081`（后端），配置在 `vite.config.ts`
- **环境文件**: `.env.development` / `.env.staging` / `.env.production`
- **后端接口地址**: 修改 `vite.config.ts` 中的 `baseUrl` 或环境变量 `VITE_APP_BASE_API`

## Project Structure

| 目录 | 职责 |
|------|------|
| `vite.config.ts` | Vite 配置（代理 `/dev-api` → `localhost:8081`、插件、构建） |
| `src/main.ts` | 应用入口 |
| `src/api/` | API 接口层（`login.ts`、`menu.ts`、`system/`、`mes/`） |
| `src/assets/` | 静态资源（icons/、styles/） |
| `src/components/` | 公共组件（`Icon/`、`Pagination/`、`RightToolbar/`、`mes/` 业务组件） |
| `src/directive/` | 自定义指令（`v-hasPermi`、`v-hasRole`） |
| `src/layout/` | 布局（`index.vue` + Navbar/Sidebar/TagsView/AppMain） |
| `src/plugins/` | JS 插件（auth、cache、download、modal、tab） |
| `src/router/` | Vue Router（`constantRoutes` 公开 + 动态路由） |
| `src/store/modules/` | Pinia 状态管理（app、user、permission、settings、tagsView、dict） |
| `src/types/` | TypeScript 类型定义 |
| `src/utils/` | 工具函数（`request.ts` Axios、`auth.ts` Token、`permission.ts` 权限检查） |
| `src/views/` | 页面组件（`login.vue`、`system/`、`mes/`、`monitor/`） |
| `vite/plugins/` | Vite 插件（Element Plus 自动导入、Gzip、SVG 图标） |

## Architecture & Key Patterns

### Technology Stack

| 类别 | 技术 | 版本 |
|------|------|------|
| 框架 | Vue 3 | 3.5.26 |
| 语言 | TypeScript | 5.6.3 |
| 构建 | Vite | 6.4.1 |
| UI 库 | Element Plus | 2.13.1 |
| 状态管理 | Pinia | 3.0.4 |
| 路由 | Vue Router | 4.6.4 |
| HTTP | Axios | 1.13.2 |
| 图表 | ECharts | 5.6.0 |
| 富文本 | Quill (via @vueup/vue-quill) | 1.2.0 |

### Dynamic Route Loading

Routes come from the backend. On login:
1. `permission.ts` store calls `GenerateRoutes` action
2. Fetches menu tree from `GET /getRouters` (returns Vue Router-compatible JSON)
3. `filterAsyncRouter()` converts string component paths to actual Vue components via dynamic `import()`
4. Routes merged: `constantRoutes` (public) + dynamic routes
5. `router.addRoute()` installs them at runtime

**To add a new page**:
1. Backend menu entry in `sys_menu` table with component path string
2. `.vue` file at matching path under `src/views/`
3. API module file under `src/api/`
4. Correct permission string (e.g., `mes:wm:item:list`)

### Permission System (Three Layers)

| Layer | Mechanism | Example |
|-------|-----------|---------|
| Route | `permissions`/`roles` meta on route | `permissions: ['mes:wm:item:list']` |
| Directive | `v-hasPermi` / `v-hasRole` | `v-hasPermi="['mes:wm:item:edit']"` |
| Method | `checkPermi()` in component | `v-if="checkPermi('...')"` |

Admin user (userId=1) has `*:*:*` wildcard — all checks pass.

### HTTP Request Lifecycle

```
Component calls API → Axios instance (utils/request.ts)
  → Request interceptor:
      1. Attach Authorization: Bearer <token>
      2. Convert GET params to query string
      3. Check duplicate POST/PUT (sessionStorage, 1s window)
  → Proxy /dev-api → localhost:8081 (dev only)
  → Response interceptor:
      1. code=200 → return res.data
      2. code=401 → re-login dialog
      3. code=500 → error toast
      4. Network error → "后端接口连接异常" toast
```

### State Management (Pinia)

```
app         → UI state: sidebar, device type, element size
user        → Auth state: token, user info, roles[], permissions[]
permission  → Dynamic routes: routes[], sidebarRouters[], topbarRouters[]
settings    → Layout config: theme, tagsView, fixedHeader, sidebarLogo
tagsView    → Open page tabs: visitedViews[], cachedViews[]
dict        → Dictionary data cache
```

### TypeScript Conventions

- All new `.vue` files should use `<script setup lang="ts">`
- API response types defined in `src/types/`
- Store modules must have proper type annotations on state/getters/actions
- Use `vue-tsc --noEmit` for type checking before commits

## Customization for 企小侠 MES

### MES 目录约定

- 页面: `src/views/mes/{md,wm,pro,qc,dv}/{entity}/`
- API: `src/api/mes/{md,wm,pro,qc,dv}/{entity}.ts`
- 业务组件: `src/components/mes/`（如 `ItemSelect.vue`、`WarehouseSelect.vue`）
- 权限格式: `mes:{domain}:{entity}:{action}`（如 `mes:wm:itemrecpt:list`）

## Testing

Vitest + @vue/test-utils，测试文件放 `__tests__/` 与组件同目录。`vi.mock('@/api/xxx')` mock API，mock Vue Router/Pinia store，用 `data-testid` 定位元素（优先级 > `aria-label` > CSS class）。

```bash
npm test                                    # 全量
npm run test:watch                          # watch 模式
npx vitest run src/views/__tests__/login.spec.ts  # 单文件
```

配置文件：`vitest.config.ts`（jsdom）、`src/__tests__/setup.ts`（全局 stub）、`src/__tests__/helpers/`（mockRouter/mockStore）。

## 代码风格

- `<script setup lang="ts">` 必须，组件名 PascalCase，文件名 kebab-case
- import 顺序：`vue` → `vue-router` → `pinia` → `element-plus` → `@/` → `./`
- 禁止 `any` 类型，Props/Emits 必须显式类型，CSS `scoped` + SCSS
- 暂未配置 ESLint/Prettier，AI 生成后跑 `npx vue-tsc --noEmit`

## 已有工具使用规范

> ⚠️ 项目已有以下工具模块，**优先使用，不要手写替代或引入新库**。

| 工具 | 位置 | 用途 | 核心 API |
|------|------|------|---------|
| `request` | `@/utils/request` | Axios 封装 | 自动带 token、防重复提交、统一错误处理 |
| `ruoyi` | `@/utils/ruoyi` | 框架工具 | `parseTime`、`handleTree`、`selectDictLabel`、`download` |
| `auth` | `@/utils/auth` | Token 存取 | `getToken()`、`setToken()`、`removeToken()` |
| `permission` | `@/utils/permission` | 权限检查 | `checkPermi(perm)`、`checkRole(role)` |
| `validate` | `@/utils/validate` | 表单校验 | 手机号、邮箱、URL 等常用规则 |
| `dict` | `@/utils/dict` | 字典缓存 | 字典数据获取 |
| `scroll-to` | `@/utils/scroll-to` | 页面滚动 | 平滑滚动到指定位置 |

## 标准 CRUD 页面模板

> 所有 MES 业务页面遵循统一模式。用 RuoYi 代码生成器产出 → 手工补充业务逻辑。

**关键模式**：`queryParams`（`reactive<XxxQuery>`）→ `getList()`（`res.rows`/`res.total`）→ 弹窗 `handleAdd`/`handleUpdate` → `submitForm`（`form.id ? updateXxx : addXxx`）→ `handleDelete`（`ElMessageBox.confirm`）。详情参考 `src/views/mes/` 已有页面。

## Composable 与组件规范

### Composable（use*）

- 放在 `src/hooks/useXxx.ts`
- 命名 `useXxx`，返回 `{ state, method }` 对象（不用 `toRefs` 自动解构）
- 单一职责：一个 Composable 只管理一个关注点
- 复用 ≥3 次 → 抽 Composable

### 组件抽取标准

| 条件 | 动作 |
|------|------|
| 同一 UI 片段出现 ≥3 次 | 抽取为独立组件 |
| 组件 > 300 行 | 拆分子组件 |
| 业务逻辑 > 100 行 | 抽到 Composable |
| 仅 1-2 个页面用 | 放在当前目录 `components/` |
| ≥3 个页面用 | 放到 `src/components/`（全局） |

### MES 业务组件

放在 `src/components/mes/`，命名 PascalCase。如物料选择器 `ItemSelect.vue`、仓库选择器 `WarehouseSelect.vue`。

## 测试覆盖率底线

- 关键页面（登录、CRUD 核心流程）组件测试覆盖率 ≥ 60%
- 生成器生成的页面可不测（框架内），手写业务组件必须测
- 测试文件放在 `__tests__/` 与组件同目录

## 前端自检清单（AI 高频遗漏项）

> ⚠️ **前端页面完成后必须逐项检查，这是最容易出低级错误的地方。**

1. **`useDict` 字典引用** — `proxy.useDict(...)` 返回的 dicts.xxx 在模板渲染时可能为 `undefined`（异步加载）。**必须**：
   - 用 `v-if="dicts.xxx"` 包裹依赖字典的 DOM，或直接硬编码固定选项
   - 确认字典名称在 `sys_dict_data` 表中存在

2. **枚举字段中文显示（禁止硬编码映射）** — 英文枚举值（`PRINT`、`IDLE`）**必须**通过后端字典系统翻译。后端在 `sys_dict_type` + `sys_dict_data` 中注册字典（命名 `mes_{模块}_{字段}`），前端用 `useDict('mes_xxx')` 加载 + `<dict-tag :options="xxx" :value="row.field" />` 渲染。**严禁** `Record<string, string>` 手写映射或 `<el-option>` 硬编码。

3. **弹窗初始状态** — `v-if="form.id"` 包裹整个表单会导致新增时弹窗为空。**必须**：
   - header 表单始终渲染（不受 `v-if` 限制），line/detail tab 才按需显示

4. **操作栏按钮样式** — 统一用 `el-button link type="primary" size="small"`，列宽 ≥180px，`class-name="small-padding fixed-width"`

5. **Element Plus Icon** — 不要用 Element UI 的图标名（如 `el-icon-edit`），用 Element Plus 的 `Edit` / `Delete` 等。不确定图标是否存在时直接去掉 `icon` 属性，纯文字按钮

6. **路由导航** — `router.push({ path: '/mes/wm/xxx', query: { id } })` 前确认目标路径的菜单 `visible='0'`（菜单隐藏则路由未注册，push 失效）

7. **确认弹窗** — `proxy.$modal.confirm(...)` 必须加 `.catch(() => {})` 处理取消操作，避免 unhandled rejection

8. **Label 间距** — 所有 dialog 表单统一加 `<style scoped> :deep(.el-form-item__label) { padding-right: 16px !important; } </style>`，避免 label 文字和输入框紧贴

9. **自动生成开关** — 用 `<el-switch size="small" />` + 独立 `<span>自动生成</span>` 标签，**不用** `active-text` 属性（会竖向排列）

10. **日期格式** — `el-date-picker` 的 `value-format` **必须**为 `YYYY-MM-DD HH:mm:ss`，不能是 `YYYY-MM-DD`（后端 Jackson 解析报错）

## 前端修改后自验证（AI 必须执行，禁止直接让用户检查）

> ⚠️ **修改任何 Vue/TS 文件后，必须自己先验证，不能丢给用户去试。**

1. **编译验证** — `npx vue-tsc --noEmit` 确保 0 类型错误
2. **Vite 热更新验证** — `curl -s http://localhost:80/src/views/.../index.vue | grep "关键新增代码"` 确认 Vite 提供的是修改后的文件
3. **API 数据流验证** — 用 `curl`/`python3` 调后端 API，模拟前端操作流程，确认数据结构匹配
4. **日期格式验证** — 每次改 `el-date-picker` 后，**必须**用 curl 模拟提交确认 `YYYY-MM-DD HH:mm:ss` 格式后端能解析
5. **仅前 4 步全部通过后**，才让用户刷新浏览器验证

## 自动编码前端接入

新增 MES 实体**必须**含自动编码功能。4 处修改：

1. `import { genSerialCode } from '@/api/mes/sys/autocoderule'`
2. `const autoGenFlag = ref(false)` + `reset()` 中重置
3. 编码输入旁放 `<el-switch size="small" @change="handleAutoGenChange" />` + `<span>自动生成</span>`（**不用** `active-text`）
4. `handleAutoGenChange(flag)` — `flag ? genSerialCode('RULE_CODE').then(r => form.code = r.data) : form.code = ''`
