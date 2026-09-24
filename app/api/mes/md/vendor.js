import request from '@/utils/request'

// 全部厂商（建外协单选厂商用，按 vendorType 过滤外协/兼营厂商）
export function listAllVendor() {
  return request({ url: '/mes/md/vendor/listAll', method: 'get' })
}
