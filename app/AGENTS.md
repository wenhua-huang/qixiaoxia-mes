# app/AGENTS.md

RuoYi-App Vue3（v1.2.0），MES 移动端。uni-app + Vue 3 + Pinia + uni-ui，一套代码多端（H5 / Android APP / iOS APP / 微信 & 支付宝小程序）。源 https://gitee.com/y_project/RuoYi-App（vue3 分支）。

> 根级约束见 [`../AGENTS.md`](../AGENTS.md)。

## Build & Run

```bash
cd app && npm install
npm run build:h5            # H5 构建
npm run build:mp-weixin     # 微信小程序构建
```

开发推荐 [HBuilder X](https://www.dcloud.io/hbuilderx.html)：运行 → 运行到浏览器/小程序模拟器/手机或模拟器。H5 端口走 HBuilder X 内置服务器；后端地址配在 `config.js`；页面路由/样式配 `pages.json`，APP 信息配 `manifest.json`。

## Project Structure

```
app/
├── App.vue / main.js        # 根组件 / 入口（注册全局组件插件）
├── manifest.json            # APP 配置（ID、权限、分发）
├── pages.json               # 页面路由 & 样式
├── uni.scss                 # 全局 SCSS 变量
├── config.js                # 后端 API 地址
├── permission.js            # 路由守卫（登录检查）
├── api/ components/ plugins/ static/
├── store/modules/           # Pinia
├── pages/                   # 页面（index/login/register/work/mine + mes/）
└── uni_modules/             # uni-app 插件（uni-ui 等）
```

## 技术栈

uni-app（Vue 3）/ JavaScript（ES6+）/ Pinia / uni-ui（全端兼容）/ RuoYi-Vue REST API / HBuilder X 构建。

## 关键机制

**条件编译**（一套代码多端）：

```vue
<!-- #ifdef H5 -->       H5 专属（WebSocket、富文本）
<!-- #endif -->
<!-- #ifdef MP-WEIXIN -->微信小程序专属（微信登录、订阅消息）
<!-- #endif -->
<!-- #ifdef APP-PLUS --> 原生 APP 专属（原生相机、推送）
<!-- #endif -->
```

原则：尽量用统一 API，**不到万不得已不加条件编译**。

**页面生命周期**：uni-app（`onLoad`/`onShow`/`onReady`/`onHide`/`onUnload`）+ Vue 3（`onMounted`/`onUnmounted`）。

**后端对接**：与 `backend/` REST API 完全对接，同认证机制（Bearer Token），`config.js` 配后端基础地址（开发指向本地 `http://localhost:8080`）。`permission.js` 路由守卫未登录跳登录页，Token 存本地缓存支持自动登录。

## MES 移动端目录

```
pages/mes/
├── wm/    # 仓储：itemrecpt(入库) / issue(出库) / inventory(盘点)
├── pro/   # 生产：workorder(工单) / report(报工)
├── qc/    # 质量：inspection(检验) / defect(缺陷)
└── md/    # 基础数据查询
```

核心场景：移动端入库（扫码、拍照留证）、报工（工单扫码、录入）、质检（结果录入、不良品拍照）、库存查询（实时库存/库位）、生产看板（工单进度/质量数据）。

## 移动端 UI 规范

- **触摸**：最小触摸区 44×44px，按钮间距 ≥8px，关键操作按钮固定屏幕底部
- **字体**：标题 16-18px，正文 14px，辅助 12px；数字/单位用 DIN 等宽
- **表单**：输入框高 ≥44px，长表单分步（每步 ≤6 字段），必填项红色星号

## 拍照 / 扫码 / 离线

- **拍照**：APP 用 `uni.chooseImage`，H5 用 `<input type="file" capture="camera">`；上传前压缩（最大 1920px 宽，JPEG 0.8）；质检拍照带水印（时间+操作人+检验单号）
- **扫码**：优先 `uni.scanCode`（条形码+二维码），结果自动填充（物料编码、工单号）
- **离线**（规划中）：核心报工/质检数据离线暂存，网络恢复自动同步，冲突以服务器为准

## 分包与测试

- 小程序主包 ≤2MB、总包 ≤20MB，MES 各领域（wm/pro/qc/md）独立分包
- 测试：核心流程（登录/报工/质检）手动覆盖，后续引入 uni-app 自动化测试框架

## Key References

- uni-app https://uniapp.dcloud.net.cn/
- HBuilder X https://www.dcloud.io/hbuilderx.html
