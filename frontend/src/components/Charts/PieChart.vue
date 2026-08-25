<template>
  <BaseChart :option="opt" :height="height" />
</template>
<script setup lang="ts">
import { computed } from 'vue'
import BaseChart from './BaseChart.vue'

const props = defineProps<{ data: { name: string; value: number }[]; title?: string; height?: string }>()

const opt = computed(() => ({
  title: props.title ? { text: props.title, textStyle: { fontSize: 14 } } : undefined,
  tooltip: { trigger: 'item' },
  legend: { bottom: 0, type: 'scroll' },
  series: [
    {
      type: 'pie',
      radius: ['40%', '70%'],
      data: props.data || [],
      label: { formatter: '{b}: {c} ({d}%)' }
    }
  ]
}))
</script>
