import { createRouter, createWebHistory } from 'vue-router'
import type { Router } from 'vue-router'

/**
 * 创建用于组件测试的最小 Vue Router 实例。
 * 用于 mock useRouter() / useRoute()。
 */
export function createMockRouter(options?: {
  currentRoute?: { name?: string; params?: Record<string, string>; query?: Record<string, string> }
}): Router {
  return createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/', name: 'home', component: { template: '<div>Home</div>' } },
      { path: '/login', name: 'login', component: { template: '<div>Login</div>' } }
    ]
  })
}
