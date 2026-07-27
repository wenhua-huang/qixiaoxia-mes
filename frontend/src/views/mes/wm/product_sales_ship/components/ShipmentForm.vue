<template>
  <el-dialog title="新增发运" v-model="show" width="820px" append-to-body @close="handleClose">
    <el-form :model="form" ref="formRef" :rules="rules" label-width="100px">
      <el-row>
        <el-col :span="8">
          <el-form-item label="发货方式" prop="shipMethod">
            <el-select v-model="form.shipMethod" style="width:100%">
              <el-option v-for="d in ship_method" :key="d.value" :label="d.label" :value="d.value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8"><el-form-item label="计划发货日期"><el-date-picker v-model="form.planShipDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="实际发货日期"><el-date-picker v-model="form.actualShipDate" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" /></el-form-item></el-col>
      </el-row>

      <!-- 物流/快递信息 -->
      <el-row v-if="form.shipMethod==='LOGISTICS' || form.shipMethod==='EXPRESS'">
        <el-col :span="12"><el-form-item label="物流公司"><el-input v-model="form.logisticsCompany" placeholder="如：顺丰速运" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="运单号"><el-input v-model="form.trackingNo" /></el-form-item></el-col>
      </el-row>
      <el-row v-if="form.shipMethod==='LOGISTICS' || form.shipMethod==='EXPRESS'">
        <el-col :span="12"><el-form-item label="物流费用"><el-input-number v-model="form.logisticsFee" :min="0" :precision="2" style="width:100%" /></el-form-item></el-col>
      </el-row>

      <!-- 车辆/司机信息（自提/客户自送） -->
      <el-row v-if="form.shipMethod==='PICKUP' || form.shipMethod==='SELF'">
        <el-col :span="8"><el-form-item label="车牌号"><el-input v-model="form.vehicleNo" placeholder="如：沪A12345" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="司机姓名"><el-input v-model="form.driverName" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="司机电话"><el-input v-model="form.driverTel" /></el-form-item></el-col>
      </el-row>

      <!-- 收货信息 -->
      <el-divider content-position="left">收货信息</el-divider>
      <el-row>
        <el-col :span="12"><el-form-item label="收货人"><el-input v-model="form.receiverName" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="收货电话"><el-input v-model="form.receiverTel" /></el-form-item></el-col>
      </el-row>
      <el-form-item label="收货地址"><el-input v-model="form.shippingAddress" type="textarea" :rows="2" /></el-form-item>

      <!-- 勾选已装箱的箱 -->
      <el-divider content-position="left">本次发运装箱</el-divider>
      <el-alert v-if="!packedBoxes.length" title="暂无已装箱可发运，请先在「装箱明细」完成装箱" type="warning" :closable="false" class="mb8" />
      <el-table v-else ref="boxTableRef" :data="packedBoxes" size="small" border @selection-change="onBoxSelect" max-height="240">
        <el-table-column type="selection" width="45" />
        <el-table-column label="箱号" prop="boxNo" width="100" />
        <el-table-column label="物料编码" prop="itemCode" width="120" />
        <el-table-column label="物料名称" prop="itemName" min-width="140" :show-overflow-tooltip="true" />
        <el-table-column label="数量" prop="quantity" width="80" align="center" />
        <el-table-column label="箱规" prop="boxSpec" width="120" />
      </el-table>
      <div v-if="packedBoxes.length" class="mt8 hint">
        已选 {{ selectedBoxes.length }} 箱，合计数量 {{ selectedQty }}
      </div>

      <el-form-item label="备注" class="mt8"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button type="primary" @click="handleSubmit" :loading="submitting">确认发运</el-button>
      <el-button @click="show=false">取 消</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, getCurrentInstance, nextTick } from 'vue'
import { parseTime } from '@/utils/ruoyi'
import { addShipment } from '@/api/mes/wm/product_sales_shipment'
import type { WmProductSales, WmProductSalesShipment, WmProductSalesBox } from '@/types'

const props = defineProps<{
  salesId: number
  header: WmProductSales
  boxes: WmProductSalesBox[]
}>()
const emit = defineEmits<{ success: [] }>()

const { proxy } = getCurrentInstance() as any
const { mes_wm_ship_method: ship_method } = proxy.useDict('mes_wm_ship_method')
const show = ref(false)
const submitting = ref(false)
const formRef = ref()
const boxTableRef = ref()
const form = reactive<Partial<WmProductSalesShipment>>({})
const selectedBoxes = ref<WmProductSalesBox[]>([])
const rules = { shipMethod: [{ required: true, message: '请选择发货方式' }] }

/** 已装箱（PACKED）可发运的箱 */
const packedBoxes = computed(() => props.boxes.filter((b: WmProductSalesBox) => b.status === 'PACKED'))
const selectedQty = computed(() => selectedBoxes.value.reduce((s: number, b: WmProductSalesBox) => s + Number(b.quantity || 0), 0))

function open() {
  Object.keys(form).forEach(k => delete (form as any)[k])
  form.salesId = props.salesId
  form.shipMethod = 'LOGISTICS'
  form.actualShipDate = parseTime(new Date(), '{y}-{m}-{d} {h}:{i}:{s}')
  // 预填出库单收货信息
  if (props.header.receiverName) form.receiverName = props.header.receiverName
  if (props.header.receiverTel) form.receiverTel = props.header.receiverTel
  if (props.header.shippingAddress) form.shippingAddress = props.header.shippingAddress
  selectedBoxes.value = []
  show.value = true
  // 默认全选已装箱（需等表格渲染后调用 toggleRowSelection 才能回显勾选）
  nextTick(() => {
    packedBoxes.value.forEach((b: WmProductSalesBox) => boxTableRef.value?.toggleRowSelection(b, true))
  })
}

function onBoxSelect(sel: WmProductSalesBox[]) { selectedBoxes.value = sel }

function handleSubmit() {
  formRef.value?.validate((v: boolean) => {
    if (!v) return
    if (!selectedBoxes.value.length) {
      proxy.$modal.msgError('请至少勾选一个已装箱的箱')
      return
    }
    submitting.value = true
    const payload = { ...form, boxes: selectedBoxes.value.map((b: WmProductSalesBox) => ({ boxId: b.boxId })) } as WmProductSalesShipment
    addShipment(payload).then(() => {
      proxy.$modal.msgSuccess('发运成功')
      show.value = false
      emit('success')
    }).finally(() => { submitting.value = false })
  })
}

function handleClose() { selectedBoxes.value = [] }

defineExpose({ open })
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
.mt8 { margin-top: 8px; }
.hint { color: #909399; font-size: 12px; }
</style>
