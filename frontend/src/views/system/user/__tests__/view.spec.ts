import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick, ref } from 'vue'

// ==================== Mock APIs ====================
vi.mock('@/api/system/user', () => ({
  getUser: vi.fn().mockResolvedValue({
    data: {
      userId: 1,
      userName: 'zhangsan',
      nickName: '张三',
      employeeType: 'REGULAR',
      wageType: 'PIECE',
      hireDate: '2024-03-15',
      openid: 'oTest123456',
    },
    posts: [],
    roles: [],
    postIds: [],
    roleIds: [],
  }),
}))

vi.mock('@/api/mes/md/employeeSkill', () => ({
  listEmployeeSkill: vi.fn().mockResolvedValue({
    rows: [
      { skillId: 1, skillName: '印刷操作', skillLevel: 'SENIOR' },
      { skillId: 2, skillName: '制袋调机', skillLevel: 'MIDDLE' },
    ],
    total: 2,
  }),
}))

// ==================== Mock router & store ====================
vi.mock('@/store/modules/dict', async () => {
  const { defineStore } = await import('pinia')
  return { default: defineStore('dict', () => ({ getDict: vi.fn(() => []) })) }
})

vi.mock('@/utils/ruoyi', () => ({
  selectDictLabel: vi.fn((dict: any[], val: string) => {
    const item = dict.find((d: any) => d.value === val)
    return item ? item.label : ''
  }),
  selectDictLabels: vi.fn(),
}))

vi.mock('@/utils/dict', () => ({
  useDict: () => ({
    sys_user_sex: ref([{ label: '男', value: '0' }, { label: '女', value: '1' }]),
    mes_wage_type: ref([{ label: '月工资', value: 'MONTHLY' }, { label: '计件', value: 'PIECE' }]),
    mes_employee_type: ref([{ label: '正式工', value: 'REGULAR' }, { label: '临时工', value: 'TEMPORARY' }]),
    mes_skill_level: ref([{ label: '初级', value: 'JUNIOR' }, { label: '中级', value: 'MIDDLE' }, { label: '高级', value: 'SENIOR' }]),
  }),
}))

// ==================== Import component ====================
import UserView from '@/views/system/user/view.vue'

function mountUserView() {
  return mount(UserView, {
    global: {
      stubs: {
        'el-drawer': { template: '<div class="mock-drawer"><slot /></div>' },
        'el-tag': { template: '<span class="mock-tag"><slot /></span>' },
      },
    },
  })
}

describe('用户详情 — user/view.vue (员工信息扩展)', () => {
  beforeEach(() => { vi.clearAllMocks() })

  it('应该正常挂载', async () => {
    const wrapper = mountUserView()
    await nextTick()
    expect(wrapper.exists()).toBe(true)
  })

  it('打开详情时应显示员工信息区块', async () => {
    const wrapper = mountUserView()
    await nextTick()
    // 模拟打开详情
    await wrapper.vm.open(1)
    await nextTick()
    await nextTick()
    const text = wrapper.text()
    expect(text).toContain('员工信息')
    expect(text).toContain('员工类型')
    expect(text).toContain('工资类型')
    expect(text).toContain('入职日期')
    expect(text).toContain('微信openid')
  })

  it('打开详情时应显示技能列表', async () => {
    const wrapper = mountUserView()
    await nextTick()
    await wrapper.vm.open(1)
    await nextTick()
    await nextTick()
    const text = wrapper.text()
    expect(text).toContain('技能')
  })

  it('打开详情时调用 getUser 和 listEmployeeSkill API', async () => {
    const { getUser } = await import('@/api/system/user')
    const { listEmployeeSkill } = await import('@/api/mes/md/employeeSkill')
    const wrapper = mountUserView()
    await nextTick()
    await wrapper.vm.open(1)
    await nextTick()
    await nextTick()
    expect(getUser).toHaveBeenCalledWith(1)
    expect(listEmployeeSkill).toHaveBeenCalledWith({ userId: 1 })
  })

  it('template 中标签应正确闭合（回归测试）', () => {
    const wrapper = mountUserView()
    expect(wrapper.exists()).toBe(true)
  })

  it('关闭抽屉时应隐藏', async () => {
    const wrapper = mountUserView()
    await nextTick()
    await wrapper.vm.open(1)
    await nextTick()
    await wrapper.vm.handleClose()
    await nextTick()
    expect(wrapper.vm.visible).toBe(false)
  })
})
