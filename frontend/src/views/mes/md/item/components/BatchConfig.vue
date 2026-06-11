<template>
  <div>
    <el-form :model="form" label-width="140px">
      <el-row>
        <el-col :span="8"><el-form-item label="生产日期"><el-switch v-model="form.produceDateFlag" active-value="1" inactive-value="0" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="有效期"><el-switch v-model="form.expireDateFlag" active-value="1" inactive-value="0" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="入库日期"><el-switch v-model="form.recptDateFlag" active-value="1" inactive-value="0" /></el-form-item></el-col>
      </el-row>
      <el-row>
        <el-col :span="8"><el-form-item label="供应商"><el-switch v-model="form.vendorFlag" active-value="1" inactive-value="0" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="客户"><el-switch v-model="form.clientFlag" active-value="1" inactive-value="0" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="销售订单号"><el-switch v-model="form.coCodeFlag" active-value="1" inactive-value="0" /></el-form-item></el-col>
      </el-row>
      <el-row>
        <el-col :span="8"><el-form-item label="采购订单号"><el-switch v-model="form.poCodeFlag" active-value="1" inactive-value="0" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="生产工单"><el-switch v-model="form.workorderFlag" active-value="1" inactive-value="0" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="生产任务"><el-switch v-model="form.taskFlag" active-value="1" inactive-value="0" /></el-form-item></el-col>
      </el-row>
      <el-row>
        <el-col :span="8"><el-form-item label="工作站"><el-switch v-model="form.workstationFlag" active-value="1" inactive-value="0" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="工具"><el-switch v-model="form.toolFlag" active-value="1" inactive-value="0" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="模具"><el-switch v-model="form.moldFlag" active-value="1" inactive-value="0" /></el-form-item></el-col>
      </el-row>
      <el-row>
        <el-col :span="8"><el-form-item label="生产批号"><el-switch v-model="form.lotNumberFlag" active-value="1" inactive-value="0" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="质量状态"><el-switch v-model="form.qualityStatusFlag" active-value="1" inactive-value="0" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="纸卷号"><el-switch v-model="form.paperRollFlag" active-value="1" inactive-value="0" /></el-form-item></el-col>
      </el-row>
      <el-row>
        <el-col :span="8"><el-form-item label="门幅"><el-switch v-model="form.paperWidthFlag" active-value="1" inactive-value="0" /></el-form-item></el-col>
      </el-row>
      <div style="text-align: center; margin-top: 20px">
        <el-button type="primary" @click="handleSave">保存批次配置</el-button>
      </div>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, toRefs } from 'vue'
import { getCurrentInstance } from 'vue'
import type { MdItemBatchConfig } from '@/types/api/mes/md/batchconfig'
import { getBatchConfigByItemId, addBatchConfig, updateBatchConfig } from '@/api/mes/md/batchconfig'

const props = defineProps<{ itemId: number }>()
const { proxy } = getCurrentInstance() as any

const data = reactive({
  form: {
    itemId: props.itemId,
    produceDateFlag: '0', expireDateFlag: '0', recptDateFlag: '0',
    vendorFlag: '0', clientFlag: '0', coCodeFlag: '0',
    poCodeFlag: '0', workorderFlag: '0', taskFlag: '0',
    workstationFlag: '0', toolFlag: '0', moldFlag: '0',
    lotNumberFlag: '0', qualityStatusFlag: '0', paperRollFlag: '0',
    paperWidthFlag: '0', enableFlag: '1'
  } as MdItemBatchConfig
})

const { form } = toRefs(data)

async function load(itemId: number) {
  try {
    const r = await getBatchConfigByItemId(itemId)
    if (r.data) {
      form.value = r.data
    } else {
      form.value = {
        configId: undefined, itemId,
        produceDateFlag: '0', expireDateFlag: '0', recptDateFlag: '0',
        vendorFlag: '0', clientFlag: '0', coCodeFlag: '0',
        poCodeFlag: '0', workorderFlag: '0', taskFlag: '0',
        workstationFlag: '0', toolFlag: '0', moldFlag: '0',
        lotNumberFlag: '0', qualityStatusFlag: '0', paperRollFlag: '0',
        paperWidthFlag: '0', enableFlag: '1'
      } as MdItemBatchConfig
    }
  } catch (e) { /* ignore */ }
}

async function handleSave() {
  if (form.value.configId) {
    await updateBatchConfig(form.value)
  } else {
    await addBatchConfig(form.value)
  }
  proxy.$modal.msgSuccess('批次配置保存成功')
}

defineExpose({ load })
</script>
