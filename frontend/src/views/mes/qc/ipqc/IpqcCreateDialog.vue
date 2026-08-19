<template>
  <el-dialog v-model="visible" title="手工创建过程检验单" width="560px" :close-on-click-modal="false" append-to-body>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
      <el-form-item label="检验类型" prop="ipqcType">
        <el-select v-model="form.ipqcType" placeholder="请选择检验类型" style="width: 100%">
          <el-option v-for="t in TYPE_OPTIONS" :key="t.value" :label="t.label" :value="t.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="工单" prop="workorderId">
        <el-select v-model="form.workorderId" filterable placeholder="选择工单（自动带入产品物料）" style="width: 100%"
          :loading="woLoading" @change="onWorkorderChange">
          <el-option v-for="w in workorders" :key="w.workorderId" :label="`${w.workorderCode} ${w.workorderName || ''}`" :value="w.workorderId" />
        </el-select>
      </el-form-item>
      <el-form-item label="产品物料">
        <el-input :value="itemText" placeholder="选择工单后自动带入" readonly />
      </el-form-item>
      <el-form-item label="检验模板" prop="templateId">
        <el-select v-model="form.templateId" filterable placeholder="选择 IPQC 检验模板" style="width: 100%" :loading="tplLoading">
          <el-option v-for="t in ipqcTemplates" :key="t.templateId" :label="`${t.templateCode} ${t.templateName || ''}`" :value="t.templateId" />
        </el-select>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="选填" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取 消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">确 定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts" name="IpqcCreateDialog">
import { ref, computed } from 'vue'
import { getCurrentInstance } from 'vue'
import type { QcIpqc } from '@/api/mes/qc/ipqc'
import { addIpqc } from '@/api/mes/qc/ipqc'
import { listAllWorkorder } from '@/api/mes/pro/workorder'
import { listTemplate, type QcTemplate } from '@/api/mes/qc/template'

const { proxy } = getCurrentInstance() as any
const emit = defineEmits(['success'])

const TYPE_OPTIONS = [
  { value: 'FIRST_CHECK', label: '首检' },
  { value: 'TOUR_CHECK', label: '巡检' },
  { value: 'SPOT_CHECK', label: '抽检' },
  { value: 'LAST_CHECK', label: '完工检' }
]

const visible = ref(false)
const saving = ref(false)
const woLoading = ref(false)
const tplLoading = ref(false)
const workorders = ref<any[]>([])
const templates = ref<QcTemplate[]>([])
const formRef = ref()
const form = ref<QcIpqc>({ ipqcType: 'TOUR_CHECK' })

const rules = {
  ipqcType: [{ required: true, message: '请选择检验类型', trigger: 'change' }],
  workorderId: [{ required: true, message: '请选择工单', trigger: 'change' }],
  templateId: [{ required: true, message: '请选择检验模板', trigger: 'change' }]
}

const ipqcTemplates = computed(() =>
  templates.value.filter(t => (t.qcTypes || '').split(',').map((s: string) => s.trim()).includes('IPQC'))
)
const itemText = computed(() => {
  const f = form.value
  return f.itemCode ? `${f.itemCode} ${f.itemName || ''}` : ''
})

function open() {
  form.value = { ipqcType: 'TOUR_CHECK' }
  visible.value = true
  loadWorkorders()
  loadTemplates()
}

function loadWorkorders() {
  if (workorders.value.length) return
  woLoading.value = true
  listAllWorkorder().then((r: any) => {
    workorders.value = r.data || r.rows || []
  }).finally(() => (woLoading.value = false))
}

function loadTemplates() {
  if (templates.value.length) return
  tplLoading.value = true
  listTemplate({ enableFlag: '1', pageNum: 1, pageSize: 500 }).then((r: any) => {
    templates.value = r.rows || r.data || []
  }).finally(() => (tplLoading.value = false))
}

/** 选工单后带入产品物料（IPQC 手工单追溯到工单+产品） */
function onWorkorderChange(workorderId: number) {
  const w = workorders.value.find(x => x.workorderId === workorderId)
  if (!w) return
  form.value.workorderCode = w.workorderCode
  form.value.workorderName = w.workorderName
  form.value.itemId = w.productId
  form.value.itemCode = w.productCode
  form.value.itemName = w.productName
  form.value.specification = w.specification
  form.value.unitOfMeasure = w.unitOfMeasure
}

function submit() {
  formRef.value.validate((valid: boolean) => {
    if (!valid) return
    saving.value = true
    addIpqc(form.value).then(() => {
      proxy.$modal.msgSuccess('创建成功')
      visible.value = false
      emit('success')
    }).finally(() => (saving.value = false))
  })
}

defineExpose({ open })
</script>
