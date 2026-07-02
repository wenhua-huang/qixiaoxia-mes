<template>
  <div>
    <el-button type="primary" size="small" icon="Plus" @click="handleAdd" style="margin-bottom:10px">添加物料</el-button>
    <el-table :data="list" size="small" border>
      <el-table-column label="物料编码" prop="itemCode" width="130" align="center" />
      <el-table-column label="物料名称" prop="itemName" :show-overflow-tooltip="true" />
      <el-table-column label="首选" width="70" align="center">
        <template #default="s">
          <el-switch v-model="s.row.isPreferred" active-value="Y" inactive-value="N" size="small" @change="handlePreferredChange(s.row)" />
        </template>
      </el-table-column>
      <el-table-column label="最小起订量" width="110" align="center">
        <template #default="s">
          <el-input-number v-model="s.row.minOrderQty" :min="0" size="small" controls-position="right" @change="handleUpdate(s.row)" />
        </template>
      </el-table-column>
      <el-table-column label="提前期(天)" width="100" align="center">
        <template #default="s">
          <el-input-number v-model="s.row.leadTimeDays" :min="0" size="small" controls-position="right" @change="handleUpdate(s.row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="60" align="center">
        <template #default="s"><el-button link type="danger" icon="Delete" size="small" @click="handleDelete(s.row)" /></template>
      </el-table-column>
    </el-table>

    <!-- 添加物料弹窗 -->
    <el-dialog v-model="addOpen" title="添加供应物料" width="500px" append-to-body>
      <el-form label-width="100px">
        <el-form-item label="选择物料">
          <el-select v-model="addForm.itemId" filterable placeholder="搜索物料" style="width:100%">
            <el-option v-for="it in itemOptions" :key="it.itemId" :label="it.itemCode + ' ' + it.itemName" :value="it.itemId" />
          </el-select>
        </el-form-item>
        <el-form-item label="最小起订量"><el-input-number v-model="addForm.minOrderQty" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="提前期(天)"><el-input-number v-model="addForm.leadTimeDays" :min="0" style="width:100%" /></el-form-item>
      </el-form>
      <template #footer><el-button type="primary" @click="submitAdd">确 定</el-button><el-button @click="addOpen=false">取 消</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { listByVendorId, addItemVendor, updateItemVendor, delItemVendor } from '@/api/mes/md/itemvendor'
import { listItem } from '@/api/mes/md/item'
import { ElMessage, ElMessageBox } from 'element-plus'

const props = defineProps<{ vendorId: number }>()

const list = ref<any[]>([])
const addOpen = ref(false)
const itemOptions = ref<any[]>([])
const addForm = ref({ itemId: null as number | null, minOrderQty: 0, leadTimeDays: 0 })

function load() {
  if (!props.vendorId) return
  listByVendorId(props.vendorId).then((r: any) => { list.value = r.data || [] }).catch(() => {})
}

watch(() => props.vendorId, load)
onMounted(load)

function handleAdd() {
  addForm.value = { itemId: null, minOrderQty: 0, leadTimeDays: 0 }
  listItem({ pageSize: 500 }).then((r: any) => { itemOptions.value = r.rows || [] }).catch(() => {})
  addOpen.value = true
}

function submitAdd() {
  if (!addForm.value.itemId) { ElMessage.warning('请选择物料'); return }
  const it = itemOptions.value.find(i => i.itemId === addForm.value.itemId)
  addItemVendor({
    vendorId: props.vendorId, itemId: addForm.value.itemId,
    itemCode: it?.itemCode, itemName: it?.itemName,
    isPreferred: 'N', minOrderQty: addForm.value.minOrderQty, leadTimeDays: addForm.value.leadTimeDays
  }).then(() => { addOpen.value = false; load(); ElMessage.success('添加成功') }).catch(() => {})
}

function handleUpdate(row: any) {
  updateItemVendor(row).then(() => ElMessage.success('已更新')).catch(() => {})
}

function handlePreferredChange(row: any) {
  updateItemVendor({ recordId: row.recordId, isPreferred: row.isPreferred }).then(() => ElMessage.success('已更新')).catch(() => {})
}

function handleDelete(row: any) {
  ElMessageBox.confirm('确认删除该物料？').then(() => {
    delItemVendor(row.recordId).then(() => { load(); ElMessage.success('已删除') })
  }).catch(() => {})
}
</script>
