<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="90px">
      <el-form-item label="操作工" prop="userName">
        <el-input v-model="queryParams.userName" placeholder="用户名/昵称" clearable style="width:160px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="工作站" prop="workstationId">
        <el-input v-model="queryParams.workstationId" placeholder="工位ID" clearable style="width:130px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable style="width:120px">
          <el-option label="在岗" value="ACTIVE" />
          <el-option label="已下工" value="CLOSED" />
        </el-select>
      </el-form-item>
      <el-form-item label="日期范围">
        <el-date-picker v-model="dateRange" type="daterange" range-separator="-"
          start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD"
          style="width:220px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()" v-hasPermi="['mes:pro:workrecord:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['mes:pro:workrecord:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="recordList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="操作工" align="center" width="120">
        <template #default="scope">{{ scope.row.nickName || scope.row.userName || '-' }}</template>
      </el-table-column>
      <el-table-column label="工作站" align="center" prop="workstationName" min-width="120" :show-overflow-tooltip="true" />
      <el-table-column label="关联任务" align="center" width="160" :show-overflow-tooltip="true">
        <template #default="scope">{{ scope.row.processName || scope.row.taskCode || '-' }}</template>
      </el-table-column>
      <el-table-column label="上工时间" align="center" prop="clockInTime" width="160">
        <template #default="scope"><span>{{ parseTime(scope.row.clockInTime) }}</span></template>
      </el-table-column>
      <el-table-column label="下工时间" align="center" prop="clockOutTime" width="160">
        <template #default="scope"><span>{{ scope.row.clockOutTime ? parseTime(scope.row.clockOutTime) : '—' }}</span></template>
      </el-table-column>
      <el-table-column label="工时(分钟)" align="center" prop="workDuration" width="100">
        <template #default="scope">{{ scope.row.status === 'CLOSED' ? scope.row.workDuration : '—' }}</template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
            {{ scope.row.status === 'ACTIVE' ? '在岗' : '已下工' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" min-width="120" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="100">
        <template #default="scope">
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mes:pro:workrecord:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup lang="ts" name="ProWorkrecord">
import { ref, reactive, toRefs, getCurrentInstance } from 'vue'
import type { WorkrecordQueryParams, Workrecord } from '@/types/api/mes/pro/workrecord'
import { listWorkrecord, delWorkrecord } from '@/api/mes/pro/workrecord'
import { parseTime } from '@/utils/ruoyi'

const { proxy } = getCurrentInstance() as any

const recordList = ref<Workrecord[]>([])
const loading = ref(true)
const showSearch = ref(true)
const ids = ref<number[]>([])
const multiple = ref(true)
const total = ref(0)
const dateRange = ref<[string, string] | []>([])

const data = reactive({
  queryParams: { pageNum: 1, pageSize: 10 } as WorkrecordQueryParams
})
const { queryParams } = toRefs(data)

function getList() {
  loading.value = true
  listWorkrecord(proxy.addDateRange(queryParams.value, dateRange.value)).then((r: any) => {
    recordList.value = r.rows
    total.value = r.total
    loading.value = false
  }).catch(() => { loading.value = false })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  dateRange.value = []
  proxy.resetForm('queryRef')
  handleQuery()
}

function handleSelectionChange(selection: any[]) {
  ids.value = selection.map(i => i.recordId!)
  multiple.value = !selection.length
}

function handleDelete(row?: Workrecord) {
  const _ids = row?.recordId ? [row.recordId] : ids.value
  proxy.$modal.confirm('是否确认删除所选上下工记录？').then(() => delWorkrecord(_ids.join(','))).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

function handleExport() {
  proxy.download('/mes/pro/workrecord/export', { ...queryParams.value }, `workrecord_${Date.now()}.xlsx`)
}

getList()
</script>

<style lang="scss" scoped>
.app-container { padding: 16px; }
.mb8 { margin-bottom: 8px; }
</style>
