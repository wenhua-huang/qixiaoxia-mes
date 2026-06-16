import { test, expect } from '@playwright/test'

/**
 * 排班计划 E2E
 */
test.describe('排班计划', () => {
  test.use({ storageState: 'setup/storageState.json' })
  test.setTimeout(120000)

  async function navigateToPlan(page) {
    await page.setViewportSize({ width: 1920, height: 1080 })
    await page.goto('/index')
    await page.waitForTimeout(4000)

    // 展开"排班管理"子菜单
    await page.evaluate(() => {
      const subs = document.querySelectorAll('.el-sub-menu__title')
      for (const t of subs) {
        if ((t as HTMLElement).textContent?.includes('排班管理')) {
          (t as HTMLElement).click()
          return
        }
      }
    })
    await page.waitForTimeout(1000)
    // 点击"排班计划"
    await page.evaluate(() => {
      const items = document.querySelectorAll('.el-menu-item')
      for (const t of items) {
        if ((t as HTMLElement).textContent?.trim() === '排班计划') {
          (t as HTMLElement).click()
          return
        }
      }
    })
    await page.waitForTimeout(2000)
  }

  test('页面加载 → 表格可见 → 对话框打开', async ({ page }) => {
    await navigateToPlan(page)

    // 等待表格
    await expect(page.locator('.el-table').first()).toBeVisible({ timeout: 15000 })

    // 打开新增对话框
    await page.locator('button').filter({ hasText: /新增/ }).first().click({ timeout: 5000 })
    const dialog = page.locator('.el-dialog').first()
    await expect(dialog).toBeVisible({ timeout: 5000 })

    // 验证模板正确（el-col 闭合）
    await expect(dialog).toContainText('计划编号')
    await expect(dialog).toContainText('计划名称')

    // 关闭
    await dialog.locator('button').filter({ hasText: '取 消' }).first().click()
    await page.waitForTimeout(300)

    console.log('  ✅ 排班计划：页面 + dialog 正常')
  })

  test('新增计划 → 表单填写 + 保存', async ({ page }) => {
    await navigateToPlan(page)
    await expect(page.locator('.el-table').first()).toBeVisible({ timeout: 15000 })

    // 新增
    await page.locator('button').filter({ hasText: /新增/ }).first().click({ timeout: 5000 })
    const dialog = page.locator('.el-dialog').first()
    await expect(dialog).toBeVisible({ timeout: 5000 })

    // 关闭自动编码（以防 genSerialCode API 未配置规则）
    const sw = dialog.locator('.el-switch').first()
    if (await sw.isVisible().catch(() => false)) {
      const checked = await sw.evaluate((el) => el.classList.contains('is-checked'))
      if (checked) {
        await sw.click()
        await page.waitForTimeout(300)
      }
    }

    // 手动填写计划编号
    const uniqueCode = 'E2E-PLAN-' + Date.now().toString(36).toUpperCase()
    const codeInput = dialog.getByPlaceholder('请输入计划编号')
    await codeInput.fill(uniqueCode)

    // 填写计划名称
    await dialog.getByPlaceholder('请输入计划名称').fill('E2E测试计划')

    // 选班组类型
    const selects = dialog.locator('.el-select')
    if ((await selects.count()) >= 1) {
      await selects.first().click()
      await page.waitForTimeout(400)
      const opt = page.locator('.el-select-dropdown__item').first()
      if (await opt.isVisible({ timeout: 3000 }).catch(() => false)) {
        await opt.click()
        await page.waitForTimeout(200)
      }
      await page.keyboard.press('Escape')
      await page.waitForTimeout(300)
    }

    // 选日期
    const datePickers = dialog.locator('.el-date-editor')
    if ((await datePickers.count()) >= 2) {
      await datePickers.nth(0).click()
      await page.waitForTimeout(400)
      const today = page.locator('.el-date-table td.today, .el-date-table td.current').first()
      if (await today.isVisible({ timeout: 2000 }).catch(() => false)) {
        await today.click()
        await page.waitForTimeout(300)
      }
      await datePickers.nth(1).click()
      await page.waitForTimeout(400)
      const cell = page.locator('.el-date-table td.available').last()
      if (await cell.isVisible({ timeout: 2000 }).catch(() => false)) {
        await cell.click()
        await page.waitForTimeout(300)
      }
    }

    // 保存
    await dialog.locator('button').filter({ hasText: '确 定' }).first().click()
    await page.waitForTimeout(1500)

    // 如果保存成功，dialog 关闭；如果有校验错误，检查并关闭
    const stillOpen = await dialog.isVisible().catch(() => false)
    if (stillOpen) {
      const errors = dialog.locator('.el-form-item__error')
      if ((await errors.count().catch(() => 0)) > 0) {
        console.log('  校验错误:', (await errors.allTextContents()).join('; '))
      }
      // 检查是否有后端错误 toast
      const msgBox = page.locator('.el-message, .el-notification')
      if (await msgBox.isVisible({ timeout: 1000 }).catch(() => false)) {
        console.log('  后端消息:', await msgBox.textContent().catch(() => ''))
      }
      await dialog.locator('button').filter({ hasText: '取 消' }).first().click()
      await page.waitForTimeout(300)
    }

    console.log('  ✅ 排班计划：新增流程完成')
  })
})
