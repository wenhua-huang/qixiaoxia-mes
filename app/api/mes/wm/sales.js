import request from '@/utils/request'

// 销售出库单列表（支持按 salesCode/status 过滤）
export function getSalesList(query) {
  return request({ url: '/mes/wm/product_sales/list', method: 'get', params: query })
}

// 出库单详情（头+行+明细+发运+装箱）
export function getSalesDetail(salesId) {
  return request({ url: '/mes/wm/product_sales/detail/' + salesId, method: 'get' })
}

// 按出库单编码查整单（扫码查单用）
export function getSalesDetailByCode(salesCode) {
  return request({ url: '/mes/wm/product_sales/byCode', method: 'get', params: { salesCode } })
}

// 出库确认（扣库存）— details 为出库明细数组（lineId/itemId/quantity/batchId...）
export function postSalesOut(salesId, details) {
  return request({ url: '/mes/wm/product_sales/post/' + salesId, method: 'put', data: details })
}

// 查可用批次列表（出库选批次用）：按 itemId 查所有 available>0 的批次记录
// 不传 warehouseId 或传 null 时跨仓返回；返回项含 warehouseName 供前端分辨仓库
export function availableBatches(itemId, warehouseId) {
  const params = { itemId }
  if (warehouseId != null) params.warehouseId = warehouseId
  return request({ url: '/mes/wm/material_stock/availableBatches', method: 'get', params })
}
