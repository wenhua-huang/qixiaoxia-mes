<template>
  <div>
    <el-row class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" size="small" @click="handleAddRow">新增检测项行</el-button>
      </el-col>
    </el-row>
    <el-table :data="rows" border size="small">
      <el-table-column label="检测项" min-width="220">
        <template #default="scope">
          <el-select v-model="scope.row.indexId" placeholder="请选择检测项" filterable size="small" style="width: 100%" @change="onIndexChange(scope.row)">
            <el-option v-for="opt in indexOptions" :key="opt.indexId" :label="`${opt.indexCode} ${opt.indexName}`" :value="opt.indexId" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="工具" prop="qcTool" width="90" align="center" />
      <el-table-column label="值类型" width="80" align="center">
        <template #default="scope">
          <dict-tag v-if="mes_qc_result_type" :options="mes_qc_result_type" :value="scope.row.qcResultType" />
        </template>
      </el-table-column>
      <el-table-column label="标准值" width="120" align="center">
        <template #default="scope">
          <el-input-number v-model="scope.row.standerVal" :precision="4" :controls="false" size="small" style="width: 100px" placeholder="标准值" />
        </template>
      </el-table-column>
      <el-table-column label="单位" width="90" align="center">
        <template #default="scope">
          <el-input v-model="scope.row.unitOfMeasure" size="small" placeholder="如 mm" />
        </template>
      </el-table-column>
      <el-table-column label="下偏差" width="110" align="center">
        <template #default="scope">
          <el-input-number v-model="scope.row.thresholdMin" :precision="4" :controls="false" size="small" style="width: 90px" placeholder="下偏差" />
        </template>
      </el-table-column>
      <el-table-column label="上偏差" width="110" align="center">
        <template #default="scope">
          <el-input-number v-model="scope.row.thresholdMax" :precision="4" :controls="false" size="small" style="width: 90px" placeholder="上偏差" />
        </template>
      </el-table-column>
      <el-table-column label="检测方法/要求" min-width="140">
        <template #default="scope">
          <el-input v-model="scope.row.checkMethod" size="small" placeholder="如 目视无划痕" />
        </template>
      </el-table-column>
      <el-table-column label="排序" width="90" align="center">
        <template #default="scope">
          <el-input-number v-model="scope.row.orderNum" :min="0" :controls="false" size="small" style="width: 60px" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="70" align="center">
        <template #default="scope">
          <el-button link type="danger" icon="Delete" size="small" @click="rows.splice(scope.$index, 1)" />
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts" name="TemplateIndexRowsTab">
import { ref, onMounted, getCurrentInstance } from 'vue'
import type { QcTemplateIndexRow } from '@/api/mes/qc/template'
import type { QcIndex } from '@/api/mes/qc/index'
import { listIndex } from '@/api/mes/qc/index'

const { proxy } = getCurrentInstance() as any
const { mes_qc_result_type } = useDict('mes_qc_result_type')

const props = defineProps<{ rows: QcTemplateIndexRow[] }>()

const indexOptions = ref<QcIndex[]>([])

onMounted(() => {
  listIndex({ pageNum: 1, pageSize: 500, enableFlag: '1' }).then((r: any) => {
    indexOptions.value = r.rows || []
  })
})

/** 新增空行（排序自动递增） */
function handleAddRow() {
  props.rows.push({ indexId: undefined, orderNum: props.rows.length + 1 })
}

/** 选择检测项后带出编码/名称/工具/值类型 */
function onIndexChange(row: QcTemplateIndexRow) {
  const opt = indexOptions.value.find(i => i.indexId === row.indexId)
  if (opt) {
    row.indexCode = opt.indexCode
    row.indexName = opt.indexName
    row.indexType = opt.indexType
    row.qcTool = opt.qcTool
    row.qcResultType = opt.qcResultType
  }
}

defineExpose({ validate: (): string | null => {
  const empty = props.rows.find(r => !r.indexId)
  if (empty) return '检测项 Tab 存在未选择检测项的行'
  return null
} })
</script>
