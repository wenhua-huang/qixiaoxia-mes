<template>
  <el-card class="todo-panel" shadow="hover">
    <template #header>
      <div class="todo-header">
        <span class="todo-title">
          我的待办
          <el-tag v-if="pendingCount > 0" type="danger" size="small" round>{{ pendingCount }}</el-tag>
        </span>
        <el-button link type="primary" size="small" @click="router.push('/mes/sys/todolist')">
          查看全部<el-icon class="el-icon--right"><ArrowRight /></el-icon>
        </el-button>
      </div>
    </template>

    <el-tabs v-model="activeTab" class="todo-tabs" @tab-change="loadTodos">
      <el-tab-pane name="PENDING">
        <template #label>待处理<span v-if="pendingCount" class="tab-count">{{ pendingCount }}</span></template>
      </el-tab-pane>
      <el-tab-pane label="已完成" name="COMPLETED" />
    </el-tabs>

    <el-table :data="todos" v-loading="loading" size="small" :max-height="300" empty-text="暂无待办">
      <el-table-column label="待办事项" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">
          <el-link type="primary" :underline="false" @click="goTodo(row)">{{ row.todoTitle }}</el-link>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="72" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="typeTag(row.todoType)">{{ typeLabel(row.todoType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="优先级" width="64" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="priorityTag(row.priority)" effect="plain">{{ priorityLabel(row.priority) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="来源单据" width="150" prop="sourceDocCode" show-overflow-tooltip />
      <el-table-column label="创建时间" width="150" prop="createTime" />
      <el-table-column label="操作" width="72" align="center" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status !== 'COMPLETED'" link type="primary" size="small" @click="goTodo(row)">去处理</el-button>
          <span v-else class="text-muted">—</span>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup lang="ts" name="TodoPanel">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight } from '@element-plus/icons-vue'
import { listTodoList, countByStatus } from '@/api/mes/sys/todolist'
import useUserStore from '@/store/modules/user'
import type { SysTodoList } from '@/types/api/mes/sys/todolist'

const router = useRouter()
const userStore = useUserStore()

const todos = ref<SysTodoList[]>([])
const loading = ref(false)
const activeTab = ref<'PENDING' | 'COMPLETED'>('PENDING')
const pendingCount = ref(0)

/** QC 检验类型 → 首页路由（待办 sourceDocType 即 IQC/IPQC/OQC/RQC） */
const QC_ROUTE: Record<string, string> = {
  IQC: '/qc/qciqc',
  IPQC: '/qc/qcipqc',
  OQC: '/qc/qcoqc',
  RQC: '/qc/qcrqc'
}

const TYPE_LABEL: Record<string, string> = {
  QC_CHECK: '质检', APPROVAL: '审批', DV_CHECK: '设备点检',
  MAINTEN: '保养', REPAIR: '维修', OTHER: '其他'
}
const TYPE_TAG: Record<string, string> = {
  QC_CHECK: 'success', APPROVAL: 'warning', DV_CHECK: 'info',
  MAINTEN: '', REPAIR: 'danger', OTHER: 'info'
}
const PRIORITY_LABEL: Record<string, string> = { URGENT: '紧急', HIGH: '高', NORMAL: '普通', LOW: '低' }
const PRIORITY_TAG: Record<string, string> = { URGENT: 'danger', HIGH: 'warning', NORMAL: 'info', LOW: '' }

const typeLabel = (t?: string) => (t && TYPE_LABEL[t]) || t || '其他'
const typeTag = (t?: string) => (t && TYPE_TAG[t]) || 'info'
const priorityLabel = (p?: string) => (p && PRIORITY_LABEL[p]) || p || '普通'
const priorityTag = (p?: string) => (p && PRIORITY_TAG[p]) || 'info'

async function loadTodos() {
  loading.value = true
  try {
    const res = await listTodoList({
      userId: userStore.id as number,
      status: activeTab.value,
      pageNum: 1,
      pageSize: 10
    })
    todos.value = res.rows || []
  } finally {
    loading.value = false
  }
}

async function loadCount() {
  try {
    const res = await countByStatus()
    const c = res.data || {}
    pendingCount.value = (c.PENDING || 0) + (c.PROCESSING || 0)
  } catch { /* 徽章非关键，失败静默 */ }
}

/** 点击待办：QC 类跳对应检验列表并自动打开单据；其余跳待办管理页 */
function goTodo(row: SysTodoList) {
  const qcType = row.sourceDocType
  if (qcType && QC_ROUTE[qcType] && row.sourceDocId) {
    router.push({
      path: QC_ROUTE[qcType],
      query: {
        openId: String(row.sourceDocId),
        ...(row.status === 'COMPLETED' ? { ro: '1' } : {})
      }
    })
  } else {
    router.push('/mes/sys/todolist')
  }
}

onMounted(() => {
  loadTodos()
  loadCount()
})
</script>

<style scoped>
.todo-panel { margin-bottom: 0; }
.todo-header { display: flex; align-items: center; justify-content: space-between; }
.todo-title { font-weight: 600; font-size: 15px; display: inline-flex; align-items: center; gap: 8px; }
.todo-tabs :deep(.el-tabs__header) { margin-bottom: 10px; }
.tab-count {
  display: inline-block; min-width: 16px; padding: 0 5px; margin-left: 4px;
  font-size: 11px; line-height: 16px; color: #fff; background: #f56c6c;
  border-radius: 8px; text-align: center;
}
.text-muted { color: #c0c4cc; }
</style>
