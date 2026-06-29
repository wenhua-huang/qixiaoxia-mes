/** 工序换型时间规范 */
export interface ProChangeover {
  id?: number
  factoryId?: number
  name?: string
  fromProcessId?: number
  toProcessId?: number
  workstationId?: number
  durationMinutes?: number
  remark?: string
  createBy?: string
  createTime?: string
  updateBy?: string
  updateTime?: string
}
