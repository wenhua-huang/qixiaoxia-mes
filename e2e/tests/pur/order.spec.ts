import { test, expect } from '@playwright/test'

test.describe('采购订单管理', () => {
  test.use({ storageState: 'setup/storageState.json' })

  test('新增采购订单 — 提交JSON验证(无Date对象/无乱码)', async ({ page }) => {
    // 1. 访问首页，等待菜单加载
    await page.goto('/')
    await page.waitForTimeout(3000)

    // 2. 展开采购管理菜单（侧边栏可能是折叠的）
    const purMenu = page.locator('.menu-title, .el-menu-item, span').filter({ hasText: '采购管理' })
    if (await purMenu.isVisible({ timeout: 3000 }).catch(() => false)) {
      await purMenu.first().click()
      await page.waitForTimeout(500)
    }

    // 3. 点击采购订单
    const orderMenu = page.locator('.menu-title, .el-menu-item, span').filter({ hasText: '采购订单' })
    if (await orderMenu.isVisible({ timeout: 3000 }).catch(() => false)) {
      await orderMenu.first().click()
      await page.waitForTimeout(2000)
    }

    // 4. 截图
    await page.screenshot({ path: 'test-results/pur-order-page.png' })

    // 5. 点击新增
    const addBtn = page.getByRole('button', { name: /新增|添加/ }).first()
    await addBtn.click({ timeout: 5000 })
    await expect(page.locator('.el-dialog, .el-dialog__wrapper').first()).toBeVisible({ timeout: 5000 })
    await page.waitForTimeout(500)

    // 6. 截图弹窗
    await page.screenshot({ path: 'test-results/pur-order-dialog.png' })

    // 7. 拦截提交请求 + 点击保存
    const [request] = await Promise.all([
      page.waitForRequest(req => req.method() === 'POST' && req.url().includes('/mes/pur/order'), { timeout: 10000 }),
      page.locator('.el-dialog .el-button').filter({ hasText: /保存|确定|提交/ }).first().click()
    ]).catch(async (e) => {
      // 如果没拦截到，可能是因为校验没通过，截图看看
      await page.screenshot({ path: 'test-results/pur-order-submit-fail.png' })
      throw e
    })

    const body = request.postDataJSON()
    console.log('  POST /mes/pur/order body:', JSON.stringify(body, null, 2))

    // === 验证 JSON 正确性 ===
    const bodyStr = JSON.stringify(body)

    // orderDate 必须为空或 yyyy-MM-dd 格式，不能是 Date 对象序列化产物
    if (body.orderDate) {
      expect(body.orderDate).toMatch(/^\d{4}-\d{2}-\d{2}$/)
    }
    // 不能包含格式字面量（el-date-picker value-format 泄露的 bug）
    expect(bodyStr).not.toContain('yyyy-')
    expect(bodyStr).not.toContain('-Su')
    // 不能包含 ISO 时间戳（new Date() 序列化的产物）
    expect(bodyStr).not.toContain('T00:00:00')
    // orderCode 应该是 PO 开头
    if (body.orderCode) {
      expect(body.orderCode).toMatch(/^PO/)
      console.log(`  ✅ orderCode=${body.orderCode}`)
    }

    console.log('  ✅ 所有 JSON 验证通过')
  })
})
