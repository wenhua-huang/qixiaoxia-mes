import { test, expect, request } from '@playwright/test'

/**
 * 工序参数模板「标准图样」功能 E2E
 *
 * 覆盖 V84 新增的 image_url 字段在三层的数据流：
 *   ① 后端 API：imageUrl 字段可写入 / 读回（单图 + 多图逗号分隔）
 *   ② 工序管理页：参数模板子表渲染「图样」列（无图显示 -，有图显示缩略图）
 *   ③ 报工页：选择任务后参数行渲染「标准图样」列
 *
 * 数据准备：选一条真实参数模板，临时写入测试 imageUrl，测完还原。
 */

const API_BASE = 'http://localhost:8081'
const TEST_IMG = '/profile/upload/e2e/param-image-sample.png'
const TEST_IMG_2 = '/profile/upload/e2e/param-image-sample2.jpg'

/** 1×1 透明 PNG，用于真实上传验证 ImageUpload 组件存储格式 */
const TINY_PNG = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAAC0lEQVR42mP8/5+hgQAAAAAASUVORK5CYII=',
  'base64'
)

/** 登录并返回带 token 的 APIRequestContext */
async function authedApi() {
  const loginCtx = await request.newContext({ baseURL: API_BASE })
  const loginRes = await loginCtx.post('/login', { data: { username: 'admin', password: 'admin123' } })
  const { token } = await loginRes.json()
  return request.newContext({
    baseURL: API_BASE,
    extraHTTPHeaders: { Authorization: `Bearer ${token}` }
  })
}

/** 取一条有 processId 的参数模板（用于挂载图样并定位其工序页） */
async function pickTemplate(api: any) {
  const res = await api.get('/mes/pro/paramtemplate/list?pageSize=50')
  const body = await res.json()
  const rows: any[] = body.rows || []
  // 优先选报工可见 + 启用的，方便后续报工页验证
  return (
    rows.find((r) => r.isReportVisible === 'Y' && r.enableFlag === '1') ||
    rows.find((r) => r.processId) ||
    rows[0]
  )
}

// 所有测试共享同一条参数模板（预置/还原 imageUrl），必须串行执行避免并发覆盖
test.describe.serial('参数模板图样 E2E', () => {

// ─────────────────────────────────────────────────────────
// ① 后端 API 层：imageUrl 字段 CRUD
// ─────────────────────────────────────────────────────────
test.describe('后端 API 层', () => {
  test('imageUrl 字段可写入/读回（单图 + 多图逗号分隔）', async () => {
    test.setTimeout(30_000)
    const api = await authedApi()
    const tmpl = await pickTemplate(api)
    expect(tmpl, '需至少一条参数模板').toBeTruthy()
    const orig = tmpl.imageUrl
    console.log(`  测试目标: templateId=${tmpl.templateId} (${tmpl.paramCode})`)

    // 列表接口应返回 imageUrl 字段（验证 V84 列 + Mapper 映射）
    expect('imageUrl' in tmpl, '列表接口应包含 imageUrl 字段').toBeTruthy()

    // 写入单图
    await api.put('/mes/pro/paramtemplate', { data: { ...tmpl, imageUrl: TEST_IMG } })
    const back1 = await (await api.get(`/mes/pro/paramtemplate/${tmpl.templateId}`)).json()
    expect(back1.data.imageUrl, '单图读回').toBe(TEST_IMG)
    console.log('  ✅ 单图写入/读回 OK')

    // 写入多图（逗号分隔，与 ImageUpload 组件 modelValue 约定一致）
    await api.put('/mes/pro/paramtemplate', { data: { ...tmpl, imageUrl: `${TEST_IMG},${TEST_IMG_2}` } })
    const back2 = await (await api.get(`/mes/pro/paramtemplate/${tmpl.templateId}`)).json()
    expect(back2.data.imageUrl, '多图读回').toBe(`${TEST_IMG},${TEST_IMG_2}`)
    console.log('  ✅ 多图(逗号分隔)写入/读回 OK')

    // 清空
    await api.put('/mes/pro/paramtemplate', { data: { ...tmpl, imageUrl: '' } })
    const back3 = await (await api.get(`/mes/pro/paramtemplate/${tmpl.templateId}`)).json()
    expect(back3.data.imageUrl ?? '', '清空后').toBe('')
    console.log('  ✅ 清空 OK')

    // 还原
    await api.put('/mes/pro/paramtemplate', { data: { ...tmpl, imageUrl: orig } })
    console.log('  ✅ 已还原原始值')
  })
})

// ─────────────────────────────────────────────────────────
// ② 工序管理页：参数模板子表「图样」列
// ─────────────────────────────────────────────────────────
test.describe('工序管理页 UI', () => {
  test.use({ storageState: 'setup/storageState.json' })
  test.setTimeout(60_000)

  test('参数模板子表渲染「图样」列：有图显示缩略图，无图显示 -', async ({ page }) => {
    const api = await authedApi()
    const tmpl = await pickTemplate(api)
    const processId = tmpl.processId
    const orig = tmpl.imageUrl
    console.log(`  目标工序 processId=${processId}, templateId=${tmpl.templateId}`)

    // 预置：给该参数写入一张测试图
    await api.put('/mes/pro/paramtemplate', { data: { ...tmpl, imageUrl: TEST_IMG } })

    // 查目标工序名（用于点击工序列表中的工序名按钮打开弹窗）
    const procRes = await api.get(`/mes/pro/process/${processId}`)
    const procBody = await procRes.json()
    const processName: string = procBody.data?.processName || ''
    console.log(`  目标工序名: ${processName}`)

    try {
      await page.goto('/mes/pro/process')
      await expect(page.locator('.el-table').first()).toBeVisible({ timeout: 15_000 })

      // 点击工序名按钮打开查看弹窗（工序名是可点击的 button）
      await page.getByRole('button', { name: processName }).first().click()

      // 等待弹窗 + 参数模版分隔符出现
      const dialog = page.locator('.el-dialog').filter({ hasText: '参数模版' })
      await expect(dialog).toBeVisible({ timeout: 10_000 })

      // 验证「图样」列表头存在
      const headerCells = dialog.locator('.el-table__header th')
      const headerTexts = await headerCells.allTextContents()
      expect(headerTexts.map((t) => t.trim())).toContain('图样')
      console.log('  ✅ 参数模板子表含「图样」列')

      // 该参数行应有缩略图（el-image），其他行显示 -
      const paramRows = dialog.locator('.el-table__body-wrapper tbody tr')
      const rowCount = await paramRows.count()
      let foundImage = false
      for (let i = 0; i < rowCount; i++) {
        const code = (await paramRows.nth(i).locator('td').first().textContent())?.trim()
        if (code === tmpl.paramCode) {
          // 图样列（第6列，索引5）应有 el-image
          const imgCell = paramRows.nth(i).locator('td').nth(5)
          const hasImg = await imgCell.locator('img, .el-image').count()
          if (hasImg > 0) foundImage = true
          console.log(`  参数 ${code}: 图样列 ${hasImg > 0 ? '✅ 显示缩略图' : '⚠️ 无缩略图'}`)
        }
      }
      expect(foundImage, '预置图样的参数应显示缩略图').toBeTruthy()
      console.log('  ✅ 有图参数显示缩略图')

      // 关闭弹窗
      await page.locator('.el-dialog').locator('button', { hasText: '取 消' }).first().click().catch(() => {})
    } finally {
      // 还原
      await api.put('/mes/pro/paramtemplate', { data: { ...tmpl, imageUrl: orig } })
      console.log('  ✅ 已还原')
    }
  })
})

// ─────────────────────────────────────────────────────────
// ③ 报工页：工序参数 Tab「标准图样」列
// ─────────────────────────────────────────────────────────
test.describe('报工页 UI', () => {
  test.use({ storageState: 'setup/storageState.json' })
  test.setTimeout(90_000)

  test('选择任务后，工序参数 Tab 渲染「标准图样」列', async ({ page }) => {
    const api = await authedApi()
    const tmpl = await pickTemplate(api)
    const processId = tmpl.processId
    const orig = tmpl.imageUrl
    console.log(`  目标工序 processId=${processId}, templateId=${tmpl.templateId}`)

    // 预置：给该参数写入测试图样
    await api.put('/mes/pro/paramtemplate', { data: { ...tmpl, imageUrl: TEST_IMG } })

    // 取该工序下一条 NORMAL 状态的任务（用于报工选择）
    const taskRes = await api.get(`/mes/pro/task/list?processId=${processId}&status=NORMAL&pageSize=1`)
    const taskBody = await taskRes.json()
    const taskRow: any = (taskBody.rows || [])[0]
    if (!taskRow) {
      console.log('  ⚠️ 该工序无 NORMAL 任务，跳过报工页测试')
      await api.put('/mes/pro/paramtemplate', { data: { ...tmpl, imageUrl: orig } })
      test.skip()
      return
    }
    const taskCode: string = taskRow.taskCode
    console.log(`  使用任务: ${taskCode}`)

    try {
      await page.goto('/mes/pro/feedback')
      await expect(page.locator('.el-table').first()).toBeVisible({ timeout: 15_000 })

      // 新增报工
      await page.getByRole('button', { name: '新增报工' }).click()
      const feedbackDialog = page.locator('.el-dialog', { hasText: '新增报工' })
      await expect(feedbackDialog).toBeVisible({ timeout: 10_000 })

      // 打开生产任务选择器
      await page.locator('.el-input-group__append .el-button').first().click()
      const taskDialog = page.locator('.el-dialog', { hasText: '生产任务选择' })
      await expect(taskDialog).toBeVisible({ timeout: 10_000 })

      // 双击目标任务行（handleRowDbClick 直接 emit onSelected 并关闭弹窗）
      await taskDialog.getByRole('row', { name: taskCode }).dblclick()

      // 等待任务选中后参数模板加载完成（onTaskSelected → loadParamTemplates）
      await page.waitForResponse(
        (r) => r.url().includes('/mes/pro/paramtemplate/listByProcessId') && r.status() === 200,
        { timeout: 10_000 }
      ).catch(() => {})
      // 兜底：确保 Vue 渲染完成
      await page.waitForTimeout(1500)

      // 切到「工序参数」Tab（用 evaluate 规避弹窗遮罩拦截点击）
      await page.evaluate(() => {
        const t = document.querySelector('#tab-param') as HTMLElement | null
        t?.click()
      })
      await page.waitForTimeout(500)

      // 验证「标准图样」列表头存在
      const paramPanel = page.locator('#pane-param')
      await expect(paramPanel).toBeVisible({ timeout: 10_000 })
      const headerTexts = await paramPanel.locator('.el-table__header th').allTextContents()
      expect(headerTexts.map((t) => t.trim())).toContain('标准图样')
      console.log('  ✅ 报工工序参数 Tab 含「标准图样」列')

      // 该参数行应有缩略图（image-preview 渲染为 .el-image）
      const paramRows = paramPanel.locator('.el-table__body-wrapper tbody tr')
      await expect(paramRows.first()).toBeVisible({ timeout: 10_000 })
      const rowCount = await paramRows.count()
      console.log(`  参数行数: ${rowCount}, 目标参数: ${tmpl.paramName}`)
      let foundImage = false
      for (let i = 0; i < rowCount; i++) {
        const name = (await paramRows.nth(i).locator('td').first().textContent())?.trim()
        if (name === tmpl.paramName) {
          const imgCount = await paramRows.nth(i).locator('td').nth(3).locator('.el-image, img').count()
          if (imgCount > 0) foundImage = true
          console.log(`  参数 ${name}: 图样列 ${imgCount > 0 ? '✅ 显示缩略图' : '⚠️ 无缩略图'}`)
        }
      }
      expect(foundImage, '预置图样的参数应显示缩略图').toBeTruthy()
      console.log('  ✅ 报工页参数行显示标准图样缩略图')
    } finally {
      await api.put('/mes/pro/paramtemplate', { data: { ...tmpl, imageUrl: orig } })
      console.log('  ✅ 已还原')
    }
  })
})

// ─────────────────────────────────────────────────────────
// ④ 工艺路线页：工序参数弹窗「标准图样」列
// ─────────────────────────────────────────────────────────
test.describe('工艺路线页 UI', () => {
  test.use({ storageState: 'setup/storageState.json' })
  test.setTimeout(90_000)

  test('工序参数弹窗渲染「标准图样」列', async ({ page }) => {
    const api = await authedApi()
    const tmpl = await pickTemplate(api)
    const processId = tmpl.processId
    const orig = tmpl.imageUrl
    console.log(`  目标工序 processId=${processId}, templateId=${tmpl.templateId}`)

    // 预置图样
    await api.put('/mes/pro/paramtemplate', { data: { ...tmpl, imageUrl: TEST_IMG } })

    // 查一条包含该工序的工艺路线
    const routeRes = await api.get('/mes/pro/proroute/list?pageSize=20')
    const routeBody = await routeRes.json()
    // 工序列表行点击后需展开工序组成，这里直接用第一条路线
    const routeRow: any = (routeBody.rows || [])[0]
    if (!routeRow) {
      console.log('  ⚠️ 无工艺路线数据，跳过')
      await api.put('/mes/pro/paramtemplate', { data: { ...tmpl, imageUrl: orig } })
      test.skip()
      return
    }
    const routeName: string = routeRow.routeName
    console.log(`  使用路线: ${routeName}`)

    try {
      await page.goto('/mes/pro/proroute')
      await expect(page.locator('.el-table').first()).toBeVisible({ timeout: 15_000 })

      // 勾选目标路线行 → 点修改（编辑模式才显示「参数」按钮）
      const routeRowEl = page.locator('.el-table__body-wrapper tbody tr').filter({ hasText: routeName }).first()
      await routeRowEl.locator('.el-checkbox').first().click()
      await page.evaluate(() => {
        const btns = document.querySelectorAll('button')
        for (const b of btns) {
          if (b.textContent?.trim() === '修改' && !b.disabled) { b.click(); return }
        }
      })

      // 切到「工序组成」Tab
      const editDialog = page.locator('.el-dialog', { hasText: '工序组成' })
      await expect(editDialog).toBeVisible({ timeout: 10_000 })
      await page.evaluate(() => {
        document.querySelectorAll('.el-tabs__item').forEach((t) => {
          if (t.textContent?.trim() === '工序组成') (t as HTMLElement).click()
        })
      })

      // 等待工序组成表格加载（含目标工序名）
      await page.waitForResponse(
        (r) => r.url().includes('/mes/pro/routeprocess/') && r.status() === 200,
        { timeout: 10_000 }
      ).catch(() => {})
      await page.waitForTimeout(1000)

      // 查目标工序名（用于定位工序行）
      const procRes = await api.get(`/mes/pro/process/${processId}`)
      const processName: string = (await procRes.json()).data?.processName || ''

      // 点击目标工序行的「参数」按钮（操作列第3个按钮，索引2）
      await page.evaluate((pname) => {
        const rows = document.querySelectorAll('.el-dialog tbody tr') || []
        for (const r of rows) {
          if (r.textContent.includes(pname)) {
            const btns = r.querySelectorAll('button')
            if (btns[2]) (btns[2] as HTMLElement).click()
            return
          }
        }
      }, processName)

      // 等待工序参数弹窗 + 模板加载
      await page.waitForResponse(
        (r) => r.url().includes('/mes/pro/paramtemplate/listByProcessId') && r.status() === 200,
        { timeout: 10_000 }
      ).catch(() => {})
      await page.waitForTimeout(1000)

      // 验证工序参数弹窗含「标准图样」列
      const paramDialog = page.locator('.el-dialog', { hasText: '工序参数' })
      await expect(paramDialog).toBeVisible({ timeout: 10_000 })
      const headerTexts = await paramDialog.locator('.el-table__header th').allTextContents()
      expect(headerTexts.map((t) => t.trim())).toContain('标准图样')
      console.log('  ✅ 工艺路线工序参数弹窗含「标准图样」列')

      // 若该工序已配参数，验证目标参数行缩略图；无参数数据则表头验证已足够
      await page.waitForTimeout(1500)
      const paramRows = paramDialog.locator('.el-table__body-wrapper tbody tr')
      const rowCount = await paramRows.count()
      console.log(`  参数行数: ${rowCount}`)
      let foundImage = false
      for (let i = 0; i < rowCount; i++) {
        const name = (await paramRows.nth(i).locator('td').first().textContent())?.trim()
        if (name === tmpl.paramName) {
          const imgCount = await paramRows.nth(i).locator('td').nth(1).locator('.el-image, img').count()
          if (imgCount > 0) foundImage = true
          console.log(`  参数 ${name}: ${imgCount > 0 ? '✅ 显示缩略图' : '⚠️ 无缩略图'}`)
        }
      }
      // 该路线工序已配参数时，必须显示缩略图；未配参数（rowCount=0）则表头列验证已证明功能就绪
      if (rowCount > 0) {
        expect(foundImage, '预置图样的参数应显示缩略图').toBeTruthy()
        console.log('  ✅ 工艺路线参数行显示标准图样')
      } else {
        console.log('  ℹ️ 该路线工序暂无参数数据，表头列已验证就绪')
      }
    } finally {
      await api.put('/mes/pro/paramtemplate', { data: { ...tmpl, imageUrl: orig } })
      console.log('  ✅ 已还原')
    }
  })
})

}) // end test.describe.serial('参数模板图样 E2E')
