<template>
  <div style="display:flex;gap:6px;width:100%">
    <el-select :model-value="modelValue" :placeholder="placeholder" filterable clearable
               :loading="loading" style="flex:1" @change="onChange">
      <el-option v-for="w in options" :key="w.warehouseId" :label="labelOf(w)" :value="w.warehouseId" />
    </el-select>
    <el-tooltip :disabled="!!clientId" content="请先选择客户" placement="top">
      <span>
        <el-button icon="Plus" :disabled="!clientId" v-hasPermi="['mes:wm:warehouse:add']"
                   @click="openCreate">新建客户仓</el-button>
      </span>
    </el-tooltip>
    <CreateWarehouseDialog ref="createRef" @onCreated="onCreated" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { listAllWmWarehouse } from '@/api/mes/wm/warehouse'
import CreateWarehouseDialog from '@/components/warehouseSelect/CreateWarehouseDialog.vue'
import type { WmWarehouse } from '@/types'

const props = withDefaults(defineProps<{
  modelValue?: number
  clientId?: number
  clientName?: string
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

/** 切换客户：若已选仓是其他客户的专属仓，自动清空（避免保存能过、过账才被硬隔离拦截） */
watch(() => props.clientId, (newCid) => {
  const cur = props.modelValue == null ? undefined : all.value.find(w => w.warehouseId === props.modelValue)
  if (cur && cur.warehouseType === 'CUSTOMER' && cur.clientId !== newCid) {
    emit('update:modelValue', undefined)
    emit('select', undefined)
  }
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

/** 新建客户仓时把当前单据客户带入，避免重复选择 */
function openCreate() {
  createRef.value?.open('CUSTOMER', { clientId: props.clientId, clientName: props.clientName })
}

function load() {
  loading.value = true
  listAllWmWarehouse().then(r => { all.value = (r.data || []) as WmWarehouse[] })
    .catch(() => { all.value = [] }).finally(() => { loading.value = false })
}

onMounted(load)
</script>
