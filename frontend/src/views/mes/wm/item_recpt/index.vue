<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="入库单号" prop="recptCode">
        <el-input v-model="queryParams.recptCode" placeholder="请输入入库单号" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="供应商" prop="vendorId">
        <el-input v-model="queryParams.vendorId" placeholder="供应商ID" clearable />
      </el-form-item>
      <el-form-item label="采购订单号" prop="purOrderCode">
        <el-input v-model="queryParams.purOrderCode" placeholder="请输入采购订单号" clearable />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable style="width:100px">
          <el-option v-for="d in mes_itemrecpt_status" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="Link" size="small" @click="handleFromPurOrder" v-hasPermi="['mes:wm:itemrecpt:add']">从采购单生成</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="Edit" size="small" :disabled="single" @click="handleUpdate()" v-hasPermi="['mes:wm:itemrecpt:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="Delete" size="small" :disabled="multiple" @click="handleDelete()" v-hasPermi="['mes:wm:itemrecpt:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="Download" size="small" @click="handleExport" v-hasPermi="['mes:wm:itemrecpt:export']">导出</el-button></el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="recptList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="入库单号" align="center" prop="recptCode" width="150">
        <template #default="scope">
          <el-button link type="primary" size="small" @click="handleView(scope.row)">
            {{ scope.row.recptCode }}
          </el-button>
        </template>
      </el-table-column>
      <el-table-column label="入库名称" align="center" prop="recptName" :show-overflow-tooltip="true" width="150" />
      <el-table-column label="供应商" align="center" prop="vendorName" width="120" />
      <el-table-column label="仓库" align="center" prop="warehouseName" width="120" />
      <el-table-column label="入库日期" align="center" prop="recptDate" width="110" />
      <el-table-column label="状态" align="center" width="90">
        <template #default="scope">
          <dict-tag :options="mes_itemrecpt_status" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="检验状态" align="center" width="110">
        <template #default="scope">
          <el-tag v-if="scope.row.qcStatus && scope.row.qcStatus !== 'NONE'" size="small" :type="qcTagType(scope.row.qcStatus)"
            style="cursor: pointer" @click="goIqc(scope.row)">{{ qcTagText(scope.row.qcStatus) }}</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="190" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-tooltip content="修改" placement="top" v-if="isEditable(scope.row)"><el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:wm:itemrecpt:edit']"></el-button></el-tooltip>
          <el-tooltip content="删除" placement="top" v-if="isEditable(scope.row)"><el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mes:wm:itemrecpt:remove']"></el-button></el-tooltip>
          <el-tooltip content="确认收货" placement="top" v-if="scope.row.status === 'DRAFT'"><el-button link type="success" icon="Check" @click="handleConfirm(scope.row)"></el-button></el-tooltip>
          <el-tooltip content="打印批次标签" placement="top" v-if="scope.row.status !== 'DRAFT'"><el-button link type="primary" icon="Printer" @click="handlePrintLabels(scope.row)"></el-button></el-tooltip>
          <el-button v-if="scope.row.status === 'CONFIRMED'" link type="warning" size="small" @click="handlePost(scope.row)" v-hasPermi="['mes:wm:itemrecpt:edit']">过账</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 新增/修改/查看弹窗（头行一体） -->
    <ItemRecptFormDialog ref="formDialogRef" @success="getList" />
    <!-- 采购订单选择（从采购单生成入口） -->
    <PurOrderSelect ref="purOrderSelectRef" @onSelected="onPurOrderSelected" />
  </div>
</template>

<script setup lang="ts" name="WmItemRecpt">
import { ref, reactive, toRefs, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import type { WmItemRecptQueryParams, WmItemRecpt } from '@/types/api/mes/wm/item_recpt'
import type { WmItemRecptLine } from '@/types/api/mes/wm/item_recpt_line'
import type { PurOrder } from '@/types/api/mes/pur/order'
import { listWmItemRecpt, delWmItemRecpt, buildFromPurOrder } from '@/api/mes/wm/item_recpt'
import { listWmItemRecptLine } from '@/api/mes/wm/item_recpt_line'
import { buildMatPayload } from '@/utils/qrPayload'
import { printQrLabels } from '@/utils/labelPrint'
import request from '@/utils/request'
import ItemRecptFormDialog from './components/ItemRecptFormDialog.vue'
import PurOrderSelect from '@/components/purOrderSelect/single.vue'

const { proxy } = getCurrentInstance() as any
const { mes_itemrecpt_status } = useDict('mes_itemrecpt_status')
const formDialogRef = ref()
const purOrderSelectRef = ref()

const recptList = ref<WmItemRecpt[]>([])
const loading = ref(true)
const showSearch = ref(true)
const ids = ref<number[]>([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)

const data = reactive({
  queryParams: { pageNum: 1, pageSize: 10 } as WmItemRecptQueryParams
})
const { queryParams } = toRefs(data)

function getList() {
  loading.value = true
  listWmItemRecpt(queryParams.value).then(r => { recptList.value = r.rows; total.value = r.total; loading.value = false })
}
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleSelectionChange(s: any[]) { ids.value = s.map(i => i.recptId); single.value = s.length !== 1; multiple.value = !s.length }
function isEditable(row: WmItemRecpt) { return row.status === 'DRAFT' }

function handleView(row: WmItemRecpt) { formDialogRef.value?.openView(row.recptId) }
function handleUpdate(row?: WmItemRecpt) {
  const id = row?.recptId || ids.value[0]
  formDialogRef.value?.openEdit(id)
}
function handleDelete(row?: WmItemRecpt) {
  const _ids = row?.recptId ? [row.recptId] : ids.value
  proxy.$modal.confirm('是否确认删除？').then(() => delWmItemRecpt(_ids)).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') })
}
function handleExport() { proxy.download('/mes/wm/item_recpt/export', { ...queryParams.value }, `itemrecpt_${Date.now()}.xlsx`) }
function handleConfirm(row: WmItemRecpt) {
  if (!row?.recptId || !row?.recptCode) return
  proxy.$modal.confirm(`确认收货 "${row.recptCode}"？系统将更新库存并通知采购订单。`).then(() => {
    request({ url: `/mes/wm/item_recpt/confirm/${row.recptId}`, method: 'put' }).then(() => {
      proxy.$modal.msgSuccess('收货确认成功，库存已更新')
      getList()
      proxy.$modal.confirm('收货成功，是否打印批次标签？').then(() => handlePrintLabels(row)).catch(() => {})
    })
  }).catch(() => {})
}
function handlePost(row: WmItemRecpt) {
  proxy.$modal.confirm(`确认过账入库单 "${row.recptCode}"？过账后将回写采购订单已收数量。`).then(() => {
    request({ url: `/mes/wm/item_recpt/post/${row.recptId}`, method: 'put' }).then(() => {
      proxy.$modal.msgSuccess('过账成功，已回写采购订单')
      getList()
    })
  }).catch(() => {})
}

// 从采购单生成
function handleFromPurOrder() { purOrderSelectRef.value?.open() }
function onPurOrderSelected(row: PurOrder) {
  buildFromPurOrder(row.orderId).then((r: any) => {
    if (!r.data) return
    formDialogRef.value?.openAdd(r.data)
  })
}

// 检验状态 tag：点击跳转 IQC 列表页并按来源单据过滤
const router = useRouter()
const QC_TAG: Record<string, { type: string; text: string }> = {
  PASSED: { type: 'success', text: '检验合格' },
  CONCESSION: { type: 'warning', text: '让步接收' },
  PENDING: { type: 'info', text: '待检验' },
  FAILED: { type: 'danger', text: '检验不合格' }
}
function qcTagType(s: string) { return QC_TAG[s]?.type || 'info' }
function qcTagText(s: string) { return QC_TAG[s]?.text || s }
function goIqc(row: WmItemRecpt) {
  router.push({ path: '/qc/qciqc', query: { sourceDocId: String(row.recptId) } })
}

// ==================== 批次标签打印 ====================
async function handlePrintLabels(row: WmItemRecpt) {
  if (!row?.recptId) return
  const r = await listWmItemRecptLine({ recptId: row.recptId, pageNum: 1, pageSize: 100 } as any)
  const lines = (r.rows || []).filter((l: WmItemRecptLine) => l.batchCode)
  if (!lines.length) { proxy.$modal.msgWarning('该单无批次物料'); return }
  const channel = await printQrLabels({
    title: '批次标签打印',
    items: lines.map(l => ({
      payload: buildMatPayload(l.batchCode!),
      headline: l.batchCode!,
      fields: [
        `${l.itemName || ''} ${l.specification || ''}`.trim(),
        `数量: ${l.quantityRecpt ?? ''} ${l.unitName || ''}`,
        `入库单: ${row.recptCode || ''}`,
        `仓库: ${l.warehouseName || row.warehouseName || ''}`
      ]
    }))
  })
  if (channel === 'clodop') proxy.$modal.msgSuccess('已发送到标签打印机')
}

getList()
</script>

<style scoped>
:deep(.el-form-item__label) {
  padding-right: 16px !important;
}
</style>
