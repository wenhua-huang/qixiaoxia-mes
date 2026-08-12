import request from '@/utils/request'

// 工单外协发料信息（App"外协按工单发料"入口）：
// 返回外协工序(is_outsource=1)、BOM 发料行、默认厂商、已有外协单（防重）
export function getWorkorderOutsourceInfo(workorderCode) {
  return request({ url: '/mes/pro/workorder/outsourceInfo', method: 'get', params: { workorderCode } })
}
