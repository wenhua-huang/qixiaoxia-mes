import { test, expect } from '@playwright/test'

test.describe('流转卡 — 列表筛选', () => {
  test.use({ storageState: 'setup/storageState.json' })
  test.setTimeout(120_000)

  /**
   * 辅助函数：导航到流转卡页面并等待表格渲染 + 数据加载完成
   */
  async function navigateToCardPage(page: any) {
    // 先开始监听 API 响应，再导航，确保消费掉组件 mount 触发的初始 getList() 响应
    const initialListPromise = page.waitForResponse(
      r => r.url().includes('/mes/pro/procard/list') && r.status() === 200,
      { timeout: 15_000 }
    )
    await page.goto('/mes/pro/card')
    await initialListPromise
    await expect(page.locator('.el-table').first()).toBeVisible({ timeout: 5_000 })
    await expect(page.locator('.el-form').first()).toBeVisible({ timeout: 5_000 })
  }

  /**
   * 辅助函数：重置搜索条件（消费重置触发的 API 响应，避免竞态）
   */
  async function resetFilters(page: any) {
    const resetBtn = page.locator('button').filter({ hasText: '重置' }).first()
    const [resetResp] = await Promise.all([
      page.waitForResponse(
        r => r.url().includes('/mes/pro/procard/list') && r.status() === 200,
        { timeout: 10_000 }
      ),
      resetBtn.click()
    ])
    await page.waitForTimeout(200)
  }

  test('F1: cardCode 精确搜索 → URL参数 + 结果校验', async ({ page }) => {
    await navigateToCardPage(page)
    await resetFilters(page)

    // 先查一次获取一个已知的 cardCode
    const [firstResp] = await Promise.all([
      page.waitForResponse(
        r => r.url().includes('/mes/pro/procard/list') && r.status() === 200,
        { timeout: 10_000 }
      ),
      page.locator('button').filter({ hasText: '搜索' }).first().click()
    ])
    const firstBody = await firstResp.json()
    if (firstBody.total === 0) {
      console.log('  ⚠️ 无流转卡记录，跳过 cardCode 筛选测试')
      return
    }
    const knownCode = firstBody.rows[0].cardCode

    // 填入 cardCode 并搜索
    await resetFilters(page)
    const codeInput = page.locator('.el-form-item').filter({ hasText: '流转卡编码' }).locator('input').first()
    await codeInput.fill(knownCode)
    // 等待 Vue v-model 响应式更新
    await page.waitForTimeout(200)

    const [filteredResp] = await Promise.all([
      page.waitForResponse(
        r => r.url().includes('/mes/pro/procard/list') && r.status() === 200,
        { timeout: 10_000 }
      ),
      page.locator('button').filter({ hasText: '搜索' }).first().click()
    ])

    const filterUrl = new URL(filteredResp.url())
    expect(filterUrl.searchParams.get('cardCode')).toBe(knownCode)
    console.log(`  ✅ URL 包含 cardCode=${knownCode}`)

    const body = await filteredResp.json()
    expect(body.total).toBeGreaterThanOrEqual(1)
    body.rows.forEach((row: any) => {
      expect(row.cardCode).toBe(knownCode)
    })
    console.log(`  ✅ 返回 ${body.total} 条记录，全部 cardCode=${knownCode}`)
  })

  test('F2: workorderName 模糊搜索 → URL参数 + 结果内容校验', async ({ page }) => {
    await navigateToCardPage(page)
    await resetFilters(page)

    // 先查全部拿一条工单名称
    const [allResp] = await Promise.all([
      page.waitForResponse(
        r => r.url().includes('/mes/pro/procard/list') && r.status() === 200,
        { timeout: 10_000 }
      ),
      page.locator('button').filter({ hasText: '搜索' }).first().click()
    ])
    const allBody = await allResp.json()
    if (allBody.total === 0) {
      console.log('  ⚠️ 无流转卡记录，跳过 workorderName 筛选测试')
      return
    }
    const knownWoName: string = allBody.rows[0].workorderName
    if (!knownWoName) {
      console.log('  ⚠️ 流转卡记录无 workorderName，跳过')
      return
    }
    const searchTerm = knownWoName.substring(0, Math.min(4, knownWoName.length))

    const woInput = page.locator('.el-form-item').filter({ hasText: '工单名称' }).locator('input').first()
    await woInput.fill(searchTerm)

    const [filteredResp] = await Promise.all([
      page.waitForResponse(
        r => r.url().includes('/mes/pro/procard/list') && r.status() === 200,
        { timeout: 10_000 }
      ),
      page.locator('button').filter({ hasText: '搜索' }).first().click()
    ])

    const filterUrl = new URL(filteredResp.url())
    expect(filterUrl.searchParams.get('workorderName')).toBe(searchTerm)
    console.log(`  ✅ URL 包含 workorderName=${searchTerm}`)

    const body = await filteredResp.json()
    expect(body.total).toBeGreaterThanOrEqual(1)
    const matchedRows = body.rows.filter((row: any) => row.workorderName !== null)
    // 验证所有非 null workorderName 都包含搜索词
    expect(matchedRows.length).toBeGreaterThan(0)
    matchedRows.forEach((row: any) => {
      expect(row.workorderName).toContain(searchTerm)
    })
    console.log(`  ✅ 返回 ${body.total} 条记录，其中 ${matchedRows.length} 条 workorderName 包含"${searchTerm}"`)
  })

  test('F3: itemName 模糊搜索 → URL参数 + 结果内容校验', async ({ page }) => {
    await navigateToCardPage(page)
    await resetFilters(page)

    // 先查全部拿一条产品名称
    const [allResp] = await Promise.all([
      page.waitForResponse(
        r => r.url().includes('/mes/pro/procard/list') && r.status() === 200,
        { timeout: 10_000 }
      ),
      page.locator('button').filter({ hasText: '搜索' }).first().click()
    ])
    const allBody = await allResp.json()
    if (allBody.total === 0) {
      console.log('  ⚠️ 无流转卡记录，跳过 itemName 筛选测试')
      return
    }
    const knownItemName: string = allBody.rows[0].itemName
    if (!knownItemName) {
      console.log('  ⚠️ 流转卡记录无 itemName，跳过')
      return
    }
    const searchTerm = knownItemName.substring(0, Math.min(3, knownItemName.length))

    const itemInput = page.locator('.el-form-item').filter({ hasText: '产品名称' }).locator('input').first()
    await itemInput.fill(searchTerm)

    const [filteredResp] = await Promise.all([
      page.waitForResponse(
        r => r.url().includes('/mes/pro/procard/list') && r.status() === 200,
        { timeout: 10_000 }
      ),
      page.locator('button').filter({ hasText: '搜索' }).first().click()
    ])

    const filterUrl = new URL(filteredResp.url())
    expect(filterUrl.searchParams.get('itemName')).toBe(searchTerm)
    console.log(`  ✅ URL 包含 itemName=${searchTerm}`)

    const body = await filteredResp.json()
    if (body.total > 0) {
      body.rows.forEach((row: any) => {
        expect(row.itemName).toContain(searchTerm)
      })
      console.log(`  ✅ 返回 ${body.total} 条记录，全部 itemName 包含"${searchTerm}"`)
    } else {
      console.log(`  ⚠️ 无 itemName 包含"${searchTerm}"的记录（total=0），但筛选参数正确传递`)
    }
  })

  test('F4: status 下拉筛选 → URL参数 + 返回全部匹配', async ({ page }) => {
    await navigateToCardPage(page)
    await resetFilters(page)

    // 打开状态下拉框并选择"流转中"
    const statusSelect = page.locator('.el-form-item').filter({ hasText: '状态' }).locator('.el-select').first()
    await statusSelect.click()
    await page.waitForTimeout(300)
    const activeOption = page.locator('.el-select-dropdown__item').filter({ hasText: '流转中' }).first()
    await activeOption.click()
    await page.waitForTimeout(200)

    const [filteredResp] = await Promise.all([
      page.waitForResponse(
        r => r.url().includes('/mes/pro/procard/list') && r.status() === 200,
        { timeout: 10_000 }
      ),
      page.locator('button').filter({ hasText: '搜索' }).first().click()
    ])

    const filterUrl = new URL(filteredResp.url())
    expect(filterUrl.searchParams.get('status')).toBe('ACTIVE')
    console.log(`  ✅ URL 包含 status=ACTIVE`)

    const body = await filteredResp.json()
    body.rows.forEach((row: any) => {
      expect(row.status).toBe('ACTIVE')
    })
    console.log(`  ✅ 返回 ${body.total} 条记录，全部 status=ACTIVE`)
  })

  test('F5: 重置按钮 → 清空筛选参数', async ({ page }) => {
    await navigateToCardPage(page)
    await resetFilters(page)

    // 先填入一个筛选值
    const codeInput = page.locator('.el-form-item').filter({ hasText: '流转卡编码' }).locator('input').first()
    await codeInput.fill('TEST_CARD')

    // 点击重置
    await resetFilters(page)

    // 再次搜索，验证URL不含筛选参数
    const [resp] = await Promise.all([
      page.waitForResponse(
        r => r.url().includes('/mes/pro/procard/list') && r.status() === 200,
        { timeout: 10_000 }
      ),
      page.locator('button').filter({ hasText: '搜索' }).first().click()
    ])

    const respUrl = new URL(resp.url())
    expect(respUrl.searchParams.get('cardCode')).toBeNull()
    console.log(`  ✅ 重置后 URL 不含 cardCode 参数`)
  })

  test('F6: 不存在的 cardCode 搜索 → 返回空结果', async ({ page }) => {
    await navigateToCardPage(page)
    await resetFilters(page)

    const codeInput = page.locator('.el-form-item').filter({ hasText: '流转卡编码' }).locator('input').first()
    await codeInput.fill('NONEXISTENT_CARD_999999')

    const [filteredResp] = await Promise.all([
      page.waitForResponse(
        r => r.url().includes('/mes/pro/procard/list') && r.status() === 200,
        { timeout: 10_000 }
      ),
      page.locator('button').filter({ hasText: '搜索' }).first().click()
    ])

    const body = await filteredResp.json()
    expect(body.total).toBe(0)
    console.log(`  ✅ 不存在的流转卡编码返回 total=0`)
  })

  test('F7: 组合筛选 workorderName + status → 多条件同时生效', async ({ page }) => {
    await navigateToCardPage(page)
    await resetFilters(page)

    // 先查全部拿到一条工单名称
    const [allResp] = await Promise.all([
      page.waitForResponse(
        r => r.url().includes('/mes/pro/procard/list') && r.status() === 200,
        { timeout: 10_000 }
      ),
      page.locator('button').filter({ hasText: '搜索' }).first().click()
    ])
    const allBody = await allResp.json()
    if (allBody.total === 0) {
      console.log('  ⚠️ 无流转卡记录，跳过组合筛选测试')
      return
    }
    const knownWoName: string = allBody.rows[0].workorderName
    if (!knownWoName) {
      console.log('  ⚠️ 流转卡记录无 workorderName，跳过组合筛选')
      return
    }
    const searchTerm = knownWoName.substring(0, Math.min(4, knownWoName.length))

    // 填入工单名称
    const woInput = page.locator('.el-form-item').filter({ hasText: '工单名称' }).locator('input').first()
    await woInput.fill(searchTerm)

    // 选择状态
    const statusSelect = page.locator('.el-form-item').filter({ hasText: '状态' }).locator('.el-select').first()
    await statusSelect.click()
    await page.waitForTimeout(300)
    const activeOption = page.locator('.el-select-dropdown__item').filter({ hasText: '流转中' }).first()
    await activeOption.click()
    await page.waitForTimeout(200)

    const [filteredResp] = await Promise.all([
      page.waitForResponse(
        r => r.url().includes('/mes/pro/procard/list') && r.status() === 200,
        { timeout: 10_000 }
      ),
      page.locator('button').filter({ hasText: '搜索' }).first().click()
    ])

    const filterUrl = new URL(filteredResp.url())
    expect(filterUrl.searchParams.get('workorderName')).toBe(searchTerm)
    expect(filterUrl.searchParams.get('status')).toBe('ACTIVE')
    console.log(`  ✅ URL 同时包含 workorderName=${searchTerm} 和 status=ACTIVE`)

    const body = await filteredResp.json()
    const matchedRows = body.rows.filter((row: any) => row.workorderName !== null)
    expect(matchedRows.length).toBeGreaterThan(0)
    matchedRows.forEach((row: any) => {
      expect(row.workorderName).toContain(searchTerm)
      expect(row.status).toBe('ACTIVE')
    })
    console.log(`  ✅ 返回 ${body.total} 条记录，其中 ${matchedRows.length} 条同时满足双条件`)
  })
})

test.describe('流转卡 — 新增记录', () => {
  test.use({ storageState: 'setup/storageState.json' })
  test.setTimeout(120_000)

  async function navigateToCardPage(page: any) {
    const initialListPromise = page.waitForResponse(
      r => r.url().includes('/mes/pro/procard/list') && r.status() === 200,
      { timeout: 15_000 }
    )
    await page.goto('/mes/pro/card')
    await initialListPromise
    await expect(page.locator('.el-table').first()).toBeVisible({ timeout: 5_000 })
    await expect(page.locator('.el-form').first()).toBeVisible({ timeout: 5_000 })
  }

  test('A1: 新增对话框 → 打开后验证表单元素存在', async ({ page }) => {
    await navigateToCardPage(page)

    // 点击新增按钮
    await page.locator('button').filter({ hasText: '新增' }).first().click()
    await page.waitForTimeout(500)

    // 验证对话框已打开
    const dialog = page.locator('.el-dialog').first()
    await expect(dialog).toBeVisible({ timeout: 5_000 })

    // 验证对话框标题
    await expect(dialog.locator('.el-dialog__title')).toHaveText('新增流转卡')

    // 验证表单字段存在
    await expect(dialog.locator('.el-form-item').filter({ hasText: '流转卡编码' })).toBeVisible()
    await expect(dialog.locator('.el-form-item').filter({ hasText: '生产工单' })).toBeVisible()
    await expect(dialog.locator('.el-form-item').filter({ hasText: '批次号' })).toBeVisible()
    await expect(dialog.locator('.el-form-item').filter({ hasText: '流转数量' })).toBeVisible()
    await expect(dialog.locator('.el-form-item').filter({ hasText: '状态' })).toBeVisible()
    await expect(dialog.locator('.el-form-item').filter({ hasText: '备注' })).toBeVisible()
    console.log('  ✅ 新增对话框所有表单字段存在')

    // 验证自动生成开关存在
    await expect(dialog.locator('.el-switch')).toBeVisible()
    console.log('  ✅ 自动生成开关存在')

    // 验证 Tabs 存在
    await expect(dialog.locator('.el-tabs')).toBeVisible()
    console.log('  ✅ 对话框含 Tabs (基本信息 / 工序流转详情)')

    // 验证"基本信息" Tab 默认激活
    const headerTab = dialog.locator('.el-tabs__item').filter({ hasText: '基本信息' }).first()
    await expect(headerTab).toHaveClass(/is-active/)
    console.log('  ✅ "基本信息" Tab 默认激活')

    // 新增模式不应显示"工序流转详情" Tab（cardId == null）
    const processTab = dialog.locator('.el-tabs__item').filter({ hasText: '工序流转详情' })
    await expect(processTab).toHaveCount(0)
    console.log('  ✅ 新增模式无"工序流转详情" Tab')

    // 关闭对话框
    await page.locator('.el-dialog__footer button').filter({ hasText: '关 闭' }).first().click()
  })

  test('A2: 自动生成流转卡编码 → 开关切换后自动填充编码', async ({ page }) => {
    await navigateToCardPage(page)

    // 点击新增
    await page.locator('button').filter({ hasText: '新增' }).first().click()
    await page.waitForTimeout(500)

    const dialog = page.locator('.el-dialog').first()
    await expect(dialog).toBeVisible({ timeout: 5_000 })

    // 流转卡编码输入框初始为空
    const cardCodeInput = dialog.locator('.el-form-item').filter({ hasText: '流转卡编码' }).locator('input').first()
    const initialValue = await cardCodeInput.inputValue()
    expect(initialValue).toBe('')
    console.log(`  ✅ 初始流转卡编码为空`)

    // 切换自动生成开关（on），并等待 API 返回
    const autoGenSwitch = dialog.locator('.el-switch').first()
    const [autoGenResp] = await Promise.all([
      page.waitForResponse(
        r => r.url().includes('/mes/sys/autocoderule/genSerialCode/') && r.status() === 200,
        { timeout: 10_000 }
      ),
      autoGenSwitch.locator('.el-switch__core').first().click()
    ])
    const autoGenBody = await autoGenResp.json()
    console.log(`  ✅ 自动编码 API 返回: ${JSON.stringify(autoGenBody)}`)

    // 编码应被自动填充
    const afterGenValue = await cardCodeInput.inputValue()
    expect(afterGenValue).toBeTruthy()
    expect(afterGenValue.length).toBeGreaterThan(0)
    console.log(`  ✅ 自动生成编码: "${afterGenValue}"`)

    // 关闭对话框
    await page.locator('.el-dialog__footer button').filter({ hasText: '关 闭' }).first().click()
  })

  test('A3: 完整新增流程 → 选择工单 + 填写表单 + 提交', async ({ page }) => {
    await navigateToCardPage(page)

    // 点击新增
    await page.locator('button').filter({ hasText: '新增' }).first().click()
    await page.waitForTimeout(500)

    const dialog = page.locator('.el-dialog').first()
    await expect(dialog).toBeVisible({ timeout: 5_000 })

    // Step 1: 自动生成流转卡编码，并等待 API 返回
    const autoGenSwitch = dialog.locator('.el-switch').first()
    const [autoGenResp] = await Promise.all([
      page.waitForResponse(
        r => r.url().includes('/mes/sys/autocoderule/genSerialCode/') && r.status() === 200,
        { timeout: 10_000 }
      ),
      autoGenSwitch.locator('.el-switch__core').first().click()
    ])
    const autoGenBody = await autoGenResp.json()
    console.log(`  ✅ 自动编码 API 返回 code=${autoGenBody.code}`)

    // 记录生成的编码用于后续校验
    const cardCodeInput = dialog.locator('.el-form-item').filter({ hasText: '流转卡编码' }).locator('input').first()
    const generatedCode = await cardCodeInput.inputValue()
    console.log(`  📝 流转卡编码: "${generatedCode}"`)

    // Step 2: 选择生产工单
    // 点击工单输入框右侧的搜索按钮
    const woSearchBtn = dialog.locator('.el-form-item').filter({ hasText: '生产工单' }).locator('button').first()
    await woSearchBtn.click()
    await page.waitForTimeout(500)

    // 工单选择对话框出现
    const woDialog = page.locator('.el-dialog').filter({ hasText: '生产工单选择' }).last()
    await expect(woDialog).toBeVisible({ timeout: 5_000 })
    console.log('  ✅ 生产工单选择对话框已打开')

    // 等待工单列表加载
    await expect(woDialog.locator('.el-table').first()).toBeVisible({ timeout: 10_000 })

    // 勾选第一条工单（radio 按钮）
    const firstRadio = woDialog.locator('.el-table tbody tr').first().locator('.el-radio').first()
    await firstRadio.click()
    await page.waitForTimeout(300)

    // 点击"确 定"
    await woDialog.locator('button').filter({ hasText: '确 定' }).first().click()
    await page.waitForTimeout(500)

    // 工单对话框关闭，工单名称已回填
    const woInput = dialog.locator('.el-form-item').filter({ hasText: '生产工单' }).locator('input').first()
    const selectedWoName = await woInput.inputValue()
    expect(selectedWoName).toBeTruthy()
    console.log(`  ✅ 已选择工单: "${selectedWoName}"`)

    // Step 3: 生成批次号
    const genBatchBtn = dialog.locator('.el-form-item').filter({ hasText: '批次号' }).locator('button').first()
    await genBatchBtn.click()
    await page.waitForTimeout(200)
    const batchInput = dialog.locator('.el-form-item').filter({ hasText: '批次号' }).locator('input').first()
    const batchValue = await batchInput.inputValue()
    expect(batchValue).toBeTruthy()
    console.log(`  ✅ 已生成批次号: "${batchValue}"`)

    // Step 4: 设置流转数量
    const quantityInput = dialog.locator('.el-form-item').filter({ hasText: '流转数量' }).locator('input').first()
    await quantityInput.fill('')
    await quantityInput.fill('100')
    console.log('  ✅ 已设置流转数量: 100')

    // Step 5: 提交表单
    // 拦截 add API 响应
    const [addResp] = await Promise.all([
      page.waitForResponse(
        r => r.url().includes('/mes/pro/procard') && r.request().method() === 'POST' && r.status() === 200,
        { timeout: 10_000 }
      ),
      dialog.locator('button').filter({ hasText: '确 定' }).first().click()
    ])

    // 验证 API 返回成功
    const addBody = await addResp.json()
    expect(addBody.code).toBe(200)
    console.log(`  ✅ 新增 API 返回 code=200`)

    // 验证成功消息
    await expect(page.locator('.el-message--success').first()).toBeVisible({ timeout: 5_000 })
    console.log('  ✅ 显示"新增成功"消息')

    // 对话框关闭
    await expect(dialog).not.toBeVisible({ timeout: 5_000 })

    // Step 6: 验证新记录出现在列表中
    await page.waitForTimeout(500)
    // 用生成的编码搜索，验证能查到
    const searchCodeInput = page.locator('.el-form-item').filter({ hasText: '流转卡编码' }).locator('input').first()
    await searchCodeInput.fill(generatedCode)

    const [searchResp] = await Promise.all([
      page.waitForResponse(
        r => r.url().includes('/mes/pro/procard/list') && r.status() === 200,
        { timeout: 10_000 }
      ),
      page.locator('button').filter({ hasText: '搜索' }).first().click()
    ])

    const searchBody = await searchResp.json()
    expect(searchBody.total).toBeGreaterThanOrEqual(1)
    expect(searchBody.rows[0].cardCode).toBe(generatedCode)
    console.log(`  ✅ 列表中可查询到新增记录 cardCode=${generatedCode}`)
  })

  test('A4: 新增对话框 → 表单校验（未填必填项无法提交）', async ({ page }) => {
    await navigateToCardPage(page)

    // 点击新增
    await page.locator('button').filter({ hasText: '新增' }).first().click()
    await page.waitForTimeout(500)

    const dialog = page.locator('.el-dialog').first()
    await expect(dialog).toBeVisible({ timeout: 5_000 })

    // 不填任何数据，直接点确定
    await dialog.locator('button').filter({ hasText: '确 定' }).first().click()
    await page.waitForTimeout(500)

    // 应显示必填校验错误（Element Plus 的 form error 提示）
    const errorMsgs = dialog.locator('.el-form-item__error')
    const errorCount = await errorMsgs.count()
    console.log(`  ✅ 表单校验触发 ${errorCount} 个必填错误提示`)
    expect(errorCount).toBeGreaterThan(0)

    // 对话框应仍然打开（不允许提交）
    await expect(dialog).toBeVisible()
    console.log('  ✅ 表单校验不通过时对话框保持打开')

    // 关闭对话框
    await page.locator('.el-dialog__footer button').filter({ hasText: '关 闭' }).first().click()
  })
})

test.describe('流转卡 — 修改记录', () => {
  test.use({ storageState: 'setup/storageState.json' })
  test.setTimeout(120_000)

  async function navigateToCardPage(page: any) {
    const initialListPromise = page.waitForResponse(
      r => r.url().includes('/mes/pro/procard/list') && r.status() === 200,
      { timeout: 15_000 }
    )
    await page.goto('/mes/pro/card')
    await initialListPromise
    await expect(page.locator('.el-table').first()).toBeVisible({ timeout: 5_000 })
    await expect(page.locator('.el-form').first()).toBeVisible({ timeout: 5_000 })
  }

  test('E1: 查看流转卡 → 查看模式下所有字段禁用', async ({ page }) => {
    await navigateToCardPage(page)

    // 点击表格第一行的流转卡编码链接（查看）
    const codeLink = page.locator('.el-table tbody tr').first().locator('td').nth(1).locator('button, a').first()
    await codeLink.click()
    await page.waitForTimeout(500)

    const dialog = page.locator('.el-dialog').first()
    await expect(dialog).toBeVisible({ timeout: 5_000 })

    // 验证标题
    await expect(dialog.locator('.el-dialog__title')).toHaveText('查看流转卡')

    // 验证流转卡编码字段为 disabled
    const cardCodeInput = dialog.locator('.el-form-item').filter({ hasText: '流转卡编码' }).locator('input').first()
    await expect(cardCodeInput).toBeDisabled()
    console.log('  ✅ 查看模式下流转卡编码字段禁用')

    // 验证工单字段为 disabled
    const woInput = dialog.locator('.el-form-item').filter({ hasText: '生产工单' }).locator('input').first()
    await expect(woInput).toBeDisabled()
    console.log('  ✅ 查看模式下生产工单字段禁用')

    // 验证无"确 定"按钮
    const submitBtn = dialog.locator('button').filter({ hasText: '确 定' })
    await expect(submitBtn).toHaveCount(0)
    console.log('  ✅ 查看模式下无"确定"按钮')

    // 应显示"工序流转详情"Tab
    const processTab = dialog.locator('.el-tabs__item').filter({ hasText: '工序流转详情' })
    await expect(processTab).toBeVisible({ timeout: 3_000 })
    console.log('  ✅ 查看模式下显示"工序流转详情" Tab')

    // 关闭对话框
    await page.locator('.el-dialog__footer button').filter({ hasText: '关 闭' }).first().click()
  })

  test('E2: 修改流转卡 → 修改备注并提交', async ({ page }) => {
    await navigateToCardPage(page)

    // 点击第一行的"修改"按钮
    const editBtn = page.locator('.el-table tbody tr').first().locator('button').filter({ hasText: '修改' }).first()
    await editBtn.click()
    await page.waitForTimeout(500)

    const dialog = page.locator('.el-dialog').first()
    await expect(dialog).toBeVisible({ timeout: 5_000 })
    await expect(dialog.locator('.el-dialog__title')).toHaveText('修改流转卡')

    // 验证表单已填充数据
    const cardCodeInput = dialog.locator('.el-form-item').filter({ hasText: '流转卡编码' }).locator('input').first()
    const existingCode = await cardCodeInput.inputValue()
    expect(existingCode).toBeTruthy()
    console.log(`  ✅ 编辑模式已填充流转卡编码: "${existingCode}"`)

    // 修改备注字段
    const testRemark = `E2E_TEST_${Date.now()}`
    const remarkInput = dialog.locator('.el-form-item').filter({ hasText: '备注' }).locator('textarea').first()
    await remarkInput.fill(testRemark)
    console.log(`  ✅ 已填写备注: "${testRemark}"`)

    // 提交修改
    const [updateResp] = await Promise.all([
      page.waitForResponse(
        r => r.url().includes('/mes/pro/procard') && r.request().method() === 'PUT' && r.status() === 200,
        { timeout: 10_000 }
      ),
      dialog.locator('button').filter({ hasText: '确 定' }).first().click()
    ])

    const updateBody = await updateResp.json()
    expect(updateBody.code).toBe(200)
    console.log(`  ✅ 修改 API 返回 code=200`)

    // 验证成功消息
    await expect(page.locator('.el-message--success').first()).toBeVisible({ timeout: 5_000 })
    console.log('  ✅ 显示"修改成功"消息')

    // 对话框关闭
    await expect(dialog).not.toBeVisible({ timeout: 5_000 })
  })

  test('E3: 修改流转卡 → 修改状态并提交', async ({ page }) => {
    await navigateToCardPage(page)

    // 点击第一行的"修改"按钮
    const editBtn = page.locator('.el-table tbody tr').first().locator('button').filter({ hasText: '修改' }).first()
    await editBtn.click()
    await page.waitForTimeout(500)

    const dialog = page.locator('.el-dialog').first()
    await expect(dialog).toBeVisible({ timeout: 5_000 })

    // 记录当前状态
    const currentStatus = await dialog.locator('.el-form-item').filter({ hasText: '状态' }).locator('.el-select input').first().inputValue()
    console.log(`  📝 当前状态: "${currentStatus}"`)

    // 修改状态：切换到"已完工"
    const statusSelect = dialog.locator('.el-form-item').filter({ hasText: '状态' }).locator('.el-select').first()
    await statusSelect.click()
    await page.waitForTimeout(300)
    const completedOption = page.locator('.el-select-dropdown__item').filter({ hasText: '已完工' }).last()
    await completedOption.click()
    await page.waitForTimeout(200)

    // 提交修改
    const [updateResp] = await Promise.all([
      page.waitForResponse(
        r => r.url().includes('/mes/pro/procard') && r.request().method() === 'PUT' && r.status() === 200,
        { timeout: 10_000 }
      ),
      dialog.locator('button').filter({ hasText: '确 定' }).first().click()
    ])

    const updateBody = await updateResp.json()
    expect(updateBody.code).toBe(200)
    console.log(`  ✅ 状态修改 API 返回 code=200`)
    await expect(page.locator('.el-message--success').first()).toBeVisible({ timeout: 5_000 })
    console.log('  ✅ 状态修改成功')
  })

  test('E4: 修改对话框 → 取消按钮关闭对话框且不提交', async ({ page }) => {
    await navigateToCardPage(page)

    // 点击第一行的"修改"按钮
    const editBtn = page.locator('.el-table tbody tr').first().locator('button').filter({ hasText: '修改' }).first()
    await editBtn.click()
    await page.waitForTimeout(500)

    const dialog = page.locator('.el-dialog').first()
    await expect(dialog).toBeVisible({ timeout: 5_000 })

    // 修改备注
    const remarkInput = dialog.locator('.el-form-item').filter({ hasText: '备注' }).locator('textarea').first()
    await remarkInput.fill('SHOULD_NOT_BE_SAVED')

    // 点击取消
    const closeBtn = dialog.locator('button').filter({ hasText: '关 闭' }).first()
    await closeBtn.click()

    // 对话框应关闭
    await expect(dialog).not.toBeVisible({ timeout: 5_000 })
    console.log('  ✅ 点击"关闭"后对话框关闭')

    // 重新打开编辑验证备注未被保存
    await page.waitForTimeout(300)

    // 重新打开第一条记录的编辑
    const editBtn2 = page.locator('.el-table tbody tr').first().locator('button').filter({ hasText: '修改' }).first()
    await editBtn2.click()
    await page.waitForTimeout(500)

    const dialog2 = page.locator('.el-dialog').first()
    await expect(dialog2).toBeVisible({ timeout: 5_000 })

    const remarkInput2 = dialog2.locator('.el-form-item').filter({ hasText: '备注' }).locator('textarea').first()
    const currentRemark = await remarkInput2.inputValue()
    expect(currentRemark).not.toBe('SHOULD_NOT_BE_SAVED')
    console.log(`  ✅ 取消后备注未被保存，当前值: "${currentRemark}"`)

    // 关闭
    await page.locator('.el-dialog__footer button').filter({ hasText: '关 闭' }).first().click()
  })
})
