import request from '@/utils/request'

// 产品入库单列表（分页，支持 workorderCode/recptCode/status 筛选）
export function listProductRecpt(query) {
  return request({
    url: '/mes/wm/product_recpt/list',
    method: 'get',
    params: query
  })
}

// 产品入库单详情（头 + 行）
export function getProductRecptDetail(recptId) {
  return request({
    url: '/mes/wm/product_recpt/' + recptId,
    method: 'get'
  })
}

// 移动端确认入库（更新行数量+确认+库存，单接口原子完成）
export function mobileConfirmProductRecpt(recptId, data) {
  return request({
    url: '/mes/wm/product_recpt/mobile/confirm/' + recptId,
    method: 'put',
    data: data
  })
}

// 仓库列表（下拉选择用）
export function listWarehouseAll() {
  return request({
    url: '/mes/wm/warehouse/listAll',
    method: 'get'
  })
}
