import { chromium, type FullConfig } from '@playwright/test'

/**
 * E2E 全局 setup — 执行一次登录，保存 storageState 供后续测试复用。
 *
 * 前提条件：
 * 1. 后端服务已启动
 * 2. 前端 dev server 可访问
 * 3. 验证码已禁用（通过 API 或数据库）：sys.account.captchaEnabled = false
 */
async function globalSetup(config: FullConfig) {
  const { baseURL, storageState } = config.projects[0].use

  if (!storageState) {
    throw new Error('storageState 配置缺失，请检查 playwright.config.ts 中 projects[0].use.storageState')
  }

  const browser = await chromium.launch()
  const page = await browser.newPage()

  try {
    await page.goto(baseURL + '/login')

    // 等待登录表单渲染
    await page.waitForSelector('input[placeholder="请输入账号"]', { timeout: 10_000 })

    // 填写凭据
    await page.fill('input[placeholder="请输入账号"]', 'admin')
    await page.fill('input[placeholder="请输入密码"]', 'admin123')

    // 检查验证码是否启用
    const captchaInput = page.locator('input[placeholder="验证码"]')
    const captchaVisible = await captchaInput.isVisible().catch(() => null)
    if (captchaVisible === true) {
      throw new Error(
        '验证码已启用，E2E 登录无法自动完成。\n' +
        '请在运行 E2E 测试前禁用验证码：\n' +
        '  curl -X PUT http://localhost:8081/system/config \\\n' +
        '    -H "Content-Type: application/json" \\\n' +
        '    -d \'{"configId":4,"configKey":"sys.account.captchaEnabled","configValue":"false"}\'\n' +
        '或通过数据库：UPDATE sys_config SET config_value=\'false\' WHERE config_key=\'sys.account.captchaEnabled\''
      )
    }

    // 点击登录
    await page.click('.login-btn')

    // 等待跳转到首页
    await page.waitForURL('**/index', { timeout: 10_000 })

    // 保存认证状态
    await page.context().storageState({ path: storageState as string })
    console.log('E2E 登录成功，storageState 已保存')
  } catch (e) {
    console.error('E2E 登录失败:', e)
    throw e
  } finally {
    await browser.close()
  }
}

export default globalSetup
