<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="编码" prop="workstationCode"><el-input v-model="queryParams.workstationCode" placeholder="请输入" clearable style="width:180px" @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item label="名称" prop="workstationName"><el-input v-model="queryParams.workstationName" placeholder="请输入" clearable style="width:180px" @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item label="工序" prop="processId"><el-select v-model="queryParams.processId" placeholder="请选择" clearable style="width:180px"><el-option v-for="p in processOptions" :key="p.processId" :label="p.processName" :value="p.processId" /></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button><el-button icon="Refresh" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['mes:md:workstation:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate()" v-hasPermi="['mes:md:workstation:edit']">修改</el-button></el-col>
      <!-- 删除按钮已移除：用启停用(enableFlag)替代物理删除 -->
      <el-col :span="1.5"><el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['mes:md:workstation:export']">导出</el-button></el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>
    <el-table v-loading="loading" :data="list" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="编码" align="center" prop="workstationCode" />
      <el-table-column label="名称" align="center" prop="workstationName" />
      <el-table-column label="类型" align="center" prop="workstationType"><template #default="s"><dict-tag :options="mes_workstation_type" :value="s.row.workstationType" /></template></el-table-column>
      <el-table-column label="工序" align="center" prop="processName">
        <template #default="s"><span>{{ s.row.processName || s.row.processType || '-' }}</span></template>
      </el-table-column>
      <el-table-column label="产能(个/时)" align="center" prop="capacity" />
      <el-table-column label="状态" align="center" prop="status"><template #default="s"><dict-tag :options="mes_workstation_status" :value="s.row.status" /></template></el-table-column>
      <el-table-column label="启用" align="center" width="70">
        <template #default="scope">
          <el-switch v-model="scope.row.enableFlag" active-value="1" inactive-value="0" @change="handleEnableChange(scope.row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="150"><template #default="s"><el-button link type="primary" icon="Edit" @click="handleUpdate(s.row)" v-hasPermi="['mes:md:workstation:edit']">修改</el-button></template></el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
    <el-dialog :title="title" v-model="open" width="750px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="12"><el-form-item label="编码" prop="workstationCode"><el-input v-model="form.workstationCode" :disabled="optType === 'edit' || optType === 'view'" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label-width="70" v-if="optType === 'add'">
              <el-switch v-model="autoGenFlag" active-color="#13ce66" @change="handleAutoGenChange" /><span style="margin-left:6px;font-size:12px;color:#13ce66">自动生成</span>
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="名称" prop="workstationName"><el-input v-model="form.workstationName" /></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="12"><el-form-item label="所属车间" prop="workshopId"><el-select v-model="form.workshopId" placeholder="请选择" style="width:100%"><el-option v-for="w in workshopOptions" :key="w.workshopId" :label="w.workshopName" :value="w.workshopId" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="类型"><el-select v-model="form.workstationType" style="width:100%"><el-option v-for="d in mes_workstation_type" :key="d.value" :label="d.label" :value="d.value" /></el-select></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="12"><el-form-item label="工序" prop="processId"><el-select v-model="form.processId" placeholder="请选择" style="width:100%"><el-option v-for="p in processOptions" :key="p.processId" :label="p.processName" :value="p.processId" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="产能(个/时)"><el-input-number v-model="form.capacity" :min="0" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="12"><el-form-item label="状态"><el-select v-model="form.status" style="width:100%"><el-option v-for="d in mes_workstation_status" :key="d.value" :label="d.label" :value="d.value" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="启用"><el-radio-group v-model="form.enableFlag"><el-radio value="1">是</el-radio><el-radio value="0">否</el-radio></el-radio-group></el-form-item></el-col>
        </el-row>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>

        <!-- 设备和人员子表 (编辑时显示) -->
        <template v-if="form.workstationId">
          <el-divider content-position="left">关联设备</el-divider>
          <el-table :data="machineList" size="small">
            <el-table-column label="设备编码" prop="machineryCode" />
            <el-table-column label="设备名称" prop="machineryName" />
            <el-table-column label="操作" width="80">
              <template #default="s"><el-button link type="danger" icon="Delete" @click="handleDelMachine(s.row)" /></template>
            </el-table-column>
          </el-table>
          <el-button type="primary" size="small" icon="Plus" @click="handleAddMachine" style="margin-top:5px">添加设备</el-button>

          <el-divider content-position="left">操作人员</el-divider>
          <el-table :data="workerList" size="small">
            <el-table-column label="用户名" prop="userName" />
            <el-table-column label="角色">
              <template #default="s">{{ roleLabel(s.row.roleType) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="80">
              <template #default="s"><el-button link type="danger" icon="Delete" @click="handleDelWorker(s.row)" /></template>
            </el-table-column>
          </el-table>
          <el-button type="primary" size="small" icon="Plus" @click="handleAddWorker" style="margin-top:5px">添加人员</el-button>
        </template>
      </el-form>
      <template #footer><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></template>
    </el-dialog>

    <!-- 设备选择器 -->
    <MachinerySelect ref="machinerySelectRef" @onSelected="onMachinesSelected" />
    <!-- 人员选择器 -->
    <UserMultiSelect v-model:showFlag="userSelectVisible" @onSelected="onUsersSelected" />
    <!-- 角色选择弹窗（选完人员后统一指定角色） -->
    <el-dialog v-model="roleDialogVisible" title="选择角色" width="360px" append-to-body>
      <el-form label-width="70px">
        <el-form-item label="角色">
          <el-select v-model="pendingRole" style="width:100%">
            <el-option v-for="r in ROLE_OPTIONS" :key="r.value" :label="r.label" :value="r.value" />
          </el-select>
        </el-form-item>
        <div style="font-size:12px;color:#909399">将为已选 {{ pendingUsers.length }} 人统一设置角色</div>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="confirmAddWorkers">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="Workstation">
import { ref, reactive, toRefs } from 'vue'
import { getCurrentInstance } from 'vue'
import { genSerialCode } from '@/api/mes/sys/autocoderule'
import type { MdWorkstation, WorkstationQueryParams, MdWorkstationMachine, MdWorkstationWorker } from '@/types/api/mes/md/workstation'
import { listWorkstation, getWorkstation, delWorkstation, addWorkstation, updateWorkstation, listMachines, addMachine, delMachine, listWorkers, addWorker, delWorker } from '@/api/mes/md/workstation'
import { listAllWorkshop } from '@/api/mes/md/workshop'
import { listAllProcess } from '@/api/mes/pro/process'
import MachinerySelect from '@/components/machinerySelect/multi.vue'
import UserMultiSelect from '@/components/UserSelect/multi.vue'

const { proxy } = getCurrentInstance() as any; const { sys_yes_no } = useDict('sys_yes_no')
const { mes_workstation_type, mes_workstation_status } = useDict('mes_workstation_type', 'mes_workstation_status')
const list = ref<MdWorkstation[]>([]); const open = ref(false); const loading = ref(true); const showSearch = ref(true)
const ids = ref<number[]>([]); const single = ref(true); const multiple = ref(true); const total = ref(0); const title = ref('')
const autoGenFlag = ref(false)
const optType = ref<string | undefined>(undefined)
const workshopOptions = ref<any[]>([]); const machineList = ref<MdWorkstationMachine[]>([]); const workerList = ref<MdWorkstationWorker[]>([])
const processOptions = ref<any[]>([])

// 设备/人员选择相关
const machinerySelectRef = ref()
const userSelectVisible = ref(false)
const roleDialogVisible = ref(false)
const pendingUsers = ref<any[]>([])
const pendingRole = ref('OPERATOR')
const ROLE_OPTIONS = [
  { label: '操作工', value: 'OPERATOR' },
  { label: '调机工', value: 'SETUP' },
  { label: '检验员', value: 'INSPECTOR' },
]
const ROLE_LABEL_MAP: Record<string, string> = { OPERATOR: '操作工', SETUP: '调机工', INSPECTOR: '检验员' }
function roleLabel(v?: string) { return v ? (ROLE_LABEL_MAP[v] || v) : '' }
const data = reactive({ form: { enableFlag: '1', status: 'IDLE' } as MdWorkstation, queryParams: { pageNum: 1, pageSize: 10, processId: undefined } as WorkstationQueryParams, rules: { workstationCode: [{ required: true, message: '编码不能为空' }], workstationName: [{ required: true, message: '名称不能为空' }], workshopId: [{ required: true, message: '请选择车间' }] } })
const { queryParams, form, rules } = toRefs(data)

function loadSubData(id: number) { listMachines(id).then(r => { machineList.value = r.data || [] }); listWorkers(id).then(r => { workerList.value = r.data || [] }) }
function getList() { loading.value = true; listWorkstation(queryParams.value).then(r => { list.value = r.rows; total.value = r.total; loading.value = false }) }
function cancel() { open.value = false; reset() }
function handleAutoGenChange(flag: boolean) {
  if (flag) genSerialCode('WORKSTATION_CODE').then((r: any) => { form.value.workstationCode = r.data })
  else form.value.workstationCode = ''
}
function reset() { optType.value = undefined; autoGenFlag.value = false; form.value = { enableFlag: '1', status: 'IDLE' } as MdWorkstation; machineList.value = []; workerList.value = []; proxy.resetForm('formRef') }
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleSelectionChange(s: MdWorkstation[]) { ids.value = s.map(i => i.workstationId!); single.value = s.length !== 1; multiple.value = !s.length }
function handleAdd() { reset(); optType.value = 'add'; listAllWorkshop().then(r => { workshopOptions.value = r.data || [] }); open.value = true; title.value = '新增工作站' }
function handleUpdate(row?: MdWorkstation) { reset(); optType.value = 'edit'; const id = row?.workstationId || ids.value[0]; Promise.all([listAllWorkshop(), getWorkstation(id)]).then(([wsRes, gwRes]) => { workshopOptions.value = wsRes.data || []; form.value = gwRes.data; loadSubData(id); open.value = true; title.value = '修改工作站' }) }
function submitForm() { proxy.$refs['formRef'].validate((v: boolean) => { if (v) { (form.value.workstationId ? updateWorkstation(form.value) : addWorkstation(form.value)).then(() => { proxy.$modal.msgSuccess('操作成功'); open.value = false; getList() }).catch((err: any) => { proxy.$modal.msgError(err?.msg || '操作失败') }) } else { proxy.$modal.msgError('请完善表单信息') } }) }
function handleEnableChange(row: any) {
  const newVal = row.enableFlag
  const text = newVal === '1' ? '启用' : '停用'
  proxy.$modal.confirm(`确认要${text}"${row.workstationName}"吗？`).then(() => {
    updateWorkstation({ workstationId: row.workstationId, enableFlag: newVal } as any).then(() => proxy.$modal.msgSuccess(`${text}成功`))
  }).catch(() => {
    row.enableFlag = newVal === '1' ? '0' : '1'
    getList()
  })
}

function handleDelete(row?: MdWorkstation) { const ids_ = row?.workstationId || ids.value; proxy.$modal.confirm('确认删除？').then(() => delWorkstation(ids_)).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') }).catch(() => {}) }
function handleExport() { proxy.download('mes/md/workstation/export', { ...queryParams.value }, `workstation_${Date.now()}.xlsx`) }

function handleAddMachine() {
  const excludeIds = machineList.value.map((m: MdWorkstationMachine) => m.machineryId!).filter((v: number | undefined) => v != null && v > 0)
  machinerySelectRef.value?.open(excludeIds)
}
async function onMachinesSelected(rows: any[]) {
  const existed = new Set(machineList.value.map((m: MdWorkstationMachine) => m.machineryId).filter((v: number | undefined) => v != null && v > 0))
  const toAdd = (rows || []).filter((r: any) => !existed.has(r.machineryId))
  if (!toAdd.length) { proxy.$modal.msgWarning('所选设备已全部关联'); return }
  try {
    for (const r of toAdd) {
      await addMachine({
        workstationId: form.value.workstationId,
        machineryId: r.machineryId,
        machineryCode: r.machineryCode,
        machineryName: r.machineryName,
      } as MdWorkstationMachine)
    }
    proxy.$modal.msgSuccess(`成功添加 ${toAdd.length} 台设备`)
  } catch (e: any) {
    proxy.$modal.msgError(e?.msg || '添加失败')
  } finally {
    loadSubData(form.value.workstationId!)
  }
}
function handleDelMachine(row: MdWorkstationMachine) { delMachine(row.recordId!).then(() => { proxy.$modal.msgSuccess('删除成功'); loadSubData(form.value.workstationId!) }) }

function handleAddWorker() { userSelectVisible.value = true }
function onUsersSelected(rows: any[]) {
  pendingUsers.value = rows || []
  pendingRole.value = 'OPERATOR'
  roleDialogVisible.value = true
}
async function confirmAddWorkers() {
  const role = pendingRole.value
  const existedKey = new Set(workerList.value.map((w: MdWorkstationWorker) => `${w.userId}-${w.roleType}`))
  const toAdd = pendingUsers.value.filter((u: any) => !existedKey.has(`${u.userId}-${role}`))
  if (!toAdd.length) { proxy.$modal.msgWarning('所选人员已关联该角色'); roleDialogVisible.value = false; return }
  try {
    for (const u of toAdd) {
      await addWorker({
        workstationId: form.value.workstationId,
        userId: u.userId,
        userName: u.userName,
        roleType: role,
      } as MdWorkstationWorker)
    }
    proxy.$modal.msgSuccess(`成功添加 ${toAdd.length} 人`)
  } catch (e: any) {
    proxy.$modal.msgError(e?.msg || '添加失败')
  } finally {
    roleDialogVisible.value = false
    loadSubData(form.value.workstationId!)
  }
}
function handleDelWorker(row: MdWorkstationWorker) { delWorker(row.recordId!).then(() => { proxy.$modal.msgSuccess('删除成功'); loadSubData(form.value.workstationId!) }) }

getList()
listAllProcess().then(r => { processOptions.value = (r as any).data || [] })
</script>

<style scoped>
:deep(.el-form-item__label) { padding-right: 16px !important; }
</style>
