<template>
  <div>
    <!-- 手动查询区 -->
    <el-card shadow="never" class="chain-search-card">
      <el-row :gutter="16" align="middle">
        <el-col :span="5">
          <el-form-item label="节点类型" label-width="80px">
            <el-select v-model="chain.nodeType" placeholder="选择起点类型" style="width:100%">
              <el-option v-for="d in nodeTypeDict" :key="d.value" :label="d.label" :value="d.value" />
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
        <span v-if="chainViewMode === 'graph'" style="font-size:12px;color:#909399">滚轮缩放 · 拖拽平移 · 点击节点查看详情</span>
        <span v-else style="font-size:12px;color:#909399">点击节点查看详情 · 按深度缩进展示分叉</span>
      </div>

      <!-- ───── 流向图视图（SVG DAG） ───── -->
      <div v-if="chainViewMode === 'graph' && chainTree" class="dag-container">
        <div class="dag-toolbar">
          <el-button-group>
            <el-button size="small" icon="ZoomIn" @click="dagZoom(0.2)">放大</el-button>
            <el-button size="small" icon="ZoomOut" @click="dagZoom(-0.2)">缩小</el-button>
            <el-button size="small" icon="Refresh" @click="dagReset">复位</el-button>
          </el-button-group>
          <span style="margin-left:12px;font-size:12px;color:#909399">缩放 {{ Math.round(dagScale * 100) }}% · 共 {{ dagNodes.length }} 个节点</span>
        </div>
        <div class="dag-viewport" @wheel="onWheel" @mousedown="onDragStart" @mousemove="onDragMove" @mouseup="onDragEnd" @mouseleave="onDragEnd">
          <svg :width="dagWidth" :height="dagHeight" :style="{ transform: `translate(${dagOffsetX}px, ${dagOffsetY}px) scale(${dagScale})`, transformOrigin: '0 0' }">
            <defs>
              <marker id="arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
                <path d="M0,0 L10,5 L0,10 z" fill="#909399" />
              </marker>
            </defs>
            <g class="dag-edges">
              <path v-for="(edge, i) in dagEdges" :key="'e' + i" :d="edge.path" stroke="#C0C4CC" stroke-width="1.5" fill="none" marker-end="url(#arrow)" />
              <g v-for="(edge, i) in dagEdges" :key="'el' + i">
                <rect :x="edge.labelX - 28" :y="edge.labelY - 9" width="56" height="18" rx="9" fill="#F4F4F5" stroke="#DCDFE6" stroke-width="0.5" />
                <text :x="edge.labelX" :y="edge.labelY + 4" text-anchor="middle" font-size="10" fill="#606266">{{ edge.quantity }}{{ edge.unit || '' }}</text>
              </g>
            </g>
            <g v-for="(node, i) in dagNodes" :key="'n' + i" :transform="`translate(${node.x}, ${node.y})`" class="dag-node" :class="{ 'dag-node-cycle': node.cycle }" @click="onNodeClick(node)">
              <rect x="0" y="0" width="4" :height="node.height" :fill="nodeColor(node)" rx="2" />
              <rect x="4" y="0" :width="node.width - 4" :height="node.height" rx="6" fill="#FFFFFF" :stroke="node.cycle ? '#F56C6C' : '#DCDFE6'" :stroke-width="node.cycle ? 2 : 1" :stroke-dasharray="node.cycle ? '4 2' : 'none'" />
              <text x="16" y="22" font-size="16">{{ nodeIcon(node.nodeType) }}</text>
              <text x="38" y="18" font-size="11" font-weight="600" :fill="nodeColor(node)">{{ node.label }}</text>
              <text x="38" y="34" font-size="11" fill="#303133">{{ truncate(node.title, 16) }}</text>
              <text x="38" y="50" font-size="10" fill="#909399">
                <tspan v-if="node.quantity">{{ node.quantity }}{{ node.unit || '' }}</tspan>
                <tspan v-if="node.event"> · {{ node.event }}</tspan>
                <tspan v-if="node.cycle"> · 🔁 循环</tspan>
              </text>
            </g>
          </svg>
        </div>
      </div>

      <!-- ───── 树形列表视图 ───── -->
      <div v-if="chainViewMode === 'list'" class="tree-list">
        <div
          v-for="(node, idx) in chainNodes"
          :key="idx"
          class="tree-node-row"
          :class="{ 'tree-node-cycle': node.cycle, 'tree-node-root': node.depth === 0 }"
          :style="{ marginLeft: node.depth * 28 + 'px' }"
          @click="onTreeNodeClick(node)"
        >
          <span class="tree-indent-line" v-if="node.depth > 0"></span>
          <div class="tree-node-card" :class="`card-${node.type}`">
            <div class="tree-node-head">
              <span class="node-icon">{{ nodeIcon(node.type) }}</span>
              <el-tag :type="nodeTagType(node.type)" size="small" effect="dark">{{ node.label }}</el-tag>
              <span class="tree-node-title">{{ node.title }}</span>
              <el-tag v-if="node.depth === 0" type="info" size="small" effect="plain" class="root-tag">起点</el-tag>
              <el-tag v-if="node.cycle" type="danger" size="small" effect="plain">🔁 循环</el-tag>
              <el-tag v-if="node.branchCount > 1" type="warning" size="small" effect="plain">分叉 {{ node.branchCount }} 路</el-tag>
            </div>
            <div class="tree-node-body" v-if="hasDetails(node)">
              <span v-if="node.event" class="meta-item"><span class="meta-key">事件</span><dict-tag :options="traceTypeDict" :value="node.traceType" /></span>
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
      <template #image><el-icon :size="60" color="#909399"><Search /></el-icon></template>
    </el-empty>

    <!-- 节点详情弹窗 -->
    <el-dialog :title="`节点详情 · ${selectedNode?.label || ''}`" v-model="nodeDetailOpen" width="520px" append-to-body>
      <el-descriptions v-if="selectedNode" :column="2" border size="small">
        <el-descriptions-item label="节点类型">
          <span class="node-icon">{{ selectedNode.icon }}</span>
          <el-tag :type="nodeTagType(selectedNode.nodeType)" size="small" effect="dark">{{ selectedNode.label }}</el-tag>
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

<script setup lang="ts" name="TraceChainTab">
import { ref, reactive, computed, getCurrentInstance } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { traceChain } from '@/api/mes/pro/materialtrace'

const props = defineProps<{ initialQuery?: { nodeType: string; nodeId: string; direction: 'forward' | 'backward' } | null }>()

const { proxy } = getCurrentInstance() as any
// 字典：追溯事件类型 + 节点类型（禁硬编码 Record<string,string>）
const { mes_material_trace_type: traceTypeDict, mes_material_trace_node_type: nodeTypeDict } = proxy.useDict('mes_material_trace_type', 'mes_material_trace_node_type')

// ── 查询状态 ──
const chain = reactive({ nodeType: 'CARD', nodeId: '', direction: 'backward' as 'forward' | 'backward' })
const chainLoading = ref(false)
const chainSearched = ref(false)
const chainNodes = ref<any[]>([])
const chainSummary = ref('')
const chainEndedReason = ref('')
const chainTree = ref<any>(null)
const chainViewMode = ref<'graph' | 'list'>('graph')
// 节点详情
const nodeDetailOpen = ref(false)
const selectedNode = ref<any>(null)

// ── DAG 渲染状态 ──
const dagNodes = ref<any[]>([])
const dagEdges = ref<any[]>([])
const dagWidth = ref(1200)
const dagHeight = ref(600)
const dagScale = ref(1)
const dagOffsetX = ref(40)
const dagOffsetY = ref(40)
let dragging = false, dragStartX = 0, dragStartY = 0, offStartX = 0, offStartY = 0

// ── 节点标签（来自字典）/ 图标/颜色（纯展示函数，基于 dict value）──
function nodeLabel(type: string): string {
  return nodeTypeDict.value?.find((d: any) => d.value === type)?.label || type
}
function nodeIcon(type: string): string {
  const map: Record<string, string> = { PUR_ORDER: '🛒', VENDOR: '🏭', MATERIAL_STOCK: '📦', BATCH: '🏷️', CARD: '📋', FEEDBACK: '✅', WORKORDER: '🔧', SALES_OUT: '🚚', NONE: '❓' }
  return map[type] || '📌'
}
function nodeColor(node: any): string {
  const map: Record<string, string> = { PUR_ORDER: '#909399', VENDOR: '#909399', MATERIAL_STOCK: '#409EFF', BATCH: '#409EFF', CARD: '#E6A23C', FEEDBACK: '#67C23A', WORKORDER: '#E6A23C', SALES_OUT: '#F56C6C' }
  return map[node.nodeType] || '#409EFF'
}
function nodeTagType(type: string): string {
  const map: Record<string, string> = { PUR_ORDER: 'info', VENDOR: 'info', MATERIAL_STOCK: '', BATCH: '', CARD: 'warning', FEEDBACK: 'success', WORKORDER: 'warning', SALES_OUT: 'danger' }
  return map[type] || 'info'
}
function truncate(s: string, max: number): string { return !s ? '' : (s.length > max ? s.slice(0, max) + '…' : s) }

async function handleTrace() {
  const id = Number(chain.nodeId)
  if (!chain.nodeId || isNaN(id) || id <= 0) { proxy.$modal.msgError('请输入有效的节点ID（正整数）'); return }
  chainLoading.value = true; chainSearched.value = true
  chainNodes.value = []; chainSummary.value = ''; chainTree.value = null; dagNodes.value = []; dagEdges.value = []
  try {
    const startLabel = nodeLabel(chain.nodeType)
    const dirLabel = chain.direction === 'forward' ? '正向' : '反向'
    const res: any = await traceChain(chain.nodeType, id, chain.direction)
    const result = res?.data || {}
    const tree: any = result.tree
    const endedReason: string = result.endedReason || ''
    chainTree.value = tree; chainEndedReason.value = tree ? endedReason : ''
    if (tree) {
      deriveLegacyFromTree(tree); computeDagLayout(tree)
      const totalNodes = countTreeNodes(tree)
      const startDesc = tree.nodeDesc || `${startLabel} #${id}`
      const branchCount = countBranches(tree)
      const branchText = branchCount > 0 ? `，含 ${branchCount} 个分支` : ''
      chainSummary.value = `${dirLabel}追溯：从「${startDesc}」出发，共 ${totalNodes} 个节点${branchText}${endedReasonText(endedReason)}`
    } else {
      chainSummary.value = `${dirLabel}追溯：未找到「${startLabel} #${id}」的追溯记录`
    }
  } catch (e: any) { chainSummary.value = '追溯失败: ' + (e?.message || '未知错误') }
  finally { chainLoading.value = false }
}

/** 完整扁平化树（DFS，带 depth），供树形列表视图用——分叉节点并列展示 */
function deriveLegacyFromTree(tree: any) {
  const nodes: any[] = []
  const walk = (n: any, depth: number) => {
    nodes.push({
      nodeType: n.nodeType, nodeId: n.nodeId, type: n.nodeType, id: n.nodeId,
      label: nodeLabel(n.nodeType), icon: nodeIcon(n.nodeType),
      title: n.nodeDesc || `${n.nodeType} #${n.nodeId}`,
      itemName: n.itemName, batchCode: n.batchCode, quantity: n.quantity, unit: n.unitName,
      event: n.traceType ? (traceTypeDict.value?.find((d: any) => d.value === n.traceType)?.label || n.traceType) : '',
      traceType: n.traceType, cycle: n.cycle, depth, branchCount: n.children?.length || 0, childCount: n.children?.length || 0
    })
    if (n.children && !n.cycle) { for (const c of n.children) walk(c, depth + 1) }
  }
  walk(tree, 0)
  chainNodes.value = nodes
}
function hasDetails(node: any): boolean { return !!(node.event || node.quantity || node.itemName || node.batchCode) }
function onTreeNodeClick(node: any) { selectedNode.value = node; nodeDetailOpen.value = true }

function countTreeNodes(node: any): number { if (!node) return 0; let c = 1; if (node.children) for (const x of node.children) c += countTreeNodes(x); return c }
function countBranches(node: any): number { if (!node || !node.children) return 0; let c = Math.max(0, node.children.length - 1); for (const x of node.children) c += countBranches(x); return c }

// ══ DAG 布局（Reingold-Tilford 横向树）══
const NODE_W = 200, NODE_H = 60, COL_GAP = 80, ROW_GAP = 16
function computeDagLayout(root: any) {
  const nodes: any[] = []; const edges: any[] = []; let leafCursor = 0
  const layout = (n: any, depth: number): { top: number; bottom: number } => {
    const x = depth * (NODE_W + COL_GAP)
    if (!n.children || n.children.length === 0 || n.cycle) {
      const y = leafCursor * (NODE_H + ROW_GAP); leafCursor++
      nodes.push({ ...buildNodeRender(n, depth), x, y, height: NODE_H, width: NODE_W, _ref: n })
      return { top: y, bottom: y + NODE_H }
    }
    const ranges: any[] = []
    for (const c of n.children) ranges.push(layout(c, depth + 1))
    const top = ranges[0].top, bottom = ranges[ranges.length - 1].bottom, y = (top + bottom) / 2 - NODE_H / 2
    nodes.push({ ...buildNodeRender(n, depth), x, y, height: NODE_H, width: NODE_W, _ref: n })
    return { top, bottom }
  }
  layout(root, 0)
  for (const parent of nodes) {
    if (!parent._ref?.children) continue
    for (const childRef of parent._ref.children) {
      const child = nodes.find(n => n._ref === childRef)
      if (!child) continue
      const x1 = parent.x + parent.width, y1 = parent.y + parent.height / 2, x2 = child.x, y2 = child.y + child.height / 2, mx = (x1 + x2) / 2
      edges.push({ path: `M ${x1},${y1} C ${mx},${y1} ${mx},${y2} ${x2},${y2}`, quantity: childRef.quantity, unit: childRef.unitName, labelX: mx, labelY: (y1 + y2) / 2 })
    }
  }
  const maxX = nodes.length ? Math.max(...nodes.map(n => n.x + n.width)) : 800
  const maxY = nodes.length ? Math.max(...nodes.map(n => n.y + n.height)) : 400
  dagWidth.value = maxX + 80; dagHeight.value = maxY + 80
  dagNodes.value = nodes; dagEdges.value = edges
  dagScale.value = 1; dagOffsetX.value = 40; dagOffsetY.value = 40
}
function buildNodeRender(n: any, depth?: number) {
  return {
    nodeType: n.nodeType, nodeId: n.nodeId, label: nodeLabel(n.nodeType), icon: nodeIcon(n.nodeType),
    title: n.nodeDesc || `${n.nodeType} #${n.nodeId}`, itemName: n.itemName, batchCode: n.batchCode,
    quantity: n.quantity, unit: n.unitName, event: n.traceType ? (traceTypeDict.value?.find((d: any) => d.value === n.traceType)?.label || n.traceType) : '',
    cycle: n.cycle, depth: depth ?? n.depth ?? 0, childCount: n.children?.length || 0
  }
}

function onNodeClick(node: any) { selectedNode.value = node; nodeDetailOpen.value = true }
function reTraceFromNode(direction: 'forward' | 'backward') {
  if (!selectedNode.value?.nodeType || !selectedNode.value?.nodeId) return
  chain.nodeType = selectedNode.value.nodeType; chain.nodeId = String(selectedNode.value.nodeId); chain.direction = direction
  nodeDetailOpen.value = false; handleTrace()
}
function onWheel(e: WheelEvent) { e.preventDefault(); dagZoom(e.deltaY > 0 ? -0.1 : 0.1) }
function dagZoom(delta: number) { dagScale.value = Math.max(0.3, Math.min(2.5, dagScale.value + delta)) }
function dagReset() { dagScale.value = 1; dagOffsetX.value = 40; dagOffsetY.value = 40 }
function onDragStart(e: MouseEvent) { dragging = true; dragStartX = e.clientX; dragStartY = e.clientY; offStartX = dagOffsetX.value; offStartY = dagOffsetY.value }
function onDragMove(e: MouseEvent) { if (!dragging) return; dagOffsetX.value = offStartX + (e.clientX - dragStartX); dagOffsetY.value = offStartY + (e.clientY - dragStartY) }
function onDragEnd() { dragging = false }

function endedReasonText(reason: string): string {
  return ({ END: '（已到链路末端）', NOT_FOUND: '（未找到追溯记录）', LOOP: '（检测到循环，已停止）', MAX_DEPTH: '（已达最大深度，链路可能被截断）' } as Record<string, string>)[reason] || ''
}
const chainAlertType = computed<string>(() => (chainEndedReason.value === 'LOOP' || chainEndedReason.value === 'MAX_DEPTH') ? 'warning' : 'success')

function resetChain() {
  chain.nodeId = ''; chainSearched.value = false; chainNodes.value = []; chainSummary.value = ''; chainEndedReason.value = ''; chainTree.value = null; dagNodes.value = []; dagEdges.value = []
}

// 父组件 traceFromRow 触发时，通过 initialQuery prop 预填并自动查询
import { watch } from 'vue'
watch(() => props.initialQuery, (q: any) => {
  if (q) { chain.nodeType = q.nodeType; chain.nodeId = q.nodeId; chain.direction = q.direction; handleTrace() }
}, { immediate: true })
</script>

<style scoped lang="scss">
.chain-search-card { margin-bottom: 16px; :deep(.el-card__body) { padding: 16px; } }
.chain-result { margin-top: 8px; }
.node-icon { font-size: 18px; margin-right: 8px; }

/* 树形列表 */
.tree-list { padding: 12px 4px; }
.tree-node-row { position: relative; margin-bottom: 8px; cursor: pointer; transition: all 0.15s; &:hover { transform: translateX(2px); } }
.tree-indent-line { position: absolute; left: -16px; top: 0; bottom: 0; width: 1px; background: #DCDFE6; &::before { content: ''; position: absolute; left: 0; top: 20px; width: 14px; height: 1px; background: #DCDFE6; } }
.tree-node-card { border: 1px solid #EBEEF5; border-left: 4px solid #409EFF; border-radius: 6px; padding: 8px 14px; background: #FFFFFF; transition: box-shadow 0.2s; &:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.08); } }
.card-MATERIAL_STOCK, .card-BATCH { border-left-color: #409EFF; }
.card-CARD, .card-WORKORDER { border-left-color: #E6A23C; }
.card-FEEDBACK { border-left-color: #67C23A; }
.card-SALES_OUT { border-left-color: #F56C6C; }
.card-PUR_ORDER, .card-VENDOR { border-left-color: #909399; }
.tree-node-row.tree-node-cycle .tree-node-card { border-left-color: #F56C6C; border-style: dashed; }
.tree-node-row.tree-node-root .tree-node-card { background: linear-gradient(90deg, #F0F9EB 0%, #FFFFFF 100%); border-left-color: #67C23A; }
.tree-node-head { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.tree-node-title { font-weight: 600; color: #303133; font-size: 14px; }
.root-tag { margin-left: auto; }
.tree-node-body { display: flex; flex-wrap: wrap; gap: 6px 18px; margin-top: 6px; font-size: 12px; color: #606266; }
.meta-item { display: inline-flex; align-items: center; gap: 4px; }
.meta-key { color: #909399; &::after { content: '：'; } }

/* 流向图 */
.dag-container { border: 1px solid #EBEEF5; border-radius: 6px; background: #FAFAFA; overflow: hidden; }
.dag-toolbar { padding: 8px 12px; background: #F5F7FA; border-bottom: 1px solid #EBEEF5; display: flex; align-items: center; }
.dag-viewport { width: 100%; height: 560px; overflow: hidden; cursor: grab; background: radial-gradient(circle, #E0E0E0 1px, transparent 1px) 0 0 / 20px 20px, #FAFAFA; &:active { cursor: grabbing; } }
.dag-node { cursor: pointer; transition: opacity 0.2s; &:hover { opacity: 0.85; } }
.dag-node-cycle text { fill: #F56C6C; }
</style>
