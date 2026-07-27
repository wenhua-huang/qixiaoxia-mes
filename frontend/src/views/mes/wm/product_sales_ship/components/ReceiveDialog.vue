<template>
  <el-dialog title="签收登记" v-model="show" width="640px" append-to-body @close="handleClose">
    <el-alert v-if="shipment" :title="`发运单：${shipment.shipmentCode} | 发运量：${shipment.shippedQuantity}`"
              type="info" :closable="false" class="mb8" />
    <el-form :model="form" ref="formRef" :rules="rules" label-width="100px">
      <el-row>
        <el-col :span="12"><el-form-item label="签收时间" prop="receivedTime">
          <el-date-picker v-model="form.receivedTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" />
        </el-form-item></el-col>
        <el-col :span="12"><el-form-item label="签收人" prop="receivedBy"><el-input v-model="form.receivedBy" /></el-form-item></el-col>
      </el-row>
      <el-form-item label="签收备注"><el-input v-model="form.receivedRemark" type="textarea" :rows="2" placeholder="如：包装完好/数量无误/部分破损等" /></el-form-item>
      <el-form-item label="回单附件">
        <file-upload v-model="form.attachmentUrl" :file-size="10" :limit="3" :file-type="['pdf','jpg','jpeg','png']" />
        <div class="hint">支持 PDF/图片，最多 3 个，单个 ≤10MB</div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button type="success" @click="handleSubmit" :loading="submitting">确认签收</el-button>
      <el-button @click="show=false">取 消</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, getCurrentInstance } from 'vue'
import { parseTime } from '@/utils/ruoyi'
import { receiveShipment } from '@/api/mes/wm/product_sales_shipment'
import useUserStore from '@/store/modules/user'
import type { WmProductSalesShipment } from '@/types'

const { proxy } = getCurrentInstance() as any
const userStore = useUserStore()
const emit = defineEmits<{ success: [] }>()
const show = ref(false)
const submitting = ref(false)
const formRef = ref()
const shipment = ref<WmProductSalesShipment | null>(null)
const form = reactive<Partial<WmProductSalesShipment>>({})
const rules = {
  receivedTime: [{ required: true, message: '请选择签收时间' }],
  receivedBy: [{ required: true, message: '请输入签收人' }]
}

function open(row: WmProductSalesShipment) {
  Object.keys(form).forEach(k => delete (form as any)[k])
  shipment.value = row
  form.receivedTime = parseTime(new Date(), '{y}-{m}-{d} {h}:{i}:{s}')
  form.receivedBy = userStore.nickName || userStore.name || ''
  show.value = true
}

function handleSubmit() {
  formRef.value?.validate((v: boolean) => {
    if (!v || !shipment.value) return
    submitting.value = true
    receiveShipment(shipment.value.shipmentId, form).then(() => {
      proxy.$modal.msgSuccess('签收成功')
      show.value = false
      emit('success')
    }).finally(() => { submitting.value = false })
  })
}

function handleClose() { shipment.value = null }

defineExpose({ open })
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
.hint { color: #909399; font-size: 12px; }
</style>
