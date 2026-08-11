/**
 * 流转卡外协智能路由 E2E
 *
 * 验证：点流转卡「外协」按钮时，按下一道外协工序的 processType 路由到不同弹窗：
 * - SLITTING → 弹「执行分切」(SlittingExecuteDialog)
 * - OUTSOURCE → 弹「外协发货」(OutsourceCreateDialog)
 * - 无外协工序 → 提示「该流转卡所在的工艺路线没有外协工序」
 *
 * 数据依赖：后端需有 ACTIVE 状态的流转卡。SLITTING 路由通过 API 临时把
 * 分切工序标为外协来构造，测完还原。
 *
 * ⚠️ 必须 --workers=1 串行执行：SLITTING 测试临时改路线数据，跨 describe 并行会数据竞争。
 *    运行：cd e2e && npx playwright test tests/pro/outsource-route.spec.ts --workers=1
 */
import { test, expect, type Page } from '@playwright/test'

const API_BASE = 'http://localhost:5173/dev-api'

// ── API 辅助（复用 slitting.spec.ts 模式）──

async function getToken(page: Page): Promise<string> {
  return await page.evaluate(async (base) => {
    const r = await fetch(base + '/login', {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: 'admin', password: 'admin123', code: '', uuid: '' })
    })
    return (await r.json()).token
  }, API_BASE)
}

async function api(page: Page, token: string, method: string, path: string, body?: any) {
  return await page.evaluate(async (args) => {
    const r = await fetch(args.base + args.path, {
      method: args.method,
      headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + args.token },
      body: args.body ? JSON.stringify(args.body) : undefined
    })
    return await r.json()
  }, { base: API_BASE, token, method, path, body })
}

/** 导航到流转卡列表，等待首屏数据加载完成 */
async function navigateToCardPage(page: Page) {
  const listPromise = page.waitForResponse(
    r => r.url().includes('/mes/pro/procard/list') && r.status() === 200,
    { timeout: 15_000 }
  )
  await page.goto('/mes/pro/card')
  await listPromise
  await expect(page.locator('.el-table').first()).toBeVisible({ timeout: 5_000 })
}

/** 查一张 ACTIVE 且有外协工序的流转卡，返回 {cardId, cardCode} */
async function findOutsourceCard(page: Page, token: string): Promise<{cardId: number, cardCode: string} | null> {
  const res = await api(page, token, 'GET', '/mes/pro/procard/list?pageNum=1&pageSize=20')
  const rows = res.rows || []
  for (const r of rows) {
    if (r.status !== 'ACTIVE') continue
    const info = await api(page, token, 'GET', `/mes/pro/procard/${r.cardId}/outsourceInfo`)
    if (info.data && info.data.nextOutsourceProcess) {
      return { cardId: r.cardId, cardCode: r.cardCode }
    }
  }
  return null
}

/** 查一张命中指定 processType 的 ACTIVE 流转卡，返回 {cardId, cardCode} */
async function findCardByProcessType(page: Page, token: string, processType: string): Promise<{cardId: number, cardCode: string} | null> {
  const res = await api(page, token, 'GET', '/mes/pro/procard/list?pageNum=1&pageSize=20')
  const rows = res.rows || []
  for (const r of rows) {
    if (r.status !== 'ACTIVE') continue
    const info = await api(page, token, 'GET', `/mes/pro/procard/${r.cardId}/outsourceInfo`)
    if (info.data?.nextOutsourceProcess?.processType === processType) {
      return { cardId: r.cardId, cardCode: r.cardCode }
    }
  }
  return null
}

// ════════════════════════════════════════════════════════════════
// 测试 1：普通外协工序 → 弹「外协发货」
// ════════════════════════════════════════════════════════════════
test.describe.serial('流转卡外协路由 — 普通外协工序', () => {
  test.use({ storageState: 'setup/storageState.json' })
  test.setTimeout(120_000)

  test('点外协按钮 → 弹出「外协发货」弹窗', async ({ page }) => {
    const token = await getToken(page)
    const card = await findOutsourceCard(page, token)
    test.skip(!card, '没有 ACTIVE 且含外协工序的流转卡，跳过')

    await navigateToCardPage(page)

    // 精确定位目标卡所在行（用 cardCode），点该行的「外协」按钮
    const outsourceInfoPromise = page.waitForResponse(
      r => r.url().includes('/outsourceInfo') && r.status() === 200,
      { timeout: 15_000 }
    )
    const targetRow = page.locator('.el-table__row').filter({ hasText: card!.cardCode }).first()
    await targetRow.locator('button').filter({ hasText: '外协' }).click()
    const infoResp = await outsourceInfoPromise
    const infoBody = await infoResp.json()
    const processType = infoBody.data?.nextOutsourceProcess?.processType

    // 确认这张卡命中 OUTSOURCE（测试数据若变了，跳过而非失败）
    test.skip(processType !== 'OUTSOURCE', `该卡 processType=${processType}，非 OUTSOURCE，跳过`)

    // 断言：弹出「外协发货」弹窗
    const dialog = page.locator('.el-dialog').filter({ hasText: '外协发货' }).last()
    await expect(dialog).toBeVisible({ timeout: 10_000 })
    await expect(dialog.locator('.el-dialog__title')).toHaveText('外协发货')
  })
})

// ════════════════════════════════════════════════════════════════
// 测试 2：分切外协工序 → 弹「执行分切」
// ════════════════════════════════════════════════════════════════
test.describe.serial('流转卡外协路由 — 分切外协工序', () => {
  test.use({ storageState: 'setup/storageState.json' })
  test.setTimeout(120_000)

  test('点外协按钮 → 弹出「执行分切」弹窗', async ({ page }) => {
    const token = await getToken(page)

    // 临时把路线209的分切工序(recordId=247)标为外协，构造 SLITTING 场景
    const originalRes = await api(page, token, 'GET', '/mes/pro/routeprocess/list?routeId=209')
    const slitProcess = (originalRes.rows || []).find((r: any) => r.processType === 'SLITTING')
    test.skip(!slitProcess, '路线209没有 SLITTING 工序，跳过')

    const updatePayload = { ...slitProcess, isOutsource: '1' }
    await api(page, token, 'PUT', '/mes/pro/routeprocess', updatePayload)

    try {
      // 找一张命中 SLITTING 路由的 ACTIVE 卡
      const card = await findCardByProcessType(page, token, 'SLITTING')
      test.skip(!card, '没有能命中 SLITTING 路由的 ACTIVE 卡，跳过')

      await navigateToCardPage(page)

      // 精确定位目标卡所在行（用 cardCode），点该行的「外协」按钮
      const outsourceInfoPromise = page.waitForResponse(
        r => r.url().includes('/outsourceInfo') && r.status() === 200,
        { timeout: 15_000 }
      )
      const targetRow = page.locator('.el-table__row').filter({ hasText: card!.cardCode }).first()
      await targetRow.locator('button').filter({ hasText: '外协' }).click()
      const infoResp = await outsourceInfoPromise
      const infoBody = await infoResp.json()
      expect(infoBody.data?.nextOutsourceProcess?.processType).toBe('SLITTING')

      // 断言：弹出「执行分切」弹窗
      const dialog = page.locator('.el-dialog').filter({ hasText: '执行分切' }).last()
      await expect(dialog).toBeVisible({ timeout: 10_000 })
      await expect(dialog.locator('.el-dialog__title')).toHaveText('执行分切')
    } finally {
      // 还原分切工序为非外协，避免污染其他测试
      await api(page, token, 'PUT', '/mes/pro/routeprocess', { ...slitProcess, isOutsource: '0' }).catch(() => {})
    }
  })
})

// ════════════════════════════════════════════════════════════════
// 测试 3：无外协工序 → 提示信息
// ════════════════════════════════════════════════════════════════
test.describe.serial('流转卡外协路由 — 无外协工序', () => {
  test.use({ storageState: 'setup/storageState.json' })
  test.setTimeout(120_000)

  test('点外协按钮 → 提示无外协工序', async ({ page }) => {
    const token = await getToken(page)

    // 找一张 ACTIVE 但无外协工序的卡
    const cards = await api(page, token, 'GET', '/mes/pro/procard/list?pageNum=1&pageSize=20')
    let targetCardCode: string | null = null
    for (const c of cards.rows || []) {
      if (c.status !== 'ACTIVE') continue
      const info = await api(page, token, 'GET', `/mes/pro/procard/${c.cardId}/outsourceInfo`)
      if (!info.data || !info.data.nextOutsourceProcess) { targetCardCode = c.cardCode; break }
    }
    test.skip(!targetCardCode, '没有「无外协工序」的 ACTIVE 卡，跳过')

    await navigateToCardPage(page)

    // 精确定位目标卡所在行，点该行的「外协」按钮
    const targetRow = page.locator('.el-table__row').filter({ hasText: targetCardCode! }).first()
    await targetRow.locator('button').filter({ hasText: '外协' }).click()

    // 断言：弹出 ElMessage 警告提示（含"没有外协工序"），且无弹窗打开
    await expect(page.locator('.el-message').filter({ hasText: '没有外协工序' })).toBeVisible({ timeout: 10_000 })
    // 确认没有弹出外协/分切弹窗
    await expect(page.locator('.el-dialog').filter({ hasText: '外协发货' })).toHaveCount(0)
    await expect(page.locator('.el-dialog').filter({ hasText: '执行分切' })).toHaveCount(0)
  })
})
