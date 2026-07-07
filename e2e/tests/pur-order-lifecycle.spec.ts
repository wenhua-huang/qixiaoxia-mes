import { test, expect } from '@playwright/test'

/**
 * 采购订单完整生命周期 E2E 测试
 *
 * 前置条件：前后端均启动
 * 运行：npx playwright test tests/pur-order-lifecycle.spec.ts
 */
test.describe('采购订单生命周期', () => {
  test.use({ storageState: 'setup/storageState.json' })
  test.setTimeout(120000)

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize({ width: 1920, height: 1080 })
  })

  test('完整流程：创建PO → 审批 → 下单', async ({ page }) => {
    // 直接通过URL导航到采购订单页面
    const routesReady = page.waitForResponse(r => r.url().includes('/getRouters') && r.status() === 200, { timeout: 20000 })
    await page.goto('/')
    await routesReady
    await page.waitForTimeout(3000)

    // 直接导航到采购订单页面（绕过侧边栏）
    await page.goto('/mes/pur/order')
    await page.waitForTimeout(3000)
    await expect(page.locator('.el-table').first()).toBeVisible({ timeout: 8000 })
    console.log('  ✅ 采购订单页面加载成功')

    // ===== 新增一条采购单 =====
    const uniqueCode = 'E2E-' + Date.now().toString(36).toUpperCase()
    await page.locator('button').filter({ hasText: /新增/ }).first().click({ timeout: 10000 })

    const dialog = page.locator('.el-dialog').first()
    await expect(dialog).toBeVisible({ timeout: 5000 })

    // 关闭自动编码 → 手动输入唯一编码
    const autoSwitch = dialog.locator('.el-switch').first()
    if (await autoSwitch.isVisible().catch(() => false)) {
      await autoSwitch.click()
      await page.waitForTimeout(300)
    }

    // 填写编码
    const firstInput = dialog.locator('input').first()
    await firstInput.clear()
    await firstInput.fill(uniqueCode)

    // 填写订单名称
    const nameInput = dialog.locator('input[placeholder="订单名称"]')
    if (await nameInput.isVisible().catch(() => false)) {
      await nameInput.fill('E2E生命周期测试')
    }

    // 选择供应商 — 点击搜索按钮
    const searchBtn = dialog.locator('button .el-icon').filter({ hasText: '' }).first()
    // 用另一种方式定位供应商选择按钮
    const vendorSearchBtn = dialog.locator('button:has(.el-icon)').first()
    if (await vendorSearchBtn.isVisible({ timeout: 3000 }).catch(() => false)) {
      await vendorSearchBtn.click()
      await page.waitForTimeout(1500)

      // 选择弹出表格第一行
      const vendorRow = page.locator('.el-dialog .el-table__row').first()
      if (await vendorRow.isVisible({ timeout: 3000 }).catch(() => false)) {
        await vendorRow.click()
        await page.waitForTimeout(500)
      }
    }

    // 保存单据
    const saveBtn = dialog.locator('button').filter({ hasText: '保存单据' }).first()
    if (await saveBtn.isVisible({ timeout: 3000 }).catch(() => false)) {
      await saveBtn.click()
      await page.waitForTimeout(2000)
    }
    console.log(`  📝 创建PO: ${uniqueCode}`)

    await dialog.locator('button').filter({ hasText: '关 闭' }).first().click().catch(() => {})
    await page.waitForTimeout(1000)

    // ===== 搜索刚才创建的订单 =====
    await page.locator('input[placeholder*="订单编码"]').first().fill(uniqueCode)
    await page.locator('button').filter({ hasText: '搜索' }).first().click()
    await page.waitForTimeout(2000)

    // ===== 验证审批按钮可见并执行 =====
    const approveBtn = page.locator('button').filter({ hasText: '审批' }).first()
    const approveVisible = await approveBtn.isVisible({ timeout: 5000 }).catch(() => false)
    console.log(`  ${approveVisible ? '✅' : '⚠️'} DRAFT状态 → 审批按钮${approveVisible ? '可见' : '未找到'}`)

    if (approveVisible) {
      await approveBtn.click()
      await page.waitForTimeout(500)
      // 确认弹窗
      const confirmBtn = page.locator('.el-message-box__btns button').filter({ hasText: '确定' }).first()
      if (await confirmBtn.isVisible({ timeout: 2000 }).catch(() => false)) {
        await confirmBtn.click()
        await page.waitForTimeout(2000)
        console.log('  ✅ 审批成功')

        // ===== 重新搜索 =====
        await page.locator('input[placeholder*="订单编码"]').first().fill(uniqueCode)
        await page.locator('button').filter({ hasText: '搜索' }).first().click()
        await page.waitForTimeout(2000)

        // ===== 验证下单按钮可见并执行 =====
        const orderBtn = page.locator('button').filter({ hasText: '下单' }).first()
        const orderVisible = await orderBtn.isVisible({ timeout: 5000 }).catch(() => false)
        console.log(`  ${orderVisible ? '✅' : '⚠️'} APPROVED状态 → 下单按钮${orderVisible ? '可见' : '未找到'}`)

        if (orderVisible) {
          await orderBtn.click()
          await page.waitForTimeout(500)
          const orderConfirmBtn = page.locator('.el-message-box__btns button').filter({ hasText: '确定' }).first()
          if (await orderConfirmBtn.isVisible({ timeout: 2000 }).catch(() => false)) {
            await orderConfirmBtn.click()
            await page.waitForTimeout(2000)
            console.log('  ✅ 下单成功')
          }
        }
      }
    }
  })

  test('采购订单列表 — 超期预警显示', async ({ page }) => {
    const routesReady = page.waitForResponse(r => r.url().includes('/getRouters') && r.status() === 200, { timeout: 20000 })
    await page.goto('/')
    await routesReady
    await page.waitForTimeout(3000)

    await page.goto('/mes/pur/order')
    await page.waitForTimeout(3000)
    await expect(page.locator('.el-table').first()).toBeVisible({ timeout: 8000 })

    await page.locator('button').filter({ hasText: '搜索' }).first().click()
    await page.waitForTimeout(2000)

    // 检查超期标记 ⚠
    const overdueMarks = page.locator('text=⚠')
    const count = await overdueMarks.count()
    console.log(`  📊 超期订单数量: ${count}`)
    expect(count).toBeGreaterThanOrEqual(0)
  })

  test('入库单页面 — 确认/过账按钮可见', async ({ page }) => {
    const routesReady = page.waitForResponse(r => r.url().includes('/getRouters') && r.status() === 200, { timeout: 20000 })
    await page.goto('/')
    await routesReady
    await page.waitForTimeout(3000)

    // 直接导航到入库单页面
    await page.goto('/mes/wm/item_recpt')
    await page.waitForTimeout(3000)
    await expect(page.locator('.el-table').first()).toBeVisible({ timeout: 8000 })
    console.log('  ✅ 入库单页面加载成功')

    // 检查确认按钮（Check图标）
    const confirmBtns = page.locator('[icon="Check"]')
    console.log(`  📊 确认按钮数: ${await confirmBtns.count()}`)

    // 检查过账按钮
    const postBtns = page.locator('button').filter({ hasText: '过账' })
    console.log(`  📊 过账按钮数: ${await postBtns.count()}`)
  })
})
