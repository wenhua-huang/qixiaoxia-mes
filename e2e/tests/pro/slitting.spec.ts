import { test, expect } from '@playwright/test'

/**
 * 分切工序 E2E 测试（库存驱动版）
 *
 * 覆盖：
 *  1. 列表页导航 + 分切批次号搜索
 *  2. 执行分切全链路（选物料 → 选库存批次 → 填领料量 → 录子卷 → 确认提交）
 *  3. 重量校验边界（子卷+纸边超领料量 → 按钮禁用）
 *  4. 分切详情查看（子卷明细）
 *
 * 前置条件：前后端均启动（dev-start skill），Docker 容器 qxx-mysql/qxx-redis 运行
 * 运行：cd e2e && npx playwright test tests/pro/slitting.spec.ts --reporter=line
 */

const API_BASE = 'http://localhost:5173/dev-api'

/** 用 admin 登录拿 token */
async function getToken(page: import('@playwright/test').Page): Promise<string> {
  return await page.evaluate(async (base) => {
    const r = await fetch(base + '/login', {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: 'admin', password: 'admin123', code: '', uuid: '' })
    })
    return (await r.json()).token
  }, API_BASE)
}

/** API 封装：带 token 的请求 */
async function api(page: import('@playwright/test').Page, token: string, method: string, path: string, body?: any) {
  return await page.evaluate(async (args) => {
    const r = await fetch(args.base + args.path, {
      method: args.method,
      headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + args.token },
      body: args.body ? JSON.stringify(args.body) : undefined
    })
    return await r.json()
  }, { base: API_BASE, token, method, path, body })
}

/** 找一个有在库库存的物料（itemId + 第一条库存批次） */
async function findItemWithStock(page: import('@playwright/test').Page, token: string): Promise<{ item: any; stock: any } | null> {
  const itemRes = await api(page, token, 'GET', '/mes/md/item/list?pageNum=1&pageSize=50')
  const items = itemRes.rows || []
  for (const item of items) {
    const stockRes = await api(page, token, 'GET', `/mes/pro/slitting/availableStock?itemId=${item.itemId}`)
    const stocks = stockRes.data || []
    const usable = stocks.find((s: any) => Number(s.quantityAvailable) >= 1)
    if (usable) return { item, stock: usable }
  }
  return null
}

test.describe('分切工序', () => {
  test.use({ storageState: 'setup/storageState.json' })
  test.setTimeout(120_000)

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize({ width: 1920, height: 1080 })
  })

  // ════════════════════════════════════════════════════════════════
  // Test 1: 列表页导航 + 分切批次号搜索
  // ════════════════════════════════════════════════════════════════
  test('S1: 列表页导航 + 分切批次号搜索', async ({ page }) => {
    const [listResp] = await Promise.all([
      page.waitForResponse(
        r => r.url().includes('/mes/pro/slitting/list') && r.status() === 200,
        { timeout: 15_000 }
      ),
      page.goto('/mes/pro/slitting')
    ])

    await expect(page.locator('.el-table').first()).toBeVisible({ timeout: 10_000 })
    const execBtn = page.locator('button').filter({ hasText: '执行分切' }).first()
    await expect(execBtn).toBeVisible()

    const listBody = await listResp.json()
    console.log(`  📋 分切记录列表: ${listBody.total || 0} 条`)

    if (listBody.total > 0 && listBody.rows.length > 0) {
      const knownBatchNo = listBody.rows[0].slitBatchNo
      const batchInput = page.locator('.el-form-item').filter({ hasText: '分切批次号' }).locator('input').first()
      await batchInput.fill(knownBatchNo)

      const [filteredResp] = await Promise.all([
        page.waitForResponse(
          r => r.url().includes('/mes/pro/slitting/list') && r.status() === 200,
          { timeout: 10_000 }
        ),
        page.locator('button').filter({ hasText: '搜索' }).first().click()
      ])

      const filterUrl = new URL(filteredResp.url())
      expect(filterUrl.searchParams.get('slitBatchNo')).toBe(knownBatchNo)
      const filteredBody = await filteredResp.json()
      expect(filteredBody.total).toBeGreaterThanOrEqual(1)
      console.log(`  ✅ 批次号搜索生效: ${filteredBody.total} 条匹配`)
    } else {
      console.log('  ⚠️ 无分切记录，跳过搜索验证')
    }
  })

  // ════════════════════════════════════════════════════════════════
  // Test 2: 执行分切全链路（三步向导核心）
  // ════════════════════════════════════════════════════════════════
  test('S2: 执行分切全链路 -> 选物料→选库存→领料→录子卷→提交', async ({ page }) => {
    const token = await getToken(page)
    const found = await findItemWithStock(page, token)
    if (!found) {
      console.log('  ⚠️ 无有库存物料，跳过执行分切测试')
      test.skip()
      return
    }
    const { item, stock } = found
    console.log(`  📝 选中物料: ${item.itemName} | 批次: ${stock.batchCode} | 可用: ${stock.quantityAvailable}${stock.unitOfMeasure}`)

    await page.goto('/mes/pro/slitting')
    await expect(page.locator('.el-table').first()).toBeVisible({ timeout: 15_000 })

    // 打开执行分切弹窗
    await page.locator('button').filter({ hasText: '执行分切' }).first().click()
    const dialog = page.locator('.el-dialog').filter({ hasText: '执行分切' }).last()
    await expect(dialog).toBeVisible({ timeout: 5_000 })
    // 验证三步向导
    await expect(dialog.locator('.el-step__title').filter({ hasText: '领料出库' })).toBeVisible()
    console.log('  ✅ 执行分切弹窗已打开（三步向导）')

    // ==== 步骤1：选物料（点击母卷物料输入框右侧搜索按钮）====
    const itemInput = dialog.locator('.el-form-item').filter({ hasText: '母卷物料' })
    await itemInput.locator('.el-input-group__append button').click()

    // 物料选择弹窗
    const itemDialog = page.locator('.el-dialog').filter({ hasText: '物料选择' }).last()
    await expect(itemDialog).toBeVisible({ timeout: 5_000 })
    // 双击第一行选中（handleRowDbClick 直接 emit onSelected）
    const firstItemRow = itemDialog.locator('.el-table__body-wrapper tbody tr').first()
    await firstItemRow.dblclick()
    await expect(itemDialog).not.toBeVisible({ timeout: 3_000 })
    console.log('  ✅ 物料已选择')

    // 等待在库库存表格加载，选中第一条库存（点击 radio）
    await expect(dialog.locator('.el-form-item').filter({ hasText: '在库库存' })).toBeVisible({ timeout: 5_000 })
    const firstStockRadio = dialog.locator('.el-form-item').filter({ hasText: '在库库存' })
      .locator('.el-table__body-wrapper tbody tr').first().locator('.el-radio')
    await firstStockRadio.click()
    await page.waitForTimeout(300)

    // 填领料数量 1.0 吨
    const pickInput = dialog.locator('.el-form-item').filter({ hasText: '领料数量' }).locator('input')
    await pickInput.fill('1')
    await pickInput.press('Tab')
    await page.waitForTimeout(300)
    console.log('  ✅ 库存批次已选，领料量=1.0吨')

    // 下一步
    await dialog.locator('.el-dialog__footer button').filter({ hasText: '下一步' }).click()

    // ==== 步骤2：分切方案 ====
    await expect(dialog.locator('.el-descriptions').filter({ hasText: '领料物料' })).toBeVisible({ timeout: 3_000 })

    // 添加2个子卷（next() 已自动加1行，再点一次添加）
    await dialog.locator('button').filter({ hasText: '添加子卷' }).first().click()
    await page.waitForTimeout(200)
    const childRows = dialog.locator('.el-table__body-wrapper tbody tr')
    await expect(childRows).toHaveCount(2, { timeout: 3_000 })

    // 填子卷重量 0.49 + 0.49 = 0.98，损耗 2%
    const weightInputs = dialog.locator('.el-table__body-wrapper tbody tr .el-input-number input')
    await weightInputs.nth(0).fill('0.49')
    await weightInputs.nth(0).press('Tab')
    await page.waitForTimeout(200)
    await weightInputs.nth(1).fill('0.49')
    await weightInputs.nth(1).press('Tab')
    await page.waitForTimeout(500)
    console.log('  ✅ 子卷规格已填（0.49 + 0.49 = 0.98吨）')

    // 下一步
    await dialog.locator('.el-dialog__footer button').filter({ hasText: '下一步' }).click()

    // ==== 步骤3：确认提交 ====
    const alert = dialog.locator('.el-alert').first()
    await expect(alert).toBeVisible({ timeout: 3_000 })
    await expect(alert).toHaveClass(/el-alert--success/, { timeout: 3_000 })
    console.log('  ✅ 重量校验通过（el-alert success）')

    // 确认分切
    const [executeResp] = await Promise.all([
      page.waitForResponse(
        r => r.url().includes('/mes/pro/slitting/execute') && r.status() === 200,
        { timeout: 30_000 }
      ),
      dialog.locator('.el-dialog__footer button').filter({ hasText: '确认分切' }).click()
    ])

    const executeBody = await executeResp.json()
    expect(executeBody.code).toBe(200)
    expect(executeBody.data).toBeTruthy()
    expect(executeBody.data.slitBatchNo).toBeTruthy()
    expect(executeBody.data.childRolls.length).toBe(2)
    console.log(`  ✅ 分切成功! 批次号=${executeBody.data.slitBatchNo}, 子卷数=${executeBody.data.childRolls.length}`)

    // 弹窗关闭 + 列表刷新
    await expect(dialog).not.toBeVisible({ timeout: 5_000 })
    await page.waitForResponse(
      r => r.url().includes('/mes/pro/slitting/list') && r.status() === 200,
      { timeout: 10_000 }
    )
    console.log('  ✅ 列表已刷新')
  })

  // ════════════════════════════════════════════════════════════════
  // Test 3: 重量校验边界（子卷总重超领料量 → 按钮禁用）
  // ════════════════════════════════════════════════════════════════
  test('S3: 重量校验边界 - 子卷超领料量 -> 按钮禁用', async ({ page }) => {
    const token = await getToken(page)
    const found = await findItemWithStock(page, token)
    if (!found) { test.skip(); return }

    await page.goto('/mes/pro/slitting')
    await expect(page.locator('.el-table').first()).toBeVisible({ timeout: 15_000 })
    await page.locator('button').filter({ hasText: '执行分切' }).first().click()
    const dialog = page.locator('.el-dialog').filter({ hasText: '执行分切' }).last()
    await expect(dialog).toBeVisible({ timeout: 5_000 })

    // 选物料
    await dialog.locator('.el-form-item').filter({ hasText: '母卷物料' }).locator('.el-input-group__append button').click()
    const itemDialog = page.locator('.el-dialog').filter({ hasText: '物料选择' }).last()
    await expect(itemDialog).toBeVisible({ timeout: 5_000 })
    await itemDialog.locator('.el-table__body-wrapper tbody tr').first().dblclick()
    await expect(itemDialog).not.toBeVisible({ timeout: 3_000 })

    // 选库存 + 领料 1.0 吨
    await expect(dialog.locator('.el-form-item').filter({ hasText: '在库库存' })).toBeVisible({ timeout: 5_000 })
    await dialog.locator('.el-form-item').filter({ hasText: '在库库存' })
      .locator('.el-table__body-wrapper tbody tr').first().locator('.el-radio').click()
    const pickInput = dialog.locator('.el-form-item').filter({ hasText: '领料数量' }).locator('input')
    await pickInput.fill('1')
    await pickInput.press('Tab')
    await page.waitForTimeout(300)

    // 下一步到方案
    await dialog.locator('.el-dialog__footer button').filter({ hasText: '下一步' }).click()
    await expect(dialog.locator('button').filter({ hasText: '添加子卷' })).toBeVisible({ timeout: 3_000 })

    // 填超重子卷 1.5 吨（超过领料 1.0 吨）
    const weightInput = dialog.locator('.el-table__body-wrapper tbody tr .el-input-number input').first()
    await weightInput.fill('1.5')
    await weightInput.press('Tab')
    await page.waitForTimeout(500)

    // 下一步到确认
    await dialog.locator('.el-dialog__footer button').filter({ hasText: '下一步' }).click()
    const alert = dialog.locator('.el-alert').first()
    await expect(alert).toHaveClass(/el-alert--error/, { timeout: 3_000 })

    // 确认分切按钮应禁用
    const submitBtn = dialog.locator('.el-dialog__footer button').filter({ hasText: '确认分切' })
    await expect(submitBtn).toBeDisabled({ timeout: 3_000 })
    console.log('  ✅ 重量超限时 el-alert=error 且"确认分切"按钮禁用')

    // 关闭弹窗（不提交）
    await dialog.locator('.el-dialog__footer button').filter({ hasText: '取消' }).click()
    await expect(dialog).not.toBeVisible({ timeout: 3_000 })
  })

  // ════════════════════════════════════════════════════════════════
  // Test 4: 分切详情查看
  // ════════════════════════════════════════════════════════════════
  test('S4: 分切详情查看 - 子卷明细', async ({ page }) => {
    await page.goto('/mes/pro/slitting')
    const listResp = await page.waitForResponse(
      r => r.url().includes('/mes/pro/slitting/list') && r.status() === 200,
      { timeout: 15_000 }
    )
    const listBody = await listResp.json()
    if (listBody.total === 0 || !listBody.rows?.length) { test.skip(); return }

    // 点第一行"详情"
    const firstRow = page.locator('.el-table__body-wrapper tbody tr').first()
    const detailBtn = firstRow.locator('button').filter({ hasText: '详情' }).first()
    const [detailResp] = await Promise.all([
      page.waitForResponse(
        r => r.url().includes('/mes/pro/slitting/') && !r.url().includes('/list') && r.status() === 200,
        { timeout: 10_000 }
      ),
      detailBtn.click()
    ])

    const detailBody = await detailResp.json()
    expect(detailBody.code).toBe(200)
    expect(detailBody.data.slitBatchNo).toBeTruthy()
    console.log(`  📝 查看详情: 批次号=${detailBody.data.slitBatchNo}`)

    const detailDialog = page.locator('.el-dialog').filter({ hasText: '分切详情' }).last()
    await expect(detailDialog).toBeVisible({ timeout: 5_000 })
    await expect(detailDialog.locator('.el-divider').filter({ hasText: '子卷明细' })).toBeVisible()

    const descText = await detailDialog.locator('.el-descriptions').first().textContent()
    expect(descText).toContain(detailBody.data.slitBatchNo)
    console.log('  ✅ 分切详情字段展示正确')

    await detailDialog.locator('.el-dialog__headerbtn').last().click()
  })
})
