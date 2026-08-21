import request from '@/utils/request'

export { listDefect } from './defect'

// ============ IQC 来料检验 ============
export function listIqc(query) {
  return request({ url: '/mes/qc/iqc/list', method: 'get', params: query })
}
export function getIqc(iqcId) {
  return request({ url: '/mes/qc/iqc/' + iqcId, method: 'get' })
}
export function updateIqc(data) {
  return request({ url: '/mes/qc/iqc', method: 'put', data })
}
export function judgeIqc(iqcId, concessionReason) {
  return request({
    url: '/mes/qc/iqc/judge/' + iqcId,
    method: 'put',
    data: { concessionReason: concessionReason || null }
  })
}
// 扫收货单号：返回 { recpt, iqcList }
export function scanIqc(recptCode) {
  return request({ url: '/mes/qc/scan/iqc', method: 'get', params: { code: recptCode } })
}

// ============ IPQC 过程检验 ============
export function listIpqc(query) {
  return request({ url: '/mes/qc/ipqc/list', method: 'get', params: query })
}
export function getIpqc(ipqcId) {
  return request({ url: '/mes/qc/ipqc/' + ipqcId, method: 'get' })
}
export function addIpqc(data) {
  return request({ url: '/mes/qc/ipqc', method: 'post', data })
}
export function updateIpqc(data) {
  return request({ url: '/mes/qc/ipqc', method: 'put', data })
}
export function judgeIpqc(ipqcId, concessionReason) {
  return request({
    url: '/mes/qc/ipqc/judge/' + ipqcId,
    method: 'put',
    data: { concessionReason: concessionReason || null }
  })
}

// ============ OQC 出货检验 ============
export function listOqc(query) {
  return request({ url: '/mes/qc/oqc/list', method: 'get', params: query })
}
export function getOqc(oqcId) {
  return request({ url: '/mes/qc/oqc/' + oqcId, method: 'get' })
}
export function updateOqc(data) {
  return request({ url: '/mes/qc/oqc', method: 'put', data })
}
export function judgeOqc(oqcId, concessionReason) {
  return request({
    url: '/mes/qc/oqc/judge/' + oqcId,
    method: 'put',
    data: { concessionReason: concessionReason || null }
  })
}

// ============ RQC 退货检验 ============
export function listRqc(query) {
  return request({ url: '/mes/qc/rqc/list', method: 'get', params: query })
}
export function getRqc(rqcId) {
  return request({ url: '/mes/qc/rqc/' + rqcId, method: 'get' })
}
export function updateRqc(data) {
  return request({ url: '/mes/qc/rqc', method: 'put', data })
}
export function judgeRqc(rqcId, concessionReason) {
  return request({
    url: '/mes/qc/rqc/judge/' + rqcId,
    method: 'put',
    data: { concessionReason: concessionReason || null }
  })
}

// ============ 检验模板（IPQC 手工建单选模板） ============
export function listTemplate(query) {
  return request({ url: '/mes/qc/template/list', method: 'get', params: query })
}
