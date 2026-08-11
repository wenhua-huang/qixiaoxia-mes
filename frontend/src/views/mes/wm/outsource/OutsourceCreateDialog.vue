<template>
  <el-dialog v-model="visible" title="外协发货" width="850px" :close-on-click-modal="false" append-to-body @close="cancel">
    <el-form label-width="100px">
      <el-row>
        <el-col :span="12">
          <el-form-item label="外协厂商" required>
            <el-select v-model="form.vendorId" placeholder="请选择外协厂商" filterable style="width: 100%" @change="onVendorChange">
              <el-option v-for="v in vendorOptions" :key="v.vendorId" :label="v.vendorName" :value="v.vendorId" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="来源类型">
            <el-select v-model="form.sourceType" placeholder="请选择" style="width: 100%">
              <el-option v-for="d in mes_outsource_type" :key="d.value" :label="d.label" :value="d.value" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="12">
          <el-form-item label="生产工单">
            <el-input v-model="form.workorderCode" placeholder="可选，点击右侧选择" readonly>
              <template #append>
                <el-button icon="Search" @click="woSelectRef?.open()" />
              </template>
            </el-input>
          </el-form-item>
        </el-col>
        <el-col :span="12" v-if="form.workorderId">
          <el-form-item label="流转卡">
            <el-select v-model="form.cardId" placeholder="可选" filterable clearable style="width: 100%" @change="onCardChange">
              <el-option v-for="c in cardOptions" :key="c.cardId" :label="c.cardCode" :value="c.cardId" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row v-if="form.workorderId">
        <el-col :span="12">
          <el-form-item label="外协工序">
            <el-select v-model="form.processId" placeholder="可选" filterable clearable style="width: 100%" @change="onProcessChange">
              <el-option v-for="p in processOptions" :key="p.processId" :label="p.processName" :value="p.processId" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 发料物料行 -->
      <el-divider content-position="left">发料明细</el-divider>
      <el-table :data="form.issueLines" border size="small" max-height="280">
        <el-table-column label="物料" align="center" min-width="160">
          <template #default="{ row }">
            <el-input v-model="row.itemName" placeholder="点击右侧选择" readonly size="small">
              <template #append>
                <el-button icon="Search" size="small" @click="currentItemIdx = form.issueLines.indexOf(row); itemSelectRef?.open()" />
              </template>
            </el-input>
          </template>
        </el-table-column>
        <el-table-column label="数量" align="center" width="110">
          <template #default="{ row }">
            <el-input-number v-model="row.quantity" :min="0" :precision="4" size="small" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column label="单位" align="center" width="80">
          <template #default="{ row }">
            <el-input v-model="row.unitName" size="small" placeholder="吨" />
          </template>
        </el-table-column>
        <el-table-column label="仓库" align="center" min-width="140">
          <template #default="{ row }">
            <el-input v-model="row.warehouseName" placeholder="点击右侧选择" readonly size="small">
              <template #append>
                <el-button icon="Search" size="small" @click="currentItemIdx = form.issueLines.indexOf(row); whSelectRef?.open()" />
              </template>
            </el-input>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="60">
          <template #default="{ $index }">
            <el-button link type="danger" size="small" icon="Delete" @click="removeLine($index)" />
          </template>
        </el-table-column>
      </el-table>
      <el-button type="primary" plain icon="Plus" size="small" class="mt8" @click="addLine">添加物料</el-button>
    </el-form>

    <ItemSelect ref="itemSelectRef" @onSelected="onItemSelected" />
    <WorkorderSelect ref="woSelectRef" @onSelected="onWorkorderSelected" />
    <WarehouseSelect ref="whSelectRef" @onSelected="onWarehouseSelected" />

    <template #footer>
      <el-button @click="cancel">取消</el-button>
      <el-button type="primary" :loading="submitting" :disabled="!canSubmit" @click="submit">确认发货</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, getCurrentInstance } from 'vue'
import { createOutsource } from '@/api/mes/wm/outsource'
import { listAllVendor } from '@/api/mes/md/vendor'
import { listProcard } from '@/api/mes/pro/procard'
import { listAllProcess } from '@/api/mes/pro/process'
import ItemSelect from '@/components/itemSelect/single.vue'
import WorkorderSelect from '@/components/workorderSelect/single.vue'
import WarehouseSelect from '@/components/warehouseSelect/single.vue'

const { proxy } = getCurrentInstance() as any
const { mes_outsource_type } = proxy.useDict('mes_outsource_type')
const emit = defineEmits(['success'])

const visible = ref(false)
const submitting = ref(false)
const itemSelectRef = ref()
const woSelectRef = ref()
const whSelectRef = ref()
const currentItemIdx = ref(0)
const vendorOptions = ref<any[]>([])
const cardOptions = ref<any[]>([])
const processOptions = ref<any[]>([])

const form = reactive<any>({
  vendorId: null, vendorCode: '', vendorName: '',
  sourceType: 'GENERIC',
  workorderId: null, workorderCode: '',
  cardId: null, routeId: null,
  processId: null, processCode: '', processName: '',
  issueLines: []
})

const canSubmit = computed(() => {
  return form.vendorId != null && form.issueLines.length > 0
    && form.issueLines.every((l: any) => l.itemId && l.quantity > 0 && l.warehouseId)
})

async function open() {
  visible.value = true
  resetForm()
  if (vendorOptions.value.length === 0) {
    const res = await listAllVendor()
    vendorOptions.value = (res.data || []).filter((v: any) => v.vendorType === 'OUTSOURCE' || v.vendorType === 'BOTH')
  }
  if (processOptions.value.length === 0) {
    const res = await listAllProcess()
    processOptions.value = res.data || []
  }
}

function resetForm() {
  Object.assign(form, {
    vendorId: null, vendorCode: '', vendorName: '',
    sourceType: 'GENERIC',
    workorderId: null, workorderCode: '',
    cardId: null, routeId: null,
    processId: null, processCode: '', processName: '',
    issueLines: []
  })
  cardOptions.value = []
}

function onVendorChange(vendorId: number) {
  const v = vendorOptions.value.find((x: any) => x.vendorId === vendorId)
  if (v) { form.vendorCode = v.vendorCode; form.vendorName = v.vendorName }
}

function onWorkorderSelected(row: any) {
  if (!row) return
  form.workorderId = row.workorderId
  form.workorderCode = row.workorderCode
  // 切换工单时清空工艺路线/工序/流转卡，避免上一张工单残留
  form.routeId = row.routeId || null
  form.processId = null
  form.processCode = ''
  form.processName = ''
  form.cardId = null
  loadCards(row.workorderId)
}

async function loadCards(workorderId: number) {
  cardOptions.value = []
  form.cardId = null
  try {
    const res = await listProcard({ workorderId, status: 'ACTIVE' })
    cardOptions.value = res.rows || []
  } catch (e) {}
}

function onCardChange(cardId: number) {
  const c = cardOptions.value.find((x: any) => x.cardId === cardId)
  if (c && c.routeId) form.routeId = c.routeId
}

function onProcessChange(processId: number) {
  const p = processOptions.value.find((x: any) => x.processId === processId)
  if (p) { form.processCode = p.processCode; form.processName = p.processName }
  else { form.processCode = ''; form.processName = '' }
}

function onItemSelected(row: any) {
  if (!row) return
  const line = form.issueLines[currentItemIdx.value]
  if (line) {
    line.itemId = row.itemId
    line.itemCode = row.itemCode
    line.itemName = row.itemName
    if (!line.unitOfMeasure) { line.unitOfMeasure = 'TON'; line.unitName = '吨' }
  }
}

function onWarehouseSelected(row: any) {
  if (!row) return
  const line = form.issueLines[currentItemIdx.value]
  if (line) {
    line.warehouseId = row.warehouseId
    line.warehouseCode = row.warehouseCode
    line.warehouseName = row.warehouseName
  }
}

function addLine() {
  form.issueLines.push({ itemId: null, itemCode: '', itemName: '', quantity: 0, unitOfMeasure: 'TON', unitName: '吨', batchId: null, warehouseId: null, warehouseCode: '', warehouseName: '' })
}

function removeLine(idx: number) {
  form.issueLines.splice(idx, 1)
}

async function submit() {
  if (!form.vendorId) { proxy.$modal.msgWarning('请选择外协厂商'); return }
  if (form.issueLines.length === 0) { proxy.$modal.msgWarning('请添加发料物料'); return }
  submitting.value = true
  try {
    await createOutsource(form)
    proxy.$modal.msgSuccess('外协发货成功')
    visible.value = false
    emit('success')
  } catch (e) {} finally { submitting.value = false }
}

function cancel() { visible.value = false; resetForm() }

defineExpose({ open })
</script>

<style scoped>
.mt8 { margin-top: 8px; }
</style>
