<template>
  <el-dialog
    title="人员选择"
    :model-value="showFlag"
    @update:model-value="(val: boolean) => emit('update:showFlag', val)"
    :modal="false"
    width="80%"
    center
    append-to-body
    @close="handleClose"
  >
    <el-row :gutter="20">
      <!-- 部门树 -->
      <el-col :span="4" :xs="24">
        <div class="head-container">
          <el-input
            v-model="deptName"
            placeholder="请输入部门名称"
            clearable
            size="small"
            prefix-icon="Search"
            style="margin-bottom: 20px"
          />
        </div>
        <div class="head-container">
          <el-tree
            :data="deptOptions"
            :props="defaultProps"
            :expand-on-click-node="false"
            :filter-node-method="filterNode"
            ref="treeRef"
            default-expand-all
            @node-click="handleNodeClick"
          />
        </div>
      </el-col>
      <!-- 用户表格 -->
      <el-col :span="20" :xs="24">
        <el-form :model="queryParams" ref="queryFormRef" size="small" :inline="true" label-width="68px">
          <el-form-item label="用户名称" prop="userName">
            <el-input
              v-model="queryParams.userName"
              placeholder="请输入用户名称"
              clearable
              style="width: 240px"
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="手机号码" prop="phonenumber">
            <el-input
              v-model="queryParams.phonenumber"
              placeholder="请输入手机号码"
              clearable
              style="width: 240px"
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="loading" :data="userList" @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="50" align="center" />
          <el-table-column label="用户名称" align="center" prop="userName" :show-overflow-tooltip="true" />
          <el-table-column label="用户昵称" align="center" prop="nickName" :show-overflow-tooltip="true" />
          <el-table-column label="部门" align="center" prop="dept.deptName" :show-overflow-tooltip="true" />
          <el-table-column label="手机号码" align="center" prop="phonenumber" width="120" />
        </el-table>

        <pagination
          v-show="total > 0"
          :total="total"
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          @pagination="getList"
        />
      </el-col>
    </el-row>

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
import { listUser } from '@/api/system/user'
import { listDept } from '@/api/system/dept'

const props = defineProps<{
  showFlag: boolean
}>()

const emit = defineEmits<{
  (e: 'update:showFlag', val: boolean): void
  (e: 'onSelected', rows: any[]): void
}>()

const loading = ref(false)
const total = ref(0)
const userList = ref<any[]>([])
const selectedRows = ref<any[]>([])
const queryFormRef = ref()
const treeRef = ref()
const deptName = ref('')
const deptOptions = ref<any[]>([])

const defaultProps = { children: 'children', label: 'deptName' }

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  userName: undefined as string | undefined,
  phonenumber: undefined as string | undefined,
  deptId: undefined as number | undefined,
})

function getList() {
  loading.value = true
  listUser(queryParams).then((response: any) => {
    userList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

function getDeptTree() {
  listDept().then((response: any) => {
    deptOptions.value = response.data
  })
}

function filterNode(value: string, data: any) {
  if (!value) return true
  return (data.deptName || '').indexOf(value) !== -1
}

function handleNodeClick(data: any) {
  queryParams.deptId = data.deptId
  handleQuery()
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

// 监听 deptName 变化过滤树
watch(deptName, (val) => {
  treeRef.value?.filter(val)
})

// 每次打开时初始化
watch(() => props.showFlag, (val) => {
  if (val) {
    getDeptTree()
    queryParams.pageNum = 1
    getList()
  }
}, { immediate: true })
</script>
