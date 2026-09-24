<template>
  <el-dialog
    v-model="visible"
    title="报异常"
    width="600px"
    append-to-body
    :close-on-click-modal="false"
    :before-close="handleClose"
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
        <!-- 直传 MinIO（/common/uploadMinio，与手机端同一存储），全局 ImageUpload 走本地盘故此处本地实现。
             v-model:file-list 单一数据源：el-upload 内部列表即提交列表，删除自动同步 -->
        <el-upload
          action="#"
          list-type="picture-card"
          name="file"
          accept="image/png,image/jpeg"
          multiple
          v-model:file-list="fileList"
          :limit="MAX_SCENE_IMAGES"
        :before-upload="beforeUpload"
        :http-request="doUpload"
        :on-exceed="onExceed"
        :on-error="onUploadError"
        :on-preview="previewImage"
          :show-file-list="true"
        >
          <el-icon><Plus /></el-icon>
        </el-upload>
        <div class="field-hint">最多 {{ MAX_SCENE_IMAGES }} 张，png/jpg，单张 ≤ {{ MAX_FILE_MB }}MB</div>
        <el-dialog v-model="previewVisible" title="预览" width="720px" append-to-body>
          <img :src="previewUrl" style="display:block;max-width:100%;margin:0 auto" />
        </el-dialog>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="uploading > 0 || submitting" @click="handleClose()">取 消</el-button>
      <el-button type="primary" :loading="submitting" :disabled="uploading > 0" @click="submit">提交异常单</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, getCurrentInstance } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { addException } from '@/api/mes/pro/exception'
import { normalizeImageUrl } from '@/utils/image'
import request from '@/utils/request'

// 与手机端 scene_images 单列逗号串、九宫格上限保持一致
const MAX_SCENE_IMAGES = 9
const MAX_FILE_MB = 10
const UPLOAD_URL = '/common/uploadMinio'
// 弱网下 10MB 图片给 30s（全局默认 10s 偏紧）
const UPLOAD_TIMEOUT_MS = 30000

const { proxy } = getCurrentInstance() as any
const emit = defineEmits(['success'])
const { mes_pro_exception_type } = proxy.useDict('mes_pro_exception_type')

const visible = ref(false)
const submitting = ref(false)
const uploading = ref(0)
const formRef = ref()
const task = ref<any>({})
const fileList = ref<any[]>([])
const previewVisible = ref(false)
const previewUrl = ref('')
const form = reactive<any>({
  exceptionType: '',
  impactQuantity: undefined as number | undefined,
  description: ''
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
  fileList.value = []
  previewVisible.value = false
  visible.value = true
}

function onClose() {
  formRef.value?.clearValidate()
  previewVisible.value = false
}

/** 上传/提交中禁止关窗：在途请求完成后会回调到已重开的弹窗形成幻影图片或幻影成功态 */
function handleClose(done?: () => void) {
  if (submitting.value) {
    proxy.$modal.msgWarning('异常单提交中，请稍候')
    return
  }
  if (uploading.value > 0) {
    proxy.$modal.msgWarning('图片仍在上传，请稍候')
    return
  }
  visible.value = false
  done?.()
}

// 纯校验：返回 false 时 el-upload 不会调用 http-request，计数只能放在 doUpload 里，
// 否则非法文件（超大/错格式）会让 uploading 永久 +1 → 按钮全灰、弹窗关不掉
function beforeUpload(file: File): boolean {
  const ext = (file.name.slice(file.name.lastIndexOf('.') + 1) || '').toLowerCase()
  if (!['png', 'jpg', 'jpeg'].includes(ext)) {
    proxy.$modal.msgError('仅支持 png/jpg 图片')
    return false
  }
  if (file.size / 1024 / 1024 > MAX_FILE_MB) {
    proxy.$modal.msgError(`单张图片不能超过 ${MAX_FILE_MB}MB`)
    return false
  }
  return true
}

function onExceed() {
  proxy.$modal.msgWarning(`最多上传 ${MAX_SCENE_IMAGES} 张`)
}

async function doUpload(options: any) {
  uploading.value++
  const fd = new FormData()
  fd.append('file', options.file)
  try {
    // repeatSubmit:false 关闭防重复提交拦截（FormData 序列化后都是 "{}"，并发上传会被误拦）；
    // 显式 multipart 头：request.ts 全局默认 Content-Type=application/json，axios 1.13 会把
    // FormData 误转成 JSON（{"file":{}}）导致后端 file=null；multipart 头让其原样发送，boundary 由浏览器补
    // 非 200 由 request 拦截器统一弹错并 reject
    const data: any = await request.post(UPLOAD_URL, fd, {
      headers: { repeatSubmit: false, 'Content-Type': 'multipart/form-data' },
      timeout: UPLOAD_TIMEOUT_MS
    })
    if (!data?.url) {
      // 自定义 http-request 内 throw 不会被 request 拦截器兜底提示，需主动弹错（EP 随后走 on-error 移卡）
      proxy.$modal.msgError('上传响应缺少文件地址，请重试')
      throw new Error('上传响应缺少文件地址')
    }
    // options.file 是 rawFile；列表渲染与 sceneImages 读取的是 v-model 里同 uid 的包装对象，URL 必须回写到包装对象
    const wrapper = fileList.value.find((f: any) => f.uid === options.file?.uid)
    const blobUrl = wrapper?.url
    // 展示用同源 /qxx-mes 代理地址；原始 9010 地址在 EP 回写的 response.url 上，提交取它
    if (wrapper) wrapper.url = normalizeImageUrl(data.url)
    else options.file.url = data.url
    if (blobUrl && String(blobUrl).startsWith('blob:')) URL.revokeObjectURL(blobUrl)
    return data
  } finally {
    uploading.value = Math.max(0, uploading.value - 1)
  }
  // 失败时让异常继续抛出：el-upload 走 on-error 移卡；网络类错误文案由 request 拦截器弹出（不重复弹）
}

// EP 失败时把卡片移出列表但不释放其 blob: 预览地址，需手动回收避免内存泄漏
function onUploadError(_err: Error, uploadFile: any) {
  const url = uploadFile?.url
  if (url && String(url).startsWith('blob:')) URL.revokeObjectURL(url)
}

function previewImage(file: any) {
  previewUrl.value = file.url || ''
  if (previewUrl.value) previewVisible.value = true
}

function sceneImages(): string {
  // 提交取响应里的原始 MinIO 地址（与手机端落库格式一致）；f.url 是展示用的同源代理地址
  return fileList.value
    .filter((f: any) => f.status === 'success' && (f.response?.url || f.url)
      && !String(f.url).startsWith('blob:'))
    .map((f: any) => f.response?.url || f.url)
    .join(',')
}

function submit() {
  if (uploading.value > 0) {
    proxy.$modal.msgWarning('图片仍在上传，请稍候')
    return
  }
  formRef.value?.validate(async (valid: boolean) => {
    if (!valid) return
    submitting.value = true
    try {
      const res = await addException({
        taskId: task.value.taskId,
        exceptionType: form.exceptionType,
        impactQuantity: form.impactQuantity,
        description: form.description.trim(),
        sceneImages: sceneImages() || null
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
