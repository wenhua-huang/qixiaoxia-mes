import { test, expect } from '@playwright/test'

test.describe('登录流程', () => {
  // 每个测试使用全新的浏览器上下文(不共享登录态)
  test.use({ storageState: { cookies: [], origins: [] } })

  test('should display login form elements', async ({ page }) => {
    // 清除可能残留的认证信息后访问登录页
    await page.context().clearCookies()
    await page.goto('/login')
    await expect(page.locator('input[placeholder="请输入账号"]')).toBeVisible({ timeout: 5000 })
    await expect(page.locator('input[placeholder="请输入密码"]')).toBeVisible()
    await expect(page.locator('.login-btn')).toBeVisible()
  })

  test('should show brand title on login page', async ({ page }) => {
    await page.context().clearCookies()
    await page.goto('/login')
    await expect(page.locator('text=企小侠平台')).toBeVisible({ timeout: 5000 })
  })
})

test.describe('已登录状态', () => {
  // 复用 global-setup 保存的登录态
  test.use({ storageState: 'setup/storageState.json' })

  test('should load home page when already authenticated', async ({ page }) => {
    await page.goto('/')
    // 已登录用户应停留在首页，不被重定向到 /login
    await expect(page).not.toHaveURL(/\/login/)
    await expect(page.locator('.main-container, .app-main, .dashboard').first()).toBeVisible({ timeout: 5000 })
  })
})
