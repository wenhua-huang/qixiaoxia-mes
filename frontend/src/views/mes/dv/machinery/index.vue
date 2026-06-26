<template>
  <div class="app-container">
    <el-row :gutter="20">
      <!-- 左侧设备类型树 -->
      <el-col :span="4" :xs="24">
        <div class="head-container">
          <el-input
            v-model="typeFilterText"
            placeholder="请输入类型名称"
            clearable
            size="small"
            prefix-icon="Search"
            style="margin-bottom:20px"
            @input="filterTypeNode"
          />
          <el-tree
            :data="typeTree"
            :props="{ children: 'children', label: 'label' }"
            :expand-on-click-node="false"
            :filter-node-method="filterTypeNodeFn"
            ref="treeRef"
            default-expand-all
            highlight-current
            @node-click="handleNodeClick"
          />
        </div>
      </el-col>

      <!-- 右侧设备列表 -->
      <el-col :span="20" :xs="24">
        <!-- 搜索 -->
        <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
          <el-form-item label="设备编码" prop="machineryCode">
            <el-input v-model="queryParams.machineryCode" placeholder="请输入设备编码" clearable style="width: 240px" @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="设备名称" prop="machineryName">
            <el-input v-model="queryParams.machineryName" placeholder="请输入设备名称" clearable style="width: 240px" @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <!-- 工具栏 -->
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['mes:dv:machinery:add']">新增</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate()" v-hasPermi="['mes:dv:machinery:edit']">修改</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()" v-hasPermi="['mes:dv:machinery:remove']">删除</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['mes:dv:machinery:export']">导出</el-button>
          </el-col>
          <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
        </el-row>

        <!-- 表格 -->
        <el-table v-loading="loading" :data="machineryList" @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="50" align="center" />
          <el-table-column label="设备编码" align="center" prop="machineryCode" width="140">
            <template #default="scope">
              <el-button link type="primary" @click="handleUpdate(scope.row)" v-hasPermi="['mes:dv:machinery:query']">{{ scope.row.machineryCode }}</el-button>
            </template>
          </el-table-column>
          <el-table-column label="设备名称" align="center" prop="machineryName" :show-overflow-tooltip="true" />
          <el-table-column label="品牌" align="center" prop="machineryBrand" :show-overflow-tooltip="true" />
          <el-table-column label="规格型号" align="center" prop="machinerySpec" :show-overflow-tooltip="true" width="180" />
          <el-table-column label="设备类型" align="center" prop="machineryTypeName" />
          <el-table-column label="所属车间" align="center" prop="workshopName" />
          <el-table-column label="设备状态" align="center" prop="status" width="100">
            <template #default="scope">
              <span><dict-tag :options="mes_machinery_status" :value="scope.row.status" /></span>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" align="center" prop="createTime" width="180">
            <template #default="scope">
              <span>{{ scope.row.createTime }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" width="150" class-name="small-padding fixed-width">
            <template #default="scope">
              <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:dv:machinery:edit']">修改</el-button>
              <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mes:dv:machinery:remove']">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
      </el-col>
    </el-row>

    <!-- 对话框 -->
    <el-dialog :title="title" v-model="open" width="650px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="设备编码" prop="machineryCode">
              <el-input v-model="form.machineryCode" placeholder="请输入设备编码" :disabled="optType === 'edit' || optType === 'view'" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备名称" prop="machineryName">
              <el-input v-model="form.machineryName" placeholder="请输入设备名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="设备品牌" prop="machineryBrand">
              <el-input v-model="form.machineryBrand" placeholder="请输入设备品牌" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备类型" prop="machineryTypeId">
              <el-tree-select v-model="form.machineryTypeId" :data="typeTree" check-strictly
                :props="{ value: 'id', label: 'label', children: 'children' }"
                placeholder="请选择设备类型" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="所属车间" prop="workshopId">
              <el-select v-model="form.workshopId" placeholder="请选择所属车间" style="width:100%">
                <el-option v-for="w in workshopList" :key="w.workshopId" :label="w.workshopName" :value="w.workshopId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备状态">
              <el-select v-model="form.status" placeholder="请选择设备状态" style="width:100%">
                <el-option v-for="d in mes_machinery_status" :key="d.value" :label="d.label" :value="d.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="规格型号" prop="machinerySpec">
          <el-input v-model="form.machinerySpec" type="textarea" placeholder="请输入规格型号" />
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

<script setup lang="ts" name="DvMachinery">
import { ref, reactive, toRefs } from 'vue'
import { getCurrentInstance } from 'vue'
import type { DvMachinery, MachineryQueryParams } from '@/types/api/mes/dv/machinery'
import type { TreeSelect } from '@/types/api/common'
import { listMachinery, getMachinery, delMachinery, addMachinery, updateMachinery } from '@/api/mes/dv/machinery'
import { treeselect } from '@/api/mes/dv/machinerytype'
import { listAllWorkshop } from '@/api/mes/md/workshop'

const { proxy } = getCurrentInstance() as any

const { mes_machinery_status } = useDict("mes_machinery_status")

const machineryList = ref<DvMachinery[]>([])
const typeTree = ref<TreeSelect[]>([])
const workshopList = ref<any[]>([])
const open = ref<boolean>(false)
const loading = ref<boolean>(true)
const showSearch = ref<boolean>(true)
const ids = ref<number[]>([])
const single = ref<boolean>(true)
const multiple = ref<boolean>(true)
const total = ref<number>(0)
const title = ref<string>('')
const typeFilterText = ref<string>('')
const optType = ref<string | undefined>(undefined)

const data = reactive({
  form: {} as DvMachinery,
  queryParams: {
    pageNum: 1, pageSize: 10,
    machineryCode: undefined, machineryName: undefined, machineryTypeId: undefined
  } as MachineryQueryParams,
  rules: {
    machineryCode: [
      { required: true, message: '设备编码不能为空', trigger: 'blur' },
      { max: 64, message: '设备编码长度不能超过64个字符', trigger: 'blur' }
    ],
    machineryName: [
      { required: true, message: '设备名称不能为空', trigger: 'blur' },
      { max: 255, message: '设备名称长度不能超过255个字符', trigger: 'blur' }
    ],
    machineryTypeId: [{ required: true, message: '设备类型不能为空', trigger: 'blur' }],
    workshopId: [{ required: true, message: '所属车间不能为空', trigger: 'blur' }]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 左侧设备类型树过滤 */
function filterTypeNode(val: string) {
  ;(proxy.$refs['treeRef'] as any)?.filter(val)
}
function filterTypeNodeFn(value: string, data: any) {
  if (!value) return true
  return data.label?.indexOf(value) !== -1
}

/** 点击树节点 → 按类型（含子类型）过滤设备 */
function handleNodeClick(data: any) {
  if (data.disabled) return // 禁用的分类不触发查询
  queryParams.value.machineryTypeId = data.id
  handleQuery()
}

function getList() {
  loading.value = true
  listMachinery(queryParams.value).then(response => {
    machineryList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

function loadSelectOptions() {
  Promise.all([treeselect(), listAllWorkshop()]).then(([treeRes, wsRes]) => {
    typeTree.value = treeRes.data || []
    workshopList.value = wsRes.data || []
  })
}

function cancel() { open.value = false; reset() }

function reset() {
  optType.value = undefined
  form.value = {
    machineryId: undefined, factoryId: undefined,
    machineryCode: undefined, machineryName: undefined, machineryBrand: undefined,
    machinerySpec: undefined, machineryTypeId: undefined,
    workshopId: undefined, status: 'IDLE', enableFlag: '1', remark: undefined
  }
  proxy.resetForm('formRef')
}

function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() {
  queryParams.value.machineryTypeId = undefined
  typeFilterText.value = ''
  ;(proxy.$refs['treeRef'] as any)?.setCurrentKey(null)
  ;(proxy.$refs['treeRef'] as any)?.filter('')
  proxy.resetForm('queryRef')
  handleQuery()
}

function handleSelectionChange(selection: DvMachinery[]) {
  ids.value = selection.map(item => item.machineryId!)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

function handleAdd() {
  reset()
  optType.value = 'add'
  // 并行加载下拉数据，完成后再打开对话框，避免空选择器闪烁
  Promise.all([treeselect(), listAllWorkshop()]).then(([treeRes, wsRes]) => {
    typeTree.value = treeRes.data || []
    workshopList.value = wsRes.data || []
    open.value = true
    title.value = '添加设备'
  })
}

function handleUpdate(row?: DvMachinery) {
  reset()
  optType.value = 'edit'
  const machineryId = row?.machineryId || ids.value[0]
  // 并行加载下拉数据 + 设备详情，全部完成后再打开对话框
  Promise.all([
    treeselect(),
    listAllWorkshop(),
    getMachinery(machineryId)
  ]).then(([treeRes, wsRes, mchRes]) => {
    typeTree.value = treeRes.data || []
    workshopList.value = wsRes.data || []
    form.value = mchRes.data
    open.value = true
    title.value = '修改设备'
  })
}

function submitForm() {
  proxy.$refs['formRef'].validate((valid: boolean) => {
    if (valid) {
      if (form.value.machineryId != undefined) {
        updateMachinery(form.value).then(() => { proxy.$modal.msgSuccess('修改成功'); open.value = false; getList() })
      } else {
        addMachinery(form.value).then(() => { proxy.$modal.msgSuccess('新增成功'); open.value = false; getList() })
      }
    }
  })
}

function handleDelete(row?: DvMachinery) {
  const machineryIds = row?.machineryId ? [row.machineryId] : ids.value
  const name = row?.machineryName || '所选'
  proxy.$modal.confirm('是否确认删除名称为"' + name + '"的设备？').then(() => delMachinery(machineryIds)).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') }).catch(() => {})
}

function handleExport() {
  proxy.download('/mes/dv/machinery/export', { ...queryParams.value }, '设备台账数据.xlsx')
}

loadSelectOptions()
getList()
</script>
