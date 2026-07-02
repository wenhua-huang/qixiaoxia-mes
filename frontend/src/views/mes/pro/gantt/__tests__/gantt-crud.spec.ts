import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { ElMessage, ElMessageBox } from 'element-plus'

// ── Mock 外部依赖 ──
vi.mock('@/utils/request', () => ({
  default: vi.fn().mockResolvedValue({ data: [] })
}))

vi.mock('@/api/mes/pro/gantt', () => ({
  getWorkOrderGantt: vi.fn().mockResolvedValue({ data: { tasks: [], links: [] } }),
  getWorkstationGantt: vi.fn().mockResolvedValue({ data: { tasks: [], links: [] } })
}))

vi.mock('@/api/mes/pro/workorder', () => ({
  listWorkorder: vi.fn().mockResolvedValue({ rows: [], total: 0 }),
  getWorkorderDetail: vi.fn().mockResolvedValue({
    data: {
      workorder: { workorderId: 1, workorderCode: 'WO-001', productName: '测试产品', quantity: 100 },
      routeProcesses: [
        { processId: 10, processName: '印刷', processCode: 'PRINT' },
        { processId: 20, processName: '分切', processCode: 'CUT' },
      ]
    }
  })
}))

vi.mock('@/api/mes/md/workstation', () => ({
  listWorkstation: vi.fn().mockResolvedValue({ rows: [{ workstationId: 1, workstationCode: 'WS01', workstationName: '印刷机1号' }], total: 1 })
}))

vi.mock('@/api/mes/pro/task', () => ({
  addTask: vi.fn().mockResolvedValue({ data: {} }),
  updateTask: vi.fn().mockResolvedValue({ data: {} }),
  delTask: vi.fn().mockResolvedValue({ data: {} })
}))

vi.mock('@/utils/auth', () => ({
  getToken: () => 'mock-token'
}))

vi.mock('@element-plus/icons-vue', () => ({
  Search: { template: '<span />' },
  Refresh: { template: '<span />' },
  Plus: { template: '<span />' },
  Delete: { template: '<span />' },
}))

// Mock router
const mockPush = vi.fn()
const mockRoute = { query: {}, path: '/mes/pro/gantt' }
vi.mock('vue-router', () => ({
  useRoute: () => mockRoute,
  useRouter: () => ({ push: mockPush })
}))

// Mock Pinia stores
vi.mock('@/store/modules/user', () => ({
  default: {
    state: { permissions: ['*:*:*'], roles: ['admin'] },
    getters: { permissions: ['*:*:*'] }
  }
}))

// ── 组件导入 ──
import ProGantt from '@/views/mes/pro/gantt/index.vue'
import { getWorkOrderGantt } from '@/api/mes/pro/gantt'
import { getWorkorderDetail } from '@/api/mes/pro/workorder'
import { addTask, delTask } from '@/api/mes/pro/task'

function mountGantt() {
  return mount(ProGantt, {
    global: {
      stubs: {
        'el-button': { template: '<button :disabled="$attrs.disabled" @click="$emit(\'click\')"><slot /></button>', inheritAttrs: true },
        'el-dialog': { template: '<div v-if="$attrs.modelValue" class="el-dialog"><div class="el-dialog__title"><slot name="title" />{{ $attrs.title }}</div><slot /><slot name="footer" /></div>', inheritAttrs: true },
        'el-select': { template: '<select><slot /></select>' },
        'el-option': { template: '<option />' },
        'el-form': { template: '<form><slot /></form>' },
        'el-form-item': { template: '<div><slot /></div>' },
        'el-input': { template: '<input />', inheritAttrs: true },
        'el-input-number': { template: '<input type="number" />', inheritAttrs: true },
        'el-date-picker': { template: '<input type="datetime" />', inheritAttrs: true },
        'el-tag': { template: '<span><slot /></span>' },
        'el-color-picker': { template: '<input type="color" />', inheritAttrs: true },
        'el-descriptions': { template: '<div><slot /></div>' },
        'el-descriptions-item': { template: '<div><slot /></div>' },
        'el-row': { template: '<div><slot /></div>' },
        'el-col': { template: '<div><slot /></div>' },
        'el-table': { template: '<table><slot /></table>' },
        'el-table-column': { template: '<td><slot /></td>' },
        'el-radio-group': { template: '<div><slot /></div>' },
        'el-radio-button': { template: '<button><slot /></button>' },
        'el-tooltip': { template: '<div><slot /></div>' },
        'GanttChart': { template: '<div class="gc-root"><div class="gc-bar" @click="$emit(\'select\', {id:\'1\',text:\'印刷→印刷机1号\',processName:\'印刷\',processId:10,workstationId:1,start:\'2026-07-01T08:00:00\',end:\'2026-07-01T09:00:00\',duration:60,quantity:100,colorCode:\'#409eff\'})" /></div>', emits: ['select', 'barMove'], methods: { render() {} } },
        'SnapShotPanel': { template: '<div class="snapshot-panel" />' },
        'WorkOrderQueue': { template: '<div class="wo-queue"><div class="queue-card" @click="$emit(\'select\',1)" /></div>', emits: ['select', 'scheduled'] },
        'UtilizationBar': { template: '<div />' },
        'right-toolbar': { template: '<div />' },
        'Pagination': { template: '<div />' },
        'svg-icon': { template: '<span />' },
        'AppIcon': { template: '<span />' },
      },
      directives: {
        hasPermi: { mounted() {} },
        hasRole: { mounted() {} },
      }
    }
  })
}

describe('甘特图任务CRUD — 单元测试', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockRoute.query = {}
  })

  // ═══ normalizeTime ═══
  describe('normalizeTime', () => {
    it('将ISO格式T替换为空格', async () => {
      const wrapper = mountGantt()
      // 通过 onTaskSelect 触发 normalizeTime（通过 GanttChart 的 select 事件）
      await wrapper.find('.gc-bar').trigger('click')
      await nextTick()
      // 验证弹窗打开
      expect(wrapper.find('.el-dialog').exists()).toBe(true)
    })

    it('normalizeTime 将 ISO 格式转为空格格式', async () => {
      const wrapper = mountGantt()
      // 触发 GanttChart select 事件，task.start 为 ISO 格式(T分隔符)
      // onTaskSelect 内部调用 normalizeTime(task.start) → 转换为空格格式
      await wrapper.find('.gc-bar').trigger('click')
      await nextTick()

      const vm = wrapper.vm as any
      // normalizeTime('2026-07-01T08:00:00') → '2026-07-01 08:00:00'
      expect(vm.taskForm.startTime).toBe('2026-07-01 08:00:00')
      expect(vm.taskForm.endTime).toBe('2026-07-01 09:00:00')
    })

    it('taskForm.startTime 初始值为空字符串', async () => {
      const wrapper = mountGantt()
      const vm = wrapper.vm as any
      expect(vm.taskForm?.startTime).toBe('')
    })
  })

  // ═══ resetTaskForm ═══
  describe('resetTaskForm', () => {
    it('将所有字段重置为默认值', async () => {
      const wrapper = mountGantt()
      const vm = wrapper.vm as any

      // 先设置一些值
      vm.taskForm.processId = 10
      vm.taskForm.processName = '印刷'
      vm.taskForm.workstationId = 1
      vm.taskForm.quantity = 50
      vm.taskForm.startTime = '2026-07-01 08:00:00'
      vm.taskForm.duration = 120

      // 调用 reset（通过新增任务触发）
      vm.resetTaskForm()

      expect(vm.taskForm.processId).toBeNull()
      expect(vm.taskForm.processName).toBe('')
      expect(vm.taskForm.workstationId).toBeNull()
      expect(vm.taskForm.quantity).toBe(1)
      expect(vm.taskForm.startTime).toBeNull()
      expect(vm.taskForm.duration).toBe(1)
      expect(vm.taskForm.setupDuration).toBe(0)
      expect(vm.taskForm.colorCode).toBe('#409eff')
    })
  })

  // ═══ onTaskSelect ═══
  describe('onTaskSelect', () => {
    it('填充 processId 到 taskForm', async () => {
      const wrapper = mountGantt()

      // 触发 GanttChart 的 select 事件
      await wrapper.find('.gc-bar').trigger('click')
      await nextTick()

      const vm = wrapper.vm as any
      expect(vm.taskForm.processId).toBe(10)
      expect(vm.taskForm.processName).toBe('印刷')
      expect(vm.detailOpen).toBe(true)
    })

    it('填充 workstationId 到 taskForm', async () => {
      const wrapper = mountGantt()
      await wrapper.find('.gc-bar').trigger('click')
      await nextTick()

      const vm = wrapper.vm as any
      expect(vm.taskForm.workstationId).toBe(1)
    })
  })

  // ═══ handleAddTask ═══
  describe('handleAddTask', () => {
    it('无工单ID时直接返回', async () => {
      const wrapper = mountGantt()
      const vm = wrapper.vm as any
      vm.queryParams.workorderId = null

      // 不应抛出错误
      expect(() => vm.handleAddTask()).not.toThrow()
      expect(vm.detailOpen).toBe(false)
    })

    it('无工序选项时显示警告', async () => {
      const wrapper = mountGantt()
      const vm = wrapper.vm as any
      vm.queryParams.workorderId = 1
      vm.routeProcesses = []  // 无工序

      const warnSpy = vi.spyOn(ElMessage, 'warning')
      vm.handleAddTask()
      await nextTick()

      expect(warnSpy).toHaveBeenCalledWith(expect.stringContaining('工艺路线'))
      expect(vm.detailOpen).toBe(false)
    })

    it('有工序选项时打开编辑弹窗并重置表单', async () => {
      const wrapper = mountGantt()
      const vm = wrapper.vm as any
      vm.queryParams.workorderId = 1
      vm.routeProcesses = [{ processId: 10, processName: '印刷', processCode: 'PRINT' }]
      // 必须填充 ganttTasks，否则 handleAddTask 中 ganttTasks.value[0] 为 undefined 导致崩溃
      vm.ganttTasks = [{ id: 'WO-1', text: '测试工单', type: 'project', quantity: 50, children: [] }]

      vm.handleAddTask()
      await nextTick()

      expect(vm.detailOpen).toBe(true)
      expect(vm.taskDialogMode).toBe('edit')
      expect(vm.taskForm.taskId).toBeNull()       // 新增模式
      expect(vm.taskForm.processId).toBeNull()    // 待选择
      expect(vm.taskForm.quantity).toBe(50)       // 从 ganttTasks[0] 获取
    })
  })

  // ═══ handleDeleteTask ═══
  describe('handleDeleteTask', () => {
    it('无选中任务时直接返回', async () => {
      const wrapper = mountGantt()
      const vm = wrapper.vm as any
      vm.selectedTask = {}

      expect(() => vm.handleDeleteTask()).not.toThrow()
    })

    it('确认后调用 delTask API', async () => {
      const wrapper = mountGantt()
      const vm = wrapper.vm as any
      vm.selectedTask = { id: '123' }
      vm.queryParams.workorderId = 1

      // Mock ElMessageBox.confirm 直接 resolve
      vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm' as any)

      await vm.handleDeleteTask()
      await nextTick()

      expect(ElMessageBox.confirm).toHaveBeenCalled()
      expect(delTask).toHaveBeenCalledWith(123)
    })
  })

  // ═══ processOptions computed ═══
  describe('processOptions', () => {
    it('从 routeProcesses 构建工序选项', async () => {
      const wrapper = mountGantt()
      const vm = wrapper.vm as any
      vm.routeProcesses = [
        { processId: 10, processName: '印刷', processCode: 'PRINT' },
        { processId: 20, processName: '分切', processCode: 'CUT' },
      ]
      await nextTick()

      expect(vm.processOptions).toHaveLength(2)
      expect(vm.processOptions[0]).toEqual({ processId: 10, processName: '印刷', processCode: 'PRINT' })
    })

    it('routeProcesses 为空时从甘特图任务提取', async () => {
      const wrapper = mountGantt()
      const vm = wrapper.vm as any
      vm.routeProcesses = []
      vm.ganttTasks = [{
        id: 'WO-1', text: '测试', type: 'project',
        children: [
          { id: '1', processId: 10, processName: '印刷', text: '印刷' },
          { id: '2', processId: 20, processName: '分切', text: '分切' },
          { id: '3', processId: 10, processName: '印刷', text: '印刷2' }, // 重复 processId
        ]
      }]
      await nextTick()

      expect(vm.processOptions).toHaveLength(2)  // 去重后仅2个
    })
  })

  // ═══ onMounted ═══
  describe('onMounted — query参数处理', () => {
    it('query.workorderId 为字符串时正常加载', async () => {
      mockRoute.query = { workorderId: '42' }

      mountGantt()
      await nextTick()
      await nextTick()  // 等待 async onMounted

      expect(getWorkorderDetail).toHaveBeenCalledWith(42)
      expect(getWorkOrderGantt).toHaveBeenCalledWith(42)
    })

    it('query.workorderId 为数组时取第一个元素', async () => {
      mockRoute.query = { workorderId: ['42', '43'] as any }

      mountGantt()
      await nextTick()
      await nextTick()

      expect(getWorkorderDetail).toHaveBeenCalledWith(42)
    })

    it('无 query 参数时不加载', async () => {
      mockRoute.query = {}

      mountGantt()
      await nextTick()
      await nextTick()

      expect(getWorkorderDetail).not.toHaveBeenCalled()
    })
  })

  // ═══ submitTaskEdit ═══
  describe('submitTaskEdit', () => {
    it('新增任务时 payload 包含 processId', async () => {
      const wrapper = mountGantt()
      const vm = wrapper.vm as any
      vm.queryParams.workorderId = 1
      vm.taskForm.processId = 10
      vm.taskForm.processName = '印刷'
      vm.taskForm.quantity = 100
      vm.taskForm.startTime = '2026-07-01 08:00:00'
      vm.taskForm.endTime = '2026-07-01 09:00:00'
      vm.taskForm.duration = 60
      vm.taskForm.taskId = null  // 新增

      await vm.submitTaskEdit()
      await nextTick()

      expect(addTask).toHaveBeenCalledWith(expect.objectContaining({
        workorderId: 1,
        processId: 10,
        processName: '印刷',
        quantity: 100,
        startTime: '2026-07-01 08:00:00',
        endTime: '2026-07-01 09:00:00',
        duration: 60,
      }))
    })

    it('编辑任务时 payload 包含 taskId', async () => {
      const wrapper = mountGantt()
      const vm = wrapper.vm as any
      vm.queryParams.workorderId = 1
      vm.taskForm.taskId = 99
      vm.taskForm.processId = 10
      vm.taskForm.processName = '印刷'
      vm.taskForm.quantity = 50

      await vm.submitTaskEdit()
      await nextTick()

      const { updateTask } = await import('@/api/mes/pro/task')
      expect(updateTask).toHaveBeenCalledWith(expect.objectContaining({
        taskId: 99,
        processId: 10,
      }))
    })
  })

  // ═══ onDialogClose ═══
  describe('onDialogClose', () => {
    it('关闭对话框时重置 taskDialogMode 和 taskForm', async () => {
      const wrapper = mountGantt()
      const vm = wrapper.vm as any

      // 模拟编辑状态
      vm.taskDialogMode = 'edit'
      vm.taskForm.processId = 99
      vm.onDialogClose()

      expect(vm.taskDialogMode).toBe('edit')    // 默认进入编辑模式
      expect(vm.taskForm.processId).toBeNull()
    })
  })

  // ═══ 时间联动 ═══
  describe('时间联动', () => {
    it('修改开始时间 → 自动计算时长', async () => {
      const wrapper = mountGantt()
      const vm = wrapper.vm as any

      vm.taskForm.startTime = '2026-07-01 08:00:00'
      vm.taskForm.endTime = '2026-07-01 09:30:00'
      await nextTick()

      expect(vm.taskForm.duration).toBe(90)  // 1.5小时 = 90分钟
    })

    it('修改结束时间 → 自动计算时长', async () => {
      const wrapper = mountGantt()
      const vm = wrapper.vm as any

      vm.taskForm.startTime = '2026-07-01 08:00:00'
      vm.taskForm.endTime = '2026-07-01 10:00:00'
      await nextTick()

      expect(vm.taskForm.duration).toBe(120)  // 2小时 = 120分钟
    })

    it('修改时长 → 自动计算结束时间', async () => {
      const wrapper = mountGantt()
      const vm = wrapper.vm as any

      vm.taskForm.startTime = '2026-07-01 08:00:00'
      await nextTick()
      vm.taskForm.duration = 60
      await nextTick()

      expect(vm.taskForm.endTime).toBe('2026-07-01 09:00:00')
    })

    it('无开始时间时不触发联动', async () => {
      const wrapper = mountGantt()
      const vm = wrapper.vm as any

      vm.taskForm.startTime = null
      vm.taskForm.duration = 60
      await nextTick()

      // 无开始时间，endTime 不变
      expect(vm.taskForm.endTime).toBe('')
    })

    it('时长为0时设为最小值1', async () => {
      const wrapper = mountGantt()
      const vm = wrapper.vm as any

      vm.taskForm.startTime = '2026-07-01 08:00:00'
      vm.taskForm.endTime = '2026-07-01 08:00:00'  // 相同时间 = 0分钟
      await nextTick()

      expect(vm.taskForm.duration).toBe(1)  // 最小为1
    })
  })
})
