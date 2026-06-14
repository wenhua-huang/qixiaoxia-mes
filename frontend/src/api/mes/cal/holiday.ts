import request from '@/utils/request'

// 查询节假日设置列表
export function listHoliday(query) {
  return request({
    url: '/mes/cal/holiday/list',
    method: 'get',
    params: query
  })
}

// 查询节假日设置详细
export function getHoliday(holidayId) {
  return request({
    url: '/mes/cal/holiday/' + holidayId,
    method: 'get'
  })
}

// 新增节假日设置
export function addHoliday(data) {
  return request({
    url: '/mes/cal/holiday',
    method: 'post',
    data: data
  })
}

// 修改节假日设置
export function updateHoliday(data) {
  return request({
    url: '/mes/cal/holiday',
    method: 'put',
    data: data
  })
}

// 删除节假日设置
export function delHoliday(holidayId) {
  return request({
    url: '/mes/cal/holiday/' + holidayId,
    method: 'delete'
  })
}
