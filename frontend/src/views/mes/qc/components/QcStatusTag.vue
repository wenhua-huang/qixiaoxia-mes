<template>
  <el-tag v-if="status && status !== 'NONE'" size="small" :type="tagType" :style="{ cursor: 'pointer' }"
    @click="$emit('click')">{{ tagText }}</el-tag>
  <span v-else>-</span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ status?: string | null }>()
defineEmits<{ click: [] }>()

// 检验状态口径全端统一：PASSED/CONCESSION/PENDING/FAILED，无单(NONE)显示 -
const QC_TAG: Record<string, { type: string; text: string }> = {
  PASSED: { type: 'success', text: '检验合格' },
  CONCESSION: { type: 'warning', text: '让步接收' },
  PENDING: { type: 'info', text: '待检验' },
  FAILED: { type: 'danger', text: '检验不合格' }
}
const tagType = computed(() => QC_TAG[props.status || '']?.type || 'info')
const tagText = computed(() => QC_TAG[props.status || '']?.text || props.status || '')
</script>
