<template>
  <div>
    <el-table v-loading="loading" :data="detailList" border>
      <el-table-column label="物料编码" align="center" prop="itemCode" width="130" />
      <el-table-column label="物料名称" align="center" prop="itemName" width="150" />
      <el-table-column label="规格型号" align="center" prop="specification" width="120" />
      <el-table-column label="单位" align="center" prop="unitName" width="70" />
      <el-table-column label="出库数量" align="center" prop="quantity" width="100" />
      <el-table-column label="批次号" align="center" prop="batchCode" width="120" />
      <el-table-column label="出库时间" align="center" prop="createTime" width="160" />
    </el-table>
  </div>
</template>

<script setup lang="ts" name="WmProductSalesDetailList">
import { ref, watch } from 'vue'
import type { WmProductSalesDetail, WmProductSalesDetailQueryParams } from '@/types/api/mes/wm/product_sales_detail'
import { listWmProductSalesDetail } from '@/api/mes/wm/product_sales_detail'

const props = defineProps<{ salesId: number }>()
const detailList = ref<WmProductSalesDetail[]>([])
const loading = ref(true)

watch(() => props.salesId, (val: number | undefined) => {
  if (val) {
    loading.value = true
    listWmProductSalesDetail({ pageNum: 1, pageSize: 1000, salesId: val } as WmProductSalesDetailQueryParams).then(r => {
      detailList.value = r.rows; loading.value = false
    })
  }
}, { immediate: true })
</script>
