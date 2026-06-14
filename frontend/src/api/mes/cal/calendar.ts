import request from '@/utils/request'

// 查询排班日历列表
export function listCalendars(query: any) {
    return request({
      url: '/mes/cal/calendar/list',
      method: 'get',
      params: query
    })
}
