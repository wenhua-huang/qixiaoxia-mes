<template>
  <el-dialog :title="title" v-model="showFlag" width="680px" append-to-body :close-on-click-modal="false" @close="onClose">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-row>
        <el-col :span="16">
          <el-form-item label="产品" prop="productName">
            <el-input v-model="form.productName" placeholder="请选择产品" readonly>
              <template #append><el-button icon="Search" @click="itemSelectRef?.open()" /></template>
            </el-input>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="订单数量" prop="quantity">
            <el-input-number v-model="form.quantity" :min="0" :precision="2" style="width:100%" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="8">
          <el-form-item label="单价" prop="unitPrice">
            <el-input-number v-model="form.unitPrice" :min="0" :precision="4" style="width:100%" @change="calcAmount" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="行金额">
            <el-input-number v-model="form.lineAmount" :min="0" :precision="2" style="width:100%" disabled />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="间距" prop="spacing">
            <el-input v-model="form.spacing" placeholder="如7.5cm" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="8">
          <el-form-item label="产品尺寸" prop="productSize">
            <el-input v-model="form.productSize" placeholder="如200*100*150mm" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="行交期" prop="requestDate">
            <el-date-picker v-model="form.requestDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="12">
          <el-form-item label="印刷要求" prop="printingReq">
            <el-input v-model="form.printingReq" placeholder="如1色满版黑印刷" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="绳料规格" prop="ropeSpec">
            <el-input v-model="form.ropeSpec" placeholder="如红色圆纸绳" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="12">
          <el-form-item label="包装要求" prop="packageReq">
            <el-input v-model="form.packageReq" type="textarea" :rows="2" placeholder="如500个/箱,贴唛头" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="发货要求" prop="shippingReq">
            <el-input v-model="form.shippingReq" type="textarea" :rows="2" placeholder="发货要求" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="form.remark" type="textarea" :rows="2" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button type="primary" @click="confirm">确 定</el-button>
      <el-button @click="showFlag = false">取 消</el-button>
    </template>
    <ItemSelect ref="itemSelectRef" @onSelected="onProductSelected" />
  </el-dialog>
</template>

<script setup lang="ts" name="SalOrderLineEdit">
import ItemSelect from '@/components/itemSelect/single.vue'
import type { SalOrderLine } from '@/types'

const props = defineProps<{ modelValue: boolean; line: SalOrderLine | null }>()
const emit = defineEmits<{
  'update:modelValue': [v: boolean]
  confirm: [line: SalOrderLine]
}>()

const showFlag = ref(props.modelValue)
watch(() => props.modelValue, (v: boolean) => { showFlag.value = v; if (v) initForm() })
watch(showFlag, (v: boolean) => emit('update:modelValue', v))

const itemSelectRef = ref()
const title = ref('新增明细行')
const form = reactive<SalOrderLine>({})
const rules = {
  productName: [{ required: true, message: '请选择产品', trigger: 'change' }],
  quantity: [{ required: true, message: '订单数量不能为空', trigger: 'blur' }]
}

function initForm() {
  const src = props.line
  title.value = src && src.lineId ? '修改明细行' : '新增明细行'
  Object.assign(form, {
    lineId: null, productId: null, productCode: null, productName: null, productSpc: null,
    unitOfMeasure: null, unitName: null, quantity: undefined, unitPrice: undefined, lineAmount: undefined,
    spacing: null, productSize: null, printingReq: null, ropeSpec: null, packageReq: null,
    shippingReq: null, requestDate: null, remark: null, lineNo: src?.lineNo
  }, src || {})
}
function onProductSelected(row: any) {
  form.productId = row.itemId
  form.productCode = row.itemCode
  form.productName = row.itemName
  form.productSpc = row.specification
  form.unitOfMeasure = row.unitOfMeasure
  form.unitName = row.unitName
}
function calcAmount() {
  if (form.unitPrice != null && form.quantity != null) {
    form.lineAmount = Number((form.unitPrice * form.quantity).toFixed(2))
  }
}
function confirm() {
  if (!form.productName) { emit('update:modelValue', false); return }
  calcAmount()
  emit('confirm', { ...form })
  showFlag.value = false
}
function onClose() { emit('update:modelValue', false) }
</script>
