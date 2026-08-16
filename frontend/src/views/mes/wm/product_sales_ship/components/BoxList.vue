<template>
  <div>
    <el-row class="mb8" v-if="!readonly">
      <el-col :span="1.5"><el-button type="primary" plain icon="Plus" size="small" @click="handleAdd">新增装箱</el-button></el-col>
      <el-col :span="20" class="hint">箱号留空自动生成 BOX-NNN；体积按长×宽×高(cm)÷1000000 自动计算；装箱量不能超过已出库确认量</el-col>
    </el-row>
    <el-alert v-if="!readonly && postedTotal === 0" title="请先在出库单上完成「出库确认」扣减库存后，再进行装箱发运"
              type="warning" :closable="false" show-icon class="mb8" />
    <el-table :data="boxes" size="small" border show-summary :summary-method="getSummaries">
      <el-table-column label="箱号" prop="boxNo" width="100" />
      <el-table-column label="物料编码" prop="itemCode" width="120" />
      <el-table-column label="物料名称" prop="itemName" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="数量" prop="quantity" width="80" align="center" />
      <el-table-column label="单位" prop="unitName" width="60" align="center" />
      <el-table-column label="箱规" prop="boxSpec" width="110" :show-overflow-tooltip="true" />
      <el-table-column label="长×宽×高(cm)" width="140" align="center">
        <template #default="s">{{ s.row.boxLength||0 }} × {{ s.row.boxWidth||0 }} × {{ s.row.boxHeight||0 }}</template>
      </el-table-column>
      <el-table-column label="体积(m³)" prop="volume" width="90" align="center" />
      <el-table-column label="重量(kg)" prop="weight" width="90" align="center" />
      <el-table-column label="状态" width="80" align="center">
        <template #default="s">
          <el-tag :type="s.row.status==='SHIPPED' ? 'success' : 'info'" size="small">
            {{ s.row.status === 'SHIPPED' ? '已发运' : '已装箱' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="130" align="center" v-if="!readonly">
        <template #default="s">
          <el-button link type="primary" icon="Edit" @click="handleEdit(s.row)"
                     :disabled="s.row.status==='SHIPPED'" v-hasPermi="['mes:wm:sales:edit']">改</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDel(s.row)"
                     :disabled="s.row.status==='SHIPPED'" v-hasPermi="['mes:wm:sales:edit']">删</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 装箱表单弹窗 -->
    <el-dialog :title="form.boxId ? '修改装箱' : '新增装箱'" v-model="formShow" width="640px" append-to-body>
      <el-form :model="form" ref="formRef" :rules="rules" label-width="100px" size="default">
        <el-row>
          <el-col :span="12">
            <el-form-item label="出库行" prop="lineId">
              <el-select v-model="form.lineId" placeholder="选择物料" style="width:100%" @change="onLineChange">
                <el-option v-for="l in lines" :key="l.lineId"
                           :label="`${l.itemCode} - ${l.itemName}`" :value="l.lineId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="箱号"><el-input v-model="form.boxNo" placeholder="留空自动生成" /></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="12"><el-form-item label="数量" prop="quantity">
            <el-input-number v-model="form.quantity" :min="0" :precision="2" style="width:100%" />
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item label="重量(kg)"><el-input-number v-model="form.weight" :min="0" :precision="2" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="12"><el-form-item label="箱长(cm)"><el-input-number v-model="form.boxLength" :min="0" :precision="2" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="箱宽(cm)"><el-input-number v-model="form.boxWidth" :min="0" :precision="2" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="12"><el-form-item label="箱高(cm)"><el-input-number v-model="form.boxHeight" :min="0" :precision="2" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="箱规"><el-input v-model="form.boxSpec" placeholder="如：纸箱50×40×30" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="唛头备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确 定</el-button>
        <el-button @click="formShow=false">取 消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, getCurrentInstance } from 'vue'
import { addBox, updateBox, delBox } from '@/api/mes/wm/product_sales_box'
import type { WmProductSalesBox, WmProductSalesLine } from '@/types'

const props = defineProps<{
  salesId: number
  lines: WmProductSalesLine[]
  boxes: WmProductSalesBox[]
  readonly: boolean
}>()
const emit = defineEmits<{ refresh: [] }>()

const { proxy } = getCurrentInstance() as any
const formShow = ref(false)
const submitting = ref(false)
const formRef = ref()
const form = reactive<Partial<WmProductSalesBox>>({})
const rules = {
  lineId: [{ required: true, message: '请选择物料行' }],
  quantity: [{ required: true, message: '请输入数量' }]
}

function handleAdd() {
  Object.keys(form).forEach(k => delete (form as any)[k])
  form.salesId = props.salesId
  formShow.value = true
}
function handleEdit(row: WmProductSalesBox) {
  Object.keys(form).forEach(k => delete (form as any)[k])
  Object.assign(form, row)
  formShow.value = true
}
function onLineChange(lineId: number) {
  const line = props.lines.find((l: WmProductSalesLine) => l.lineId === lineId)
  if (line) {
    form.itemId = line.itemId
    form.itemCode = line.itemCode
    form.itemName = line.itemName
    form.specification = line.specification
    form.unitOfMeasure = line.unitOfMeasure
    form.unitName = line.unitName
    if (form.quantity == null) form.quantity = remainQty(line)
  }
}

/** 该行剩余可装箱量 = 已出库确认量 - 已装箱数量（排除当前编辑的箱） */
function remainQty(line: WmProductSalesLine): number {
  const packed = props.boxes
    .filter((b: WmProductSalesBox) => b.lineId === line.lineId && b.boxId !== form.boxId)
    .reduce((s: number, b: WmProductSalesBox) => s + Number(b.quantity || 0), 0)
  return Math.max(0, Number(line.quantityPosted || 0) - packed)
}

/** 整单已出库确认总量（未出库确认时不允许装箱） */
const postedTotal = computed(() =>
  props.lines.reduce((s: number, l: WmProductSalesLine) => s + Number(l.quantityPosted || 0), 0))

function handleSubmit() {
  formRef.value?.validate((v: boolean) => {
    if (!v) return
    // 校验数量不超过该行剩余可装箱量
    const line = props.lines.find((l: WmProductSalesLine) => l.lineId === form.lineId)
    if (line && Number(form.quantity || 0) > remainQty(line)) {
      proxy.$modal.msgError(`装箱数量超过该行可装箱量（已出库确认剩余 ${remainQty(line)}）`)
      return
    }
    submitting.value = true
    const action = form.boxId ? updateBox(form as WmProductSalesBox) : addBox(form as WmProductSalesBox)
    action.then(() => {
      proxy.$modal.msgSuccess('保存成功')
      formShow.value = false
      emit('refresh')
    }).finally(() => { submitting.value = false })
  })
}
function handleDel(row: WmProductSalesBox) {
  proxy.$modal.confirm(`删除箱[${row.boxNo}]？`).then(() => delBox(row.boxId))
    .then(() => { proxy.$modal.msgSuccess('删除成功'); emit('refresh') }).catch(() => {})
}

/** 合计行：数量/体积/重量 */
function getSummaries(param: any) {
  const { columns, data } = param
  const sums: string[] = []
  columns.forEach((col: any, i: number) => {
    if (i === 0) { sums[i] = '合计'; return }
    if (['quantity', 'volume', 'weight'].includes(col.property)) {
      sums[i] = data.reduce((s: number, r: any) => s + Number(r[col.property] || 0), 0).toFixed(2)
    } else sums[i] = ''
  })
  return sums
}
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
.hint { color: #909399; font-size: 12px; padding-left: 12px; line-height: 32px; }
</style>
