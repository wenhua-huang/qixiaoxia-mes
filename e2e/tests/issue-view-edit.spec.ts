import { test, expect } from '@playwright/test'

test.describe('生产领料单', () => {
  test.use({ storageState: 'setup/storageState.json' })

  test('查看：弹窗显示头信息', async ({ page }) => {
    // 等列表加载
    const listP = page.waitForResponse(r => r.url().includes('/mes/wm/issueheader/list') && r.status() === 200, { timeout: 15000 })
    await page.goto('/mes/wm/issue')
    await listP
    await expect(page.locator('.el-table__body tr').first()).toBeVisible({ timeout: 10000 })

    // 等详情API
    const detailP = page.waitForResponse(r => r.url().includes('/mes/wm/issueheader/') && !r.url().includes('/list') && r.status() === 200, { timeout: 10000 })

    // 点查看按钮（第一个按钮）
    const firstRow = page.locator('.el-table__body tr').first()
    await firstRow.locator('td').last().locator('button').first().click()

    // 等API返回
    await detailP
    await page.waitForTimeout(1000)

    // 弹窗可见
    const dialog = page.locator('.el-dialog').last()
    await expect(dialog).toBeVisible({ timeout: 5000 })

    // 直接查看DOM中所有input
    const allInputs = dialog.locator('input')
    const count = await allInputs.count()
    console.log(`Total inputs: ${count}`)
    for (let i = 0; i < Math.min(count, 5); i++) {
      const type = await allInputs.nth(i).getAttribute('type')
      const disabled = await allInputs.nth(i).isDisabled()
      const val = await allInputs.nth(i).inputValue()
      const placeholder = await allInputs.nth(i).getAttribute('placeholder')
      console.log(`  input[${i}]: type=${type} disabled=${disabled} placeholder="${placeholder}" value="${val}"`)
    }

    // 检查el-input的placeholder（我们改成了DEBUG:xxx）
    // Read window debug vars
    const debug = await page.evaluate(() => ({
      rData: (window as any).__QX_DEBUG,
      form: (window as any).__QX_FORM,
    }))
    console.log('DEBUG r.data keys:', debug.rData?.['r.data keys'])
    console.log('DEBUG form issueCode:', debug.form?.issueCode)
    console.log('DEBUG form keys:', debug.form ? Object.keys(debug.form).slice(0,8) : 'NULL')

    if (debug.form?.issueCode) {
      console.log('✅ FORM IS SET - v-model rendering issue')
    } else if (debug.rData?.['r.data']?.data) {
      console.log('❌ Double-wrapped: r.data.data exists')
    } else {
      console.log('❌ Form not set - unknown issue')
    }
  })

  test('弹窗可关闭', async ({ page }) => {
    const listP = page.waitForResponse(r => r.url().includes('/mes/wm/issueheader/list') && r.status() === 200, { timeout: 15000 })
    await page.goto('/mes/wm/issue')
    await listP
    await expect(page.locator('.el-table__body tr').first()).toBeVisible({ timeout: 10000 })

    const firstRow = page.locator('.el-table__body tr').first()
    await firstRow.locator('td').last().locator('button').first().click()
    const dialog = page.locator('.el-dialog').last()
    await expect(dialog).toBeVisible({ timeout: 5000 })

    await dialog.locator('button:has-text("取 消")').click()
    await page.waitForTimeout(500)
    await expect(dialog).not.toBeVisible({ timeout: 3000 })
  })
})
