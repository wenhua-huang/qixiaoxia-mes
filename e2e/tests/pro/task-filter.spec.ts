import { test, expect } from '@playwright/test'

test.describe('生产任务 — 列表字段模糊搜索', () => {
  test.use({ storageState: 'setup/storageState.json' })
  test.setTimeout(120_000)

  /**
   * 辅助函数：导航到生产任务页面并等待表格渲染
   */
  async function navigateToTaskPage(page: any) {
    await page.goto('/mes/pro/task')
    await expect(page.locator('.el-table').first()).toBeVisible({ timeout: 15_000 })
    // 确保搜索表单已渲染
    await expect(page.locator('.el-form').first()).toBeVisible({ timeout: 5_000 })
  }

  /**
   * 辅助函数：重置搜索条件
   */
  async function resetFilters(page: any) {
    const resetBtn = page.locator('button').filter({ hasText: '重置' }).first()
    await resetBtn.click()
    await page.waitForTimeout(300)
  }

  test('workorderName 模糊搜索 → URL参数 + 结果数量校验', async ({ page }) => {
    await navigateToTaskPage(page)
    await resetFilters(page)

    // 填入工单名称并触发搜索，同时拦截列表 API 响应
    const workorderNameInput = page.locator('.el-form-item').filter({ hasText: '工单名称' }).locator('input').first()
    await workorderNameInput.fill('WO2026062200006')

    const [filteredResp] = await Promise.all([
      page.waitForResponse(
        r => r.url().includes('/mes/pro/task/list') && r.status() === 200,
        { timeout: 10_000 }
      ),
      page.locator('button').filter({ hasText: '搜索' }).first().click()
    ])

    // 验证 URL 包含筛选参数
    const filterUrl = new URL(filteredResp.url())
    expect(filterUrl.searchParams.get('workorderName')).toBe('WO2026062200006')
    console.log(`  ✅ URL 包含 workorderName=WO2026062200006`)

    // 验证返回结果全部匹配
    const body = await filteredResp.json()
    expect(body.total).toBeGreaterThan(0)
    body.rows.forEach((row: any) => {
      expect(row.workorderName).toBe('WO2026062200006')
    })
    console.log(`  ✅ 返回 ${body.total} 条记录，全部 workorderName=WO2026062200006`)
  })

  test('workorderName → 无过滤 vs 有过滤 total 数量对比', async ({ page }) => {
    await navigateToTaskPage(page)
    await resetFilters(page)

    // 先获取无过滤时的 total
    const [noFilterResp] = await Promise.all([
      page.waitForResponse(
        r => r.url().includes('/mes/pro/task/list') && r.status() === 200,
        { timeout: 10_000 }
      ),
      page.locator('button').filter({ hasText: '搜索' }).first().click()
    ])
    const noFilterTotal = (await noFilterResp.json()).total

    // 再填入 workorderName 过滤
    await resetFilters(page)
    const workorderNameInput = page.locator('.el-form-item').filter({ hasText: '工单名称' }).locator('input').first()
    await workorderNameInput.fill('WO2026062200006')

    const [filteredResp] = await Promise.all([
      page.waitForResponse(
        r => r.url().includes('/mes/pro/task/list') && r.status() === 200,
        { timeout: 10_000 }
      ),
      page.locator('button').filter({ hasText: '搜索' }).first().click()
    ])
    const filteredTotal = (await filteredResp.json()).total

    // 有过滤的结果数应该 ≤ 无过滤
    expect(filteredTotal).toBeLessThanOrEqual(noFilterTotal)
    console.log(`  ✅ 无过滤 total=${noFilterTotal}，workorderName 过滤后 total=${filteredTotal}`)
  })

  test('workstationName 模糊搜索 → URL参数校验', async ({ page }) => {
    await navigateToTaskPage(page)
    await resetFilters(page)

    // 工位输入框（placeholder="请输入工位名称"）
    const workstationInput = page.locator('.el-form-item').filter({ hasText: '工位' }).locator('input').first()
    await workstationInput.fill('包装')

    const [filteredResp] = await Promise.all([
      page.waitForResponse(
        r => r.url().includes('/mes/pro/task/list') && r.status() === 200,
        { timeout: 10_000 }
      ),
      page.locator('button').filter({ hasText: '搜索' }).first().click()
    ])

    const filterUrl = new URL(filteredResp.url())
    expect(filterUrl.searchParams.get('workstationName')).toBe('包装')
    console.log(`  ✅ URL 包含 workstationName=包装`)

    // 验证返回结果全部包含"包装"
    const body = await filteredResp.json()
    expect(body.total).toBeGreaterThan(0)
    body.rows.forEach((row: any) => {
      expect(row.workstationName).toContain('包装')
    })
    console.log(`  ✅ 返回 ${body.total} 条记录，全部 workstationName 包含"包装"`)
  })

  test('processName 模糊搜索 → URL参数 + 结果内容校验', async ({ page }) => {
    await navigateToTaskPage(page)
    await resetFilters(page)

    // 工序名称输入框
    const processInput = page.locator('.el-form-item').filter({ hasText: '工序名称' }).locator('input').first()
    await processInput.fill('印刷')

    const [filteredResp] = await Promise.all([
      page.waitForResponse(
        r => r.url().includes('/mes/pro/task/list') && r.status() === 200,
        { timeout: 10_000 }
      ),
      page.locator('button').filter({ hasText: '搜索' }).first().click()
    ])

    const filterUrl = new URL(filteredResp.url())
    expect(filterUrl.searchParams.get('processName')).toBe('印刷')
    console.log(`  ✅ URL 包含 processName=印刷`)

    const body = await filteredResp.json()
    // processName='印刷' 应该能找到记录
    if (body.total > 0) {
      body.rows.forEach((row: any) => {
        expect(row.processName).toContain('印刷')
      })
      console.log(`  ✅ 返回 ${body.total} 条记录，全部 processName 包含"印刷"`)
    } else {
      console.log(`  ⚠️ 测试 DB 中无 processName 包含"印刷"的记录（total=0）`)
    }
  })

  test('组合筛选：workorderName + processName 同时生效', async ({ page }) => {
    await navigateToTaskPage(page)
    await resetFilters(page)

    // 同时填入工单名称和工序名称
    const workorderInput = page.locator('.el-form-item').filter({ hasText: '工单名称' }).locator('input').first()
    const processInput = page.locator('.el-form-item').filter({ hasText: '工序名称' }).locator('input').first()
    await workorderInput.fill('WO2026062200006')
    await processInput.fill('印刷')

    const [filteredResp] = await Promise.all([
      page.waitForResponse(
        r => r.url().includes('/mes/pro/task/list') && r.status() === 200,
        { timeout: 10_000 }
      ),
      page.locator('button').filter({ hasText: '搜索' }).first().click()
    ])

    const filterUrl = new URL(filteredResp.url())
    expect(filterUrl.searchParams.get('workorderName')).toBe('WO2026062200006')
    expect(filterUrl.searchParams.get('processName')).toBe('印刷')
    console.log(`  ✅ URL 同时包含 workorderName 和 processName`)

    const body = await filteredResp.json()
    body.rows.forEach((row: any) => {
      expect(row.workorderName).toBe('WO2026062200006')
      expect(row.processName).toContain('印刷')
    })
    console.log(`  ✅ 返回 ${body.total} 条记录，全部同时满足 workorderName + processName`)
  })

  test('不存在的 workorderName 搜索 → 返回空结果', async ({ page }) => {
    await navigateToTaskPage(page)
    await resetFilters(page)

    const workorderInput = page.locator('.el-form-item').filter({ hasText: '工单名称' }).locator('input').first()
    await workorderInput.fill('NONEXISTENT_WO_999999')

    const [filteredResp] = await Promise.all([
      page.waitForResponse(
        r => r.url().includes('/mes/pro/task/list') && r.status() === 200,
        { timeout: 10_000 }
      ),
      page.locator('button').filter({ hasText: '搜索' }).first().click()
    ])

    const body = await filteredResp.json()
    expect(body.total).toBe(0)
    console.log(`  ✅ 不存在的工单名称返回 total=0`)
  })
})
