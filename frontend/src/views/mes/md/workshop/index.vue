<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="车间编码" prop="workshopCode">
        <el-input
          v-model="queryParams.workshopCode"
          placeholder="请输入车间编码"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="车间名称" prop="workshopName">
        <el-input
          v-model="queryParams.workshopName"
          placeholder="请输入车间名称"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="是否启用" prop="enableFlag">
        <el-select v-model="queryParams.enableFlag" placeholder="是否启用" clearable style="width: 200px">
          <el-option label="是" value="1" />
          <el-option label="否" value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['mes:md:workshop:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate()"
          v-hasPermi="['mes:md:workshop:edit']"
        >修改</el-button>
      </el-col>
      <!-- 删除按钮已移除：用启停用(enableFlag)替代物理删除 -->
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['mes:md:workshop:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="workshopList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="车间编码" align="center" prop="workshopCode" />
      <el-table-column label="车间名称" align="center" prop="workshopName" />
      <el-table-column label="车间地址" align="center" prop="address" :show-overflow-tooltip="true" />
      <el-table-column label="负责人" align="center" prop="manager" />
      <el-table-column label="是否启用" align="center" prop="enableFlag">
        <template #default="scope">
          <dict-tag :options="sys_yes_no" :value="scope.row.enableFlag" />
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" width="150" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:md:workshop:edit']">修改</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改车间对话框 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="16">
            <el-form-item label="车间编码" prop="workshopCode">
              <el-input v-model="form.workshopCode" placeholder="请输入车间编码" :disabled="optType === 'edit' || optType === 'view'" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label-width="70" v-if="optType === 'add'">
              <el-switch v-model="autoGenFlag" active-color="#13ce66" size="small" @change="handleAutoGenChange" /><span style="margin-left:6px;font-size:12px;color:#13ce66">自动生成</span>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="车间名称" prop="workshopName">
          <el-input v-model="form.workshopName" placeholder="请输入车间名称" />
        </el-form-item>
        <el-form-item label="车间地址" prop="address">
          <el-input v-model="form.address" placeholder="请输入车间地址" />
        </el-form-item>
        <el-form-item label="负责人" prop="manager">
          <el-input v-model="form.manager" placeholder="请输入负责人" />
        </el-form-item>
        <el-form-item label="是否启用" prop="enableFlag">
          <el-radio-group v-model="form.enableFlag">
            <el-radio value="1">是</el-radio>
            <el-radio value="0">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
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

<script setup lang="ts" name="Workshop">
import { ref, reactive, toRefs } from 'vue'
import { getCurrentInstance } from 'vue'
import { genSerialCode } from '@/api/mes/sys/autocoderule'
import type { MdWorkshop, WorkshopQueryParams } from '@/types/api/mes/md/workshop'
import { listWorkshop, getWorkshop, delWorkshop, addWorkshop, updateWorkshop } from '@/api/mes/md/workshop'

const { proxy } = getCurrentInstance() as any
const { sys_yes_no } = useDict('sys_yes_no')

const workshopList = ref<MdWorkshop[]>([])
const open = ref<boolean>(false)
const loading = ref<boolean>(true)
const showSearch = ref<boolean>(true)
const ids = ref<number[]>([])
const single = ref<boolean>(true)
const multiple = ref<boolean>(true)
const total = ref<number>(0)
const title = ref<string>('')
const autoGenFlag = ref(false)
const optType = ref<string | undefined>(undefined)

const data = reactive({
  form: {} as MdWorkshop,
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    workshopCode: undefined,
    workshopName: undefined,
    enableFlag: undefined
  } as WorkshopQueryParams,
  rules: {
    workshopCode: [
      { required: true, message: '车间编码不能为空', trigger: 'blur' },
      { max: 64, message: '车间编码长度不能超过64个字符', trigger: 'blur' }
    ],
    workshopName: [
      { required: true, message: '车间名称不能为空', trigger: 'blur' },
      { max: 255, message: '车间名称长度不能超过255个字符', trigger: 'blur' }
    ],
    enableFlag: [
      { required: true, message: '是否启用不能为空', trigger: 'blur' }
    ]
  }
})

const { queryParams, form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listWorkshop(queryParams.value).then(response => {
    workshopList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

function cancel() {
  open.value = false
  reset()
}

function handleAutoGenChange(flag: boolean) {
  if (flag) genSerialCode('WORKSHOP_CODE').then((r: any) => { form.value.workshopCode = r.data })
  else form.value.workshopCode = ''
}
function reset() {
  optType.value = undefined
  autoGenFlag.value = false
  form.value = {
    workshopId: undefined,
    factoryId: undefined,
    workshopCode: undefined,
    workshopName: undefined,
    address: undefined,
    manager: undefined,
    enableFlag: '1',
    remark: undefined
  }
  proxy.resetForm('formRef')
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

function handleSelectionChange(selection: MdWorkshop[]) {
  ids.value = selection.map(item => item.workshopId!)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

function handleAdd() {
  reset()
  optType.value = 'add'
  open.value = true
  title.value = '添加车间'
}

function handleUpdate(row?: MdWorkshop) {
  reset()
  optType.value = 'edit'
  const workshopId = row?.workshopId || ids.value[0]
  getWorkshop(workshopId).then(response => {
    form.value = response.data
    open.value = true
    title.value = '修改车间'
  })
}

function submitForm() {
  proxy.$refs['formRef'].validate((valid: boolean) => {
    if (valid) {
      if (form.value.workshopId != undefined) {
        updateWorkshop(form.value).then(() => {
          proxy.$modal.msgSuccess('修改成功')
          open.value = false
          getList()
        })
      } else {
        addWorkshop(form.value).then(() => {
          proxy.$modal.msgSuccess('新增成功')
          open.value = false
          getList()
        })
      }
    }
  })
}

function handleDelete(row?: MdWorkshop) {
  const workshopIds = row?.workshopId || ids.value
  proxy.$modal.confirm('是否确认删除车间编号为"' + workshopIds + '"的数据项？').then(function () {
    return delWorkshop(workshopIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

function handleExport() {
  proxy.download('mes/md/workshop/export', {
    ...queryParams.value
  }, `workshop_${new Date().getTime()}.xlsx`)
}

getList()
</script>

<style scoped>
:deep(.el-form-item__label) { padding-right: 16px !important; }
</style>
