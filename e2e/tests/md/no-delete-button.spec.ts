import { test, expect } from '@playwright/test'

/**
 * 主数据页面 — 无删除按钮 E2E 测试
 *
 * 验证删除按钮已移除，启停用开关替代物理删除。
 */

// ═══════════ 工具函数 ═══════════

async function navigateTo(page: any, parentMenuName: string, menuName: string) {
  await page.setViewportSize({ width: 1920, height: 1080 })
  await page.goto('/index')
  await page.waitForTimeout(4000)

  // 展开父级子菜单
  await page.evaluate((label: string) => {
    const subs = document.querySelectorAll('.el-sub-menu__title')
    for (const t of Array.from(subs)) {
      if ((t as HTMLElement).textContent?.includes(label)) {
        (t as HTMLElement).click()
        return
      }
    }
  }, parentMenuName)
  await page.waitForTimeout(1000)

  // 点击目标菜单项
  await page.evaluate((name: string) => {
    const items = document.querySelectorAll('.el-menu-item')
    for (const t of Array.from(items)) {
      if ((t as HTMLElement).textContent?.trim() === name) {
        (t as HTMLElement).click()
        return
      }
    }
  }, menuName)
  await page.waitForTimeout(4000)

  await expect(page.locator('.el-table, .el-row, .el-col').first()).toBeVisible({ timeout: 20000 })
}

// ═══════════ 测试 ═══════════

const PAGES = [
  { parent: '基础数据', name: '车间管理' },
  { parent: '基础数据', name: '供应商管理' },
  { parent: '基础数据', name: '客户管理' },
  { parent: '仓储管理', name: '仓库设置' },
]

test.describe('主数据页面 — 无删除按钮', () => {
  test.use({ storageState: 'setup/storageState.json' })
  test.setTimeout(180000)

  for (const { parent, name } of PAGES) {
    test(`${parent} > ${name} 工具栏无删除按钮`, async ({ page }) => {
      await navigateTo(page, parent, name)

      // 工具栏区域（.mb8）不应含"删除"按钮
      const toolbar = page.locator('.mb8').first()
      await expect(toolbar).toBeVisible({ timeout: 10000 })
      // 查找 toolbar 中所有按钮文本
      const btnTexts = await toolbar.locator('.el-button').allTextContents()
      const allText = btnTexts.join('|')
      expect(allText).not.toContain('删除')
    })
  }
})
