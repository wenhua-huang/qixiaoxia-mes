<template>
  <div class="app-container">
    <el-page-header @back="goBack" class="mb16">
      <template #content>
        <span class="header-title">生产异常单</span>
        <el-tag v-if="form.exceptionCode" type="info" class="ml8">{{ form.exceptionCode }}</el-tag>
        <dict-tag v-if="form.status" :options="mes_pro_exception_status" :value="form.status" class="ml8" />
      </template>
    </el-page-header>

    <div v-loading="loading">
      <!-- 基本信息 -->
      <el-card shadow="never" class="mb16">
        <template #header><span>基本信息</span></template>
        <el-descriptions :column="3" size="small" border>
          <el-descriptions-item label="异常类型">
            <dict-tag :options="mes_pro_exception_type" :value="form.exceptionType" />
          </el-descriptions-item>
          <el-descriptions-item label="发生时间">{{ fmt(form.occurTime) }}</el-descriptions-item>
          <el-descriptions-item label="上报人">{{ form.reporterName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="生产工单">{{ form.workorderName || form.workorderCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="任务编号">{{ form.taskCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="工序">{{ form.processName || form.processCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="出口动作">
            <dict-tag v-if="form.resolveType" :options="mes_pro_exception_resolve" :value="form.resolveType" />
            <span v-else style="color:#909399">未处理</span>
          </el-descriptions-item>
          <el-descriptions-item label="处理单据">
            <template v-if="form.targetDocCode">
              <el-button link type="primary" @click="goTargetDoc">{{ form.targetDocCode }}</el-button>
              <el-tag v-if="form.targetDocType === 'TASK' && form.targetDocStatus" size="small"
                :type="TASK_STATUS_TAG_TYPE[form.targetDocStatus] || 'info'" class="ml8">
                {{ TASK_STATUS_LABEL[form.targetDocStatus] || form.targetDocStatus }}
              </el-tag>
              <dict-tag v-else-if="form.targetDocType === 'PUR_ORDER' && form.targetDocStatus"
                :options="mes_order_status" :value="form.targetDocStatus" class="ml8" />
            </template>
            <span v-else style="color:#909399">-</span>
          </el-descriptions-item>
          <el-descriptions-item label="处理时间">{{ fmt(form.resolveTime) }}</el-descriptions-item>
          <el-descriptions-item label="处理结论" :span="3">
            <span v-if="form.conclusion">{{ form.conclusion }}</span>
            <span v-else style="color:#909399">-</span>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 定责与补全 -->
      <el-card shadow="never" class="mb16">
        <template #header>
          <span>定责与补全</span>
          <el-tag v-if="!canEdit" size="small" type="info" class="ml8">仅查看</el-tag>
        </template>
        <el-form :model="form" label-width="120px" :disabled="!canEdit">
          <el-row :gutter="16">
            <el-col :span="8">
              <el-form-item label="责任方">
                <el-select v-model="form.responsibleParty" style="width:100%" :disabled="!canEdit || partyLocked">
                  <el-option v-for="d in partyOptions" :key="d.value" :label="d.label" :value="d.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="影响数量">
                <el-input-number v-model="form.impactQuantity" :min="0" :precision="2" style="width:100%" />
              </el-form-item>
            </el-col>
          </el-row>

          <!-- 质量数量 -->
          <template v-if="form.exceptionType === 'QUALITY'">
            <el-row :gutter="16">
              <el-col :span="8">
                <el-form-item label="质量小类">
                  <el-select v-model="form.qualitySubclass" style="width:100%" clearable placeholder="请选择">
                    <el-option v-for="d in mes_pro_exception_quality_sub" :key="d.value" :label="d.label" :value="d.value" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="可使用数量">
                  <el-input-number v-model="form.usableQuantity" :min="0" :precision="2" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="是否需要返工">
                  <el-radio-group v-model="form.needRework">
                    <el-radio value="Y">是</el-radio><el-radio value="N">否</el-radio>
                  </el-radio-group>
                </el-form-item>
              </el-col>
            </el-row>
          </template>

          <!-- 缺料 -->
          <template v-else-if="form.exceptionType === 'MATERIAL'">
            <el-row :gutter="16">
              <el-col :span="8">
                <el-form-item label="缺料物料"><el-input v-model="form.itemName" maxlength="255" /></el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="缺料数量">
                  <el-input-number v-model="form.shortageQuantity" :min="0" :precision="2" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="预计到货日期">
                  <el-date-picker v-model="form.expectedArrivalDate" type="date" style="width:100%" value-format="YYYY-MM-DD" />
                </el-form-item>
              </el-col>
            </el-row>
          </template>

          <!-- 进度延迟 -->
          <template v-else-if="form.exceptionType === 'DELAY'">
            <el-row :gutter="16">
              <el-col :span="8">
                <el-form-item label="原计划完成时间">
                  <el-date-picker v-model="form.originalPlanTime" type="datetime" style="width:100%" value-format="YYYY-MM-DD HH:mm:ss" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="新预计完成时间">
                  <el-date-picker v-model="form.newExpectedTime" type="datetime" style="width:100%" value-format="YYYY-MM-DD HH:mm:ss" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="延迟原因">
              <el-input v-model="form.delayReason" type="textarea" :rows="2" maxlength="500" show-word-limit />
            </el-form-item>
          </template>

          <!-- 客户退货 -->
          <template v-else-if="form.exceptionType === 'RETURN'">
            <el-row :gutter="16">
              <el-col :span="8">
                <el-form-item label="退货数量">
                  <el-input-number v-model="form.returnQuantity" :min="0" :precision="2" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="客户接受返工">
                  <el-radio-group v-model="form.customerAcceptRework">
                    <el-radio value="Y">是</el-radio><el-radio value="N">否</el-radio>
                  </el-radio-group>
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="退货原因">
              <el-input v-model="form.returnReason" type="textarea" :rows="2" maxlength="500" show-word-limit />
            </el-form-item>
          </template>

          <el-form-item label="异常情况说明">
            <el-input v-model="form.description" type="textarea" :rows="3" maxlength="1000" show-word-limit />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="form.remark" maxlength="500" />
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 现场照片 -->
      <el-card v-if="imageList.length" shadow="never" class="mb16">
        <template #header><span>现场照片</span></template>
        <div class="img-list">
          <img v-for="(url, i) in imageList" :key="i" :src="url" class="img-thumb" @click="previewIndex = i" />
        </div>
        <el-image-viewer v-if="previewIndex !== -1" :url-list="imageList" :initial-index="previewIndex" @close="previewIndex = -1" />
      </el-card>

      <!-- 底部操作 -->
      <div class="footer-bar">
        <el-button :disabled="closing" @click="goBack">返 回</el-button>
        <el-button type="primary" v-if="canEdit" :loading="saving" :disabled="closing" @click="save">保存补全</el-button>
        <el-button type="warning" v-if="canResolve" :disabled="closing" @click="openResolve">选出口处理</el-button>
        <el-button type="success" v-if="canClose" :loading="closing" @click="closeEx">关闭异常单</el-button>
        <el-button type="danger" plain v-if="canVoid" :loading="closing" @click="voidEx">作废</el-button>
      </div>
    </div>

    <ResolveDialog ref="resolveRef" @success="loadDetail" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, getCurrentInstance } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getException, updateException, closeException, voidException } from '@/api/mes/pro/exception'
import { checkPermi } from '@/utils/permission'
import { normalizeImageUrl } from '@/utils/image'
import { TASK_STATUS_LABEL, TASK_STATUS_TAG_TYPE } from '../taskStatus'
import ResolveDialog from './components/ResolveDialog.vue'

const { proxy } = getCurrentInstance() as any
const route = useRoute()
const router = useRouter()
const {
  mes_pro_exception_type, mes_pro_exception_party, mes_pro_exception_status,
  mes_pro_exception_resolve, mes_pro_exception_quality_sub, mes_order_status
} = proxy.useDict('mes_pro_exception_type', 'mes_pro_exception_party', 'mes_pro_exception_status',
  'mes_pro_exception_resolve', 'mes_pro_exception_quality_sub', 'mes_order_status')

const exceptionId = Number(route.query.exceptionId)
const loading = ref(false)
const saving = ref(false)
const closing = ref(false)
const form = ref<any>({})
// 已落库责任方：未保存的本地选择不锁定下拉、不放行出口（后端 PENDING 另有硬拦兜底）
const savedParty = ref('')
const previewIndex = ref(-1)
const resolveRef = ref<InstanceType<typeof ResolveDialog>>()

const imageList = computed(() => (form.value.sceneImages || '').split(',').map((s: string) => normalizeImageUrl(s.trim())).filter(Boolean))
const partyOptions = computed(() => mes_pro_exception_party.value?.filter((d: any) => d.value !== 'PENDING') || [])
const partyLocked = computed(() => !!savedParty.value && savedParty.value !== 'PENDING')
const canEdit = computed(() => form.value.status === 'OPEN' && checkPermi(['mes:pro:exception:edit']))
const canResolve = computed(() => form.value.status === 'OPEN' && checkPermi(['mes:pro:exception:handle']))
const canClose = computed(() => form.value.status === 'PROCESSING' && checkPermi(['mes:pro:exception:handle']))
const canVoid = computed(() => form.value.status === 'OPEN' && checkPermi(['mes:pro:exception:handle']))

function fmt(v?: string) { return v ? proxy.parseTime(v, '{y}-{m}-{d} {h}:{i}') : '-' }

function loadDetail() {
  if (!exceptionId) return Promise.resolve()
  loading.value = true
  return getException(exceptionId).then((r: any) => {
    form.value = r.data || {}
    savedParty.value = form.value.responsibleParty || ''
  }).finally(() => { loading.value = false })
}

function save() {
  saving.value = true
  updateException(form.value).then(() => {
    proxy.$modal.msgSuccess('保存成功')
    savedParty.value = form.value.responsibleParty || ''
    loadDetail()
  }).finally(() => { saving.value = false })
}

function openResolve() {
  if (partyLocked.value) resolveRef.value?.open(form.value)
  else proxy.$modal.msgWarning('请先在上方确定责任方并保存，再选择处理出口')
}

function closeEx() {
  proxy.$modal.prompt('请填写处理结论（必填，2~1000 字）', '关闭异常单', {
    inputType: 'textarea', inputValidator: (v: string) => {
      const len = (v || '').trim().length
      return (len >= 2 && len <= 1000) || '处理结论需为 2~1000 个字'
    }
  }).then(({ value }: any) => {
    closing.value = true
    return closeException(exceptionId, value.trim())
  }).then(() => { proxy.$modal.msgSuccess('已关闭'); return loadDetail() })
    .catch(() => {}).finally(() => { closing.value = false })
}

function voidEx() {
  proxy.$modal.prompt('请填写作废原因（必填，2~996 字）。作废后单据留痕不可恢复，请确认是否挂错对象。', '作废异常单', {
    inputType: 'textarea',
    // 落库结论带【作废】前缀占 4 字，conclusion 列总长 1000
    inputValidator: (v: string) => {
      const len = (v || '').trim().length
      return (len >= 2 && len <= 996) || '作废原因需为 2~996 个字'
    }
  }).then(({ value }: any) => {
    closing.value = true
    return voidException(exceptionId, value.trim())
  }).then(() => { proxy.$modal.msgSuccess('已作废'); return loadDetail() })
    .catch(() => {}).finally(() => { closing.value = false })
}

/** E3 验收：处理单据可点回源——异常任务跳排产任务，补料采购单跳采购单详情 */
function goTargetDoc() {
  if (form.value.targetDocType === 'TASK') {
    router.push({ path: '/mes/pro/task', query: { taskCode: form.value.targetDocCode } })
  } else if (form.value.targetDocType === 'PUR_ORDER') {
    router.push({ path: '/mes/pur/order_detail', query: { orderId: form.value.targetDocId } })
  } else {
    proxy.$modal.msgWarning('暂不支持该类型单据的跳转')
  }
}

function goBack() { router.push({ path: '/mes/pro/exception' }) }

if (Number.isNaN(exceptionId)) {
  proxy.$modal.msgError('缺少异常单参数，即将返回台账')
  router.replace({ path: '/mes/pro/exception' })
} else {
  loadDetail()
}
</script>

<style lang="scss" scoped>
.app-container { padding: 16px; }
.mb16 { margin-bottom: 16px; }
.ml8 { margin-left: 8px; }
.header-title { font-weight: bold; font-size: 16px; margin-right: 8px; }
.img-list { display: flex; flex-wrap: wrap; gap: 10px; }
.img-thumb { width: 104px; height: 104px; object-fit: cover; border-radius: 4px; cursor: pointer; border: 1px solid #ebeef5; }
.footer-bar { text-align: center; padding: 12px 0 24px; }
:deep(.el-form-item.is-disabled) { opacity: 0.95; }
</style>
