<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="来源单据编码" prop="sourceDocCode">
        <el-input v-model="queryParams.sourceDocCode" placeholder="请输入" clearable style="width:180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="物料编码" prop="itemCode">
        <el-input v-model="queryParams.itemCode" placeholder="请输入" clearable style="width:180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="物料名称" prop="itemName">
        <el-input v-model="queryParams.itemName" placeholder="请输入" clearable style="width:180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['mes:wm:transaction:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate()" v-hasPermi="['mes:wm:transaction:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()" v-hasPermi="['mes:wm:transaction:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['mes:wm:transaction:export']">导出</el-button></el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="transactionList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="事务ID" align="center" prop="transactionId" width="100" :show-overflow-tooltip="true" />
      <el-table-column label="来源单据编码" align="center" prop="sourceDocCode" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="物料编码" align="center" prop="itemCode" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="物料名称" align="center" prop="itemName" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="单位名称" align="center" prop="unitName" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="辅助单位名称" align="center" prop="unit2Name" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="160">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:wm:transaction:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mes:wm:transaction:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="工厂ID" prop="factoryId">
          <el-input v-model="form.factoryId" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="事务类型:RECEIPT-入库,ISSUE-出库,TRANSFER-调拨,RETURN-退货,ADJUST-调整,SPLIT-分切" prop="transactionType">
          <el-input v-model="form.transactionType" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="来源单据类型" prop="sourceDocType">
          <el-input v-model="form.sourceDocType" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="来源单据ID" prop="sourceDocId">
          <el-input v-model="form.sourceDocId" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="来源单据编码" prop="sourceDocCode">
          <el-input v-model="form.sourceDocCode" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="来源单据行ID" prop="sourceLineId">
          <el-input v-model="form.sourceLineId" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="库存记录ID" prop="materialStockId">
          <el-input v-model="form.materialStockId" placeholder="请输入" clearable />
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
        <el-form-item label="变动数量" prop="quantity">
          <el-input v-model="form.quantity" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="辅助单位编码" prop="unit2">
          <el-input v-model="form.unit2" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="辅助单位名称" prop="unit2Name">
          <el-input v-model="form.unit2Name" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="变动数量" prop="quantity2">
          <el-input v-model="form.quantity2" placeholder="请输入" clearable />
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
        <el-form-item label="仓库编码" prop="warehouseCode">
          <el-input v-model="form.warehouseCode" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="仓库名称" prop="warehouseName">
          <el-input v-model="form.warehouseName" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="库区ID" prop="locationId">
          <el-input v-model="form.locationId" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="库位ID" prop="areaId">
          <el-input v-model="form.areaId" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="生产工单ID" prop="workorderId">
          <el-input v-model="form.workorderId" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="生产工单编码" prop="workorderCode">
          <el-input v-model="form.workorderCode" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="供应商ID" prop="vendorId">
          <el-input v-model="form.vendorId" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="客户ID" prop="clientId">
          <el-input v-model="form.clientId" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="关联事务ID" prop="relatedTransactionId">
          <el-input v-model="form.relatedTransactionId" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="事务时间" prop="transactionTime">
          <el-date-picker v-model="form.transactionTime" type="datetime" placeholder="选择日期" clearable style="width:100%" value-format="YYYY-MM-DD HH:mm:ss" />
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

<script setup lang="ts" name="WmTransaction">
import { ref, reactive, toRefs, getCurrentInstance } from 'vue'
import type { WmTransactionQueryParams, WmTransaction } from '@/types/api/mes/wm/transaction'
import { listWmTransaction, getWmTransaction, delWmTransaction, addWmTransaction, updateWmTransaction } from '@/api/mes/wm/transaction'

const { proxy } = getCurrentInstance() as any
const dicts = proxy.useDict('sys_yes_no')

const transactionList = ref<WmTransaction[]>([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref<number[]>([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref('')

const data = reactive({
  form: {} as WmTransaction,
  queryParams: { pageNum: 1, pageSize: 10 } as WmTransactionQueryParams,
  rules: {}
})
const { queryParams, form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listWmTransaction(queryParams.value).then(r => {
    transactionList.value = r.rows
    total.value = r.total
    loading.value = false
  })
}

function cancel() { open.value = false; reset() }
function reset() { form.value = {} as WmTransaction; proxy.resetForm('formRef') }
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleSelectionChange(s: any[]) { ids.value = s.map(i => i.transactionId); single.value = s.length !== 1; multiple.value = !s.length }
function handleAdd() { reset(); open.value = true; title.value = '新增库存事务表' }
function handleUpdate(row?: WmTransaction) {
  reset()
  const _id = row?.transactionId || ids.value[0]
  getWmTransaction(_id).then(r => { form.value = r.data; open.value = true; title.value = '修改库存事务表' })
}
function submitForm() {
  proxy.$refs['formRef'].validate((v: boolean) => {
    if (v) {
      const fn = form.value.transactionId ? updateWmTransaction : addWmTransaction
      fn(form.value).then(() => { proxy.$modal.msgSuccess('操作成功'); open.value = false; getList() })
    }
  })
}
function handleDelete(row?: WmTransaction) {
  const _ids = row?.transactionId ? [row.transactionId] : ids.value
  proxy.$modal.confirm('是否确认删除？').then(() => delWmTransaction(_ids)).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') })
}
function handleExport() { proxy.download('/mes/wm/transaction/export', { ...queryParams.value }, `transaction_${Date.now()}.xlsx`) }

getList()
</script>