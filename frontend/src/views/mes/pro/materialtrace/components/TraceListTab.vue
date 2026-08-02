<template>
  <div>
    <el-form :model="queryParams" ref="queryRef" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="事件类型" prop="traceType">
        <el-select v-model="queryParams.traceType" placeholder="全部" clearable style="width:140px">
          <el-option v-for="d in traceTypeDict" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="工单号" prop="workorderCode">
        <el-input v-model="queryParams.workorderCode" placeholder="工单编码 WO2026..." clearable style="width:180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="流转卡号" prop="cardCode">
        <el-input v-model="queryParams.cardCode" placeholder="流转卡号 000CRD..." clearable style="width:180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="物料" prop="queryItemName">
        <el-input v-model="queryParams.queryItemName" placeholder="物料名称/编码" clearable style="width:180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="批次号" prop="queryBatchCode">
        <el-input v-model="queryParams.queryBatchCode" placeholder="批次号 BATCH..." clearable style="width:160px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="时间范围" prop="dateRange">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width:240px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="traceList" stripe>
      <el-table-column label="事件" align="center" prop="traceType" width="100">
        <template #default="scope">
          <dict-tag :options="traceTypeDict" :value="scope.row.traceType" />
        </template>
      </el-table-column>
      <el-table-column label="物料" align="left" min-width="180" show-overflow-tooltip>
        <template #default="scope">
          <div>{{ scope.row.itemName || '—' }}</div>
          <div style="font-size:12px;color:#909399">{{ scope.row.itemCode }}</div>
        </template>
      </el-table-column>
      <el-table-column label="批次" align="center" prop="batchCode" width="140" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.batchCode || '—' }}</template>
      </el-table-column>
      <el-table-column label="源（从哪来）" align="left" min-width="200" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.parentDesc || '—' }}</template>
      </el-table-column>
      <el-table-column label="目标（到哪去）" align="left" min-width="200" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.childDesc || '—' }}</template>
      </el-table-column>
      <el-table-column label="数量" align="center" width="110">
        <template #default="scope">
          {{ scope.row.quantity }} {{ scope.row.unitName || scope.row.unitOfMeasure || '' }}
        </template>
      </el-table-column>
      <el-table-column label="追溯时间" align="center" prop="traceTime" width="160">
        <template #default="scope">
          <span>{{ parseTime(scope.row.traceTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="240" class-name="small-padding fixed-width" fixed="right">
        <template #default="scope">
          <el-button link type="primary" icon="View" size="small" @click="handleView(scope.row)" v-hasPermi="['mes:pro:materialtrace:query']">详情</el-button>
          <el-button link type="success" size="small" @click="$emit('traceFromRow', scope.row, 'forward')" v-hasPermi="['mes:pro:materialtrace:query']">追去向 →</el-button>
          <el-button link type="warning" size="small" @click="$emit('traceFromRow', scope.row, 'backward')" v-hasPermi="['mes:pro:materialtrace:query']">← 追来源</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 查看详情弹窗 -->
    <el-dialog title="物料追溯详情" v-model="viewOpen" width="680px" append-to-body>
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="事件类型">
          <dict-tag :options="traceTypeDict" :value="viewForm.traceType" />
        </el-descriptions-item>
        <el-descriptions-item label="物料">{{ viewForm.itemName }} <span style="color:#909399">{{ viewForm.itemCode }}</span></el-descriptions-item>
        <el-descriptions-item label="批次号">{{ viewForm.batchCode || '—' }}</el-descriptions-item>
        <el-descriptions-item label="数量">{{ viewForm.quantity }} {{ viewForm.unitName || viewForm.unitOfMeasure }}</el-descriptions-item>
        <el-descriptions-item label="源（从哪来）" :span="2">{{ viewForm.parentDesc || '—' }}</el-descriptions-item>
        <el-descriptions-item label="目标（到哪去）" :span="2">{{ viewForm.childDesc || '—' }}</el-descriptions-item>
        <el-descriptions-item label="工单">{{ viewForm.workorderCode || viewForm.workorderId || '—' }}</el-descriptions-item>
        <el-descriptions-item label="流转卡">{{ viewForm.cardCode || viewForm.cardId || '—' }}</el-descriptions-item>
        <el-descriptions-item label="追溯时间" :span="2">{{ parseTime(viewForm.traceTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</el-descriptions-item>
      </el-descriptions>
      <el-collapse style="margin-top:12px">
        <el-collapse-item title="🔧 技术信息（管理员）" name="tech">
          <div style="font-size:12px;color:#909399;line-height:1.8">
            traceId:{{ viewForm.traceId }}<br>
            链路: {{ viewForm.parentType }}:{{ viewForm.parentId }} → {{ viewForm.childType }}:{{ viewForm.childId }}<br>
            <span v-if="viewForm.transactionId">事务ID: {{ viewForm.transactionId }}</span>
            <span v-if="viewForm.processId"> | 工序ID: {{ viewForm.processId }}</span>
            <span v-if="viewForm.vendorId"> | 供应商ID: {{ viewForm.vendorId }}</span>
          </div>
        </el-collapse-item>
      </el-collapse>
      <template #footer>
        <el-button @click="viewOpen = false">关 闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="TraceListTab">
import { ref, reactive, toRefs, getCurrentInstance } from 'vue'
import { listMaterialTrace, getMaterialTrace } from '@/api/mes/pro/materialtrace'

defineEmits<{ (e: 'traceFromRow', row: any, direction: 'forward' | 'backward'): void }>()

const { proxy } = getCurrentInstance() as any
// 追溯事件类型字典（后端字典 mes_material_trace_type，禁硬编码 Record<string,string>）
const { mes_material_trace_type: traceTypeDict } = proxy.useDict('mes_material_trace_type')

const traceList = ref<any[]>([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const viewOpen = ref(false)
const viewForm = ref<any>({})
const dateRange = ref<string[]>([])

const data = reactive({
  queryParams: {
    pageNum: 1, pageSize: 10,
    traceType: undefined, workorderCode: undefined, cardCode: undefined,
    queryItemName: undefined, queryBatchCode: undefined
  } as any
})
const { queryParams } = toRefs(data)

function buildQueryParams() {
  const p = { ...queryParams.value }
  p.params = {}
  if (dateRange.value && dateRange.value.length === 2) {
    p.params.beginTime = dateRange.value[0] + ' 00:00:00'
    p.params.endTime = dateRange.value[1] + ' 23:59:59'
  }
  return p
}

function getList() {
  loading.value = true
  listMaterialTrace(buildQueryParams()).then((r: any) => {
    traceList.value = r.rows; total.value = r.total; loading.value = false
  }).catch(() => { loading.value = false })
}
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() {
  proxy.resetForm('queryRef')
  dateRange.value = []
  handleQuery()
}
function handleView(row: any) {
  getMaterialTrace(row.traceId).then((r: any) => { viewForm.value = r.data; viewOpen.value = true })
}

getList()
</script>

<style scoped lang="scss">
:deep(.el-form-item__label) { padding-right: 16px !important; }
.mb8 { margin-bottom: 8px; }
</style>
