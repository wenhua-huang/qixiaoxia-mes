# CLAUDE.md

This file provides guidance to Claude Code when working with code in this repository.

## Project Overview

RuoYi-App Vue3 (v1.2.0), the mobile app for the **企小侠文化纸盒MES系统** (Qixiaoxia Cultural Paper Box MES). Built with uni-app + Vue 3 + Pinia + uni-ui. Supports H5, Android APP, iOS APP, WeChat Mini Program, Alipay Mini Program — one codebase, multi-platform.

**Source**: Cloned from https://gitee.com/y_project/RuoYi-App (vue3 branch).

## Build & Run

**开发工具**: [HBuilder X](https://www.dcloud.io/hbuilderx.html) (推荐) 或 HBuilder CLI

```bash
# 安装依赖
npm install

# H5 开发 (浏览器运行)
# 在 HBuilder X 中: 运行 → 运行到浏览器 → Chrome

# 微信小程序开发
# 在 HBuilder X 中: 运行 → 运行到小程序模拟器 → 微信开发者工具

# APP 开发
# 在 HBuilder X 中: 运行 → 运行到手机或模拟器

# H5 构建
npm run build:h5

# 微信小程序构建
npm run build:mp-weixin
```

- **H5 开发端口**: 默认通过 HBuilder X 内置服务器
- **后端接口**: 配置在 `config.js` 中，指向 RuoYi-Vue 后端 API
- **多端适配**: `pages.json` 统一配置页面路由和样式，`manifest.json` 配置 APP 信息

## Project Structure

```
app/
├── App.vue                    # 根组件 (应用生命周期)
├── main.js                    # 应用入口 (注册全局组件/插件)
├── manifest.json              # 应用配置 (APP ID、权限、分发)
├── pages.json                 # 页面路由 & 样式配置
├── uni.scss                   # 全局 SCSS 变量
├── config.js                  # 后端 API 地址配置
├── permission.js              # 路由守卫 (登录检查)
├── api/                       # API 接口层
│   └── ...
├── components/                # 公共组件
├── pages/                     # 页面 (符合 uni-app 规范)
│   ├── index/                 # 首页 (工作台入口)
│   ├── login/                 # 登录页
│   ├── register/              # 注册页
│   ├── work/                  # 工作台
│   └── mine/                  # 我的 (个人中心)
│       ├── index/             # 个人信息首页
│       ├── avatar/            # 头像修改
│       └── info/              # 资料编辑
├── plugins/                   # JS 插件
├── static/                    # 静态资源
├── store/                     # Pinia 状态管理
│   └── modules/
├── uni_modules/               # uni-app 插件模块 (uni-ui 等)
└── utils/                     # 工具函数
```

## Architecture & Key Patterns

### Technology Stack

| 类别 | 技术 |
|------|------|
| 框架 | uni-app (Vue 3) |
| 语言 | JavaScript (ES6+) |
| 状态管理 | Pinia |
| UI 组件 | uni-ui (全端兼容) |
| 后端对接 | RuoYi-Vue REST API |
| 构建 | HBuilder X / uni-app CLI |

### Platform Adaptation

uni-app 通过条件编译实现一套代码多端运行：
```vue
<!-- #ifdef H5 -->
  H5 专属代码
<!-- #endif -->
<!-- #ifdef MP-WEIXIN -->
  微信小程序专属代码
<!-- #endif -->
```

### Page Lifecycle

uni-app 页面生命周期与 Vue 3 结合：
- `onLoad` / `onShow` / `onReady` / `onHide` / `onUnload` (uni-app)
- `onMounted` / `onUnmounted` 等 (Vue 3 Composition API)

### Backend Communication

- 与 `backend/` (RuoYi-Vue) 后端接口完全对接
- 使用与 PC 前端相同的认证机制 (Bearer Token)
- `config.js` 中配置后端 API 基础地址

### Authentication

- `permission.js` — 路由守卫，未登录跳转登录页
- Token 存储在本地缓存
- 支持自动登录（token 未过期时）

### Key Dependencies

| 依赖 | 用途 |
|------|------|
| uni-app | 跨端应用框架 |
| uni-ui | 全端兼容 UI 组件库 |
| Pinia | 状态管理 |
| uView / uni-icons | 图标/扩展组件 |

## Customization for 企小侠 MES

### MES Mobile Page Directory

```
pages/mes/
├── wm/       # 仓储管理移动端页面
│   ├── itemrecpt/   # 入库
│   ├── issue/       # 出库
│   └── inventory/   # 盘点
├── pro/      # 生产管理移动端页面
│   ├── workorder/   # 工单操作
│   └── report/      # 报工
├── qc/       # 质量管理移动端页面
│   ├── inspection/  # 检验
│   └── defect/      # 缺陷记录
└── md/       # 基础数据查询
```

### MES APP 核心功能场景

1. **移动端入库**: 扫码入库、拍照留证
2. **移动端报工**: 工单扫码、报工录入
3. **移动端质检**: 检验结果录入、不良品拍照
4. **库存查询**: 实时库存、库位查询
5. **生产看板**: 工单进度、质量数据

### API Configuration

- 修改 `config.js` 中的后端 API 地址
- 开发环境指向本地后端: `http://localhost:8080`
- 生产环境指向实际部署地址

### Component Notes

- uni-app 使用自定义组件规范（非标准 HTML 标签）
- `pages.json` 中配置页面路径、导航栏样式
- 小程序开发需注意包体积限制 (分包加载)

## 移动端 UI 规范

### 触摸交互

- 最小触摸区域 44×44px（符合 iOS/Android 人机交互指南）
- 按钮间距 ≥ 8px，避免误触
- 关键操作按钮固定屏幕底部（如"提交""确认入库"）

### 字体与排版

- 标题 16-18px，正文 14px，辅助文字 12px
- 中文优先，数字/单位用 DIN 等宽字体

### 表单规范

- 输入框高度 ≥ 44px
- 长表单分步骤（每步 ≤ 6 个字段）
- 必填项标红色星号

## 平台适配与条件编译

### 条件编译原则

- `#ifdef H5`：仅 H5 浏览器需的代码（如 WebSocket、富文本编辑器）
- `#ifdef MP-WEIXIN`：仅小程序需的代码（如微信登录、订阅消息）
- `#ifdef APP-PLUS`：仅原生 APP 需的代码（如原生相机、推送）
- 尽量用统一 API，**不到万不得已不加条件编译**

```vue
<!-- 示例：拍照功能 -->
<!-- #ifdef APP-PLUS -->
<button @tap="openNativeCamera">拍照</button>
<!-- #endif -->
<!-- #ifdef H5 -->
<button @tap="openH5Camera">拍照</button>
<!-- #endif -->
```

### 分包加载

- 小程序主包 ≤ 2MB，总包 ≤ 20MB
- MES 各领域（wm/pro/qc/md）各自独立分包

## 拍照/扫码/离线模式

### 拍照功能

- APP 端用 `uni.chooseImage`，H5 用 `<input type="file" capture="camera">`
- 上传前压缩（最大 1920px 宽，JPEG 质量 0.8）
- 质检拍照需带水印（时间 + 操作人 + 检验单号）

### 扫码功能

- 优先用 `uni.scanCode`，支持条形码和二维码
- 扫码结果自动填充对应字段（如物料编码、工单号）

### 离线模式（规划中）

- 核心报工/质检数据支持离线暂存
- 网络恢复后自动同步，冲突时以服务器数据为准

## 测试覆盖率底线

- 核心流程（登录、报工、质检）手动测试覆盖
- 后续引入 uni-app 自动化测试框架
