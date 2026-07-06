# frontend/AGENTS.md

RuoYi-Vue3-TypeScript 前端（v3.9.2），PC web 端。Vue 3.5 + TypeScript 5.6 + Element Plus 2.13 + Vite 6.4。源 https://gitcode.com/yangzongzhuan/RuoYi-Vue3（typescript 分支）。

> 根级约束（factory_id、命名、测试策略）见 [`../AGENTS.md`](../AGENTS.md)，本文不重复。

## Build & Run

```bash
cd frontend
yarn install              # 或 npm install
yarn dev                  # :80，代理 /dev-api → http://localhost:8081
yarn build:prod           # 生产构建
npx vue-tsc --noEmit      # 类型检查（提交前必跑）
```

- 代理配置 `vite.config.ts`（`/dev-api` → `localhost:8081`）
- 环境文件 `.env.development` / `.env.staging` / `.env.production`（`VITE_APP_BASE_API`）

## Project Structure

| 目录 | 职责 |
|------|------|
| `vite.config.ts` | Vite 配置（代理、插件、构建）|
| `src/main.ts` | 应用入口 |
| `src/api/` | API 接口层（`login.ts`、`menu.ts`、`system/`、`mes/`）|
| `src/components/` | 公共组件（`Icon/`、`Pagination/`、`RightToolbar/`、`mes/` 业务组件）|
| `src/directive/` | 自定义指令（`v-hasPermi`、`v-hasRole`）|
| `src/layout/` | 布局（Navbar/Sidebar/TagsView/AppMain）|
| `src/plugins/` | JS 插件（auth、cache、download、modal、tab）|
| `src/router/` | Vue Router（constantRoutes + 动态路由）|
| `src/store/modules/` | Pinia（app/user/permission/settings/tagsView/dict）|
| `src/types/` | TypeScript 类型定义 |
| `src/utils/` | 工具（`request.ts` Axios、`auth.ts`、`permission.ts`）|
| `src/views/` | 页面（`login.vue`、`system/`、`mes/`、`monitor/`）|
| `vite/plugins/` | Vite 插件（Element Plus 自动导入、Gzip、SVG）|

## 关键机制

**动态路由**：登录后 `permission.ts` store 调 `GenerateRoutes` → `GET /getRouters` 拉菜单树 → `filterAsyncRouter()` 把字符串组件路径转真实组件（动态 `import()`）→ `router.addRoute()`。新增页面 3 步：后端 `sys_menu` 加菜单 + `.vue` 放对应路径 + API 模块 + 权限串。

**权限三层**：路由 meta（`permissions: ['mes:wm:item:list']`）→ 指令（`v-hasPermi="['mes:wm:item:edit']"`）→ 方法（`checkPermi('...')`）。admin（userId=1）有 `*:*:*` 通配，全过。

**HTTP 生命周期**（`utils/request.ts`）：请求拦截器加 Bearer token + GET 转 query + 防重复 POST（sessionStorage 1s 窗口）；代理 `/dev-api → :8081`；响应拦截器 `code=200 → return res.data`、`401 → 重登`、`500 → toast`。

**Pinia**：app（UI 状态）/ user（token/roles/permissions）/ permission（动态路由）/ settings（布局配置）/ tagsView / dict（字典缓存）。

**MES 目录约定**：页面 `src/views/mes/{md,wm,pro,qc,dv}/{entity}/`；API `src/api/mes/{domain}/{entity}.ts`；业务组件 `src/components/mes/`；权限 `mes:{domain}:{entity}:{action}`。

## TypeScript 约定

- 新 `.vue` 用 `<script setup lang="ts">`，组件名 PascalCase，文件名 kebab-case
- import 顺序：`vue` → `vue-router` → `pinia` → `element-plus` → `@/` → `./`
- 禁止 `any`，Props/Emits 显式类型，CSS `scoped` + SCSS
- 暂未配 ESLint/Prettier，AI 生成后跑 `npx vue-tsc --noEmit`

## 标准 CRUD 页面模板

用 RuoYi 代码生成器产出 → 手工补业务。关键模式：`queryParams`（`reactive<XxxQuery>`）→ `getList()`（`res.rows`/`res.total`）→ 弹窗 `handleAdd`/`handleUpdate` → `submitForm`（`form.id ? updateXxx : addXxx`）→ `handleDelete`（`ElMessageBox.confirm`）。详情参考 `src/views/mes/` 已有页面。

## RuoYi 页面排版规范（生成/改任何 MES 页面必遵循）

### 1. 布局 — el-row + el-col 栅格

表单一行 2 列，弹窗同。

```html
<!-- ✅ el-row + el-col :span="12" 两列 -->
<el-row>
  <el-col :span="12"><el-form-item label="编码"><el-input v-model="form.code" /></el-form-item></el-col>
  <el-col :span="12"><el-form-item label="名称"><el-input v-model="form.name" /></el-form-item></el-col>
</el-row>
<!-- 单字段占满行用 :span="24" -->
<!-- ❌ 裸 el-form-item 不套 row+col，宽窄不一 -->
```

### 2. 对齐

- `label-width="100px"`（搜索区 `80px`），所有页面统一
- 控件 `style="width:100%"` 撑满 col，不设固定像素
- dialog 表单统一加 `<style scoped> :deep(.el-form-item__label) { padding-right: 16px !important; } </style>`

### 3. 按钮位置

| 区域 | 规范 |
|------|------|
| 搜索区 | 查询+重置放 `<el-form>` 最后一个 `<el-form-item>` 内 |
| 表格上方 | 新增/修改/删除/导出放 `<el-row :gutter="10" class="mb8">` 内 |
| 弹窗底部 | `确 定`+`取 消` 放 `<template #footer>` 居中 |

### 4. 代码分层

- **Template 顺序**：搜索表单 → 工具栏按钮 → 数据表格 → 新增/编辑弹窗
- **Script 顺序**：import → 类型 → 响应式状态 → computed → 生命周期 → 方法（getList→reset→handleQuery→handleAdd→handleUpdate→submitForm→handleDelete）
- 缩进 HTML/TS 统一 2 空格

### 5. CSS

只用 `<style scoped>` 或行内 `style=""`，**禁止**非 scoped 全局样式。弹窗 scoped 样式用 `:deep()` 穿透。

### 6. 命名（RuoYi 习惯）

| 概念 | 命名 | 概念 | 命名 |
|------|------|------|------|
| 查询参数 | `queryParams` | 列表数据 | `xxxList` |
| 表单数据 | `form` | 加载状态 | `loading` |
| 弹窗开关 | `open` | 批量选中 | `ids`/`single`/`multiple` |
| 新增/修改/删除/提交/导出 | `handleAdd`/`handleUpdate`/`handleDelete`/`submitForm`/`handleExport` | | |

分页绑定：`v-model:page="queryParams.pageNum"` + `v-model:limit="queryParams.pageSize"`。

### 7. 输出格式

- 标签闭合完整（每个 `<el-row>` 有 `</el-row>`）
- 缩进严格对齐，同级一致
- 输出完整 `<template>` + `<script setup>` + `<style scoped>` 三段式，不输出零散片段

## 已有工具（禁止手写替代或引新库）

| 工具 | 位置 | 用途 |
|------|------|------|
| `request` | `@/utils/request` | Axios 封装（自动 token、防重提交、统一错误）|
| `ruoyi` | `@/utils/ruoyi` | `parseTime`/`handleTree`/`selectDictLabel`/`download` |
| `auth` | `@/utils/auth` | `getToken()`/`setToken()`/`removeToken()` |
| `permission` | `@/utils/permission` | `checkPermi(perm)`/`checkRole(role)` |
| `validate` | `@/utils/validate` | 表单校验规则（手机/邮箱/URL）|
| `dict` | `@/utils/dict` | 字典缓存 |

## Composable 与组件

- Composable 放 `src/hooks/useXxx.ts`，命名 `useXxx`，返回 `{ state, method }`（不用 `toRefs`）。单一职责，复用 ≥3 次才抽
- 组件抽取：同一 UI 片段 ≥3 次 / 组件 >300 行 / 业务逻辑 >100 行 → 抽。1-2 页面用放当前 `components/`，≥3 页面用放 `src/components/`
- MES 业务组件放 `src/components/mes/`，PascalCase（`ItemSelect.vue`、`WarehouseSelect.vue`）

## 自动编码前端接入（4 处修改）

新增 MES 实体**必须**含自动编码：

1. `import { genSerialCode } from '@/api/mes/sys/autocoderule'`
2. `const autoGenFlag = ref(false)` + `reset()` 中重置
3. 编码输入旁放 `<el-switch size="small" @change="handleAutoGenChange" />` + `<span>自动生成</span>`（**不用** `active-text`）
4. `handleAutoGenChange(flag)` — `flag ? genSerialCode('RULE_CODE').then(r => form.code = r.data) : form.code = ''`

## 前端自检清单（AI 高频踩坑，必逐项查）

1. **`useDict` 字典引用** — `proxy.useDict(...)` 的 `dicts.xxx` 渲染时可能 `undefined`（异步）。用 `v-if="dicts.xxx"` 包裹依赖字典的 DOM，或硬编码固定选项；确认字典名在 `sys_dict_data` 存在。
2. **枚举中文显示（禁硬编码映射）** — 英文枚举（`PRINT`/`IDLE`）**必须**走后端字典：`sys_dict_type`+`sys_dict_data` 注册（命名 `mes_{模块}_{字段}`），前端 `useDict('mes_xxx')` + `<dict-tag :options="xxx" :value="row.field" />`。**严禁** `Record<string,string>` 手写映射或 `<el-option>` 硬编码。
3. **弹窗初始状态** — `v-if="form.id"` 包裹整个表单会导致新增时弹空。header 表单始终渲染，line/detail tab 按需显示。
4. **操作栏按钮** — 统一 `el-button link type="primary" size="small"`，列宽 ≥180px，`class-name="small-padding fixed-width"`。
5. **Element Plus Icon** — 用 `Edit`/`Delete` 等，不用 Element UI 的 `el-icon-edit`。不确定图标存在就直接去掉 `icon` 属性。
6. **路由导航** — `router.push({ path:'/mes/wm/xxx', query:{id} })` 前确认目标菜单 `visible='0'`（隐藏则路由未注册，push 失效）。
7. **确认弹窗** — `proxy.$modal.confirm(...)` 必须加 `.catch(() => {})` 处理取消。
8. **Label 间距** — 所有 dialog 表单统一加 `:deep(.el-form-item__label) { padding-right: 16px !important; }`。
9. **自动生成开关** — `<el-switch size="small" />` + 独立 `<span>自动生成</span>`，**不用** `active-text`。
10. **日期格式** — `el-date-picker` 的 `value-format` **必须** `YYYY-MM-DD HH:mm:ss`，不能 `YYYY-MM-DD`（后端 Jackson 解析报错）。

## 修改后自验证（禁止丢给用户试）

1. `npx vue-tsc --noEmit` 0 类型错误
2. Vite 热更新：`curl -s http://localhost:80/src/views/.../index.vue | grep "关键新增代码"` 确认 Vite 提供的是修改后的文件
3. API 数据流：`curl`/`python3` 调后端模拟前端操作，确认数据结构匹配
4. 改 `el-date-picker` 后**必须** curl 模拟提交确认 `YYYY-MM-DD HH:mm:ss` 后端能解析
5. 前 4 步全过才让用户刷新浏览器

## Testing（Vitest）

组件测试 Vitest + @vue/test-utils，文件放 `__tests__/` 与组件同目录。**🚫 禁止调真实后端 API**（jsdom 无后端），所有 API 模块必须 mock：

```typescript
// ✅ mock API
vi.mock('@/api/mes/pro/workorder', () => ({
  listWorkorder: vi.fn().mockResolvedValue({ code: 200, rows: [...], total: 3 }),
  addWorkorder: vi.fn().mockResolvedValue({ code: 200, msg: '成功' }),
}))
// ❌ 不 mock → mount 触发真实 HTTP → 超时/失败
```

> **断网后 `npm test` 必须能全过。** 配合 mock Vue Router/Pinia，用 `data-testid` 定位（优先级 > `aria-label` > class）。

```bash
npm test                                    # 全量
npm run test:watch                          # watch
npx vitest run src/views/__tests__/login.spec.ts  # 单文件
```

配置：`vitest.config.ts`（jsdom）、`src/__tests__/setup.ts`（全局 stub）、`src/__tests__/helpers/`（mockRouter/mockStore）。覆盖率底线：关键页面 ≥60%，生成器页面可不测，手写业务必须测。
