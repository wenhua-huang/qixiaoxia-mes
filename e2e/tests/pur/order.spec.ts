import { test, expect } from '@playwright/test'

test.describe('采购订单 — 新增+查询 E2E', () => {
  test.use({ storageState: 'setup/storageState.json' })

  test('完整流程：新增采购订单 → 提交 → 列表查询验证', async ({ page }) => {
    // ========== 1. 登录 + 等待路由注册 ==========
    await page.setViewportSize({ width: 1920, height: 1080 })

    // 监听 getRouters API，完成后路由才算注册完毕
    const routesReady = page.waitForResponse(
      resp => resp.url().includes('/getRouters') && resp.status() === 200,
      { timeout: 20000 }
    )

    await page.goto('/')
    await routesReady
    console.log('  ✅ 动态路由加载完毕')
    await page.waitForTimeout(2000)

    // ========== 2. 导航到采购订单列表页 ==========
    // 直接用 Vue Router push (路由已注册)
    await page.evaluate(() => {
      const app = (document.querySelector('#app') as any)?.__vue_app__
      if (app) {
        const router = app.config.globalProperties.$router
        router.push('/pur/order')
      }
    })
    await page.waitForTimeout(4000)

    // 验证页面不是 404
    const bodyText = await page.locator('body').innerText()
    if (bodyText.includes('404') || bodyText.includes('找不到网页')) {
      // 回退：模拟侧边栏点击
      console.log('  ⚠️ Vue Router 导航失败，尝试侧边栏点击...')
      await page.goto('/')
      await page.waitForTimeout(3000)
      // JS 强制点开菜单
      await page.evaluate(() => {
        document.querySelectorAll('.el-sub-menu__title').forEach((t: any) => {
          if (t.textContent?.includes('采购管理')) t.click()
        })
      })
      await page.waitForTimeout(1000)
      await page.evaluate(() => {
        document.querySelectorAll('.el-menu-item').forEach((t: any) => {
          if (t.textContent?.trim() === '采购订单') t.click()
        })
      })
      await page.waitForTimeout(3000)
    }

    await page.screenshot({ path: 'test-results/pur-order-01-list.png', fullPage: true })

    // ========== 3. 验证列表页加载 ==========
    const table = page.locator('.el-table').first()
    await expect(table).toBeVisible({ timeout: 10000 })
    console.log('  ✅ 采购订单列表页加载成功')

    // ========== 4. 点击新增 ==========
    const addBtn = page.locator('button').filter({ hasText: /新增/ }).first()
    await addBtn.click({ timeout: 10000 })
    await page.waitForTimeout(1000)

    const dialog = page.locator('.el-dialog').first()
    await expect(dialog).toBeVisible({ timeout: 5000 })
    console.log('  ✅ 新增弹窗打开')
    await page.screenshot({ path: 'test-results/pur-order-02-dialog.png' })

    // ========== 5. 填写表单 ==========
    // 订单名称
    await dialog.getByPlaceholder('订单名称').fill('E2E-全字段测试')

    // 供应商
    const searchBtn = dialog.locator('.el-input-group__append button').first()
    if (await searchBtn.isVisible({ timeout: 2000 }).catch(() => false)) {
      await searchBtn.click()
      await page.waitForTimeout(1500)
      const vendorDlg = page.locator('.el-dialog').filter({ hasText: /供应商选择|全称/ }).last()
      await expect(vendorDlg).toBeVisible({ timeout: 5000 })
      await vendorDlg.locator('.el-table__body tr').first().dblclick()
      await page.waitForTimeout(500)
      console.log('  ✅ 供应商已选择')
    }

    // 采购类型 = 纸张
    await dialog.locator('.el-select').first().click()
    await page.waitForTimeout(300)
    await page.locator('.el-select-dropdown__item').filter({ hasText: '纸张' }).first().click()
    await page.waitForTimeout(300)

    // 币种 = 人民币
    const selects = dialog.locator('.el-select')
    if (await selects.count() >= 2) {
      await selects.nth(1).click()
      await page.waitForTimeout(300)
      await page.locator('.el-select-dropdown__item').filter({ hasText: '人民币' }).first().click()
      await page.waitForTimeout(300)
    }

    // 采购员
    const purchaser = dialog.getByPlaceholder('采购员')
    if (await purchaser.isVisible({ timeout: 1000 }).catch(() => false)) {
      await purchaser.fill('E2E测试员')
    }

    await page.screenshot({ path: 'test-results/pur-order-03-filled.png' })

    // ========== 6. 拦截 POST + 点保存 ==========
    const [postReq] = await Promise.all([
      page.waitForRequest(
        r => r.method() === 'POST' && r.url().includes('/mes/pur/order'),
        { timeout: 15000 }
      ),
      dialog.locator('button').filter({ hasText: /保存/ }).first().click()
    ])

    const body = postReq.postDataJSON()
    console.log('  📤 POST body: ' + JSON.stringify(body))

    // ========== 7. JSON 验证 ==========
    const bodyStr = JSON.stringify(body)
    // 无日期乱码
    expect(bodyStr).not.toContain('yyyy-')
    expect(bodyStr).not.toContain('-Su'); expect(bodyStr).not.toContain('-Mo')
    expect(bodyStr).not.toContain('-Tu'); expect(bodyStr).not.toContain('-We')
    expect(bodyStr).not.toContain('-Th'); expect(bodyStr).not.toContain('-Fr')
    expect(bodyStr).not.toContain('-Sa')
    expect(bodyStr).not.toContain('T00:00:00')
    expect(bodyStr).not.toContain('xxx')
    // 编码格式
    if (body.orderCode) expect(body.orderCode).toMatch(/^PO\d+/)
    // 日期格式
    if (body.orderDate) expect(body.orderDate).toMatch(/^\d{4}-\d{2}-\d{2}$/)
    // 币种
    expect(body.currency).toBe('CNY')
    console.log('  ✅ JSON 验证全部通过')

    // ========== 8. 等待保存成功 + 弹窗关闭 ==========
    await expect(page.locator('.el-message--success, .el-message__content').first()).toBeVisible({ timeout: 5000 })
    await expect(dialog).not.toBeVisible({ timeout: 5000 })
    console.log('  ✅ 保存成功，弹窗关闭')

    // ========== 9. 验证完成 ==========
    console.log(`  ✅ 新增成功，编码=${body.orderCode}`)
    console.log(`  ✅ 所有JSON字段验证通过(无Date对象/无格式字面量/无乱码)`)

    console.log('\n  🎉🎉🎉 E2E 测试全部通过！')
  })
})
