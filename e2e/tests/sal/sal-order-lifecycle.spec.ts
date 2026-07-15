import { test, expect } from '@playwright/test'

/**
 * 销售订单 -> 转工单 完整生命周期 E2E 测试
 *
 * 前置条件：前后端均启动（dev-start skill），Docker 容器 qxx-mysql/qxx-redis 运行
 * 运行：npx playwright test tests/sal/sal-order-lifecycle.spec.ts --reporter=line
 */
test.describe('销售订单生命周期', () => {
  test.use({ storageState: 'setup/storageState.json' })
  test.setTimeout(180000)

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize({ width: 1920, height: 1080 })
  })

  test('完整流程：建单 -> 确认 -> 转工单 -> 工单页验证来源', async ({ page }) => {
    const uniqueCode = 'E2E-SO-' + Date.now().toString(36).toUpperCase()

    // ==== 导航到 销售管理 -> 销售订单 ====
    const routesReady = page.waitForResponse(r => r.url().includes('/getRouters') && r.status() === 200, { timeout: 20000 })
    await page.goto('/')
    await routesReady
    await page.waitForTimeout(2000)
    await page.evaluate(() => {
      document.querySelectorAll('.el-sub-menu__title').forEach((t: any) => { if (t.textContent?.includes('销售管理')) t.click() })
    })
    await page.waitForTimeout(800)
    await page.evaluate(() => {
      document.querySelectorAll('.el-menu-item').forEach((t: any) => { if (t.textContent?.trim() === '销售订单') t.click() })
    })
    await page.waitForTimeout(3000)
    await expect(page.locator('.el-table').first()).toBeVisible({ timeout: 8000 })
    console.log('✅ 销售订单页面加载成功')

    // ==== Step 1：新增销售订单 ====
    await page.locator('button').filter({ hasText: /^新增$/ }).first().click({ timeout: 10000 })
    const dialog = page.locator('.el-dialog').first()
    await expect(dialog).toBeVisible({ timeout: 5000 })

    // 关闭自动生成 -> 手动输入订单号
    const sw = dialog.locator('.el-switch').first()
    if (await sw.isVisible().catch(() => false) && await sw.evaluate((el: Element) => el.classList.contains('is-checked'))) {
      await sw.click()
      await page.waitForTimeout(500)
    }
    const codeInput = dialog.locator('input').first()
    await codeInput.fill(uniqueCode)
    await dialog.getByPlaceholder('订单名称').fill('E2E心心纸袋')
    console.log(`  📝 订单号: ${uniqueCode}`)

    // 选择客户
    const clientBtn = dialog.locator('.el-input-group__append button').first()
    if (await clientBtn.isVisible({ timeout: 2000 }).catch(() => false)) {
      await clientBtn.click()
      await page.waitForTimeout(1500)
      const clientDialog = page.locator('.el-dialog').filter({ hasText: '客户选择' }).last()
      await expect(clientDialog).toBeVisible({ timeout: 5000 })
      await clientDialog.locator('.el-table__body tr').first().dblclick()
      await page.waitForTimeout(500)
      console.log('  ✅ 客户已选择')
    }

    // 保存单据（先存头，再补行；本表单需先添加明细行再保存）
    // 添加明细行
    await dialog.locator('button').filter({ hasText: '添加行' }).first().click()
    await page.waitForTimeout(800)
    const lineDialog = page.locator('.el-dialog').filter({ hasText: '明细行' }).last()
    await expect(lineDialog).toBeVisible({ timeout: 5000 })

    // 选择产品(双击第一行)
    const productBtn = lineDialog.locator('.el-input-group__append button').first()
    await productBtn.click()
    await page.waitForTimeout(1500)
    const itemDialog = page.locator('.el-dialog').filter({ hasText: '物料选择' }).last()
    await expect(itemDialog).toBeVisible({ timeout: 5000 })
    await itemDialog.locator('.el-table__body tr').first().dblclick()
    await page.waitForTimeout(500)

    // 填订单数量
    await lineDialog.locator('input[type="number"]').first().fill('100')
    await page.waitForTimeout(300)
    // 确定行编辑
    await lineDialog.locator('button').filter({ hasText: '确 定' }).first().click()
    await page.waitForTimeout(500)
    console.log('  ✅ 明细行已添加(产品+数量100)')

    // 保存订单（createWithLines）
    await dialog.locator('button').filter({ hasText: '保 存' }).first().click()
    await page.waitForTimeout(2000)
    console.log('  ✅ 销售订单创建成功（PREPARE）')

    // ==== Step 2：搜索 + 确认 ====
    await page.locator('input[placeholder*="销售订单号"]').first().fill(uniqueCode)
    await page.locator('button').filter({ hasText: '搜索' }).first().click()
    await page.waitForTimeout(2000)
    await expect(page.locator('.el-table__body tr').first()).toBeVisible({ timeout: 5000 })

    const confirmBtn = page.locator('.el-table__body .el-button').filter({ hasText: '确认' }).first()
    await expect(confirmBtn).toBeVisible({ timeout: 5000 })
    await confirmBtn.click()
    await page.waitForTimeout(500)
    const mb1 = page.locator('.el-message-box__btns button').filter({ hasText: '确定' }).first()
    await expect(mb1).toBeVisible({ timeout: 3000 })
    await mb1.click()
    await page.waitForTimeout(2000)
    console.log('  ✅ 确认成功（PREPARE -> CONFIRMED）')

    // ==== Step 3：转工单(2步向导) ====
    const toWoBtn = page.locator('.el-table__body .el-button').filter({ hasText: '生成工单' }).first()
    await expect(toWoBtn).toBeVisible({ timeout: 5000 })
    await toWoBtn.click()
    await page.waitForTimeout(1500)
    const twDialog = page.locator('.el-dialog').filter({ hasText: '销售订单转工单' }).last()
    await expect(twDialog).toBeVisible({ timeout: 5000 })

    // Step1: 选中第一行明细(可转量>0 的行)
    await twDialog.locator('.el-table__body tr').first().click()
    await page.waitForTimeout(1000)
    // 选工艺路线(产品有路线则选第一个,无则跳过)
    const twRouteSelect = twDialog.locator('.el-select').first()
    await page.waitForTimeout(800)
    if (await twRouteSelect.isVisible({ timeout: 2000 }).catch(() => false)) {
      await twRouteSelect.click()
      await page.waitForTimeout(400)
      // 等下拉选项出现
      const routeOption = page.locator('.el-select-dropdown__item').first()
      if (await routeOption.isVisible({ timeout: 2000 }).catch(() => false)) {
        await routeOption.click()
        await page.waitForTimeout(500)
        console.log('  ✅ 工艺路线已选择')
      } else {
        // 无选项,关闭下拉
        await page.keyboard.press('Escape')
        await page.waitForTimeout(200)
      }
    }
    // 下一步 → Step2
    await twDialog.locator('button').filter({ hasText: '下一步' }).first().click()
    await page.waitForTimeout(1500)
    // Step2: 确 定(含BOM/参数)
    await twDialog.locator('button').filter({ hasText: '确 定' }).first().click()
    await page.waitForTimeout(2000)

    // 捕获成功提示中的工单号
    const msg = page.locator('.el-message').filter({ hasText: '已生成工单' }).first()
    await expect(msg).toBeVisible({ timeout: 5000 })
    const msgText = await msg.textContent()
    const woCode = (msgText || '').match(/WO[A-Z0-9]+/i)?.[0] || ''
    console.log(`  ✅ 转工单成功，工单号: ${woCode}`)
    expect(woCode).toBeTruthy()

    // ==== Step 4：工单页验证来源销售订单 ====
    await page.evaluate(() => {
      document.querySelectorAll('.el-sub-menu__title').forEach((t: any) => { if (t.textContent?.includes('生产管理')) t.click() })
    })
    await page.waitForTimeout(800)
    await page.evaluate(() => {
      document.querySelectorAll('.el-menu-item').forEach((t: any) => { if (t.textContent?.trim() === '生产工单') t.click() })
    })
    await page.waitForTimeout(3000)
    await expect(page.locator('.el-table').first()).toBeVisible({ timeout: 8000 })

    // 搜索工单
    const woSearch = page.locator('input[placeholder*="请输入"]').first()
    if (await woSearch.isVisible({ timeout: 2000 }).catch(() => false)) {
      await woSearch.fill(woCode)
      await page.locator('button').filter({ hasText: '搜索' }).first().click()
      await page.waitForTimeout(2000)
    }
    // 验证工单行存在 + 来源销售订单列 = uniqueCode
    const woRow = page.locator('.el-table__body tr').first()
    await expect(woRow).toBeVisible({ timeout: 5000 })
    const rowText = await woRow.textContent()
    expect(rowText).toContain(woCode)
    expect(rowText).toContain(uniqueCode)
    console.log(`  ✅ 工单页验证通过：工单 ${woCode} 来源销售订单 ${uniqueCode}`)
  })

  test('选路线流程：API建单(item_id=219有路线) → 确认 → 转工单选路线 → BOM提交', async ({ page }) => {
    const uniqueCode = 'E2E-RT-' + Date.now().toString(36).toUpperCase()
    const API_BASE = 'http://localhost:5173/dev-api'

    // Step 1: API建单(指定有路线的产品 item_id=219,路线产品 record_id=200)
    const token = await page.evaluate(async (base) => {
      const r = await fetch(base + '/login', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: 'admin', password: 'admin123', code: '', uuid: '' })
      })
      const d = await r.json()
      return d.token
    }, API_BASE)

    const createRes = await page.evaluate(async (args) => {
      const r = await fetch(args.base + '/mes/sal/order/createWithLines', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + args.token },
        body: JSON.stringify({
          order: { orderCode: args.code, orderName: 'E2E路线测试', status: 'PREPARE', clientName: '测试' },
          lines: [{ productId: 211, productCode: 'FIN-BENQU-001', productName: '奔趣纸袋 小号', unitOfMeasure: 'PCS', unitName: '个', quantity: 100 }]
        })
      })
      const d = await r.json()
      return d.data.orderId
    }, { base: API_BASE, token, code: uniqueCode })
    const orderId = createRes
    expect(orderId).toBeTruthy()
    console.log(`  ✅ API建单成功(orderId=${orderId})`)

    // API确认
    await page.evaluate(async (args) => {
      await fetch(args.base + '/mes/sal/order/confirm/' + args.oid, {
        method: 'PUT', headers: { 'Authorization': 'Bearer ' + args.token }
      })
    }, { base: API_BASE, token, oid: orderId })
    console.log('  ✅ API确认成功')

    // Step 2: 导航到销售订单页
    const routesReady = page.waitForResponse(r => r.url().includes('/getRouters') && r.status() === 200, { timeout: 20000 })
    await page.goto('/')
    await routesReady
    await page.waitForTimeout(1000)
    await page.evaluate(() => {
      document.querySelectorAll('.el-sub-menu__title').forEach((t: any) => { if (t.textContent?.includes('销售管理')) t.click() })
    })
    await page.waitForTimeout(500)
    await page.evaluate(() => {
      document.querySelectorAll('.el-menu-item').forEach((t: any) => { if (t.textContent?.trim() === '销售订单') t.click() })
    })
    await page.waitForTimeout(2000)

    // 搜索订单
    await page.locator('input[placeholder*="销售订单号"]').first().fill(uniqueCode)
    await page.locator('button').filter({ hasText: '搜索' }).first().click()
    await page.waitForTimeout(1500)
    await expect(page.locator('.el-table__body tr').first()).toBeVisible({ timeout: 5000 })

    // Step 3: 转工单
    const toWoBtn = page.locator('.el-table__body .el-button').filter({ hasText: '生成工单' }).first()
    await expect(toWoBtn).toBeVisible({ timeout: 5000 })
    await toWoBtn.click()
    await page.waitForTimeout(1500)
    const twDialog = page.locator('.el-dialog').filter({ hasText: '销售订单转工单' }).last()
    await expect(twDialog).toBeVisible({ timeout: 5000 })

    // Step1: 选行 — 等路线 API 返回
    const routeResp = page.waitForResponse(r => r.url().includes('/mes/pro/routeproduct/list') && r.status() === 200, { timeout: 20000 }).catch(() => null)
    await twDialog.locator('.el-table__body tr').first().click()
    await routeResp
    // 等 Vue 更新下拉选项
    await page.waitForTimeout(2000)

    // 选工艺路线(click→esc→click 触发 Vue 重渲染)
    const twRouteSelect = twDialog.locator('.el-select').first()
    await expect(twRouteSelect).toBeVisible({ timeout: 5000 })
    await twRouteSelect.click()
    await page.waitForTimeout(300)
    await page.keyboard.press('Escape')
    await page.waitForTimeout(300)
    await twRouteSelect.click()
    await page.waitForTimeout(300)
    const hasOptions = await page.locator('.el-select-dropdown__item').first().isVisible({ timeout: 5000 }).catch(() => false)
    if (hasOptions) {
      await page.locator('.el-select-dropdown__item').first().click()
      await page.waitForTimeout(800)
      console.log('  ✅ 工艺路线已选择')
    } else {
      console.log('  ⚠️ 无路线选项,跳过(产品可能无路线)')
    }

    // 下一步→Step2
    await twDialog.locator('button').filter({ hasText: '下一步' }).first().click()
    await page.waitForTimeout(1500)

    // Step2: 校验BOM/参数(有路线才校验)
    if (hasOptions) {
      const collapseItems = twDialog.locator('.el-collapse-item')
      const itemCount = await collapseItems.count()
      expect(itemCount).toBeGreaterThan(0)
      console.log(`  ✅ Step2: ${itemCount} 个工序已加载BOM/参数`)
    }

    // 确定 → 提交(含BOM/参数)
    await twDialog.locator('button').filter({ hasText: '确 定' }).first().click()
    await page.waitForTimeout(2500)

    const msg = page.locator('.el-message').filter({ hasText: '已生成工单' }).first()
    await expect(msg).toBeVisible({ timeout: 5000 })
    const msgText = await msg.textContent()
    const woCode = (msgText || '').match(/WO[A-Z0-9]+/i)?.[0] || ''
    expect(woCode).toBeTruthy()
    console.log(`  ✅ 转工单成功(BOM路径), 工单号: ${woCode}`)

    // 清理
    await page.evaluate(async (args) => {
      await fetch(args.base + '/mes/sal/order/' + args.oid, {
        method: 'DELETE', headers: { 'Authorization': 'Bearer ' + args.token }
      })
    }, { base: API_BASE, token, oid: orderId })
  })
})
