<template>
  <div class="app-container">
    <!-- 搜索区 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="检验单号" prop="iqcCode">
        <el-input v-model="queryParams.iqcCode" placeholder="请输入检验单号" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="物料" prop="itemName">
        <el-input v-model="queryParams.itemName" placeholder="名称/编码" clearable style="width: 160px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="供应商" prop="vendorName">
        <el-input v-model="queryParams.vendorName" placeholder="请输入供应商" clearable style="width: 160px" @keyup.enter="handleQuery" />
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
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()" v-hasPermi="['mes:qc:iqc:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['mes:qc:iqc:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="iqcList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="检验单号" align="center" prop="iqcCode" width="170" fixed="left">
        <template #default="scope">
          <el-button link type="primary" @click="handleView(scope.row)">{{ scope.row.iqcCode }}</el-button>
        </template>
      </el-table-column>
      <el-table-column label="来源单据" align="center" prop="sourceDocCode" width="160" :show-overflow-tooltip="true" />
      <el-table-column label="供应商" align="center" prop="vendorName" width="140" :show-overflow-tooltip="true" />
      <el-table-column label="物料" align="center" min-width="170" :show-overflow-tooltip="true">
        <template #default="scope">{{ scope.row.itemCode }} {{ scope.row.itemName }}</template>
      </el-table-column>
      <el-table-column label="接收数" align="center" prop="quantityReceived" width="90" />
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
          <el-button v-if="isEditable(scope.row)" link type="primary" icon="Edit" @click="handleEdit(scope.row)" v-hasPermi="['mes:qc:iqc:edit']">录入检验</el-button>
          <el-button link type="success" icon="View" @click="handleView(scope.row)" v-hasPermi="['mes:qc:iqc:query']">详情</el-button>
          <el-button v-if="isEditable(scope.row)" link type="danger" icon="CircleClose" @click=" handleClose(scope.row)" v-hasPermi="['mes:qc:iqc:edit']">作废</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 录入/详情抽屉 -->
    <IqcEditDrawer ref="drawerRef" @success="getList" />
  </div>
</template>

<script setup lang="ts" name="QcIqc">
import { ref, onMounted, nextTick, getCurrentInstance } from 'vue'
import { useRoute } from 'vue-router'
import type { QcIqc } from '@/api/mes/qc/iqc'
import { listIqc, delIqc, closeIqc } from '@/api/mes/qc/iqc'
import IqcEditDrawer from './IqcEditDrawer.vue'

const { proxy } = getCurrentInstance() as any
const route = useRoute()
const { mes_qc_status, mes_qc_check_result } = useDict('mes_qc_status', 'mes_qc_check_result')

const iqcList = ref<QcIqc[]>([])
const loading = ref(true)
const showSearch = ref(true)
const ids = ref<number[]>([])
const multiple = ref(true)
const total = ref(0)
const drawerRef = ref()

const queryParams = ref<any>({ pageNum: 1, pageSize: 10 })

function getList() {
  loading.value = true
  listIqc(queryParams.value).then((response: any) => {
    iqcList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}
function resetQuery() {
  // 保留来源单据过滤（入库单页跳转带入），其余条件重置
  const keep = queryParams.value.sourceDocId
  proxy.resetForm('queryRef')
  queryParams.value.sourceDocId = keep
  handleQuery()
}
function handleSelectionChange(selection: QcIqc[]) {
  ids.value = selection.map(item => item.iqcId!)
  multiple.value = !selection.length
}
function isEditable(row: QcIqc) {
  return row.status === 'PENDING' || row.status === 'INSPECTING'
}
function handleEdit(row: QcIqc) {
  drawerRef.value?.open(row.iqcId!, false)
}
function handleView(row: QcIqc) {
  drawerRef.value?.open(row.iqcId!, true)
}
function handleClose(row: QcIqc) {
  proxy.$modal.confirm(`确认作废检验单 "${row.iqcCode}"？作废后不可恢复。`).then(() => closeIqc(row.iqcId!)).then(() => {
    proxy.$modal.msgSuccess('作废成功')
    getList()
  }).catch(() => {})
}
function handleDelete(row?: QcIqc) {
  const _ids = row?.iqcId ? [row.iqcId] : ids.value
  proxy.$modal.confirm('是否确认删除检验单编号为"' + _ids + '"的数据项？').then(() => delIqc(_ids)).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}
function handleExport() {
  proxy.download('mes/qc/iqc/export', { ...queryParams.value }, `qc_iqc_${new Date().getTime()}.xlsx`)
}

onMounted(() => {
  // 入库单页"检验状态"tag 跳转：携带 sourceDocId 自动过滤
  const sid = route.query.sourceDocId as string
  if (sid) {
    queryParams.value.sourceDocType = 'wm_item_recpt'
    queryParams.value.sourceDocId = Number(sid)
  }
  getList()
  // 首页待办跳转：携带 openId 自动打开检验抽屉
  const oid = route.query.openId as string
  if (oid) {
    nextTick(() => drawerRef.value?.open(Number(oid), route.query.ro === '1'))
  }
})
</script>

<style scoped>
:deep(.el-form-item__label) { padding-right: 16px !important; }
</style>
