import { config } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, vi } from 'vitest'
import ElementPlus from 'element-plus'

// ==================== jsdom 缺失的浏览器 API ====================

// Element Plus 组件依赖 matchMedia
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: vi.fn().mockImplementation((query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn()
  }))
})

// window.scrollTo
window.scrollTo = vi.fn() as any

// ResizeObserver (Element Plus 组件使用) — 必须用普通函数，箭头函数不能作 constructor
global.ResizeObserver = function ResizeObserver() {
  return { observe() {}, unobserve() {}, disconnect() {} }
} as any

// IntersectionObserver
global.IntersectionObserver = function IntersectionObserver() {
  return { observe() {}, unobserve() {}, disconnect() {} }
} as any

// ==================== 每个测试前初始化 ====================

beforeEach(() => {
  // 创建新的 Pinia 实例，避免测试间状态污染
  setActivePinia(createPinia())
})

// ==================== 全局插件 ====================

// 安装 Element Plus（组件在 login.vue 中不是 auto-imported，需要全局注册）
config.global.plugins = [ElementPlus]

// ==================== 全局组件 Stub ====================
// 仅 stub jsdom 中确实无法渲染的组件。对话/浮层组件由各测试自行 stub，
// 避免全局 stub 掩盖渲染回归。
config.global.stubs = {
  'svg-icon': { template: '<span class="mock-svg-icon" />' },
  'AppIcon': { template: '<span class="mock-app-icon" />' },
  'dict-tag': { template: '<span class="mock-dict-tag" />' }
}

// ==================== 全局方法（RuoYi globalProperties）====================
// useDict stub：通过 plugin 注册到 app.config.globalProperties，使 setup 中
// getCurrentInstance().proxy.useDict 可用。返回空对象，模板用 v-if 守卫跳过 dict-tag。
const useDictPlugin = {
  install(app: any) {
    app.config.globalProperties.useDict = () => ({})
  }
}
config.global.plugins = [ElementPlus, useDictPlugin]
