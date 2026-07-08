<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="物料编码" prop="itemCode">
        <el-input v-model="queryParams.itemCode" placeholder="请输入物料编码" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="物料名称" prop="itemName">
        <el-input v-model="queryParams.itemName" placeholder="请输入物料名称" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="批次号" prop="batchCode">
        <el-input v-model="queryParams.batchCode" placeholder="请输入批次号" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="仓库" prop="warehouseId">
        <el-input v-model="queryParams.warehouseId" placeholder="仓库ID" clearable style="width:100px" />
      </el-form-item>
      <el-form-item label="质量状态" prop="qualityStatus">
        <el-select v-model="queryParams.qualityStatus" placeholder="请选择" clearable style="width:110px">
          <el-option label="正常" value="NORMAL" />
          <el-option label="冻结" value="HOLD" />
          <el-option label="不合格" value="REJECT" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" size="small" @click="handleExport" v-hasPermi="['mes:wm:stock:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="stockList" @selection-change="handleSelectionChange" stripe>
      <el-table-column label="物料编码" align="center" prop="itemCode" width="130" />
      <el-table-column label="物料名称" align="center" prop="itemName" :show-overflow-tooltip="true" width="150" />
      <el-table-column label="规格型号" align="center" prop="specification" :show-overflow-tooltip="true" width="120" />
      <el-table-column label="库存数量" align="center" prop="quantityOnhand" width="100" sortable />
	      <el-table-column label="可用库存" align="center" prop="quantityAvailable" width="100" sortable />
      <el-table-column label="占用库存" align="center" width="100" sortable :sort-method="(a,b) => occupied(a) - occupied(b)">
        <template #default="scope">
          <span :class="{ 'occu-pending': occupied(scope.row) > 0 }">{{ occupied(scope.row) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="单位" align="center" prop="unitName" width="70" />
      <el-table-column label="批次号" align="center" prop="batchCode" width="130" :show-overflow-tooltip="true">
        <template #default="scope">
          <el-button v-if="scope.row.batchCode" link type="primary" size="small" @click="handleBatchClick(scope.row)">
            {{ scope.row.batchCode }}
          </el-button>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="仓库" align="center" prop="warehouseName" width="120" />
      <el-table-column label="库区" align="center" prop="locationId" width="80" />
      <el-table-column label="库位" align="center" prop="areaId" width="80" />
      <el-table-column label="供应商" align="center" prop="vendorId" width="80" />
      <el-table-column label="质量状态" align="center" prop="qualityStatus" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.qualityStatus === 'NORMAL' ? 'success' : scope.row.qualityStatus === 'HOLD' ? 'warning' : 'danger'" size="small">
            {{ scope.row.qualityStatus === 'NORMAL' ? '正常' : scope.row.qualityStatus === 'HOLD' ? '冻结' : '不合格' }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup lang="ts" name="WmMaterialStock">
import { ref, reactive, toRefs, getCurrentInstance } from 'vue'
import type { WmMaterialStockQueryParams, WmMaterialStock } from '@/types/api/mes/wm/material_stock'
import { listWmMaterialStock } from '@/api/mes/wm/material_stock'
import { useRouter } from 'vue-router'

const { proxy } = getCurrentInstance() as any
const router = useRouter()

const stockList = ref<WmMaterialStock[]>([])
const loading = ref(true)
const showSearch = ref(true)
const ids = ref<number[]>([])
const total = ref(0)

const data = reactive({
  queryParams: { pageNum: 1, pageSize: 10 } as WmMaterialStockQueryParams
})
const { queryParams } = toRefs(data)

function getList() {
  loading.value = true
  listWmMaterialStock(queryParams.value).then(r => { stockList.value = r.rows; total.value = r.total; loading.value = false })
}
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleSelectionChange(s: any[]) { ids.value = s.map(i => i.materialStockId) }
function handleExport() { proxy.download('/mes/wm/material_stock/export', { ...queryParams.value }, `stock_${Date.now()}.xlsx`) }
function handleBatchClick(row: WmMaterialStock) {
  if (row.batchId && row.batchId > 0) {
    router.push({ path: '/mes/wm/batch', query: { batchId: row.batchId } })
  }
}

/** 占用库存 = 现有量 - 可用量（已预占未出库的部分） */
function occupied(row: WmMaterialStock): number {
  const onhand = Number(row.quantityOnhand) || 0
  const avail = Number(row.quantityAvailable) || 0
  return Math.max(0, onhand - avail)
}

getList()
</script>

<style lang="scss" scoped>
.occu-pending {
  color: #e6a23c;
  font-weight: 600;
}
</style>
