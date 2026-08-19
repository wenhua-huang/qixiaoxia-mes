<template>
  <el-table :data="lines" border size="small">
    <el-table-column label="检测项" min-width="150">
      <template #default="scope">
        <div>{{ scope.row.indexName }}</div>
        <el-text size="small" type="info">{{ scope.row.indexCode }}</el-text>
      </template>
    </el-table-column>
    <el-table-column label="工具" prop="qcTool" width="80" align="center" />
    <el-table-column label="检测方法" prop="checkMethod" min-width="110" :show-overflow-tooltip="true" />
    <el-table-column label="标准值/区间" width="150" align="center">
      <template #default="scope">
        <span v-if="scope.row.qcResultType === 'NUMBER'">{{ rangeText(scope.row) }}</span>
        <span v-else>{{ scope.row.standerVal ?? '—' }}</span>
      </template>
    </el-table-column>
    <el-table-column label="实测值" width="170" align="center">
      <template #default="scope">
        <!-- NUMBER：数字输入，存字符串文本（服务端判定） -->
        <el-input-number v-if="scope.row.qcResultType === 'NUMBER'" :model-value="toNum(scope.row.checkValText)"
          @update:model-value="(v: any) => (scope.row.checkValText = v == null ? undefined : String(v))"
          :precision="4" :controls="false" size="small" style="width: 130px" placeholder="实测值" :disabled="readonly" />
        <!-- COUNT：计数输入，同存字符串 -->
        <el-input-number v-else-if="scope.row.qcResultType === 'COUNT'" :model-value="toNum(scope.row.checkValText)"
          @update:model-value="(v: any) => (scope.row.checkValText = v == null ? undefined : String(v))"
          :precision="0" :min="0" :controls="false" size="small" style="width: 130px" placeholder="计数值" :disabled="readonly" />
        <!-- DICT：行快照无 dict_type，用 PASS/FAIL 二值 -->
        <el-select v-else-if="scope.row.qcResultType === 'DICT'" v-model="scope.row.checkValText" size="small" style="width: 130px" placeholder="请选择" :disabled="readonly">
          <el-option label="PASS" value="PASS" />
          <el-option label="FAIL" value="FAIL" />
        </el-select>
        <el-input v-else v-model="scope.row.checkValText" size="small" style="width: 130px"
          :placeholder="scope.row.qcResultType === 'FILE' ? '图片/文件 URL' : '实测内容'" :disabled="readonly" />
      </template>
    </el-table-column>
    <el-table-column label="缺陷数(致/严/轻)" width="190" align="center">
      <template #default="scope">
        <el-input-number v-model="scope.row.crQuantity" :min="0" :max="99" :controls="false" size="small" style="width: 52px" placeholder="致" :disabled="readonly" />
        <el-input-number v-model="scope.row.majQuantity" :min="0" :max="99" :controls="false" size="small" style="width: 52px; margin: 0 3px" placeholder="严" :disabled="readonly" />
        <el-input-number v-model="scope.row.minQuantity" :min="0" :max="99" :controls="false" size="small" style="width: 52px" placeholder="轻" :disabled="readonly" />
      </template>
    </el-table-column>
    <el-table-column label="行结果" width="140" align="center">
      <template #default="scope">
        <template v-if="scope.row.qcResultType === 'NUMBER'">
          <!-- 服务端判定为准，前端仅按同公式预览提示 -->
          <el-tag v-if="previewResult(scope.row)" size="small" :type="previewResult(scope.row) === 'PASS' ? 'success' : 'danger'" disable-transitions>
            {{ previewResult(scope.row) === 'PASS' ? '预览:合格' : '预览:超差' }}
          </el-tag>
          <el-tag v-else size="small" type="info" disable-transitions>判定时自动计算</el-tag>
        </template>
        <el-select v-else v-model="scope.row.lineResult" size="small" style="width: 100px" placeholder="请选择" :disabled="readonly">
          <el-option label="PASS" value="PASS" />
          <el-option label="FAIL" value="FAIL" />
        </el-select>
      </template>
    </el-table-column>
    <el-table-column label="备注" min-width="110">
      <template #default="scope">
        <el-input v-model="scope.row.remark" size="small" placeholder="备注" :disabled="readonly" />
      </template>
    </el-table-column>
  </el-table>
</template>

<script setup lang="ts" name="QcLineEditor">
import type { QcOrderLine } from '@/api/mes/qc/common'

const props = defineProps<{ lines: QcOrderLine[]; readonly?: boolean }>()

/** 文本实测值 → 数字输入 model（非数字文本回退空） */
function toNum(v?: string): number | undefined {
  if (v == null || v === '') return undefined
  const n = Number(v)
  return Number.isNaN(n) ? undefined : n
}

/** 标准区间文本：[std+min, std+max]，std 空显示绝对区间 */
function rangeText(row: QcOrderLine): string {
  const std = row.standerVal
  const lo = std != null && row.thresholdMin != null ? std + row.thresholdMin : row.thresholdMin
  const hi = std != null && row.thresholdMax != null ? std + row.thresholdMax : row.thresholdMax
  if (lo == null && hi == null) return row.standerVal != null ? `${row.standerVal}` : '—'
  if (lo == null) return `≤${hi}`
  if (hi == null) return `≥${lo}`
  return `[${lo}, ${hi}]`
}

/** NUMBER 行前端预览（与服务端 judgeLine 同公式，仅提示） */
function previewResult(row: QcOrderLine): 'PASS' | 'FAIL' | null {
  if (row.qcResultType !== 'NUMBER' || row.checkValText == null || row.checkValText === '') return null
  const val = toNum(row.checkValText)
  if (val == null) return null
  const std = row.standerVal
  const lo = std != null && row.thresholdMin != null ? std + row.thresholdMin : row.thresholdMin
  const hi = std != null && row.thresholdMax != null ? std + row.thresholdMax : row.thresholdMax
  const fail = (lo != null && val < lo) || (hi != null && val > hi)
  return fail ? 'FAIL' : 'PASS'
}
</script>
