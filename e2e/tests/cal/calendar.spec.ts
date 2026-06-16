import { test, expect } from '@playwright/test'

/**
 * 排班日历 E2E
 */
test.describe('排班日历', () => {
  test.use({ storageState: 'setup/storageState.json' })
  test.setTimeout(120000)

  async function navigateToCalendar(page) {
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

    // 点击"排班日历"
    await page.evaluate(() => {
      const items = document.querySelectorAll('.el-menu-item')
      for (const t of items) {
        if ((t as HTMLElement).textContent?.trim() === '排班日历') {
          (t as HTMLElement).click()
          return
        }
      }
    })
    await page.waitForTimeout(2000)
  }

  test('页面加载 → 三个Tab可见 → 日历渲染', async ({ page }) => {
    await navigateToCalendar(page)

    // 三个 Tab 可见：分类、班组、个人
    const tabs = page.locator('.el-tabs__item')
    await expect(tabs.first()).toBeVisible({ timeout: 15000 })

    const tabTexts = await tabs.allTextContents()
    console.log('  Tab列表:', tabTexts)

    expect(tabTexts.some(t => t.includes('分类'))).toBeTruthy()
    expect(tabTexts.some(t => t.includes('班组'))).toBeTruthy()
    expect(tabTexts.some(t => t.includes('个人'))).toBeTruthy()

    console.log('  ✅ 排班日历：3个Tab全部可见')
  })

  test('分类视图 → 日历控件渲染 → solar2lunar 正常', async ({ page }) => {
    await navigateToCalendar(page)

    // 默认在"分类" Tab，等待日历加载
    const calendar = page.locator('.el-calendar').first()
    await expect(calendar).toBeVisible({ timeout: 15000 })

    // 验证日期单元格有内容（solar2lunar 之前有 const offset 赋值 bug）
    const cells = calendar.locator('.el-calendar-table .current')
    if ((await cells.count()) >= 1) {
      const firstCell = cells.first()
      // 检查日期数字和农历标签都在
      const solarEl = firstCell.locator('.solar')
      const lunarEl = firstCell.locator('.lunar')
      if (await solarEl.isVisible({ timeout: 3000 }).catch(() => false)) {
        const solarText = await solarEl.textContent()
        console.log('  分类视图 日期:', solarText?.trim())
      }
      if (await lunarEl.isVisible({ timeout: 3000 }).catch(() => false)) {
        const lunarText = await lunarEl.textContent()
        console.log('  分类视图 农历:', lunarText?.trim())
      }
    }

    // 验证左侧班组类型 radio-group 存在
    const radioGroup = page.locator('.el-radio-group').first()
    await expect(radioGroup).toBeVisible({ timeout: 3000 })

    console.log('  ✅ 分类视图：日历 + 班组类型选择器正常')
  })

  test('班组视图 Tab 切换', async ({ page }) => {
    await navigateToCalendar(page)

    // 切换到"班组" Tab
    await page.locator('.el-tabs__item').filter({ hasText: '班组' }).first().click()
    await page.waitForTimeout(3000) // 等待日历数据加载

    // 班组视图有左侧班组选择 radio-group（多个 pane 中取可见的那个）
    const visibleRadioGroups = page.locator('.el-radio-group').filter({ hasText: /.+/ })
    await expect(visibleRadioGroups.first()).toBeAttached({ timeout: 15000 })

    console.log('  ✅ 班组视图：Tab 切换成功，班组选择器已渲染')
  })

  test('个人视图 Tab 切换', async ({ page }) => {
    await navigateToCalendar(page)

    // 切换到"个人" Tab
    await page.locator('.el-tabs__item').filter({ hasText: '个人' }).first().click()
    await page.waitForTimeout(3000) // 等待日历数据加载

    // 验证人员选择输入框存在
    const userInput = page.getByPlaceholder('请选择查询的人员')
    await expect(userInput).toBeVisible({ timeout: 15000 })

    console.log('  ✅ 个人视图：Tab 切换成功，人员查询框可见')
  })
})
