// 质检通用常量与客户端预判（公式镜像后端 QcJudgeServiceImpl / PC QcJudgeDialog）

export const QC_STATUS_MAP = {
  PENDING: '待检验',
  INSPECTING: '检验中',
  COMPLETED: '已判定',
  CLOSED: '已作废'
}
export function qcStatusText(s) { return QC_STATUS_MAP[s] || s || '' }
export function qcStatusTagType(s) {
  const m = { PENDING: 'info', INSPECTING: 'warning', COMPLETED: 'success', CLOSED: 'default' }
  return m[s] || 'default'
}

export const QC_RESULT_MAP = {
  PASS: '合格',
  FAIL: '不合格',
  CONCESSION: '让步接收'
}
export function qcResultText(s) { return QC_RESULT_MAP[s] || s || '' }
export function qcResultTagType(s) {
  const m = { PASS: 'success', FAIL: 'error', CONCESSION: 'warning' }
  return m[s] || 'default'
}

export const IPQC_TYPE_MAP = {
  FIRST_CHECK: '首检',
  TOUR_CHECK: '巡检',
  LAST_CHECK: '完工检',
  SPOT_CHECK: '抽检'
}
export function ipqcTypeText(s) { return IPQC_TYPE_MAP[s] || s || '' }

export const RQC_TYPE_MAP = {
  PROD_RETURN: '生产退料',
  PURCHASE_RETURN: '采购退货',
  QC_REJECT: '质检退货'
}
export function rqcTypeText(s) { return RQC_TYPE_MAP[s] || s || '' }

export const RESPONSIBILITY_MAP = {
  SUPPLIER: '供应商',
  PRODUCTION: '生产部门',
  STORAGE: '仓储部门',
  OTHER: '其他'
}
export function responsibilityText(s) { return RESPONSIBILITY_MAP[s] || s || '' }
// 责任归属单选项（inspect 页 RQC 用）
export const RESPONSIBILITY_OPTIONS = Object.keys(RESPONSIBILITY_MAP).map(v => ({ value: v, label: RESPONSIBILITY_MAP[v] }))

// inspect 页各类型的主键/编码字段（api 由页面注入，避免 util 反向依赖 api 层）
export const QC_TYPE_KEYS = {
  IPQC: { id: 'ipqcId', code: 'ipqcCode' },
  IQC:  { id: 'iqcId',  code: 'iqcCode'  },
  OQC:  { id: 'oqcId',  code: 'oqcCode'  },
  RQC:  { id: 'rqcId',  code: 'rqcCode'  }
}

/**
 * 构造检验单暂存/update 的 head（lines/defectRecords 由页面拼接）。
 * 各类型可编辑头字段：
 * - IPQC：流转卡/工序/工单上下文 + quantityCheck
 * - IQC/OQC：来源单据 + 物料/模板（OQC 客户快照只读不回传）
 * - RQC：在 IQC 基础上额外可编辑责任归属与退料原因
 */
export function buildInspectHead(type, f, keys) {
  if (type === 'IPQC') {
    return { ipqcId: f.ipqcId, ipqcCode: f.ipqcCode, quantityCheck: f.quantityCheck,
      workorderId: f.workorderId, cardId: f.cardId, processId: f.processId,
      itemId: f.itemId, templateId: f.templateId, status: f.status }
  }
  const head = { [keys.id]: f[keys.id], [keys.code]: f[keys.code], quantityCheck: f.quantityCheck,
    sourceDocId: f.sourceDocId, sourceDocType: f.sourceDocType,
    itemId: f.itemId, templateId: f.templateId, status: f.status }
  if (type === 'RQC') {
    head.responsibility = f.responsibility
    head.returnReason = f.returnReason
  }
  return head
}

export const DEFECT_LEVEL_MAP = {
  CRITICAL: '致命',
  MAJOR: '严重',
  MINOR: '轻微'
}
export function defectLevelText(s) { return DEFECT_LEVEL_MAP[s] || s || '' }
export function defectLevelTagType(s) {
  const m = { CRITICAL: 'error', MAJOR: 'warning', MINOR: 'default' }
  return m[s] || 'default'
}
export const DEFECT_LEVEL_ORDER = ['CRITICAL', 'MAJOR', 'MINOR']

export const VALUE_TYPES = { NUMBER: 'NUMBER', COUNT: 'COUNT', DICT: 'DICT', TEXT: 'TEXT', FILE: 'FILE' }

function toNum(v) {
  if (v === null || v === undefined || v === '') return null
  const n = Number(v)
  return Number.isNaN(n) ? null : n
}

/**
 * 单行预判：NUMBER 按标准值±偏差区间；DICT 取 PASS/FAIL；TEXT/COUNT/FILE 填了即 PASS。
 * 返回 'PASS'|'FAIL'|null（null 表示未录入）。
 */
export function judgeLine(line) {
  if (!line) return null
  if (line.qcResultType === 'NUMBER') {
    if (!line.checkValText) return null
    const val = toNum(line.checkValText)
    if (val === null) return null
    const std = line.standerVal
    const lo = std != null && line.thresholdMin != null ? Number(std) + Number(line.thresholdMin) : line.thresholdMin
    const hi = std != null && line.thresholdMax != null ? Number(std) + Number(line.thresholdMax) : line.thresholdMax
    const fail = (lo != null && val < Number(lo)) || (hi != null && val > Number(hi))
    return fail ? 'FAIL' : 'PASS'
  }
  if (line.qcResultType === 'DICT') {
    if (!line.checkValText) return null
    return line.checkValText === 'PASS' ? 'PASS' : 'FAIL'
  }
  // TEXT/COUNT/FILE：显式行结果优先，否则填了实测值即 PASS
  if (line.lineResult) return line.lineResult
  if (line.checkValText) return 'PASS'
  return null
}

/**
 * 整单预判：镜像服务端引擎（Ac 值/致命缺陷/三档缺陷率）。
 * 入参：{ lines, defects, quantityCheck, acQuantity, crRateLimit, majRateLimit, minRateLimit }
 * 返回：{ result:'PASS'|'FAIL'|null, reasons:string[], unqualified, cr, maj, min, unentered }
 */
export function predictOrder({ lines, defects, quantityCheck, acQuantity, crRateLimit, majRateLimit, minRateLimit }) {
  const unentered = (lines || []).find(l => judgeLine(l) == null)
  if (unentered) {
    return { result: null, unentered, reasons: [] }
  }
  let failLines = 0
  for (const line of lines || []) {
    if (judgeLine(line) === 'FAIL') failLines++
  }
  let cr = 0, maj = 0, min = 0
  for (const d of defects || []) {
    const q = d.defectQuantity ?? 1
    if (d.defectLevel === 'CRITICAL') cr += q
    else if (d.defectLevel === 'MAJOR') maj += q
    else if (d.defectLevel === 'MINOR') min += q
  }
  const unqualified = Math.max(cr + maj + min, failLines)
  const qty = quantityCheck || 0
  const pct = (q) => qty > 0 ? Math.round((q * 10000) / qty) / 100 : 0
  const reasons = []
  let result = 'PASS'
  if (acQuantity != null && unqualified > acQuantity) {
    result = 'FAIL'
    reasons.push(`不合格数 ${unqualified} 超过 Ac 值 ${acQuantity}`)
  }
  if (cr > 0) {
    result = 'FAIL'
    reasons.push(`存在致命缺陷 ${cr} 件`)
  }
  if (pct(cr) > (crRateLimit ?? 0) || pct(maj) > (majRateLimit ?? 0) || pct(min) > (minRateLimit ?? 0)) {
    result = 'FAIL'
    reasons.push(`缺陷率超阈值（致命 ${pct(cr)}%/${crRateLimit ?? 0}%，严重 ${pct(maj)}%/${majRateLimit ?? 0}%，轻微 ${pct(min)}%/${minRateLimit ?? 0}%）`)
  }
  if (!reasons.length) reasons.push(`不合格数 ${unqualified} ≤ Ac 值，缺陷率未超阈值`)
  return { result, reasons, unqualified, cr, maj, min, unentered: null }
}
