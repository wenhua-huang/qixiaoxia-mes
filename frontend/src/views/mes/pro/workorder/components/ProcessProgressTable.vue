<template>
  <el-table :data="rows" border stripe size="small" style="width:100%">
    <el-table-column label="工序" min-width="160">
      <template #default="{ row }">
        <div>{{ row.processName || '—' }}</div>
        <div class="sub">{{ row.taskCode }}</div>
      </template>
    </el-table-column>
    <el-table-column label="工作站" min-width="140">
      <template #default="{ row }">
        <span class="dot" :style="{ background: row.colorCode || '#409eff' }"></span>
        <span>{{ row.workstationName || '—' }}</span>
      </template>
    </el-table-column>
    <el-table-column label="计划时间" min-width="150">
      <template #default="{ row }">
        <div>{{ fmtDateTime(row.planStartTime) }}</div>
        <div class="sub">~ {{ fmtDateTime(row.planEndTime) }}</div>
      </template>
    </el-table-column>
    <el-table-column label="实际时间" min-width="150">
      <template #default="{ row }">
        <template v-if="row.actualStartTime || row.actualEndTime">
          <div>{{ fmtDateTime(row.actualStartTime) }}</div>
          <div class="sub">~ {{ fmtDateTime(row.actualEndTime) }}</div>
        </template>
        <span v-else>—</span>
      </template>
    </el-table-column>
    <el-table-column label="数量" min-width="140" align="center">
      <template #default="{ row }">
        <div>{{ row.quantityProduced ?? 0 }} / {{ row.quantity ?? 0 }}</div>
        <div class="sub" v-if="row.quantityQualified != null">合格 {{ row.quantityQualified }}</div>
        <div class="sub bad" v-if="row.quantityUnqualified != null && Number(row.quantityUnqualified) > 0">
          不合格 {{ row.quantityUnqualified }}
        </div>
      </template>
    </el-table-column>
    <el-table-column label="完成率" min-width="160" align="center">
      <template #default="{ row }">
        <el-progress :percentage="row.completionRate ?? 0" :stroke-width="14" />
      </template>
    </el-table-column>
    <el-table-column label="延期" width="100" align="center">
      <template #default="{ row }">
        <el-tag :type="delayType(row.delayLevel)">{{ delayText(row.delayLevel) }}</el-tag>
      </template>
    </el-table-column>
  </el-table>
</template>

<script setup lang="ts">
import { parseTime } from '@/utils/ruoyi'
import { delayText, delayType } from '@/utils/mes/progress'

defineProps<{ rows: any[] }>()

function fmtDateTime(v: any): string {
  if (!v) return '—'
  return parseTime(v, '{y}-{m}-{d} {h}:{i}')
}
</script>

<style scoped>
.sub { font-size: 12px; color: #909399; line-height: 1.4; }
.sub.bad { color: #f56c6c; }
.dot {
  display: inline-block; width: 8px; height: 8px; border-radius: 50%;
  margin-right: 6px; vertical-align: middle;
}
</style>
