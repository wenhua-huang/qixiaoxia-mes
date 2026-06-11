<template>
  <div>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" size="small" @click="handleAdd" v-hasPermi="['mes:md:item:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" size="small" :disabled="multiple" @click="handleDelete" v-hasPermi="['mes:md:item:remove']">删除</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="bomList" @selection-change="handleSelectionChange" size="small">
      <el-table-column type="selection" width="45" align="center" />
      <el-table-column label="子物料编码" align="center" prop="bomItemCode" />
      <el-table-column label="子物料名称" align="center" prop="bomItemName" :show-overflow-tooltip="true" />
      <el-table-column label="用量单位" width="80" align="center" prop="unitOfMeasure" />
      <el-table-column label="用量" width="80" align="center" prop="quantity" />
      <el-table-column label="损耗率" width="80" align="center" prop="scrapRate">
        <template #default="s">{{ s.row.scrapRate ? (s.row.scrapRate * 100).toFixed(1) + '%' : '0%' }}</template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" width="110" class-name="small-padding fixed-width">
        <template #default="s">
          <el-button link type="primary" size="small" @click="handleUpdate(s.row)">修改</el-button>
          <el-button link type="primary" size="small" @click="handleDelete(s.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- BOM新增/修改子对话框 -->
    <el-dialog :title="bomTitle" v-model="bomOpen" width="500px" append-to-body>
      <el-form ref="bomFormRef" :model="bomForm" :rules="bomRules" label-width="90px">
        <el-form-item label="子物料" prop="bomItemId">
          <el-select v-model="bomForm.bomItemId" filterable placeholder="请选择子物料" style="width:100%">
            <el-option v-for="i in itemOptions" :key="i.itemId" :label="i.itemName + ' (' + i.itemCode + ')'" :value="i.itemId" />
          </el-select>
        </el-form-item>
        <el-row>
          <el-col :span="12">
            <el-form-item label="用量" prop="quantity"><el-input-number v-model="bomForm.quantity" :precision="4" :min="0" style="width:100%" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单位" prop="unitOfMeasure"><el-input v-model="bomForm.unitOfMeasure" placeholder="如PCS" /></el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="损耗率"><el-input-number v-model="bomForm.scrapRate" :precision="4" :step="0.01" :min="0" :max="1" style="width:160px" /><span style="margin-left:8px;color:#999;font-size:12px">0~1，0.05=5%</span></el-form-item>
        <el-form-item label="备注"><el-input v-model="bomForm.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer><el-button type="primary" @click="submitBomForm">确 定</el-button><el-button @click="bomOpen = false">取 消</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, toRefs } from 'vue'
import { getCurrentInstance } from 'vue'
import type { MdProductBom } from '@/types/api/mes/md/bom'
import { listBom, addBom, updateBom, delBom } from '@/api/mes/md/bom'
import { listItem } from '@/api/mes/md/item'

const props = defineProps<{ itemId: number }>()
const { proxy } = getCurrentInstance() as any

const loading = ref(false)
const bomList = ref<any[]>([])
const bomOpen = ref(false)
const bomTitle = ref('')
const multiple = ref(true)
const itemOptions = ref<any[]>([])
const ids = ref<number[]>([])

const data = reactive({
  bomForm: { itemId: props.itemId, quantity: 1, scrapRate: 0, enableFlag: '1' } as MdProductBom,
  bomRules: {
    bomItemId: [{ required: true, message: '请选择子物料', trigger: 'blur' }],
    quantity: [{ required: true, message: '用量不能为空', trigger: 'blur' }]
  }
})
const { bomForm, bomRules } = toRefs(data)

async function load() {
  loading.value = true
  try {
    const r = await listBom({ pageSize: 999, itemId: props.itemId })
    bomList.value = r.rows || []
  } catch (e) { /* ignore */ }
  loading.value = false
}

async function loadItems() {
  try {
    const r = await listItem({ pageSize: 999 })
    itemOptions.value = r.rows || []
  } catch (e) { /* ignore */ }
}

function handleSelectionChange(s: any[]) { ids.value = s.map(i => i.bomId); multiple.value = !s.length }

function resetBomForm() {
  bomForm.value = { itemId: props.itemId, quantity: 1, scrapRate: 0, enableFlag: '1' } as MdProductBom
  proxy?.resetForm('bomFormRef')
}

function handleAdd() { loadItems(); resetBomForm(); bomOpen.value = true; bomTitle.value = '添加BOM物料' }
function handleUpdate(row: any) { loadItems(); bomForm.value = { ...row }; bomOpen.value = true; bomTitle.value = '修改BOM物料' }

async function submitBomForm() {
  proxy.$refs['bomFormRef'].validate(async (valid: boolean) => {
    if (!valid) return
    try {
      if (bomForm.value.bomId) {
        await updateBom(bomForm.value)
      } else {
        await addBom(bomForm.value)
      }
      proxy.$modal.msgSuccess('操作成功')
      bomOpen.value = false
      load()
    } catch (e) { /* show error */ }
  })
}

async function handleDelete(row?: any) {
  const bomIds = row?.bomId || ids.value
  try {
    proxy.$modal.confirm('确认删除？').then(async () => {
      await delBom(bomIds)
      proxy.$modal.msgSuccess('删除成功')
      load()
    })
  } catch (e) { /* ignore */ }
}

defineExpose({ load })
</script>
