<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="盘点任务编码" prop="takingCode">
        <el-input v-model="queryParams.takingCode" placeholder="请输入" clearable style="width:180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="盘点方案编码" prop="planCode">
        <el-input v-model="queryParams.planCode" placeholder="请输入" clearable style="width:180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="物料编码" prop="itemCode">
        <el-input v-model="queryParams.itemCode" placeholder="请输入" clearable style="width:180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['mes:wm:stock_taking:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate()" v-hasPermi="['mes:wm:stock_taking:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()" v-hasPermi="['mes:wm:stock_taking:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['mes:wm:stock_taking:export']">导出</el-button></el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="stocktakingList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="盘点任务ID" align="center" prop="takingId" width="100" :show-overflow-tooltip="true" />
      <el-table-column label="盘点任务编码" align="center" prop="takingCode" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="盘点方案编码" align="center" prop="planCode" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="物料编码" align="center" prop="itemCode" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="物料名称" align="center" prop="itemName" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="单位名称" align="center" prop="unitName" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="160">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:wm:stock_taking:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mes:wm:stock_taking:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="工厂ID" prop="factoryId">
          <el-input v-model="form.factoryId" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="盘点任务编码" prop="takingCode">
          <el-input v-model="form.takingCode" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="盘点方案ID" prop="planId">
          <el-input v-model="form.planId" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="盘点方案编码" prop="planCode">
          <el-input v-model="form.planCode" placeholder="请输入" clearable />
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
        <el-form-item label="账面数量" prop="bookQuantity">
          <el-input v-model="form.bookQuantity" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="实际盘点数量" prop="actualQuantity">
          <el-input v-model="form.actualQuantity" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="差异数量" prop="difference">
          <el-input v-model="form.difference" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="差异原因" prop="diffReason">
          <el-input v-model="form.diffReason" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="盘点人" prop="takingUser">
          <el-input v-model="form.takingUser" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="盘点日期" prop="takingDate">
          <el-date-picker v-model="form.takingDate" type="datetime" placeholder="选择日期" clearable style="width:100%" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="状态:PENDING-待盘点,TAKEN-已盘点,ADJUSTED-已调整" prop="status">
          <el-input v-model="form.status" placeholder="请输入" clearable />
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

<script setup lang="ts" name="WmStockTaking">
import { ref, reactive, toRefs, getCurrentInstance } from 'vue'
import type { WmStockTakingQueryParams, WmStockTaking } from '@/types/api/mes/wm/stock_taking'
import { listWmStockTaking, getWmStockTaking, delWmStockTaking, addWmStockTaking, updateWmStockTaking } from '@/api/mes/wm/stock_taking'

const { proxy } = getCurrentInstance() as any
const dicts = proxy.useDict('sys_yes_no')

const stocktakingList = ref<WmStockTaking[]>([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref<number[]>([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref('')

const data = reactive({
  form: {} as WmStockTaking,
  queryParams: { pageNum: 1, pageSize: 10 } as WmStockTakingQueryParams,
  rules: {}
})
const { queryParams, form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listWmStockTaking(queryParams.value).then(r => {
    stocktakingList.value = r.rows
    total.value = r.total
    loading.value = false
  })
}

function cancel() { open.value = false; reset() }
function reset() { form.value = {} as WmStockTaking; proxy.resetForm('formRef') }
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleSelectionChange(s: any[]) { ids.value = s.map(i => i.takingId); single.value = s.length !== 1; multiple.value = !s.length }
function handleAdd() { reset(); open.value = true; title.value = '新增盘点任务表' }
function handleUpdate(row?: WmStockTaking) {
  reset()
  const _id = row?.takingId || ids.value[0]
  getWmStockTaking(_id).then(r => { form.value = r.data; open.value = true; title.value = '修改盘点任务表' })
}
function submitForm() {
  proxy.$refs['formRef'].validate((v: boolean) => {
    if (v) {
      const fn = form.value.takingId ? updateWmStockTaking : addWmStockTaking
      fn(form.value).then(() => { proxy.$modal.msgSuccess('操作成功'); open.value = false; getList() })
    }
  })
}
function handleDelete(row?: WmStockTaking) {
  const _ids = row?.takingId ? [row.takingId] : ids.value
  proxy.$modal.confirm('是否确认删除？').then(() => delWmStockTaking(_ids)).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') })
}
function handleExport() { proxy.download('/mes/wm/stock_taking/export', { ...queryParams.value }, `stocktaking_${Date.now()}.xlsx`) }

getList()
</script>