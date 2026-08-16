<template>
  <div>
    <el-row class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" size="small" @click="handleSelectItem">从物料库选择</el-button>
      </el-col>
    </el-row>
    <el-table :data="rows" border size="small">
      <el-table-column label="物料编码" prop="itemCode" width="130" align="center" />
      <el-table-column label="物料名称" prop="itemName" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="规格型号" prop="specification" width="130" align="center" :show-overflow-tooltip="true" />
      <el-table-column v-if="showProcess" label="工序" width="170">
        <template #default="scope">
          <el-select v-model="scope.row.processId" placeholder="工序级绑定(可选)" filterable clearable size="small" style="width: 100%" @change="onProcessChange(scope.row)">
            <el-option v-for="p in processOptions" :key="p.processId" :label="`${p.processCode} ${p.processName}`" :value="p.processId" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="抽检样本量" width="120" align="center">
        <template #default="scope">
          <el-input-number v-model="scope.row.quantityCheck" :min="0" :controls="false" size="small" style="width: 90px" />
        </template>
      </el-table-column>
      <el-table-column label="最大不合格数" width="120" align="center">
        <template #default="scope">
          <el-input-number v-model="scope.row.quantityUnqualified" :min="0" :controls="false" size="small" style="width: 90px" />
        </template>
      </el-table-column>
      <el-table-column label="致命缺陷率%" width="110" align="center">
        <template #default="scope">
          <el-input-number v-model="scope.row.crRate" :min="0" :precision="2" :controls="false" size="small" style="width: 80px" />
        </template>
      </el-table-column>
      <el-table-column label="严重缺陷率%" width="110" align="center">
        <template #default="scope">
          <el-input-number v-model="scope.row.majRate" :min="0" :precision="2" :controls="false" size="small" style="width: 80px" />
        </template>
      </el-table-column>
      <el-table-column label="轻微缺陷率%" width="110" align="center">
        <template #default="scope">
          <el-input-number v-model="scope.row.minRate" :min="0" :precision="2" :controls="false" size="small" style="width: 80px" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="70" align="center">
        <template #default="scope">
          <el-button link type="danger" icon="Delete" size="small" @click="rows.splice(scope.$index, 1)" />
        </template>
      </el-table-column>
    </el-table>

    <ItemSelect ref="itemSelectRef" @onSelected="onItemSelected" />
  </div>
</template>

<script setup lang="ts" name="TemplateProductRowsTab">
import { ref, onMounted, getCurrentInstance } from 'vue'
import type { QcTemplateProductRow } from '@/api/mes/qc/template'
import { listAllProcess } from '@/api/mes/pro/process'
import ItemSelect from '@/components/itemSelect/single.vue'

const { proxy } = getCurrentInstance() as any

const props = defineProps<{ rows: QcTemplateProductRow[]; showProcess: boolean }>()

const itemSelectRef = ref()
const processOptions = ref<any[]>([])

onMounted(() => {
  listAllProcess().then((r: any) => {
    processOptions.value = r.data || []
  })
})

function handleSelectItem() {
  itemSelectRef.value?.open()
}

/** 选物料 → 追加绑定行（同物料+工序维度前端先查重，后端仍兜底校验） */
function onItemSelected(row: any) {
  const dup = props.rows.some(r => r.itemId === row.itemId)
  if (dup) {
    proxy.$modal.msgWarning(`物料[${row.itemName}]已绑定，请勿重复添加`)
    return
  }
  props.rows.push({
    itemId: row.itemId, itemCode: row.itemCode, itemName: row.itemName,
    specification: row.specification, quantityCheck: 1, quantityUnqualified: 0,
    crRate: 0, majRate: 0, minRate: 0
  })
}

/** 选择工序后带出编码/名称（清空时同步清除） */
function onProcessChange(row: QcTemplateProductRow) {
  const p = processOptions.value.find(i => i.processId === row.processId)
  row.processCode = p?.processCode
  row.processName = p?.processName
}
</script>
