import request from '@/utils/request'

// 查询全部仓库（收货/出库选仓库用）
export function listAllWarehouse() {
  return request({ url: '/mes/wm/warehouse/listAll', method: 'get' })
}
