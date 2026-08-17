<template>
  <el-drawer v-model="visible" :title="drawerTitle" size="72%" :close-on-click-modal="false">
    <template #header>
      <div>
        <span style="font-size: 16px; font-weight: 600">{{ drawerTitle }}</span>
        <dict-tag v-if="form.status" :options="mes_qc_status" :value="form.status" style="margin-left: 10px" />
        <el-tag v-if="typeTag" size="small" style="margin-left: 8px">{{ typeTag }}</el-tag>
      </div>
    </template>

    <!-- 基本信息（只读区，头字段由来源单据/手工建单生成，不可改） -->
    <el-descriptions :column="3" border size="small" class="mb8">
      <el-descriptions-item label="物料">{{ form.itemCode }} {{ form.itemName }}</el-descriptions-item>
      <el-descriptions-item label="规格/单位">{{ form.specification || '—' }} / {{ form.unitOfMeasure || '—' }}</el-descriptions-item>
      <el-descriptions-item label="工单">{{ form.workorderCode || '—' }}</el-descriptions-item>
      <el-descriptions-item label="流转卡/任务">{{ form.cardCode || '—' }} / {{ form.taskCode || '—' }}</el-descriptions-item>
      <el-descriptions-item label="工序">{{ form.processCode || '—' }} {{ form.processName || '' }}</el-descriptions-item>
      <el-descriptions-item label="工位">{{ form.workstationName || '—' }}</el-descriptions-item>
      <el-descriptions-item label="来源单据">{{ sourceText }}</el-descriptions-item>
      <el-descriptions-item label="抽检量/Ac值">{{ form.quantityMinCheck ?? '—' }} / {{ form.quantityMaxUnqualified ?? '—' }}</el-descriptions-item>
      <el-descriptions-item label="缺陷率阈值(致/严/轻)">{{ form.crRateLimit ?? '—' }}% / {{ form.majRateLimit ?? '—' }}% / {{ form.minRateLimit ?? '—' }}%</el-descriptions-item>
      <el-descriptions-item label="实际检测数量">
        <el-input-number v-if="!readonly" v-model="quantityCheck" :min="1" :controls="false" size="small" style="width: 110px" />
        <span v-else>{{ form.quantityCheck ?? '—' }}</span>
      </el-descriptions-item>
    </el-descriptions>

    <!-- 判定汇总（已完成后展示） -->
    <el-descriptions v-if="form.status === 'COMPLETED'" :column="4" border size="small" class="mb8">
      <el-descriptions-item label="判定结果">
        <dict-tag :options="mes_qc_check_result" :value="form.checkResult" />
      </el-descriptions-item>
      <el-descriptions-item label="合格/不合格数">{{ form.quantityQualified ?? '—' }} / {{ form.quantityUnqualified ?? '—' }}</el-descriptions-item>
      <el-descriptions-item label="缺陷数(致/严/轻)">{{ form.crQuantity ?? 0 }} / {{ form.majQuantity ?? 0 }} / {{ form.minQuantity ?? 0 }}</el-descriptions-item>
      <el-descriptions-item label="缺陷率(致/严/轻)">{{ form.crRate ?? 0 }}% / {{ form.majRate ?? 0 }}% / {{ form.minRate ?? 0 }}%</el-descriptions-item>
      <el-descriptions-item label="让步理由" :span="4" v-if="form.checkResult === 'CONCESSION'">{{ form.concessionReason }}</el-descriptions-item>
    </el-descriptions>

    <!-- 检测项录入 -->
    <el-divider content-position="left">检测项实测值</el-divider>
    <QcLineEditor :lines="lines" :readonly="readonly" />

    <!-- 缺陷记录 -->
    <el-divider content-position="left">
      <span>缺陷记录（{{ defectRecords.length }} 条）</span>
    </el-divider>
    <el-button v-if="!readonly" type="primary" plain icon="Plus" size="small" class="mb8" @click="defectVisible = true">维护缺陷记录</el-button>
    <el-table v-if="defectRecords.length" :data="defectRecords" border size="small">
      <el-table-column label="缺陷" align="center" min-width="150">
        <template #default="scope">{{ scope.row.defectCode }} {{ scope.row.defectName }}</template>
      </el-table-column>
      <el-table-column label="等级" align="center" width="100">
        <template #default="scope"><dict-tag :options="mes_qc_defect_level" :value="scope.row.defectLevel" /></template>
      </el-table-column>
      <el-table-column label="数量" align="center" prop="defectQuantity" width="70" />
      <el-table-column label="处置方法" align="center" prop="processMethod" min-width="110" :show-overflow-tooltip="true" />
      <el-table-column label="备注" align="center" prop="remark" min-width="110" :show-overflow-tooltip="true" />
    </el-table>
    <el-empty v-else description="暂无缺陷记录" :image-size="50" />

    <template #footer>
      <el-button v-if="!readonly" type="primary" :loading="saving" @click="save">保 存</el-button>
      <el-button v-if="!readonly && canJudge" type="warning" @click="judgeVisible = true" v-hasPermi="['mes:qc:ipqc:judge']">执行判定</el-button>
      <el-button @click="visible = false">关 闭</el-button>
    </template>

    <QcDefectDialog v-model:visible="defectVisible" :defects="defectRecords" qc-type="IPQC" :readonly="readonly" :lines="lines" />
    <QcJudgeDialog ref="judgeDialogRef" v-model:visible="judgeVisible" :quantity-check="quantityCheck" :min-check="form.quantityMinCheck"
      :ac-quantity="form.quantityMaxUnqualified" :cr-rate-limit="form.crRateLimit" :maj-rate-limit="form.majRateLimit" :min-rate-limit="form.minRateLimit"
      :lines="lines" :defects="defectRecords" :loading="judging" @judge="onJudge" />
  </el-drawer>
</template>

<script setup lang="ts" name="IpqcEditDrawer">
import { ref, computed } from 'vue'
import { getCurrentInstance } from 'vue'
import type { QcIpqc, QcOrderLine, QcDefectRecord } from '@/api/mes/qc/ipqc'
import { getIpqc, updateIpqc, judgeIpqc } from '@/api/mes/qc/ipqc'
import QcLineEditor from '../components/QcLineEditor.vue'
import QcDefectDialog from '../components/QcDefectDialog.vue'
import QcJudgeDialog from '../components/QcJudgeDialog.vue'

const { proxy } = getCurrentInstance() as any
const emit = defineEmits(['success'])
const { mes_qc_status, mes_qc_check_result, mes_qc_defect_level } = useDict('mes_qc_status', 'mes_qc_check_result', 'mes_qc_defect_level')

const IPQC_TYPE_TAG: Record<string, string> = {
  FIRST_CHECK: '首检', TOUR_CHECK: '巡检', LAST_CHECK: '完工检', SPOT_CHECK: '抽检'
}

const visible = ref(false)
const readonly = ref(false)
const saving = ref(false)
const judging = ref(false)
const form = ref<QcIpqc>({})
const lines = ref<QcOrderLine[]>([])
const defectRecords = ref<QcDefectRecord[]>([])
const quantityCheck = ref<number | undefined>(undefined)
const defectVisible = ref(false)
const judgeVisible = ref(false)
const judgeDialogRef = ref()

const drawerTitle = computed(() => `${readonly.value ? '检验单详情' : '录入检验'} - ${form.value.ipqcCode || ''}`)
const sourceText = computed(() => `${form.value.sourceDocCode || '—'}（${form.value.sourceDocType || ''}）`)
const typeTag = computed(() => (form.value.ipqcType ? IPQC_TYPE_TAG[form.value.ipqcType] : ''))
const canJudge = computed(() => form.value.status === 'PENDING' || form.value.status === 'INSPECTING')

/** 打开抽屉：readonly=true 详情 / false 录入 */
function open(ipqcId: number, ro: boolean) {
  readonly.value = ro
  visible.value = true
  load(ipqcId)
}

function load(ipqcId: number) {
  getIpqc(ipqcId).then((response: any) => {
    form.value = response.data || {}
    lines.value = form.value.lines || []
    defectRecords.value = form.value.defectRecords || []
    quantityCheck.value = form.value.quantityCheck ?? form.value.quantityMinCheck
  })
}

function validateDefects(): string | null {
  const bad = defectRecords.value.find(d => !d.defectId)
  return bad ? '存在未选择缺陷的缺陷记录行，请先维护' : null
}

function save() {
  const err = validateDefects()
  if (err) {
    proxy.$modal.msgWarning(err)
    return
  }
  saving.value = true
  updateIpqc({ ...form.value, quantityCheck: quantityCheck.value, lines: lines.value, defectRecords: defectRecords.value }).then(() => {
    proxy.$modal.msgSuccess('保存成功')
    emit('success')
    load(form.value.ipqcId!)
  }).finally(() => (saving.value = false))
}

function onJudge(payload: { quantityCheck: number; concessionReason?: string }) {
  const err = validateDefects()
  if (err) {
    proxy.$modal.msgWarning(err)
    return
  }
  judging.value = true
  quantityCheck.value = payload.quantityCheck
  updateIpqc({ ...form.value, quantityCheck: payload.quantityCheck, lines: lines.value, defectRecords: defectRecords.value })
    .then(() => judgeIpqc(form.value.ipqcId!, payload.concessionReason))
    .then(() => getIpqc(form.value.ipqcId!))
    .then((r: any) => {
      form.value = r.data || {}
      readonly.value = true
      emit('success')
      judgeDialogRef.value?.setResult(form.value.checkResult!)
    })
    .catch(() => {})
    .finally(() => (judging.value = false))
}

defineExpose({ open })
</script>
