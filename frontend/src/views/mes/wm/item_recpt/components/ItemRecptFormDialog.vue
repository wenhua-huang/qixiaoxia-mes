<template>
  <el-dialog :title="title" v-model="show" width="1000px" append-to-body :close-on-click-modal="false" @close="handleClose">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" :disabled="readonly">
      <el-row>
        <el-col :span="8">
          <el-form-item label="入库单号" prop="recptCode">
            <el-input v-model="form.recptCode" placeholder="入库单号" />
          </el-form-item>
        </el-col>
        <el-col :span="6" v-if="!form.recptId">
          <el-form-item>
            <el-switch v-model="autoGenFlag" active-color="#13ce66" size="small" @change="handleAutoGenChange" />
            <span style="margin-left:6px;font-size:12px;color:#13ce66">自动生成</span>
          </el-form-item>
        </el-col>
        <el-col :span="!form.recptId ? 10 : 16">
          <el-form-item label="入库名称" prop="recptName">
            <el-input v-model="form.recptName" placeholder="入库名称" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="8">
          <el-form-item label="入库日期" prop="recptDate">
            <el-date-picker v-model="form.recptDate" type="datetime" placeholder="选择日期" style="width:100%" value-format="YYYY-MM-DD HH:mm:ss" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="供应商" prop="vendorName">
            <el-input v-model="form.vendorName" readonly placeholder="请选择供应商">
              <template #append><el-button icon="Search" @click="handleSelectVendor" :disabled="readonly" /></template>
            </el-input>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="仓库" prop="warehouseId">
            <el-input v-model="form.warehouseName" readonly placeholder="请选择仓库">
              <template #append><el-button icon="Search" @click="handleSelectWarehouse" :disabled="readonly" /></template>
            </el-input>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="8">
          <el-form-item label="采购订单号" prop="purOrderCode">
            <el-input v-model="form.purOrderCode" readonly placeholder="选采购订单生成后自动带出">
              <template #append><el-button icon="Search" @click="handleSelectPurOrder" :disabled="readonly" /></template>
            </el-input>
          </el-form-item>
        </el-col>
        <el-col :span="16">
          <el-form-item label="备注" prop="remark">
            <el-input v-model="form.remark" placeholder="备注" />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <el-divider content-position="center">物料信息</el-divider>
    <el-row class="mb8" v-if="!readonly">
      <el-col :span="1.5"><el-button type="primary" plain icon="Plus" size="small" @click="handleAddLine">新增行</el-button></el-col>
    </el-row>
    <el-table :data="lineList" border size="small">
      <el-table-column label="物料编码" prop="itemCode" width="120" align="center" />
      <el-table-column label="物料名称" prop="itemName" :show-overflow-tooltip="true" min-width="140" />
      <el-table-column label="规格型号" prop="specification" :show-overflow-tooltip="true" width="120" align="center" />
      <el-table-column label="批次码" prop="batchCode" :show-overflow-tooltip="true" width="150" align="center" v-if="form.recptId">
        <template #default="scope">{{ scope.row.batchCode || '-' }}</template>
      </el-table-column>
      <el-table-column label="单位" prop="unitName" width="60" align="center" />
      <el-table-column label="订购量" width="90" align="center">
        <template #default="scope">{{ scope.row.quantityOrdered != null ? scope.row.quantityOrdered : '-' }}</template>
      </el-table-column>
      <el-table-column label="已收量" width="90" align="center">
        <template #default="scope">{{ scope.row.quantityReceived != null ? scope.row.quantityReceived : '-' }}</template>
      </el-table-column>
      <el-table-column label="本次入库量" width="130" align="center">
        <template #default="scope">
          <el-input-number v-if="!readonly" v-model="scope.row.quantityRecpt" :min="0" :precision="4" size="small" controls-position="right" style="width:115px" />
          <span v-else>{{ scope.row.quantityRecpt }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="70" align="center" v-if="!readonly">
        <template #default="scope">
          <el-button link type="danger" icon="Delete" @click="handleDelLine(scope.$index)" />
        </template>
      </el-table-column>
    </el-table>

    <template #footer>
      <div class="dialog-footer">
        <el-button type="primary" :loading="submitting" @click="submitForm" v-if="!readonly">保存单据</el-button>
        <el-button @click="show = false">{{ readonly ? '关 闭' : '取 消' }}</el-button>
      </div>
    </template>

    <VendorSelect ref="vendorSelectRef" @onSelected="onVendorSelected" />
    <WarehouseSelect ref="warehouseSelectRef" @onSelected="onWarehouseSelected" />
    <PurOrderSelect ref="purOrderSelectRef" @onSelected="onPurOrderSelected" />
    <ItemSelect ref="itemSelectRef" @onSelected="onItemSelected" />
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, getCurrentInstance } from 'vue'
import type { WmItemRecpt } from '@/types/api/mes/wm/item_recpt'
import type { WmItemRecptLine } from '@/types/api/mes/wm/item_recpt_line'
import type { PurOrder } from '@/types/api/mes/pur/order'
import { addWmItemRecpt, updateWmItemRecpt, getWmItemRecpt } from '@/api/mes/wm/item_recpt'
import { listWmItemRecptLine } from '@/api/mes/wm/item_recpt_line'
import { buildFromPurOrder } from '@/api/mes/wm/item_recpt'
import { genSerialCode } from '@/api/mes/sys/autocoderule'
import VendorSelect from '@/components/vendorSelect/single.vue'
import WarehouseSelect from '@/components/warehouseSelect/single.vue'
import PurOrderSelect from '@/components/purOrderSelect/single.vue'
import ItemSelect from '@/components/itemSelect/single.vue'

const { proxy } = getCurrentInstance() as any

const emit = defineEmits<{ success: [] }>()
const show = ref(false)
const readonly = ref(false)
const title = ref('')
const autoGenFlag = ref(false)
const form = reactive<WmItemRecpt>({} as WmItemRecpt)
const lineList = ref<WmItemRecptLine[]>([])
const submitting = ref(false)

const rules = {
  recptCode: [{ required: true, message: '入库单号不能为空', trigger: 'blur' }],
  warehouseId: [{ required: true, message: '仓库不能为空', trigger: 'blur' }]
}

const vendorSelectRef = ref()
const warehouseSelectRef = ref()
const purOrderSelectRef = ref()
const itemSelectRef = ref()
const formRef = ref()

/** 打开新增（可带 prefill — 从采购订单生成时传入） */
function openAdd(prefill?: Partial<WmItemRecpt>) {
  reset()
  Object.assign(form, prefill || {})
  form.status = 'DRAFT'
  // 入库日期默认当天（prefill 未指定时；与 el-date-picker value-format 保持一致）
  if (!form.recptDate) form.recptDate = todayDateTime()
  if (prefill?.lines) lineList.value = prefill.lines.map(l => ({ ...l }))
  readonly.value = false
  title.value = '新增入库单'
  // 新增默认开启自动生成入库单号（参照 SalesFormDialog，避免必填校验拦截）
  autoGenFlag.value = true
  handleAutoGenChange(true)
  show.value = true
}

/** 返回当前时间字符串 YYYY-MM-DD HH:mm:ss（与 date-picker value-format 对齐） */
function todayDateTime(): string {
  const d = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

/** 打开编辑：从 DB 加载头 + 行 */
function openEdit(id: number) {
  reset()
  getWmItemRecpt(id).then(r => {
    if (!r.data) return
    Object.assign(form, r.data)
    // 编辑时从 DB 加载行（detail 接口未返回 lines，单独查行表）
    loadLines(id)
    readonly.value = false
    title.value = '修改入库单'
    show.value = true
  })
}

/** 打开查看：从 DB 加载头 + 行，只读 */
function openView(id: number) {
  reset()
  getWmItemRecpt(id).then(r => {
    if (!r.data) return
    Object.assign(form, r.data)
    loadLines(id)
    readonly.value = true
    title.value = '查看入库单'
    show.value = true
  })
}

function loadLines(id: number) {
  listWmItemRecptLine({ recptId: id, pageNum: 1, pageSize: 100 } as any).then(r => {
    lineList.value = r.rows || []
  })
}

function reset() {
  Object.keys(form).forEach(k => delete (form as any)[k])
  lineList.value = []
  autoGenFlag.value = false
  submitting.value = false
}

function handleClose() { reset() }

function handleAutoGenChange(flag: boolean) {
  if (flag) genSerialCode('RECEIPT_NO').then((r: any) => { form.recptCode = r.data })
  else form.recptCode = ''
}

// 选择器
function handleSelectVendor() { vendorSelectRef.value?.open() }
function onVendorSelected(row: any) {
  form.vendorId = row.vendorId
  form.vendorName = row.vendorName
  form.vendorCode = row.vendorCode
}
function handleSelectWarehouse() { warehouseSelectRef.value?.open() }
function onWarehouseSelected(row: any) {
  form.warehouseId = row.warehouseId
  form.warehouseName = row.warehouseName
  form.warehouseCode = row.warehouseCode
}
function handleSelectPurOrder() { purOrderSelectRef.value?.open() }
/** 弹窗内选采购订单 → 后端生成草稿 → 覆盖头字段 + 替换行 */
function onPurOrderSelected(row: PurOrder) {
  buildFromPurOrder(row.orderId).then((r: any) => {
    if (!r.data) return
    const draft = r.data
    form.purOrderId = draft.purOrderId
    form.purOrderCode = draft.purOrderCode
    form.vendorId = draft.vendorId
    form.vendorCode = draft.vendorCode
    form.vendorName = draft.vendorName
    form.recptType = draft.recptType || 'PURCHASE'
    lineList.value = (draft.lines || []).map((l: WmItemRecptLine) => ({ ...l }))
    proxy.$modal.msgSuccess('已从采购订单带出物料行')
  }).catch(() => {})
}

// 物料行
function handleAddLine() { itemSelectRef.value?.open() }
function onItemSelected(row: any) {
  lineList.value.push({
    itemId: row.itemId, itemCode: row.itemCode, itemName: row.itemName,
    specification: row.specification, unitOfMeasure: row.unitOfMeasure, unitName: row.unitName,
    quantityRecpt: 1, warehouseId: form.warehouseId, warehouseCode: form.warehouseCode, warehouseName: form.warehouseName
  } as WmItemRecptLine)
}
function handleDelLine(idx: number) { lineList.value.splice(idx, 1) }

function submitForm() {
  if (submitting.value) return
  formRef.value?.validate((v: boolean) => {
    if (!v) return
    if (lineList.value.length === 0) {
      proxy.$modal.msgWarning('请至少添加一行物料')
      return
    }
    submitting.value = true
    form.lines = lineList.value
    const fn = form.recptId ? updateWmItemRecpt(form) : addWmItemRecpt(form)
    fn.then(() => {
      proxy.$modal.msgSuccess('保存成功')
      show.value = false
      emit('success')
    }).finally(() => {
      submitting.value = false
    })
  })
}

defineExpose({ openAdd, openEdit, openView })
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
.dialog-footer { text-align: right; }
</style>
