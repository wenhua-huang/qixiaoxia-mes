<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="物料编码" prop="itemCode">
        <el-input v-model="queryParams.itemCode" placeholder="请输入" clearable style="width:180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="物料名称" prop="itemName">
        <el-input v-model="queryParams.itemName" placeholder="请输入" clearable style="width:180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="单位名称" prop="unitName">
        <el-input v-model="queryParams.unitName" placeholder="请输入" clearable style="width:180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['mes:wm:package_line:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate()" v-hasPermi="['mes:wm:package_line:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()" v-hasPermi="['mes:wm:package_line:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['mes:wm:package_line:export']">导出</el-button></el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="packagelineList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="行ID" align="center" prop="lineId" width="100" :show-overflow-tooltip="true" />
      <el-table-column label="物料编码" align="center" prop="itemCode" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="物料名称" align="center" prop="itemName" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="单位名称" align="center" prop="unitName" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="批次编码" align="center" prop="batchCode" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="条码内容" align="center" prop="barcodeCode" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="160">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:wm:package_line:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mes:wm:package_line:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="工厂ID" prop="factoryId">
          <el-input v-model="form.factoryId" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="装箱单ID" prop="packageId">
          <el-input v-model="form.packageId" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="物料ID" prop="itemId">
          <el-input v-model="form.itemId" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="物料编码" prop="itemCode">
          <el-input v-model="form.itemCode" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="物料名称" prop="itemName">
          <el-input v-model="form.itemName" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="规格型号" prop="specification">
          <el-input v-model="form.specification" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="单位编码" prop="unitOfMeasure">
          <el-input v-model="form.unitOfMeasure" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="单位名称" prop="unitName">
          <el-input v-model="form.unitName" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="装箱数量" prop="quantity">
          <el-input v-model="form.quantity" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="批次ID" prop="batchId">
          <el-input v-model="form.batchId" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="批次编码" prop="batchCode">
          <el-input v-model="form.batchCode" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="条码内容" prop="barcodeCode">
          <el-input v-model="form.barcodeCode" placeholder="请输入" clearable />
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

<script setup lang="ts" name="WmPackageLine">
import { ref, reactive, toRefs, getCurrentInstance } from 'vue'
import type { WmPackageLineQueryParams, WmPackageLine } from '@/types/api/mes/wm/package_line'
import { listWmPackageLine, getWmPackageLine, delWmPackageLine, addWmPackageLine, updateWmPackageLine } from '@/api/mes/wm/package_line'

const { proxy } = getCurrentInstance() as any
const dicts = proxy.useDict('sys_yes_no')

const packagelineList = ref<WmPackageLine[]>([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref<number[]>([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref('')

const data = reactive({
  form: {} as WmPackageLine,
  queryParams: { pageNum: 1, pageSize: 10 } as WmPackageLineQueryParams,
  rules: {}
})
const { queryParams, form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listWmPackageLine(queryParams.value).then(r => {
    packagelineList.value = r.rows
    total.value = r.total
    loading.value = false
  })
}

function cancel() { open.value = false; reset() }
function reset() { form.value = {} as WmPackageLine; proxy.resetForm('formRef') }
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleSelectionChange(s: any[]) { ids.value = s.map(i => i.lineId); single.value = s.length !== 1; multiple.value = !s.length }
function handleAdd() { reset(); open.value = true; title.value = '新增装箱明细表' }
function handleUpdate(row?: WmPackageLine) {
  reset()
  const _id = row?.lineId || ids.value[0]
  getWmPackageLine(_id).then(r => { form.value = r.data; open.value = true; title.value = '修改装箱明细表' })
}
function submitForm() {
  proxy.$refs['formRef'].validate((v: boolean) => {
    if (v) {
      const fn = form.value.lineId ? updateWmPackageLine : addWmPackageLine
      fn(form.value).then(() => { proxy.$modal.msgSuccess('操作成功'); open.value = false; getList() })
    }
  })
}
function handleDelete(row?: WmPackageLine) {
  const _ids = row?.lineId ? [row.lineId] : ids.value
  proxy.$modal.confirm('是否确认删除？').then(() => delWmPackageLine(_ids)).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') })
}
function handleExport() { proxy.download('/mes/wm/package_line/export', { ...queryParams.value }, `packageline_${Date.now()}.xlsx`) }

getList()
</script>