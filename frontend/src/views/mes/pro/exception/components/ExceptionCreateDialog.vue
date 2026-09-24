<template>
  <el-dialog
    v-model="visible"
    title="报异常"
    width="600px"
    append-to-body
    :close-on-click-modal="false"
    @close="onClose"
  >
    <el-descriptions :column="1" size="small" border class="mb12">
      <el-descriptions-item label="生产工单">{{ task.workorderName || task.workorderCode || '-' }}</el-descriptions-item>
      <el-descriptions-item label="工序任务">{{ task.taskCode }}（{{ task.processName || '-' }}）</el-descriptions-item>
    </el-descriptions>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="92px" @submit.prevent>
      <el-form-item label="异常类型" prop="exceptionType">
        <el-radio-group v-model="form.exceptionType">
          <el-radio v-for="d in mes_pro_exception_type" :key="d.value" :value="d.value" border class="type-radio">
            {{ d.label }}
          </el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="影响数量" prop="impactQuantity">
        <el-input-number v-model="form.impactQuantity" :min="0.01" :precision="2" :step="1" style="width:200px" />
        <span class="field-hint">受本次异常影响的数量（选填，建议填写）</span>
      </el-form-item>
      <el-form-item label="异常说明" prop="description">
        <el-input v-model="form.description" type="textarea" :rows="3" maxlength="1000" show-word-limit
          placeholder="简述异常情况，责任方与处理出口由主管在 PC 端补全" />
      </el-form-item>
      <el-form-item label="现场照片">
        <image-upload v-model="form.sceneImages" :limit="MAX_SCENE_IMAGES" :file-size="10"
          :file-type="['png', 'jpg', 'jpeg']" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取 消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">提交异常单</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, getCurrentInstance } from 'vue'
import { addException } from '@/api/mes/pro/exception'

// 与手机端 scene_images 单列逗号串、九宫格上限保持一致
const MAX_SCENE_IMAGES = 9

const { proxy } = getCurrentInstance() as any
const emit = defineEmits(['success'])
const { mes_pro_exception_type } = proxy.useDict('mes_pro_exception_type')

const visible = ref(false)
const submitting = ref(false)
const formRef = ref()
const task = ref<any>({})
const form = reactive<any>({
  exceptionType: '',
  impactQuantity: undefined as number | undefined,
  description: '',
  sceneImages: ''
})

const rules = {
  exceptionType: [{ required: true, message: '请选择异常类型', trigger: 'change' }],
  description: [{ required: true, trigger: 'blur',
    validator: (_r: any, v: string, cb: (e?: Error) => void) => ((v || '').trim() ? cb() : cb(new Error('请填写异常说明'))) }]
}

function open(row: any) {
  task.value = row || {}
  form.exceptionType = ''
  form.impactQuantity = undefined
  form.description = ''
  form.sceneImages = ''
  visible.value = true
}

function onClose() {
  formRef.value?.clearValidate()
}

function submit() {
  formRef.value?.validate(async (valid: boolean) => {
    if (!valid) return
    submitting.value = true
    try {
      const res = await addException({
        taskId: task.value.taskId,
        exceptionType: form.exceptionType,
        impactQuantity: form.impactQuantity,
        description: form.description.trim(),
        sceneImages: form.sceneImages || null
      })
      proxy.$modal.msgSuccess('异常单已提交，状态：待处理')
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
.mb12 { margin-bottom: 12px; }
.type-radio { margin-bottom: 8px; }
.field-hint { margin-left: 10px; color: #909399; font-size: 12px; }
</style>
