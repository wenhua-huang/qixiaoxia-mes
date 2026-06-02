import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import autoImport from 'unplugin-auto-import/vite'
import path from 'path'

export default defineConfig({
  plugins: [
    vue(),
    autoImport({
      imports: ['vue', 'vue-router', 'pinia'],
      dts: false
    })
  ],
  test: {
    // jsdom 提供 DOM API 模拟
    environment: 'jsdom',
    // 测试文件匹配模式
    include: ['src/**/__tests__/**/*.spec.ts'],
    // 全局 setup 文件
    setupFiles: ['src/__tests__/setup.ts'],
    // 路径别名（与 vite.config.ts 保持一致）
    alias: {
      '@': path.resolve(__dirname, './src'),
      '~': path.resolve(__dirname, './')
    },
    // CSS 处理
    css: {
      modules: {
        classNameStrategy: 'non-scoped'
      }
    }
  }
})
