<template>
  <el-dialog v-model="visible" title="采购单快捷创建" width="900px" append-to-body @open="load">
    <el-table :data="lines" size="small" border ref="tableRef" @selection-change="handleSelection">
      <el-table-column type="selection" width="45" />
      <el-table-column label="物料编码" prop="itemCode" width="120" align="center" />
      <el-table-column label="物料名称" prop="itemName" :show-overflow-tooltip="true" />
      <el-table-column label="缺口" width="90" align="center">
        <template #default="s">{{ s.row.shortageQty }} {{ s.row.unitName }}</template>
      </el-table-column>
      <el-table-column label="采购数量" width="130" align="center">
        <template #default="s">
          <el-input-number v-model="s.row.recommendedQty" :min="0" size="small" controls-position="right" style="width:110px" />
        </template>
      </el-table-column>
      <el-table-column label="推荐供应商" min-width="140">
        <template #default="s">
          <span v-if="s.row.hasVendor" style="color:#67C23A">{{ s.row.vendorName }}</span>
          <span v-else style="color:#F56C6C">待定</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110" align="center">
        <template #default="s">
          <el-tag v-if="s.row.hasExistingPO" type="success" size="small">已生成 {{ s.row.existingPOCode }}</el-tag>
          <el-tag v-else type="info" size="small">未生成</el-tag>
        </template>
      </el-table-column>
    </el-table>

    <el-divider />
    <div v-if="checked.length === 0" style="color:#909399;text-align:center;padding:12px">请勾选需要采购的物料</div>
    <div v-else>
      <el-alert :title="'将生成 ' + Object.keys(groupedChecked).length + ' 张采购单（' + checked.length + ' 种物料）'" type="info" :closable="false" show-icon style="margin-bottom:10px" />
      <div v-for="(g, vid) in groupedChecked" :key="vid" style="margin-bottom:4px;font-size:13px">
        PO：<b>{{ g[0].vendorName || '待定供应商' }}</b>
        — <span v-for="(l,i) in g" :key="i">{{ l.itemName }} {{ l.recommendedQty }}{{ l.unitName }}<span v-if="i < g.length-1">、</span></span>
      </div>
    </div>

    <template #footer>
      <el-button @click="visible = false">取 消</el-button>
      <el-button type="primary" :disabled="checked.length===0" :loading="submitting" @click="submit">
        确认生成（{{ checked.length }} 项）
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { loadPurOrderWizard, submitPurOrderWizard } from '@/api/mes/pro/kit'
import { ElMessage } from 'element-plus'

const props = defineProps<{ modelValue: boolean; workorderId: number | null }>()
const emit = defineEmits<{ (e: 'update:modelValue', v: boolean): void; (e: 'refresh'): void }>()

const visible = ref(false)
const lines = ref<any[]>([])
const tableRef = ref()
const checked = ref<any[]>([])
const submitting = ref(false)

watch(() => props.modelValue, (v) => { visible.value = v })
watch(visible, (v) => { emit('update:modelValue', v) })

function load() {
  if (!props.workorderId) return
  loadPurOrderWizard(props.workorderId).then((r: any) => {
    lines.value = r.data?.lines || []
    // 默认勾选有供应商、未生成PO、有推荐数量的行
    nextTick(() => {
      lines.value.forEach((row) => {
        if (row.checked && row.recommendedQty > 0 && !row.hasExistingPO) tableRef.value?.toggleRowSelection(row, true)
      })
    })
  }).catch(() => {})
}

function handleSelection(rows: any[]) { checked.value = rows }

const groupedChecked = computed(() => {
  const g: Record<number, any[]> = {}
  for (const l of checked.value) { const vid = l.vendorId || 0; (g[vid] ||= []).push(l) }
  return g
})

function submit() {
  if (!props.workorderId || checked.value.length === 0) return
  // 构建提交行：从 checked 中提取字段
  const payload = checked.value.map(l => ({
    itemId: l.itemId, itemCode: l.itemCode, itemName: l.itemName,
    unitOfMeasure: l.unitOfMeasure || l.unitName, unitName: l.unitName,
    vendorId: l.vendorId, vendorCode: l.vendorCode, vendorName: l.vendorName,
    quantity: l.recommendedQty
  }))
  submitting.value = true
  submitPurOrderWizard(props.workorderId, payload).then((r: any) => {
    ElMessage.success(r.data?.message || '采购单已生成')
    visible.value = false
    emit('refresh')
  }).catch(() => {}).finally(() => { submitting.value = false })
}
</script>
