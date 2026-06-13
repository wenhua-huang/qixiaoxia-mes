<template>
  <div>
    <el-table v-loading="loading" :data="lineList" border>
      <el-table-column label="物料编码" align="center" prop="itemCode" width="120" />
      <el-table-column label="物料名称" align="center" prop="itemName" width="150" />
      <el-table-column label="规格型号" align="center" prop="specification" width="120" />
      <el-table-column label="单位" align="center" prop="unitName" width="70" />
      <el-table-column label="调拨数量" align="center" prop="quantityTransfer" width="100" />
      <el-table-column label="批次号" align="center" prop="batchCode" width="100" />
    </el-table>
  </div>
</template>

<script setup lang="ts" name="WmTransferLineDetail">
import { ref, watch } from 'vue'
import type { WmTransferLine, WmTransferLineQueryParams } from '@/types/api/mes/wm/transfer_line'
import { listWmTransferLine } from '@/api/mes/wm/transfer_line'

const props = defineProps<{ transferId: number }>()
const lineList = ref<WmTransferLine[]>([])
const loading = ref(true)

watch(() => props.transferId, (val) => {
  if (val) {
    loading.value = true
    listWmTransferLine({ pageNum: 1, pageSize: 1000, transferId: val } as WmTransferLineQueryParams).then(r => {
      lineList.value = r.rows; loading.value = false
    })
  }
}, { immediate: true })
</script>
