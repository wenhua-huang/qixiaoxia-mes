import request from '@/utils/request'
import type { TodoListQueryParams, SysTodoList, AjaxResult, TableDataInfo } from '@/types'

// 查询待办列表
export function listTodoList(query: TodoListQueryParams): Promise<TableDataInfo<SysTodoList[]>> {
  return request({ url: '/mes/sys/todolist/list', method: 'get', params: query })
}

// 查询待办详细
export function getTodoList(todoId: number): Promise<AjaxResult<SysTodoList>> {
  return request({ url: '/mes/sys/todolist/' + todoId, method: 'get' })
}

// 新增待办
export function addTodoList(data: SysTodoList): Promise<AjaxResult> {
  return request({ url: '/mes/sys/todolist', method: 'post', data: data })
}

// 修改待办
export function updateTodoList(data: SysTodoList): Promise<AjaxResult> {
  return request({ url: '/mes/sys/todolist', method: 'put', data: data })
}

// 删除待办
export function delTodoList(todoId: number | number[]): Promise<AjaxResult> {
  return request({ url: '/mes/sys/todolist/' + todoId, method: 'delete' })
}

// 处理待办
export function handleTodo(todoId: number, data: SysTodoList): Promise<AjaxResult> {
  return request({ url: '/mes/sys/todolist/' + todoId + '/handle', method: 'put', data: data })
}

// 按状态统计待办数量
export function countByStatus(): Promise<AjaxResult<Record<string, number>>> {
  return request({ url: '/mes/sys/todolist/countByStatus', method: 'get' })
}
