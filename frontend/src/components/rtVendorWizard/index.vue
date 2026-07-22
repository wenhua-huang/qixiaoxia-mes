<template>
  <el-dialog title="从采购订单生成退货单" v-model="show" width="1100px" append-to-body :close-on-click-modal="false" @close="onClose">
    <el-steps :active="step - 1" finish-status="success" style="margin-bottom:20px">
      <el-step title="选择可退批次" />
      <el-step title="确认并生成" />
    </el-steps>

    <!-- Step 1: 选 PO(非预填模式才显示) + 拉可退批次 + 勾选填数量 -->
    <div v-show="step === 1">
      <el-form :model="headerForm" label-width="100px">
        <el-row>
          <el-col :span="8">
            <el-form-item label="采购订单" prop="purOrderCode">
              <el-input v-model="headerForm.purOrderCode" readonly placeholder="请选择采购订单">
                <template #append v-if="!isPrefilled">
                  <el-button icon="Search" @click="handleSelectPo" />
                </template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="供应商">
              <el-input v-model="headerForm.vendorName" readonly />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="退货单号" prop="rtCode">
              <el-input v-model="headerForm.rtCode" placeholder="请输入或自动生成">
                <template #append>
                  <el-button icon="Refresh" @click="autoGenCode" :loading="genLoading">生成</el-button>
                </template>
              </el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="16">
            <el-form-item label="退货单名称">
              <el-input v-model="headerForm.rtName" placeholder="可空,默认 退货-采购订单号" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <el-table v-loading="batchLoading" :data="batches" border @selection-change="handleBatchSelect" style="margin-top:8px" size="small">
        <el-table-column type="selection" width="45" align="center" :selectable="(r: any): boolean => Number(r.quantityReturnable) > 0" />
        <el-table-column label="物料编码" align="center" prop="itemCode" width="130" />
        <el-table-column label="物料名称" align="center" prop="itemName" width="150" :show-overflow-tooltip="true" />
        <el-table-column label="规格" align="center" prop="specification" width="110" :show-overflow-tooltip="true" />
        <el-table-column label="仓库" align="center" prop="warehouseName" width="100" :show-overflow-tooltip="true" />
        <el-table-column label="批次号" align="center" prop="batchCode" width="110" :show-overflow-tooltip="true" />
        <el-table-column label="有效期" align="center" prop="expireDate" width="110" />
        <el-table-column label="原入库单" align="center" prop="recptCode" width="120" :show-overflow-tooltip="true" />
        <el-table-column label="已收" align="center" prop="quantityRecptTotal" width="75" />
        <el-table-column label="已退" align="center" prop="quantityReturned" width="70" />
        <el-table-column label="可退" align="center" width="70">
          <template #default="s"><span :style="{ color: Number(s.row.quantityReturnable) > 0 ? '' : '#c0c4cc', fontWeight: 'bold' }">{{ s.row.quantityReturnable }}</span></template>
        </el-table-column>
        <el-table-column label="本次退货" align="center" width="140">
          <template #default="s">
            <el-input-number v-if="selectedMap[batchKey(s.row)] !== undefined" v-model="selectedMap[batchKey(s.row)]" :min="0" :max="Number(s.row.quantityReturnable)" :precision="4" size="small" style="width:120px" />
            <span v-else style="color:#c0c4cc">—</span>
          </template>
        </el-table-column>
      </el-table>
      <el-alert v-if="headerForm.purOrderId && batches.length > 0 && !batches.some(r => Number(r.quantityReturnable) > 0)" type="warning" :closable="false" show-icon title="该采购订单的所有批次均已退完，无可退数量" style="margin-top:8px" />
    </div>

    <!-- Step 2: 确认汇总 -->
    <div v-show="step === 2">
      <el-descriptions :column="2" border size="small" title="退货单头">
        <el-descriptions-item label="退货单号">{{ headerForm.rtCode }}</el-descriptions-item>
        <el-descriptions-item label="采购订单">{{ headerForm.purOrderCode }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ headerForm.vendorName }}</el-descriptions-item>
        <el-descriptions-item label="退货单名称">{{ headerForm.rtName || ('退货-' + headerForm.purOrderCode) }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="confirmLines" border style="margin-top:12px" size="small">
        <el-table-column label="物料编码" align="center" prop="itemCode" width="130" />
        <el-table-column label="物料名称" align="center" prop="itemName" width="180" :show-overflow-tooltip="true" />
        <el-table-column label="仓库" align="center" prop="warehouseName" width="110" />
        <el-table-column label="批次号" align="center" prop="batchCode" width="120" />
        <el-table-column label="可退量" align="center" prop="quantityReturnable" width="100" />
        <el-table-column label="本次退货数量" align="center" width="140">
          <template #default="s"><span style="color:#e6a23c;font-weight:bold">{{ s.row.quantityRt }}</span></template>
        </el-table-column>
      </el-table>
    </div>

    <template #footer>
      <el-button @click="step > 1 ? step-- : onClose()">{{ step === 1 ? '取消' : '上一步' }}</el-button>
      <el-button v-if="step === 1" type="primary" @click="goNext" :disabled="!canGoNext">下一步</el-button>
      <el-button v-if="step === 2" type="primary" @click="submit" :loading="submitting">确 定</el-button>
    </template>

    <PurOrderSelect v-if="!isPrefilled" ref="purOrderSelectRef" @onSelected="onPoSelected" />
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { getCurrentInstance } from 'vue'
import type { PurOrder, ReturnableBatch, RtVendorFromPurOrderRequest } from '@/types'
import { listReturnableBatches, createRtVendorFromPurOrder } from '@/api/mes/wm/rt_vendor'
import { genSerialCode } from '@/api/mes/sys/autocoderule'
import PurOrderSelect from '@/components/purOrderSelect/single.vue'

const { proxy } = getCurrentInstance() as any

const props = defineProps<{
  modelValue: boolean
  /** 预填的采购订单;传入时跳过"选 PO",直接拉可退批次 */
  purOrder?: { orderId: number; orderCode?: string; vendorName?: string } | null
}>()
const emit = defineEmits<{ 'update:modelValue': [boolean]; success: [] }>()

const show = computed({
  get: () => props.modelValue,
  set: (v: boolean) => emit('update:modelValue', v)
})

const isPrefilled = computed(() => !!props.purOrder?.orderId)

const step = ref(1)
const batchLoading = ref(false)
const genLoading = ref(false)
const submitting = ref(false)
const purOrderSelectRef = ref()
const batches = ref<ReturnableBatch[]>([])
// batchKey -> 退货数量
const selectedMap = reactive<Record<string, number>>({})

const headerForm = reactive({
  purOrderId: undefined as number | undefined,
  purOrderCode: '',
  vendorName: '',
  rtCode: '',
  rtName: ''
})

const confirmLines = computed(() => {
  return batches.value
    .filter(r => selectedMap[batchKey(r)] !== undefined && Number(selectedMap[batchKey(r)]) > 0)
    .map(r => ({
      ...r,
      quantityRt: selectedMap[batchKey(r)]
    }))
})

const canGoNext = computed(() => {
  if (!headerForm.rtCode) return false
  return confirmLines.value.length > 0
})

function batchKey(r: ReturnableBatch): string {
  const bk = r.batchId ?? 0
  return r.itemId + ':' + r.warehouseId + ':' + bk
}

function onClose() {
  resetAll()
  show.value = false
}

function resetAll() {
  step.value = 1
  batches.value = []
  Object.keys(selectedMap).forEach(k => delete selectedMap[k])
  Object.assign(headerForm, { purOrderId: undefined, purOrderCode: '', vendorName: '', rtCode: '', rtName: '' })
}

// 弹窗打开时:预填模式直接载入,否则等用户选 PO
watch(() => props.modelValue, (open) => {
  if (open) {
    if (props.purOrder?.orderId) {
      onPoSelected(props.purOrder)
    }
  }
})

// 选择采购订单
function handleSelectPo() { purOrderSelectRef.value?.open() }
function onPoSelected(row: { orderId: number; orderCode?: string; vendorName?: string } | PurOrder | null) {
  if (!row) return
  const r: any = row
  // 切换 PO:清空旧勾选状态,避免跨 PO 同 (item,wh,batch) 残留数量被误提交
  Object.keys(selectedMap).forEach(k => delete selectedMap[k])
  headerForm.purOrderId = r.orderId
  headerForm.purOrderCode = r.orderCode || ''
  headerForm.vendorName = r.vendorName || ''
  loadBatches(r.orderId)
  if (!headerForm.rtCode) autoGenCode()
}
function loadBatches(orderId: number) {
  batchLoading.value = true
  batches.value = []  // 先清空,避免显示过期数据(接口失败时不残留旧批次)
  listReturnableBatches(orderId).then((res: any) => {
    batches.value = res.data || []
  }).finally(() => { batchLoading.value = false })
}

function handleBatchSelect(selection: ReturnableBatch[]) {
  const selectedKeys = new Set(selection.map(s => batchKey(s)))
  batches.value.forEach(r => {
    const k = batchKey(r)
    if (selectedKeys.has(k) && selectedMap[k] === undefined) {
      selectedMap[k] = Number(r.quantityReturnable)
    }
    if (!selectedKeys.has(k) && selectedMap[k] !== undefined) {
      delete selectedMap[k]
    }
  })
}

function autoGenCode() {
  genLoading.value = true
  genSerialCode('RT_VENDOR_NO').then((r: any) => {
    headerForm.rtCode = r.data
  }).finally(() => { genLoading.value = false })
}

function goNext() {
  if (!canGoNext.value) {
    proxy.$modal.msgWarning('请填写退货单号,并至少勾选一个批次填入退货数量')
    return
  }
  step.value = 2
}

function submit() {
  const req: RtVendorFromPurOrderRequest = {
    purOrderId: headerForm.purOrderId!,
    rtCode: headerForm.rtCode,
    rtName: headerForm.rtName || undefined,
    lines: confirmLines.value.map(l => ({
      itemId: l.itemId,
      warehouseId: l.warehouseId,
      warehouseCode: l.warehouseCode,
      warehouseName: l.warehouseName,
      batchId: l.batchId ?? undefined,
      batchCode: l.batchCode,
      purOrderLineId: l.purOrderLineId ?? undefined,
      quantityRt: l.quantityRt
    }))
  }
  submitting.value = true
  createRtVendorFromPurOrder(req).then((r: any) => {
    proxy.$modal.msgSuccess('已生成退货单: ' + r.data?.rtCode)
    emit('success')
    onClose()
  }).finally(() => { submitting.value = false })
}
</script>
