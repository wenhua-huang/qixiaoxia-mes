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
          <dict-tag :options="mes_wm_transaction_type" :value="scope.row.transactionType" />
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
      <el-table-column label="操作人" align="center" prop="createBy" width="100" :show-overflow-tooltip="true" />
      <el-table-column label="操作时间" align="center" prop="createTime" width="160" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="100">
        <template #default="scope">
          <el-button link type="primary" icon="View" size="small" @click="handleDetail(scope.row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog title="库存事务详情" v-model="open" width="680px" append-to-body>
      <el-form ref="formRef" :model="form" label-width="100px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="事务类型">
              <dict-tag :options="mes_wm_transaction_type" :value="form.transactionType || ''" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="事务时间" prop="transactionTime">
              <el-date-picker v-model="form.transactionTime" type="datetime" placeholder="选择日期" style="width:100%" value-format="YYYY-MM-DD HH:mm:ss" disabled />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="物料编码">
              <el-input v-model="form.itemCode" disabled style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="物料名称">
              <el-input v-model="form.itemName" disabled style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="规格型号">
              <el-input v-model="form.specification" disabled style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单位名称">
              <el-input v-model="form.unitName" disabled style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="变动数量">
              <span :style="{ color: Number(form.quantity) > 0 ? '#67c23a' : Number(form.quantity) < 0 ? '#f56c6c' : '#909399', fontWeight: 'bold', lineHeight: '32px' }">
                {{ Number(form.quantity) > 0 ? '+' : '' }}{{ form.quantity }}
              </span>
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="form.quantity2">
            <el-form-item label="辅助变动数量">
              <el-input v-model="form.quantity2" disabled style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row v-if="form.unit2Name">
          <el-col :span="12">
            <el-form-item label="辅助单位名称">
              <el-input v-model="form.unit2Name" disabled style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="来源单据类型">
              <dict-tag :options="mes_wm_source_doc_type" :value="form.sourceDocType || ''" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="来源单据编码">
              <el-input v-model="form.sourceDocCode" disabled style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="仓库名称">
              <el-input v-model="form.warehouseName" disabled style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="form.batchCode">
            <el-form-item label="批次编码">
              <el-input v-model="form.batchCode" disabled style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row v-if="form.workorderCode">
          <el-col :span="12">
            <el-form-item label="生产工单编码">
              <el-input v-model="form.workorderCode" disabled style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="操作人">
              <el-input v-model="form.createBy" disabled style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="操作时间">
              <el-input v-model="form.createTime" disabled style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
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
import { useDict } from '@/utils/dict'

const { proxy } = getCurrentInstance() as any
const { mes_wm_transaction_type, mes_wm_source_doc_type } = useDict('mes_wm_transaction_type', 'mes_wm_source_doc_type')

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

<style scoped>
:deep(.el-form-item__label) {
  padding-right: 16px !important;
}
</style>
