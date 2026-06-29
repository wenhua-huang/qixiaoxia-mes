<template>
  <div class="snapshot-panel">
    <el-card shadow="never" size="small" header="排产快照">
      <el-button size="small" type="primary" @click="createSnapshot" :icon="Plus" :loading="creating">
        保存当前快照
      </el-button>
    </el-card>
    <el-table :data="list" size="small" v-loading="loading" @row-click="preview" highlight-current-row
      empty-text="暂无快照，拖拽调整后点击「保存当前快照」">
      <el-table-column prop="name" label="名称" min-width="120" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="80" align="center">
        <template #default="{row}">
          <el-tag :type="row.status==='DRAFT'?'info':row.status==='PUBLISHED'?'success':'danger'" size="small">
            {{ row.status==='DRAFT'?'草稿':row.status==='PUBLISHED'?'已发布':'已废弃' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="时间" width="140" />
      <el-table-column label="操作" width="120" align="center">
        <template #default="{row}">
          <el-button v-if="row.status==='DRAFT'" link type="success" size="small"
            @click.stop="publish(row)">发布</el-button>
          <el-button v-if="row.status==='DRAFT'" link type="danger" size="small"
            @click.stop="discard(row)">废弃</el-button>
          <el-button link type="danger" size="small" @click.stop="del(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const props = defineProps<{ workorderId?: number | null }>()
const emit = defineEmits<{ (e: 'preview', id: number): void; (e: 'refresh'): void }>()

const loading = ref(false)
const creating = ref(false)
const list = ref<any[]>([])

async function load() {
  loading.value = true
  try {
    const res: any = await request({ url: '/mes/pro/gantt/snapshot/list', method: 'get' })
    list.value = res?.rows || []
  } finally { loading.value = false }
}

async function createSnapshot() {
  if (!props.workorderId) { ElMessage.warning('请先选择工单再保存快照'); return }
  creating.value = true
  try {
    await request({
      url: '/mes/pro/gantt/snapshot',
      method: 'post',
      data: { name: '排产快照 ' + new Date().toLocaleString(), workorderId: props.workorderId }
    })
    ElMessage.success('快照已保存')
    load()
  } catch (e: any) {
    const msg = e?.response?.data?.msg || e?.msg || '保存失败'
    ElMessage.warning(msg)
  }
  finally { creating.value = false }
}

async function publish(row: any) {
  if (row.status === 'PUBLISHED') { ElMessage.warning('该快照已发布，无需重复操作'); return }
  if (row.status === 'DISCARDED') { ElMessage.warning('该快照已废弃'); return }
  try {
    await ElMessageBox.confirm(`确认发布快照「${row.name}」？将恢复该快照保存的排产时间`, '发布快照', { type: 'warning' })
    await request({ url: `/mes/pro/gantt/snapshot/${row.id}/publish`, method: 'put' })
    ElMessage.success('快照已发布 — 排产时间已恢复')
    load()
    emit('refresh')
  } catch { /* 取消或失败 */ }
}

async function discard(row: any) {
  try {
    await request({ url: `/mes/pro/gantt/snapshot/${row.id}/discard`, method: 'put' })
    ElMessage.success('已废弃')
    load()
  } catch { }
}

async function del(row: any) {
  try {
    await ElMessageBox.confirm('确认删除?')
    await request({ url: `/mes/pro/gantt/snapshot/${row.id}`, method: 'delete' })
    load()
  } catch { }
}

function preview(row: any) {
  if (row.status === 'DRAFT') {
    emit('preview', row.id)
  }
}

onMounted(load)
defineExpose({ load })
</script>

<style scoped>
.snapshot-panel { margin-top: 8px; }
</style>
