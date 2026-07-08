<template>
  <el-drawer
    :title="'工单齐套看板 — ' + dashboard?.workorderCode"
    v-model="visible"
    direction="rtl"
    size="950px"
    append-to-body
    @close="handleClose"
  >
    <!-- 工单信息条 -->
    <el-descriptions :column="4" border size="small" style="margin-bottom: 16px">
      <el-descriptions-item label="产品">{{ dashboard?.productName }}</el-descriptions-item>
      <el-descriptions-item label="计划数量">{{ dashboard?.planQuantity }} {{ dashboard?.unitName }}</el-descriptions-item>
      <el-descriptions-item label="需求日期">{{ dashboard?.requestDate }}</el-descriptions-item>
      <el-descriptions-item label="状态">
        <el-tag :type="statusTagType" size="small">{{ statusLabel }}</el-tag>
      </el-descriptions-item>
    </el-descriptions>

    <!-- 操作按钮区 -->
    <el-space wrap style="margin-bottom: 16px">
      <el-button type="primary" @click="handleGenerateAll" :loading="genLoading">
        一键全部生成
      </el-button>
      <el-button @click="handleGenerateIssue" :loading="genLoading" :disabled="!hasMaterialShortage">
        仅生成领料单
      </el-button>
      <el-button @click="handleGeneratePurOrder" :loading="genLoading" :disabled="!canGenPurOrder">
        仅生成采购单
      </el-button>
      <el-tooltip content="需已报工审核后自动或手动触发" :disabled="isCompleted">
        <el-button @click="handleGenerateReceipt" :loading="genLoading" :disabled="!isCompleted || dashboard?.receiptRecommend?.alreadyHasReceipt">
          仅生成入库单
        </el-button>
      </el-tooltip>
      <el-button @click="handleGenerateReturn" :loading="genLoading" :disabled="!isCompleted">
        仅生成退料单
      </el-button>
      <el-tag v-if="genMsg" type="info">{{ genMsg }}</el-tag>
    </el-space>

    <!-- 面板 1: 物料齐套分析 -->
    <el-collapse v-model="activePanels">
      <el-collapse-item name="material">
        <template #title>
          <div class="panel-header">
            <span>物料齐套分析</span>
            <el-tag :type="dashboard?.allSufficient ? 'success' : 'warning'" size="small" style="margin-left: 12px">
              {{ dashboard?.sufficientCount ?? 0 }}/{{ (dashboard?.sufficientCount ?? 0) + (dashboard?.shortageCount ?? 0) }} 齐套
            </el-tag>
          </div>
        </template>
        <el-table :data="dashboard?.materialRows" size="small" border>
          <el-table-column label="序号" type="index" width="55" align="center" />
          <el-table-column label="物料编码" prop="itemCode" width="120" align="center" />
          <el-table-column label="物料名称" prop="itemName" :show-overflow-tooltip="true" />
          <el-table-column label="需求数量" width="110" align="center">
            <template #default="scope">{{ scope.row.requiredQty }} {{ scope.row.unitName }}</template>
          </el-table-column>
          <el-table-column label="可用库存" width="110" align="center">
            <template #default="scope">{{ scope.row.availableQty }} {{ scope.row.unitName }}</template>
          </el-table-column>
          <el-table-column label="本单已领/预占" width="120" align="center">
            <template #default="scope">
              <span v-if="(scope.row.reservedForOrder ?? 0) > 0" style="color: #409EFF">
                {{ scope.row.reservedForOrder }} {{ scope.row.unitName }}
              </span>
              <span v-else style="color: #909399">0</span>
            </template>
          </el-table-column>
          <el-table-column label="缺口" width="100" align="center">
            <template #default="scope">
              <span :style="{ color: scope.row.sufficient ? '#67C23A' : '#F56C6C' }">
                {{ scope.row.shortageQty }} {{ scope.row.unitName }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="齐套" width="70" align="center">
            <template #default="scope">
              <el-tag :type="scope.row.sufficient ? 'success' : 'danger'" size="small">
                {{ scope.row.sufficient ? '充足' : '缺料' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-collapse-item>

      <!-- 面板 2: 生产领料单 -->
      <el-collapse-item name="issue">
        <template #title>
          <div class="panel-header">
            <span>生产领料单</span>
            <el-tag size="small" style="margin-left: 12px" :type="dashboard?.hasIssueDocs ? '' : 'info'">
              {{ issueStatusSummary }}
            </el-tag>
          </div>
        </template>
        <el-table v-if="dashboard?.hasIssueDocs" :data="dashboard?.issueStatuses" size="small" border>
          <el-table-column label="单据编码" prop="issueCode" width="150" align="center" />
          <el-table-column label="单据名称" prop="issueName" :show-overflow-tooltip="true" />
          <el-table-column label="状态" width="90" align="center">
            <template #default="scope">
              <el-tag :type="scope.row.status === 'POSTED' ? 'success' : scope.row.status === 'DRAFT' ? 'warning' : 'info'" size="small">
                {{ scope.row.status === 'POSTED' ? '已过账' : scope.row.status === 'DRAFT' ? '草稿' : scope.row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="领料总数" prop="totalQuantity" width="100" align="center" />
        </el-table>
        <el-empty v-else description="暂无领料单" :image-size="50" />
      </el-collapse-item>

      <!-- 面板 3: 采购单建议 -->
      <el-collapse-item name="purOrder">
        <template #title>
          <div class="panel-header">
            <span>采购单建议</span>
            <el-tag size="small" style="margin-left: 12px" :type="dashboard?.hasPurchaseRecommend ? 'warning' : 'success'">
              {{ dashboard?.hasPurchaseRecommend ? (dashboard?.purchaseRecommends?.length ?? 0) + '项需采购' : '无需采购' }}
            </el-tag>
          </div>
        </template>
        <el-table v-if="dashboard?.hasPurchaseRecommend" :data="dashboard?.purchaseRecommends" size="small" border>
          <el-table-column label="物料编码" prop="itemCode" width="120" align="center" />
          <el-table-column label="物料名称" prop="itemName" :show-overflow-tooltip="true" />
          <el-table-column label="缺口" width="100" align="center">
            <template #default="scope">{{ scope.row.shortageQty }} {{ scope.row.unitName }}</template>
          </el-table-column>
          <el-table-column label="建议采购" width="100" align="center">
            <template #default="scope">{{ scope.row.recommendedQty }} {{ scope.row.unitName }}</template>
          </el-table-column>
          <el-table-column label="在途PO" width="140" align="center">
            <template #default="scope">
              <span v-if="scope.row.hasPendingPO" style="color: #409EFF">{{ scope.row.pendingPOInfo }}</span>
              <span v-else style="color: #909399">无</span>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="所有物料齐套，无需采购" :image-size="50" />
      </el-collapse-item>

      <!-- 面板 4: 退料单 -->
      <el-collapse-item name="return">
        <template #title>
          <div class="panel-header">
            <span>退料单</span>
            <el-tag size="small" style="margin-left: 12px" :type="dashboard?.returnReady ? 'warning' : 'info'">
              {{ dashboard?.returnReady ? '有余料可退' : dashboard?.hasPendingReturns ? (dashboard?.returnStatuses?.length ?? 0) + '张退料单' : '暂无退料' }}
            </el-tag>
          </div>
        </template>
        <el-table v-if="dashboard?.hasPendingReturns" :data="dashboard?.returnStatuses" size="small" border>
          <el-table-column label="单据编码" prop="rtCode" width="150" align="center" />
          <el-table-column label="单据名称" prop="rtName" :show-overflow-tooltip="true" />
          <el-table-column label="关联领料单" prop="issueCode" width="150" align="center" />
          <el-table-column label="状态" width="90" align="center">
            <template #default="scope">
              <el-tag :type="scope.row.status === 'POSTED' ? 'success' : 'warning'" size="small">
                {{ scope.row.status === 'POSTED' ? '已过账' : '草稿' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="退料总数" prop="totalQuantity" width="100" align="center" />
        </el-table>
        <el-empty v-else description="暂无退料单" :image-size="50">
          <span v-if="!isCompleted" style="color: #909399; font-size: 12px">工单完工后自动计算余料</span>
        </el-empty>
      </el-collapse-item>

      <!-- 面板 5: 产品入库单 -->
      <el-collapse-item name="receipt">
        <template #title>
          <div class="panel-header">
            <span>产品入库单</span>
            <el-tag size="small" style="margin-left: 12px" :type="dashboard?.receiptReady ? 'warning' : 'info'">
              {{ receiptStatusLabel }}
            </el-tag>
          </div>
        </template>
        <el-descriptions v-if="dashboard?.receiptRecommend" :column="2" border size="small">
          <el-descriptions-item label="已产出">{{ dashboard.receiptRecommend.producedQty }}</el-descriptions-item>
          <el-descriptions-item label="合格数">{{ dashboard.receiptRecommend.qualifiedQty }}</el-descriptions-item>
          <el-descriptions-item label="末工序">{{ dashboard.receiptRecommend.lastProcessName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="已有入库单">
            <el-tag v-if="dashboard.receiptRecommend.alreadyHasReceipt" type="success" size="small">
              {{ dashboard.receiptRecommend.existingReceiptCode }}
            </el-tag>
            <span v-else style="color: #909399">无</span>
          </el-descriptions-item>
        </el-descriptions>
        <el-empty v-if="!dashboard?.receiptRecommend?.recommended" description="暂无合格品可入库" :image-size="50">
          <span v-if="!isCompleted" style="color: #909399; font-size: 12px">末工序报工审核后自动生成</span>
        </el-empty>
      </el-collapse-item>
    </el-collapse>
  </el-drawer>

  <!-- 采购单快捷创建向导 -->
  <PurOrderWizard v-model="purWizOpen" :workorderId="workorderId" @refresh="loadData(); emit('refresh')" />
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { loadKitDashboard, generateDocs } from '@/api/mes/pro/kit'
import { ElMessage } from 'element-plus'
import PurOrderWizard from './PurOrderWizard.vue'

const props = defineProps<{
  modelValue: boolean
  workorderId: number | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  (e: 'refresh'): void
}>()

const visible = ref(false)
const loading = ref(false)
const genLoading = ref(false)
const genMsg = ref('')
const purWizOpen = ref(false)
const dashboard = ref<any>(null)
const activePanels = ref<string[]>(['material'])

// 从 prop 同步 visible
watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val && props.workorderId) {
    loadData()
  }
})

watch(visible, (val) => {
  emit('update:modelValue', val)
})

function loadData() {
  if (!props.workorderId) return
  loading.value = true
  loadKitDashboard(props.workorderId).then(r => {
    dashboard.value = r.data
    // 自动展开有内容的/有问题的面板
    const panels: string[] = ['material']
    if (dashboard.value?.allSufficient === false) panels.push('purOrder')
    if (dashboard.value?.hasIssueDocs) panels.push('issue')
    if (dashboard.value?.receiptReady) panels.push('receipt')
    activePanels.value = panels
  }).finally(() => { loading.value = false })
}

const isCompleted = computed(() => dashboard.value?.workorderStatus === 'COMPLETED')

const statusLabel = computed(() => {
  const m: Record<string, string> = { PREPARE: '待生产', PRODUCING: '生产中', COMPLETED: '已完成', CANCEL: '已取消', CLOSED: '已关闭' }
  return m[dashboard.value?.workorderStatus] || dashboard.value?.workorderStatus || '-'
})

const statusTagType = computed(() => {
  const m: Record<string, string> = { PREPARE: 'info', PRODUCING: 'warning', COMPLETED: 'success', CANCEL: 'danger', CLOSED: '' }
  return m[dashboard.value?.workorderStatus] || ''
})

const issueStatusSummary = computed(() => {
  if (!dashboard.value?.hasIssueDocs) return '未生成'
  return `${dashboard.value.issuePostedCount ?? 0}张已过账, ${dashboard.value.issueConfirmedCount ?? 0}张已确认, ${dashboard.value.issueDraftCount ?? 0}张草稿`
})

const receiptStatusLabel = computed(() => {
  const rec = dashboard.value?.receiptRecommend
  if (rec?.alreadyHasReceipt) return `已生成：${rec.existingReceiptCode}`
  if (rec?.recommended) return `可入库：${rec.qualifiedQty}`
  return '暂无'
})

const hasMaterialShortage = computed(() => dashboard.value && !dashboard.value?.allSufficient)
const canGenPurOrder = computed(() => dashboard.value?.hasPurchaseRecommend)

function handleGenerateAll() {
  if (!props.workorderId) return
  genLoading.value = true
  genMsg.value = '正在生成...'
  generateDocs({
    workorderId: props.workorderId,
    generateIssue: true,
    generatePurOrder: dashboard.value?.hasPurchaseRecommend ?? false,
    generateReturn: isCompleted.value,
    generateReceipt: isCompleted.value && !dashboard.value?.receiptRecommend?.alreadyHasReceipt
  }).then(r => {
    genMsg.value = r.data?.message || '生成完成'
    ElMessage.success('一键生成完成')
    loadData()
    emit('refresh')
  }).catch(() => { genMsg.value = '' }).finally(() => { genLoading.value = false })
}

function handleGenerateIssue() {
  if (!props.workorderId) return
  genLoading.value = true
  genMsg.value = '正在生成领料单...'
  generateDocs({ workorderId: props.workorderId, generateIssue: true, generatePurOrder: false, generateReturn: false, generateReceipt: false }).then(r => {
    genMsg.value = r.data?.message || '生成完成'
    ElMessage.success('领料单生成完成')
    loadData()
    emit('refresh')
  }).catch(() => { genMsg.value = '' }).finally(() => { genLoading.value = false })
}

function handleGeneratePurOrder() {
  if (!props.workorderId) return
  purWizOpen.value = true
}

function handleGenerateReceipt() {
  if (!props.workorderId) return
  genLoading.value = true
  genMsg.value = '正在生成入库单...'
  generateDocs({ workorderId: props.workorderId, generateIssue: false, generatePurOrder: false, generateReturn: false, generateReceipt: true }).then(r => {
    genMsg.value = r.data?.message || '生成完成'
    ElMessage.success('入库单生成完成')
    loadData()
    emit('refresh')
  }).catch(() => { genMsg.value = '' }).finally(() => { genLoading.value = false })
}

function handleGenerateReturn() {
  if (!props.workorderId) return
  genLoading.value = true
  genMsg.value = '正在生成退料单...'
  generateDocs({ workorderId: props.workorderId, generateIssue: false, generatePurOrder: false, generateReturn: true, generateReceipt: false }).then(r => {
    genMsg.value = r.data?.message || '生成完成'
    ElMessage.success('退料单生成完成')
    loadData()
    emit('refresh')
  }).catch(() => { genMsg.value = '' }).finally(() => { genLoading.value = false })
}

function handleClose() {
  visible.value = false
}
</script>

<style scoped>
.panel-header {
  display: flex;
  align-items: center;
  font-weight: 500;
}
:deep(.el-collapse-item__header) {
  font-size: 14px;
  padding: 0 8px;
}
</style>
