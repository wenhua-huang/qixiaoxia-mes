import { chromium, type FullConfig } from '@playwright/test'
import { existsSync, statSync } from 'fs'

const STORAGE_STATE_MAX_AGE_MS = 30 * 60 * 1000 // 30分钟有效，过期重新登录

/**
 * E2E 全局 setup — 通过 API 快速获取 token，保存 storageState。
 *
 * 优化：优先复用上次的 storageState（30分钟内有效），避免每次跑测试都启动浏览器登录。
 * 如果 storageState 过期或不存在，则通过浏览器登录一次并缓存。
 */
async function globalSetup(config: FullConfig) {
  const { baseURL, storageState } = config.projects[0].use
  const stateFile = storageState as string

  if (!stateFile) {
    throw new Error('storageState 配置缺失')
  }

  // === 快速路径：复用缓存的 storageState ===
  if (existsSync(stateFile)) {
    const age = Date.now() - statSync(stateFile).mtimeMs
    if (age < STORAGE_STATE_MAX_AGE_MS) {
      console.log(`E2E 复用缓存 storageState（${Math.round(age / 1000)}s 前）`)
      return
    }
    console.log(`storageState 已过期(${Math.round(age / 1000)}s)，重新登录...`)
  }

  // === 慢路径：通过 API 直接登录，写入 localStorage，比浏览器 UI 登录快数倍 ===
  const API_BASE = (baseURL as string).replace(/:80$/, '') + '/dev-api'
  let token: string

  try {
    // Step 1: 调用登录 API 获取 token
    const res = await fetch(`${API_BASE}/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: 'admin', password: 'admin123', code: '', uuid: '' })
    })
    const data = await res.json() as any
    if (!data.token) {
      throw new Error(`登录 API 未返回 token: ${JSON.stringify(data)}`)
    }
    token = data.token
    console.log('API 登录成功，token 已获取')
  } catch (e) {
    // API 登录失败 → 降级为浏览器登录
    console.log('API 登录失败，降级为浏览器登录:', (e as Error).message)
    await browserLogin(baseURL as string, stateFile)
    return
  }

  // Step 2: 用 token 构造 storageState（写入 localStorage + cookie）
  const browser = await chromium.launch({ channel: 'chrome' })
  const page = await browser.newPage()
  try {
    // 访问任意页面以建立 origin
    await page.goto(baseURL + '/index', { waitUntil: 'commit', timeout: 5000 }).catch(() => {})
    // 注入 token 到 Cookie（RuoYi 前端使用 js-cookie 读取 Admin-Token）
    await page.evaluate((t: string) => {
      document.cookie = `Admin-Token=${t}; path=/`
    }, token)
    // 保存认证状态
    await page.context().storageState({ path: stateFile })
    console.log('storageState 已保存（API 快速路径）')
  } finally {
    await browser.close()
  }
}

/** 降级方案：浏览器 UI 登录 */
async function browserLogin(baseURL: string, stateFile: string) {
  const browser = await chromium.launch({ channel: 'chrome' })
  const page = await browser.newPage()
  try {
    await page.goto(baseURL + '/login')
    await page.waitForSelector('input[placeholder="请输入账号"]', { timeout: 10_000 })
    await page.fill('input[placeholder="请输入账号"]', 'admin')
    await page.fill('input[placeholder="请输入密码"]', 'admin123')
    const captchaInput = page.locator('input[placeholder="验证码"]')
    if (await captchaInput.isVisible().catch(() => false)) {
      throw new Error('验证码已启用，请先禁用: UPDATE sys_config SET config_value=\'false\' WHERE config_key=\'sys.account.captchaEnabled\'')
    }
    await page.click('.login-btn')
    await page.waitForURL('**/index', { timeout: 10_000 })
    await page.context().storageState({ path: stateFile })
    console.log('浏览器登录成功，storageState 已保存')
  } finally {
    await browser.close()
  }
}

export default globalSetup
