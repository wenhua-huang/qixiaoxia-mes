<template>
  <el-dialog
    v-model="visible"
    :title="`质检放行 · ${task?.processName || task?.taskCode || ''}`"
    width="520px"
    append-to-body
    :close-on-click-modal="false"
    @close="onClose"
  >
    <el-alert
      :title="blockReason || '该工序被跟单质检不合格拦截，放行后下道工序方可报工'"
      type="warning"
      :closable="false"
      show-icon
      style="margin-bottom: 12px"
    />
    <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" @submit.prevent>
      <el-form-item label="放行理由" prop="reason">
        <el-input
          v-model="form.reason"
          type="textarea"
          :rows="3"
          :maxlength="MAX_LEN"
          show-word-limit
          placeholder="请填写放行理由（必填，2~500字）"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取 消</el-button>
      <el-button type="danger" :loading="submitting" @click="submit">确认放行</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, getCurrentInstance } from 'vue'
import { releaseQcBlock } from '@/api/mes/pro/task'

const MAX_LEN = 500
const MIN_LEN = 2

const { proxy } = getCurrentInstance() as any
const emit = defineEmits(['success'])

const visible = ref(false)
const submitting = ref(false)
const formRef = ref()
const task = ref<any>(null)
const blockReason = ref('')
const form = reactive({ reason: '' })

const rules = {
  reason: [
    { required: true, trigger: 'blur', validator: (_r: any, value: string, cb: (e?: Error) => void) => {
      const len = (value || '').trim().length
      if (len < MIN_LEN || len > MAX_LEN) cb(new Error(`放行理由需 ${MIN_LEN}~${MAX_LEN} 个字`))
      else cb()
    } }
  ]
}

function open(row: any, reason: string | null) {
  task.value = row
  blockReason.value = reason || ''
  form.reason = ''
  visible.value = true
}

function onClose() {
  formRef.value?.clearValidate()
}

function submit() {
  if (!task.value) return
  formRef.value?.validate(async (valid: boolean) => {
    if (!valid) return
    submitting.value = true
    try {
      await releaseQcBlock(task.value.taskId, form.reason.trim())
      proxy.$modal.msgSuccess('已放行')
      visible.value = false
      emit('success')
    } catch {
      // 错误提示已由 request 拦截器统一弹出
    } finally {
      submitting.value = false
    }
  })
}

defineExpose({ open })
</script>
