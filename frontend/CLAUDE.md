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

- **开发服务器代理**: 默认代理 `/dev-api` → `http://localhost:8080`（后端），配置在 `vite.config.ts`
- **环境文件**: `.env.development` / `.env.staging` / `.env.production`
- **后端接口地址**: 修改 `vite.config.ts` 中的 `baseUrl` 或环境变量 `VITE_APP_BASE_API`

## Project Structure

```
frontend/
├── vite.config.ts             # Vite 配置 (代理、插件、构建)
├── tsconfig.json              # TypeScript 配置
├── package.json               # 依赖 & 脚本
├── index.html                 # HTML 入口
├── vite/                      # Vite 插件配置
│   ├── plugins/index.ts       # 插件注册中心
│   ├── auto-import.ts         # Element Plus 自动导入
│   ├── compression.ts         # Gzip 压缩
│   ├── setup-extend.ts        # Vue setup 组件名扩展
│   └── svg-icon.ts            # SVG 图标加载
├── public/                    # 静态资源 (不经过构建)
└── src/
    ├── main.ts                # 应用入口
    ├── App.vue                # 根组件
    ├── api/                   # API 接口层
    │   ├── login.ts           # 登录认证
    │   ├── menu.ts            # 菜单路由
    │   ├── system/            # 系统管理 API
    │   ├── monitor/           # 监控 API
    │   └── tool/              # 工具 API
    ├── assets/                # 静态资源 (经构建处理)
    │   ├── icons/             # SVG 图标
    │   └── styles/            # 全局样式
    ├── components/            # 公共组件
    │   ├── Icon/              # SVG Icon 组件
    │   ├── Pagination/        # 分页组件
    │   ├── RightToolbar/      # 右侧工具栏
    │   ├── Editor/            # 富文本编辑器
    │   └── ...                # 更多复用组件
    ├── directive/             # 自定义指令
    │   ├── permission/        # v-hasPermi 权限指令
    │   └── common/            # 通用指令
    ├── layout/                # 布局组件
    │   ├── index.vue          # 主布局
    │   └── components/        # Navbar, Sidebar, TagsView, AppMain, Settings
    ├── plugins/               # JS 插件 (auth, cache, download, modal, tab)
    ├── router/                # Vue Router 配置
    │   └── index.ts           # constantRoutes (公开) + 动态路由
    ├── store/                 # Pinia 状态管理
    │   └── modules/
    │       ├── app.ts         # UI 状态 (sidebar, device, size)
    │       ├── user.ts        # 用户/token/角色/权限
    │       ├── permission.ts  # 动态路由生成
    │       ├── settings.ts    # 布局配置
    │       ├── tagsView.ts    # 标签页状态
    │       └── dict.ts        # 字典数据缓存
    ├── types/                 # TypeScript 类型定义
    ├── utils/                 # 工具函数
    │   ├── request.ts         # Axios 实例 (拦截器、token、错误处理)
    │   ├── auth.ts            # Token 存取 (js-cookie)
    │   ├── permission.ts      # 权限检查辅助函数
    │   ├── ruoyi.ts           # 框架工具 (日期、树、字典等)
    │   └── validate.ts        # 表单校验规则
    └── views/                 # 页面组件
        ├── index.vue          # 首页/仪表盘
        ├── login.vue          # 登录页
        ├── register.vue       # 注册页
        ├── system/            # 系统管理页面
        ├── monitor/           # 监控页面
        └── tool/              # 工具页面
```

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
  → Proxy /dev-api → localhost:8080 (dev only)
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

### MES Page Directory

所有 MES 业务页面放在 `src/views/mes/` 下，按领域分子目录：

```
src/views/mes/
├── md/       # 基础数据 (物料/BOM/工艺路线)
├── wm/       # 仓储管理 (入库/出库/库存)
├── pro/      # 生产管理 (工单/报工)
├── qc/       # 质量管理 (检验/不良品)
└── dv/       # 设备管理
```

### MES API Directory

```
src/api/mes/
├── md/       # 基础数据 API
├── wm/       # 仓储管理 API
├── pro/      # 生产管理 API
├── qc/       # 质量管理 API
└── dv/       # 设备管理 API
```

### Permission Naming Convention

格式: `mes:{domain}:{entity}:{action}`
- 示例: `mes:wm:itemrecpt:list`, `mes:pro:workorder:edit`, `mes:qc:inspection:add`

### Custom Components

MES 业务专用组件（下拉选择器等）放在 `src/components/mes/` 下。

## Testing

### Component Tests (Vitest + @vue/test-utils)

测试文件位于 `__tests__/` 目录，与组件同目录。示例：`src/views/__tests__/login.spec.ts`

- `vi.mock('@/api/xxx')` mock 所有 API 调用
- mock Vue Router（`useRouter`/`useRoute`）和 Pinia store
- 使用 `data-testid` 属性定位元素
- 测试覆盖：渲染、用户交互、错误状态、权限控制

### data-testid 约定

在模板中为可交互元素添加 `data-testid` 属性：

```html
<el-input v-model="form.name" data-testid="config-name-input" />
<el-button type="primary" data-testid="config-save-btn" @click="submit">保存</el-button>
```

选择器优先级：`data-testid` > `aria-label` > CSS class

### Running Tests

```bash
# 运行全部组件测试
npm test

# Watch 模式（开发时使用）
npm run test:watch

# 特定测试文件
npx vitest run src/views/__tests__/login.spec.ts
```

### Setup

- `vitest.config.ts` — 测试配置（jsdom 环境、路径别名）
- `src/__tests__/setup.ts` — 全局 setup（stub 浏览器 API、Element Plus 插件）
- `src/__tests__/helpers/` — 测试辅助工具（mockRouter、mockStore）

## 代码风格与静态检查

### 当前状态

项目暂未配置 ESLint，以下为 AI 生成代码的编码约定。后续应添加 ESLint + Prettier 自动化检查。

### 编码约定

- `<script setup lang="ts">` 必须（新的 `<script setup>` 写法 + TypeScript）
- 组件名 PascalCase，文件名 kebab-case
- import 顺序：`vue` → `vue-router` → `pinia` → `element-plus` → `@/` → `./`
- 禁止 `any` 类型（除非确实无法推断的第三方库）
- Props/Emits 必须显式声明类型
- CSS 使用 `scoped` + SCSS 变量

### 计划添加的 lint 工具

- ESLint（`@typescript-eslint` + `eslint-plugin-vue`）
- Prettier（统一格式化）
- AI 生成代码后运行 `npx eslint --fix src/`

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

所有 MES 业务页面遵循统一模板：

```vue
<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { XxxVO, XxxQuery } from '@/types/mes/{domain}'
import { listXxx, getXxx, addXxx, updateXxx, delXxx } from '@/api/mes/{domain}/xxx'

// 搜索表单
const queryParams = reactive<XxxQuery>({ pageNum: 1, pageSize: 10 })

// 表格数据
const tableData = ref<XxxVO[]>([])
const total = ref(0)
const loading = ref(false)

// 弹窗控制
const open = ref(false)
const title = ref('')
const formRef = ref()
const form = reactive<XxxVO>({})

// 查询列表
function getList() {
  loading.value = true
  listXxx(queryParams).then(res => {
    tableData.value = res.rows
    total.value = res.total
    loading.value = false
  })
}

// 新增/编辑弹窗
function handleAdd() { /* open=true, title='新增', form={} */ }
function handleUpdate(row: XxxVO) { /* open=true, title='修改', form={...row} */ }

// 提交
function submitForm() {
  formRef.value?.validate((valid: boolean) => {
    if (!valid) return
    const api = form.id ? updateXxx : addXxx
    api(form).then(() => { ElMessage.success('操作成功'); getList(); open.value = false })
  })
}

// 删除
function handleDelete(ids: number[]) {
  ElMessageBox.confirm('确认删除？', '警告', { type: 'warning' })
    .then(() => delXxx(ids)).then(() => { ElMessage.success('删除成功'); getList() })
}

onMounted(() => getList())
</script>
```

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
