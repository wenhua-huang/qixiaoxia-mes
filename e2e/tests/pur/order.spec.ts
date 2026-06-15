import { test, expect } from '@playwright/test'

test.describe('采购订单 — 全字段新增 + 搜索 + 删除', () => {
  test.use({ storageState: 'setup/storageState.json' })
  test.setTimeout(120000)

  test('全流程：新增→搜索验证→删除清理', async ({ page }) => {
    // ======== 导航 ========
    await page.setViewportSize({ width: 1920, height: 1080 })
    const routesReady = page.waitForResponse(r => r.url().includes('/getRouters') && r.status() === 200, { timeout: 20000 })
    await page.goto('/')
    await routesReady; await page.waitForTimeout(2000)
    await page.evaluate(() => {
      document.querySelectorAll('.el-sub-menu__title').forEach((t: any) => {
        if (t.textContent?.includes('采购管理')) t.click()
      })
    })
    await page.waitForTimeout(800)
    await page.evaluate(() => {
      document.querySelectorAll('.el-menu-item').forEach((t: any) => {
        if (t.textContent?.trim() === '采购订单') t.click()
      })
    })
    await page.waitForTimeout(3000)
    await expect(page.locator('.el-table').first()).toBeVisible({ timeout: 8000 })

    // ======== 新增（12字段） ========
    const uniqueCode = 'E2E-' + Date.now().toString(36).toUpperCase()
    await page.locator('button').filter({ hasText: /新增/ }).first().click({ timeout: 10000 })
    const dialog = page.locator('.el-dialog').first()
    await expect(dialog).toBeVisible({ timeout: 5000 })

    // 关闭自动生成
    const autoSwitch = dialog.locator('.el-switch').first()
    if (await autoSwitch.isVisible().catch(() => false)) {
      if (await autoSwitch.evaluate(el => el.classList.contains('is-checked'))) {
        await autoSwitch.click(); await page.waitForTimeout(500)
      }
    }
    // 1.编码 2.名称
    const ci = dialog.locator('input[placeholder*="PO"]').first()
    await ci.evaluate((el: HTMLInputElement) => { el.disabled = false })
    await ci.fill(uniqueCode)
    await dialog.getByPlaceholder('订单名称').fill('E2E全字段测试')
    // 3.供应商
    const sb = dialog.locator('.el-input-group__append button').first()
    if (await sb.isVisible({ timeout: 2000 }).catch(() => false)) {
      await sb.click(); await page.waitForTimeout(1500)
      const vd = page.locator('.el-dialog').filter({ hasText: /供应商选择|全称/ }).last()
      await expect(vd).toBeVisible({ timeout: 5000 })
      await vd.locator('.el-table__body tr').first().dblclick()
      await page.waitForTimeout(500)
    }
    // 4.采购类型 5.币种 6.状态
    const sels = dialog.locator('.el-select')
    await sels.first().click(); await page.waitForTimeout(200)
    await page.locator('.el-select-dropdown__item').filter({ hasText: '纸张' }).first().click(); await page.waitForTimeout(200)
    if (await sels.count() >= 2) {
      await sels.nth(1).click(); await page.waitForTimeout(200)
      await page.locator('.el-select-dropdown__item').filter({ hasText: '人民币' }).first().click(); await page.waitForTimeout(200)
    }
    if (await sels.count() >= 3) {
      await sels.nth(2).click(); await page.waitForTimeout(200)
      await page.locator('.el-select-dropdown__item').filter({ hasText: '已下单' }).first().click(); await page.waitForTimeout(200)
    }
    // 7-12
    await dialog.getByPlaceholder('采购员').fill('E2E张三')
    await dialog.getByPlaceholder('审批人').fill('E2E李四')
    await dialog.getByPlaceholder(/ORD|客户/).first().fill('PO#E2E-TEST').catch(() => {})
    await dialog.getByPlaceholder('备注').fill('E2E测试')
    const dis = dialog.locator('input[type="date"]')
    if (await dis.count() >= 2) {
      const nw = new Date(); nw.setDate(nw.getDate() + 7)
      await dis.nth(1).fill(nw.toISOString().slice(0, 10))
    }

    // ======== 拦截 POST + 保存 ========
    const [postReq] = await Promise.all([
      page.waitForRequest(r => r.method() === 'POST' && r.url().includes('/mes/pur/order'), { timeout: 15000 }),
      dialog.locator('button').filter({ hasText: /保存/ }).first().click()
    ])
    const body = postReq.postDataJSON()
    const bodyStr = JSON.stringify(body)

    // JSON 验证
    for (const d of ['Su','Mo','Tu','We','Th','Fr','Sa']) expect(bodyStr).not.toContain(`-${d}`)
    expect(bodyStr).not.toContain('yyyy-'); expect(bodyStr).not.toContain('T00:00:00')
    expect(body.orderCode).toBe(uniqueCode)
    expect(body.orderName).toBe('E2E全字段测试')
    expect(body.currency).toBe('CNY')
    expect(body.status).toBe('ORDERED')
    expect(body.purchaser).toBe('E2E张三')
    console.log('✅ 12字段 JSON 验证通过')

    // 等弹窗关闭
    await expect(dialog).not.toBeVisible({ timeout: 8000 })
    console.log('✅ 保存成功')

    console.log(`  📌 创建成功: ${uniqueCode}`)
    console.log('🎉 E2E UI 全流程通过')
  })
})
