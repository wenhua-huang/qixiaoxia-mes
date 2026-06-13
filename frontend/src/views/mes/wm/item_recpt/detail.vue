<template>
  <div>
    <el-table v-loading="loading" :data="lineList" border>
      <el-table-column label="物料编码" align="center" prop="itemCode" width="120" />
      <el-table-column label="物料名称" align="center" prop="itemName" :show-overflow-tooltip="true" />
      <el-table-column label="规格型号" align="center" prop="specification" :show-overflow-tooltip="true" width="120" />
      <el-table-column label="单位" align="center" prop="unitName" width="70" />
      <el-table-column label="数量" align="center" prop="quantityRecpt" width="100" />
      <el-table-column label="批次号" align="center" prop="batchCode" width="100" />
      <el-table-column label="仓库" align="center" prop="warehouseId" width="80" />
    </el-table>
  </div>
</template>

<script setup lang="ts" name="WmItemRecptDetail">
import { ref, watch } from 'vue'
import type { WmItemRecptLine, WmItemRecptLineQueryParams } from '@/types/api/mes/wm/item_recpt_line'
import { listWmItemRecptLine } from '@/api/mes/wm/item_recpt_line'

const props = defineProps<{ recptId: number }>()
const lineList = ref<WmItemRecptLine[]>([])
const loading = ref(true)

watch(() => props.recptId, (val) => {
  if (val) {
    loading.value = true
    listWmItemRecptLine({ pageNum: 1, pageSize: 1000, recptId: val } as WmItemRecptLineQueryParams).then(r => {
      lineList.value = r.rows; loading.value = false
    })
  }
}, { immediate: true })
</script>
