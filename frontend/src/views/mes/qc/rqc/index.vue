<template>
  <div class="app-container">
    <!-- 搜索区 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="检验单号" prop="rqcCode">
        <el-input v-model="queryParams.rqcCode" placeholder="请输入检验单号" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="物料" prop="itemName">
        <el-input v-model="queryParams.itemName" placeholder="名称/编码" clearable style="width: 160px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="退料类型" prop="rqcType">
        <el-select v-model="queryParams.rqcType" placeholder="退料类型" clearable style="width: 140px">
          <el-option v-for="d in mes_qc_rqc_type" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="责任归属" prop="responsibility">
        <el-select v-model="queryParams.responsibility" placeholder="责任归属" clearable style="width: 130px">
          <el-option v-for="d in mes_qc_responsibility" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
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
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()" v-hasPermi="['mes:qc:rqc:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['mes:qc:rqc:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="rqcList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="检验单号" align="center" prop="rqcCode" width="170" fixed="left">
        <template #default="scope">
          <el-button link type="primary" @click="handleView(scope.row)">{{ scope.row.rqcCode }}</el-button>
        </template>
      </el-table-column>
      <el-table-column label="退料类型" align="center" width="100">
        <template #default="scope">
          <dict-tag :options="mes_qc_rqc_type" :value="scope.row.rqcType" />
        </template>
      </el-table-column>
      <el-table-column label="来源单据" align="center" prop="sourceDocCode" width="150" :show-overflow-tooltip="true" />
      <el-table-column label="工单" align="center" prop="workorderCode" width="130" :show-overflow-tooltip="true" />
      <el-table-column label="物料" align="center" min-width="170" :show-overflow-tooltip="true">
        <template #default="scope">{{ scope.row.itemCode }} {{ scope.row.itemName }}</template>
      </el-table-column>
      <el-table-column label="批次号" align="center" prop="batchCode" width="120" :show-overflow-tooltip="true" />
      <el-table-column label="检测数" align="center" width="130">
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
      <el-table-column label="责任归属" align="center" width="100">
        <template #default="scope">
          <el-tag v-if="scope.row.responsibility" :type="respTag[scope.row.responsibility] || 'info'" size="small">
            {{ respLabel(scope.row.responsibility) }}
          </el-tag>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column label="检验员" align="center" prop="inspector" width="90" />
      <el-table-column label="检验日期" align="center" prop="inspectDate" width="160" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" width="200" class-name="small-padding fixed-width" fixed="right">
        <template #default="scope">
          <el-button v-if="isEditable(scope.row)" link type="primary" icon="Edit" @click="handleEdit(scope.row)" v-hasPermi="['mes:qc:rqc:edit']">录入检验</el-button>
          <el-button link type="success" icon="View" @click="handleView(scope.row)" v-hasPermi="['mes:qc:rqc:query']">详情</el-button>
          <el-button v-if="isEditable(scope.row)" link type="danger" icon="CircleClose" @click=" handleClose(scope.row)" v-hasPermi="['mes:qc:rqc:edit']">作废</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 录入/详情抽屉 -->
    <RqcEditDrawer ref="drawerRef" @success="getList" />
  </div>
</template>

<script setup lang="ts" name="QcRqc">
import { ref, onMounted, nextTick, getCurrentInstance } from 'vue'
import { useRoute } from 'vue-router'
import type { QcRqc } from '@/api/mes/qc/rqc'
import { listRqc, delRqc, closeRqc } from '@/api/mes/qc/rqc'
import RqcEditDrawer from './RqcEditDrawer.vue'

const { proxy } = getCurrentInstance() as any
const route = useRoute()
const { mes_qc_status, mes_qc_check_result, mes_qc_rqc_type, mes_qc_responsibility } = useDict(
  'mes_qc_status', 'mes_qc_check_result', 'mes_qc_rqc_type', 'mes_qc_responsibility'
)

// 责任归属 tag 配色：供应商=danger、生产=warning、仓储/其他=info
const respTag: Record<string, string> = {
  SUPPLIER: 'danger',
  PRODUCTION: 'warning',
  STORAGE: 'info',
  OTHER: 'info'
}

const rqcList = ref<QcRqc[]>([])
const loading = ref(true)
const showSearch = ref(true)
const ids = ref<number[]>([])
const multiple = ref(true)
const total = ref(0)
const drawerRef = ref()

const queryParams = ref<any>({ pageNum: 1, pageSize: 10 })

function respLabel(value: string) {
  return mes_qc_responsibility.value.find((d: any) => d.value === value)?.label || value
}

function getList() {
  loading.value = true
  listRqc(queryParams.value).then((response: any) => {
    rqcList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}
function resetQuery() {
  // 保留来源单据过滤（退料单页跳转带入），其余条件重置
  const keep = queryParams.value.sourceDocId
  const keepType = queryParams.value.sourceDocType
  proxy.resetForm('queryRef')
  queryParams.value.sourceDocId = keep
  queryParams.value.sourceDocType = keepType
  handleQuery()
}
function handleSelectionChange(selection: QcRqc[]) {
  ids.value = selection.map(item => item.rqcId!)
  multiple.value = !selection.length
}
function isEditable(row: QcRqc) {
  return row.status === 'PENDING' || row.status === 'INSPECTING'
}
function handleEdit(row: QcRqc) {
  drawerRef.value?.open(row.rqcId!, false)
}
function handleView(row: QcRqc) {
  drawerRef.value?.open(row.rqcId!, true)
}
function handleClose(row: QcRqc) {
  proxy.$modal.confirm(`确认作废检验单 "${row.rqcCode}"？作废后不可恢复。`).then(() => closeRqc(row.rqcId!)).then(() => {
    proxy.$modal.msgSuccess('作废成功')
    getList()
  }).catch(() => {})
}
function handleDelete(row?: QcRqc) {
  const _ids = row?.rqcId ? [row.rqcId] : ids.value
  proxy.$modal.confirm('是否确认删除检验单编号为"' + _ids + '"的数据项？').then(() => delRqc(_ids)).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}
function handleExport() {
  proxy.download('mes/qc/rqc/export', { ...queryParams.value }, `qc_rqc_${new Date().getTime()}.xlsx`)
}

onMounted(() => {
  // 退料单页"检验状态"tag 跳转：携带 sourceDocId 自动过滤
  const sid = route.query.sourceDocId as string
  if (sid) {
    queryParams.value.sourceDocType = (route.query.sourceDocType as string) || 'wm_rt_issue'
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
