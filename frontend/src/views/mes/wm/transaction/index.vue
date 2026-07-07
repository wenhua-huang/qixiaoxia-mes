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
      <el-col :span="1.5"><el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['mes:wm:transaction:export']">导出</el-button></el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="transactionList">
      <el-table-column label="事务ID" align="center" prop="transactionId" width="100" :show-overflow-tooltip="true" />
      <el-table-column label="来源单据编码" align="center" prop="sourceDocCode" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="事务类型" align="center" prop="transactionType" width="100">
        <template #default="scope">
          <el-tag :type="transactionTypeTag[scope.row.transactionType] || 'info'" size="small">
            {{ transactionTypeMap[scope.row.transactionType] || scope.row.transactionType }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="物料编码" align="center" prop="itemCode" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="物料名称" align="center" prop="itemName" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="变动数量" align="center" prop="quantity" width="120">
        <template #default="scope">
          <span :style="{ color: scope.row.quantity > 0 ? '#67c23a' : scope.row.quantity < 0 ? '#f56c6c' : '#909399', fontWeight: 'bold' }">
            {{ scope.row.quantity > 0 ? '+' : '' }}{{ scope.row.quantity }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="单位名称" align="center" prop="unitName" width="100" :show-overflow-tooltip="true" />
      <el-table-column label="辅助单位名称" align="center" prop="unit2Name" width="120" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="100">
        <template #default="scope">
          <el-button link type="primary" icon="View" size="small" @click="handleDetail(scope.row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog title="库存事务详情" v-model="open" width="600px" append-to-body>
      <el-form ref="formRef" :model="form" label-width="100px" :disabled="true">
        <el-form-item label="工厂ID" prop="factoryId">
          <el-input v-model="form.factoryId" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="事务类型" prop="transactionType">
          <el-input v-model="form.transactionType" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="来源单据类型" prop="sourceDocType">
          <el-input v-model="form.sourceDocType" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="来源单据ID" prop="sourceDocId">
          <el-input v-model="form.sourceDocId" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="来源单据编码" prop="sourceDocCode">
          <el-input v-model="form.sourceDocCode" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="来源单据行ID" prop="sourceLineId">
          <el-input v-model="form.sourceLineId" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="库存记录ID" prop="materialStockId">
          <el-input v-model="form.materialStockId" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="物料ID" prop="itemId">
          <el-input v-model="form.itemId" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="物料编码" prop="itemCode">
          <el-input v-model="form.itemCode" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="物料名称" prop="itemName">
          <el-input v-model="form.itemName" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="规格型号" prop="specification">
          <el-input v-model="form.specification" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="单位编码" prop="unitOfMeasure">
          <el-input v-model="form.unitOfMeasure" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="单位名称" prop="unitName">
          <el-input v-model="form.unitName" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="变动数量" prop="quantity">
          <el-input v-model="form.quantity" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="辅助单位编码" prop="unit2">
          <el-input v-model="form.unit2" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="辅助单位名称" prop="unit2Name">
          <el-input v-model="form.unit2Name" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="辅助变动数量" prop="quantity2">
          <el-input v-model="form.quantity2" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="批次ID" prop="batchId">
          <el-input v-model="form.batchId" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="批次编码" prop="batchCode">
          <el-input v-model="form.batchCode" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="仓库ID" prop="warehouseId">
          <el-input v-model="form.warehouseId" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="仓库编码" prop="warehouseCode">
          <el-input v-model="form.warehouseCode" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="仓库名称" prop="warehouseName">
          <el-input v-model="form.warehouseName" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="库区ID" prop="locationId">
          <el-input v-model="form.locationId" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="库位ID" prop="areaId">
          <el-input v-model="form.areaId" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="生产工单ID" prop="workorderId">
          <el-input v-model="form.workorderId" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="生产工单编码" prop="workorderCode">
          <el-input v-model="form.workorderCode" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="供应商ID" prop="vendorId">
          <el-input v-model="form.vendorId" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="客户ID" prop="clientId">
          <el-input v-model="form.clientId" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="关联事务ID" prop="relatedTransactionId">
          <el-input v-model="form.relatedTransactionId" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="事务时间" prop="transactionTime">
          <el-date-picker v-model="form.transactionTime" type="datetime" placeholder="选择日期" style="width:100%" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="closeDetail">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="WmTransaction">
import { ref, reactive, toRefs, getCurrentInstance } from 'vue'
import type { WmTransactionQueryParams, WmTransaction } from '@/types/api/mes/wm/transaction'
import { listWmTransaction, getWmTransaction } from '@/api/mes/wm/transaction'

const { proxy } = getCurrentInstance() as any

const transactionTypeMap: Record<string, string> = {
  ITEM_RECPT: '物料入库', MISC_RECPT: '杂项入库', MISC_ISSUE: '杂项出库',
  ITEM_RTV: '供应商退货', PRODUCT_SALES: '销售出库', PRODUCT_RT: '销售退货',
  TRANS_OUT: '调拨出库', TRANS_IN: '调拨入库',
  ALLOCATE: '分配', RELEASE: '释放', ISSUE_OUT: '领料出库', RETURN_IN: '退货入库',
}
const transactionTypeTag: Record<string, string> = {
  ITEM_RECPT: 'success', MISC_RECPT: 'success',
  MISC_ISSUE: 'danger', ITEM_RTV: 'danger', PRODUCT_SALES: 'danger', ISSUE_OUT: 'danger',
  PRODUCT_RT: 'success', RETURN_IN: 'success',
  TRANS_OUT: 'warning', TRANS_IN: 'warning',
  ALLOCATE: '', RELEASE: 'info',
}

const transactionList = ref<WmTransaction[]>([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)

const data = reactive({
  form: {} as WmTransaction,
  queryParams: { pageNum: 1, pageSize: 10 } as WmTransactionQueryParams,
})
const { queryParams, form } = toRefs(data)

function getList() {
  loading.value = true
  listWmTransaction(queryParams.value).then(r => {
    transactionList.value = r.rows
    total.value = r.total
    loading.value = false
  })
}

function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }

function handleDetail(row: WmTransaction) {
  getWmTransaction(row.transactionId).then(r => { form.value = r.data; open.value = true })
}

function closeDetail() { open.value = false }

function handleExport() { proxy.download('/mes/wm/transaction/export', { ...queryParams.value }, `transaction_${Date.now()}.xlsx`) }

getList()
</script>
