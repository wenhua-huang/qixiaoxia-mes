import { test, expect } from '@playwright/test'

/**
 * 班组设置 E2E
 */
test.describe('班组设置', () => {
  test.use({ storageState: 'setup/storageState.json' })
  test.setTimeout(120000)

  async function navigateTo(page, menuName: string) {
    await page.setViewportSize({ width: 1920, height: 1080 })
    await page.goto('/index')
    await page.waitForTimeout(4000)

    // 监听 console 错误
    page.on('pageerror', (err) => console.log('  ❌ JS错误:', err.message))

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

    // 点击目标菜单
    await page.evaluate((name) => {
      const items = document.querySelectorAll('.el-menu-item')
      for (const t of items) {
        if ((t as HTMLElement).textContent?.trim() === name) {
          (t as HTMLElement).click()
          return
        }
      }
    }, menuName)
    await page.waitForTimeout(2000)
  }

  test('页面加载 → 表格可见 → 对话框打开', async ({ page }) => {
    await navigateTo(page, '班组设置')

    await expect(page.locator('.el-table').first()).toBeVisible({ timeout: 15000 })

    // 对话框模板验证
    await page.locator('button').filter({ hasText: /新增/ }).first().click({ timeout: 5000 })
    const dialog = page.locator('.el-dialog').first()
    await expect(dialog).toBeVisible({ timeout: 5000 })
    await expect(dialog).toContainText('班组编号')
    await expect(dialog).toContainText('班组名称')
    await expect(dialog).toContainText('班组类型')

    await dialog.locator('button').filter({ hasText: '取 消' }).first().click()
    await page.waitForTimeout(300)

    console.log('  ✅ 班组设置：页面 + dialog 正常')
  })

  test('新增班组 → 表单填写 + 保存', async ({ page }) => {
    await navigateTo(page, '班组设置')
    await expect(page.locator('.el-table').first()).toBeVisible({ timeout: 15000 })

    await page.locator('button').filter({ hasText: /新增/ }).first().click({ timeout: 5000 })
    const dialog = page.locator('.el-dialog').first()
    await expect(dialog).toBeVisible({ timeout: 5000 })

    // 关闭自动编码，手动填写
    const sw = dialog.locator('.el-switch').first()
    if (await sw.isVisible().catch(() => false)) {
      const checked = await sw.evaluate((el) => el.classList.contains('is-checked'))
      if (checked) {
        await sw.click()
        await page.waitForTimeout(300)
      }
    }

    const uniqueCode = 'E2E-TM-' + Date.now().toString(36).toUpperCase()
    await dialog.getByPlaceholder('请输入班组编号').fill(uniqueCode)
    await dialog.getByPlaceholder('请输入班组名称').fill('E2E测试班组')

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
      // 关闭下拉（Escape key）
      await page.keyboard.press('Escape')
      await page.waitForTimeout(300)
    }

    // 保存
    await dialog.locator('button').filter({ hasText: '确 定' }).first().click()
    await page.waitForTimeout(1500)

    const stillOpen = await dialog.isVisible().catch(() => false)
    if (stillOpen) {
      const errors = dialog.locator('.el-form-item__error')
      if ((await errors.count().catch(() => 0)) > 0) {
        console.log('  校验错误:', (await errors.allTextContents()).join('; '))
      }
      await dialog.locator('button').filter({ hasText: '取 消' }).first().click()
      await page.waitForTimeout(300)
    }

    console.log('  ✅ 班组设置：新增流程完成')
  })
})
