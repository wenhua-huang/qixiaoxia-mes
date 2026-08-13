<template>
  <div style="display:flex;gap:6px;width:100%">
    <el-select :model-value="modelValue" :placeholder="placeholder" filterable clearable
               :loading="loading" style="flex:1" @change="onChange">
      <el-option v-for="w in options" :key="w.warehouseId" :label="labelOf(w)" :value="w.warehouseId" />
    </el-select>
    <el-button icon="Plus" v-hasPermi="['mes:wm:warehouse:add']"
               @click="createRef?.open('CUSTOMER')">新建客户仓</el-button>
    <CreateWarehouseDialog ref="createRef" @onCreated="onCreated" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { listAllWmWarehouse } from '@/api/mes/wm/warehouse'
import CreateWarehouseDialog from '@/components/warehouseSelect/CreateWarehouseDialog.vue'
import type { WmWarehouse } from '@/types'

const props = withDefaults(defineProps<{
  modelValue?: number
  clientId?: number
  placeholder?: string
}>(), { placeholder: '可选（批量改仓）' })

const emit = defineEmits<{
  'update:modelValue': [v: number | undefined]
  select: [w: WmWarehouse | undefined]
}>()

const all = ref<WmWarehouse[]>([])
const loading = ref(false)
const createRef = ref()

/** 候选：有客户→该客户专属仓置顶 + 非客户仓(公共/其他类型)；无客户→全部。后端 Task7 做客户隔离硬校验 */
const options = computed<WmWarehouse[]>(() => {
  const list = all.value, cid = props.clientId
  const merged = cid == null
    ? [...list]
    : [
        ...list.filter(w => w.warehouseType === 'CUSTOMER' && w.clientId === cid),
        ...list.filter(w => w.warehouseType !== 'CUSTOMER')
      ]
  // 切换客户后已选仓不在候选内仍保留可见（不丢标签；后端最终校验合法性）
  if (props.modelValue && !merged.some(w => w.warehouseId === props.modelValue)) {
    const cur = list.find(w => w.warehouseId === props.modelValue)
    if (cur) merged.unshift(cur)
  }
  return merged
})

function labelOf(w: WmWarehouse): string {
  const n = w.warehouseName || w.warehouseCode || String(w.warehouseId)
  return w.warehouseType === 'CUSTOMER' ? `${n}（客户仓${w.clientName ? '·' + w.clientName : ''}）` : n
}

function find(id?: number): WmWarehouse | undefined {
  return id == null ? undefined : all.value.find(w => w.warehouseId === id)
}

function onChange(v: number | undefined) {
  emit('update:modelValue', v)
  emit('select', find(v))
}

function onCreated(w: WmWarehouse) {
  if (w.warehouseId && !all.value.some(x => x.warehouseId === w.warehouseId)) all.value.push(w)
  if (w.warehouseId) { emit('update:modelValue', w.warehouseId); emit('select', w) }
  load()
}

function load() {
  loading.value = true
  listAllWmWarehouse().then(r => { all.value = (r.data || []) as WmWarehouse[] })
    .catch(() => { all.value = [] }).finally(() => { loading.value = false })
}

onMounted(load)
</script>
