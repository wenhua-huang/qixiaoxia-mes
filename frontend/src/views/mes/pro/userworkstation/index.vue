<template>
  <div class="app-container">
    <!-- 搜索区域 -->
    <el-form :model="queryParams" ref="queryRef" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="用户ID" prop="userId">
        <el-input-number v-model="queryParams.userId" :min="1" style="width:180px" placeholder="请输入用户ID" controls-position="right" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="工作站ID" prop="workstationId">
        <el-input-number v-model="queryParams.workstationId" :min="1" style="width:180px" placeholder="请输入工作站ID" controls-position="right" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="enableFlag">
        <el-select v-model="queryParams.enableFlag" placeholder="请选择启用状态" clearable style="width:120px">
          <el-option v-for="d in dicts.sys_yes_no" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 工具栏 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" size="small" @click="handleAdd" v-hasPermi="['mes:pro:userworkstation:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" size="small" :disabled="single" @click="handleUpdate()" v-hasPermi="['mes:pro:userworkstation:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <!-- 删除按钮已移除：用启停用(enableFlag)替代物理删除 -->
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" size="small" @click="handleExport" v-hasPermi="['mes:pro:userworkstation:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="dataList" size="small" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="45" align="center" />
      <el-table-column label="用户ID" align="center" prop="userId" width="90" />
      <el-table-column label="用户名" align="center" prop="userName" width="120" :show-overflow-tooltip="true" />
      <el-table-column label="昵称" align="center" prop="nickName" width="130" :show-overflow-tooltip="true" />
      <el-table-column label="工作站ID" align="center" prop="workstationId" width="100" />
      <el-table-column label="工作站名称" align="center" prop="workstationName" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="是否启用" align="center" prop="enableFlag" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.enableFlag === '1' ? 'success' : 'info'" size="small">
            {{ scope.row.enableFlag === '1' ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="绑定时间" align="center" width="160">
        <template #default="scope">
          <span>{{ parseTime(scope.row.operationTime || scope.row.createTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" min-width="120" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" width="120" fixed="right" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" size="small" @click="handleView(scope.row)" v-hasPermi="['mes:pro:userworkstation:query']">查看</el-button>
          <el-button link type="primary" icon="Edit" size="small" @click="handleUpdate(scope.row)" v-hasPermi="['mes:pro:userworkstation:edit']">修改</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 新增/修改/查看弹窗 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body :close-on-click-modal="false" @close="cancel">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" :disabled="isView">
        <el-row>
          <el-col :span="12">
            <el-form-item label="用户ID" prop="userId">
              <el-input-number v-model="form.userId" :min="1" style="width:100%" placeholder="请输入用户ID" controls-position="right" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用户名" prop="userName">
              <el-input v-model="form.userName" placeholder="自动回填或手动输入" maxlength="64" show-word-limit />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="昵称" prop="nickName">
              <el-input v-model="form.nickName" placeholder="自动回填或手动输入" maxlength="64" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工作站ID" prop="workstationId">
              <el-input-number v-model="form.workstationId" :min="1" style="width:100%" placeholder="请输入工作站ID" controls-position="right" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="工作站名称" prop="workstationName">
              <el-input v-model="form.workstationName" placeholder="自动回填或手动输入" maxlength="64" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="绑定时间" prop="operationTime">
              <el-date-picker v-model="form.operationTime" type="datetime" placeholder="请选择绑定时间" style="width:100%" value-format="YYYY-MM-DD HH:mm:ss" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="是否启用" prop="enableFlag">
              <el-radio-group v-model="form.enableFlag">
                <el-radio v-for="d in dicts.sys_yes_no" :key="d.value" :label="d.value">{{ d.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注信息" maxlength="500" show-word-limit />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :loading="submitLoading" @click="submitForm" v-if="!isView">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="ProUserWorkstation">
import { ref, reactive, toRefs, getCurrentInstance, onMounted } from 'vue'
import type { UserWorkstation, UserWorkstationQueryParams } from '@/types/api/mes/pro/userworkstation'
import { listUserWorkstation, getUserWorkstation, addUserWorkstation, updateUserWorkstation, delUserWorkstation } from '@/api/mes/pro/userworkstation'

const { proxy } = getCurrentInstance() as any
const dicts = proxy.useDict('sys_yes_no')

// -------------------- 状态定义 --------------------
const loading = ref(true)
const submitLoading = ref(false)
const open = ref(false)
const showSearch = ref(true)
const isView = ref(false)
const title = ref('')
const ids = ref<number[]>([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const dataList = ref<UserWorkstation[]>([])

const data = reactive({
  form: { enableFlag: '1' } as UserWorkstation,
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    userId: undefined,
    workstationId: undefined,
    enableFlag: undefined
  } as UserWorkstationQueryParams,
  rules: {
    userId: [
      { required: true, message: '用户ID不能为空', trigger: 'blur' }
    ],
    workstationId: [
      { required: true, message: '工作站ID不能为空', trigger: 'blur' }
    ],
    enableFlag: [
      { required: true, message: '请选择启用状态', trigger: 'change' }
    ]
  }
})

const { queryParams, form, rules } = toRefs(data)

// -------------------- 生命周期 --------------------
onMounted(() => getList())

// -------------------- 列表查询 --------------------
function getList() {
  loading.value = true
  listUserWorkstation(queryParams.value)
    .then((r: any) => {
      dataList.value = r.rows
      total.value = r.total
    })
    .catch(() => { proxy.$modal.msgError('查询失败') })
    .finally(() => { loading.value = false })
}

// -------------------- 搜索 & 重置 --------------------
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

// -------------------- 行选 --------------------
function handleSelectionChange(selection: UserWorkstation[]) {
  ids.value = selection.map(item => item.recordId as number)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

// -------------------- 弹窗开关 --------------------
function cancel() {
  open.value = false
  reset()
}

function reset() {
  isView.value = false
  submitLoading.value = false
  form.value = { enableFlag: '1' } as UserWorkstation
  proxy.resetForm('formRef')
}

// -------------------- 新增 --------------------
function handleAdd() {
  reset()
  open.value = true
  title.value = '新增用户工作站绑定'
}

// -------------------- 查看 --------------------
function handleView(row: UserWorkstation) {
  reset()
  getUserWorkstation(row.recordId!).then((r: any) => {
    form.value = r.data
    open.value = true
    title.value = '查看用户工作站绑定'
    isView.value = true
  }).catch(() => { proxy.$modal.msgError('获取详情失败') })
}

// -------------------- 修改 --------------------
function handleUpdate(row?: UserWorkstation) {
  reset()
  const id = row?.recordId || ids.value[0]
  getUserWorkstation(id)
    .then((r: any) => {
      form.value = r.data
      open.value = true
      title.value = '修改用户工作站绑定'
    })
    .catch(() => { proxy.$modal.msgError('获取详情失败') })
}

// -------------------- 删除 --------------------
function handleDelete(row?: UserWorkstation) {
  const _ids = row?.recordId ? String(row.recordId) : ids.value.join(',')
  proxy.$modal
    .confirm('是否确认删除该绑定记录？删除后不可恢复。')
    .then(() => delUserWorkstation(_ids))
    .then(() => {
      getList()
      proxy.$modal.msgSuccess('删除成功')
    })
    .catch(() => {})
}

// -------------------- 导出 --------------------
function handleExport() {
  proxy.download('/mes/pro/userworkstation/export', { ...queryParams.value }, `用户工作站绑定_${new Date().getTime()}.xlsx`)
}

// -------------------- 提交保存 --------------------
function submitForm() {
  proxy.$refs['formRef'].validate((valid: boolean) => {
    if (!valid) return
    submitLoading.value = true
    const action = form.value.recordId
      ? updateUserWorkstation(form.value)
      : addUserWorkstation(form.value)
    action
      .then(() => {
        proxy.$modal.msgSuccess(form.value.recordId ? '修改成功' : '新增成功')
        open.value = false
        getList()
      })
      .catch(() => { proxy.$modal.msgError('保存失败') })
      .finally(() => { submitLoading.value = false })
  })
}
</script>

<style scoped>
:deep(.el-form-item__label) {
  padding-right: 16px !important;
}
</style>
