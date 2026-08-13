<template>
  <el-dialog title="新建专属仓" v-model="showFlag" width="560px" center append-to-body :close-on-click-modal="false">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
      <el-form-item label="仓库编码" prop="warehouseCode">
        <el-input v-model="form.warehouseCode" placeholder="留空则后端自动生成" maxlength="32" />
      </el-form-item>
      <el-form-item label="仓库名称" prop="warehouseName">
        <el-input v-model="form.warehouseName" placeholder="请输入仓库名称" maxlength="50" />
      </el-form-item>
      <el-form-item label="仓库归属" prop="ownershipType">
        <el-radio-group v-model="form.ownershipType" @change="onTypeChange">
          <el-radio value="CUSTOMER">客户仓</el-radio>
          <el-radio value="SUPPLIER">供应商仓</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="form.ownershipType === 'CUSTOMER'" label="归属客户" prop="clientId">
        <el-input :model-value="ownerDisplay" readonly placeholder="请选择归属客户">
          <template #append><el-button icon="Search" @click="clientSelectRef?.open(form.clientId)" /></template>
        </el-input>
      </el-form-item>
      <el-form-item v-else-if="form.ownershipType === 'SUPPLIER'" label="归属供应商" prop="vendorId">
        <el-input :model-value="ownerDisplay" readonly placeholder="请选择归属供应商">
          <template #append><el-button icon="Search" @click="vendorSelectRef?.open(form.vendorId)" /></template>
        </el-input>
      </el-form-item>
      <el-form-item label="地址" prop="address">
        <el-input v-model="form.address" type="textarea" :rows="2" placeholder="请输入仓库地址" maxlength="200" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button type="primary" :loading="submitting" @click="submit">确 定</el-button>
      <el-button @click="showFlag = false">取 消</el-button>
    </template>
    <ClientSelect ref="clientSelectRef" @onSelected="onClientSelected" />
    <VendorSelect ref="vendorSelectRef" @onSelected="onVendorSelected" />
  </el-dialog>
</template>

<script setup lang="ts" name="CreateWarehouseDialog">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { addWmWarehouse } from '@/api/mes/wm/warehouse'
import { genSerialCode } from '@/api/mes/sys/autocoderule'
import type { WmWarehouse } from '@/types/api/mes/wm/warehouse'
import type { MdClient } from '@/types/api/mes/md/client'
import type { MdVendor } from '@/types/api/mes/md/vendor'
import ClientSelect from '@/components/clientSelect/single.vue'
import VendorSelect from '@/components/vendorSelect/single.vue'

const emit = defineEmits<{ onCreated: [warehouse: WmWarehouse] }>()

const showFlag = ref(false)
const submitting = ref(false)
const formRef = ref()
const clientSelectRef = ref()
const vendorSelectRef = ref()

/** 归属类型（客户仓/供应商仓） */
type OwnerType = 'CUSTOMER' | 'SUPPLIER'

/** 归属类型 → 内容类型默认值（客户仓放成品，供应商仓放原料） */
const DEFAULT_CONTENT_TYPE: Record<OwnerType, string> = {
  CUSTOMER: 'FINISHED',
  SUPPLIER: 'RAW'
}

function getDefaultForm(): WmWarehouse {
  return { ownershipType: 'CUSTOMER', enableFlag: '1' } as WmWarehouse
}

const form = ref<WmWarehouse>(getDefaultForm())

/** 归属展示值：无 name 时回退到 #id，避免选中后空白 */
const ownerDisplay = computed(() => {
  if (form.value.ownershipType === 'CUSTOMER') {
    return form.value.clientName || (form.value.clientId ? `#${form.value.clientId}` : '')
  }
  return form.value.vendorName || (form.value.vendorId ? `#${form.value.vendorId}` : '')
})

/** 校验规则：归属按类型动态必填 */
const rules = computed(() => ({
  warehouseName: [{ required: true, message: '仓库名称不能为空', trigger: 'blur' }],
  ownershipType: [{ required: true, message: '请选择仓库归属', trigger: 'change' }],
  address: [{ required: true, message: '地址不能为空', trigger: 'blur' }],
  clientId: form.value.ownershipType === 'CUSTOMER'
    ? [{ required: true, message: '请选择归属客户', trigger: 'change' }]
    : [],
  vendorId: form.value.ownershipType === 'SUPPLIER'
    ? [{ required: true, message: '请选择归属供应商', trigger: 'change' }]
    : []
})) as any

/** 切换类型时清空归属，避免脏数据（与 Task 9 互斥规则一致） */
function onTypeChange() {
  form.value.clientId = undefined
  form.value.clientName = undefined
  form.value.vendorId = undefined
  form.value.vendorName = undefined
  formRef.value?.clearValidate(['clientId', 'vendorId'])
}

function onClientSelected(row: MdClient) {
  form.value.clientId = row.clientId
  form.value.clientName = row.clientName
  form.value.vendorId = undefined
  form.value.vendorName = undefined
  formRef.value?.validateField('clientId')
}

function onVendorSelected(row: MdVendor) {
  form.value.vendorId = row.vendorId
  form.value.vendorName = row.vendorName
  form.value.clientId = undefined
  form.value.clientName = undefined
  formRef.value?.validateField('vendorId')
}

/**
 * 打开新建弹窗。
 * @param type 预设仓库类型（可选），不传默认客户仓。
 * @param preset 预设归属（可选），从单据带入客户/供应商，避免重复选择。
 */
function open(type?: OwnerType, preset?: { clientId?: number; clientName?: string; vendorId?: number; vendorName?: string }) {
  form.value = getDefaultForm()
  if (type) form.value.ownershipType = type
  if (preset) {
    form.value.clientId = preset.clientId
    form.value.clientName = preset.clientName
    form.value.vendorId = preset.vendorId
    form.value.vendorName = preset.vendorName
  }
  showFlag.value = true
  formRef.value?.clearValidate()
}

function submit() {
  formRef.value?.validate((valid: boolean) => {
    if (!valid) return
    submitting.value = true
    const doSubmit = (code?: string) => {
      const payload: WmWarehouse = { ...form.value }
      if (code) payload.warehouseCode = code
      // 自动填内容类型：客户仓→成品仓，供应商仓→原料仓
      if (payload.ownershipType) {
        payload.warehouseType = DEFAULT_CONTENT_TYPE[payload.ownershipType as OwnerType]
      }
      // 归属互斥清空，与后端 normalizeOwnership 一致
      if (payload.ownershipType === 'CUSTOMER') {
        payload.vendorId = undefined
        payload.vendorName = undefined
      } else if (payload.ownershipType === 'SUPPLIER') {
        payload.clientId = undefined
        payload.clientName = undefined
      }
      addWmWarehouse(payload)
        .then(r => {
          // 后端 add 返回 useGeneratedKeys 回填 warehouseId 的实体，直接引用，避免名称回查静默选错
          const created = r.data as WmWarehouse
          ElMessage.success('新建成功')
          emit('onCreated', created)
          showFlag.value = false
        })
        .finally(() => { submitting.value = false })
    }
    // 编码留空时自动生成（warehouse_code NOT NULL，与仓库主页一致用 WAREHOUSE_CODE 规则）
    if (!form.value.warehouseCode) {
      genSerialCode('WAREHOUSE_CODE').then((r: any) => doSubmit(r.data)).finally(() => {})
    } else {
      doSubmit()
    }
  })
}

defineExpose({ open })
</script>
