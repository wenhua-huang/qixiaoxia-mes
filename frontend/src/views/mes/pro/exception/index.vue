<template>
  <div class="app-container">
    <!-- 搜索表单 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="异常单号" prop="exceptionCode">
        <el-input v-model="queryParams.exceptionCode" placeholder="请输入" clearable style="width:180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="工单号" prop="workorderCode">
        <el-input v-model="queryParams.workorderCode" placeholder="请输入" clearable style="width:160px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="异常类型" prop="exceptionType">
        <el-select v-model="queryParams.exceptionType" placeholder="请选择" clearable style="width:140px">
          <el-option v-for="d in mes_pro_exception_type" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="责任方" prop="responsibleParty">
        <el-select v-model="queryParams.responsibleParty" placeholder="请选择" clearable style="width:130px">
          <el-option v-for="d in mes_pro_exception_party" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable style="width:120px">
          <el-option v-for="d in mes_pro_exception_status" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="发生时间">
        <el-date-picker v-model="dateRange" value-format="YYYY-MM-DD" type="daterange"
          range-separator="-" start-placeholder="开始日期" end-placeholder="结束日期" style="width:240px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 工具栏 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['mes:pro:exception:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="list" @row-click="goDetail" class="row-link">
      <el-table-column label="异常单号" align="center" prop="exceptionCode" width="160" :show-overflow-tooltip="true" />
      <el-table-column label="类型" align="center" prop="exceptionType" width="100">
        <template #default="s"><dict-tag :options="mes_pro_exception_type" :value="s.row.exceptionType" /></template>
      </el-table-column>
      <el-table-column label="关联对象" align="left" min-width="240">
        <template #default="s">
          <div>{{ s.row.workorderName || s.row.workorderCode || '-' }}</div>
          <div class="sub-text">{{ s.row.taskCode }}<span v-if="s.row.processName"> · {{ s.row.processName }}</span></div>
        </template>
      </el-table-column>
      <el-table-column label="影响数量" align="center" prop="impactQuantity" width="90" />
      <el-table-column label="发生时间" align="center" prop="occurTime" width="160">
        <template #default="s">{{ parseTime(s.row.occurTime, '{y}-{m}-{d} {h}:{i}') }}</template>
      </el-table-column>
      <el-table-column label="上报人" align="center" prop="reporterName" width="100" />
      <el-table-column label="责任方" align="center" prop="responsibleParty" width="90">
        <template #default="s"><dict-tag :options="mes_pro_exception_party" :value="s.row.responsibleParty" /></template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template #default="s"><dict-tag :options="mes_pro_exception_status" :value="s.row.status" /></template>
      </el-table-column>
      <el-table-column label="出口动作" align="center" prop="resolveType" width="100">
        <template #default="s">
          <dict-tag v-if="s.row.resolveType" :options="mes_pro_exception_resolve" :value="s.row.resolveType" />
          <span v-else style="color:#909399">-</span>
        </template>
      </el-table-column>
      <el-table-column label="处理单据" align="center" prop="targetDocCode" width="150" :show-overflow-tooltip="true">
        <template #default="s">{{ s.row.targetDocCode || '-' }}</template>
      </el-table-column>
      <el-table-column label="关闭时间" align="center" prop="closeTime" width="160">
        <template #default="s">{{ s.row.closeTime ? parseTime(s.row.closeTime, '{y}-{m}-{d} {h}:{i}') : '-' }}</template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup lang="ts" name="ProException">
import { ref, reactive, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import { listException } from '@/api/mes/pro/exception'

const { proxy } = getCurrentInstance() as any
const router = useRouter()
const {
  mes_pro_exception_type, mes_pro_exception_party,
  mes_pro_exception_status, mes_pro_exception_resolve
} = proxy.useDict('mes_pro_exception_type', 'mes_pro_exception_party', 'mes_pro_exception_status', 'mes_pro_exception_resolve')

const loading = ref(true)
const showSearch = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const dateRange = ref<string[]>([])
const data = reactive({
  queryParams: {
    pageNum: 1, pageSize: 10, exceptionCode: undefined, workorderCode: undefined,
    exceptionType: undefined, responsibleParty: undefined, status: undefined
  } as any
})
const { queryParams } = data

function getList() {
  loading.value = true
  listException(proxy.addDateRange(queryParams, dateRange.value, 'occurTime')).then((r: any) => {
    list.value = r.rows || []
    total.value = r.total || 0
  }).finally(() => { loading.value = false })
}
function handleQuery() { queryParams.pageNum = 1; getList() }
function resetQuery() {
  dateRange.value = []
  proxy.resetForm('queryRef')
  handleQuery()
}
function goDetail(row: any) {
  router.push({ path: '/mes/pro/exception_detail', query: { exceptionId: row.exceptionId } })
}
function handleExport() {
  proxy.download('/mes/pro/exception/export',
    { ...proxy.addDateRange(queryParams, dateRange.value, 'occurTime') },
    `pro_exception_${new Date().getTime()}.xlsx`)
}

getList()
</script>

<style lang="scss" scoped>
.app-container { padding: 16px; }
.mb8 { margin-bottom: 8px; }
:deep(.row-link) .el-table__row { cursor: pointer; }
.sub-text { color: #909399; font-size: 12px; }
</style>
