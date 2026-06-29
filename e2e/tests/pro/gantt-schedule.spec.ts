import { test, expect } from '@playwright/test'

test.describe('甘特图排产 — E2E', () => {
  test.use({ storageState: 'setup/storageState.json' })
  test.setTimeout(120000)

  test('1. 甘特图页面可访问', async ({ page }) => {
    await page.goto('/mes/pro/gantt')
    await page.waitForTimeout(2000)
    // 搜索栏应可见
    await expect(page.locator('.gantt-page')).toBeVisible()
    // 工单队列应可见
    await expect(page.locator('.wo-queue')).toBeVisible()
  })

  test('2. 工单队列加载工单列表', async ({ page }) => {
    await page.goto('/mes/pro/gantt')
    await page.waitForTimeout(3000)
    // 队列应有卡片
    const cards = page.locator('.queue-card')
    const count = await cards.count()
    expect(count).toBeGreaterThan(0)
  })

  test('3. 点击队列卡片加载甘特图', async ({ page }) => {
    await page.goto('/mes/pro/gantt')
    await page.waitForTimeout(3000)
    // 点击第一个卡片
    const card = page.locator('.queue-card').first()
    await card.click()
    await page.waitForTimeout(2000)
    // 甘特图主体应可见
    await expect(page.locator('.gc-root')).toBeVisible({ timeout: 10000 })
  })

  test('4. 自动排产按钮可触发', async ({ page }) => {
    await page.goto('/mes/pro/gantt')
    await page.waitForTimeout(3000)
    // 找一张有"自动排产"按钮的卡片
    const card = page.locator('.queue-card').first()
    await card.click()
    await page.waitForTimeout(1000)
    // 点击卡片上的自动排产按钮
    const schedBtn = card.locator('button', { hasText: '自动排产' })
    if (await schedBtn.isVisible()) {
      await schedBtn.click()
      await page.waitForTimeout(3000)
      // 应有成功提示
      await expect(page.locator('.el-message').filter({ hasText: '排产完成' }).first()).toBeVisible({ timeout: 10000 })
    }
  })

  test('5. 搜索栏选工单后甘特图渲染', async ({ page }) => {
    await page.goto('/mes/pro/gantt')
    await page.waitForTimeout(3000)
    // 打开工单下拉选
    const select = page.locator('.el-select').first()
    await select.click()
    await page.waitForTimeout(500)
    // 输入搜索
    const input = select.locator('input')
    if (await input.isVisible()) {
      await input.fill('0628')
      await page.waitForTimeout(1000)
    }
    // 选第一个option
    const option = page.locator('.el-select-dropdown__item').first()
    if (await option.isVisible()) {
      await option.click()
      await page.waitForTimeout(2000)
      await expect(page.locator('.gc-root')).toBeVisible({ timeout: 10000 })
    }
  })

  test('6. 甘特图缩放按钮可用', async ({ page }) => {
    await page.goto('/mes/pro/gantt')
    await page.waitForTimeout(3000)
    const card = page.locator('.queue-card').first()
    await card.click()
    await page.waitForTimeout(2000)
    // 缩放按钮
    const zoomBtns = page.locator('.gc-toolbar button')
    const count = await zoomBtns.count()
    expect(count).toBeGreaterThanOrEqual(2)
  })

  test('7. 快照面板可访问', async ({ page }) => {
    await page.goto('/mes/pro/gantt')
    await page.waitForTimeout(2000)
    await expect(page.locator('.snapshot-panel')).toBeVisible({ timeout: 10000 })
  })

  test('8. 换型时间配置页可访问', async ({ page }) => {
    await page.goto('/mes/pro/changeover')
    await page.waitForTimeout(2000)
    await expect(page.locator('.changeover-page')).toBeVisible({ timeout: 10000 })
  })
})
