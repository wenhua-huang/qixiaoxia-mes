<template>
  <div class="changeover-page">
    <!-- 搜索栏 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="90px">
      <el-form-item label="换型名称" prop="name">
        <el-input v-model="queryParams.name" placeholder="换型名称" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="到工序" prop="toProcessId">
        <el-select v-model="queryParams.toProcessId" placeholder="选择工序" clearable filterable style="width:200px">
          <el-option v-for="p in processList" :key="p.processId" :label="p.processName" :value="p.processId" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery" :icon="Search">查询</el-button>
        <el-button @click="resetQuery" :icon="Refresh">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作栏 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain @click="handleAdd" :icon="Plus" v-hasPermi="['mes:pro:changeover:add']">新增</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="dataList" border stripe @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="换型名称" prop="name" min-width="180" show-overflow-tooltip />
      <el-table-column label="从工序" prop="fromProcessId" width="120" align="center">
        <template #default="{ row }">{{ row.fromProcessId ? getProcessName(row.fromProcessId) : '任意工序' }}</template>
      </el-table-column>
      <el-table-column label="到工序" prop="toProcessId" width="120" align="center">
        <template #default="{ row }">{{ getProcessName(row.toProcessId) }}</template>
      </el-table-column>
      <el-table-column label="工作站" prop="workstationId" width="140" align="center">
        <template #default="{ row }">{{ row.workstationId ? getWorkstationName(row.workstationId) : '通用' }}</template>
      </el-table-column>
      <el-table-column label="换型时长(分钟)" prop="durationMinutes" width="130" align="center" />
      <el-table-column label="备注" prop="remark" min-width="150" show-overflow-tooltip />
      <el-table-column label="操作" align="center" width="150" fixed="right">
        <template #default="{ row }">
          <el-tooltip content="修改" placement="top" v-hasPermi="['mes:pro:changeover:edit']">
            <el-button link type="primary" :icon="Edit" @click="handleUpdate(row)" />
          </el-tooltip>
          <el-tooltip content="删除" placement="top" v-hasPermi="['mes:pro:changeover:remove']">
            <el-button link type="danger" :icon="Delete" @click="handleDelete(row)" />
          </el-tooltip>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 新增/编辑弹窗 -->
    <el-dialog :title="dialogTitle" v-model="dialogOpen" width="550px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="换型名称" prop="name">
          <el-input v-model="form.name" placeholder="如：印刷→制袋换型" />
        </el-form-item>
        <el-form-item label="从工序" prop="fromProcessId">
          <el-select v-model="form.fromProcessId" placeholder="留空=任意工序" clearable filterable style="width:100%">
            <el-option v-for="p in processList" :key="p.processId" :label="p.processName" :value="p.processId" />
          </el-select>
        </el-form-item>
        <el-form-item label="到工序" prop="toProcessId">
          <el-select v-model="form.toProcessId" placeholder="必选" filterable style="width:100%">
            <el-option v-for="p in processList" :key="p.processId" :label="p.processName" :value="p.processId" />
          </el-select>
        </el-form-item>
        <el-form-item label="工作站" prop="workstationId">
          <el-select v-model="form.workstationId" placeholder="留空=通用换型" clearable filterable style="width:100%">
            <el-option v-for="ws in workstationList" :key="ws.workstationId" :label="ws.workstationName" :value="ws.workstationId" />
          </el-select>
        </el-form-item>
        <el-form-item label="换型时长(分钟)" prop="durationMinutes">
          <el-input-number v-model="form.durationMinutes" :min="0" :step="5" controls-position="right" style="width:100%" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogOpen = false">取 消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="ProChangeover">
import { ref, reactive, onMounted, getCurrentInstance } from 'vue'
import { Search, Refresh, Plus, Edit, Delete } from '@element-plus/icons-vue'
import { listChangeover, getChangeover, addChangeover, updateChangeover, delChangeover } from '@/api/mes/pro/changeover'
import { listProcess } from '@/api/mes/pro/process'
import { listWorkstation } from '@/api/mes/md/workstation'
import type { ProChangeover } from '@/types/api/mes/pro/changeover'

const { proxy } = getCurrentInstance() as any

const showSearch = ref(true)
const loading = ref(false)
const submitting = ref(false)
const ids = ref<number[]>([])
const total = ref(0)
const dataList = ref<ProChangeover[]>([])
const processList = ref<any[]>([])
const workstationList = ref<any[]>([])
const dialogOpen = ref(false)
const dialogTitle = ref('')

const queryParams = reactive<any>({
  pageNum: 1, pageSize: 10, name: undefined, toProcessId: undefined
})

const form = reactive<ProChangeover>({
  id: undefined, name: '', fromProcessId: undefined,
  toProcessId: undefined, workstationId: undefined, durationMinutes: 0
})

const rules = {
  toProcessId: [{ required: true, message: '请选择到工序', trigger: 'change' }],
  durationMinutes: [{ required: true, message: '请输入换型时长', trigger: 'blur' }]
}

onMounted(async () => {
  getList()
  const [pRes, wsRes] = await Promise.all([
    listProcess({} as any), listWorkstation({} as any)
  ])
  processList.value = (pRes as any).rows || []
  workstationList.value = (wsRes as any).rows || []
})

function getList() {
  loading.value = true
  listChangeover(queryParams).then((res: any) => {
    dataList.value = res.rows
    total.value = res.total
  }).finally(() => { loading.value = false })
}

function handleQuery() { queryParams.pageNum = 1; getList() }
function resetQuery() { Object.assign(queryParams, { pageNum: 1, name: undefined, toProcessId: undefined }); getList() }

function handleSelectionChange(selection: ProChangeover[]) { ids.value = selection.map(i => i.id!) }

function handleAdd() {
  Object.assign(form, { id: undefined, name: '', fromProcessId: undefined, toProcessId: undefined, workstationId: undefined, durationMinutes: 0, remark: '' })
  dialogTitle.value = '新增换型时间'
  dialogOpen.value = true
}

function handleUpdate(row: ProChangeover) {
  Object.assign(form, row)
  dialogTitle.value = '修改换型时间'
  dialogOpen.value = true
}

function handleDelete(row: ProChangeover) {
  proxy.$modal.confirm(`确认删除换型记录"${row.name || '未命名'}"?`).then(() => {
    return delChangeover(row.id!)
  }).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') })
}

function submitForm() {
  (proxy.$refs.formRef as any).validate((valid: boolean) => {
    if (!valid) return
    submitting.value = true
    const api = form.id ? updateChangeover(form) : addChangeover(form)
    api.then(() => { proxy.$modal.msgSuccess(form.id ? '修改成功' : '新增成功'); dialogOpen.value = false; getList() })
      .finally(() => { submitting.value = false })
  })
}

function getProcessName(id: number) {
  return processList.value.find((p: any) => p.processId === id)?.processName || id
}
function getWorkstationName(id: number) {
  return workstationList.value.find((w: any) => w.workstationId === id)?.workstationName || id
}
</script>

<style scoped lang="scss">
.changeover-page { padding: 8px; background: #fff; }
.mb8 { margin-bottom: 8px; }
</style>
