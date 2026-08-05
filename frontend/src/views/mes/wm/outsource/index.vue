<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-form :model="queryParams" ref="queryFormRef" :inline="true" v-show="showSearch" label-width="90px">
      <el-form-item label="外协单号" prop="orderCode">
        <el-input v-model="queryParams.orderCode" placeholder="请输入外协单号" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="外协厂商" prop="vendorName">
        <el-input v-model="queryParams.vendorName" placeholder="请输入厂商名称" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 160px">
          <el-option v-for="d in mes_outsource_status" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="来源类型" prop="sourceType">
        <el-select v-model="queryParams.sourceType" placeholder="全部" clearable style="width: 160px">
          <el-option v-for="d in mes_outsource_type" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 工具栏 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Promotion" size="small" @click="handleCreate" v-hasPermi="['mes:wm:outsource:add']">外协发货</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <!-- 列表 -->
    <el-table v-loading="loading" :data="list" border>
      <el-table-column label="外协单号" align="center" prop="orderCode" width="160" />
      <el-table-column label="来源" align="center" prop="sourceType" width="80">
        <template #default="{ row }">
          <dict-tag :options="mes_outsource_type" :value="row.sourceType" />
        </template>
      </el-table-column>
      <el-table-column label="外协厂商" align="center" prop="vendorName" width="100" />
      <el-table-column label="工单编码" align="center" prop="workorderCode" width="140" />
      <el-table-column label="工序" align="center" prop="processName" width="100">
        <template #default="{ row }">{{ row.processName || '-' }}</template>
      </el-table-column>
      <el-table-column label="发料总量" align="center" prop="issueTotalQty" width="100">
        <template #default="{ row }">{{ row.issueTotalQty }}吨</template>
      </el-table-column>
      <el-table-column label="收货总量" align="center" prop="recptTotalQty" width="100">
        <template #default="{ row }">{{ row.recptTotalQty ? row.recptTotalQty + '吨' : '-' }}</template>
      </el-table-column>
      <el-table-column label="操作人" align="center" prop="operator" width="80" />
      <el-table-column label="发料时间" align="center" prop="issueTime" width="160" />
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template #default="{ row }">
          <dict-tag :options="mes_outsource_status" :value="row.status" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="handleDetail(row)">详情</el-button>
          <el-button v-if="row.status === 'PROCESSING'" link type="success" size="small" @click="handleReceive(row)" v-hasPermi="['mes:wm:outsource:receive']">收货</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:current-page="queryParams.pageNum" v-model:page-size="queryParams.pageSize" @pagination="getList" />

    <!-- 外协发货弹窗 -->
    <OutsourceCreateDialog ref="createDialogRef" @success="getList" />

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="外协单详情" width="800px" append-to-body>
      <el-descriptions v-if="detail" :column="3" border size="small">
        <el-descriptions-item label="外协单号">{{ detail.orderCode }}</el-descriptions-item>
        <el-descriptions-item label="来源类型"><dict-tag :options="mes_outsource_type" :value="detail.sourceType" /></el-descriptions-item>
        <el-descriptions-item label="外协厂商">{{ detail.vendorName }}</el-descriptions-item>
        <el-descriptions-item label="工单编码">{{ detail.workorderCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="工序">{{ detail.processName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态"><dict-tag :options="mes_outsource_status" :value="detail.status" /></el-descriptions-item>
        <el-descriptions-item label="发料总量">{{ detail.issueTotalQty }}吨</el-descriptions-item>
        <el-descriptions-item label="收货总量">{{ detail.recptTotalQty ? detail.recptTotalQty + '吨' : '-' }}</el-descriptions-item>
        <el-descriptions-item label="报工ID">{{ detail.feedbackId || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-divider content-position="left">发料明细</el-divider>
      <el-table :data="detail?.issueLines || []" border size="small">
        <el-table-column label="物料编码" prop="itemCode" width="120" />
        <el-table-column label="物料名称" prop="itemName" min-width="120" show-overflow-tooltip />
        <el-table-column label="数量" prop="quantity" width="90" />
        <el-table-column label="单位" prop="unitName" width="60" />
        <el-table-column label="批次" prop="batchCode" width="120" />
        <el-table-column label="仓库" prop="warehouseName" width="100" />
      </el-table>
      <template v-if="detail?.recptLines?.length > 0">
        <el-divider content-position="left">收货明细</el-divider>
        <el-table :data="detail.recptLines" border size="small">
          <el-table-column label="物料编码" prop="itemCode" width="120" />
          <el-table-column label="物料名称" prop="itemName" min-width="120" show-overflow-tooltip />
          <el-table-column label="数量" prop="quantity" width="90" />
          <el-table-column label="单位" prop="unitName" width="60" />
          <el-table-column label="仓库" prop="warehouseName" width="100" />
        </el-table>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, getCurrentInstance } from 'vue'
import { listOutsource, getOutsource, receiveOutsource } from '@/api/mes/wm/outsource'
import OutsourceCreateDialog from './OutsourceCreateDialog.vue'

const { proxy } = getCurrentInstance() as any
const { mes_outsource_status, mes_outsource_type } = proxy.useDict('mes_outsource_status', 'mes_outsource_type')
const queryFormRef = ref()
const loading = ref(false)
const showSearch = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const createDialogRef = ref()
const detailVisible = ref(false)
const detail = ref<any>(null)

const queryParams = reactive<any>({
  pageNum: 1, pageSize: 10,
  orderCode: '', vendorName: '', status: '', sourceType: ''
})

function getList() {
  loading.value = true
  listOutsource(queryParams).then((res: any) => {
    list.value = res.rows || []
    total.value = res.total || 0
  }).finally(() => { loading.value = false })
}

function handleQuery() { queryParams.pageNum = 1; getList() }
function resetQuery() { queryFormRef.value?.resetFields(); handleQuery() }
function handleCreate() { createDialogRef.value?.open() }

async function handleDetail(row: any) {
  const res = await getOutsource(row.orderId)
  detail.value = res.data
  detailVisible.value = true
}

async function handleReceive(row: any) {
  await proxy.$modal.confirm('确认收货？收货后将入库并推进流转卡。')
  await receiveOutsource(row.orderId)
  proxy.$modal.msgSuccess('收货成功')
  getList()
}

onMounted(() => getList())
</script>
