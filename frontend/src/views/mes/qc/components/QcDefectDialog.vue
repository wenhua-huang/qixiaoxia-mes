<template>
  <el-dialog title="缺陷记录" :model-value="visible" @update:model-value="emit('update:visible', $event)" width="980px" append-to-body :close-on-click-modal="false">
    <el-row class="mb8" v-if="!readonly">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" size="small" @click="addRow">新增缺陷记录</el-button>
      </el-col>
    </el-row>
    <el-table :data="defects" border size="small">
      <el-table-column label="关联检测项" width="160" v-if="lines && lines.length">
        <template #default="scope">
          <el-select v-model="scope.row.lineId" placeholder="可选" size="small" clearable filterable style="width: 130px" :disabled="readonly">
            <el-option v-for="l in lines" :key="l.lineId" :label="l.indexName" :value="l.lineId!" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="缺陷" min-width="200">
        <template #default="scope">
          <el-select v-if="!readonly" v-model="scope.row.defectId" placeholder="请选择缺陷" size="small" filterable style="width: 100%" @change="onDefectChange(scope.row)">
            <el-option v-for="d in defectOptions" :key="d.defectId" :label="`${d.defectCode} ${d.defectName}`" :value="d.defectId!" />
          </el-select>
          <span v-else>{{ scope.row.defectCode }} {{ scope.row.defectName }}</span>
        </template>
      </el-table-column>
      <el-table-column label="等级" width="100" align="center">
        <template #default="scope">
          <dict-tag v-if="mes_qc_defect_level" :options="mes_qc_defect_level" :value="scope.row.defectLevel" />
        </template>
      </el-table-column>
      <el-table-column label="数量" width="110" align="center">
        <template #default="scope">
          <el-input-number v-model="scope.row.defectQuantity" :min="1" :max="999999" :controls="false" size="small" style="width: 80px" :disabled="readonly" />
        </template>
      </el-table-column>
      <el-table-column label="处置方法" min-width="130">
        <template #default="scope">
          <el-input v-model="scope.row.processMethod" size="small" placeholder="如 退货/挑选使用" :disabled="readonly" />
        </template>
      </el-table-column>
      <el-table-column label="图片URL" min-width="130">
        <template #default="scope">
          <el-input v-model="scope.row.defectImage" size="small" placeholder="缺陷图片 URL" :disabled="readonly" />
        </template>
      </el-table-column>
      <el-table-column label="备注" min-width="110">
        <template #default="scope">
          <el-input v-model="scope.row.remark" size="small" placeholder="备注" :disabled="readonly" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="70" align="center" v-if="!readonly">
        <template #default="scope">
          <el-button link type="danger" icon="Delete" size="small" @click="defects.splice(scope.$index, 1)" />
        </template>
      </el-table-column>
    </el-table>
    <template #footer>
      <el-button @click="emit('update:visible', false)">关 闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts" name="QcDefectDialog">
import { ref, watch } from 'vue'
import type { QcDefectRecord, QcOrderLine } from '@/api/mes/qc/iqc'
import type { QcDefect } from '@/api/mes/qc/defect'
import { listDefect } from '@/api/mes/qc/defect'

const props = defineProps<{
  visible: boolean
  defects: QcDefectRecord[]
  /** IQC/IPQC/OQC/RQC — 缺陷字典按检验类型过滤 */
  qcType: string
  readonly?: boolean
  /** 可选：行选择下拉（lineId 关联检测项） */
  lines?: QcOrderLine[]
}>()
const emit = defineEmits(['update:visible'])

const { mes_qc_defect_level } = useDict('mes_qc_defect_level')

const defectOptions = ref<QcDefect[]>([])

watch(
  () => props.visible,
  v => {
    if (v && defectOptions.value.length === 0) {
      listDefect({ indexType: props.qcType, enableFlag: '1', pageNum: 1, pageSize: 500 }).then((r: any) => {
        defectOptions.value = r.rows || []
      })
    }
  },
  { immediate: true }
)

/** 新增空缺陷行（增删直接改 props 数组，与波次一 Tab 组件同款模式） */
function addRow() {
  props.defects.push({ defectQuantity: 1 })
}

/** 选缺陷字典带出编码/名称/等级/处置方法 */
function onDefectChange(row: QcDefectRecord) {
  const opt = defectOptions.value.find(d => d.defectId === row.defectId)
  if (opt) {
    row.defectCode = opt.defectCode
    row.defectName = opt.defectName
    row.defectLevel = opt.defectLevel
    row.processMethod = opt.processMethod
  }
}

defineExpose({
  /** 提交前校验：未选缺陷的行返回错误文案 */
  validate: (): string | null => {
    const empty = props.defects.find(d => !d.defectId)
    if (empty) return '存在未选择缺陷的缺陷记录行'
    return null
  }
})
</script>
