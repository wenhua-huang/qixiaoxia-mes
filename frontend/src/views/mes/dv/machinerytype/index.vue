<template>
  <div class="app-container">
    <!-- 搜索 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="90px">
      <el-form-item label="类型名称" prop="machineryTypeName">
        <el-input v-model="queryParams.machineryTypeName" placeholder="请输入类型名称" clearable style="width:200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="是否启用" prop="enableFlag">
        <el-select v-model="queryParams.enableFlag" placeholder="选择" clearable style="width:200px">
          <el-option label="是" value="1" /><el-option label="否" value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 工具栏 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="info" plain icon="Sort" @click="toggleExpandAll">展开/折叠</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 树形表格 -->
    <el-table
      v-if="refreshTable"
      v-loading="loading"
      :data="typeList"
      row-key="machineryTypeId"
      :default-expand-all="isExpandAll"
      :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
    >
      <el-table-column prop="machineryTypeName" label="设备类型" width="260" />
      <el-table-column prop="orderNum" label="排序" width="100" align="center" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ scope.row.createTime }}</span>
        </template>
      </el-table-column>
      <el-table-column label="启用" align="center" width="70">
        <template #default="scope">
          <el-switch v-model="scope.row.enableFlag" active-value="1" inactive-value="0" @change="handleEnableChange(scope.row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="200" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:dv:machinerytype:edit']">修改</el-button>
          <el-button link type="primary" icon="Plus" @click="handleAdd(scope.row)" v-hasPermi="['mes:dv:machinerytype:add']">新增</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 对话框 -->
    <el-dialog :title="title" v-model="open" width="550px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item v-if="form.parentTypeId !== 0" label="上级类型" prop="parentTypeId">
          <el-tree-select v-model="form.parentTypeId" :data="typeTree" check-strictly
            :props="{ value: 'id', label: 'label', children: 'children' }"
            placeholder="请选择上级类型" disabled style="width:100%" />
        </el-form-item>
        <el-row>
          <el-col :span="12">
            <el-form-item label="类型编码" prop="machineryTypeCode">
              <el-input v-model="form.machineryTypeCode" placeholder="请输入类型编码" :disabled="optType === 'edit' || optType === 'view'" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="类型名称" prop="machineryTypeName">
              <el-input v-model="form.machineryTypeName" placeholder="请输入类型名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="排序" prop="orderNum">
              <el-input-number v-model="form.orderNum" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="启用状态">
              <el-radio-group v-model="form.enableFlag">
                <el-radio value="1">是</el-radio>
                <el-radio value="0">否</el-radio>
              </el-radio-group>
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

<script setup lang="ts" name="DvMachineryType">
import { ref, reactive, toRefs } from 'vue'
import { getCurrentInstance } from 'vue'
import type { DvMachineryType, MachineryTypeQueryParams } from '@/types/api/mes/dv/machinerytype'
import type { TreeSelect } from '@/types/api/common'
import { listMachinerytype, treeselect, listExcludeChild, getMachinerytype, delMachinerytype, addMachinerytype, updateMachinerytype } from '@/api/mes/dv/machinerytype'
import { handleTree } from '@/utils/ruoyi'

const { proxy } = getCurrentInstance() as any
const { sys_yes_no } = useDict('sys_yes_no')

const loading = ref(true)
const showSearch = ref(true)
const typeList = ref<DvMachineryType[]>([])
const typeTree = ref<TreeSelect[]>([])
const open = ref(false)
const title = ref('')
const isExpandAll = ref(true)
const refreshTable = ref(true)
const optType = ref<string | undefined>(undefined)

const data = reactive({
  form: {} as DvMachineryType,
  queryParams: { machineryTypeName: undefined, enableFlag: undefined } as MachineryTypeQueryParams,
  rules: {
    machineryTypeCode: [{ required: true, message: '类型编码不能为空', trigger: 'blur' }],
    machineryTypeName: [{ required: true, message: '类型名称不能为空', trigger: 'blur' }],
    orderNum: [{ required: true, message: '排序不能为空', trigger: 'blur' }]
  }
})
const { queryParams, form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listMachinerytype(queryParams.value).then(response => {
    typeList.value = handleTree(response.data || [], 'machineryTypeId', 'parentTypeId')
    loading.value = false
  })
}

function toggleExpandAll() {
  refreshTable.value = false
  isExpandAll.value = !isExpandAll.value
  proxy.$nextTick(() => { refreshTable.value = true })
}

function cancel() { open.value = false; reset() }

function reset() {
  optType.value = undefined
  form.value = {
    machineryTypeId: undefined, parentTypeId: undefined, machineryTypeCode: '',
    machineryTypeName: undefined, orderNum: 1, enableFlag: '1'
  }
  proxy.resetForm('formRef')
}

function handleQuery() { getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }

function handleAdd(row?: DvMachineryType) {
  reset()
  optType.value = 'add'
  if (row) {
    form.value.parentTypeId = row.machineryTypeId
  }
  open.value = true
  title.value = '添加设备类型'
  treeselect().then(r => { typeTree.value = r.data || [] })
}

function handleUpdate(row: DvMachineryType) {
  reset()
  optType.value = 'edit'
  getMachinerytype(row.machineryTypeId!).then(response => {
    form.value = response.data
    open.value = true
    title.value = '修改设备类型'
  })
  listExcludeChild(row.machineryTypeId!).then(response => {
    typeTree.value = handleTree(response.data || [], 'machineryTypeId', 'parentTypeId')
  })
}

function submitForm() {
  proxy.$refs['formRef'].validate((valid: boolean) => {
    if (valid) {
      if (form.value.machineryTypeId != undefined) {
        updateMachinerytype(form.value).then(() => { proxy.$modal.msgSuccess('修改成功'); open.value = false; getList() })
      } else {
        addMachinerytype(form.value).then(() => { proxy.$modal.msgSuccess('新增成功'); open.value = false; getList() })
      }
    }
  })
}

function handleEnableChange(row: any) {
  const newVal = row.enableFlag
  const text = newVal === '1' ? '启用' : '停用'
  proxy.$modal.confirm(`确认要${text}"${row.machineryTypeName}"吗？`).then(() => {
    updateMachinerytype({ machineryTypeId: row.machineryTypeId, enableFlag: newVal } as any).then(() => proxy.$modal.msgSuccess(`${text}成功`))
  }).catch(() => {
    row.enableFlag = newVal === '1' ? '0' : '1'
    getList()
  })
}

function handleDelete(row: DvMachineryType) {
  proxy.$modal.confirm('是否确认删除名称为"' + row.machineryTypeName + '"的设备类型？').then(() => delMachinerytype(row.machineryTypeId!)).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') }).catch(() => {})
}

getList()
</script>
