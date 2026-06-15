import { test, expect } from '@playwright/test'

test.describe('采购订单管理', () => {
  test.use({ storageState: 'setup/storageState.json' })

  test('新增采购订单 — 菜单点击+填写+提交', async ({ page }) => {
    // === 1. 宽屏 + 等加载 ===
    await page.setViewportSize({ width: 1920, height: 1080 })
    await page.goto('/')
    await page.waitForTimeout(5000)
    await page.screenshot({ path: 'test-results/01-home.png' })

    // === 2. JS 直接操作：展开侧边栏 → 点菜单 → 等路由 ===
    await page.evaluate(() => {
      // 展开侧边栏（如果折叠了）
      const hamburger = document.querySelector('.hamburger-container, .fold-unfold, .navbar-breadcrumb-container + div')
      if (hamburger) (hamburger as HTMLElement).click()

      // 等一下 sidebar 展开
      setTimeout(() => {
        // 找到"采购管理" sub-menu，模拟展开
        const submenus = document.querySelectorAll('.el-sub-menu')
        for (const sm of Array.from(submenus)) {
          const title = sm.querySelector('.el-sub-menu__title')
          if (title?.textContent?.includes('采购管理')) {
            sm.setAttribute('opened', '')
            sm.classList.add('is-opened')
            // 展开状态
          }
        }
        // 等一下展开动画，然后点击"采购订单"
        setTimeout(() => {
          const items = document.querySelectorAll('.el-menu-item')
          for (const item of Array.from(items)) {
            if (item.textContent?.trim() === '采购订单') {
              (item as HTMLElement).click()
              break
            }
          }
        }, 500)
      }, 500)
    })
    await page.waitForTimeout(5000)
    await page.screenshot({ path: 'test-results/02-order-list.png' })

    // 如果上面没成功，用 Vue Router 重试
    const hasTable = await page.locator('.el-table').first().isVisible({ timeout: 2000 }).catch(() => false)
    if (!hasTable) {
      console.log('  ⚠️ 菜单点击未生效，尝试 Vue Router 导航...')
      await page.evaluate(() => {
        const app = (document.querySelector('#app') as any)?.__vue_app__
        if (app) {
          const router = app.config.globalProperties.$router
          // 尝试多种路径
          router.push('/pur/order').catch(() => router.push('/order'))
        }
      })
      await page.waitForTimeout(5000)
    }

    // === 3. 验证表格存在 ===
    const table = page.locator('.el-table').first()
    await expect(table).toBeVisible({ timeout: 10000 })
    console.log('  ✅ 采购订单列表页加载成功')

    // === 4. 点击新增 ===
    const addBtn = page.locator('button, .el-button').filter({ hasText: /新增/ }).first()
    await addBtn.click({ force: true, timeout: 10000 })
    await page.waitForTimeout(1000)

    const dialog = page.locator('.el-dialog').first()
    await expect(dialog).toBeVisible({ timeout: 5000 })
    await page.screenshot({ path: 'test-results/03-add-dialog.png' })
    console.log('  ✅ 新增弹窗打开')

    // === 5. 填写表单 ===
    // 订单名称
    const nameInput = dialog.getByPlaceholder('订单名称').first()
    if (await nameInput.isVisible({ timeout: 2000 }).catch(() => false)) {
      await nameInput.fill('E2E测试-采购订单')
    }

    // 供应商 — 点击搜索
    const searchBtn = dialog.locator('.el-input-group__append .el-button, button').first()
    if (await searchBtn.isVisible({ timeout: 2000 }).catch(() => false)) {
      await searchBtn.click()
      await page.waitForTimeout(1500)

      // 供应商选择弹窗
      const vendorDialog = page.locator('.el-dialog').filter({ hasText: /供应商选择|全称/ }).last()
      await expect(vendorDialog).toBeVisible({ timeout: 5000 })
      // 双击第一行
      await vendorDialog.locator('.el-table__body tr').first().dblclick()
      await page.waitForTimeout(500)
      console.log('  ✅ 供应商已选择')
    }

    // 采购类型 — 下拉
    const select1 = dialog.locator('.el-select').first()
    await select1.click()
    await page.waitForTimeout(300)
    await page.locator('.el-select-dropdown__item').filter({ hasText: '纸张' }).first().click()
    await page.waitForTimeout(300)

    // 币种
    const selects = dialog.locator('.el-select')
    if (await selects.count() >= 2) {
      await selects.nth(1).click()
      await page.waitForTimeout(300)
      await page.locator('.el-select-dropdown__item').filter({ hasText: '人民币' }).first().click()
      await page.waitForTimeout(300)
    }

    await page.screenshot({ path: 'test-results/04-form-filled.png' })

    // === 6. 拦截提交 + 保存 ===
    const [request] = await Promise.all([
      page.waitForRequest(
        req => req.method() === 'POST' && req.url().includes('/mes/pur/order'),
        { timeout: 15000 }
      ),
      dialog.locator('button').filter({ hasText: /保存/ }).first().click()
    ])

    const body = request.postDataJSON()
    console.log('  📤 POST body: ' + JSON.stringify(body))

    const bodyStr = JSON.stringify(body)
    expect(bodyStr).not.toContain('yyyy-')
    expect(bodyStr).not.toContain('-Su')
    expect(bodyStr).not.toContain('-Mo')
    expect(bodyStr).not.toContain('-Tu')
    expect(bodyStr).not.toContain('xxx')
    if (body.orderCode) expect(body.orderCode).toMatch(/^PO\d+/)
    if (body.orderDate) expect(body.orderDate).toMatch(/^\d{4}-\d{2}-\d{2}$/)

    console.log('  🎉 提交JSON验证全部通过')
  })
})
