<template>
  <div class="app-container">
    <el-tabs v-model="activeTab" type="border-card">
      <el-tab-pane label="追溯记录" name="list">
        <TraceListTab ref="listRef" @traceFromRow="traceFromRow" />
      </el-tab-pane>
      <el-tab-pane label="追溯查询" name="chain">
        <TraceChainTab :initialQuery="chainInitialQuery" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts" name="MaterialTrace">
import { ref } from 'vue'
import TraceListTab from './components/TraceListTab.vue'
import TraceChainTab from './components/TraceChainTab.vue'

const activeTab = ref('list')
const chainInitialQuery = ref<{ nodeType: string; nodeId: string; direction: 'forward' | 'backward' } | null>(null)

/** 列表行「追去向/追来源」：预填追溯查询并切到查询 tab */
function traceFromRow(row: any, direction: 'forward' | 'backward') {
  chainInitialQuery.value = direction === 'forward'
    ? { nodeType: row.parentType, nodeId: String(row.parentId), direction: 'forward' }
    : { nodeType: row.childType, nodeId: String(row.childId), direction: 'backward' }
  activeTab.value = 'chain'
}
</script>

<style scoped lang="scss">
:deep(.el-form-item__label) { padding-right: 16px !important; }
</style>
