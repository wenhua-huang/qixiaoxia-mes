<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="编码" prop="workstationCode"><el-input v-model="queryParams.workstationCode" placeholder="请输入" clearable style="width:180px" @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item label="名称" prop="workstationName"><el-input v-model="queryParams.workstationName" placeholder="请输入" clearable style="width:180px" @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item><el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button><el-button icon="Refresh" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['mes:md:workstation:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate()" v-hasPermi="['mes:md:workstation:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()" v-hasPermi="['mes:md:workstation:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['mes:md:workstation:export']">导出</el-button></el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>
    <el-table v-loading="loading" :data="list" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="编码" align="center" prop="workstationCode" />
      <el-table-column label="名称" align="center" prop="workstationName" />
      <el-table-column label="类型" align="center" prop="workstationType" />
      <el-table-column label="工序" align="center" prop="processType" />
      <el-table-column label="产能(个/时)" align="center" prop="capacity" />
      <el-table-column label="状态" align="center" prop="status" />
      <el-table-column label="启用" align="center" prop="enableFlag"><template #default="s"><dict-tag :options="sys_yes_no" :value="s.row.enableFlag" /></template></el-table-column>
      <el-table-column label="操作" align="center" width="150"><template #default="s"><el-button link type="primary" icon="Edit" @click="handleUpdate(s.row)" v-hasPermi="['mes:md:workstation:edit']">修改</el-button><el-button link type="primary" icon="Delete" @click="handleDelete(s.row)" v-hasPermi="['mes:md:workstation:remove']">删除</el-button></template></el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
    <el-dialog :title="title" v-model="open" width="750px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="12"><el-form-item label="编码" prop="workstationCode"><el-input v-model="form.workstationCode" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label-width="70" v-if="!form.workstationId">
              <el-switch v-model="autoGenFlag" active-color="#13ce66" @change="handleAutoGenChange" /><span style="margin-left:6px;font-size:12px;color:#13ce66">自动生成</span>
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="名称" prop="workstationName"><el-input v-model="form.workstationName" /></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="12"><el-form-item label="所属车间" prop="workshopId"><el-select v-model="form.workshopId" placeholder="请选择" style="width:100%"><el-option v-for="w in workshopOptions" :key="w.workshopId" :label="w.workshopName" :value="w.workshopId" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="类型"><el-select v-model="form.workstationType" style="width:100%"><el-option label="印刷机" value="PRINT" /><el-option label="全自动制袋机" value="BAG_AUTO" /><el-option label="半自动制袋机" value="BAG_SEMI" /><el-option label="其他" value="OTHER" /></el-select></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="12"><el-form-item label="工序类型"><el-select v-model="form.processType" style="width:100%"><el-option label="印刷" value="PRINT" /><el-option label="制袋" value="BAG_MAKE" /><el-option label="分切" value="SLITTING" /><el-option label="检验" value="INSPECT" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="产能(个/时)"><el-input-number v-model="form.capacity" :min="0" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="12"><el-form-item label="状态"><el-select v-model="form.status" style="width:100%"><el-option label="空闲" value="IDLE" /><el-option label="运行中" value="RUNNING" /><el-option label="保养中" value="MAINTENANCE" /><el-option label="故障" value="BREAKDOWN" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="启用"><el-radio-group v-model="form.enableFlag"><el-radio value="1">是</el-radio><el-radio value="0">否</el-radio></el-radio-group></el-form-item></el-col>
        </el-row>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>

        <!-- 设备和人员子表 (编辑时显示) -->
        <template v-if="form.workstationId">
          <el-divider content-position="left">关联设备</el-divider>
          <el-table :data="machineList" size="small"><el-table-column label="设备编码" prop="machineryCode" /><el-table-column label="设备名称" prop="machineryName" /><el-table-column label="操作" width="80"><template #default="s"><el-button link type="danger" icon="Delete" @click="handleDelMachine(s.row)" /></template></el-table-column></el-table>
          <el-button type="primary" size="small" icon="Plus" @click="handleAddMachine" style="margin-top:5px">添加设备</el-button>

          <el-divider content-position="left">操作人员</el-divider>
          <el-table :data="workerList" size="small"><el-table-column label="用户名" prop="userName" /><el-table-column label="角色" prop="roleType" /><el-table-column label="操作" width="80"><template #default="s"><el-button link type="danger" icon="Delete" @click="handleDelWorker(s.row)" /></template></el-table-column></el-table>
          <el-button type="primary" size="small" icon="Plus" @click="handleAddWorker" style="margin-top:5px">添加人员</el-button>
        </template>
      </el-form>
      <template #footer><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></template>
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

const { proxy } = getCurrentInstance() as any; const { sys_yes_no } = useDict('sys_yes_no')
const list = ref<MdWorkstation[]>([]); const open = ref(false); const loading = ref(true); const showSearch = ref(true)
const ids = ref<number[]>([]); const single = ref(true); const multiple = ref(true); const total = ref(0); const title = ref('')
const autoGenFlag = ref(false)
const workshopOptions = ref<any[]>([]); const machineList = ref<MdWorkstationMachine[]>([]); const workerList = ref<MdWorkstationWorker[]>([])
const data = reactive({ form: { enableFlag: '1', status: 'IDLE' } as MdWorkstation, queryParams: { pageNum: 1, pageSize: 10 } as WorkstationQueryParams, rules: { workstationCode: [{ required: true, message: '编码不能为空' }], workstationName: [{ required: true, message: '名称不能为空' }], workshopId: [{ required: true, message: '请选择车间' }] } })
const { queryParams, form, rules } = toRefs(data)

function loadSubData(id: number) { listMachines(id).then(r => { machineList.value = r.data || [] }); listWorkers(id).then(r => { workerList.value = r.data || [] }) }
function getList() { loading.value = true; listWorkstation(queryParams.value).then(r => { list.value = r.rows; total.value = r.total; loading.value = false }) }
function cancel() { open.value = false; reset() }
function handleAutoGenChange(flag: boolean) {
  if (flag) genSerialCode('WORKSTATION_CODE').then((r: any) => { form.value.workstationCode = r.data })
  else form.value.workstationCode = ''
}
function reset() { autoGenFlag.value = false; form.value = { enableFlag: '1', status: 'IDLE' } as MdWorkstation; machineList.value = []; workerList.value = []; proxy.resetForm('formRef') }
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleSelectionChange(s: MdWorkstation[]) { ids.value = s.map(i => i.workstationId!); single.value = s.length !== 1; multiple.value = !s.length }
function handleAdd() { reset(); listAllWorkshop().then(r => { workshopOptions.value = r.data || [] }); open.value = true; title.value = '新增工作站' }
function handleUpdate(row?: MdWorkstation) { reset(); listAllWorkshop().then(r => { workshopOptions.value = r.data || [] }); const id = row?.workstationId || ids.value[0]; getWorkstation(id).then(r => { form.value = r.data; loadSubData(id); open.value = true; title.value = '修改工作站' }) }
function submitForm() { proxy.$refs['formRef'].validate((v: boolean) => { if (v) { (form.value.workstationId ? updateWorkstation(form.value) : addWorkstation(form.value)).then(() => { proxy.$modal.msgSuccess('操作成功'); open.value = false; getList() }) } }) }
function handleDelete(row?: MdWorkstation) { const ids_ = row?.workstationId || ids.value; proxy.$modal.confirm('确认删除？').then(() => delWorkstation(ids_)).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') }).catch(() => {}) }
function handleExport() { proxy.download('mes/md/workstation/export', { ...queryParams.value }, `workstation_${Date.now()}.xlsx`) }

function handleAddMachine() { proxy.$prompt('请输入设备编码', '添加设备').then(({ value: code }: any) => { proxy.$prompt('请输入设备名称', '添加设备').then(({ value: name }: any) => { const m = { workstationId: form.value.workstationId, machineryCode: code, machineryName: name } as MdWorkstationMachine; addMachine(m).then(() => { proxy.$modal.msgSuccess('添加成功'); loadSubData(form.value.workstationId!) }) }) }) }
function handleDelMachine(row: MdWorkstationMachine) { delMachine(row.recordId!).then(() => { proxy.$modal.msgSuccess('删除成功'); loadSubData(form.value.workstationId!) }) }
function handleAddWorker() { proxy.$prompt('请输入用户名', '添加人员').then(({ value: name }: any) => { proxy.$prompt('请输入角色(OPERATOR/SETUP/INSPECTOR)', '添加人员', { inputValue: 'OPERATOR' }).then(({ value: role }: any) => { const w = { workstationId: form.value.workstationId, userName: name, roleType: role } as MdWorkstationWorker; addWorker(w).then(() => { proxy.$modal.msgSuccess('添加成功'); loadSubData(form.value.workstationId!) }) }) }) }
function handleDelWorker(row: MdWorkstationWorker) { delWorker(row.recordId!).then(() => { proxy.$modal.msgSuccess('删除成功'); loadSubData(form.value.workstationId!) }) }

getList()
</script>

<style scoped>
:deep(.el-form-item__label) { padding-right: 16px !important; }
</style>
