import { defineConfig, devices } from '@playwright/test'

export default defineConfig({
  testDir: './tests',
  // 单测试超时
  timeout: 30_000,
  // 并行执行
  fullyParallel: true,
  // CI 环境禁止 test.only()
  forbidOnly: !!process.env.CI,
  // CI 环境重试
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: [
    ['html', { outputFolder: 'playwright-report' }],
    ['list']
  ],
  // 全局 setup：登录获取 storageState
  globalSetup: './setup/global-setup.ts',

  use: {
    // 前端 dev server 地址
    baseURL: process.env.BASE_URL || 'http://localhost:5173',

    // 失败时保留 trace
    trace: 'retain-on-failure',

    // 失败时自动截图
    screenshot: 'only-on-failure',

    // selector 优先级：data-testid
    testIdAttribute: 'data-testid'
  },

  projects: [
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
        // 复用登录态（仅在项目级别设置，未认证的 describe 块须显式 override 为 undefined）
        storageState: 'setup/storageState.json'
      }
    }
  ],

  // 自动启动前端 dev server
  webServer: {
    command: 'cd ../frontend && npm run dev',
    url: 'http://localhost:5173',
    reuseExistingServer: !process.env.CI,
    timeout: 120_000
  }
})
