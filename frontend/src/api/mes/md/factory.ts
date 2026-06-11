import request from '@/utils/request'
import type { AjaxResult } from '@/types'

// 查询所有启用工厂（Vendor页面外协工厂下拉用）
export function listAllFactory(): Promise<AjaxResult<any[]>> {
  return request({ url: '/mes/md/factory/listAll', method: 'get' })
}
