import request from '@/utils/request'

// 扫流转卡码反查报工上下文
export function getCardScanResult(cardCode) {
  return request({ url: '/mes/pro/procard/scan/' + encodeURIComponent(cardCode), method: 'get' })
}
