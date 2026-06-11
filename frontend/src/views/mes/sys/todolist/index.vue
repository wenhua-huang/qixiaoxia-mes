<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="待办标题" prop="todoTitle">
        <el-input v-model="queryParams.todoTitle" placeholder="请输入待办标题" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="待办类型" prop="todoType">
        <el-select v-model="queryParams.todoType" placeholder="待办类型" clearable style="width: 200px">
          <el-option label="审批" value="APPROVAL" /><el-option label="质检" value="QC_CHECK" />
          <el-option label="点检" value="DV_CHECK" /><el-option label="保养" value="MAINTEN" />
          <el-option label="维修" value="REPAIR" /><el-option label="其他" value="OTHER" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="状态" clearable style="width: 200px">
          <el-option label="待处理" value="PENDING" /><el-option label="处理中" value="PROCESSING" />
          <el-option label="已完成" value="COMPLETED" /><el-option label="已驳回" value="REJECTED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['mes:sys:todolist:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate()" v-hasPermi="['mes:sys:todolist:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Check" :disabled="single" @click="handleDo()" v-hasPermi="['mes:sys:todolist:edit']">处理</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()" v-hasPermi="['mes:sys:todolist:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="todoList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="待办标题" align="center" prop="todoTitle" show-overflow-tooltip />
      <el-table-column label="待办类型" align="center" prop="todoType" width="100">
        <template #default="scope">
          <el-tag>{{ scope.row.todoType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="优先级" align="center" prop="priority" width="80">
        <template #default="scope">
          <el-tag :type="priorityType(scope.row.priority)">{{ scope.row.priority }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template #default="scope">
          <el-tag :type="statusType(scope.row.status)">{{ scope.row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="单据编码" align="center" prop="sourceDocCode" width="140" />
      <el-table-column label="截止时间" align="center" prop="deadline" width="170" />
      <el-table-column label="操作" align="center" width="250">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:sys:todolist:edit']">修改</el-button>
          <el-button link type="primary" icon="Check" @click="handleDo(scope.row)" v-if="scope.row.status !== 'COMPLETED' && scope.row.status !== 'REJECTED'">处理</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mes:sys:todolist:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 新增/修改待办 -->
    <el-dialog :title="title" v-model="open" width="650px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="待办人ID" prop="userId">
          <el-input-number v-model="form.userId" :min="1" style="width: 200px" />
        </el-form-item>
        <el-form-item label="待办标题" prop="todoTitle">
          <el-input v-model="form.todoTitle" placeholder="请输入待办标题" />
        </el-form-item>
        <el-form-item label="待办类型" prop="todoType">
          <el-select v-model="form.todoType" placeholder="请选择" style="width: 100%">
            <el-option label="审批" value="APPROVAL" /><el-option label="质检" value="QC_CHECK" />
            <el-option label="点检" value="DV_CHECK" /><el-option label="保养" value="MAINTEN" />
            <el-option label="维修" value="REPAIR" /><el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-select v-model="form.priority" placeholder="请选择" style="width: 100%">
            <el-option label="紧急" value="URGENT" /><el-option label="高" value="HIGH" />
            <el-option label="普通" value="NORMAL" /><el-option label="低" value="LOW" />
          </el-select>
        </el-form-item>
        <el-form-item label="单据类型" prop="sourceDocType">
          <el-input v-model="form.sourceDocType" placeholder="如 pro_workorder" />
        </el-form-item>
        <el-form-item label="单据ID" prop="sourceDocId">
          <el-input-number v-model="form.sourceDocId" :min="0" style="width: 200px" />
        </el-form-item>
        <el-form-item label="单据编码" prop="sourceDocCode">
          <el-input v-model="form.sourceDocCode" placeholder="请输入单据编码" />
        </el-form-item>
        <el-form-item label="截止时间" prop="deadline">
          <el-date-picker v-model="form.deadline" type="datetime" placeholder="请选择" style="width: 100%" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
      </el-form>
      <template #footer><div class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div></template>
    </el-dialog>

    <!-- 处理待办 -->
    <el-dialog title="处理待办" v-model="handleOpen" width="500px" append-to-body>
      <el-form ref="handleFormRef" :model="handleForm" label-width="100px">
        <el-form-item label="待办标题">
          <el-input :model-value="handleForm.todoTitle" disabled />
        </el-form-item>
        <el-form-item label="处理状态" prop="status">
          <el-select v-model="handleForm.status" placeholder="请选择" style="width: 100%">
            <el-option label="处理中" value="PROCESSING" /><el-option label="已完成" value="COMPLETED" /><el-option label="已驳回" value="REJECTED" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理意见" prop="handleResult">
          <el-input v-model="handleForm.handleResult" type="textarea" :rows="3" placeholder="请输入处理结果或意见" />
        </el-form-item>
      </el-form>
      <template #footer><div class="dialog-footer"><el-button type="primary" @click="submitHandle">确 定</el-button><el-button @click="handleOpen = false">取 消</el-button></div></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="TodoList">
import { ref, reactive, toRefs } from 'vue'
import { getCurrentInstance } from 'vue'
import type { SysTodoList, TodoListQueryParams } from '@/types/api/mes/sys/todolist'
import { listTodoList, getTodoList, addTodoList, updateTodoList, delTodoList, handleTodo } from '@/api/mes/sys/todolist'

const { proxy } = getCurrentInstance() as any

const todoList = ref<SysTodoList[]>([])
const open = ref<boolean>(false)
const handleOpen = ref<boolean>(false)
const loading = ref<boolean>(true)
const showSearch = ref<boolean>(true)
const ids = ref<number[]>([])
const single = ref<boolean>(true)
const multiple = ref<boolean>(true)
const total = ref<number>(0)
const title = ref<string>('')

const data = reactive({
  form: {} as SysTodoList,
  handleForm: {} as SysTodoList,
  queryParams: { pageNum: 1, pageSize: 10 } as TodoListQueryParams,
  rules: {
    todoTitle: [{ required: true, message: '待办标题不能为空', trigger: 'blur' }],
    todoType: [{ required: true, message: '待办类型不能为空', trigger: 'change' }],
    userId: [{ required: true, message: '待办人ID不能为空', trigger: 'blur' }]
  }
})
const { queryParams, form, handleForm, rules } = toRefs(data)

function priorityType(p: string) { const m: Record<string,string> = { URGENT:'danger', HIGH:'warning', NORMAL:'info', LOW:'' }; return m[p] || '' }
function statusType(s: string) { const m: Record<string,string> = { PENDING:'warning', PROCESSING:'info', COMPLETED:'success', REJECTED:'danger' }; return m[s] || '' }

function getList() { loading.value = true; listTodoList(queryParams.value).then(r => { todoList.value = r.rows; total.value = r.total; loading.value = false }) }
function cancel() { open.value = false; reset() }
function reset() { form.value = { status: 'PENDING', priority: 'NORMAL' } as SysTodoList; proxy.resetForm('formRef') }
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleSelectionChange(s: SysTodoList[]) { ids.value = s.map(i => i.todoId!); single.value = s.length !== 1; multiple.value = !s.length }
function handleAdd() { reset(); open.value = true; title.value = '添加待办' }
function handleUpdate(row?: SysTodoList) { reset(); const id = row?.todoId || ids.value[0]; getTodoList(id).then(r => { form.value = r.data; open.value = true; title.value = '修改待办' }) }
function submitForm() {
  proxy.$refs['formRef'].validate((v: boolean) => {
    if (v) {
      if (form.value.todoId) updateTodoList(form.value).then(() => { proxy.$modal.msgSuccess('修改成功'); open.value = false; getList() })
      else addTodoList(form.value).then(() => { proxy.$modal.msgSuccess('新增成功'); open.value = false; getList() })
    }
  })
}
function handleDo(row?: SysTodoList) {
  handleForm.value = row ? { ...row } : { todoId: ids.value[0] } as SysTodoList
  handleForm.value.status = 'COMPLETED'
  handleOpen.value = true
}
function submitHandle() {
  handleTodo(handleForm.value.todoId!, handleForm.value).then(() => { proxy.$modal.msgSuccess('处理成功'); handleOpen.value = false; getList() })
}
function handleDelete(row?: SysTodoList) {
  proxy.$modal.confirm('是否确认删除该待办？').then(() => delTodoList(row?.todoId || ids.value)).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') }).catch(() => {})
}
getList()
</script>
