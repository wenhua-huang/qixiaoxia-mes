<template>
  <el-row :gutter="16">
    <el-col v-for="card in cards" :key="card.key" :xs="12" :sm="6" :md="3">
      <div class="kpi-card" :style="{ borderLeftColor: card.color }">
        <div class="kpi-value" :style="{ color: card.color }">
          {{ card.nullable && overview[card.key] == null ? '—' : (overview[card.key] ?? 0) }}{{ card.suffix || '' }}
        </div>
        <div class="kpi-label">{{ card.label }}</div>
      </div>
    </el-col>
  </el-row>
</template>

<script setup lang="ts">
defineProps<{ overview: any }>()

const cards = [
  { key: 'workorderTotal', label: '工单总数', color: '#409eff' },
  { key: 'workorderCompleted', label: '完工数', color: '#67c23a' },
  { key: 'completionRate', label: '完成率', suffix: '%', color: '#67c23a' },
  { key: 'workorderDelayed', label: '延期数', color: '#f56c6c' },
  { key: 'delayRate', label: '延期率', suffix: '%', color: '#f56c6c' },
  { key: 'workorderInProgress', label: '在制数', color: '#e6a23c' },
  { key: 'actualMinutes', label: '实际工时(分)', color: '#909399' },
  { key: 'efficiencyPercent', label: '效率', suffix: '%', color: '#67c23a', nullable: true }
]
</script>

<style lang="scss" scoped>
.kpi-card {
  background: #fff;
  border-radius: 6px;
  padding: 16px;
  border-left: 4px solid #409eff;
  margin-bottom: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}
.kpi-value { font-size: 24px; font-weight: 600; line-height: 1.4; }
.kpi-label { color: #909399; font-size: 13px; margin-top: 4px; }
</style>
