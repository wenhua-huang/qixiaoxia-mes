<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="90px">
      <el-form-item label="用户名称" prop="userName">
        <el-input v-model="queryParams.userName" placeholder="请输入" clearable style="width:180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="工位ID" prop="workstationId">
        <el-input v-model="queryParams.workstationId" placeholder="请输入" clearable style="width:150px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="任务ID" prop="taskId">
        <el-input v-model="queryParams.taskId" placeholder="请输入" clearable style="width:150px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="操作标记" prop="operationFlag">
        <el-select v-model="queryParams.operationFlag" placeholder="请选择" clearable style="width:140px">
          <el-option label="上工" value="ON" />
          <el-option label="下工" value="OFF" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['mes:pro:workrecord:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate()" v-hasPermi="['mes:pro:workrecord:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()" v-hasPermi="['mes:pro:workrecord:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['mes:pro:workrecord:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="recordList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="用户名称" align="center" prop="userName" width="120" :show-overflow-tooltip="true" />
      <el-table-column label="用户昵称" align="center" prop="nickName" width="120" :show-overflow-tooltip="true" />
      <el-table-column label="工位" align="center" prop="workstationName" width="140" :show-overflow-tooltip="true" />
      <el-table-column label="任务编码" align="center" prop="taskCode" width="150" :show-overflow-tooltip="true" />
      <el-table-column label="操作标记" align="center" prop="operationFlag" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.operationFlag === 'ON' ? 'success' : 'danger'" size="small">
            {{ scope.row.operationFlag === 'ON' ? '上工' : '下工' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作时间" align="center" prop="operationTime" width="160">
        <template #default="scope">
          <span>{{ parseTime(scope.row.operationTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" width="160" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="160">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:pro:workrecord:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mes:pro:workrecord:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="650px" append-to-body @close="cancel">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="用户ID" prop="userId">
              <el-input-number v-model="form.userId" placeholder="请输入" style="width: 100%" :min="1" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用户名称" prop="userName">
              <el-input v-model="form.userName" placeholder="请输入" maxlength="64" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="工位ID" prop="workstationId">
              <el-input-number v-model="form.workstationId" placeholder="请输入" style="width: 100%" :min="1" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="任务ID" prop="taskId">
              <el-input-number v-model="form.taskId" placeholder="请输入" style="width: 100%" :min="1" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="操作标记" prop="operationFlag">
              <el-radio-group v-model="form.operationFlag">
                <el-radio value="ON">上工</el-radio>
                <el-radio value="OFF">下工</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="操作时间" prop="operationTime">
              <el-date-picker v-model="form.operationTime" type="datetime" placeholder="选择日期" clearable style="width: 100%" value-format="YYYY-MM-DD HH:mm:ss" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" :rows="3" maxlength="500" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="ProWorkrecord">
import { ref, reactive, toRefs, getCurrentInstance } from 'vue'
import type { WorkrecordQueryParams, Workrecord } from '@/types/api/mes/pro/workrecord'
import { listWorkrecord, getWorkrecord, delWorkrecord, addWorkrecord, updateWorkrecord } from '@/api/mes/pro/workrecord'
import { parseTime } from '@/utils/ruoyi'

const { proxy } = getCurrentInstance() as any

const recordList = ref<Workrecord[]>([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref<number[]>([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref('')

const data = reactive({
  form: {} as Workrecord,
  queryParams: { pageNum: 1, pageSize: 10 } as WorkrecordQueryParams,
  rules: {
    userId: [{ required: true, message: '用户ID不能为空', trigger: 'blur' }],
    userName: [{ required: true, message: '用户名称不能为空', trigger: 'blur' }],
    workstationId: [{ required: true, message: '工位ID不能为空', trigger: 'blur' }],
    taskId: [{ required: true, message: '任务ID不能为空', trigger: 'blur' }],
    operationFlag: [{ required: true, message: '操作标记不能为空', trigger: 'change' }],
    operationTime: [{ required: true, message: '操作时间不能为空', trigger: 'change' }]
  }
})
const { queryParams, form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listWorkrecord(queryParams.value).then(r => {
    recordList.value = r.rows
    total.value = r.total
    loading.value = false
  }).catch(() => { loading.value = false })
}

function cancel() {
  open.value = false
  reset()
}

function reset() {
  form.value = {} as Workrecord
  if (proxy.$refs['formRef']) proxy.resetForm('formRef')
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

function handleSelectionChange(selection: any[]) {
  ids.value = selection.map(i => i.recordId)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

function handleAdd() {
  reset()
  open.value = true
  title.value = '新增上下工记录'
}

function handleUpdate(row?: Workrecord) {
  reset()
  const id = row?.recordId || ids.value[0]
  getWorkrecord(id).then(r => {
    form.value = r.data
    open.value = true
    title.value = '修改上下工记录'
  })
}

function submitForm() {
  proxy.$refs['formRef'].validate((valid: boolean) => {
    if (!valid) return
    if (form.value.recordId != null) {
      updateWorkrecord(form.value).then(() => {
        proxy.$modal.msgSuccess('修改成功')
        open.value = false
        getList()
      })
    } else {
      addWorkrecord(form.value).then(() => {
        proxy.$modal.msgSuccess('新增成功')
        open.value = false
        getList()
      })
    }
  })
}

function handleDelete(row?: Workrecord) {
  const _ids = row?.recordId ? [row.recordId] : ids.value
  proxy.$modal.confirm('是否确认删除所选上下工记录？').then(() => delWorkrecord(_ids.join(','))).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

function handleExport() {
  proxy.download('/mes/pro/workrecord/export', { ...queryParams.value }, `workrecord_${Date.now()}.xlsx`)
}

getList()
</script>

<style lang="scss" scoped>
.app-container {
  padding: 16px;
}

.mb8 {
  margin-bottom: 8px;
}
</style>
