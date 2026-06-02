import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick, reactive } from 'vue'

// ==================== Mock 外部依赖 ====================

// vue-router: login.vue 使用 auto-imported useRouter/useRoute
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn(), currentRoute: { value: reactive({ query: {} }) } }),
  useRoute: () => reactive({ query: {} })
}))

// js-cookie
vi.mock('js-cookie', () => ({
  default: { get: vi.fn(() => ''), set: vi.fn(), remove: vi.fn() }
}))

// @/utils/jsencrypt: login.vue 的实际导入路径
vi.mock('@/utils/jsencrypt', () => ({
  encrypt: vi.fn(() => 'encrypted'),
  decrypt: vi.fn(() => '')
}))

// @/api/login
vi.mock('@/api/login', () => ({
  login: vi.fn().mockResolvedValue({ token: 'mock-token' }),
  getCodeImg: vi.fn().mockResolvedValue({ captchaEnabled: false, img: '', uuid: '' }),
  logout: vi.fn(),
  getInfo: vi.fn()
}))

// @/store/modules/user
vi.mock('@/store/modules/user', async () => {
  const { defineStore } = await import('pinia')
  return {
    default: defineStore('user', () => ({
      token: undefined, id: '', name: '', nickName: '', avatar: '',
      roles: [], permissions: [], login: vi.fn().mockResolvedValue({})
    }))
  }
})

// ==================== 导入组件 ====================
import Login from '@/views/login.vue'

function mountLogin() {
  return mount(Login, {
    global: {
      stubs: {
        'AppIcon': { template: '<span class="mock-appicon" />' },
        // 浮层组件在 jsdom 中需 stub（非全局，避免掩盖其他测试的渲染回归）
        'el-dialog': { template: '<div />' },
        'el-drawer': { template: '<div />' },
        'el-popover': { template: '<div />' },
        'el-tooltip': { template: '<div />' }
      }
    }
  })
}

describe('登录页面', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should mount login page and display brand title', async () => {
    const wrapper = mountLogin()
    await nextTick()
    await nextTick()

    expect(wrapper.find('.login').exists()).toBe(true)
    expect(wrapper.text()).toContain('企小侠平台')
  })

  it('should render username and password input fields', async () => {
    const wrapper = mountLogin()
    await nextTick()
    await nextTick()

    expect(wrapper.find('input[placeholder="请输入账号"]').exists()).toBe(true)
    expect(wrapper.find('input[placeholder="请输入密码"]').exists()).toBe(true)
  })
})
