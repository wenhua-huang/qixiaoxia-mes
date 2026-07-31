<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-form :model="queryParams" ref="queryFormRef" :inline="true" v-show="showSearch" label-width="90px">
      <el-row>
        <el-col :span="8">
          <el-form-item label="分切批次号" prop="slitBatchNo">
            <el-input v-model="queryParams.slitBatchNo" placeholder="请输入分切批次号" clearable @keyup.enter="handleQuery" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="工单编码" prop="workorderCode">
            <el-input v-model="queryParams.workorderCode" placeholder="请输入工单编码" clearable @keyup.enter="handleQuery" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="领料物料" prop="sourceItemName">
            <el-input v-model="queryParams.sourceItemName" placeholder="请输入领料物料名称" clearable @keyup.enter="handleQuery" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="母卷号" prop="parentRollCode">
            <el-input v-model="queryParams.parentRollCode" placeholder="请输入母卷号" clearable @keyup.enter="handleQuery" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="8">
          <el-form-item label="操作人" prop="operator">
            <el-input v-model="queryParams.operator" placeholder="请输入操作人" clearable @keyup.enter="handleQuery" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item>
            <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <!-- 工具栏 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Scissor" size="small" @click="handleExecute" v-hasPermi="['mes:pro:slitting:add']">执行分切</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <!-- 列表 -->
    <el-table v-loading="loading" :data="list" border>
      <el-table-column label="分切批次号" align="center" prop="slitBatchNo" width="160" />
      <el-table-column label="工单编码" align="center" prop="workorderCode" width="140" />
      <el-table-column label="领料物料" align="center" prop="sourceItemName" min-width="120" show-overflow-tooltip />
      <el-table-column label="领料量(吨)" align="center" prop="pickQty" width="100" />
      <el-table-column label="母卷号" align="center" prop="parentRollCode" width="160" />
      <el-table-column label="子卷数" align="center" prop="childCount" width="70" />
      <el-table-column label="子卷总重" align="center" prop="childTotalWeight" width="100">
        <template #default="{ row }">{{ row.childTotalWeight }}吨</template>
      </el-table-column>
      <el-table-column label="纸边重量" align="center" prop="edgeWeight" width="90">
        <template #default="{ row }">{{ row.edgeWeight ? row.edgeWeight + 'kg' : '-' }}</template>
      </el-table-column>
      <el-table-column label="损耗率" align="center" prop="lossRate" width="80">
        <template #default="{ row }">
          <el-tag :type="row.lossRate > 3 ? 'danger' : 'success'" size="small">{{ row.lossRate }}%</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作人" align="center" prop="operator" width="80" />
      <el-table-column label="分切时间" align="center" prop="slitTime" width="160" />
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 'EXECUTED' ? 'success' : 'info'" size="small">
            {{ row.status === 'EXECUTED' ? '已执行' : row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="80" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="handleDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:current-page="queryParams.pageNum" v-model:page-size="queryParams.pageSize" @pagination="getList" />

    <!-- 执行分切弹窗 -->
    <SlittingExecuteDialog ref="executeDialogRef" @success="getList" />

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="分切详情" width="800px" append-to-body>
      <el-descriptions v-if="detail" :column="3" border size="small">
        <el-descriptions-item label="分切批次号">{{ detail.slitBatchNo }}</el-descriptions-item>
        <el-descriptions-item label="工单编码">{{ detail.workorderCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="工序">{{ detail.processName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="领料物料">{{ detail.sourceItemName }}</el-descriptions-item>
        <el-descriptions-item label="领料量">{{ detail.pickQty }}吨</el-descriptions-item>
        <el-descriptions-item label="领料仓库">{{ detail.sourceWarehouseName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="母卷号">{{ detail.parentRollCode }}</el-descriptions-item>
        <el-descriptions-item label="母卷物料">{{ detail.parentItemName }}</el-descriptions-item>
        <el-descriptions-item label="子卷数">{{ detail.childCount }}</el-descriptions-item>
        <el-descriptions-item label="子卷总重">{{ detail.childTotalWeight }}吨</el-descriptions-item>
        <el-descriptions-item label="纸边重量">{{ detail.edgeWeight ? detail.edgeWeight + 'kg' : '-' }}</el-descriptions-item>
        <el-descriptions-item label="损耗重量">{{ detail.lossWeight }}吨</el-descriptions-item>
        <el-descriptions-item label="损耗率">{{ detail.lossRate }}%</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ detail.operator }}</el-descriptions-item>
        <el-descriptions-item label="分切时间">{{ detail.slitTime }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.status === 'EXECUTED' ? '已执行' : detail.status }}</el-descriptions-item>
      </el-descriptions>
      <el-divider content-position="left">子卷明细</el-divider>
      <el-table :data="detail?.childRolls || []" border size="small">
        <el-table-column label="子卷号" prop="rollCode" width="160" />
        <el-table-column label="物料编码" prop="itemCode" width="120" />
        <el-table-column label="物料名称" prop="itemName" min-width="120" show-overflow-tooltip />
        <el-table-column label="门幅(mm)" prop="actualWidth" width="90" />
        <el-table-column label="长度(m)" prop="actualLength" width="90" />
        <el-table-column label="重量(吨)" prop="actualWeight" width="100" />
        <el-table-column label="状态" prop="status" width="80" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, getCurrentInstance } from 'vue'
import { listSlitting, getSlitting } from '@/api/mes/pro/slitting'
import SlittingExecuteDialog from './SlittingExecuteDialog.vue'

const { proxy } = getCurrentInstance() as any
const queryFormRef = ref()
const loading = ref(false)
const showSearch = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const executeDialogRef = ref()
const detailVisible = ref(false)
const detail = ref<any>(null)

const queryParams = reactive<any>({
  pageNum: 1, pageSize: 10,
  slitBatchNo: '', workorderCode: '', parentRollCode: '',
  sourceItemName: '', operator: ''
})

function getList() {
  loading.value = true
  listSlitting(queryParams).then((res: any) => {
    list.value = res.rows || []
    total.value = res.total || 0
  }).finally(() => { loading.value = false })
}

function handleQuery() { queryParams.pageNum = 1; getList() }

function resetQuery() {
  queryFormRef.value?.resetFields()
  handleQuery()
}

function handleExecute() { executeDialogRef.value?.open() }

async function handleDetail(row: any) {
  const res = await getSlitting(row.slitId)
  detail.value = res.data
  detailVisible.value = true
}

onMounted(() => getList())
</script>
