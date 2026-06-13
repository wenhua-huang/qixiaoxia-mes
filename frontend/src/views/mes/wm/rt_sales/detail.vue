<template>
  <div>
    <el-table v-loading="loading" :data="lineList" border>
      <el-table-column label="物料编码" align="center" prop="itemCode" width="120" />
      <el-table-column label="物料名称" align="center" prop="itemName" width="150" />
      <el-table-column label="规格型号" align="center" prop="specification" width="120" />
      <el-table-column label="单位" align="center" prop="unitName" width="70" />
      <el-table-column label="退货数量" align="center" prop="quantityRt" width="100" />
      <el-table-column label="批次号" align="center" prop="batchCode" width="100" />
    </el-table>
  </div>
</template>

<script setup lang="ts" name="WmRtSalesLineDetail">
import { ref, watch } from 'vue'
import type { WmRtSalesLine, WmRtSalesLineQueryParams } from '@/types/api/mes/wm/rt_sales_line'
import { listWmRtSalesLine } from '@/api/mes/wm/rt_sales_line'

const props = defineProps<{ rtId: number }>()
const lineList = ref<WmRtSalesLine[]>([])
const loading = ref(true)

watch(() => props.rtId, (val) => {
  if (val) {
    loading.value = true
    listWmRtSalesLine({ pageNum: 1, pageSize: 1000, rtId: val } as WmRtSalesLineQueryParams).then(r => {
      lineList.value = r.rows; loading.value = false
    })
  }
}, { immediate: true })
</script>
