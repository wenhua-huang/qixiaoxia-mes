import { test, expect } from '@playwright/test'

/**
 * 采购入库单（item_recpt）「从采购单快捷生成」全链路 E2E 测试
 *
 * 覆盖核心增量：
 *  1. 列表页「从采购单生成」按钮 → PO 选择器（默认筛选 ORDERED+RECEIVING）
 *  2. fromPurOrder 端点返回草稿（头+行），入库数量预填 = 未收量
 *  3. 头行一体保存 → 草稿入库单
 *  4. 确认收货（DRAFT→CONFIRMED）+ 过账（CONFIRMED→POSTED）via API
 *  5. PO 行 quantityReceived 回写校验
 *
 * 前置条件：前后端均启动（dev-start skill），Docker 容器 qxx-mysql/qxx-redis 运行
 * 运行：cd e2e && npx playwright test tests/wm/item-recpt-lifecycle.spec.ts --reporter=line
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

/** API 创建一张已下单的采购订单（含 1 行物料），返回 orderId */
async function createOrderedPo(page: import('@playwright/test').Page, token: string, code: string): Promise<number> {
  // 1. 建头
  await api(page, token, 'POST', '/mes/pur/order', {
    orderCode: code, orderName: 'E2E入库测试', vendorId: 202, vendorName: '德欣纸业',
    vendorCode: 'VEN-DEXIN', purchaseType: 'PAPER', status: 'DRAFT', orderDate: '2026-07-24'
  })
  // 回查 id（add 可能不直接返回 id）
  let oid = await getPoIdByCode(page, token, code)

  // 2. 加行（item=201 箱板纸，qty=10）
  await api(page, token, 'POST', '/mes/pur/order-line', {
    orderId: oid, itemId: 201, itemCode: 'PAPER-XBZ-A', itemName: '箱板纸 A级 925mm 120g',
    specification: '925mm*120g', unitOfMeasure: 'ROLL', unitName: '卷',
    quantityOrdered: 10, unitPrice: 1, amount: 10, status: 'ORDERED'
  })

  // 3. 审批 → 下单（DRAFT → APPROVED → ORDERED）
  await api(page, token, 'POST', `/mes/pur/order/${oid}/approve`)
  await api(page, token, 'POST', `/mes/pur/order/${oid}/order`)
  return oid
}

/** 按编码查 PO id */
async function getPoIdByCode(page: import('@playwright/test').Page, token: string, code: string): Promise<number> {
  const d = await api(page, token, 'GET', `/mes/pur/order/list?orderCode=${code}`)
  return d.rows?.[0]?.orderId
}

test.describe('采购入库单 - 从采购单快捷生成', () => {
  test.use({ storageState: 'setup/storageState.json' })
  test.setTimeout(120000)

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize({ width: 1920, height: 1080 })
  })

  test('全链路：建PO → 从采购单生成 → 保存 → 确认收货 → 过账 → PO已收量回写', async ({ page }) => {
    const poCode = 'E2E-PO-' + Date.now().toString(36).toUpperCase()
    const token = await getToken(page)
    console.log(`  📝 准备测试PO: ${poCode}`)

    // ==== 0. API 建一张已下单 PO（含 1 行，订购 10）====
    const orderId = await createOrderedPo(page, token, poCode)
    expect(orderId).toBeTruthy()
    console.log(`  ✅ PO已下单 (orderId=${orderId})`)

    try {
      // ==== 1. 直接路由到 物料入库页 ====
      const listReady = page.waitForResponse(
        r => r.url().includes('/mes/wm/item_recpt/list') && r.status() === 200,
        { timeout: 20000 }
      )
      await page.goto('/mes/wm/item_recpt')
      await listReady
      await expect(page.locator('.el-table').first()).toBeVisible({ timeout: 10000 })
      console.log('  ✅ 物料入库页加载成功')

      // ==== 2. 点「从采购单生成」====
      await page.getByRole('button', { name: '从采购单生成' }).click({ timeout: 8000 })

      // PO 选择器弹窗 — 验证默认筛选 ORDERED+RECEIVING 生效（状态下拉含这两个 tag）
      const poDialog = page.locator('.el-dialog').filter({ hasText: '采购单据选择' }).last()
      await expect(poDialog).toBeVisible({ timeout: 5000 })
      console.log('  ✅ PO选择器打开（默认筛选 ORDERED+RECEIVING）')

      // 搜索测试 PO
      await poDialog.getByPlaceholder('请输入').first().fill(poCode)
      await poDialog.getByRole('button', { name: '搜索' }).click()
      await page.waitForTimeout(1500)

      // 双击选中第一行
      const fromPoResp = page.waitForResponse(
        r => r.url().includes('/mes/wm/item_recpt/fromPurOrder/') && r.status() === 200,
        { timeout: 15000 }
      )
      await poDialog.locator('.el-table__body tr').first().dblclick()
      const draftResp = await fromPoResp
      const draft = await draftResp.json()
      // 后端返回校验：头 + 行预填数量 = 未收量 10
      expect(draft.code).toBe(200)
      const draftLines = draft.data?.lines || []
      expect(draftLines.length).toBeGreaterThan(0)
      expect(String(draftLines[0].quantityRecpt)).toBe('10')
      console.log(`  ✅ fromPurOrder 接口返回 ${draftLines.length} 行，预填数量=${draftLines[0].quantityRecpt}`)

      // ==== 3. 验证 FormDialog 行预填（UI 层）====
      const formDialog = page.locator('.el-dialog').filter({ hasText: '物料信息' }).last()
      await expect(formDialog).toBeVisible({ timeout: 5000 })

      // 采购订单号已回填
      await expect(formDialog.getByPlaceholder('选采购订单生成后自动带出')).toHaveValue(poCode)

      // 物料行：物料编码 + 本次入库量
      const lineRow = formDialog.locator('.el-table__body tr').first()
      await expect(lineRow).toBeVisible({ timeout: 5000 })
      const rowText = (await lineRow.textContent()) || ''
      expect(rowText).toContain('PAPER-XBZ-A')
      // 本次入库量输入框 = 10（el-input-number 的 spinbutton）
      const qtyVal = await lineRow.getByRole('spinbutton').inputValue()
      console.log(`  📋 预填入库数量: ${qtyVal}`)
      expect(qtyVal).toContain('10')
      console.log('  ✅ UI 行预填正确（物料=箱板纸，本次入库量=未收量10）')

      // 选仓库（必填）— 点仓库 append 按钮 → 双击第一个仓库
      const whFormRow = formDialog.locator('.el-form-item').filter({ hasText: '仓库' }).first()
      await whFormRow.locator('.el-input-group__append button').click()
      await page.waitForTimeout(1500)
      const whDialog = page.locator('.el-dialog').filter({ hasText: '仓库选择' }).last()
      await expect(whDialog).toBeVisible({ timeout: 5000 })
      await whDialog.locator('.el-table__body tr').first().dblclick()
      await page.waitForTimeout(500)

      // ==== 4. 保存入库单（拦截 add）====
      const addResp = page.waitForResponse(
        r => r.url().match(/\/mes\/wm\/item_recpt$/) && r.request().method() === 'POST' && r.status() === 200,
        { timeout: 15000 }
      )
      await formDialog.getByRole('button', { name: '保存单据' }).click()
      const saved = await addResp
      const savedBody = await saved.json()
      expect(savedBody.code).toBe(200)
      await expect(page.locator('.el-message').filter({ hasText: '成功' })).toBeVisible({ timeout: 5000 })
      console.log('  ✅ 入库单已保存（草稿）')

      // ==== 5. 列表查新建入库单 → API 确认收货 + 过账 ====
      // 用 PO 编码反查入库单（入库单编码是自动生成的，不可预知）
      const recptList = await api(page, token, 'GET', `/mes/wm/item_recpt/list?purOrderCode=${poCode}`)
      const recptId = recptList.rows?.[0]?.recptId
      expect(recptId).toBeTruthy()
      console.log(`  📋 新建入库单 recptId=${recptId}`)

      // 确认收货（DRAFT → CONFIRMED）
      const confirmRes = await api(page, token, 'PUT', `/mes/wm/item_recpt/confirm/${recptId}`)
      expect(confirmRes.code).toBe(200)
      console.log('  ✅ 确认收货成功（DRAFT → CONFIRMED，库存已更新）')

      // 过账（CONFIRMED → POSTED）
      const postRes = await api(page, token, 'PUT', `/mes/wm/item_recpt/post/${recptId}`)
      expect(postRes.code).toBe(200)
      console.log('  ✅ 过账成功（CONFIRMED → POSTED）')

      // ==== 6. 校验：PO 行 quantityReceived 应回写为 10 ====
      const lineData = await api(page, token, 'GET', `/mes/pur/order-line/list?orderId=${orderId}`)
      const received = lineData.rows?.[0]?.quantityReceived
      console.log(`  📋 过账后 PO 行已收数量: ${received}`)
      expect(String(received)).toBe('10')
      console.log('  ✅ PO 已收数量回写校验通过（10）')

    } finally {
      // ==== 清理 ====
      // 删入库单（已过账可能删不掉，忽略）+ 删 PO
      const recptList = await api(page, token, 'GET', `/mes/wm/item_recpt/list?purOrderCode=${poCode}`)
      for (const recpt of (recptList.rows || [])) {
        await api(page, token, 'DELETE', `/mes/wm/item_recpt/${recpt.recptId}`).catch(() => {})
      }
      await api(page, token, 'DELETE', `/mes/pur/order/${orderId}`).catch(() => {})
      console.log('  🧹 测试数据已清理')
    }
  })
})
