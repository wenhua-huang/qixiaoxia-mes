<template>
  <el-drawer v-model="visible" :title="drawerTitle" size="72%" :close-on-click-modal="false">
    <template #header>
      <div>
        <span style="font-size: 16px; font-weight: 600">{{ drawerTitle }}</span>
        <dict-tag v-if="form.status" :options="mes_qc_status" :value="form.status" style="margin-left: 10px" />
      </div>
    </template>

    <!-- 基本信息（只读区，头字段由来源单据生成，不可改） -->
    <el-descriptions :column="3" border size="small" class="mb8">
      <el-descriptions-item label="物料">{{ form.itemCode }} {{ form.itemName }}</el-descriptions-item>
      <el-descriptions-item label="规格/单位">{{ form.specification || '—' }} / {{ form.unitOfMeasure || '—' }}</el-descriptions-item>
      <el-descriptions-item label="客户">{{ form.clientName || '—' }}（批次：{{ form.batchCode || '—' }}）</el-descriptions-item>
      <el-descriptions-item label="来源单据">{{ sourceText }}</el-descriptions-item>
      <el-descriptions-item label="出库数量">{{ form.quantityOut ?? '—' }}</el-descriptions-item>
      <el-descriptions-item label="抽检量/Ac值">{{ form.quantityMinCheck ?? '—' }} / {{ form.quantityMaxUnqualified ?? '—' }}</el-descriptions-item>
      <el-descriptions-item label="缺陷率阈值(致/严/轻)">{{ form.crRateLimit ?? '—' }}% / {{ form.majRateLimit ?? '—' }}% / {{ form.minRateLimit ?? '—' }}%</el-descriptions-item>
      <el-descriptions-item label="出货日期">{{ form.outDate || '—' }}</el-descriptions-item>
      <el-descriptions-item label="实际检测数量">
        <el-input-number v-if="!readonly" v-model="quantityCheck" :min="1" :controls="false" size="small" style="width: 110px" />
        <span v-else>{{ form.quantityCheck ?? '—' }}</span>
      </el-descriptions-item>
    </el-descriptions>

    <!-- 判定汇总（已完成/已判定后展示） -->
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
      <el-button v-if="!readonly && canJudge" type="warning" @click="judgeVisible = true" v-hasPermi="['mes:qc:oqc:judge']">执行判定</el-button>
      <el-button @click="visible = false">关 闭</el-button>
    </template>

    <QcDefectDialog v-model:visible="defectVisible" :defects="defectRecords" qc-type="OQC" :readonly="readonly" :lines="lines" />
    <QcJudgeDialog ref="judgeDialogRef" v-model:visible="judgeVisible" :quantity-check="quantityCheck" :min-check="form.quantityMinCheck"
      :ac-quantity="form.quantityMaxUnqualified" :cr-rate-limit="form.crRateLimit" :maj-rate-limit="form.majRateLimit" :min-rate-limit="form.minRateLimit"
      :lines="lines" :defects="defectRecords" :loading="judging" @judge="onJudge" />
  </el-drawer>
</template>

<script setup lang="ts" name="OqcEditDrawer">
import { ref, computed } from 'vue'
import { getCurrentInstance } from 'vue'
import type { QcOqc, QcOrderLine, QcDefectRecord } from '@/api/mes/qc/oqc'
import { getOqc, updateOqc, judgeOqc } from '@/api/mes/qc/oqc'
import QcLineEditor from '../components/QcLineEditor.vue'
import QcDefectDialog from '../components/QcDefectDialog.vue'
import QcJudgeDialog from '../components/QcJudgeDialog.vue'

const { proxy } = getCurrentInstance() as any
const emit = defineEmits(['success'])
const { mes_qc_status, mes_qc_check_result, mes_qc_defect_level } = useDict('mes_qc_status', 'mes_qc_check_result', 'mes_qc_defect_level')

const visible = ref(false)
const readonly = ref(false)
const saving = ref(false)
const judging = ref(false)
const form = ref<QcOqc>({})
const lines = ref<QcOrderLine[]>([])
const defectRecords = ref<QcDefectRecord[]>([])
const quantityCheck = ref<number | undefined>(undefined)
const defectVisible = ref(false)
const judgeVisible = ref(false)
const judgeDialogRef = ref()

const drawerTitle = computed(() => `${readonly.value ? '检验单详情' : '录入检验'} - ${form.value.oqcCode || ''}`)
const sourceText = computed(() => `${form.value.sourceDocCode || '—'}（${form.value.sourceDocType || ''}）`)
const canJudge = computed(() => form.value.status === 'PENDING' || form.value.status === 'INSPECTING')

/** 打开抽屉：readonly=true 详情 / false 录入 */
function open(oqcId: number, ro: boolean) {
  readonly.value = ro
  visible.value = true
  load(oqcId)
}

function load(oqcId: number) {
  getOqc(oqcId).then((response: any) => {
    form.value = response.data || {}
    lines.value = form.value.lines || []
    defectRecords.value = form.value.defectRecords || []
    quantityCheck.value = form.value.quantityCheck ?? form.value.quantityMinCheck
  })
}

/** 缺陷行校验：未选缺陷字典的行拦截 */
function validateDefects(): string | null {
  const bad = defectRecords.value.find(d => !d.defectId)
  return bad ? '存在未选择缺陷的缺陷记录行，请先维护' : null
}

/** 整头保存：lines + defectRecords 永远随头提交（null=不动是给仅改头场景留的） */
function save() {
  const err = validateDefects()
  if (err) {
    proxy.$modal.msgWarning(err)
    return
  }
  saving.value = true
  updateOqc({ ...form.value, quantityCheck: quantityCheck.value, lines: lines.value, defectRecords: defectRecords.value }).then(() => {
    proxy.$modal.msgSuccess('保存成功')
    emit('success')
    load(form.value.oqcId!)
  }).finally(() => (saving.value = false))
}

/** 判定：先自动保存（保证判定基于最新录入），再单次 judge（让步理由随本次提交）；结果回显由 setResult 驱动 */
function onJudge(payload: { quantityCheck: number; concessionReason?: string }) {
  const err = validateDefects()
  if (err) {
    proxy.$modal.msgWarning(err)
    return
  }
  judging.value = true
  quantityCheck.value = payload.quantityCheck
  updateOqc({ ...form.value, quantityCheck: payload.quantityCheck, lines: lines.value, defectRecords: defectRecords.value })
    .then(() => judgeOqc(form.value.oqcId!, payload.concessionReason))
    .then(() => getOqc(form.value.oqcId!))
    .then((r: any) => {
      form.value = r.data || {}
      readonly.value = true   // 判定后 COMPLETED，编辑入口关闭
      emit('success')
      judgeDialogRef.value?.setResult(form.value.checkResult!)
    })
    .catch(() => {})
    .finally(() => (judging.value = false))
}

defineExpose({ open })
</script>
