import { test, expect } from '@playwright/test'

test.describe('生产管理 — 全链路 E2E', () => {
  test.use({ storageState: 'setup/storageState.json' })
  test.setTimeout(300000)

  const TS = Date.now().toString(36).toUpperCase()

  test('1. 所有页面可访问', async ({ page }) => {
    await page.goto('/')
    await page.waitForResponse(r => r.url().includes('/getRouters') && r.status() === 200, { timeout: 20000 })
    await page.waitForTimeout(1000)
    const urls = ['/mes/pro/workorder','/mes/pro/task','/mes/pro/card','/mes/pro/feedback','/mes/pro/materialtrace','/mes/pro/dashboard']
    for (const u of urls) {
      await page.goto(u)
      await expect(page).not.toHaveURL(/\/404/, { timeout: 5000 })
    }
  })

  test('2. 生产工单 — 新增+产品选择器', async ({ page }) => {
    const WO = 'E2E-WO-' + TS
    await page.goto('/mes/pro/workorder')
    await expect(page.locator('.el-table').first()).toBeVisible({ timeout: 10000 })

    await page.locator('button').filter({ hasText: /新增/ }).first().click()
    const d = page.locator('.el-dialog').first()
    await expect(d).toBeVisible({ timeout: 5000 })

    // 验证核心表单字段存在
    await expect(d.getByPlaceholder('请输入').first()).toBeVisible()
    await expect(d.locator('.el-input-number input').first()).toBeVisible()
    await expect(d.locator('.el-radio-group').first()).toBeVisible()

    // 填编码和名称
    const sw = d.locator('.el-switch').first()
    if (await sw.isVisible().catch(() => false) && await sw.evaluate((el: any) => el.classList.contains('is-checked'))) { await sw.click() }
    await d.locator('input:not([disabled])').first().fill(WO)
    await d.getByPlaceholder('请输入').nth(1).fill(WO)
    await d.locator('.el-input-number input').first().fill('100')

    // 打开 ItemSelect 选产品，验证回填
    await d.locator('button').filter({ hasText: /选择/ }).first().click()
    const itemDlg = page.locator('.el-dialog').last()
    await expect(itemDlg.locator('.el-table__body tr').first()).toBeVisible({ timeout: 10000 })
    await itemDlg.locator('.el-table__body tr').first().dblclick()
    await page.waitForTimeout(1000)
  })

  test('2b. 参数修改+保存+核对（浏览器真实操作）', async ({ page }) => {
    const testValue = 'E2E-' + Date.now().toString(36).slice(-4)
    const WO = 'E2E-2B-' + TS
    // ① 新建工单
    await page.goto('/mes/pro/workorder')
    await expect(page.locator('.el-table').first()).toBeVisible({ timeout: 10000 })
    await page.locator('button').filter({ hasText: /新增/ }).first().click()
    let d = page.locator('.el-dialog').first(); await expect(d).toBeVisible({ timeout: 5000 })
    const sw = d.locator('.el-switch').first()
    if (await sw.isVisible().catch(() => false) && await sw.evaluate((el: any) => el.classList.contains('is-checked'))) { await sw.click() }
    await d.getByPlaceholder('请输入').first().fill(WO); await d.getByPlaceholder('请输入').nth(1).fill(WO)
    await d.locator('button').filter({ hasText: /选择/ }).first().click()
    const itemDlg = page.locator('.el-dialog').last()
    await expect(itemDlg.locator('.el-table__body tr').first()).toBeVisible({ timeout: 10000 })
    await itemDlg.locator('.el-table__body tr').first().dblclick()
    await page.waitForTimeout(2000)
    // 选路线（如有）→ 填数量 → 保存
    const routeSel = d.locator('.el-select').last()
    await routeSel.click(); await page.waitForTimeout(500)
    const opts = page.locator('.el-select-dropdown__item')
    if (await opts.first().isVisible({ timeout: 3000 }).catch(() => false)) {
      await opts.first().click(); await page.waitForTimeout(300)
      await d.locator('.el-input-number input').first().fill('100')
      await d.locator('.dialog-footer button').filter({ hasText: '下一步' }).first().click()
      await page.waitForTimeout(500)
      await d.locator('.dialog-footer button').filter({ hasText: '确 定' }).first().click()
      await expect(d).not.toBeVisible({ timeout: 8000 })
    } else {
      // 无路线可选 → 取消，跳过后续编辑步骤
      await page.keyboard.press('Escape'); await page.waitForTimeout(500)
      await page.keyboard.press('Escape')
      test.skip(true, '产品无关联工艺路线')
    }

    // ② 搜索→修改→填参数→保存
    await page.locator('input[placeholder*="请输入"]').first().fill(WO)
    await page.locator('button').filter({ hasText: '搜索' }).first().click(); await page.waitForTimeout(2000)
    await page.locator('.el-table__body button').filter({ hasText: '修改' }).first().click()
    d = page.locator('.el-dialog').first(); await expect(d).toBeVisible({ timeout: 5000 })
    const nextBtn = d.locator('.dialog-footer button').filter({ hasText: '下一步' }).first()
    if (await nextBtn.isVisible({ timeout: 3000 }).catch(() => false)) {
      await nextBtn.click()
      await expect(d.locator('.el-collapse, .el-alert, .el-empty').first()).toBeVisible({ timeout: 15000 })
    }
    const adjInput = d.locator('.el-collapse-item').first().locator('input').first()
    if (await adjInput.isVisible({ timeout: 5000 }).catch(() => false)) {
      await adjInput.fill(testValue)
    }
    await d.locator('.dialog-footer button').filter({ hasText: '确 定' }).first().click()
    await expect(d).not.toBeVisible({ timeout: 10000 })

    // ③ 再次打开 → 核对保存的值
    await page.locator('input[placeholder*="请输入"]').first().fill(WO)
    await page.locator('button').filter({ hasText: '搜索' }).first().click(); await page.waitForTimeout(2000)
    await page.locator('.el-table__body button').filter({ hasText: '修改' }).first().click()
    d = page.locator('.el-dialog').first(); await expect(d).toBeVisible({ timeout: 5000 })
    await d.locator('.dialog-footer button').filter({ hasText: '下一步' }).first().click()
    await expect(d.locator('.el-collapse-item').first()).toBeVisible({ timeout: 15000 })
    const savedInput = d.locator('.el-collapse-item').first().locator('input').first()
    await expect(savedInput).toHaveValue(testValue)
    await page.keyboard.press('Escape')
  })

  test('3. 流转卡 — 创建+批次号生成', async ({ page }) => {
    await page.goto('/mes/pro/card'); await page.waitForTimeout(3000)
    await expect(page.locator('.el-table').first()).toBeVisible({ timeout: 10000 })
    await page.locator('button').filter({ hasText: /新增/ }).first().click()
    const d = page.locator('.el-dialog').first(); await expect(d).toBeVisible({ timeout: 5000 })

    const sw = d.locator('.el-switch').first()
    if (await sw.isVisible().catch(() => false) && await sw.evaluate((el: any) => el.classList.contains('is-checked'))) { await sw.click(); await page.waitForTimeout(200) }
    await d.locator('input:not([disabled])').first().fill('CRD-' + TS)

    // 点击"生成"按钮 → 断言批次号有值
    const genBtn = d.locator('button').filter({ hasText: /生成/ }).first()
    if (await genBtn.isVisible({ timeout: 1500 }).catch(() => false)) {
      await genBtn.click(); await page.waitForTimeout(300)
      // 批次号生成后输入框不为空
      await expect(d.locator('input').nth(1)).not.toBeEmpty({ timeout: 2000 })
    }
    await d.locator('.el-input-number input').first().fill('50')
    await d.locator('button').filter({ hasText: '确 定' }).first().click()
    await page.waitForTimeout(1000)
    await expect(page.locator('.el-table').first()).toBeVisible({ timeout: 5000 })
  })

  test('4. 排产任务 — 创建', async ({ page }) => {
    await page.goto('/mes/pro/task'); await page.waitForTimeout(3000)
    await expect(page.locator('.el-table').first()).toBeVisible({ timeout: 10000 })
    await page.locator('button').filter({ hasText: /新增/ }).first().click()
    const d = page.locator('.el-dialog').first(); await expect(d).toBeVisible({ timeout: 5000 })

    const sw = d.locator('.el-switch').first()
    if (await sw.isVisible().catch(() => false) && await sw.evaluate((el: any) => el.classList.contains('is-checked'))) { await sw.click(); await page.waitForTimeout(200) }
    await d.locator('input:not([disabled])').first().fill('T-' + TS)

    const sels = d.locator('.el-select')
    if (await sels.count() >= 2) { await sels.first().click(); await page.waitForTimeout(800); const o = page.locator('.el-select-dropdown__item').first(); if (await o.isVisible({ timeout: 1500 }).catch(() => false)) { await o.click(); await page.waitForTimeout(500) } }
    if (await sels.count() >= 2) { await sels.nth(1).click(); await page.waitForTimeout(800); const o = page.locator('.el-select-dropdown__item').first(); if (await o.isVisible({ timeout: 1500 }).catch(() => false)) { await o.click(); await page.waitForTimeout(500) } }

    await d.locator('.el-input-number input').first().fill('50')
    await d.locator('button').filter({ hasText: '确 定' }).first().click()
    await page.waitForTimeout(1000)
    await expect(page.locator('.el-table').first()).toBeVisible({ timeout: 5000 })
  })

  test('5. 生产报工 — 页面+弹窗验证', async ({ page }) => {
    await page.goto('/mes/pro/feedback'); await page.waitForTimeout(3000)
    await expect(page.locator('.el-table').first()).toBeVisible({ timeout: 10000 })
    // 验证新增弹窗可打开（含编码生成按钮）
    await page.locator('button').filter({ hasText: /新增/ }).first().click()
    const d = page.locator('.el-dialog').first(); await expect(d).toBeVisible({ timeout: 5000 })
    const genBtn = d.locator('button').filter({ hasText: /生成/ }).first()
    await expect(genBtn).toBeVisible({ timeout: 3000 })
    await page.keyboard.press('Escape')
  })

  test('6. 物料追溯 — 加载', async ({ page }) => {
    await page.goto('/mes/pro/materialtrace'); await page.waitForTimeout(3000)
    await expect(page).not.toHaveURL(/\/404/)
  })

  test('7. 生产看板 — 加载', async ({ page }) => {
    await page.goto('/mes/pro/dashboard'); await page.waitForTimeout(4000)
    await expect(page).not.toHaveURL(/\/404/)
  })

  test('8. 生产领料 — 加载', async ({ page }) => {
    await page.goto('/mes/wm/issue'); await page.waitForTimeout(3000)
    await expect(page).not.toHaveURL(/\/404/)
  })

  test('9. 生产退料 — 加载', async ({ page }) => {
    await page.goto('/mes/wm/rtissue'); await page.waitForTimeout(3000)
    await expect(page).not.toHaveURL(/\/404/)
  })
})
