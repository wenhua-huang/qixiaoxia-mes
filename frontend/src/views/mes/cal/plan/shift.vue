<template>
  <div class="app-container">
    <el-row v-if="optType !== 'view'" :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="small"
          @click="handleAdd"
          v-hasPermi="['mes:cal:plan:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="small"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['mes:cal:plan:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="small"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['mes:cal:plan:remove']"
        >删除</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="shiftList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="序号" align="center" prop="shiftSeq" />
      <el-table-column label="班次名称" align="center" prop="shiftName" />
      <el-table-column label="开始时间" align="center" prop="startTime" width="180" />
      <el-table-column label="结束时间" align="center" prop="endTime" width="180" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="操作" align="center" v-if="optType !== 'view'" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button
            size="small"
            link
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['mes:cal:plan:edit']"
          >修改</el-button>
          <el-button
            size="small"
            link
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['mes:cal:plan:remove']"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:current-page="queryParams.pageNum"
      v-model:page-size="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加/修改班次对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="序号" prop="shiftSeq">
              <el-input-number :min="1" v-model="form.shiftSeq" placeholder="请输入序号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="班次名称" prop="shiftName">
              <el-input v-model="form.shiftName" placeholder="请输入班次名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="开始时间" prop="startTime">
              <el-time-picker
                v-model="form.startTime"
                placeholder="请选择开始时间"
                format="HH:mm"
                value-format="HH:mm:ss"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间" prop="endTime">
              <el-time-picker
                v-model="form.endTime"
                placeholder="请选择结束时间"
                format="HH:mm"
                value-format="HH:mm:ss"
              />
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

<script setup lang="ts">
import { ref, reactive, getCurrentInstance } from 'vue'
import { listShift, getShift, delShift, addShift, updateShift } from '@/api/mes/cal/shift'

const props = defineProps<{
  planId: number | null
  optType: string | undefined
}>()

const { proxy } = getCurrentInstance() as any

const loading = ref(true)
const ids = ref<number[]>([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const shiftList = ref<any[]>([])
const title = ref('')
const open = ref(false)
const formRef = ref()

const queryParams = reactive<any>({
  pageNum: 1,
  pageSize: 10,
  planId: props.planId,
  shiftSeq: null,
  shiftName: null,
  startTime: null,
  endTime: null,
})

const form = reactive<any>({
  shiftId: null,
  planId: props.planId,
  shiftSeq: null,
  shiftName: null,
  startTime: null,
  endTime: null,
  remark: null,
})

const rules = {
  shiftName: [{ required: true, message: '班次名称不能为空', trigger: 'blur' }],
  startTime: [{ required: true, message: '开始时间不能为空', trigger: 'blur' }],
  endTime: [{ required: true, message: '结束时间不能为空', trigger: 'blur' }],
}

function getList() {
  loading.value = true
  listShift(queryParams).then((response: any) => {
    shiftList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

function cancel() {
  open.value = false
  reset()
}

function reset() {
  form.shiftId = null
  form.planId = props.planId
  form.shiftSeq = null
  form.shiftName = null
  form.startTime = null
  form.endTime = null
  form.remark = null
  proxy.resetForm('formRef')
}

function handleSelectionChange(selection: any[]) {
  ids.value = selection.map((item) => item.shiftId)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

function handleAdd() {
  reset()
  open.value = true
  title.value = '添加班次'
}

function handleUpdate(row: any) {
  reset()
  const shiftId = row.shiftId || ids.value
  getShift(shiftId).then((response: any) => {
    Object.assign(form, response.data)
    open.value = true
    title.value = '修改班次'
  })
}

function submitForm() {
  formRef.value?.validate((valid: boolean) => {
    if (valid) {
      if (form.shiftId != null) {
        updateShift(form).then(() => {
          proxy.$modal.msgSuccess('修改成功')
          open.value = false
          getList()
        })
      } else {
        addShift(form).then(() => {
          proxy.$modal.msgSuccess('新增成功')
          open.value = false
          getList()
        })
      }
    }
  })
}

function handleDelete(row: any) {
  const shiftIds = row.shiftId || ids.value
  proxy.$modal.confirm('是否确认删除班次？').then(() => {
    return delShift(shiftIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

getList()
</script>
