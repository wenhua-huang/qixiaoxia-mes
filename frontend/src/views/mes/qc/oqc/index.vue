<template>
  <div class="app-container">
    <!-- 搜索区 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="检验单号" prop="oqcCode">
        <el-input v-model="queryParams.oqcCode" placeholder="请输入检验单号" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="物料" prop="itemName">
        <el-input v-model="queryParams.itemName" placeholder="名称/编码" clearable style="width: 160px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="客户" prop="clientName">
        <el-input v-model="queryParams.clientName" placeholder="请输入客户" clearable style="width: 160px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="状态" clearable style="width: 130px">
          <el-option v-for="d in mes_qc_status" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="判定结果" prop="checkResult">
        <el-select v-model="queryParams.checkResult" placeholder="判定结果" clearable style="width: 130px">
          <el-option v-for="d in mes_qc_check_result" :key="d.value" :label="d.label" :value="d.value" />
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
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()" v-hasPermi="['mes:qc:oqc:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['mes:qc:oqc:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="oqcList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="检验单号" align="center" prop="oqcCode" width="170" fixed="left">
        <template #default="scope">
          <el-button link type="primary" @click="handleView(scope.row)">{{ scope.row.oqcCode }}</el-button>
        </template>
      </el-table-column>
      <el-table-column label="来源单据" align="center" prop="sourceDocCode" width="160" :show-overflow-tooltip="true" />
      <el-table-column label="客户" align="center" prop="clientName" width="140" :show-overflow-tooltip="true" />
      <el-table-column label="物料" align="center" min-width="170" :show-overflow-tooltip="true">
        <template #default="scope">{{ scope.row.itemCode }} {{ scope.row.itemName }}</template>
      </el-table-column>
      <el-table-column label="批次号" align="center" prop="batchCode" width="120" :show-overflow-tooltip="true" />
      <el-table-column label="出库数" align="center" prop="quantityOut" width="90" />
      <el-table-column label="检测数" align="center" width="140">
        <template #default="scope">
          <span>{{ scope.row.quantityCheck ?? '-' }} / {{ scope.row.quantityMinCheck ?? '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="90">
        <template #default="scope">
          <dict-tag :options="mes_qc_status" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="判定结果" align="center" width="100">
        <template #default="scope">
          <dict-tag :options="mes_qc_check_result" :value="scope.row.checkResult" />
        </template>
      </el-table-column>
      <el-table-column label="检验员" align="center" prop="inspector" width="90" />
      <el-table-column label="检验日期" align="center" prop="inspectDate" width="160" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" width="200" class-name="small-padding fixed-width" fixed="right">
        <template #default="scope">
          <el-button v-if="isEditable(scope.row)" link type="primary" icon="Edit" @click="handleEdit(scope.row)" v-hasPermi="['mes:qc:oqc:edit']">录入检验</el-button>
          <el-button link type="success" icon="View" @click="handleView(scope.row)" v-hasPermi="['mes:qc:oqc:query']">详情</el-button>
          <el-button v-if="isEditable(scope.row)" link type="danger" icon="CircleClose" @click="handleClose(scope.row)" v-hasPermi="['mes:qc:oqc:edit']">作废</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 录入/详情抽屉 -->
    <OqcEditDrawer ref="drawerRef" @success="getList" />
  </div>
</template>

<script setup lang="ts" name="QcOqc">
import { ref, onMounted, getCurrentInstance } from 'vue'
import { useRoute } from 'vue-router'
import type { QcOqc } from '@/api/mes/qc/oqc'
import { listOqc, delOqc, closeOqc } from '@/api/mes/qc/oqc'
import OqcEditDrawer from './OqcEditDrawer.vue'

const { proxy } = getCurrentInstance() as any
const route = useRoute()
const { mes_qc_status, mes_qc_check_result } = useDict('mes_qc_status', 'mes_qc_check_result')

const oqcList = ref<QcOqc[]>([])
const loading = ref(true)
const showSearch = ref(true)
const ids = ref<number[]>([])
const multiple = ref(true)
const total = ref(0)
const drawerRef = ref()

const queryParams = ref<any>({ pageNum: 1, pageSize: 10 })

function getList() {
  loading.value = true
  listOqc(queryParams.value).then((response: any) => {
    oqcList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}
function resetQuery() {
  // 保留来源单据过滤（出库单页跳转带入），其余条件重置
  const keep = queryParams.value.sourceDocId
  proxy.resetForm('queryRef')
  queryParams.value.sourceDocId = keep
  handleQuery()
}
function handleSelectionChange(selection: QcOqc[]) {
  ids.value = selection.map(item => item.oqcId!)
  multiple.value = !selection.length
}
function isEditable(row: QcOqc) {
  return row.status === 'PENDING' || row.status === 'INSPECTING'
}
function handleEdit(row: QcOqc) {
  drawerRef.value?.open(row.oqcId!, false)
}
function handleView(row: QcOqc) {
  drawerRef.value?.open(row.oqcId!, true)
}
function handleClose(row: QcOqc) {
  proxy.$modal.confirm(`确认作废检验单 "${row.oqcCode}"？作废后不可恢复。`).then(() => closeOqc(row.oqcId!)).then(() => {
    proxy.$modal.msgSuccess('作废成功')
    getList()
  }).catch(() => {})
}
function handleDelete(row?: QcOqc) {
  const _ids = row?.oqcId ? [row.oqcId] : ids.value
  proxy.$modal.confirm('是否确认删除检验单编号为"' + _ids + '"的数据项？').then(() => delOqc(_ids)).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}
function handleExport() {
  proxy.download('mes/qc/oqc/export', { ...queryParams.value }, `qc_oqc_${new Date().getTime()}.xlsx`)
}

onMounted(() => {
  // 出库单页"检验状态"tag 跳转：携带 sourceDocId 自动过滤
  const sid = route.query.sourceDocId as string
  if (sid) {
    queryParams.value.sourceDocType = 'wm_product_sales'
    queryParams.value.sourceDocId = Number(sid)
  }
  getList()
})
</script>

<style scoped>
:deep(.el-form-item__label) { padding-right: 16px !important; }
</style>
