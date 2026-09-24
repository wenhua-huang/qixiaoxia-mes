<template>
  <div class="app-container">
    <!-- 搜索区域 -->
    <el-form :model="queryParams" ref="queryRef" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="用户" prop="userKeyword">
        <el-input v-model="queryParams.userKeyword" placeholder="用户名/昵称" clearable style="width:180px"
          @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="工位" prop="workstationKeyword">
        <el-input v-model="queryParams.workstationKeyword" placeholder="编码/名称" clearable style="width:180px"
          @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="enableFlag">
        <el-select v-model="queryParams.enableFlag" placeholder="全部" clearable style="width:110px">
          <el-option label="启用" value="1" />
          <el-option label="停用" value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 工具栏 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" size="small" @click="handleAdd"
          v-hasPermi="['mes:pro:userworkstation:add']">新增绑定</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" size="small" @click="handleExport"
          v-hasPermi="['mes:pro:userworkstation:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="dataList" size="small">
      <el-table-column label="用户名" align="center" prop="userName" width="130" :show-overflow-tooltip="true" />
      <el-table-column label="昵称" align="center" prop="nickName" width="140" :show-overflow-tooltip="true" />
      <el-table-column label="工位编码" align="center" prop="workstationCode" width="140" :show-overflow-tooltip="true" />
      <el-table-column label="工位名称" align="center" prop="workstationName" min-width="150" :show-overflow-tooltip="true" />
      <el-table-column label="绑定时间" align="center" width="170">
        <template #default="scope">
          <span>{{ parseTime(scope.row.operationTime || scope.row.createTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" min-width="120" :show-overflow-tooltip="true" />
      <el-table-column label="启用" align="center" width="80">
        <template #default="scope">
          <el-switch v-model="scope.row.enableFlag" active-value="1" inactive-value="0"
            @change="handleEnableChange(scope.row)" />
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 批量绑定弹窗 -->
    <BindDialog v-model:showFlag="bindOpen" @success="getList" />
  </div>
</template>

<script setup lang="ts" name="ProUserWorkstation">
import { ref, reactive, toRefs, getCurrentInstance, onMounted } from 'vue'
import type { UserWorkstation, UserWorkstationQueryParams } from '@/types/api/mes/pro/userworkstation'
import { listUserWorkstation, updateUserWorkstation } from '@/api/mes/pro/userworkstation'
import BindDialog from './components/BindDialog.vue'

const { proxy } = getCurrentInstance() as any

const loading = ref(true)
const bindOpen = ref(false)
const showSearch = ref(true)
const total = ref(0)
const dataList = ref<UserWorkstation[]>([])

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    userKeyword: undefined,
    workstationKeyword: undefined,
    enableFlag: undefined
  } as UserWorkstationQueryParams
})
const { queryParams } = toRefs(data)

onMounted(() => getList())

function getList() {
  loading.value = true
  listUserWorkstation(queryParams.value)
    .then((r: any) => { dataList.value = r.rows; total.value = r.total })
    .catch(() => proxy.$modal.msgError('查询失败'))
    .finally(() => loading.value = false)
}

function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleAdd() { bindOpen.value = true }

function handleEnableChange(row: UserWorkstation & { enableFlag: string }) {
  const newVal = row.enableFlag
  const text = newVal === '1' ? '启用' : '停用'
  // 还原开关并重查（取消确认 / 服务端更新失败均走这里，避免乐观更新残留）
  const revert = () => {
    ;(row as any).enableFlag = newVal === '1' ? '0' : '1'
    getList()
  }
  proxy.$modal.confirm(`确认要${text}"${row.nickName || row.userName}"的"${row.workstationName}"工位绑定吗？`)
    .then(() => updateUserWorkstation({ recordId: row.recordId, enableFlag: newVal } as any)
      .then(() => proxy.$modal.msgSuccess(`${text}成功`))
      // 更新失败：请求拦截器已弹服务端错误，这里仅还原开关+重查，避免重复 toast
      .catch(() => revert()))
    .catch(() => revert())
}

function handleExport() {
  proxy.download('/mes/pro/userworkstation/export', { ...queryParams.value },
    `用户工作站绑定_${new Date().getTime()}.xlsx`)
}
</script>
