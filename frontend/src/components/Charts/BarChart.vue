<template>
  <BaseChart :option="opt" :height="height" />
</template>
<script setup lang="ts">
import { computed } from 'vue'
import BaseChart from './BaseChart.vue'

const props = defineProps<{ xData: string[]; series: any[]; title?: string; height?: string }>()

const opt = computed(() => ({
  title: props.title ? { text: props.title, textStyle: { fontSize: 14 } } : undefined,
  tooltip: { trigger: 'axis' },
  legend: { bottom: 0 },
  grid: { left: 48, right: 20, top: 40, bottom: 40 },
  xAxis: { type: 'category', data: props.xData || [] },
  yAxis: { type: 'value' },
  series: (props.series || []).map((s: any) => ({ ...s, type: 'bar' }))
}))
</script>
