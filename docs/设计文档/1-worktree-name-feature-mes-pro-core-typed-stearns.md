# 修复 pro 页面白屏 & 菜单切换问题

## 根因分析

| # | 问题 | 根因 |
|---|------|------|
| 1 | 菜单"跳出新页面" | `constantRoutes` 添加了**平级路由** `/mes/pro/process`（无 Layout 包裹），匹配优先级高于后端嵌套路由，渲染时无 Layout shell（无侧边栏/导航栏） |
| 2 | 切几次白屏 | `loadView` 的 `@vite-ignore` fallback path `@/views/${view}.vue` 在运行时无法被 Vite 解析 → 404 → 组件 `undefined` |
| 3 | 侧边栏重复条目 | constantRoutes 缺少 `hidden: true`，污染 `sidebarRouters` |

## 修复方案

### 1. 移除 `constantRoutes` 中的 pro 路由

**文件**: `frontend/src/router/index.ts`
- 删除之前添加的 3 个 pro 路由（`/mes/pro/process`, `/mes/pro/proroute`, `/mes/pro/workorder`）
- 恢复原有 `constantRoutes` 结尾

### 2. 修复 `loadView` 的 fallback import

**文件**: `frontend/src/store/modules/permission.ts`

将：
```ts
const importPath = `@/views/${view}.vue`
viewCache[view] = () => import(/* @vite-ignore */ importPath)
```

改为使用 **相对路径** + **去掉 @vite-ignore**：
```ts
// 使用相对路径，Vite 能正确解析动态 import
viewCache[view] = () => import(`../../views/${view}.vue`)
```

这样 Vite 会将 `../../views/mes/pro/process/index.vue` 正确解析为静态可追踪的 import。

### 3. 验证

步骤：
1. 移除 constantRoutes 中的 pro 路由
2. 修复 loadView fallback
3. 重启前端 dev server
4. curl 验证 3 个页面编译 200
5. 模拟 10 轮切换验证页面不 404
6. API E2E (创建→列表→详情→修改→删除) 验证后端正常
