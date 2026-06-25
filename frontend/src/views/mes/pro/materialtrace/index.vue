<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" size="small" :inline="true" v-show="showSearch" label-width="90px">
      <el-form-item label="追溯类型" prop="traceType">
        <el-select v-model="queryParams.traceType" placeholder="请选择" clearable style="width:120px">
          <el-option v-for="d in traceTypeOptions" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="工单" prop="workorderId">
        <el-input v-model="queryParams.workorderId" placeholder="请输入工单ID" clearable style="width:120px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="流转卡号" prop="cardId">
        <el-input v-model="queryParams.cardId" placeholder="请输入流转卡号" clearable style="width:120px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="工序" prop="processId">
        <el-input v-model="queryParams.processId" placeholder="请输入工序ID" clearable style="width:120px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Search" size="small" @click="handleQuery" v-hasPermi="['mes:pro:materialtrace:query']">搜索</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="traceList">
      <el-table-column label="追溯ID" align="center" prop="traceId" width="100" />
      <el-table-column label="追溯类型" align="center" prop="traceType" width="110">
        <template #default="scope">
          <el-tag :type="getTypeTag(scope.row.traceType)" size="small">{{ getTypeLabel(scope.row.traceType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="源类型" align="center" prop="parentType" width="100" />
      <el-table-column label="源ID" align="center" prop="parentId" width="100" />
      <el-table-column label="目标类型" align="center" prop="childType" width="100" />
      <el-table-column label="目标ID" align="center" prop="childId" width="100" />
      <el-table-column label="数量" align="center" prop="quantity" width="90" />
      <el-table-column label="单位" align="center" prop="unitOfMeasure" width="80" />
      <el-table-column label="工单" align="center" prop="workorderId" width="100" />
      <el-table-column label="追溯时间" align="center" prop="traceTime" width="160">
        <template #default="scope">
          <span>{{ parseTime(scope.row.traceTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="80" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" size="small" @click="handleView(scope.row)" v-hasPermi="['mes:pro:materialtrace:query']">查看</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 查看详情弹窗 -->
    <el-dialog title="物料追溯详情" v-model="viewOpen" width="650px" append-to-body>
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="追溯ID">{{ viewForm.traceId }}</el-descriptions-item>
        <el-descriptions-item label="追溯类型">
          <el-tag :type="getTypeTag(viewForm.traceType)" size="small">{{ getTypeLabel(viewForm.traceType) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="源类型">{{ viewForm.parentType }}</el-descriptions-item>
        <el-descriptions-item label="源ID">{{ viewForm.parentId }}</el-descriptions-item>
        <el-descriptions-item label="目标类型">{{ viewForm.childType }}</el-descriptions-item>
        <el-descriptions-item label="目标ID">{{ viewForm.childId }}</el-descriptions-item>
        <el-descriptions-item label="数量">{{ viewForm.quantity }}</el-descriptions-item>
        <el-descriptions-item label="单位">{{ viewForm.unitOfMeasure }}</el-descriptions-item>
        <el-descriptions-item label="工单">{{ viewForm.workorderId }}</el-descriptions-item>
        <el-descriptions-item label="流转卡">{{ viewForm.cardId }}</el-descriptions-item>
        <el-descriptions-item label="工序">{{ viewForm.processId }}</el-descriptions-item>
        <el-descriptions-item label="追溯时间">{{ parseTime(viewForm.traceTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{ parseTime(viewForm.createTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="viewOpen = false">关 闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="MaterialTrace">
import { ref, reactive, toRefs, getCurrentInstance } from 'vue'
import { listMaterialTrace, getMaterialTrace } from '@/api/mes/pro/materialtrace'

const { proxy } = getCurrentInstance() as any

const traceTypeOptions = [
  { label: '投料消耗', value: 'ISSUE' },
  { label: '生产产出', value: 'PRODUCE' },
  { label: '分切', value: 'SLIT' },
  { label: '合并', value: 'MERGE' },
  { label: '调整', value: 'ADJUST' }
]

const traceTypeLabelMap: Record<string, string> = {
  ISSUE: '投料消耗', PRODUCE: '生产产出', SLIT: '分切', MERGE: '合并', ADJUST: '调整'
}
const traceTypeTagMap: Record<string, string> = {
  ISSUE: 'warning', PRODUCE: 'success', SLIT: '', MERGE: 'info', ADJUST: 'danger'
}
function getTypeLabel(t: string): string { return traceTypeLabelMap[t] || t }
function getTypeTag(t: string): string { return traceTypeTagMap[t] || 'info' }

const traceList = ref<any[]>([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const viewOpen = ref(false)
const viewForm = ref<any>({})

const data = reactive({
  queryParams: { pageNum: 1, pageSize: 10 } as any
})
const { queryParams } = toRefs(data)

function getList() {
  loading.value = true
  listMaterialTrace(queryParams.value).then((r: any) => { traceList.value = r.rows; total.value = r.total; loading.value = false })
}
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleView(row: any) {
  getMaterialTrace(row.traceId).then((r: any) => { viewForm.value = r.data; viewOpen.value = true })
}

getList()
</script>

<style scoped lang="scss">
:deep(.el-form-item__label) { padding-right: 16px !important; }
</style>
