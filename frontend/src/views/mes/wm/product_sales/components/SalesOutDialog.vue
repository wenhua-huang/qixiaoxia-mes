<template>
  <el-dialog title="出库确认（批次拣货）" v-model="show" width="960px" append-to-body @close="handleClose">
    <el-alert v-if="header" :title="`出库单：${header.salesCode} | 客户：${header.clientName || '-'} | 仓库：${header.warehouseName || '-'}`"
              type="info" :closable="false" class="mb8" />

    <!-- 批次码识别：USB 扫码枪=键盘输入回车触发，也支持手输 -->
    <div class="mb8 scan-bar">
      <el-input ref="scanInputRef" v-model="scanCode" size="small" clearable style="width:320px"
                placeholder="扫描/输入批次码（QXX|MAT|xxx 或裸批次码）" @keyup.enter="handleScan" />
      <el-button type="success" plain icon="Search" size="small" @click="handleScan">识别批次</el-button>
      <span class="scan-tip">识别后自动匹配待出库物料并预填一条明细</span>
    </div>

    <!-- 待出库行 -->
    <el-table :data="lines" size="small" border class="mb8">
      <el-table-column label="物料编码" prop="itemCode" width="130" />
      <el-table-column label="物料名称" prop="itemName" :show-overflow-tooltip="true" min-width="140" />
      <el-table-column label="需出库量" prop="quantitySales" width="90" align="center" />
      <el-table-column label="已出库量" prop="quantityPosted" width="90" align="center">
        <template #default="scope">{{ scope.row.quantityPosted || 0 }}</template>
      </el-table-column>
      <el-table-column label="未出库量" width="90" align="center">
        <template #default="scope">{{ remain(scope.row) }}</template>
      </el-table-column>
      <el-table-column label="本仓可用量" width="110" align="center">
        <template #default="scope">
          <span :style="{ color: availColor(scope.row), fontWeight: 'bold' }">
            {{ availOf(scope.row) == null ? '...' : availOf(scope.row) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="单位" prop="unitName" width="60" align="center" />
    </el-table>
    <el-alert v-if="insufficientHint" :title="insufficientHint" type="error" :closable="false" class="mb8" show-icon />

    <!-- 本次出库录入 -->
    <div class="mb8">
      <el-button type="primary" plain icon="Plus" size="small" @click="addDetailRow">添加出库明细</el-button>
      <el-button type="warning" plain icon="Sort" size="small" @click="autoFifoAll">自动 FIFO 分配</el-button>
      <span class="ml8" style="color:#909399;font-size:12px">为每个物料指定批次与本次出库数量</span>
    </div>
    <el-table :data="details" size="small" border>
      <el-table-column label="物料" min-width="170">
        <template #default="scope">
          <el-select v-model="scope.row.lineId" placeholder="选择物料" size="small" style="width:100%"
                     @change="(v: any) => onLineChange(scope.$index, v)">
            <el-option v-for="l in postableLines" :key="l.lineId" :label="`${l.itemCode} - ${l.itemName}`" :value="l.lineId" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="批次编码" width="150">
        <template #default="scope">
          <el-input v-model="scope.row.batchCode" size="small" placeholder="留空=系统匹配(FIFO)，填写=精确扣减该批次" />
        </template>
      </el-table-column>
      <el-table-column label="本次出库量" width="130">
        <template #default="scope">
          <el-input-number v-model="scope.row.quantity" :min="0" :precision="2" size="small"
                           controls-position="right" style="width:120px" />
        </template>
      </el-table-column>
      <el-table-column label="单位" width="60" align="center">
        <template #default="scope">{{ scope.row.unitName }}</template>
      </el-table-column>
      <el-table-column label="操作" width="70" align="center">
        <template #default="scope">
          <el-button link type="danger" icon="Delete" @click="details.splice(scope.$index, 1)" />
        </template>
      </el-table-column>
    </el-table>

    <!-- 历史出库明细 -->
    <div v-if="historyDetails.length" class="mt8">
      <div style="font-size:13px;font-weight:bold;margin-bottom:6px">历史出库记录</div>
      <el-table :data="historyDetails" size="small" border>
        <el-table-column label="物料编码" prop="itemCode" width="130" />
        <el-table-column label="物料名称" prop="itemName" :show-overflow-tooltip="true" min-width="140" />
        <el-table-column label="出库量" prop="quantity" width="90" align="center" />
        <el-table-column label="批次" prop="batchCode" width="120" />
        <el-table-column label="出库时间" prop="createTime" width="160" />
      </el-table>
    </div>

    <template #footer>
      <el-button type="primary" @click="handleConfirm" :loading="submitting">确认出库</el-button>
      <el-button @click="show = false">取 消</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, getCurrentInstance } from 'vue'
import { postOut, getSalesDetail } from '@/api/mes/wm/product_sales'
import { listWmMaterialStock, getStockByBatchCode } from '@/api/mes/wm/material_stock'
import { parseQrPayload } from '@/utils/qrPayload'
import type { WmProductSales, WmProductSalesLine, WmProductSalesDetail, WmMaterialStock } from '@/types'

const proxy = getCurrentInstance()?.proxy as any
const emit = defineEmits<{ success: [] }>()

const show = ref(false)
const submitting = ref(false)
const header = ref<WmProductSales | null>(null)
const lines = ref<WmProductSalesLine[]>([])
const details = ref<WmProductSalesDetail[]>([])
const historyDetails = ref<WmProductSalesDetail[]>([])
// itemId → 可用量（本仓库聚合）
const availMap = ref<Record<number, number>>({})
// 批次码识别输入（扫码枪/手输）
const scanCode = ref('')
const scanInputRef = ref()

const postableLines = computed(() => lines.value.filter((l: WmProductSalesLine) => remain(l) > 0))

function remain(row: WmProductSalesLine): number {
  const need = Number(row.quantitySales || 0)
  const posted = Number(row.quantityPosted || 0)
  return Math.max(0, need - posted)
}

function availOf(row: WmProductSalesLine): number | null {
  return row.itemId ? (availMap.value[row.itemId] ?? null) : null
}
function availColor(row: WmProductSalesLine): string {
  const a = availOf(row)
  if (a == null) return '#909399'
  return a < remain(row) ? '#F56C6C' : '#67C23A'
}
const insufficientHint = computed(() => {
  const lack = postableLines.value.filter((l: WmProductSalesLine) => {
    const a = availOf(l); return a != null && a < remain(l)
  })
  if (!lack.length) return ''
  return `以下物料在仓库[${header.value?.warehouseName || ''}]可用库存不足：${lack.map((l: WmProductSalesLine) => l.itemCode).join('、')}。请先入库或调拨，否则出库将失败。`
})

/** 拉取本单各 item 在指定仓库的可用量聚合 */
function loadAvail(warehouseId: number | undefined, items: WmProductSalesLine[]) {
  availMap.value = {}
  if (!warehouseId || !items.length) return
  const itemIds = [...new Set(items.map(i => i.itemId).filter(Boolean))]
  Promise.all(itemIds.map(id =>
    listWmMaterialStock({ itemId: id, warehouseId } as any).then((r: any) => {
      const rows = r.rows || []
      const sum = rows.reduce((s: number, x: any) => s + Number(x.quantityAvailable || 0), 0)
      availMap.value[id as number] = sum
    }).catch(() => { availMap.value[id as number] = 0 })
  ))
}

/** 打开出库确认弹窗：传入出库单头，内部查详情 */
function open(headerData: WmProductSales) {
  header.value = headerData
  details.value = []
  show.value = true
  getSalesDetail(headerData.salesId).then(r => {
    const d = r.data
    if (!d) return
    header.value = d
    lines.value = d.lines || []
    historyDetails.value = d.details || []
    loadAvail(d.warehouseId, d.lines || [])
  })
}

function addDetailRow() {
  const first = postableLines.value[0]
  details.value.push({
    lineId: first?.lineId,
    itemId: first?.itemId,
    itemCode: first?.itemCode || '',
    itemName: first?.itemName || '',
    unitOfMeasure: first?.unitOfMeasure || '',
    unitName: first?.unitName || '',
    batchId: undefined,
    batchCode: '',
    quantity: first ? remain(first) : 0
  } as unknown as WmProductSalesDetail)
}

function onLineChange(idx: number, lineId: number) {
  const line = lines.value.find((l: WmProductSalesLine) => l.lineId === lineId)
  if (line) {
    Object.assign(details.value[idx], {
      itemId: line.itemId, itemCode: line.itemCode, itemName: line.itemName,
      unitOfMeasure: line.unitOfMeasure, unitName: line.unitName,
      quantity: remain(line)
    })
  }
}

/** 自动 FIFO：为每个未出完的行按剩余量填一行 */
function autoFifoAll() {
  details.value = []
  for (const line of postableLines.value as WmProductSalesLine[]) {
    const r = remain(line)
    if (r > 0) {
      details.value.push({
        lineId: line.lineId, itemId: line.itemId, itemCode: line.itemCode,
        itemName: line.itemName, unitOfMeasure: line.unitOfMeasure, unitName: line.unitName,
        batchId: undefined, batchCode: '', quantity: r
      } as unknown as WmProductSalesDetail)
    }
  }
  if (!details.value.length) proxy.$modal.msgWarning('没有待出库的物料')
  else proxy.$modal.msgSuccess('已按 FIFO 填充，批次由系统自动匹配')
}

/** 从扫码原文提取批次码：QXX|MAT|xxx → xxx；其余按裸批次码处理，非 MAT 码返回 null */
function extractBatchCode(raw: string): string | null {
  const parsed = parseQrPayload(raw)
  return parsed ? (parsed.type === 'MAT' ? parsed.code : null) : raw
}

/** 该批次多仓时取出库仓库内可用量最大的一行；批次不在出库仓库返回 null */
function pickStockRow(rows: WmMaterialStock[]): WmMaterialStock | null {
  const inWh = rows.filter(r => r.warehouseId === header.value?.warehouseId)
  if (!inWh.length) return null
  return inWh.reduce((best, r) => Number(r.quantityOnhand || 0) > Number(best.quantityOnhand || 0) ? r : best)
}

/** 批次码识别：反查库存 → 按 itemCode 匹配待出库行 → 自动添加并预填一条明细 */
async function handleScan() {
  const raw = scanCode.value.trim()
  if (!raw) return
  const code = extractBatchCode(raw)
  if (!code) { proxy.$modal.msgError(`二维码非物料批次码（MAT）：${raw}`); scanCode.value = ''; return }
  try {
    const res = await getStockByBatchCode(code)
    const rows = res.data || []
    if (!rows.length) { proxy.$modal.msgError(`批次[${code}]不存在或无库存记录`); return }
    const row = pickStockRow(rows)
    if (!row) { proxy.$modal.msgError(`批次[${code}]在出库仓库[${header.value?.warehouseName || ''}]无库存`); return }
    if (Number(row.quantityOnhand || 0) <= 0) { proxy.$modal.msgError(`批次[${code}]库存量为 0，无法出库`); return }
    const line = postableLines.value.find(l => l.itemCode === row.itemCode)
    if (!line) { proxy.$modal.msgError(`批次[${code}]物料[${row.itemCode}]不在待出库行或已出库完成`); return }
    const qty = Math.min(remain(line), Number(row.quantityOnhand || 0))
    details.value.push({
      lineId: line.lineId, itemId: line.itemId, itemCode: line.itemCode, itemName: line.itemName,
      unitOfMeasure: line.unitOfMeasure, unitName: line.unitName,
      batchId: row.batchId, batchCode: row.batchCode || code, quantity: qty
    } as unknown as WmProductSalesDetail)
    proxy.$modal.msgSuccess(`已添加：${line.itemCode} × ${qty}（批次 ${row.batchCode || code}）`)
  } finally {
    scanCode.value = ''
    scanInputRef.value?.focus?.()
  }
}

/** 提交前补齐"有 batchCode 无 batchId"明细的 batchId，使其走后端精确批次扣减（修存量 bug）；全部可补齐返回 true */
async function fillMissingBatchIds(valid: WmProductSalesDetail[]): Promise<boolean> {
  const lack = valid.filter(d => d.batchCode && !d.batchId)
  if (!lack.length) return true
  const resList = await Promise.all(lack.map(d =>
    getStockByBatchCode(d.batchCode as string).then(r => ({ d, rows: r.data || [] }))))
  for (const { d, rows } of resList) {
    const row = pickStockRow(rows)
    if (!row || row.itemId !== d.itemId) { proxy.$modal.msgError(`批次[${d.batchCode}]在出库仓库[${header.value?.warehouseName || ''}]不存在或不属于物料[${d.itemCode}]，无法出库`); return false }
    d.batchId = row.batchId
  }
  return true
}

async function handleConfirm() {
  const valid = details.value.filter((d: WmProductSalesDetail) => d.lineId && Number(d.quantity) > 0)
  if (!valid.length) { proxy.$modal.msgError('请添加有效的出库明细'); return }
  for (const d of valid) {
    const line = lines.value.find((l: WmProductSalesLine) => l.lineId === d.lineId)
    if (line && Number(d.quantity) > remain(line)) {
      proxy.$modal.msgError(`物料[${d.itemCode}]本次出库量(${d.quantity})超过未出库量(${remain(line)})`)
      return
    }
  }
  submitting.value = true
  try {
    if (!await fillMissingBatchIds(valid)) return
    await postOut(header.value!.salesId, valid)
    proxy.$modal.msgSuccess('过账出库成功')
    show.value = false
    emit('success')
  } catch (e: any) {
    // 后端会返回具体批次/库存不足信息，统一 toast 已由 axios 拦截器处理；此处可补充刷新可用量
    loadAvail(header.value?.warehouseId, lines.value)
  } finally { submitting.value = false }
}

function handleClose() { details.value = []; historyDetails.value = []; scanCode.value = '' }

defineExpose({ open })
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
.ml8 { margin-left: 8px; }
.mt8 { margin-top: 8px; }
.scan-bar { display: flex; align-items: center; gap: 8px; }
.scan-tip { color: #909399; font-size: 12px; }
</style>
