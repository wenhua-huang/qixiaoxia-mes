import request from '@/utils/request'

// 加载齐套看板
export function loadKitDashboard(workorderId: number) {
  return request({ url: '/mes/pro/workorder/kitDashboard/' + workorderId, method: 'get' })
}

// 一键批量生成单据
export function generateDocs(data: any) {
  return request({ url: '/mes/pro/workorder/generateDocs', method: 'post', data })
}

// 单独生成入库单
export function generateReceipt(workorderId: number) {
  return request({ url: '/mes/pro/workorder/generateReceipt/' + workorderId, method: 'post' })
}

// 单独生成退料单
export function generateReturn(workorderId: number) {
  return request({ url: '/mes/pro/workorder/generateReturn/' + workorderId, method: 'post' })
}

// 单独生成采购单
export function generatePurOrder(workorderId: number) {
  return request({ url: '/mes/pro/workorder/generatePurOrder/' + workorderId, method: 'post' })
}

// 采购向导 — 获取推荐数据
export function loadPurOrderWizard(workorderId: number) {
  return request({ url: '/mes/pro/workorder/purOrderWizard/' + workorderId, method: 'get' })
}

// 采购向导 — 提交用户确认的物料行
export function submitPurOrderWizard(workorderId: number, lines: any[]) {
  return request({ url: '/mes/pro/workorder/purOrderWizard/' + workorderId + '/submit', method: 'post', data: lines })
}
