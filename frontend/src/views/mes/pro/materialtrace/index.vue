<template>
  <div class="app-container">
    <el-tabs v-model="activeTab" type="border-card">
      <!-- ==================== Tab1: 追溯记录 ==================== -->
      <el-tab-pane label="追溯记录" name="list">
        <el-form :model="queryParams" ref="queryRef" size="small" :inline="true" v-show="showSearch" label-width="80px">
          <el-form-item label="事件类型" prop="traceType">
            <el-select v-model="queryParams.traceType" placeholder="全部" clearable style="width:140px">
              <el-option v-for="d in traceTypeOptions" :key="d.value" :label="d.label" :value="d.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="工单号" prop="workorderCode">
            <el-input v-model="queryParams.workorderCode" placeholder="工单编码 WO2026..." clearable style="width:180px" @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="流转卡号" prop="cardCode">
            <el-input v-model="queryParams.cardCode" placeholder="流转卡号 000CRD..." clearable style="width:180px" @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="物料" prop="queryItemName">
            <el-input v-model="queryParams.queryItemName" placeholder="物料名称/编码" clearable style="width:180px" @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="批次号" prop="queryBatchCode">
            <el-input v-model="queryParams.queryBatchCode" placeholder="批次号 BATCH..." clearable style="width:160px" @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="时间范围" prop="dateRange">
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              style="width:240px"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-row :gutter="10" class="mb8">
          <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
        </el-row>

        <el-table v-loading="loading" :data="traceList" stripe>
          <el-table-column label="事件" align="center" prop="traceType" width="100">
            <template #default="scope">
              <el-tag :type="getTypeTag(scope.row.traceType)" size="small" effect="light">{{ getTypeLabel(scope.row.traceType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="物料" align="left" min-width="180" show-overflow-tooltip>
            <template #default="scope">
              <div>{{ scope.row.itemName || '—' }}</div>
              <div style="font-size:12px;color:#909399">{{ scope.row.itemCode }}</div>
            </template>
          </el-table-column>
          <el-table-column label="批次" align="center" prop="batchCode" width="140" show-overflow-tooltip>
            <template #default="scope">{{ scope.row.batchCode || '—' }}</template>
          </el-table-column>
          <el-table-column label="源（从哪来）" align="left" min-width="200" show-overflow-tooltip>
            <template #default="scope">{{ scope.row.parentDesc || '—' }}</template>
          </el-table-column>
          <el-table-column label="目标（到哪去）" align="left" min-width="200" show-overflow-tooltip>
            <template #default="scope">{{ scope.row.childDesc || '—' }}</template>
          </el-table-column>
          <el-table-column label="数量" align="center" width="110">
            <template #default="scope">
              {{ scope.row.quantity }} {{ scope.row.unitName || scope.row.unitOfMeasure || '' }}
            </template>
          </el-table-column>
          <el-table-column label="追溯时间" align="center" prop="traceTime" width="160">
            <template #default="scope">
              <span>{{ parseTime(scope.row.traceTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" width="240" class-name="small-padding fixed-width" fixed="right">
            <template #default="scope">
              <el-button link type="primary" icon="View" size="small" @click="handleView(scope.row)" v-hasPermi="['mes:pro:materialtrace:query']">详情</el-button>
              <el-button link type="success" size="small" @click="traceFromRow(scope.row, 'forward')" v-hasPermi="['mes:pro:materialtrace:query']">追去向 →</el-button>
              <el-button link type="warning" size="small" @click="traceFromRow(scope.row, 'backward')" v-hasPermi="['mes:pro:materialtrace:query']">← 追来源</el-button>
            </template>
          </el-table-column>
        </el-table>

        <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
      </el-tab-pane>

      <!-- ==================== Tab2: 追溯查询 ==================== -->
      <el-tab-pane label="追溯查询" name="chain">
        <!-- 手动查询区（默认展开，标签清晰） -->
        <el-card shadow="never" class="chain-search-card">
          <el-row :gutter="16" align="middle">
            <el-col :span="5">
              <el-form-item label="节点类型" label-width="80px">
                <el-select v-model="chain.nodeType" placeholder="选择起点类型" style="width:100%">
                  <el-option v-for="n in nodeTypeOptions" :key="n.value" :label="n.label" :value="n.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="4">
              <el-form-item label="ID" label-width="40px">
                <el-input v-model="chain.nodeId" placeholder="正整数" @keyup.enter="handleTrace" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="方向" label-width="50px">
                <el-radio-group v-model="chain.direction" size="small">
                  <el-radio-button value="forward">正向（追去向）</el-radio-button>
                  <el-radio-button value="backward">反向（追来源）</el-radio-button>
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col :span="7" style="text-align:right">
              <el-button type="primary" icon="Search" :loading="chainLoading" @click="handleTrace">一键追溯</el-button>
              <el-button icon="Refresh" @click="resetChain">清空</el-button>
            </el-col>
          </el-row>
          <div style="font-size:12px;color:#909399;margin-top:-8px">
            💡 推荐：在「追溯记录」列表点行内「追去向 →」/「← 追来源」按钮自动填入起点，无需手动输入
          </div>
        </el-card>

        <!-- 链路结果 -->
        <div v-if="chainNodes.length > 0" class="chain-result">
          <el-alert :title="chainSummary" :type="chainAlertType" :closable="false" show-icon style="margin-bottom:12px" />
          <el-alert
            v-if="chainEndedReason === 'END'"
            type="info" :closable="false" show-icon style="margin-bottom:16px"
            title="已追溯到链路末端：此节点之后没有后续流转记录"
          />

          <!-- 视图切换 -->
          <div style="margin-bottom:12px;display:flex;align-items:center;gap:12px">
            <el-radio-group v-model="chainViewMode" size="small">
              <el-radio-button value="graph">🌊 流向图</el-radio-button>
              <el-radio-button value="list">📋 树形列表</el-radio-button>
            </el-radio-group>
            <span v-if="chainViewMode === 'graph'" style="font-size:12px;color:#909399">
              滚轮缩放 · 拖拽平移 · 点击节点查看详情
            </span>
            <span v-else style="font-size:12px;color:#909399">
              点击节点查看详情 · 按深度缩进展示分叉
            </span>
          </div>

          <!-- ───── 流向图视图（SVG DAG） ───── -->
          <div v-if="chainViewMode === 'graph' && chainTree" class="dag-container">
            <div class="dag-toolbar">
              <el-button-group>
                <el-button size="small" icon="ZoomIn" @click="dagZoom(0.2)">放大</el-button>
                <el-button size="small" icon="ZoomOut" @click="dagZoom(-0.2)">缩小</el-button>
                <el-button size="small" icon="Refresh" @click="dagReset">复位</el-button>
              </el-button-group>
              <span style="margin-left:12px;font-size:12px;color:#909399">
                缩放 {{ Math.round(dagScale * 100) }}% · 共 {{ dagNodes.length }} 个节点
              </span>
            </div>
            <div class="dag-viewport" ref="dagViewport" @wheel="onWheel" @mousedown="onDragStart" @mousemove="onDragMove" @mouseup="onDragEnd" @mouseleave="onDragEnd">
              <svg :width="dagWidth" :height="dagHeight" :style="{ transform: `translate(${dagOffsetX}px, ${dagOffsetY}px) scale(${dagScale})`, transformOrigin: '0 0' }">
                <defs>
                  <!-- 箭头 -->
                  <marker id="arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
                    <path d="M0,0 L10,5 L0,10 z" fill="#909399" />
                  </marker>
                  <marker id="arrow-highlight" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
                    <path d="M0,0 L10,5 L0,10 z" fill="#409EFF" />
                  </marker>
                </defs>
                <!-- 连线 -->
                <g class="dag-edges">
                  <path
                    v-for="(edge, i) in dagEdges"
                    :key="'e' + i"
                    :d="edge.path"
                    :stroke="edge.highlight ? '#409EFF' : '#C0C4CC'"
                    :stroke-width="edge.highlight ? 2.5 : 1.5"
                    fill="none"
                    :marker-end="edge.highlight ? 'url(#arrow-highlight)' : 'url(#arrow)'"
                    :class="{ 'edge-highlight': edge.highlight }"
                  />
                  <!-- 边标签（数量+事件） -->
                  <g v-for="(edge, i) in dagEdges" :key="'el' + i">
                    <rect
                      :x="edge.labelX - 28" :y="edge.labelY - 9" width="56" height="18" rx="9"
                      :fill="edge.highlight ? '#ECF5FF' : '#F4F4F5'" stroke="#DCDFE6" stroke-width="0.5"
                    />
                    <text :x="edge.labelX" :y="edge.labelY + 4" text-anchor="middle" :font-size="10" :fill="edge.highlight ? '#409EFF' : '#606266'">
                      {{ edge.quantity }}{{ edge.unit || '' }}
                    </text>
                  </g>
                </g>
                <!-- 节点 -->
                <g v-for="(node, i) in dagNodes" :key="'n' + i"
                   :transform="`translate(${node.x}, ${node.y})`"
                   class="dag-node"
                   :class="{ 'dag-node-highlight': node.highlight, 'dag-node-cycle': node.cycle }"
                   @click="onNodeClick(node)"
                  >
                  <!-- 左侧色条 -->
                  <rect x="0" y="0" width="4" :height="node.height" :fill="nodeColor(node)" rx="2" />
                  <!-- 主体 -->
                  <rect x="4" y="0" :width="node.width - 4" :height="node.height" rx="6"
                        :fill="node.highlight ? '#ECF5FF' : '#FFFFFF'"
                        :stroke="node.cycle ? '#F56C6C' : (node.highlight ? '#409EFF' : '#DCDFE6')"
                        :stroke-width="node.highlight || node.cycle ? 2 : 1"
                        :stroke-dasharray="node.cycle ? '4 2' : 'none'"
                  />
                  <!-- 图标 -->
                  <text x="16" y="22" font-size="16">{{ node.icon }}</text>
                  <!-- 类型标签 -->
                  <text x="38" y="18" font-size="11" font-weight="600" :fill="nodeColorRaw(node)">{{ node.label }}</text>
                  <!-- 描述 -->
                  <text x="38" y="34" font-size="11" fill="#303133">
                    {{ truncate(node.title, 16) }}
                  </text>
                  <!-- 第二行：数量+事件 -->
                  <text x="38" y="50" font-size="10" fill="#909399">
                    <tspan v-if="node.quantity">{{ node.quantity }}{{ node.unit || '' }}</tspan>
                    <tspan v-if="node.event"> · {{ node.event }}</tspan>
                    <tspan v-if="node.cycle"> · 🔁 循环</tspan>
                  </text>
                </g>
              </svg>
            </div>
          </div>

          <!-- ───── 树形列表视图（缩进卡片，分叉并列展示） ───── -->
          <div v-if="chainViewMode === 'list'" class="tree-list">
            <div
              v-for="(node, idx) in chainNodes"
              :key="idx"
              class="tree-node-row"
              :class="{ 'tree-node-cycle': node.cycle, 'tree-node-root': node.depth === 0 }"
              :style="{ marginLeft: node.depth * 28 + 'px' }"
              @click="onTreeNodeClick(node)"
            >
              <!-- 缩进连接线 -->
              <span class="tree-indent-line" v-if="node.depth > 0"></span>
              <!-- 节点卡片 -->
              <div class="tree-node-card" :class="`card-${node.type}`">
                <div class="tree-node-head">
                  <span class="node-icon">{{ nodeIcon(node.type) }}</span>
                  <el-tag :type="nodeTagType(node)" size="small" effect="dark">{{ node.label }}</el-tag>
                  <span class="tree-node-title">{{ node.title }}</span>
                  <el-tag v-if="node.depth === 0" type="info" size="small" effect="plain" class="root-tag">起点</el-tag>
                  <el-tag v-if="node.cycle" type="danger" size="small" effect="plain">🔁 循环</el-tag>
                  <el-tag v-if="node.branchCount > 1" type="warning" size="small" effect="plain">分叉 {{ node.branchCount }} 路</el-tag>
                </div>
                <div class="tree-node-body" v-if="hasDetails(node)">
                  <span v-if="node.event" class="meta-item"><span class="meta-key">事件</span><el-tag size="small" :type="traceTypeTagMap[node.traceType] || 'info'" effect="light">{{ node.event }}</el-tag></span>
                  <span v-if="node.quantity" class="meta-item"><span class="meta-key">数量</span>{{ node.quantity }} {{ node.unit || '' }}</span>
                  <span v-if="node.itemName" class="meta-item"><span class="meta-key">物料</span>{{ node.itemName }}</span>
                  <span v-if="node.batchCode" class="meta-item"><span class="meta-key">批次</span>{{ node.batchCode }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <el-empty v-if="chainSearched && !chainTree" description="未找到追溯链路">
          <template #image>
            <el-icon :size="60" color="#909399"><Search /></el-icon>
          </template>
        </el-empty>
      </el-tab-pane>
    </el-tabs>

    <!-- 查看详情弹窗 -->
    <el-dialog title="物料追溯详情" v-model="viewOpen" width="680px" append-to-body>
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="事件类型">
          <el-tag :type="getTypeTag(viewForm.traceType)" size="small">{{ getTypeLabel(viewForm.traceType) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="物料">{{ viewForm.itemName }} <span style="color:#909399">{{ viewForm.itemCode }}</span></el-descriptions-item>
        <el-descriptions-item label="批次号">{{ viewForm.batchCode || '—' }}</el-descriptions-item>
        <el-descriptions-item label="数量">{{ viewForm.quantity }} {{ viewForm.unitName || viewForm.unitOfMeasure }}</el-descriptions-item>
        <el-descriptions-item label="源（从哪来）" :span="2">{{ viewForm.parentDesc || '—' }}</el-descriptions-item>
        <el-descriptions-item label="目标（到哪去）" :span="2">{{ viewForm.childDesc || '—' }}</el-descriptions-item>
        <el-descriptions-item label="工单">{{ viewForm.workorderCode || viewForm.workorderId || '—' }}</el-descriptions-item>
        <el-descriptions-item label="流转卡">{{ viewForm.cardCode || viewForm.cardId || '—' }}</el-descriptions-item>
        <el-descriptions-item label="追溯时间" :span="2">{{ parseTime(viewForm.traceTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</el-descriptions-item>
      </el-descriptions>
      <!-- 技术信息折叠（管理员用） -->
      <el-collapse style="margin-top:12px">
        <el-collapse-item title="🔧 技术信息（管理员）" name="tech">
          <div style="font-size:12px;color:#909399;line-height:1.8">
            traceId:{{ viewForm.traceId }}<br>
            链路: {{ viewForm.parentType }}:{{ viewForm.parentId }} → {{ viewForm.childType }}:{{ viewForm.childId }}<br>
            <span v-if="viewForm.transactionId">事务ID: {{ viewForm.transactionId }}</span>
            <span v-if="viewForm.processId"> | 工序ID: {{ viewForm.processId }}</span>
            <span v-if="viewForm.vendorId"> | 供应商ID: {{ viewForm.vendorId }}</span>
          </div>
        </el-collapse-item>
      </el-collapse>
      <template #footer>
        <el-button @click="viewOpen = false">关 闭</el-button>
      </template>
    </el-dialog>

    <!-- 节点详情弹窗（流向图/树形列表节点点击） -->
    <el-dialog :title="`节点详情 · ${selectedNode?.label || ''}`" v-model="nodeDetailOpen" width="520px" append-to-body>
      <el-descriptions v-if="selectedNode" :column="2" border size="small">
        <el-descriptions-item label="节点类型">
          <span class="node-icon">{{ selectedNode.icon }}</span>
          <el-tag :type="nodeTagType({ type: selectedNode.nodeType })" size="small" effect="dark">{{ selectedNode.label }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="节点ID">{{ selectedNode.nodeType }}:{{ selectedNode.nodeId }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ selectedNode.title || '—' }}</el-descriptions-item>
        <el-descriptions-item label="物料">{{ selectedNode.itemName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="批次">{{ selectedNode.batchCode || '—' }}</el-descriptions-item>
        <el-descriptions-item label="数量">{{ selectedNode.quantity || '—' }} {{ selectedNode.unit || '' }}</el-descriptions-item>
        <el-descriptions-item label="进入事件">{{ selectedNode.event || '（查询起点）' }}</el-descriptions-item>
        <el-descriptions-item label="深度">第 {{ selectedNode.depth ?? 0 }} 层</el-descriptions-item>
        <el-descriptions-item label="分支数">{{ selectedNode.childCount ?? 0 }} 个下游</el-descriptions-item>
        <el-descriptions-item v-if="selectedNode.cycle" label="循环" :span="2">
          <el-tag type="danger" size="small">🔁 循环引用节点（已停止展开）</el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button type="primary" @click="reTraceFromNode('forward')">以此为起点 · 正向追溯 →</el-button>
        <el-button type="warning" @click="reTraceFromNode('backward')">← 以此为起点 · 逆向追溯</el-button>
        <el-button @click="nodeDetailOpen = false">关 闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="MaterialTrace">
import { ref, reactive, toRefs, getCurrentInstance, nextTick, computed } from 'vue'
import { Search, Right } from '@element-plus/icons-vue'
import { listMaterialTrace, getMaterialTrace, traceChain } from '@/api/mes/pro/materialtrace'

const { proxy } = getCurrentInstance() as any

// ==================== 常量 ====================
const activeTab = ref('list')

const traceTypeOptions = [
  { label: '投料消耗', value: 'ISSUE' }, { label: '生产产出', value: 'PRODUCE' },
  { label: '工序加工', value: 'PROCESS' }, { label: '外协加工', value: 'OUTSOURCE_PROCESS' },
  { label: '合并', value: 'MERGE' }, { label: '调整', value: 'ADJUST' },
  { label: '采购入库', value: 'RECEIPT' },
  { label: '外协发料', value: 'OUTSOURCE_ISSUE' }, { label: '外协入库', value: 'OUTSOURCE_RECPT' },
  { label: '生产退料', value: 'RETURN' }, { label: '销售出库', value: 'SALES_OUT' },
  { label: '产出入库', value: 'PRODUCE_STOCKIN' }
]
const traceTypeLabelMap: Record<string, string> = {
  ISSUE: '投料消耗', PRODUCE: '生产产出', PROCESS: '工序加工', OUTSOURCE_PROCESS: '外协加工',
  MERGE: '合并', ADJUST: '调整',
  RECEIPT: '采购入库', OUTSOURCE_ISSUE: '外协发料', OUTSOURCE_RECPT: '外协入库',
  RETURN: '生产退料', SALES_OUT: '销售出库', PRODUCE_STOCKIN: '产出入库'
}
const traceTypeTagMap: Record<string, string> = {
  ISSUE: 'warning', PRODUCE: 'success', PROCESS: 'info', OUTSOURCE_PROCESS: 'warning',
  MERGE: 'info', ADJUST: 'danger',
  RECEIPT: '', OUTSOURCE_ISSUE: 'warning', OUTSOURCE_RECPT: 'success',
  RETURN: 'danger', SALES_OUT: 'danger', PRODUCE_STOCKIN: 'success'
}
function getTypeLabel(t: string): string { return traceTypeLabelMap[t] || t }
function getTypeTag(t: string): string { return traceTypeTagMap[t] || 'info' }

// ==================== Tab1: 追溯记录列表 ====================
const traceList = ref<any[]>([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const viewOpen = ref(false)
const viewForm = ref<any>({})
const dateRange = ref<string[]>([])

const data = reactive({
  queryParams: {
    pageNum: 1, pageSize: 10,
    traceType: undefined, workorderCode: undefined, cardCode: undefined,
    queryItemName: undefined, queryBatchCode: undefined
  } as any
})
const { queryParams } = toRefs(data)

/** 组装后端入参：dateRange → params.beginTime/endTime（若依标准模式） */
function buildQueryParams() {
  const p = { ...queryParams.value }
  // 若依 BaseEntity.params Map 传时间范围
  p.params = {}
  if (dateRange.value && dateRange.value.length === 2) {
    p.params.beginTime = dateRange.value[0] + ' 00:00:00'
    p.params.endTime = dateRange.value[1] + ' 23:59:59'
  }
  return p
}

function getList() {
  loading.value = true
  listMaterialTrace(buildQueryParams()).then((r: any) => {
    traceList.value = r.rows; total.value = r.total; loading.value = false
  }).catch(() => { loading.value = false })
}
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() {
  proxy.resetForm('queryRef')
  dateRange.value = []
  handleQuery()
}
function handleView(row: any) {
  getMaterialTrace(row.traceId).then((r: any) => { viewForm.value = r.data; viewOpen.value = true })
}

getList()

// ==================== Tab2: 追溯查询 ====================
// 后端 selectByParent/Child 实际支持的节点类型（剔除 BATCH/ROLL_DETAIL 等死选项）
const nodeTypeOptions = [
  { label: '流转卡', value: 'CARD' },
  { label: '工单', value: 'WORKORDER' },
  { label: '采购订单', value: 'PUR_ORDER' },
  { label: '报工记录', value: 'FEEDBACK' },
  { label: '库存记录', value: 'MATERIAL_STOCK' },
  { label: '供应商', value: 'VENDOR' },
]

const chain = reactive({ nodeType: 'CARD', nodeId: '', direction: 'backward' as 'forward' | 'backward' })
const chainLoading = ref(false)
const chainSearched = ref(false)
const chainNodes = ref<any[]>([])
const chainSummary = ref('')
const chainEndedReason = ref('')
// DAG 流向图 state
const chainTree = ref<any>(null)             // 后端返回的完整追溯树
const chainViewMode = ref<'graph' | 'list'>('graph')  // 视图切换，默认流向图
const dagNodes = ref<any[]>([])              // SVG 节点（带坐标）
const dagEdges = ref<any[]>([])              // SVG 连线（带路径）
// 节点详情弹窗
const nodeDetailOpen = ref(false)
const selectedNode = ref<any>(null)
const dagWidth = ref(1200)
const dagHeight = ref(600)
const dagScale = ref(1)
const dagOffsetX = ref(40)
const dagOffsetY = ref(40)
const dagViewport = ref<HTMLElement | null>(null)
let dagDragging = false
let dagDragStartX = 0
let dagDragStartY = 0
let dagOffsetStartX = 0
let dagOffsetStartY = 0

// 节点标签
const nodeLabelMap: Record<string, string> = {
  CARD: '流转卡', MATERIAL_STOCK: '库存记录', FEEDBACK: '报工记录',
  PUR_ORDER: '采购订单', VENDOR: '供应商', BATCH: '批次',
  WORKORDER: '工单', NONE: '未知', SALES_OUT: '销售出库'
}

// 节点 emoji 图标
function nodeIcon(type: string): string {
  const map: Record<string, string> = {
    PUR_ORDER: '🛒', VENDOR: '🏭', MATERIAL_STOCK: '📦', BATCH: '🏷️',
    CARD: '📋', FEEDBACK: '✅', WORKORDER: '🔧', SALES_OUT: '🚚', NONE: '❓'
  }
  return map[type] || '📌'
}

// el-timeline 节点颜色
function nodeColor(node: any): string {
  const map: Record<string, string> = {
    PUR_ORDER: '#909399', VENDOR: '#909399', MATERIAL_STOCK: '#409EFF',
    BATCH: '#409EFF', CARD: '#E6A23C', FEEDBACK: '#67C23A',
    WORKORDER: '#E6A23C', SALES_OUT: '#F56C6C'
  }
  return map[node.type] || '#409EFF'
}

// el-tag 类型
function nodeTagType(node: any): string {
  const map: Record<string, string> = {
    PUR_ORDER: 'info', VENDOR: 'info', MATERIAL_STOCK: '', BATCH: '',
    CARD: 'warning', FEEDBACK: 'success', WORKORDER: 'warning', SALES_OUT: 'danger'
  }
  return map[node.type] || 'info'
}

/**
 * 列表行一键追溯：从本行出发，向前/向后追完整链路
 * 「追去向 →」：从本行 parent（源头）往后追下游
 * 「← 追来源」：从本行 child（目标）往前追上游
 * 注意：只切 tab，不清理列表数据，方便用户切回继续浏览
 */
function traceFromRow(row: any, direction: 'forward' | 'backward') {
  if (direction === 'forward') {
    chain.nodeType = row.parentType
    chain.nodeId = String(row.parentId)
    chain.direction = 'forward'
  } else {
    chain.nodeType = row.childType
    chain.nodeId = String(row.childId)
    chain.direction = 'backward'
  }
  activeTab.value = 'chain'
  nextTick(() => { handleTrace() })
}

async function handleTrace() {
  const id = Number(chain.nodeId)
  if (!chain.nodeId || isNaN(id) || id <= 0) {
    proxy.$modal.msgError('请输入有效的节点ID（正整数）')
    return
  }

  chainLoading.value = true; chainSearched.value = true
  chainNodes.value = []; chainSummary.value = ''
  chainTree.value = null
  dagNodes.value = []; dagEdges.value = []

  try {
    const startLabel = nodeLabelMap[chain.nodeType] || chain.nodeType
    const dirLabel = chain.direction === 'forward' ? '正向' : '反向'

    const res: any = await traceChain(chain.nodeType, id, chain.direction)
    const result = res?.data || {}
    const tree: any = result.tree
    const endedReason: string = result.endedReason || ''

    chainTree.value = tree
    chainEndedReason.value = tree ? endedReason : ''

    if (tree) {
      // 从 tree 派生主链节点（DFS 第一条路径）+ 兄弟分支，供时间线视图用
      deriveLegacyFromTree(tree)
      // 计算流向图布局
      computeDagLayout(tree)
      // 摘要：统计节点总数
      const totalNodes = countTreeNodes(tree)
      const startDesc = tree.nodeDesc || `${startLabel} #${id}`
      const branchCount = countBranches(tree)
      const branchText = branchCount > 0 ? `，含 ${branchCount} 个分支` : ''
      chainSummary.value = `${dirLabel}追溯：从「${startDesc}」出发，共 ${totalNodes} 个节点${branchText}${endedReasonText(endedReason)}`
    } else {
      chainSummary.value = `${dirLabel}追溯：未找到「${startLabel} #${id}」的追溯记录`
    }
  } catch (e: any) {
    chainSummary.value = '追溯失败: ' + (e?.message || '未知错误')
  } finally {
    chainLoading.value = false
  }
}

/** 完整扁平化树（DFS，带 depth），供树形列表视图用——分叉节点并列展示 */
function deriveLegacyFromTree(tree: any) {
  const nodes: any[] = []
  const walk = (n: any, depth: number) => {
    nodes.push({
      nodeType: n.nodeType,
      nodeId: n.nodeId,
      type: n.nodeType,
      id: n.nodeId,
      label: nodeLabelMap[n.nodeType] || n.nodeType,
      icon: nodeIcon(n.nodeType),
      title: n.nodeDesc || `${n.nodeType} #${n.nodeId}`,
      itemName: n.itemName,
      batchCode: n.batchCode,
      quantity: n.quantity,
      unit: n.unitName,
      event: n.traceType ? getTypeLabel(n.traceType) : '',
      traceType: n.traceType,
      cycle: n.cycle,
      depth,
      branchCount: n.children?.length || 0,
      childCount: n.children?.length || 0
    })
    if (n.children && !n.cycle) {
      for (const c of n.children) walk(c, depth + 1)
    }
  }
  walk(tree, 0)
  chainNodes.value = nodes
}

/** 树形列表节点是否有详情可展示 */
function hasDetails(node: any): boolean {
  return !!(node.event || node.quantity || node.itemName || node.batchCode)
}

/** 树形列表节点点击：弹详情 */
function onTreeNodeClick(node: any) {
  selectedNode.value = node
  nodeDetailOpen.value = true
}

/** 统计树节点总数 */
function countTreeNodes(node: any): number {
  if (!node) return 0
  let count = 1
  if (node.children) {
    for (const c of node.children) count += countTreeNodes(c)
  }
  return count
}

/** 统计分支数（非主链的子节点总数） */
function countBranches(node: any): number {
  if (!node || !node.children) return 0
  let count = Math.max(0, node.children.length - 1)
  for (const c of node.children) count += countBranches(c)
  return count
}

// ══════════════════════════════════════════════════════
// 流向图（SVG DAG）布局算法 + 交互
// ══════════════════════════════════════════════════════
const NODE_W = 200
const NODE_H = 60
const COL_GAP = 80   // 列间距（连线空间）
const ROW_GAP = 16   // 行间距

/** 计算每个节点的 {x, y, height, ...}，简化的 Reingold-Tilford 横向树布局 */
function computeDagLayout(root: any) {
  const nodes: any[] = []
  const edges: any[] = []
  // 第一步：后序遍历，为每个节点分配 y 坐标（叶子按序堆叠，父节点居中于子树）
  let leafCursor = 0
  const layout = (n: any, depth: number): { top: number; bottom: number } => {
    const x = depth * (NODE_W + COL_GAP)
    if (!n.children || n.children.length === 0 || n.cycle) {
      // 叶子节点
      const y = leafCursor * (NODE_H + ROW_GAP)
      leafCursor++
      nodes.push({ ...buildNodeRender(n, depth), x, y, height: NODE_H, width: NODE_W, _depth: depth, _ref: n })
      return { top: y, bottom: y + NODE_H }
    }
    // 内部节点：先布局子节点
    const childRanges: { top: number; bottom: number; child: any }[] = []
    for (const c of n.children) {
      const r = layout(c, depth + 1)
      childRanges.push({ ...r, child: c })
    }
    // 父节点 y 居中于子树
    const top = childRanges[0].top
    const bottom = childRanges[childRanges.length - 1].bottom
    const y = (top + bottom) / 2 - NODE_H / 2
    nodes.push({ ...buildNodeRender(n, depth), x, y, height: NODE_H, width: NODE_W, _depth: depth, _ref: n })
    return { top, bottom }
  }
  layout(root, 0)

  // 第二步：生成边（父右侧 → 子左侧，贝塞尔曲线）
  for (const parent of nodes) {
    if (!parent._ref?.children) continue
    for (const childRef of parent._ref.children) {
      const child = nodes.find(n => n._ref === childRef)
      if (!child) continue
      const x1 = parent.x + parent.width
      const y1 = parent.y + parent.height / 2
      const x2 = child.x
      const y2 = child.y + child.height / 2
      const mx = (x1 + x2) / 2
      const path = `M ${x1},${y1} C ${mx},${y1} ${mx},${y2} ${x2},${y2}`
      edges.push({
        path,
        quantity: childRef.quantity,
        unit: childRef.unitName,
        labelX: mx,
        labelY: (y1 + y2) / 2,
        highlight: false
      })
    }
  }

  // 计算画布尺寸
  const maxX = nodes.length ? Math.max(...nodes.map(n => n.x + n.width)) : 800
  const maxY = nodes.length ? Math.max(...nodes.map(n => n.y + n.height)) : 400
  dagWidth.value = maxX + 80
  dagHeight.value = maxY + 80
  dagNodes.value = nodes
  dagEdges.value = edges
  // 复位视图
  dagScale.value = 1
  dagOffsetX.value = 40
  dagOffsetY.value = 40
}

/** 构造节点渲染数据 */
function buildNodeRender(n: any, depth?: number) {
  return {
    nodeType: n.nodeType,
    nodeId: n.nodeId,
    label: nodeLabelMap[n.nodeType] || n.nodeType,
    icon: nodeIcon(n.nodeType),
    title: n.nodeDesc || `${n.nodeType} #${n.nodeId}`,
    itemName: n.itemName,
    batchCode: n.batchCode,
    quantity: n.quantity,
    unit: n.unitName,
    event: n.traceType ? getTypeLabel(n.traceType) : '',
    cycle: n.cycle,
    highlight: false,
    depth: depth ?? n.depth ?? 0,
    childCount: n.children?.length || 0
  }
}

/** 节点颜色（原始 hex 值，用于 SVG text fill） */
function nodeColorRaw(node: any): string {
  const m: Record<string, string> = {
    PUR_ORDER: '#909399', VENDOR: '#909399', MATERIAL_STOCK: '#409EFF',
    BATCH: '#409EFF', CARD: '#E6A23C', FEEDBACK: '#67C23A',
    WORKORDER: '#E6A23C', SALES_OUT: '#F56C6C'
  }
  return m[node.nodeType] || '#409EFF'
}

function truncate(s: string, max: number): string {
  if (!s) return ''
  return s.length > max ? s.slice(0, max) + '…' : s
}

/** 节点点击：弹出详情 */
function onNodeClick(node: any) {
  selectedNode.value = node
  nodeDetailOpen.value = true
}

/** 从详情弹窗「以此为起点追溯」：以当前节点为新起点，支持正向/逆向 */
function reTraceFromNode(direction: 'forward' | 'backward') {
  if (!selectedNode.value?.nodeType || !selectedNode.value?.nodeId) return
  chain.nodeType = selectedNode.value.nodeType
  chain.nodeId = String(selectedNode.value.nodeId)
  chain.direction = direction
  nodeDetailOpen.value = false
  handleTrace()
}

/** 滚轮缩放 */
function onWheel(e: WheelEvent) {
  e.preventDefault()
  const delta = e.deltaY > 0 ? -0.1 : 0.1
  dagZoom(delta)
}

function dagZoom(delta: number) {
  const next = dagScale.value + delta
  dagScale.value = Math.max(0.3, Math.min(2.5, next))
}

function dagReset() {
  dagScale.value = 1
  dagOffsetX.value = 40
  dagOffsetY.value = 40
}

/** 拖拽平移 */
function onDragStart(e: MouseEvent) {
  dagDragging = true
  dagDragStartX = e.clientX
  dagDragStartY = e.clientY
  dagOffsetStartX = dagOffsetX.value
  dagOffsetStartY = dagOffsetY.value
}
function onDragMove(e: MouseEvent) {
  if (!dagDragging) return
  dagOffsetX.value = dagOffsetStartX + (e.clientX - dagDragStartX)
  dagOffsetY.value = dagOffsetStartY + (e.clientY - dagDragStartY)
}
function onDragEnd() {
  dagDragging = false
}

function endedReasonText(reason: string): string {
  const map: Record<string, string> = {
    END: '（已到链路末端）',
    NOT_FOUND: '（未找到追溯记录）',
    LOOP: '（检测到循环，已停止）',
    MAX_DEPTH: '（已达最大深度，链路可能被截断）'
  }
  return map[reason] || ''
}

const chainAlertType = computed<string>(() => {
  const r = chainEndedReason.value
  if (r === 'LOOP' || r === 'MAX_DEPTH') return 'warning'
  return 'success'
})

function resetChain() {
  chain.nodeId = ''
  chainSearched.value = false
  chainNodes.value = []
  chainSummary.value = ''
  chainEndedReason.value = ''
  chainTree.value = null
  dagNodes.value = []
  dagEdges.value = []
}
</script>

<style scoped lang="scss">
:deep(.el-form-item__label) { padding-right: 16px !important; }
.mb8 { margin-bottom: 8px; }

.chain-search-card {
  margin-bottom: 16px;
  :deep(.el-card__body) { padding: 16px; }
}

.chain-result { margin-top: 8px; }

.node-icon {
  font-size: 18px;
  margin-right: 8px;
}

/* 节点卡片左侧色条（按节点类型区分） */
.card-MATERIAL_STOCK, .card-BATCH { border-left-color: #409EFF; }
.card-CARD, .card-WORKORDER { border-left-color: #E6A23C; }
.card-FEEDBACK { border-left-color: #67C23A; }
.card-SALES_OUT { border-left-color: #F56C6C; }
.card-PUR_ORDER, .card-VENDOR { border-left-color: #909399; }

/* ───── 树形列表视图 ───── */
.tree-list {
  padding: 12px 4px;
}

.tree-node-row {
  position: relative;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.15s;
  &:hover {
    transform: translateX(2px);
  }
}

.tree-indent-line {
  position: absolute;
  left: -16px;
  top: 0;
  bottom: 0;
  width: 1px;
  background: #DCDFE6;
  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 20px;
    width: 14px;
    height: 1px;
    background: #DCDFE6;
  }
}

.tree-node-card {
  border: 1px solid #EBEEF5;
  border-left: 4px solid #409EFF;
  border-radius: 6px;
  padding: 8px 14px;
  background: #FFFFFF;
  transition: box-shadow 0.2s;
  &:hover {
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  }
}

.tree-node-row.tree-node-cycle .tree-node-card {
  border-left-color: #F56C6C;
  border-style: dashed;
}

.tree-node-row.tree-node-root .tree-node-card {
  background: linear-gradient(90deg, #F0F9EB 0%, #FFFFFF 100%);
  border-left-color: #67C23A;
}

.tree-node-head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.tree-node-title {
  font-weight: 600;
  color: #303133;
  font-size: 14px;
}

.root-tag {
  margin-left: auto;
}

.tree-node-body {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 18px;
  margin-top: 6px;
  font-size: 12px;
  color: #606266;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.meta-key {
  color: #909399;
  &::after { content: '：'; }
}

/* ───── 流向图（SVG DAG） ───── */
.dag-container {
  border: 1px solid #EBEEF5;
  border-radius: 6px;
  background: #FAFAFA;
  overflow: hidden;
}

.dag-toolbar {
  padding: 8px 12px;
  background: #F5F7FA;
  border-bottom: 1px solid #EBEEF5;
  display: flex;
  align-items: center;
}

.dag-viewport {
  width: 100%;
  height: 560px;
  overflow: hidden;
  cursor: grab;
  background:
    radial-gradient(circle, #E0E0E0 1px, transparent 1px) 0 0 / 20px 20px,
    #FAFAFA;
  &:active {
    cursor: grabbing;
  }
}

.dag-node {
  cursor: pointer;
  transition: opacity 0.2s;
  &:hover {
    opacity: 0.85;
  }
}

.dag-node-cycle text {
  fill: #F56C6C;
}

.edge-highlight {
  stroke-dasharray: none;
  animation: dash 1s linear infinite;
}

@keyframes dash {
  to { stroke-dashoffset: -10; }
}
</style>
