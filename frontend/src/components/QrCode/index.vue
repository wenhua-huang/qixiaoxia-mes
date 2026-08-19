<template>
  <canvas ref="canvasRef"></canvas>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import QRCode from 'qrcode'

const props = defineProps<{ payload: string; size?: number }>()
const canvasRef = ref<HTMLCanvasElement>()

async function render() {
  if (!canvasRef.value || !props.payload) return
  await QRCode.toCanvas(canvasRef.value, props.payload, { width: props.size ?? 200, margin: 1 })
}

onMounted(render)
watch(() => props.payload, render)
</script>
