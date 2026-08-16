<template>
  <el-dialog title="执行判定" :model-value="visible" @update:model-value="onVisibleChange" width="520px" append-to-body :close-on-click-modal="false">
    <el-form label-width="110px">
      <el-form-item label="实际检测数量">
        <el-input-number v-model="checkQty" :min="1" :max="999999999" size="small" style="width: 160px" :disabled="step !== 'input'" />
        <el-text size="small" type="info" style="margin-left: 8px">最少抽检 {{ minCheck ?? '—' }}，Ac 值 {{ acQuantity ?? '—' }}</el-text>
      </el-form-item>

      <!-- 第一步：本地预判（与服务端判定引擎同公式；正式结果以服务端为准） -->
      <template v-if="step === 'result' && !finalResult">
        <el-form-item label="预判结果">
          <el-tag :type="predicted === 'PASS' ? 'success' : 'danger'" size="large" disable-transitions>{{ predicted === 'PASS' ? 'PASS 合格' : 'FAIL 不合格' }}</el-tag>
          <el-text size="small" type="info" style="margin-left: 8px">预判仅供参考，正式结果以服务端判定为准</el-text>
        </el-form-item>
        <el-form-item label="判定依据" v-if="predictReason">
          <el-text size="small">{{ predictReason }}</el-text>
        </el-form-item>
        <!-- 预判 FAIL：提交前选择是否让步接收（服务端判定 COMPLETED 后不可再判定，让步须随本次提交） -->
        <template v-if="predicted === 'FAIL'">
          <el-alert type="warning" :closable="false" title="预判不合格。可按不合格提交，或填写让步理由随本次判定一并提交升级为让步接收。" style="margin-bottom: 12px" />
          <el-form-item label="让步理由">
            <el-input v-model="concessionReason" type="textarea" :rows="3" placeholder="如选择让步接收则必填，如：轻微外观缺陷，客户书面同意让步接收" />
          </el-form-item>
        </template>
      </template>

      <!-- 第二步：服务端最终结果回显 -->
      <el-form-item label="判定结果" v-if="finalResult">
        <el-tag :type="finalTagType" size="large" disable-transitions>{{ finalText }}</el-tag>
      </el-form-item>
      <el-alert v-if="finalResult === 'FAIL'" type="error" :closable="false" title="该检验单已判定不合格，不可再判定。" style="margin-bottom: 12px" />
    </el-form>

    <template #footer>
      <!-- 录入步骤 -->
      <el-button v-if="step === 'input'" type="primary" :loading="loading" @click="doPredict">执行判定</el-button>
      <!-- 预判结果步骤：单次提交（让步理由随本次判定带上） -->
      <template v-else-if="!finalResult">
        <el-button v-if="predicted === 'FAIL'" type="danger" :loading="loading" @click="submit()">按不合格提交</el-button>
        <el-button v-if="predicted === 'FAIL'" type="warning" :loading="loading" :disabled="!concessionReason || !concessionReason.trim()" @click="submit(concessionReason.trim())">让步接收</el-button>
        <el-button v-else type="primary" :loading="loading" @click="submit()">确认判定</el-button>
        <el-button @click="step = 'input'">返 回</el-button>
      </template>
      <el-button v-else type="primary" @click="onVisibleChange(false)">关 闭</el-button>
      <el-button v-if="step === 'input'" @click="onVisibleChange(false)">取 消</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts" name="QcJudgeDialog">
import { ref, watch, computed } from 'vue'
import { getCurrentInstance } from 'vue'
import type { QcOrderLine, QcDefectRecord } from '@/api/mes/qc/iqc'

const props = defineProps<{
  visible: boolean
  /** 已保存的实际检测数量（回显） */
  quantityCheck?: number
  /** 抽检样本量（默认值来源） */
  minCheck?: number
  /** Ac 值（最大不合格数，判定依据展示） */
  acQuantity?: number
  /** 三档缺陷率阈值(%)，预判依据 */
  crRateLimit?: number
  majRateLimit?: number
  minRateLimit?: number
  /** 判定引擎输入：检验行 + 缺陷记录 */
  lines: QcOrderLine[]
  defects: QcDefectRecord[]
  loading?: boolean
}>()
const emit = defineEmits(['update:visible', 'judge'])
const { proxy } = getCurrentInstance() as any

const checkQty = ref<number | undefined>(undefined)
const step = ref<'input' | 'result'>('input')
const predicted = ref<'PASS' | 'FAIL'>('PASS')
const predictReason = ref('')
const concessionReason = ref('')
const finalResult = ref<string | null>(null)

const finalTagType = computed(() => (finalResult.value === 'PASS' ? 'success' : finalResult.value === 'CONCESSION' ? 'warning' : 'danger'))
const finalText = computed(() => (finalResult.value === 'PASS' ? 'PASS 合格' : finalResult.value === 'CONCESSION' ? 'CONCESSION 让步接收' : 'FAIL 不合格'))

watch(
  () => props.visible,
  v => {
    if (v) {
      checkQty.value = props.quantityCheck ?? props.minCheck ?? 1
      step.value = 'input'
      predicted.value = 'PASS'
      predictReason.value = ''
      concessionReason.value = ''
      finalResult.value = null
    }
  }
)

function onVisibleChange(v: boolean) {
  if (!v) finalResult.value = null
  emit('update:visible', v)
}

/** 行判定（NUMBER 按区间公式，其余取行结果），与 QcJudgeServiceImpl.judgeLine 同公式 */
function judgeLine(line: QcOrderLine): 'PASS' | 'FAIL' | null {
  if (line.qcResultType !== 'NUMBER') return (line.lineResult as 'PASS' | 'FAIL') || null
  if (!line.checkValText) return null
  const val = Number(line.checkValText)
  if (Number.isNaN(val)) return null
  const std = line.standerVal
  const lo = std != null && line.thresholdMin != null ? std + line.thresholdMin : line.thresholdMin
  const hi = std != null && line.thresholdMax != null ? std + line.thresholdMax : line.thresholdMax
  return (lo != null && val < lo) || (hi != null && val > hi) ? 'FAIL' : 'PASS'
}

/** 整单预判：镜像服务端引擎（Ac/致命缺陷/三档缺陷率），未录入实测值时拦截 */
function doPredict() {
  if (!checkQty.value || checkQty.value < 1) {
    proxy.$modal.msgWarning('请填写实际检测数量')
    return
  }
  let failLines = 0
  const unentered = props.lines.find(l => judgeLine(l) == null)
  if (unentered) {
    proxy.$modal.msgError(`检测项[${unentered.indexName}]未录入结果，无法判定`)
    return
  }
  for (const line of props.lines) {
    if (judgeLine(line) === 'FAIL') failLines++
  }
  let cr = 0, maj = 0, min = 0
  for (const d of props.defects) {
    const q = d.defectQuantity ?? 1
    if (d.defectLevel === 'CRITICAL') cr += q
    else if (d.defectLevel === 'MAJOR') maj += q
    else if (d.defectLevel === 'MINOR') min += q
  }
  const unqualified = Math.max(cr + maj + min, failLines)
  const pct = (q: number) => Math.round((q * 10000) / checkQty.value!) / 100
  const reasons: string[] = []
  let result: 'PASS' | 'FAIL' = 'PASS'
  if (props.acQuantity != null && unqualified > props.acQuantity) {
    result = 'FAIL'
    reasons.push(`不合格数 ${unqualified} 超过 Ac 值 ${props.acQuantity}`)
  }
  if (cr > 0) {
    result = 'FAIL'
    reasons.push(`存在致命缺陷 ${cr} 件`)
  }
  if (pct(cr) > (props.crRateLimit ?? 0) || pct(maj) > (props.majRateLimit ?? 0) || pct(min) > (props.minRateLimit ?? 0)) {
    result = 'FAIL'
    reasons.push(`缺陷率超阈值（致命 ${pct(cr)}%/${props.crRateLimit ?? 0}%，严重 ${pct(maj)}%/${props.majRateLimit ?? 0}%，轻微 ${pct(min)}%/${props.minRateLimit ?? 0}%）`)
  }
  if (!reasons.length) reasons.push(`不合格数 ${unqualified} ≤ Ac 值，缺陷率未超阈值`)
  predicted.value = result
  predictReason.value = reasons.join('；')
  step.value = 'result'
}

/** 单次提交判定（concessionReason 非空时服务端 FAIL→CONCESSION） */
function submit(concessionReason?: string) {
  if (!checkQty.value) return
  emit('judge', { quantityCheck: checkQty.value, concessionReason })
}

/** 供父组件回写服务端最终判定结果 */
function setResult(r: string) {
  finalResult.value = r
}

defineExpose({ setResult })
</script>
