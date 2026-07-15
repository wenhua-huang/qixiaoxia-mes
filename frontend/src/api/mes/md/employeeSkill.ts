import request from '@/utils/request'
import type { AjaxResult, TableDataInfo } from '@/types'

/** 员工技能 */
export interface MdEmployeeSkill {
  /** 技能ID */
  skillId?: number
  /** 工厂ID */
  factoryId?: number
  /** 用户ID */
  userId?: number
  /** 用户姓名 */
  userName?: string
  /** 技能名称 */
  skillName?: string
  /** 技能等级:JUNIOR-初级,MIDDLE-中级,SENIOR-高级 */
  skillLevel?: string
  /** 备注 */
  remark?: string
  /** 创建者 */
  createBy?: string
  /** 创建时间 */
  createTime?: string
  /** 更新者 */
  updateBy?: string
  /** 更新时间 */
  updateTime?: string
}

/** 员工技能查询参数 */
export interface EmployeeSkillQueryParams {
  /** 用户ID */
  userId?: number
  /** 用户姓名 */
  userName?: string
  /** 技能名称 */
  skillName?: string
  /** 技能等级 */
  skillLevel?: string
  pageNum?: number
  pageSize?: number
}

// 查询员工技能列表
export function listEmployeeSkill(query: EmployeeSkillQueryParams): Promise<TableDataInfo<MdEmployeeSkill[]>> {
  return request({
    url: '/mes/md/employee-skill/list',
    method: 'get',
    params: query
  })
}

// 查询员工技能详细
export function getEmployeeSkill(skillId: number): Promise<AjaxResult<MdEmployeeSkill>> {
  return request({
    url: '/mes/md/employee-skill/' + skillId,
    method: 'get'
  })
}

// 新增员工技能
export function addEmployeeSkill(data: MdEmployeeSkill): Promise<AjaxResult<MdEmployeeSkill>> {
  return request({
    url: '/mes/md/employee-skill',
    method: 'post',
    data: data
  })
}

// 修改员工技能
export function updateEmployeeSkill(data: MdEmployeeSkill): Promise<AjaxResult> {
  return request({
    url: '/mes/md/employee-skill',
    method: 'put',
    data: data
  })
}

// 删除员工技能
export function delEmployeeSkill(skillId: number | number[]): Promise<AjaxResult> {
  return request({
    url: '/mes/md/employee-skill/' + skillId,
    method: 'delete'
  })
}
