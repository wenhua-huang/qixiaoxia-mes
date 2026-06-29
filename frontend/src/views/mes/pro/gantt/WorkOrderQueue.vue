<template>
  <div class="wo-queue">
    <div class="queue-header">
      <b>待排产工单</b>
      <el-button link size="small" @click="load" :loading="loading">刷新</el-button>
    </div>
    <div class="queue-list" v-loading="loading">
      <el-empty v-if="!list.length" description="无待排产工单" :image-size="40" />
      <div v-for="wo in list" :key="wo.workorderId" class="queue-card"
        :class="{ active: activeId === wo.workorderId }"
        @click="$emit('select', wo.workorderId)">
        <div class="card-code">{{ wo.workorderCode }}</div>
        <div class="card-name">{{ wo.productName }}</div>
        <div class="card-meta">
          <el-tag size="small" :type="statusMap[wo.status]?.type">{{ statusMap[wo.status]?.label }}</el-tag>
          <span style="font-size:11px">数量: {{ wo.quantity }}</span>
        </div>
        <el-button size="small" type="primary" plain @click.stop="scheduleOne(wo)"
          :loading="schedId === wo.workorderId">
          自动排产
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { listWorkorder } from '@/api/mes/pro/workorder'

const emit = defineEmits<{ (e: 'select', id: number): void; (e: 'scheduled'): void }>()

const loading = ref(false)
const schedId = ref<number | null>(null)
const activeId = ref<number | null>(null)
const list = ref<any[]>([])

const statusMap: Record<string, any> = {
  PREPARE: { label: '待生产', type: 'info' },
  PRODUCING: { label: '生产中', type: 'warning' },
  COMPLETED: { label: '已完成', type: 'success' },
  CANCEL: { label: '已取消', type: 'danger' },
}

async function load() {
  loading.value = true
  try {
    // 排除已完成/取消的工单
    const res: any = await listWorkorder({ pageSize: 50 })
    const all = res?.rows || []
    list.value = all.filter((wo: any) => wo.status !== 'COMPLETED' && wo.status !== 'CANCEL' && wo.status !== 'CLOSED')
  } finally { loading.value = false }
}

async function scheduleOne(wo: any) {
  schedId.value = wo.workorderId
  try {
    await request({ url: `/mes/pro/gantt/schedule/${wo.workorderId}`, method: 'post' })
    ElMessage.success(`${wo.workorderCode} 排产完成`)
    emit('scheduled')
  } catch { ElMessage.error('排产失败') }
  finally { schedId.value = null }
}

function setActive(id: number | null) { activeId.value = id }

onMounted(load)
defineExpose({ load, setActive })
</script>

<style scoped>
.wo-queue { width: 220px; flex-shrink:0; border-right:1px solid #e4e7ed; background:#fafafa; overflow-y:auto; max-height:calc(100vh - 250px); }
.queue-header { display:flex; align-items:center; justify-content:space-between; padding:6px 8px; border-bottom:1px solid #e4e7ed; position:sticky; top:0; background:#f5f7fa; z-index:1; b { font-size:12px; } }
.queue-list { padding:4px; }
.queue-card { padding:6px 8px; margin-bottom:4px; background:#fff; border-radius:4px; border:1px solid #ebeef5; cursor:pointer; transition: all .15s; &:hover { border-color:#409eff; } &.active { border-color:#409eff; background:#ecf5ff; } .card-code { font-size:12px; font-weight:600; color:#303133; } .card-name { font-size:11px; color:#606266; margin:2px 0; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; } .card-meta { display:flex; align-items:center; gap:6px; margin:4px 0; } }
</style>
