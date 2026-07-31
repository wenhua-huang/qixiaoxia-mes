<template>
  <el-dialog v-model="visible" title="执行分切" width="900px" :close-on-click-modal="false" append-to-body @close="cancel">
    <el-steps :active="step" align-center class="mb16">
      <el-step title="领料出库" description="选物料·查库存·填领料量" />
      <el-step title="分切方案" description="录子卷规格" />
      <el-step title="确认提交" description="校验·执行" />
    </el-steps>

    <!-- 步骤1：领料 -->
    <div v-show="step === 0">
      <el-form label-width="110px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="母卷物料" required>
              <el-input v-model="form.sourceItemName" placeholder="点击右侧按钮选择物料" readonly>
                <template #append>
                  <el-button icon="Search" @click="sourceItemSelectRef?.open()" />
                </template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="生产工单">
              <el-input v-model="form.workorderCode" placeholder="可选，点击右侧按钮选择" readonly>
                <template #append>
                  <el-button icon="Search" @click="woSelectRef?.open()" />
                </template>
              </el-input>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item v-if="form.sourceItemId" label="在库库存">
          <el-table :data="stockList" border size="small" max-height="200" highlight-current-row @current-change="onStockChange">
            <el-table-column label="" width="50" align="center">
              <template #default="{ row }">
                <el-radio v-model="selectedStockId" :value="row.materialStockId" @change="onStockChange(row)">&nbsp;</el-radio>
              </template>
            </el-table-column>
            <el-table-column label="仓库" prop="warehouseName" align="center" />
            <el-table-column label="批次号" prop="batchCode" align="center" />
            <el-table-column label="可用量" prop="quantityAvailable" align="center" width="120">
              <template #default="{ row }">{{ row.quantityAvailable }} {{ row.unitOfMeasure }}</template>
            </el-table-column>
          </el-table>
        </el-form-item>

        <el-row>
          <el-col :span="8">
            <el-form-item label="领料数量(吨)" required>
              <el-input-number v-model="form.pickQty" :min="0" :precision="4" style="width: 100%" :max="maxPickQty" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="工序">
              <el-select v-model="form.processId" placeholder="可选" filterable clearable style="width: 100%" @change="onProcessChange">
                <el-option v-for="p in processOptions" :key="p.processId" :label="p.processName" :value="p.processId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="工作站">
              <el-input v-model="form.workstationName" placeholder="点击右侧按钮选择" readonly>
                <template #append>
                  <el-button icon="Search" @click="wsSelectRef?.open()" />
                </template>
              </el-input>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </div>

    <!-- 步骤2：分切方案 -->
    <div v-show="step === 1">
      <el-descriptions :column="3" border size="small" class="mb8">
        <el-descriptions-item label="领料物料">{{ form.sourceItemName }}</el-descriptions-item>
        <el-descriptions-item label="领料数量">{{ form.pickQty }} 吨</el-descriptions-item>
        <el-descriptions-item label="出库仓库">{{ selectedStock?.warehouseName || '-' }}</el-descriptions-item>
      </el-descriptions>

      <div class="mb8"><span style="font-weight:600">子卷规格</span></div>
      <el-table :data="form.childRolls" border size="small" max-height="240">
        <el-table-column label="#" type="index" width="50" align="center" />
        <el-table-column label="物料名称" align="center" min-width="140">
          <template #default="{ row }">
            <el-input v-model="row.itemName" size="small" placeholder="默认继承母卷" />
          </template>
        </el-table-column>
        <el-table-column label="门幅(mm)" align="center" width="110">
          <template #default="{ row }">
            <el-input v-model="row.actualWidth" size="small" placeholder="如400" />
          </template>
        </el-table-column>
        <el-table-column label="长度(m)" align="center" width="110">
          <template #default="{ row }">
            <el-input-number v-model="row.actualLength" :min="0" size="small" style="width: 100%" :precision="2" />
          </template>
        </el-table-column>
        <el-table-column label="重量(吨)" align="center" width="120">
          <template #default="{ row }">
            <el-input-number v-model="row.actualWeight" :min="0" size="small" style="width: 100%" :precision="4" />
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="70">
          <template #default="{ $index }">
            <el-button link type="danger" size="small" icon="Delete" @click="removeChild($index)" />
          </template>
        </el-table-column>
      </el-table>
      <el-button type="primary" plain icon="Plus" size="small" class="mt8" @click="addChild">添加子卷</el-button>

      <el-divider content-position="left">纸边/损耗（可选）</el-divider>
      <el-row>
        <el-col :span="12">
          <el-form-item label="纸边物料">
            <el-input v-model="form.edgeItemName" placeholder="点击右侧按钮选择物料" readonly>
              <template #append>
                <el-button icon="Search" @click="edgeItemSelectRef?.open()" />
              </template>
            </el-input>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="纸边重量(kg)">
            <el-input-number v-model="form.edgeWeight" :min="0" size="default" style="width: 100%" :precision="2" />
          </el-form-item>
        </el-col>
      </el-row>
    </div>

    <!-- 步骤3：确认 -->
    <div v-show="step === 2">
      <el-descriptions :column="2" border size="small" class="mb8">
        <el-descriptions-item label="领料物料">{{ form.sourceItemName }}</el-descriptions-item>
        <el-descriptions-item label="领料数量">{{ form.pickQty }} 吨</el-descriptions-item>
        <el-descriptions-item label="子卷数量">{{ form.childRolls.length }} 卷</el-descriptions-item>
        <el-descriptions-item label="子卷总重">{{ childTotalWeight.toFixed(4) }} 吨</el-descriptions-item>
        <el-descriptions-item label="纸边重量">{{ form.edgeWeight || 0 }} kg</el-descriptions-item>
        <el-descriptions-item label="出库仓库">{{ selectedStock?.warehouseName || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-alert :title="weightCheckText" :type="weightCheckType" show-icon :closable="false" />
    </div>

    <ItemSelect ref="sourceItemSelectRef" @onSelected="onSourceItemSelected" />
    <ItemSelect ref="edgeItemSelectRef" @onSelected="onEdgeItemSelected" />
    <WorkorderSelect ref="woSelectRef" @onSelected="onWorkorderSelected" />
    <WorkstationSelect ref="wsSelectRef" @onSelected="onWorkstationSelected" />
    <template #footer>
      <el-button @click="cancel">取消</el-button>
      <el-button v-if="step > 0" @click="step--">上一步</el-button>
      <el-button v-if="step < 2" type="primary" :disabled="!canNext" @click="next">下一步</el-button>
      <el-button v-if="step === 2" type="primary" :loading="submitting" :disabled="!weightValid" @click="submit">确认分切</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, getCurrentInstance } from 'vue'
import { executeSlitting, listAvailableStock } from '@/api/mes/pro/slitting'
import { listAllProcess } from '@/api/mes/pro/process'
import WorkorderSelect from '@/components/workorderSelect/single.vue'
import WorkstationSelect from '@/components/workstationSelect/single.vue'
import ItemSelect from '@/components/itemSelect/single.vue'

const { proxy } = getCurrentInstance() as any
const emit = defineEmits(['success'])

const visible = ref(false)
const submitting = ref(false)
const step = ref(0)
const sourceItemSelectRef = ref()
const edgeItemSelectRef = ref()
const woSelectRef = ref()
const wsSelectRef = ref()
const stockList = ref<any[]>([])
const selectedStockId = ref<number>()
const selectedStock = ref<any>(null)
const processOptions = ref<any[]>([])

const form = reactive<any>({
  workorderId: null, workorderCode: '', processId: null, processCode: '', processName: '',
  cardId: null, routeId: null, workstationId: null, workstationCode: '', workstationName: '',
  sourceItemId: null, sourceItemCode: '', sourceItemName: '',
  sourceWarehouseId: null, sourceWarehouseCode: '', sourceWarehouseName: '',
  sourceBatchId: null, sourceBatchCode: '',
  pickQty: 0,
  childRolls: [], edgeItemId: null, edgeItemCode: '', edgeItemName: '', edgeWeight: 0,
  remark: ''
})

const maxPickQty = computed(() => {
  const v = selectedStock.value?.quantityAvailable
  return v != null ? Number(v) : undefined
})

const childTotalWeight = computed(() => {
  return (form.childRolls || []).reduce((sum: number, c: any) => sum + (c.actualWeight || 0), 0)
})

const edgeWeightTon = computed(() => (form.edgeWeight || 0) / 1000)
const lossWeight = computed(() => form.pickQty - childTotalWeight.value - edgeWeightTon.value)
const lossRate = computed(() => form.pickQty > 0 ? (lossWeight.value / form.pickQty * 100) : 0)

// 纸边校验：① 物料不能等于领料母卷物料 ② 量级合理性（占领料量 ≤ 20%）
const edgeError = computed(() => {
  if (!form.edgeItemId) return ''
  if (form.edgeItemId === form.sourceItemId) return '⚠️ 纸边物料不能与领料母卷物料相同，请选边角料/废料物料'
  if (form.edgeWeight > 0 && form.pickQty > 0) {
    const ratio = (form.edgeWeight / 1000) / form.pickQty * 100
    if (ratio > 20) return `⚠️ 纸边占领料量 ${ratio.toFixed(2)}% 过高，请检查单位（纸边kg，领料吨）`
  }
  return ''
})

const weightValid = computed(() => lossWeight.value >= 0 && lossRate.value <= 3 && !edgeError.value)

const weightCheckText = computed(() => {
  if (edgeError.value) return edgeError.value
  if (!form.pickQty) return '请先填写领料数量'
  const loss = lossWeight.value.toFixed(4)
  const rate = lossRate.value.toFixed(2)
  if (lossWeight.value < 0) return `⚠️ 子卷+纸边总重(${(childTotalWeight.value + edgeWeightTon.value).toFixed(4)}吨)超过领料量(${form.pickQty}吨)！`
  if (lossRate.value > 3) return `⚠️ 损耗率 ${rate}% 超过 3% 上限（损耗 ${loss} 吨），请检查录入数据`
  return `✓ 领料: ${form.pickQty}吨 | 子卷: ${childTotalWeight.value.toFixed(4)}吨 + 纸边: ${edgeWeightTon.value.toFixed(4)}吨 = ${(childTotalWeight.value + edgeWeightTon.value).toFixed(4)}吨 | 损耗: ${loss}吨 (${rate}%)`
})

const weightCheckType = computed(() => weightValid.value ? 'success' : 'error')

// 步骤1可下一步：已选物料+已选库存+领料量>0
const canNext = computed(() => {
  if (step.value === 0) return !!form.sourceItemId && !!selectedStock.value && form.pickQty > 0
  if (step.value === 1) return form.childRolls.length > 0 && !edgeError.value
  return true
})

async function open() {
  visible.value = true
  step.value = 0
  resetForm()
  // 加载工序列表（仅加载一次，避免重复请求）
  if (processOptions.value.length === 0) {
    const res = await listAllProcess()
    processOptions.value = res.data || []
  }
}

function resetForm() {
  Object.assign(form, {
    workorderId: null, workorderCode: '', processId: null, processCode: '', processName: '',
    cardId: null, routeId: null, workstationId: null, workstationCode: '', workstationName: '',
    sourceItemId: null, sourceItemCode: '', sourceItemName: '',
    sourceWarehouseId: null, sourceWarehouseCode: '', sourceWarehouseName: '',
    sourceBatchId: null, sourceBatchCode: '',
    pickQty: 0,
    childRolls: [], edgeItemId: null, edgeItemCode: '', edgeItemName: '', edgeWeight: 0,
    remark: ''
  })
  stockList.value = []
  selectedStockId.value = undefined
  selectedStock.value = null
}

async function onSourceItemSelected(row: any) {
  if (!row) return
  form.sourceItemId = row.itemId
  form.sourceItemCode = row.itemCode
  form.sourceItemName = row.itemName
  // 切换物料时清空已选库存/领料量，避免残留物料A的数据带到物料B
  selectedStock.value = null
  selectedStockId.value = undefined
  form.pickQty = 0
  form.sourceWarehouseId = null
  form.sourceWarehouseCode = ''
  form.sourceWarehouseName = ''
  form.sourceBatchId = null
  form.sourceBatchCode = ''
  const res = await listAvailableStock(row.itemId)
  stockList.value = res.data || []
}

function onStockChange(row: any) {
  if (!row) return
  selectedStock.value = row
  selectedStockId.value = row.materialStockId
  form.sourceWarehouseId = row.warehouseId
  form.sourceWarehouseCode = row.warehouseCode
  form.sourceWarehouseName = row.warehouseName
  form.sourceBatchId = row.batchId
  form.sourceBatchCode = row.batchCode
}

function onWorkorderSelected(row: any) {
  if (!row) return
  form.workorderId = row.workorderId
  form.workorderCode = row.workorderCode
}

function onProcessChange(processId: number) {
  const p = processOptions.value.find((x: any) => x.processId === processId)
  if (p) {
    form.processCode = p.processCode
    form.processName = p.processName
  } else {
    form.processCode = ''
    form.processName = ''
  }
}

function onWorkstationSelected(row: any) {
  if (!row) return
  form.workstationId = row.workstationId
  form.workstationCode = row.workstationCode
  form.workstationName = row.workstationName
}

function onEdgeItemSelected(row: any) {
  if (!row) return
  form.edgeItemId = row.itemId
  form.edgeItemCode = row.itemCode
  form.edgeItemName = row.itemName
}

function next() {
  if (step.value === 0 && form.childRolls.length === 0) {
    addChild()  // 自动加一行子卷
  }
  step.value++
}

function addChild() {
  if (!form.childRolls) form.childRolls = []
  form.childRolls.push({
    itemId: form.sourceItemId || null,
    itemCode: form.sourceItemCode || '',
    itemName: form.sourceItemName || '',
    actualWidth: '', actualLength: 0, actualWeight: 0
  })
}

function removeChild(index: number) {
  form.childRolls.splice(index, 1)
}

async function submit() {
  if (!form.childRolls || form.childRolls.length === 0) {
    proxy.$modal.msgWarning('请至少添加一条子卷规格')
    return
  }
  if (!weightValid.value) {
    proxy.$modal.msgWarning('重量校验未通过，请检查领料量与子卷/纸边重量')
    return
  }
  submitting.value = true
  try {
    await executeSlitting(form)
    proxy.$modal.msgSuccess('分切执行成功')
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
.mt8 { margin-top: 8px; }
.mb8 { margin-bottom: 8px; }
.mb16 { margin-bottom: 16px; }
</style>
