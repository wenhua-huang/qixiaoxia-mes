<template>
  <div ref="chartRef" :style="{ width: '100%', height: height || '320px' }" />
</template>
<script setup lang="ts">
import * as echarts from 'echarts'
import { ref, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'

const props = defineProps<{ option: any; height?: string }>()
const chartRef = ref<HTMLElement>()
let chart: echarts.ECharts | null = null
let ro: ResizeObserver | null = null

function render() {
  if (!chartRef.value) return
  if (!chart) chart = echarts.init(chartRef.value)
  chart.setOption(props.option || {}, true)
}
function handleResize() {
  chart?.resize()
}

onMounted(async () => {
  await nextTick()
  render()
  window.addEventListener('resize', handleResize)
  if (chartRef.value && typeof ResizeObserver !== 'undefined') {
    ro = new ResizeObserver(() => chart?.resize())
    ro.observe(chartRef.value)
  }
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  ro?.disconnect()
  chart?.dispose()
  chart = null
})
watch(() => props.option, render, { deep: true })
</script>
