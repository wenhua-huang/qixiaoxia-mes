import request from '@/utils/request'
import type { ProChangeover } from '@/types/api/mes/pro/changeover'

export function listChangeover(query: any) {
  return request({ url: '/mes/pro/changeover/list', method: 'get', params: query })
}

export function getChangeover(id: number) {
  return request({ url: '/mes/pro/changeover/' + id, method: 'get' })
}

export function addChangeover(data: ProChangeover) {
  return request({ url: '/mes/pro/changeover', method: 'post', data })
}

export function updateChangeover(data: ProChangeover) {
  return request({ url: '/mes/pro/changeover', method: 'put', data })
}

export function delChangeover(ids: number | number[]) {
  const idStr = Array.isArray(ids) ? ids.join(',') : ids
  return request({ url: '/mes/pro/changeover/' + idStr, method: 'delete' })
}
