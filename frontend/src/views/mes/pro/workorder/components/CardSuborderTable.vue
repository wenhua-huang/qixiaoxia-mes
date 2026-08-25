<template>
  <el-table :data="rows" border stripe size="small" style="width:100%">
    <el-table-column label="流转卡" min-width="160">
      <template #default="{ row }">
        <div>{{ row.cardCode || '—' }}</div>
        <div class="sub" v-if="row.batchCode">{{ row.batchCode }}</div>
      </template>
    </el-table-column>
    <el-table-column label="当前工序" min-width="140">
      <template #default="{ row }">{{ row.currentProcessName || '—' }}</template>
    </el-table-column>
    <el-table-column label="工序进度" min-width="170" align="center">
      <template #default="{ row }">
        <el-progress :percentage="cardCompletion(row)" :stroke-width="14" />
        <div class="sub">{{ row.finishedProcessCount ?? 0 }} / {{ row.totalProcessCount ?? 0 }}</div>
      </template>
    </el-table-column>
    <el-table-column label="流转数量" prop="quantityTransfered" width="100" align="center" />
    <el-table-column label="开始时间" min-width="140">
      <template #default="{ row }">{{ fmtDateTime(row.actualStartTime) }}</template>
    </el-table-column>
    <el-table-column label="完工时间" min-width="140">
      <template #default="{ row }">{{ fmtDateTime(row.actualEndTime) }}</template>
    </el-table-column>
    <el-table-column label="状态" width="100" align="center">
      <template #default="{ row }"><el-tag>{{ row.status }}</el-tag></template>
    </el-table-column>
    <el-table-column label="完成率" min-width="150" align="center">
      <template #default="{ row }">
        <el-progress :percentage="row.completionRate ?? 0" :stroke-width="14" />
      </template>
    </el-table-column>
  </el-table>
</template>

<script setup lang="ts">
import { parseTime } from '@/utils/ruoyi'

defineProps<{ rows: any[] }>()

function cardCompletion(row: any): number {
  const total = Number(row.totalProcessCount) || 0
  const finished = Number(row.finishedProcessCount) || 0
  if (total <= 0) return 0
  return Math.round((finished / total) * 100)
}

function fmtDateTime(v: any): string {
  if (!v) return '—'
  return parseTime(v, '{y}-{m}-{d} {h}:{i}')
}
</script>

<style scoped>
.sub { font-size: 12px; color: #909399; line-height: 1.4; }
</style>
