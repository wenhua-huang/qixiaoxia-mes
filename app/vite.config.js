import { defineConfig } from 'vite'
import { resolve } from 'node:path'
import { existsSync } from 'node:fs'
import uni from '@dcloudio/vite-plugin-uni'

// 本项目是根目录结构（main.js / App.vue / pages.json 均在 app/ 根目录，
// src/ 仅为兼容 CLI 的符号链接目录）。uni-app CLI 在 uni() 执行时若 UNI_INPUT_DIR 未设置，
// 会默认取 <cwd>/src，导致 uni-h5-vite 的 main.js / App.vue 改写插件按 app/src/main.js
// 匹配，而 Vite 经符号链接 realpath 后传入的是 app/main.js，二者字符串不等 →
// main.js 不会被注入 import './pages-json-js'（其中 window.uni = uni）和 .mount("#app")，
// 于是 window.uni 不存在，permission.js 顶层 uni.addInterceptor 抛 "uni is not defined"。
//
// HBuilderX 启动时根本不设 UNI_INPUT_DIR；CLI 启动时 runDev 会在加载本 config 前把它
// 默认成 <cwd>/src。两种情况都需校正为根目录（前提是根目录确有 main.js），不影响其他项目。
const rootInputDir = process.cwd()
const defaultSrcInputDir = resolve(rootInputDir, 'src')
if ((!process.env.UNI_INPUT_DIR || process.env.UNI_INPUT_DIR === defaultSrcInputDir)
    && existsSync(resolve(rootInputDir, 'main.js'))) {
  process.env.UNI_INPUT_DIR = rootInputDir
  process.env.VITE_ROOT_DIR = rootInputDir
}

export default defineConfig({
  plugins: [uni()],
  server: {
    // 端口以 manifest.json 的 h5.devServer.port(9090) 为准，这里显式对齐避免误导；
    // host:true 监听 0.0.0.0，手机同局域网可通过 http://<电脑IP>:9090 访问
    // 多 worktree 并行联调时可用 VITE_PORT / VITE_API_TARGET 覆盖
    host: true,
    port: Number(process.env.VITE_PORT) || 9090,
    proxy: {
      // H5 dev 态 config.baseUrl='/dev-api'，由这里转发到本机后端 8081
      '/dev-api': {
        target: process.env.VITE_API_TARGET || 'http://localhost:8081',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/dev-api/, '')
      }
    }
  }
})
