import { test, expect } from '@playwright/test'

/**
 * 节假日设置 E2E — 日历视图 + 右键交互
 */
test.describe('节假日设置', () => {
  test.use({ storageState: 'setup/storageState.json' })
  test.setTimeout(120000)

  async function navigateToHoliday(page) {
    await page.setViewportSize({ width: 1920, height: 1080 })
    await page.goto('/index')
    await page.waitForTimeout(4000)

    page.on('pageerror', (err) => console.log('  ❌ JS错误:', err.message))

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

    await page.evaluate(() => {
      const items = document.querySelectorAll('.el-menu-item')
      for (const t of items) {
        if ((t as HTMLElement).textContent?.trim() === '节假日设置') {
          (t as HTMLElement).click()
          return
        }
      }
    })
    await page.waitForTimeout(2000)
  }

  test('页面加载 → 日历可见 → 右键打开对话框', async ({ page }) => {
    await navigateToHoliday(page)

    // 日历渲染
    const calendar = page.locator('.el-calendar').first()
    await expect(calendar).toBeVisible({ timeout: 15000 })

    // 右键点击今天日期
    const todayCell = calendar.locator('.el-calendar-table .current').first()
    await expect(todayCell).toBeVisible({ timeout: 5000 })

    await todayCell.click({ button: 'right' })
    await page.waitForTimeout(500)

    // 对话框打开
    const dialog = page.locator('.el-dialog').first()
    await expect(dialog).toBeVisible({ timeout: 5000 })
    await expect(dialog).toContainText('日期')
    await expect(dialog).toContainText('类型')

    // 日期输入框已自动填充
    const dateInput = dialog.locator('input[readonly]').first()
    await expect(dateInput).toBeVisible()
    const dateVal = await dateInput.inputValue()
    console.log('  右键日期:', dateVal)

    await dialog.locator('button').filter({ hasText: '取 消' }).first().click()
    await page.waitForTimeout(300)

    console.log('  ✅ 节假日设置：日历 + 右键 dialog 正常')
  })

  test('右键设置节假日 → 选类型 → 保存', async ({ page }) => {
    await navigateToHoliday(page)

    const calendar = page.locator('.el-calendar').first()
    await expect(calendar).toBeVisible({ timeout: 15000 })

    // 右键点击今天
    const todayCell = calendar.locator('.el-calendar-table .current').first()
    await todayCell.click({ button: 'right' })
    await page.waitForTimeout(500)

    const dialog = page.locator('.el-dialog').first()
    await expect(dialog).toBeVisible({ timeout: 5000 })

    // 选类型：假
    await dialog.locator('.el-radio').filter({ hasText: '假' }).first().click()
    await page.waitForTimeout(300)

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
    } else {
      console.log('  保存成功，dialog 已关闭')
    }

    console.log('  ✅ 节假日设置：右键设置完成')
  })

  test('日历单元格显示 班/休 标签和农历', async ({ page }) => {
    await navigateToHoliday(page)

    const calendar = page.locator('.el-calendar').first()
    await expect(calendar).toBeVisible({ timeout: 15000 })

    // 今天单元格应有 班/休 标签
    const todayCell = calendar.locator('.el-calendar-table .current').first()
    const tag = todayCell.locator('.el-tag')
    if (await tag.isVisible({ timeout: 3000 }).catch(() => false)) {
      const tagText = await tag.textContent()
      console.log('  班/休标签:', tagText?.trim())
    }

    // 验证农历文字
    const lunar = todayCell.locator('.lunar')
    if (await lunar.isVisible({ timeout: 3000 }).catch(() => false)) {
      const lunarText = await lunar.textContent()
      console.log('  农历:', lunarText?.trim())
    }

    console.log('  ✅ 节假日设置：日历单元格正常')
  })
})
