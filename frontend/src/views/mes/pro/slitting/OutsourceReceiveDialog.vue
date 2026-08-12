<template>
  <el-dialog v-model="visible" title="外协收货" width="900px" :close-on-click-modal="false" append-to-body @close="cancel">
    <el-descriptions v-if="record" :column="3" border size="small" class="mb8">
      <el-descriptions-item label="分切批次号">{{ record.slitBatchNo }}</el-descriptions-item>
      <el-descriptions-item label="外协厂商">{{ record.vendorName }}</el-descriptions-item>
      <el-descriptions-item label="母卷号">{{ record.parentRollCode }}</el-descriptions-item>
      <el-descriptions-item label="母卷物料">{{ record.parentItemName }}</el-descriptions-item>
      <el-descriptions-item label="母卷门幅">{{ record.parentWidth || '-' }}mm</el-descriptions-item>
      <el-descriptions-item label="母卷重量">{{ record.parentWeight }}吨</el-descriptions-item>
    </el-descriptions>

    <div class="mb8"><span style="font-weight:600">子卷明细（厂商录结果）</span></div>
    <el-table :data="record?.childRolls || []" border size="small" max-height="240" class="mb8">
      <el-table-column label="子卷号" prop="rollCode" width="150" align="center" />
      <el-table-column label="门幅(mm)" prop="actualWidth" width="90" align="center" />
      <el-table-column label="克重(g)" prop="actualWeightGsm" width="90" align="center" />
      <el-table-column label="长度(m)" prop="actualLength" width="90" align="center" />
      <el-table-column label="重量(吨)" prop="actualWeight" width="100" align="center" />
      <el-table-column label="状态" prop="status" width="90" align="center" />
    </el-table>

    <el-divider content-position="left">收货入库（可选调整）</el-divider>
    <el-form label-width="100px">
      <el-row>
        <el-col :span="12">
          <el-form-item label="收货仓库" required>
            <el-input v-model="receiveWarehouseName" placeholder="点击右侧按钮选择" readonly>
              <template #append>
                <el-button icon="Search" @click="whSelectRef?.open()" />
              </template>
            </el-input>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="纸边/边角料">
            <el-input v-model="edgeItemName" placeholder="可选，点击右侧按钮选择" readonly>
              <template #append>
                <el-button icon="Search" @click="edgeItemSelectRef?.open()" />
              </template>
            </el-input>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row v-if="edgeItemId">
        <el-col :span="12">
          <el-form-item label="纸边重量(kg)">
            <el-input-number v-model="edgeWeight" :min="0" :precision="2" style="width: 100%" />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <el-alert :title="weightCheckText" :type="weightCheckType" show-icon :closable="false" class="mb8" />

    <WarehouseSelect ref="whSelectRef" @onSelected="onWarehouseSelected" />
    <ItemSelect ref="edgeItemSelectRef" @onSelected="onEdgeItemSelected" />

    <template #footer>
      <el-button @click="cancel">取消</el-button>
      <el-button type="primary" :loading="submitting" :disabled="!canReceive" @click="submit">确认收货</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, getCurrentInstance } from 'vue'
import { getSlitting, receiveOutsource } from '@/api/mes/pro/slitting'
import WarehouseSelect from '@/components/warehouseSelect/single.vue'
import ItemSelect from '@/components/itemSelect/single.vue'

const { proxy } = getCurrentInstance() as any
const emit = defineEmits(['success'])

const visible = ref(false)
const submitting = ref(false)
const record = ref<any>(null)
const whSelectRef = ref()
const edgeItemSelectRef = ref()

const receiveWarehouseId = ref<number | null>(null)
const receiveWarehouseCode = ref('')
const receiveWarehouseName = ref('')
const edgeItemId = ref<number | null>(null)
const edgeItemCode = ref('')
const edgeItemName = ref('')
const edgeWeight = ref(0)

// 子卷总重（厂商录入）
const childTotalWeight = computed(() => {
  return (record.value?.childRolls || []).reduce((sum: number, c: any) => sum + Number(c.actualWeight || 0), 0)
})

const parentWeight = computed(() => Number(record.value?.parentWeight || 0))

const edgeWeightTon = computed(() => Number(edgeWeight.value || 0) / 1000)
const lossWeight = computed(() => parentWeight.value - childTotalWeight.value - edgeWeightTon.value)
const lossRate = computed(() => parentWeight.value > 0 ? (lossWeight.value / parentWeight.value * 100) : 0)

const weightValid = computed(() => lossWeight.value >= 0 && lossRate.value <= 3)

const weightCheckText = computed(() => {
  if (!record.value) return ''
  const loss = lossWeight.value.toFixed(4)
  const rate = lossRate.value.toFixed(2)
  if (lossWeight.value < 0) return `⚠️ 子卷+纸边总重(${(childTotalWeight.value + edgeWeightTon.value).toFixed(4)}吨)超过母卷重量(${parentWeight.value}吨)！`
  if (lossRate.value > 3) return `⚠️ 损耗率 ${rate}% 超过 3% 上限（损耗 ${loss} 吨），请检查录入数据`
  return `✓ 母卷: ${parentWeight.value}吨 | 子卷: ${childTotalWeight.value.toFixed(4)}吨 + 纸边: ${edgeWeightTon.value.toFixed(4)}吨 = ${(childTotalWeight.value + edgeWeightTon.value).toFixed(4)}吨 | 损耗: ${loss}吨 (${rate}%)`
})
const weightCheckType = computed(() => weightValid.value ? 'success' : 'error')

const canReceive = computed(() => {
  return !!record.value && !!receiveWarehouseId.value && weightValid.value
})

async function open(row: any) {
  visible.value = true
  resetForm()
  const res = await getSlitting(row.slitId)
  record.value = res.data
}

function resetForm() {
  record.value = null
  receiveWarehouseId.value = null
  receiveWarehouseCode.value = ''
  receiveWarehouseName.value = ''
  edgeItemId.value = null
  edgeItemCode.value = ''
  edgeItemName.value = ''
  edgeWeight.value = 0
}

function onWarehouseSelected(row: any) {
  if (!row) return
  receiveWarehouseId.value = row.warehouseId
  receiveWarehouseCode.value = row.warehouseCode
  receiveWarehouseName.value = row.warehouseName
}

function onEdgeItemSelected(row: any) {
  if (!row) return
  edgeItemId.value = row.itemId
  edgeItemCode.value = row.itemCode
  edgeItemName.value = row.itemName
}

async function submit() {
  if (!record.value) return
  if (!receiveWarehouseId.value) {
    proxy.$modal.msgWarning('请选择收货仓库')
    return
  }
  submitting.value = true
  try {
    await receiveOutsource(record.value.slitId, {
      receiveWarehouseId: receiveWarehouseId.value,
      receiveWarehouseCode: receiveWarehouseCode.value,
      receiveWarehouseName: receiveWarehouseName.value,
      edgeItemId: edgeItemId.value,
      edgeItemCode: edgeItemCode.value,
      edgeItemName: edgeItemName.value,
      edgeWeight: edgeWeight.value
    })
    proxy.$modal.msgSuccess('收货成功，子卷已入库')
    visible.value = false
    emit('success')
  } catch (e) {
    // request 拦截器已处理错误提示
  } finally {
    submitting.value = false
  }
}

function cancel() {
  visible.value = false
  resetForm()
}

defineExpose({ open })
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
</style>
