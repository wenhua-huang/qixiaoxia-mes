<template>
  <el-dialog title="发料出库（分批）" v-model="show" width="900px" append-to-body @close="handleClose">
    <el-alert v-if="header" :title="`领料单：${header.issueCode} | 状态：${statusLabel}`" type="info" :closable="false" class="mb8" />
    <!-- 待发料行（来自领料单 line） -->
    <el-table :data="lines" size="small" border class="mb8">
      <el-table-column label="物料编码" prop="itemCode" width="120" />
      <el-table-column label="物料名称" prop="itemName" :show-overflow-tooltip="true" min-width="120" />
      <el-table-column label="申请量" prop="quantityIssue" width="80" align="center" />
      <el-table-column label="已发料" prop="quantityIssued" width="80" align="center">
        <template #default="scope">{{ scope.row.quantityIssued || 0 }}</template>
      </el-table-column>
      <el-table-column label="未发料" width="80" align="center">
        <template #default="scope">{{ remain(scope.row) }}</template>
      </el-table-column>
      <el-table-column label="单位" prop="unitName" width="60" align="center" />
    </el-table>

    <!-- 本次发料录入 -->
    <div class="mb8">
      <el-button type="primary" plain icon="Plus" size="small" @click="addDetailRow">添加发料明细</el-button>
      <span class="ml8" style="color:#909399;font-size:12px">为每个物料指定本次发料数量，可选批次/库位</span>
    </div>
    <el-table :data="details" size="small" border>
      <el-table-column label="物料" min-width="160">
        <template #default="scope">
          <el-select v-model="scope.row.lineId" placeholder="选择领料行" size="small" style="width:100%" @change="(v) => onLineChange(scope.$index, v)">
            <el-option v-for="l in issueableLines" :key="l.lineId" :label="`${l.itemCode} - ${l.itemName}`" :value="l.lineId" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="批次" width="240">
        <template #default="scope">
          <el-select v-model="scope.row.materialStockId" placeholder="按预占发料" size="small" clearable filterable style="width:100%" @change="(v) => onBatchChange(scope.$index, v)">
            <el-option v-for="b in (scope.row._batchOptions || [])" :key="b.materialStockId"
              :label="`${b.batchCode || '无批次'} · ${b.warehouseName || ('仓' + b.warehouseId)} (可用${b.quantityAvailable}/${b.quantityOnhand})`" :value="b.materialStockId" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="本次发料量" width="120">
        <template #default="scope">
          <el-input-number v-model="scope.row.quantity" :min="0" :precision="2" size="small" controls-position="right" style="width:110px" />
        </template>
      </el-table-column>
      <el-table-column label="单位" width="60" align="center">
        <template #default="scope">{{ scope.row.unitName }}</template>
      </el-table-column>
      <el-table-column label="操作" width="70" align="center">
        <template #default="scope"><el-button link type="danger" icon="Delete" @click="details.splice(scope.$index, 1)" /></template>
      </el-table-column>
    </el-table>

    <!-- 已发料历史 -->
    <div v-if="historyDetails.length" class="mt8">
      <div style="font-size:13px;font-weight:bold;margin-bottom:6px">历史发料记录</div>
      <el-table :data="historyDetails" size="small" border>
        <el-table-column label="物料编码" prop="itemCode" width="120" />
        <el-table-column label="物料名称" prop="itemName" :show-overflow-tooltip="true" min-width="120" />
        <el-table-column label="已发料量" prop="quantity" width="90" align="center" />
        <el-table-column label="批次" prop="batchCode" width="110" />
        <el-table-column label="发料时间" prop="createTime" width="150" />
      </el-table>
    </div>

    <template #footer>
      <el-button type="primary" @click="handleConfirm" :loading="submitting">确认发料</el-button>
      <el-button @click="show = false">取 消</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { issueOut } from '@/api/mes/wm/issueheader'
import { listIssueDetail } from '@/api/mes/wm/issuedetail'
import { availableBatches } from '@/api/mes/wm/material_stock'
import { getCurrentInstance } from 'vue'

const proxy = getCurrentInstance()?.proxy as any
const emit = defineEmits<{ success: [] }>()

const show = ref(false)
const submitting = ref(false)
const header = ref<any>(null)
const lines = ref<any[]>([])
const details = ref<any[]>([])
const historyDetails = ref<any[]>([])

// 可发料的行：未发料量 > 0
const issueableLines = computed(() => lines.value.filter(l => remain(l) > 0))
const statusLabel = computed(() => proxy?.useDict ? '' : (header.value?.status || ''))

function remain(row: any) {
  const issue = row.quantityIssue || 0
  const issued = row.quantityIssued || 0
  return Math.max(0, issue - issued)
}

/** 打开发料弹窗 */
function open(headerData: any, lineData: any[]) {
  header.value = headerData
  lines.value = lineData
  details.value = []
  show.value = true
  // 查询历史发料明细
  listIssueDetail({ issueId: headerData.issueId }).then((r: any) => {
    historyDetails.value = r.rows || []
  }).catch(() => { historyDetails.value = [] })
}

function addDetailRow() {
  // 默认选第一个可发料行
  const first = issueableLines.value[0]
  const row = {
    lineId: first?.lineId || null,
    itemId: first?.itemId || null,
    itemCode: first?.itemCode || '',
    itemName: first?.itemName || '',
    unitOfMeasure: first?.unitOfMeasure || '',
    unitName: first?.unitName || '',
    materialStockId: null,  // 不选 = 按预占发料
    batchId: null,
    batchCode: '',
    quantity: first ? remain(first) : 0,
    _batchOptions: [] as any[]
  }
  details.value.push(row)
  if (first) loadBatchOptions(details.value.length - 1, first)
}

function onLineChange(idx: number, lineId: number) {
  const line = lines.value.find(l => l.lineId === lineId)
  if (line) {
    Object.assign(details.value[idx], {
      itemId: line.itemId, itemCode: line.itemCode, itemName: line.itemName,
      unitOfMeasure: line.unitOfMeasure, unitName: line.unitName,
      quantity: remain(line),
      materialStockId: null, batchId: null, batchCode: ''  // 切换物料重置批次
    })
    loadBatchOptions(idx, line)
  }
}

/** 加载该行物料的可选批次（跨仓：不传 warehouseId），按入库时间升序 */
function loadBatchOptions(idx: number, line: any) {
  if (!line?.itemId) return
  availableBatches(line.itemId).then((r: any) => {
    details.value[idx]._batchOptions = r.data || []
  }).catch(() => { details.value[idx]._batchOptions = [] })
}

/** 选择批次下拉：把 materialStockId 反填 batchId/batchCode */
function onBatchChange(idx: number, materialStockId: any) {
  const row = details.value[idx]
  if (!materialStockId) {
    row.batchId = null; row.batchCode = ''
    return
  }
  const b = (row._batchOptions || []).find((x: any) => x.materialStockId === materialStockId)
  row.batchId = b?.batchId ?? null
  row.batchCode = b?.batchCode ?? ''
  row.warehouseId = b?.warehouseId ?? row.warehouseId
}

function handleConfirm() {
  // 过滤有效行（有 lineId 且数量>0）
  const valid = details.value.filter(d => d.lineId && d.quantity > 0)
  if (!valid.length) { proxy.$modal.msgError('请添加有效的发料明细'); return }
  // 校验不超过未发料量
  for (const d of valid) {
    const line = lines.value.find(l => l.lineId === d.lineId)
    if (line && d.quantity > remain(line)) {
      proxy.$modal.msgError(`物料[${d.itemCode}]本次发料量(${d.quantity})超过未发料量(${remain(line)})`)
      return
    }
  }
  submitting.value = true
  // 剥离前端辅助字段 _batchOptions，组装后端需要的字段
  const payload = valid.map(d => ({
    lineId: d.lineId, itemId: d.itemId, itemCode: d.itemCode, itemName: d.itemName,
    unitOfMeasure: d.unitOfMeasure, unitName: d.unitName,
    quantity: d.quantity,
    batchId: d.batchId ?? null,
    batchCode: d.batchCode || null,
    materialStockId: d.materialStockId ?? null,
    warehouseId: d.warehouseId ?? null
  }))
  issueOut(header.value.issueId, payload).then(() => {
    proxy.$modal.msgSuccess('发料成功')
    show.value = false
    emit('success')
  }).catch(() => {}).finally(() => { submitting.value = false })
}

function handleClose() { details.value = []; historyDetails.value = [] }

defineExpose({ open })
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
.ml8 { margin-left: 8px; }
.mt8 { margin-top: 8px; }
</style>
