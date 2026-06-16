<template>
  <el-dialog
    title="班组选择"
    :model-value="showFlag"
    @update:model-value="(val: boolean) => emit('update:showFlag', val)"
    :modal="false"
    width="80%"
    center
    append-to-body
    @close="handleClose"
  >
    <el-form :model="queryParams" ref="queryFormRef" size="small" :inline="true" label-width="68px">
      <el-form-item label="班组编号" prop="teamCode">
        <el-input
          v-model="queryParams.teamCode"
          placeholder="请输入班组编号"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="班组名称" prop="teamName">
        <el-input
          v-model="queryParams.teamName"
          placeholder="请输入班组名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="teamList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="班组编号" align="center" prop="teamCode" />
      <el-table-column label="班组名称" align="center" prop="teamName" />
      <el-table-column label="备注" align="center" prop="remark" />
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:current-page="queryParams.pageNum"
      v-model:page-size="queryParams.pageSize"
      @pagination="getList"
    />

    <template #footer>
      <div class="dialog-footer">
        <el-button type="primary" @click="confirmSelect">确 定</el-button>
        <el-button @click="emit('update:showFlag', false)">取 消</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { listTeam } from '@/api/mes/cal/team'

const props = defineProps<{
  showFlag: boolean
}>()

const emit = defineEmits<{
  (e: 'update:showFlag', val: boolean): void
  (e: 'onSelected', rows: any[]): void
}>()

const loading = ref(false)
const total = ref(0)
const teamList = ref<any[]>([])
const selectedRows = ref<any[]>([])
const queryFormRef = ref()

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  teamCode: null as string | null,
  teamName: null as string | null,
})

function getList() {
  loading.value = true
  listTeam(queryParams).then((response: any) => {
    teamList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  queryFormRef.value?.resetFields()
  handleQuery()
}

function handleSelectionChange(selection: any[]) {
  selectedRows.value = selection
}

function confirmSelect() {
  if (!selectedRows.value || selectedRows.value.length === 0) {
    ElMessage.warning('请至少选择一条数据!')
    return
  }
  emit('onSelected', selectedRows.value)
  emit('update:showFlag', false)
}

function handleClose() {
  emit('update:showFlag', false)
}

// 每次打开时刷新列表
watch(() => props.showFlag, (val) => {
  if (val) {
    queryParams.pageNum = 1
    getList()
  }
}, { immediate: true })
</script>
