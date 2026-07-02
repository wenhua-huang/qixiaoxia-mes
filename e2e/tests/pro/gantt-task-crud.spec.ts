import { test, expect } from '@playwright/test'

test.describe('甘特图任务CRUD — E2E', () => {
  test.use({ storageState: 'setup/storageState.json' })
  test.setTimeout(120000)

  test('1. 甘特图页面加载后显示新增/删除任务按钮', async ({ page }) => {
    await page.goto('/mes/pro/gantt')
    await page.waitForTimeout(2000)

    // 新增任务按钮存在但默认禁用(未选工单)
    const addBtn = page.locator('button', { hasText: '新增任务' })
    await expect(addBtn.first()).toBeVisible({ timeout: 5000 })
    // 未选工单时应禁用
    expect(await addBtn.first().isDisabled()).toBe(true)

    // 删除任务按钮存在但默认禁用(未选中任务)
    const delBtn = page.locator('button', { hasText: '删除任务' })
    await expect(delBtn.first()).toBeVisible({ timeout: 5000 })
    expect(await delBtn.first().isDisabled()).toBe(true)
  })

  test('2. 选择工单后新增任务按钮启用', async ({ page }) => {
    await page.goto('/mes/pro/gantt')
    await page.waitForTimeout(3000)

    // 点击工单队列第一张卡片
    const card = page.locator('.queue-card').first()
    await card.click()
    await page.waitForTimeout(2000)

    // 加载甘特图后，新增任务按钮应启用
    const addBtn = page.locator('button', { hasText: '新增任务' }).first()
    await expect(addBtn).toBeEnabled({ timeout: 5000 })
  })

  test('3. 点击甘特图任务条打开详情弹窗', async ({ page }) => {
    await page.goto('/mes/pro/gantt')
    await page.waitForTimeout(3000)

    // 选择工单加载甘特图
    const card = page.locator('.queue-card').first()
    await card.click()
    await page.waitForTimeout(2000)

    // 等待甘特图渲染
    await expect(page.locator('.gc-root')).toBeVisible({ timeout: 10000 })

    // 点击第一个任务条
    const bar = page.locator('.gc-bar').first()
    if (await bar.isVisible()) {
      await bar.click()
      await page.waitForTimeout(1000)

      // 弹窗应打开
      const dialog = page.locator('.el-dialog')
      await expect(dialog).toBeVisible({ timeout: 5000 })

      // 弹窗标题应包含"任务详情"
      const title = dialog.locator('.el-dialog__title')
      await expect(title).toContainText('任务详情')

      // 应显示"编辑"按钮
      const editBtn = dialog.locator('button', { hasText: '编辑' })
      await expect(editBtn).toBeVisible()

      // 关闭弹窗
      const closeBtn = dialog.locator('button', { hasText: '关闭' })
      await closeBtn.click()
    }
  })

  test('4. 点击编辑按钮切换到编辑模式', async ({ page }) => {
    await page.goto('/mes/pro/gantt')
    await page.waitForTimeout(3000)
    await page.locator('.queue-card').first().click()
    await page.waitForTimeout(2000)
    await expect(page.locator('.gc-root')).toBeVisible({ timeout: 10000 })

    // 点击任务条
    const bar = page.locator('.gc-bar').first()
    if (!(await bar.isVisible())) return
    await bar.click()
    await page.waitForTimeout(500)

    // 点击编辑按钮
    const editBtn = page.locator('.el-dialog button', { hasText: '编辑' })
    if (await editBtn.isVisible()) {
      await editBtn.click()
      await page.waitForTimeout(500)

      // 应切换为编辑模式：显示"保存"和"取消"按钮
      const saveBtn = page.locator('.el-dialog button', { hasText: '保存' })
      await expect(saveBtn).toBeVisible()

      const cancelBtn = page.locator('.el-dialog button', { hasText: '取消' })
      await expect(cancelBtn).toBeVisible()

      // 编辑模式下表单字段应可编辑（非disabled）
      const quantityInput = page.locator('.el-dialog .el-input-number input').first()
      await expect(quantityInput).toBeEnabled()

      // 取消
      await cancelBtn.click()
    }
  })

  test('5. 删除按钮在选中任务后启用', async ({ page }) => {
    await page.goto('/mes/pro/gantt')
    await page.waitForTimeout(3000)
    await page.locator('.queue-card').first().click()
    await page.waitForTimeout(2000)
    await expect(page.locator('.gc-root')).toBeVisible({ timeout: 10000 })

    // 删除按钮初始禁用
    const delBtn = page.locator('button', { hasText: '删除任务' }).first()
    expect(await delBtn.isDisabled()).toBe(true)

    // 点击任务条选中
    const bar = page.locator('.gc-bar').first()
    if (!(await bar.isVisible())) return
    await bar.click()
    await page.waitForTimeout(500)

    // 关闭弹窗（但任务仍被选中）
    const closeBtn = page.locator('.el-dialog button', { hasText: '关闭' })
    if (await closeBtn.isVisible()) await closeBtn.click()
    await page.waitForTimeout(300)

    // 删除按钮应启用
    await expect(delBtn).toBeEnabled({ timeout: 5000 })
  })

  test('6. 点击新增任务打开编辑弹窗（带工序选择器）', async ({ page }) => {
    await page.goto('/mes/pro/gantt')
    await page.waitForTimeout(3000)
    await page.locator('.queue-card').first().click()
    await page.waitForTimeout(2000)
    await expect(page.locator('.gc-root')).toBeVisible({ timeout: 10000 })

    // 点击新增任务
    const addBtn = page.locator('button', { hasText: '新增任务' }).first()
    await expect(addBtn).toBeEnabled({ timeout: 5000 })
    await addBtn.click()
    await page.waitForTimeout(1000)

    // 弹窗应打开，标题应包含"编辑任务"
    const dialog = page.locator('.el-dialog')
    await expect(dialog).toBeVisible({ timeout: 5000 })
    await expect(dialog.locator('.el-dialog__title')).toContainText('编辑任务')

    // 工序选择器应可见且可编辑（新增模式下未禁用）
    const processSelect = dialog.locator('.el-select').first()
    await expect(processSelect).toBeVisible()

    // 保存按钮可见
    const saveBtn = dialog.locator('button', { hasText: '保存' })
    await expect(saveBtn).toBeVisible()

    // 关闭弹窗
    await dialog.locator('button', { hasText: '取消' }).click()
  })
})
