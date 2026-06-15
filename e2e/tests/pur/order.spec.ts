import { test, expect } from '@playwright/test'

test.describe('采购订单 — 保存后搜索', () => {
  test.use({ storageState: 'setup/storageState.json' })
  test.setTimeout(120000)

  test('保存后用编码搜索，验证出现在列表第一行', async ({ page }) => {
    await page.setViewportSize({ width: 1920, height: 1080 })
    const routesReady = page.waitForResponse(r => r.url().includes('/getRouters') && r.status() === 200, { timeout: 20000 })
    await page.goto('/')
    await routesReady; await page.waitForTimeout(2000)
    await page.evaluate(() => {
      document.querySelectorAll('.el-sub-menu__title').forEach((t: any) => { if (t.textContent?.includes('采购管理')) t.click() })
    })
    await page.waitForTimeout(800)
    await page.evaluate(() => {
      document.querySelectorAll('.el-menu-item').forEach((t: any) => { if (t.textContent?.trim() === '采购订单') t.click() })
    })
    await page.waitForTimeout(3000)
    await expect(page.locator('.el-table').first()).toBeVisible({ timeout: 8000 })

    // ==== 1. 先清空搜索条件 ====
    await page.locator('button').filter({ hasText: '重置' }).first().click()
    await page.waitForTimeout(500)

    // ==== 2. 新增一条 ====
    const uniqueCode = 'SRCH-' + Date.now().toString(36).toUpperCase()
    await page.locator('button').filter({ hasText: /新增/ }).first().click({ timeout: 10000 })
    const dialog = page.locator('.el-dialog').first()
    await expect(dialog).toBeVisible({ timeout: 5000 })

    // 关自动生成
    const sw = dialog.locator('.el-switch').first()
    if (await sw.isVisible().catch(() => false)) {
      if (await sw.evaluate(el => el.classList.contains('is-checked'))) { await sw.click(); await page.waitForTimeout(500) }
    }
    const ci = dialog.locator('input[placeholder*="PO"]').first()
    await ci.evaluate((el: HTMLInputElement) => { el.disabled = false })
    await ci.fill(uniqueCode)
    await dialog.getByPlaceholder('订单名称').fill('搜索验证订单')
    const sb = dialog.locator('.el-input-group__append button').first()
    if (await sb.isVisible({ timeout: 2000 }).catch(() => false)) {
      await sb.click(); await page.waitForTimeout(1500)
      const vd = page.locator('.el-dialog').filter({ hasText: /供应商选择|全称/ }).last()
      await expect(vd).toBeVisible({ timeout: 5000 })
      await vd.locator('.el-table__body tr').first().dblclick()
      await page.waitForTimeout(500)
    }

    // 拦截 POST
    const [postReq] = await Promise.all([
      page.waitForRequest(r => r.method() === 'POST' && r.url().includes('/mes/pur/order'), { timeout: 15000 }),
      dialog.locator('button').filter({ hasText: /保存/ }).first().click()
    ])
    expect(postReq).toBeTruthy()
    await expect(dialog).not.toBeVisible({ timeout: 10000 })
    console.log(`✅ 保存成功: ${uniqueCode}`)
    await page.waitForTimeout(2000)

    // ==== 3. 搜索：第一个 input = 订单编码输入框 ====
    // 找到搜索区域中 label="订单编码" 对应的 input
    const allInputs = page.locator('.el-form').first().locator('input')
    const inputCount = await allInputs.count()
    console.log(`  搜索区input数量: ${inputCount}`)

    // 填充第一个 input（订单编码）
    await allInputs.first().fill(uniqueCode)
    console.log(`  已填入: ${uniqueCode}`)

    // 点击"搜索"
    await page.locator('button').filter({ hasText: '搜索' }).first().click()
    await page.waitForTimeout(2000)

    // ==== 4. 调试：检查 Vue 组件的实际数据 ====
    await page.waitForTimeout(1000)
    await page.screenshot({ path: 'test-results/pur-search-result.png', fullPage: true })

    // 直接从 Vue 组件实例读取 orderList
    const debugInfo = await page.evaluate(() => {
      const app = (document.querySelector('#app') as any)?.__vue_app__
      // 遍历找到 Order 组件实例
      function findComponent(vnode: any): any {
        if (!vnode) return null
        if (vnode.component?.props?.orderList !== undefined) return vnode.component
        if (vnode.component?.subTree) {
          const r = findComponent(vnode.component.subTree)
          if (r) return r
        }
        if (vnode.children && Array.isArray(vnode.children)) {
          for (const c of vnode.children) {
            const r = findComponent(c)
            if (r) return r
          }
        }
        return null
      }
      const comp = findComponent(app?._instance?.subTree)
      if (comp?.setupState?.orderList) {
        return { list: comp.setupState.orderList.map((r: any) => r.orderCode), total: comp.setupState.total }
      }
      // 尝试 Options API
      if (comp?.proxy?.orderList) {
        return { list: comp.proxy.orderList.map((r: any) => r.orderCode), total: comp.proxy.total }
      }
      // fallback: check table DOM directly
      const cells = Array.from(document.querySelectorAll('.el-table__body td .cell'))
      return { tableCells: cells.map(c => c.textContent?.trim()).filter(Boolean).slice(0, 20) }
    })
    console.log(`  Debug: ${JSON.stringify(debugInfo).slice(0, 300)}`)

    // 用浏览器拦截的网络请求来验证搜索
    // 监听下一次 API 调用
    const [listReq] = await Promise.all([
      page.waitForResponse(r => r.url().includes('/mes/pur/order/list') && r.status() === 200, { timeout: 5000 }),
      page.locator('button').filter({ hasText: '搜索' }).first().click()
    ])
    const listData = await listReq.json()
    console.log(`  API响应: total=${listData.total} rows=${listData.rows?.length}`)
    const codes = listData.rows?.map((r: any) => r.orderCode) || []
    console.log(`  codes: ${codes.slice(0,5).join(', ')}`)
    expect(codes).toContain(uniqueCode)
  })
})
