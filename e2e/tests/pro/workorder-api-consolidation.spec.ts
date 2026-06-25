import { test, expect, APIRequestContext } from '@playwright/test'

/**
 * 生产工单 — createWithBom / updateWithBom 合并接口 E2E
 *
 * 验证点（浏览器真实操作 + 网络请求捕获）：
 * 1. 新增 → POST /mes/pro/workorder/createWithBom（非旧版多次请求）
 * 2. 修改 → PUT /mes/pro/workorder/updateWithBom（非旧版多次请求）
 * 3. 查看详情 → GET /mes/pro/workorder/detail/{id}（非旧版多次请求）
 * 4. payload 结构：{ workorder, bomList, paramList }
 *
 * 数据依赖：产品需有关联工艺路线（routeproduct + proroute）。
 * 测试前先查询后端确认路线数据存在，不存在则 skip。
 */
const BACKEND = 'http://localhost:8081'

/** 从浏览器 cookie 提取 token，创建直连后端的 API context */
async function createApiContext(page: any, playwright: any): Promise<APIRequestContext> {
  const token = await page.evaluate(() => {
    const match = document.cookie.match(/(?:^|;\s*)Admin-Token=([^;]*)/)
    return match ? match[1] : ''
  })
  return playwright.request.newContext({
    baseURL: BACKEND,
    extraHTTPHeaders: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
  })
}

/**
 * 查询后端确认工艺路线数据是否存在。
 * 返回路线的 productId（routeproduct.itemId）供 UI 选择产品时参考。
 * 无数据时返回 null，测试自动 skip。
 */
async function findProductWithRoute(api: APIRequestContext): Promise<{
  routeId: number; productId: number; routeName: string
} | null> {
  // 查询路线-产品关联
  const rpRes = await api.get('/mes/pro/routeproduct/list?pageNum=1&pageSize=5')
  if (rpRes.status() !== 200) return null
  const rpBody = await rpRes.json()
  const rpRows = rpBody.rows || []
  if (rpRows.length === 0) return null

  const first = rpRows[0]
  const routeId = first.routeId || first.recordId
  const productId = first.itemId

  // 确认产品名称可用
  const itemRes = await api.get(`/mes/md/item/${productId}`)
  const itemName = itemRes.status() === 200
    ? ((await itemRes.json()).data || {}).itemName || ''
    : ''

  // 查询路线名称
  let routeName = '路线#' + routeId
  if (routeId) {
    const routeRes = await api.get(`/mes/pro/proroute/${routeId}`)
    if (routeRes.status() === 200) {
      routeName = ((await routeRes.json()).data || {}).routeName || routeName
    }
  }

  console.log(`  📦 产品: id=${productId} name="${itemName}" | 路线: id=${routeId} name="${routeName}"`)
  return { routeId: routeId!, productId: productId!, routeName }
}

test.describe('生产工单 — createWithBom / updateWithBom / detail 合并接口', () => {
  test.use({ storageState: 'setup/storageState.json' })
  test.setTimeout(180_000)

  const TS = Date.now().toString(36).toUpperCase()

  /**
   * 新增 → POST /createWithBom
   * 使用真实后端路线数据（查询确认存在后再执行）
   */
  test('新增工单应调用 POST /createWithBom（零次旧版 BOM/param 写入）', async ({ page, playwright }) => {
    const WO = 'E2E-CWB-' + TS

    // 先查询后端确认路线数据存在
    await page.goto('/mes/pro/workorder')
    await expect(page.locator('.el-table').first()).toBeVisible({ timeout: 15000 })
    const api = await createApiContext(page, playwright)

    const routeData = await findProductWithRoute(api)
    if (!routeData) {
      test.skip(true, '后端无工艺路线数据，请先播种路线/产品/工序数据')
      return
    }

    const oldWrites: string[] = []
    page.on('request', (req) => {
      const u = req.url()
      if ((u.includes('/mes/pro/workorderbom') || u.includes('/mes/pro/workorderparam')) &&
          (req.method() === 'POST' || req.method() === 'PUT' || req.method() === 'DELETE')) {
        oldWrites.push(req.method() + ' ' + u.replace(/^.*\/dev-api/, '/dev-api'))
      }
    })

    // 刷新页面（cookie 已就绪），开始真实 UI 操作
    await page.reload()
    await expect(page.locator('.el-table').first()).toBeVisible({ timeout: 15000 })

    await page.locator('button').filter({ hasText: /新增/ }).first().click()
    const d = page.locator('.el-dialog').first()
    await expect(d).toBeVisible({ timeout: 5000 })

    const sw = d.locator('.el-switch').first()
    if (await sw.isVisible().catch(() => false)) {
      if (await sw.evaluate((el: any) => el.classList.contains('is-checked'))) { await sw.click(); await page.waitForTimeout(300) }
    }
    await d.locator('input:not([disabled])').first().fill(WO)
    await d.getByPlaceholder('请输入').nth(1).fill(WO)

    // 选产品
    await d.locator('button').filter({ hasText: /选择/ }).first().click()
    const itemDlg = page.locator('.el-dialog').last()
    await expect(itemDlg.locator('.el-table__body tr').first()).toBeVisible({ timeout: 10000 })
    await itemDlg.locator('.el-table__body tr').first().dblclick()
    await page.waitForTimeout(2000)

    // 选路线 → 下一步 → 确定
    const routeSel = d.locator('.el-form-item').filter({ hasText: '工艺路线' }).locator('.el-select')
    await routeSel.click(); await page.waitForTimeout(500)
    const opts = page.locator('.el-select-dropdown__item:visible')

    const createWithBomReq = page.waitForRequest(
      r => r.method() === 'POST' && r.url().includes('/mes/pro/workorder/createWithBom'),
      { timeout: 15000 }
    ).catch(() => null)

    if (await opts.first().isVisible({ timeout: 3000 }).catch(() => false)) {
      await opts.first().click(); await page.waitForTimeout(300)
      await d.locator('.el-input-number input').first().fill('100')
      await d.locator('.dialog-footer button').filter({ hasText: '下一步' }).first().click()
      await page.waitForTimeout(500)

      await d.locator('.dialog-footer button').filter({ hasText: '确 定' }).first().click()
      await page.waitForTimeout(2000)

      const req = await createWithBomReq
      expect(req).not.toBeNull()
      if (req) {
        const body = req.postDataJSON()
        console.log(`  📦 createWithBom: ${Object.keys(body).join(', ')}`)
        expect(body).toHaveProperty('workorder')
        expect(body).toHaveProperty('bomList')
        expect(body).toHaveProperty('paramList')
        expect(body.workorder.workorderCode).toBe(WO)
        if (body.bomList.length > 0) {
          expect(body.bomList[0]).not.toHaveProperty('_processId')
          expect(body.bomList[0]).not.toHaveProperty('_processName')
        }
        if (body.paramList.length > 0) {
          expect(body.paramList[0]).not.toHaveProperty('_paramName')
          expect(body.paramList[0]).not.toHaveProperty('paramName')
        }
        console.log(`  ✅ createWithBom | BOM:${body.bomList.length} param:${body.paramList.length}`)
      }
      await expect(d).not.toBeVisible({ timeout: 8000 })
    } else {
      await page.keyboard.press('Escape'); await page.waitForTimeout(300)
      await page.keyboard.press('Escape')
      test.skip(true, '产品无关联工艺路线（查询时已确认存在但选择时未出现）')
    }

    // ⚡ 零次旧版写入
    console.log(`  📋 旧版写入: ${oldWrites.length > 0 ? oldWrites.join(' | ') : '(无)'}`)
    expect(oldWrites.length).toBe(0)
  })

  /**
   * 修改 → PUT /updateWithBom
   * 先创建工单 → 搜索 → 修改 → 确定，验证 updateWithBom 被调用
   */
  test('修改工单应调用 PUT /updateWithBom（零次旧版 BOM/param 写入）', async ({ page, playwright }) => {
    const WO = 'E2E-UWB-' + TS

    // 先查询后端确认路线数据存在
    await page.goto('/mes/pro/workorder')
    await expect(page.locator('.el-table').first()).toBeVisible({ timeout: 15000 })
    const api = await createApiContext(page, playwright)

    const routeData = await findProductWithRoute(api)
    if (!routeData) {
      test.skip(true, '后端无工艺路线数据，请先播种路线/产品/工序数据')
      return
    }

    const oldWrites: string[] = []
    page.on('request', (req) => {
      const u = req.url()
      if ((u.includes('/mes/pro/workorderbom') || u.includes('/mes/pro/workorderparam')) &&
          (req.method() === 'POST' || req.method() === 'PUT' || req.method() === 'DELETE')) {
        oldWrites.push(req.method() + ' ' + u.replace(/^.*\/dev-api/, '/dev-api'))
      }
    })

    await page.reload()
    await expect(page.locator('.el-table').first()).toBeVisible({ timeout: 15000 })

    // ① 创建工单
    await page.locator('button').filter({ hasText: /新增/ }).first().click()
    let d = page.locator('.el-dialog').first()
    await expect(d).toBeVisible({ timeout: 5000 })

    const sw = d.locator('.el-switch').first()
    if (await sw.isVisible().catch(() => false)) {
      if (await sw.evaluate((el: any) => el.classList.contains('is-checked'))) { await sw.click(); await page.waitForTimeout(300) }
    }
    await d.locator('input:not([disabled])').first().fill(WO)
    await d.getByPlaceholder('请输入').nth(1).fill(WO)
    await d.locator('button').filter({ hasText: /选择/ }).first().click()
    const itemDlg = page.locator('.el-dialog').last()
    await expect(itemDlg.locator('.el-table__body tr').first()).toBeVisible({ timeout: 10000 })
    await itemDlg.locator('.el-table__body tr').first().dblclick()
    await page.waitForTimeout(2000)

    const routeSel = d.locator('.el-form-item').filter({ hasText: '工艺路线' }).locator('.el-select')
    await routeSel.click(); await page.waitForTimeout(500)
    const opts = page.locator('.el-select-dropdown__item:visible')

    if (await opts.first().isVisible({ timeout: 3000 }).catch(() => false)) {
      await opts.first().click(); await page.waitForTimeout(300)
      await d.locator('.el-input-number input').first().fill('100')
      await d.locator('.dialog-footer button').filter({ hasText: '下一步' }).first().click()
      await page.waitForTimeout(500)
      await d.locator('.dialog-footer button').filter({ hasText: '确 定' }).first().click()
      await expect(d).not.toBeVisible({ timeout: 10000 })
    } else {
      await page.keyboard.press('Escape'); await page.waitForTimeout(300)
      await page.keyboard.press('Escape')
      test.skip(true, '产品无关联工艺路线')
      return
    }

    // ② 搜索 → 修改
    await page.waitForTimeout(2000)
    await page.locator('input[placeholder*="请输入"]').first().fill(WO)
    await page.locator('button').filter({ hasText: '搜索' }).first().click()
    await page.waitForTimeout(2000)

    const writesBeforeEdit = oldWrites.length

    await page.locator('.el-table__body .el-button').nth(4).click()
    d = page.locator('.el-dialog').first()
    await expect(d).toBeVisible({ timeout: 5000 })

    await d.getByPlaceholder('请输入').nth(1).fill(WO + '-MOD')
    await page.waitForTimeout(500)

    const updateWithBomReq = page.waitForRequest(
      r => r.method() === 'PUT' && r.url().includes('/mes/pro/workorder/updateWithBom'),
      { timeout: 15000 }
    ).catch(() => null)

    const nextBtn = d.locator('.dialog-footer button').filter({ hasText: '下一步' }).first()
    if (await nextBtn.isVisible({ timeout: 3000 }).catch(() => false)) {
      await nextBtn.click(); await page.waitForTimeout(500)
    }

    await d.locator('.dialog-footer button').filter({ hasText: '确 定' }).first().click()
    await page.waitForTimeout(2000)

    const req = await updateWithBomReq
    expect(req).not.toBeNull()
    if (req) {
      const body = req.postDataJSON()
      console.log(`  📦 updateWithBom: ${Object.keys(body).join(', ')}`)
      expect(body).toHaveProperty('workorder')
      expect(body).toHaveProperty('bomList')
      expect(body).toHaveProperty('paramList')
      expect(body.workorder.workorderId).toBeGreaterThan(0)
      expect(body.workorder.workorderName).toBe(WO + '-MOD')
      if (body.bomList.length > 0) {
        expect(body.bomList[0]).not.toHaveProperty('_processId')
      }
      if (body.paramList.length > 0) {
        expect(body.paramList[0]).toHaveProperty('recordId')
        expect(body.paramList[0]).not.toHaveProperty('_paramName')
      }
      console.log(`  ✅ updateWithBom | BOM:${body.bomList.length} param:${body.paramList.length}`)
    }

    // ⚡ 修改阶段零次旧版写入
    const editWrites = oldWrites.length - writesBeforeEdit
    console.log(`  📋 修改阶段旧版写入: ${editWrites}`)
    expect(editWrites).toBe(0)

    await page.keyboard.press('Escape')
  })

  /**
   * 查看详情 → GET /detail/{id}
   * 验证查看工单详情时调用合并详情接口，不调用旧版 listByWorkorderId
   */
  test('查看工单详情：应调用 GET /detail/{id} 且不调用旧版 listByWorkorderId', async ({ page, playwright }) => {
    const WO = 'E2E-DTL-' + TS

    // 先查询后端确认路线数据存在
    await page.goto('/mes/pro/workorder')
    await expect(page.locator('.el-table').first()).toBeVisible({ timeout: 15000 })
    const api = await createApiContext(page, playwright)

    const routeData = await findProductWithRoute(api)
    if (!routeData) {
      test.skip(true, '后端无工艺路线数据，请先播种路线/产品/工序数据')
      return
    }

    let detailCalls = 0, oldBomReads = 0, oldParamReads = 0
    page.on('request', (req) => {
      const u = req.url()
      if (u.includes('/mes/pro/workorder/detail/')) detailCalls++
      if (u.includes('/mes/pro/workorderbom/listByWorkorderId')) oldBomReads++
      if (u.includes('/mes/pro/workorderparam/listByWorkorderId')) oldParamReads++
    })

    await page.reload()
    await expect(page.locator('.el-table').first()).toBeVisible({ timeout: 15000 })

    // 创建工单
    await page.locator('button').filter({ hasText: /新增/ }).first().click()
    let d = page.locator('.el-dialog').first()
    await expect(d).toBeVisible({ timeout: 5000 })

    const sw = d.locator('.el-switch').first()
    if (await sw.isVisible().catch(() => false)) {
      if (await sw.evaluate((el: any) => el.classList.contains('is-checked'))) { await sw.click(); await page.waitForTimeout(300) }
    }
    await d.locator('input:not([disabled])').first().fill(WO)
    await d.getByPlaceholder('请输入').nth(1).fill(WO)
    await d.locator('button').filter({ hasText: /选择/ }).first().click()
    const itemDlg = page.locator('.el-dialog').last()
    await expect(itemDlg.locator('.el-table__body tr').first()).toBeVisible({ timeout: 10000 })
    await itemDlg.locator('.el-table__body tr').first().dblclick()
    await page.waitForTimeout(2000)

    const routeSel = d.locator('.el-form-item').filter({ hasText: '工艺路线' }).locator('.el-select')
    await routeSel.click(); await page.waitForTimeout(500)
    const opts = page.locator('.el-select-dropdown__item:visible')

    if (await opts.first().isVisible({ timeout: 3000 }).catch(() => false)) {
      await opts.first().click(); await page.waitForTimeout(300)
      await d.locator('.el-input-number input').first().fill('50')
      await d.locator('.dialog-footer button').filter({ hasText: '下一步' }).first().click()
      await page.waitForTimeout(500)
      await d.locator('.dialog-footer button').filter({ hasText: '确 定' }).first().click()
      await expect(d).not.toBeVisible({ timeout: 10000 })
    } else {
      await page.keyboard.press('Escape'); await page.waitForTimeout(300)
      await page.keyboard.press('Escape')
      test.skip(true, '产品无关联工艺路线')
      return
    }

    // 搜索 → 查看
    await page.waitForTimeout(2000)
    // 重置计数（忽略创建阶段请求）
    detailCalls = 0; oldBomReads = 0; oldParamReads = 0

    await page.locator('input[placeholder*="请输入"]').first().fill(WO)
    await page.locator('button').filter({ hasText: '搜索' }).first().click()
    await page.waitForTimeout(2000)

    await page.locator('.el-table__body tr').first().locator('.el-button').last().click()
    d = page.locator('.el-dialog').first()
    await expect(d).toBeVisible({ timeout: 5000 })
    await page.waitForTimeout(3000)

    // ⚡ 验证：调用了合并详情接口
    expect(detailCalls).toBeGreaterThanOrEqual(1)
    console.log(`  📋 查看详情 | detail:${detailCalls} | 旧BOM读取:${oldBomReads} | 旧参数读取:${oldParamReads}`)

    // ⚡ 验证：未调用旧版 listByWorkorderId
    expect(oldBomReads).toBe(0)
    expect(oldParamReads).toBe(0)

    console.log('  ✅ 查看详情验证通过：一次合并接口，零次旧版读取')
    await page.keyboard.press('Escape')
  })
})
