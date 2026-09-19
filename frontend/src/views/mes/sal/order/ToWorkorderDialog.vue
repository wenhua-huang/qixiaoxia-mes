<template>
  <!-- 生成工单(2步向导,对齐直接建工单) -->
  <el-dialog title="销售订单转工单" v-model="dialogVisible" width="920px" append-to-body :close-on-click-modal="false">
    <!-- 选销售订单行 -->
    <el-table :data="twLines" size="small" @row-click="twSelectLine" highlight-current-row v-if="twStep===1" :row-class-name="twRowClass">
      <el-table-column label="选" width="50" align="center"><template #default="s"><el-radio v-model="twForm.lineId" :value="s.row.lineId" :disabled="(s.row.quantityConvertible||0)<=0">{{ '' }}</el-radio></template></el-table-column>
      <el-table-column label="产品" align="center" prop="productName" :show-overflow-tooltip="true" />
      <el-table-column label="订单数" align="center" prop="quantity" width="90" />
      <el-table-column label="已转" align="center" prop="quantityProduced" width="90" />
      <el-table-column label="可转" align="center" prop="quantityConvertible" width="90">
        <template #default="s"><span :style="{color: (s.row.quantityConvertible||0)<=0?'#f56c6c':''}">{{ s.row.quantityConvertible || 0 }}</span></template>
      </el-table-column>
    </el-table>
    <el-alert v-if="twStep===1 && twLines.length>0 && !twLines.some(l=>(l.quantityConvertible||0)>0)" title="所有明细行已全部转工单，无可转数量" type="warning" :closable="false" show-icon style="margin-bottom:12px" />

    <!-- Step1: 工单基本信息 -->
    <el-form ref="twFormRef" :model="twForm" label-width="100px" v-show="twStep===1 && twForm.lineId" style="margin-top:12px">
      <el-row>
        <el-col :span="16"><el-form-item label="工单编码"><el-input v-model="twForm.workorderCode" :disabled="twAutoGenFlag" placeholder="自动生成" /><el-switch v-if="!twForm.workorderCode" v-model="twAutoGenFlag" size="small" @change="twAutoGen" /><span v-if="!twForm.workorderCode" style="font-size:12px;color:#13ce66;margin-left:6px">自动生成</span></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="工单类型"><span style="line-height:32px;color:#909399">自制</span></el-form-item></el-col>
      </el-row>
      <el-row>
        <el-col :span="12"><el-form-item label="工单名称"><el-input v-model="twForm.workorderName" placeholder="留空则自动生成" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="来源销售订单"><el-input :model-value="twOrderCode" disabled /></el-form-item></el-col>
      </el-row>
      <el-row>
        <el-col :span="12"><el-form-item label="产品"><el-input :model-value="twProductName" disabled /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="工艺路线">
          <el-select v-model="twForm.routeProductId" placeholder="可选,留空则无BOM/参数" clearable style="width:100%" @change="onTwRouteChange">
            <el-option v-for="r in twRouteOptions" :key="r.recordId" :label="(r._routeName||'路线#'+r.routeId)+' — '+r.itemName" :value="r.recordId" />
          </el-select>
        </el-form-item></el-col>
      </el-row>
      <el-row>
        <el-col :span="12"><el-form-item label="计划数量"><el-input-number v-model="twForm.quantity" :min="1" style="width:100%" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="需求日期"><el-date-picker v-model="twForm.requestDate" type="date" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" /></el-form-item></el-col>
      </el-row>
      <el-row>
        <el-col :span="12"><el-form-item label="客户PO号"><el-input v-model="twForm.clientOrderCode" placeholder="客户PO号" /></el-form-item></el-col>
      </el-row>
      <el-row>
        <el-col :span="12"><el-form-item label="产品尺寸"><el-input v-model="twForm.productSize" placeholder="如254*127*330mm" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="绳料规格"><el-input v-model="twForm.ropeSpec" placeholder="纸袋专用" /></el-form-item></el-col>
      </el-row>
      <el-row><el-col :span="24"><el-form-item label="印刷要求"><el-input v-model="twForm.printingReq" placeholder="如1色满版黑印刷" /></el-form-item></el-col></el-row>
      <el-row><el-col :span="24"><el-form-item label="包装要求"><el-input v-model="twForm.packageReq" placeholder="如250个/箱,贴唛头" /></el-form-item></el-col></el-row>
      <el-row><el-col :span="24"><el-form-item label="备注"><el-input v-model="twForm.remark" type="textarea" :rows="2" /></el-form-item></el-col></el-row>
    </el-form>

    <!-- Step2: BOM/参数编辑 -->
    <div v-show="twStep===2">
      <el-alert title="从工艺路线加载，可按工单调整物料或参数" type="info" :closable="false" show-icon style="margin-bottom:12px" />
      <el-empty v-if="twProcessGroupList.length===0" description="暂无工序数据，请先选工艺路线" :image-size="80" />
      <el-collapse v-if="twProcessGroupList.length>0" v-model="twActiveProcesses">
        <el-collapse-item v-for="proc in twProcessGroupList" :key="proc._key" :name="proc._key">
          <template #title>
            <span style="font-weight:bold;font-size:14px">{{ proc.processName || '未分配工序' }}</span>
            <el-tag size="small" style="margin-left:10px">{{ proc.bomItems.length }} 物料</el-tag>
            <el-tag size="small" type="warning" style="margin-left:4px">{{ proc.paramItems.length }} 参数</el-tag>
          </template>
          <!-- BOM -->
          <el-divider content-position="left">BOM 物料清单</el-divider>
          <el-row class="mb8"><el-col :span="1.5"><el-button type="primary" plain icon="Plus" size="small" @click="handleTwAddBom(proc.processId)">新增物料行</el-button></el-col></el-row>
          <el-table :data="proc.bomItems" size="small" v-if="proc.bomItems.length>0">
            <el-table-column label="物料编码" align="center" prop="itemCode" width="120" />
            <el-table-column label="物料名称" align="center" prop="itemName" :show-overflow-tooltip="true" />
            <el-table-column label="单位用量" align="center" prop="quantity" width="100"><template #default="s"><el-input-number v-model="s.row.quantity" :min="0" :precision="4" size="small" controls-position="right" style="width:90px" /></template></el-table-column>
            <el-table-column label="预计总用量" align="center" width="110"><template #default="s">{{ (s.row.quantity * (twForm.quantity||1)).toFixed(2) }}</template></el-table-column>
            <el-table-column label="操作" align="center" width="80"><template #default="s"><el-button link type="primary" icon="Edit" size="small" @click="handleTwEditBom(s.row)"></el-button><el-button link type="primary" icon="Delete" size="small" @click="handleTwDelBom(s.row)"></el-button></template></el-table-column>
          </el-table>
          <el-empty v-else description="暂无物料" :image-size="40" />
          <!-- 参数 -->
          <el-divider content-position="left">工序参数</el-divider>
          <el-table :data="proc.paramItems" size="small" v-if="proc.paramItems.length>0">
            <el-table-column label="参数名称" align="center" prop="_paramName" width="120" />
            <el-table-column label="标准值" align="center" prop="standardValue" width="120" />
            <el-table-column label="调整值" align="center" width="120"><template #default="s"><el-input v-model="s.row.adjustedValue" size="small" /></template></el-table-column>
            <el-table-column label="备注" align="center" min-width="120"><template #default="s"><el-input v-model="s.row.remark" size="small" placeholder="调整原因" /></template></el-table-column>
          </el-table>
          <el-empty v-else description="暂无参数" :image-size="40" />
        </el-collapse-item>
      </el-collapse>
    </div>

    <template #footer>
      <el-button @click="twStep>1?twStep--:twCancel()" v-if="twForm.lineId">{{ twStep===1?'取消':'上一步' }}</el-button>
      <el-button type="primary" @click="twNext" v-if="twStep===1" :disabled="!twForm.quantity">下一步</el-button>
      <el-button type="primary" @click="handleTwCheck" v-if="twStep===2">确 定</el-button>
    </template>
  </el-dialog>

  <!-- 转工单SKU变体弹窗 -->
  <el-dialog title="检测到 BOM/参数偏离标准" v-model="twSkuDialogOpen" width="750px" append-to-body :close-on-click-modal="false">
    <el-alert type="warning" :closable="false" show-icon style="margin-bottom:16px"><template #title>以下物料或工序参数与标准不一致，可能产生新产品变体</template></el-alert>
    <el-table :data="twDeviationList" size="small" max-height="220">
      <el-table-column label="来源" align="center" width="65"><template #default="s"><el-tag size="small" :type="s.row.source==='BOM'?'':'info'">{{ s.row.source }}</el-tag></template></el-table-column>
      <el-table-column label="名称" align="center" prop="itemName" min-width="120" />
      <el-table-column label="标准值" align="center" width="90"><template #default="s">{{ s.row.standardVal }}</template></el-table-column>
      <el-table-column label="实际值" align="center" width="90"><template #default="s">{{ s.row.actualVal }}</template></el-table-column>
      <el-table-column label="差异" align="center" width="80"><template #default="s"><el-tag size="small" :type="s.row.diffType">{{ s.row.diffLabel }}</el-tag></template></el-table-column>
    </el-table>
    <p style="margin-top:16px;color:#606266">当前产品：<strong>{{ twProductName }}</strong></p>
    <p style="color:#909399;font-size:13px">是否将此定制规格保存为新产品变体（SKU）？</p>
    <el-radio-group v-model="twSkuChoice" style="margin:12px 0;display:flex;flex-direction:column;gap:8px">
      <el-radio value="yes"><span style="font-weight:bold">是，创建新变体</span><span style="color:#909399;font-size:12px;margin-left:8px">下次返单可直接选此变体</span></el-radio>
      <el-radio value="no"><span style="font-weight:bold">否，沿用原产品</span><span style="color:#909399;font-size:12px;margin-left:8px">本次仅做微调，不创建新品种</span></el-radio>
    </el-radio-group>
    <el-row :gutter="12" v-if="twSkuChoice==='yes'" style="margin-top:12px">
      <el-col :span="12"><el-form-item label="变体编码"><el-input v-model="twForm.skuCode" placeholder="自动生成" /><el-switch v-if="!twForm.skuCode" size="small" @change="twGenSkuCode" /><span v-if="!twForm.skuCode" style="font-size:12px;color:#13ce66;margin-left:6px">自动生成</span></el-form-item></el-col>
      <el-col :span="12"><el-form-item label="变体名称"><el-input v-model="twForm.skuName" placeholder="如 奔趣纸袋-彩印红绳" /></el-form-item></el-col>
    </el-row>
    <template #footer><el-button type="primary" @click="twConfirmSku" :disabled="twSkuChoice==='yes' && (!twForm.skuCode || !twForm.skuName)">确认，继续</el-button><el-button @click="twSkuDialogOpen=false">取消</el-button></template>
  </el-dialog>

  <!-- 转工单BOM编辑弹窗 -->
  <el-dialog :title="'编辑物料行'" v-model="twBomEditOpen" width="500px" append-to-body>
    <el-form :model="twBomEditForm" label-width="100px">
      <el-form-item label="所属工序"><el-select v-model="twBomEditForm._processId" style="width:100%" placeholder="请选择"><el-option v-for="p in twRouteProcesses" :key="p.processId" :label="p.processName" :value="p.processId" /></el-select></el-form-item>
      <el-form-item label="物料"><el-row :gutter="8"><el-col :span="18"><el-input v-model="twBomEditForm.itemName" placeholder="请选择物料" disabled /></el-col><el-col :span="6"><el-button type="primary" icon="Search" size="small" @click="$refs.twBomItemSelectRef?.open()">选择</el-button></el-col></el-row></el-form-item>
      <el-form-item label="物料类型"><el-select v-model="twBomEditForm.itemOrProduct" style="width:100%"><el-option label="原料" value="RAW" /><el-option label="半成品" value="SEMI" /><el-option label="成品" value="FINISHED" /><el-option label="辅料" value="AUXILIARY" /><el-option label="包材" value="PACK" /></el-select></el-form-item>
      <el-form-item label="单位用量"><el-input-number v-model="twBomEditForm.quantity" :min="0" :precision="4" style="width:100%" /></el-form-item>
    </el-form>
    <template #footer><el-button type="primary" @click="twConfirmBomEdit">确 定</el-button><el-button @click="twBomEditOpen=false">取 消</el-button></template>
  </el-dialog>
  <ItemSelect ref="twBomItemSelectRef" @onSelected="onTwBomItemSelected" />
</template>

<script>
import { getOrderDetail, toWorkorder } from '@/api/mes/sal/order'
import { genSerialCode } from '@/api/mes/sys/autocoderule'
import { listRouteProduct } from '@/api/mes/pro/routeproduct'
import { listRoute } from '@/api/mes/pro/proroute'
import { listRouteProcessByRouteId } from '@/api/mes/pro/routeprocess'
import { listRouteProductBomByRouteId } from '@/api/mes/pro/routeproductbom'
import { listRouteProcessParamByRouteProductId } from '@/api/mes/pro/routeprocessparam'
import { listParamTemplate } from '@/api/mes/pro/paramtemplate'
import { checkDeviation } from '@/api/mes/pro/workorder'

function emptyTwForm(requestDate) {
  return { lineId: null, quantity: undefined, workorderCode: '', workorderName: '', requestDate: requestDate || null, routeProductId: null, clientOrderCode: '', productSize: '', ropeSpec: '', printingReq: '', packageReq: '', createSkuVariant: false, skuCode: '', skuName: '', remark: '' }
}

export default {
  name: 'SalOrderToWorkorderDialog',
  props: {
    modelValue: { type: Boolean, default: false },
    order: { type: Object, default: () => ({}) }
  },
  emits: ['update:modelValue', 'created'],
  data() {
    return {
      twLines: [], twOrderCode: '', twAutoGenFlag: true,
      twStep: 1, twRouteOptions: [], twProductId: null, twProductCode: '', twProductName: '',
      twRouteProcesses: [], twActiveProcesses: [],
      twBomList: [], twParamList: [],
      twSkuDialogOpen: false, twSkuChoice: '', twDeviationList: [],
      twBomEditOpen: false, twBomEditIdx: -1, twBomEditForm: {},
      // 异步请求序号: 切换行/路线或重开向导时丢弃过期响应, 防止旧行 BOM/参数污染
      twReqSeq: 0,
      twForm: emptyTwForm(null)
    }
  },
  computed: {
    dialogVisible: {
      get() { return this.modelValue },
      set(v) { this.$emit('update:modelValue', v) }
    },
    twProcessGroupList() {
      const procs = this.twRouteProcesses
      return procs.map(p => ({
        _key: 'twp_' + p.processId,
        processId: p.processId, processName: p.processName,
        bomItems: this.twBomList.filter(b => (b.processId || b._processId) === p.processId),
        paramItems: this.twParamList.filter(pp => (pp.processId || pp._processId) === p.processId)
      })).concat(
        // 未分配工序的物料
        this.twBomList.filter(b => !(b.processId || b._processId) || !procs.find(p => p.processId === (b.processId || b._processId))).length > 0
        ? [{ _key: 'twp_unassigned', processId: null, processName: '未分配工序', bomItems: this.twBomList.filter(b => !(b.processId || b._processId) || !procs.find(p => p.processId === (b.processId || b._processId))), paramItems: [] }]
        : []
      )
    }
  },
  watch: {
    modelValue(v) { if (v) this.initWizard() }
  },
  methods: {
    /** 打开向导：重置状态 + 拉订单可转行 */
    initWizard() {
      const seq = ++this.twReqSeq
      const row = this.order || {}
      this.twOrderCode = row.orderCode || ''
      this.twStep = 1; this.twAutoGenFlag = true
      this.twLines = []; this.twRouteOptions = []; this.twProductId = null; this.twProductCode = ''; this.twProductName = ''
      this.twBomList = []; this.twParamList = []; this.twRouteProcesses = []; this.twActiveProcesses = []
      this.twSkuDialogOpen = false; this.twSkuChoice = ''; this.twDeviationList = []
      this.twBomEditOpen = false; this.twBomEditIdx = -1; this.twBomEditForm = {}
      this.twForm = emptyTwForm(row.requestDate ? row.requestDate + ' 00:00:00' : null)
      if (row.orderId) getOrderDetail(row.orderId).then(r => {
        if (seq !== this.twReqSeq) return
        this.twLines = r.data.lines || []
      }).catch(() => {})
      this.twAutoGen()
    },
    twCancel() { this.dialogVisible = false; this.twStep = 1 },
    twNext() {
      if (!this.twForm.quantity) { this.$modal.msgWarning('请输入计划数量'); return }
      this.twStep = 2
    },
    twSelectLine(line) {
      const seq = ++this.twReqSeq
      this.twForm.lineId = line.lineId; this.twForm.quantity = line.quantityConvertible
      this.twForm.workorderName = (line.productName || '') + '-' + this.twOrderCode
      // 默认沿用开单带出的路线, 可改选; 置空不影响 BOM/参数装载
      this.twForm.routeProductId = line.routeProductId || null
      this.twForm.createSkuVariant = false; this.twForm.skuCode = ''; this.twForm.skuName = ''
      this.twForm.productSize = line.productSize || ''; this.twForm.ropeSpec = line.ropeSpec || ''; this.twForm.printingReq = line.printingReq || ''; this.twForm.packageReq = line.packageReq || ''; this.twForm.clientOrderCode = line.clientOrderCode || ''
      this.twBomList = []; this.twParamList = []; this.twRouteProcesses = []
      this.twProductId = line.productId; this.twProductCode = line.productCode || ''; this.twProductName = line.productName || ''
      this.twRouteOptions = []
      if (line.productId) {
        listRouteProduct({ itemId: line.productId, pageSize: 100 }).then(r => {
          if (seq !== this.twReqSeq || this.twForm.lineId !== line.lineId) return
          if (r.rows && r.rows.length > 0) {
            const rows = r.rows
            listRoute({ pageSize: 1000 }).then(routeRes => {
              if (seq !== this.twReqSeq || this.twForm.lineId !== line.lineId) return
              const routeMap = {}; (routeRes.rows || []).forEach(rt => { routeMap[rt.routeId] = rt.routeName || rt.routeCode || '路线#' + rt.routeId })
              this.twRouteOptions = rows.map(rp => ({ ...rp, _routeName: routeMap[rp.routeId] || '路线#' + rp.routeId }))
              // 默认路线存在则主动装载其工序/BOM/参数
              if (this.twForm.routeProductId) this.$nextTick(() => this.onTwRouteChange(this.twForm.routeProductId))
            })
          }
        })
      }
    },
    onTwRouteChange(recordId) {
      const seq = ++this.twReqSeq
      const rp = this.twRouteOptions.find(r => r.recordId === recordId)
      if (!rp) { this.twBomList = []; this.twParamList = []; this.twRouteProcesses = []; return }
      listRouteProcessByRouteId(rp.routeId).then(res => {
        if (seq !== this.twReqSeq || this.twForm.routeProductId !== recordId) return
        this.twRouteProcesses = res.data || []
        const routePids = new Set(this.twRouteProcesses.map(p => p.processId))
        listRouteProductBomByRouteId(rp.routeId).then(r => {
          if (seq !== this.twReqSeq || this.twForm.routeProductId !== recordId) return
          this.twBomList = (r.data || []).filter(b => this.twProductId === b.productId && routePids.has(b.processId))
                .map(b => ({ ...b, _processId: b.processId, _processName: this.getTwProcessName(b.processId) }))
        })
        listRouteProcessParamByRouteProductId(recordId).then(r => {
          if (seq !== this.twReqSeq || this.twForm.routeProductId !== recordId) return
          const l2Params = r.data || []
          listParamTemplate({ pageSize: 1000 }).then(tmplRes => {
            if (seq !== this.twReqSeq || this.twForm.routeProductId !== recordId) return
            const allTemplates = tmplRes.rows || []
            const routeTemplates = allTemplates.filter(t => routePids.has(t.processId))
            const templateMap = {}; allTemplates.forEach(t => { templateMap[t.templateId] = t })
            this.twParamList = (l2Params.length > 0)
              ? l2Params.map(p => { const tmpl = templateMap[p.templateId] || {}; return { ...p, _paramName: tmpl.paramName || '', _processId: p.processId, standardValue: p.paramValue || tmpl.defaultValue || '' } })
              : routeTemplates.map(t => ({ templateId: t.templateId, processId: t.processId, _processId: t.processId, routeProductId: recordId, _paramName: t.paramName || '', standardValue: t.defaultValue || '', adjustedValue: '' }))
          })
        })
      })
    },
    getTwProcessName(pid) { const p = this.twRouteProcesses.find(x => x.processId === pid); return p ? p.processName : '' },
    // BOM 编辑
    handleTwAddBom(processId) { this.twBomEditIdx = -1; const proc = this.twRouteProcesses.find(p => p.processId === processId); this.twBomEditForm = { _processId: processId || null, itemId: null, itemCode: null, itemName: null, itemOrProduct: 'RAW', quantity: 0, unitOfMeasure: null, unitName: null }; this.twBomEditOpen = true },
    handleTwEditBom(row) { this.twBomEditIdx = this.twBomList.indexOf(row); this.twBomEditForm = { ...row }; this.twBomEditOpen = true },
    handleTwDelBom(row) { const idx = this.twBomList.indexOf(row); if (idx >= 0) this.twBomList.splice(idx, 1) },
    onTwBomItemSelected(row) { this.twBomEditForm.itemId = row.itemId; this.twBomEditForm.itemCode = row.itemCode; this.twBomEditForm.itemName = row.itemName; this.twBomEditForm.unitOfMeasure = row.unitOfMeasure; this.twBomEditForm.unitName = row.unitName },
    twConfirmBomEdit() {
      const f = this.twBomEditForm; f._processName = this.getTwProcessName(f._processId)
      if (this.twBomEditIdx >= 0) this.twBomList.splice(this.twBomEditIdx, 1, f); else this.twBomList.push({ ...f })
      this.twBomEditOpen = false
    },
    // SKU
    twGenSkuCode(v) { if (v === false) { this.twForm.skuCode = ''; return } genSerialCode('SKU_CODE', this.twProductCode + '-V').then(r => { this.twForm.skuCode = r.data || '' }) },
    twAutoGen(v) { if (v === false) { this.twForm.workorderCode = ''; return } genSerialCode('WORKORDER_NO').then(r => { this.twForm.workorderCode = r.data }) },
    twConfirmSku() {
      this.twForm.createSkuVariant = this.twSkuChoice === 'yes'
      this.twSkuDialogOpen = false; this.doTwSubmit()
    },
    // 提交(含偏离检测)
    handleTwCheck() {
      const payload = this.buildTwPayload()
      if (!this.twForm.routeProductId) { this.doTwSubmit(); return }
      checkDeviation(payload).then(res => {
        const result = res.data
        if (result.hasDeviation && result.deviations && result.deviations.length > 0) {
          this.twDeviationList = result.deviations; this.twSkuChoice = ''
          genSerialCode('SKU_CODE', this.twProductCode + '-V').then(r => { this.twForm.skuCode = (r.data || '').trim() })
          this.twForm.skuName = ''; this.twSkuDialogOpen = true
        } else { this.doTwSubmit() }
      }).catch(() => { this.doTwSubmit() })
    },
    buildTwPayload() {
      return {
        workorder: { ...this.twForm, routeProductId: this.twForm.routeProductId, createSkuVariant: this.twForm.createSkuVariant, skuCode: this.twForm.skuCode, skuName: this.twForm.skuName },
        bomList: this.twBomList.map(b => ({ processId: b.processId || b._processId || null, processName: b.processName || b._processName || '', itemId: b.itemId, itemCode: b.itemCode || '', itemName: b.itemName || '', itemOrProduct: b.itemOrProduct || 'RAW', quantity: b.quantity || 0, unitOfMeasure: b.unitOfMeasure || '', unitName: b.unitName || '' })),
        paramList: this.twParamList.map(p => ({ recordId: p.recordId || null, routeProductId: p.routeProductId || null, templateId: p.templateId || null, standardValue: p.standardValue || '', adjustedValue: p.adjustedValue || '', remark: p.remark || '' }))
      }
    },
    doTwSubmit() {
      const payload = this.buildTwPayload()
      const req = { ...this.twForm, bomList: payload.bomList, paramList: payload.paramList }
      toWorkorder(req).then(r => {
        this.$modal.msgSuccess('已生成工单: ' + r.data.workorderCode)
        this.dialogVisible = false; this.twStep = 1
        this.$emit('created')
      })
    },
    twRowClass({ row }) { return (row.quantityConvertible || 0) <= 0 ? 'tw-row-exhausted' : '' }
  }
}
</script>
