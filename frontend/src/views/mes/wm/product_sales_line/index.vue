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
      <el-col :span="1.5"><el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['mes:wm:product_sales_line:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate()" v-hasPermi="['mes:wm:product_sales_line:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()" v-hasPermi="['mes:wm:product_sales_line:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['mes:wm:product_sales_line:export']">导出</el-button></el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="productsaleslineList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="行ID" align="center" prop="lineId" width="100" :show-overflow-tooltip="true" />
      <el-table-column label="物料编码" align="center" prop="itemCode" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="物料名称" align="center" prop="itemName" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="单位名称" align="center" prop="unitName" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="批次编码" align="center" prop="batchCode" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="160">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:wm:product_sales_line:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mes:wm:product_sales_line:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="工厂ID" prop="factoryId">
          <el-input v-model="form.factoryId" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="出库单ID" prop="salesId">
          <el-input v-model="form.salesId" placeholder="请输入" clearable />
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
        <el-form-item label="出库数量" prop="quantitySales">
          <el-input v-model="form.quantitySales" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="出库箱数" prop="quantityBox">
          <el-input v-model="form.quantityBox" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="装箱规格" prop="boxSpec">
          <el-input v-model="form.boxSpec" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="箱长" prop="boxLength">
          <el-input v-model="form.boxLength" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="箱宽" prop="boxWidth">
          <el-input v-model="form.boxWidth" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="箱高" prop="boxHeight">
          <el-input v-model="form.boxHeight" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="体积" prop="volume">
          <el-input v-model="form.volume" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="重量" prop="weight">
          <el-input v-model="form.weight" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="批次ID" prop="batchId">
          <el-input v-model="form.batchId" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="批次编码" prop="batchCode">
          <el-input v-model="form.batchCode" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="仓库ID" prop="warehouseId">
          <el-input v-model="form.warehouseId" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="库区ID" prop="locationId">
          <el-input v-model="form.locationId" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="库位ID" prop="areaId">
          <el-input v-model="form.areaId" placeholder="请输入" clearable />
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

<script setup lang="ts" name="WmProductSalesLine">
import { ref, reactive, toRefs, getCurrentInstance } from 'vue'
import type { WmProductSalesLineQueryParams, WmProductSalesLine } from '@/types/api/mes/wm/product_sales_line'
import { listWmProductSalesLine, getWmProductSalesLine, delWmProductSalesLine, addWmProductSalesLine, updateWmProductSalesLine } from '@/api/mes/wm/product_sales_line'

const { proxy } = getCurrentInstance() as any
const dicts = proxy.useDict('sys_yes_no')

const productsaleslineList = ref<WmProductSalesLine[]>([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref<number[]>([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref('')

const data = reactive({
  form: {} as WmProductSalesLine,
  queryParams: { pageNum: 1, pageSize: 10 } as WmProductSalesLineQueryParams,
  rules: {}
})
const { queryParams, form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listWmProductSalesLine(queryParams.value).then(r => {
    productsaleslineList.value = r.rows
    total.value = r.total
    loading.value = false
  })
}

function cancel() { open.value = false; reset() }
function reset() { form.value = {} as WmProductSalesLine; proxy.resetForm('formRef') }
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleSelectionChange(s: any[]) { ids.value = s.map(i => i.lineId); single.value = s.length !== 1; multiple.value = !s.length }
function handleAdd() { reset(); open.value = true; title.value = '新增销售出库单行表' }
function handleUpdate(row?: WmProductSalesLine) {
  reset()
  const _id = row?.lineId || ids.value[0]
  getWmProductSalesLine(_id).then(r => { form.value = r.data; open.value = true; title.value = '修改销售出库单行表' })
}
function submitForm() {
  proxy.$refs['formRef'].validate((v: boolean) => {
    if (v) {
      const fn = form.value.lineId ? updateWmProductSalesLine : addWmProductSalesLine
      fn(form.value).then(() => { proxy.$modal.msgSuccess('操作成功'); open.value = false; getList() })
    }
  })
}
function handleDelete(row?: WmProductSalesLine) {
  const _ids = row?.lineId ? [row.lineId] : ids.value
  proxy.$modal.confirm('是否确认删除？').then(() => delWmProductSalesLine(_ids)).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') })
}
function handleExport() { proxy.download('/mes/wm/product_sales_line/export', { ...queryParams.value }, `productsalesline_${Date.now()}.xlsx`) }

getList()
</script>