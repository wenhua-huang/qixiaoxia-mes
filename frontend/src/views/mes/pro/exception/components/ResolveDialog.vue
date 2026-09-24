<template>
  <el-dialog
    v-model="visible"
    title="选择处理出口"
    width="640px"
    append-to-body
    :close-on-click-modal="false"
    @close="onClose"
  >
    <el-alert
      title="回流类处理后异常单进入处理中，需待返工/补料/改期实际完成后手动关闭；终结类直接关闭。"
      type="info" :closable="false" show-icon style="margin-bottom: 12px"
    />
    <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" @submit.prevent>
      <el-row :gutter="16">
        <el-col :span="12">
          <div class="group-title">回流类（处理中）</div>
          <el-radio-group v-model="form.resolveType" class="resolve-group" @change="onTypeChange">
            <el-radio v-for="r in FLOWBACK" :key="r.value" :value="r.value" border class="resolve-radio">
              {{ r.label }}
            </el-radio>
          </el-radio-group>
        </el-col>
        <el-col :span="12">
          <div class="group-title">终结类（直接关闭）</div>
          <el-radio-group v-model="form.resolveType" class="resolve-group" @change="onTypeChange">
            <el-radio v-for="r in TERMINAL" :key="r.value" :value="r.value" border class="resolve-radio">
              {{ r.label }}
            </el-radio>
          </el-radio-group>
        </el-col>
      </el-row>

      <template v-if="form.resolveType">
        <el-form-item v-if="showQuantity" label="数量" prop="resolveQuantity">
          <el-input-number v-model="form.resolveQuantity" :min="0.01" :precision="2" :step="1" style="width:200px" />
          <span class="field-hint">{{ quantityHint }}</span>
        </el-form-item>
        <el-form-item v-if="form.resolveType === 'RESCHEDULE'" label="新计划完成时间" prop="newExpectedTime">
          <el-date-picker v-model="form.newExpectedTime" type="datetime" placeholder="选择新的计划完成时间"
            style="width:260px" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item v-if="form.resolveType === 'SCRAP'" label="报废数量" prop="scrapQuantity">
          <el-input-number v-model="form.scrapQuantity" :min="0.01" :precision="2" :step="1" style="width:200px" />
          <span class="field-hint">必须显式确认，不带入影响数量</span>
        </el-form-item>
        <el-form-item :label="conclusionLabel" :prop="conclusionRequired ? 'conclusion' : undefined">
          <el-input v-model="form.conclusion" type="textarea" :rows="3" :maxlength="1000" show-word-limit
            :placeholder="conclusionRequired ? '必填' : '可填写处理结论（选填）'" />
        </el-form-item>
      </template>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取 消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">确认处理</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, getCurrentInstance } from 'vue'
import { resolveException } from '@/api/mes/pro/exception'

const { proxy } = getCurrentInstance() as any
const emit = defineEmits(['success'])

const FLOWBACK = [
  { value: 'REWORK', label: '开返工任务' },
  { value: 'REMAKE', label: '开补做任务' },
  { value: 'PURCHASE', label: '开补料采购' },
  { value: 'RESCHEDULE', label: '顺延改期' }
]
const TERMINAL = [
  { value: 'SCRAP', label: '报废' },
  { value: 'CONCESSION', label: '让步接收' },
  { value: 'REFUND', label: '退款结单' }
]

const visible = ref(false)
const submitting = ref(false)
const formRef = ref()
const exceptionId = ref<number>(0)
const form = reactive<any>({
  resolveType: '', resolveQuantity: undefined as number | undefined,
  newExpectedTime: '', scrapQuantity: undefined as number | undefined, conclusion: ''
})

const showQuantity = computed(() => ['REWORK', 'REMAKE', 'PURCHASE'].includes(form.resolveType))
const conclusionRequired = computed(() => ['CONCESSION', 'REFUND'].includes(form.resolveType))
const conclusionLabel = computed(() => form.resolveType === 'CONCESSION' ? '让步接收结论'
  : form.resolveType === 'REFUND' ? '退款结单说明' : '处理结论')
const quantityHint = computed(() => {
  if (form.resolveType === 'PURCHASE') return '默认带出缺料数量，将建 DRAFT 采购单'
  return '将以原任务为模板开新任务（编号 -E1/-E2…），状态待排产'
})

const positiveQty = (_r: any, value: number, cb: (e?: Error) => void) => {
  if (!value || value <= 0) cb(new Error('请填写大于 0 的数量'))
  else cb()
}
const rules = {
  resolveQuantity: [{ required: true, trigger: 'blur', validator: positiveQty }],
  scrapQuantity: [{ required: true, trigger: 'blur', validator: positiveQty }],
  newExpectedTime: [{ required: true, message: '请选择新的计划完成时间', trigger: 'change' }],
  conclusion: [{ required: true, trigger: 'blur', validator: (_r: any, v: string, cb: (e?: Error) => void) => {
    if (!(v || '').trim()) cb(new Error('请填写结论说明'))
    else cb()
  } }]
}

function open(ex: any) {
  exceptionId.value = ex.exceptionId
  form.resolveType = ''
  form.resolveQuantity = ex.shortageQuantity || ex.impactQuantity || undefined
  form.newExpectedTime = ex.newExpectedTime || ''
  form.scrapQuantity = undefined
  form.conclusion = ''
  visible.value = true
}

function onTypeChange() {
  formRef.value?.clearValidate()
}
function onClose() {
  formRef.value?.clearValidate()
}

function submit() {
  formRef.value?.validate(async (valid: boolean) => {
    if (!valid) return
    submitting.value = true
    try {
      const res = await resolveException(exceptionId.value, {
        resolveType: form.resolveType,
        resolveQuantity: showQuantity.value ? form.resolveQuantity : undefined,
        newExpectedTime: form.resolveType === 'RESCHEDULE' ? form.newExpectedTime : undefined,
        scrapQuantity: form.resolveType === 'SCRAP' ? form.scrapQuantity : undefined,
        conclusion: form.conclusion?.trim() || undefined
      })
      proxy.$modal.msgSuccess('处理成功')
      visible.value = false
      emit('success', res.data)
    } catch {
      // 错误提示由 request 拦截器统一弹出
    } finally {
      submitting.value = false
    }
  })
}

defineExpose({ open })
</script>

<style lang="scss" scoped>
.group-title { font-weight: 600; margin: 4px 0 10px; color: #303133; }
.resolve-group { display: flex; flex-direction: column; gap: 10px; width: 100%; }
.resolve-radio { margin-right: 0; width: 100%; }
.field-hint { margin-left: 10px; color: #909399; font-size: 12px; }
</style>
