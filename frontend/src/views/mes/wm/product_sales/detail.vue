<template>
  <div>
    <el-table v-loading="loading" :data="lineList" border>
      <el-table-column label="物料编码" align="center" prop="itemCode" width="120" />
      <el-table-column label="物料名称" align="center" prop="itemName" width="150" />
      <el-table-column label="规格型号" align="center" prop="specification" width="120" />
      <el-table-column label="单位" align="center" prop="unitName" width="70" />
      <el-table-column label="出库数量" align="center" prop="quantitySales" width="100" />
      <el-table-column label="箱数" align="center" prop="quantityBox" width="70" />
      <el-table-column label="批次号" align="center" prop="batchCode" width="100" />
    </el-table>
  </div>
</template>

<script setup lang="ts" name="WmProductSalesLineDetail">
import { ref, watch } from 'vue'
import type { WmProductSalesLine, WmProductSalesLineQueryParams } from '@/types/api/mes/wm/product_sales_line'
import { listWmProductSalesLine } from '@/api/mes/wm/product_sales_line'

const props = defineProps<{ salesId: number }>()
const lineList = ref<WmProductSalesLine[]>([])
const loading = ref(true)

watch(() => props.salesId, (val) => {
  if (val) {
    loading.value = true
    listWmProductSalesLine({ pageNum: 1, pageSize: 1000, salesId: val } as WmProductSalesLineQueryParams).then(r => {
      lineList.value = r.rows; loading.value = false
    })
  }
}, { immediate: true })
</script>
