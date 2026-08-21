// 外协订单通用映射（状态、来源类型）
// 多个外协页面共用，避免重复定义。

const STATUS_TEXT = {
  DRAFT: '草稿',
  ISSUED: '已发料',
  VENDOR_RCVD: '厂商已签收',
  PROCESSING: '加工中',
  FINISHED: '已完工',
  SHIPPED: '已发货',
  RECEIVED: '已收货',
  CLOSED: '已关闭'
}

const STATUS_TAG_TYPE = {
  DRAFT: 'default',
  ISSUED: 'warning',
  VENDOR_RCVD: 'warning',
  PROCESSING: 'primary',
  FINISHED: 'primary',
  SHIPPED: 'warning',
  RECEIVED: 'success',
  CLOSED: 'default'
}

const SOURCE_TEXT = {
  GENERIC: '通用外协',
  SLITTING: '分切',
  PRINTING: '印刷'
}

export function outsourceStatusText(s) {
  return STATUS_TEXT[s] || s || ''
}

export function outsourceStatusTagType(s) {
  return STATUS_TAG_TYPE[s] || 'default'
}

export function outsourceSourceText(s) {
  return SOURCE_TEXT[s] || s || '-'
}
