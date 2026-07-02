import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import KitDashboard from '../KitDashboard.vue'

// ==================== Mock kit API ====================
const mockLoadKitDashboard = vi.fn()
const mockGenerateDocs = vi.fn()

vi.mock('@/api/mes/pro/kit', () => ({
  loadKitDashboard: (...args: any[]) => mockLoadKitDashboard(...args),
  generateDocs: (...args: any[]) => mockGenerateDocs(...args),
  generateReceipt: vi.fn().mockResolvedValue({ code: 200, data: {} }),
  generateReturn: vi.fn().mockResolvedValue({ code: 200, data: {} }),
  generatePurOrder: vi.fn().mockResolvedValue({ code: 200, data: {} }),
}))

function buildDashboard(overrides: Record<string, any> = {}) {
  return {
    workorderId: 1, workorderCode: 'WO-001', workorderName: '测试工单',
    workorderStatus: 'PREPARE', planQuantity: '100', productName: '测试产品',
    productCode: 'PROD-001', requestDate: '2026-07-01', unitName: '个',
    materialRows: [
      { itemId: 10, itemCode: 'MAT-001', itemName: '白牛皮', unitName: 'KG', requiredQty: 50, availableQty: 800, sufficient: true, shortageQty: 0 },
    ],
    sufficientCount: 1, shortageCount: 0, allSufficient: true,
    purchaseRecommends: [], hasPurchaseRecommend: false,
    issueStatuses: [], hasIssueDocs: false, issueDraftCount: 0, issuePostedCount: 0,
    returnStatuses: [], hasPendingReturns: false, returnReady: false,
    receiptRecommend: { recommended: false, producedQty: 0, qualifiedQty: 0, alreadyHasReceipt: false },
    receiptReady: false,
    ...overrides,
  }
}

// el-drawer teleports content to body — find content there
function bodyHtml() { return document.body.innerHTML }

describe('KitDashboard.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    document.body.innerHTML = ''
  })

  // ==================== 1. API 调用验证 ====================

  it('应在打开时调用 loadKitDashboard API 并传入正确的 workorderId', async () => {
    mockLoadKitDashboard.mockResolvedValue({ code: 200, data: buildDashboard() })
    // mount with modelValue:false → then update to true to trigger watch
    const wrapper = mount(KitDashboard, {
      props: { modelValue: false, workorderId: 1 },
      attachTo: document.body,
    })
    await wrapper.setProps({ modelValue: true })
    await nextTick(); await nextTick()
    expect(mockLoadKitDashboard).toHaveBeenCalledWith(1)
  })

  it('不应在 workorderId 为 null 且关闭时调用 API', () => {
    mockLoadKitDashboard.mockResolvedValue({ code: 200, data: buildDashboard() })
    mount(KitDashboard, {
      props: { modelValue: false, workorderId: null },
      attachTo: document.body,
    })
    expect(mockLoadKitDashboard).not.toHaveBeenCalled()
  })

  // ==================== 2. 渲染验证（el-drawer teleported → body） ====================

  async function openDashboard(data: any) {
    mockLoadKitDashboard.mockResolvedValue({ code: 200, data })
    const wrapper = mount(KitDashboard, {
      props: { modelValue: false, workorderId: 1 },
      attachTo: document.body,
    })
    await wrapper.setProps({ modelValue: true })
    await nextTick(); await nextTick()
    return wrapper
  }

  it('应渲染工单编码和产品名称', async () => {
    await openDashboard(buildDashboard())
    expect(bodyHtml()).toContain('WO-001')
    expect(bodyHtml()).toContain('测试产品')
  })

  it('全部齐套时应显示无需采购', async () => {
    await openDashboard(buildDashboard())
    expect(bodyHtml()).toContain('无需采购')
  })

  it('缺料时应显示缺料物料名和采购建议数', async () => {
    await openDashboard(buildDashboard({
      materialRows: [
        { itemId: 20, itemCode: 'MAT-002', itemName: '胶水', unitName: 'KG', requiredQty: 200, availableQty: 50, sufficient: false, shortageQty: 150 },
      ],
      sufficientCount: 0, shortageCount: 1, allSufficient: false,
      purchaseRecommends: [{ itemId: 20, itemCode: 'MAT-002', itemName: '胶水', unitName: 'KG', shortageQty: 150, recommendedQty: 150, hasPendingPO: false, pendingPOInfo: '' }],
      hasPurchaseRecommend: true,
    }))
    expect(bodyHtml()).toContain('胶水')
    expect(bodyHtml()).toContain('1项需采购')
  })

  it('COMPLETED 工单应显示合格数量', async () => {
    await openDashboard(buildDashboard({
      workorderStatus: 'COMPLETED',
      receiptRecommend: { recommended: true, producedQty: 100, qualifiedQty: 95, alreadyHasReceipt: false },
      receiptReady: true,
    }))
    expect(bodyHtml()).toContain('95')
  })

  it('已有入库单时应显示复用编码', async () => {
    await openDashboard(buildDashboard({
      workorderStatus: 'COMPLETED',
      receiptRecommend: { recommended: true, producedQty: 100, qualifiedQty: 95, alreadyHasReceipt: true, existingReceiptCode: 'RK-EXIST' },
      receiptReady: false,
    }))
    expect(bodyHtml()).toContain('RK-EXIST')
  })

  it('有领料单时应显示领料单编码', async () => {
    await openDashboard(buildDashboard({
      issueStatuses: [{ issueId: 100, issueCode: 'LL-001', issueName: '测试-印刷', processName: '印刷工序', status: 'DRAFT', totalQuantity: 5000 }],
      hasIssueDocs: true, issueDraftCount: 1, issuePostedCount: 0,
    }))
    expect(bodyHtml()).toContain('LL-001')
  })

  // ==================== 3. 按钮行为 ====================

  it('点击仅生成领料单应传递正确的参数给 generateDocs', async () => {
    mockGenerateDocs.mockResolvedValue({ code: 200, data: { message: 'OK' } })
    const wrapper = await openDashboard(buildDashboard({
      allSufficient: false, shortageCount: 1, hasPurchaseRecommend: true,
    }))
    await nextTick()

    // 在 body 里找按钮
    const btns = Array.from(document.body.querySelectorAll('button'))
    const genIssueBtn = btns.find(b => b.textContent?.includes('仅生成领料单'))
    expect(genIssueBtn).toBeTruthy()
    genIssueBtn!.click()
    await nextTick(); await nextTick()

    expect(mockGenerateDocs).toHaveBeenCalledWith(
      expect.objectContaining({ workorderId: 1, generateIssue: true, generatePurOrder: false, generateReturn: false, generateReceipt: false })
    )
    wrapper.unmount()
  })

  // ==================== 4. 错误处理 ====================

  it('API 返回 500 时不应崩溃', async () => {
    mockLoadKitDashboard.mockRejectedValue(new Error('Server Error'))
    const wrapper = mount(KitDashboard, {
      props: { modelValue: false, workorderId: 1 },
      attachTo: document.body,
    })
    await wrapper.setProps({ modelValue: true })
    await nextTick(); await nextTick()
    // 组件不应崩溃
    expect(bodyHtml()).toBeTruthy()
    wrapper.unmount()
  })

  // ==================== 5. 关闭抽屉 ====================

  it('关闭抽屉时应 emit update:modelValue', async () => {
    mockLoadKitDashboard.mockResolvedValue({ code: 200, data: buildDashboard() })
    const wrapper = mount(KitDashboard, {
      props: { modelValue: false, workorderId: 1 },
      attachTo: document.body,
    })
    await wrapper.setProps({ modelValue: true })
    await nextTick()
    await wrapper.setProps({ modelValue: false })
    await nextTick()

    expect(wrapper.emitted('update:modelValue')).toBeTruthy()
    wrapper.unmount()
  })
})
