<template>
  <el-dialog :title="title" v-model="showFlag" width="760px" append-to-body :close-on-click-modal="false" @close="onClose">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="基本信息" name="basic">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
          <el-row>
            <el-col :span="16">
              <el-form-item label="产品" prop="productName">
                <el-input v-model="form.productName" placeholder="请选择产品" readonly>
                  <template #append><el-button icon="Search" @click="itemSelectRef?.open()" /></template>
                </el-input>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="订单数量" prop="quantity">
                <el-input-number v-model="form.quantity" :min="0" :precision="2" style="width:100%" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row>
            <el-col :span="8">
              <el-form-item label="单价" prop="unitPrice">
                <el-input-number v-model="form.unitPrice" :min="0" :precision="4" style="width:100%" @change="calcAmount" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="行金额">
                <el-input-number v-model="form.lineAmount" :min="0" :precision="2" style="width:100%" disabled />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="间距" prop="spacing">
                <el-input v-model="form.spacing" placeholder="如7.5cm" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row>
            <el-col :span="8">
              <el-form-item label="产品尺寸" prop="productSize">
                <el-input v-model="form.productSize" placeholder="如200*100*150mm" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="行交期" prop="requestDate">
                <el-date-picker v-model="form.requestDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row>
            <el-col :span="16">
              <el-form-item label="工艺路线">
                <el-select v-model="form.routeProductId" clearable placeholder="留空则转工单时手选" style="width:100%" @change="onRouteChange">
                  <el-option v-for="r in routeOptions" :key="r.recordId"
                    :label="r._routeName + ' — ' + r.itemName" :value="r.recordId">
                    <span>{{ r._routeName }} — {{ r.itemName }}</span>
                    <el-tag v-if="r.isDefault === 'Y'" size="small" type="success" style="margin-left:6px">默认</el-tag>
                  </el-option>
                </el-select>
                <div v-if="routeBlocked" style="color:#f56c6c;font-size:12px;line-height:1.4">{{ routeBlockMsg }}</div>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row>
            <el-col :span="12">
              <el-form-item label="印刷要求" prop="printingReq">
                <el-input v-model="form.printingReq" placeholder="如1色满版黑印刷" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="绳料规格" prop="ropeSpec">
                <el-input v-model="form.ropeSpec" placeholder="如红色圆纸绳" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row>
            <el-col :span="12">
              <el-form-item label="包装要求" prop="packageReq">
                <el-input v-model="form.packageReq" type="textarea" :rows="2" placeholder="如500个/箱,贴唛头" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="发货要求" prop="shippingReq">
                <el-input v-model="form.shippingReq" type="textarea" :rows="2" placeholder="发货要求" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="备注" prop="remark">
            <el-input v-model="form.remark" type="textarea" :rows="2" />
          </el-form-item>
        </el-form>
      </el-tab-pane>
      <el-tab-pane label="扩展属性" name="extAttr">
        <ExtAttrForm ref="extAttrFormRef" :schema="effAttrSchema" v-model="form.lineAttrs" />
      </el-tab-pane>
    </el-tabs>
    <template #footer>
      <el-button type="primary" @click="confirm">确 定</el-button>
      <el-button @click="showFlag = false">取 消</el-button>
    </template>
    <ItemSelect ref="itemSelectRef" @onSelected="onProductSelected" />
  </el-dialog>
</template>

<script setup lang="ts" name="SalOrderLineEdit">
import ItemSelect from '@/components/itemSelect/single.vue'
import ExtAttrForm from '@/components/ExtAttrForm/index.vue'
import { ElMessage } from 'element-plus'
import { getItem } from '@/api/mes/md/item'
import { getEffAttrSchema } from '@/api/mes/md/attr'
import { listRouteProduct, resolveRouteProduct } from '@/api/mes/pro/routeproduct'
import { listRoute } from '@/api/mes/pro/proroute'
import type { SalOrderLine } from '@/types'
import type { MdItemTypeAttr } from '@/types/api/mes/md/attr'

const props = defineProps<{
  modelValue: boolean
  line: SalOrderLine | null
  orderType?: string
  outsourceFlag?: string
  packageFlag?: string
}>()
const emit = defineEmits<{
  'update:modelValue': [v: boolean]
  confirm: [line: SalOrderLine]
}>()

const showFlag = ref(props.modelValue)
watch(() => props.modelValue, (v: boolean) => { showFlag.value = v; if (v) initForm() })
watch(showFlag, (v: boolean) => emit('update:modelValue', v))

const itemSelectRef = ref()
const extAttrFormRef = ref()
const title = ref('新增明细行')
const activeTab = ref('basic')
const effAttrSchema = ref<MdItemTypeAttr[]>([])
const routeOptions = ref<any[]>([])
const routeBlocked = ref(false)
const routeBlockMsg = ref('')
const form = reactive<SalOrderLine>({})
const rules = {
  productName: [{ required: true, message: '请选择产品', trigger: 'change' }],
  quantity: [{ required: true, message: '订单数量不能为空', trigger: 'blur' }]
}

function initForm() {
  const src = props.line
  title.value = src && src.lineId ? '修改明细行' : '新增明细行'
  activeTab.value = 'basic'
  Object.assign(form, {
    lineId: null, productId: null, productCode: null, productName: null, productSpc: null,
    unitOfMeasure: null, unitName: null, quantity: undefined, unitPrice: undefined, lineAmount: undefined,
    spacing: null, productSize: null, printingReq: null, ropeSpec: null, packageReq: null,
    shippingReq: null, routeProductId: null, routeCode: null, routeName: null,
    requestDate: null, remark: null, lineAttrs: {}, lineNo: src?.lineNo
  }, src || {})
  // 编辑已有行：若带产品，按产品分类拉 schema + 回填扩展属性
  if (src?.productId) {
    loadExtAttrsByProduct(src.productId, src.lineAttrs)
    // 已有路线选择(自动带出或手选)时只拉候选不重算, 保留原选择
    loadRouteOptions(!src.routeProductId)
  }
}

/** 拉产品绑定的路线候选, 并按头维度预览默认路线 */
async function loadRouteOptions(selectResolved = true) {
  routeBlocked.value = false
  routeBlockMsg.value = ''
  if (!form.productId) { routeOptions.value = []; return }
  const [bindRes, routeRes] = await Promise.all([
    listRouteProduct({ itemId: form.productId, pageSize: 100 }),
    listRoute({ pageSize: 1000 })
  ])
  const routeMap: Record<number, string> = {}
  ;(routeRes.rows || []).forEach((rt: any) => { routeMap[rt.routeId] = rt.routeName || rt.routeCode })
  routeOptions.value = (bindRes.rows || []).map((rp: any) => ({
    ...rp, _routeName: routeMap[rp.routeId] || ('路线#' + rp.routeId)
  }))
  if (selectResolved) await applyResolvedRoute()
}

async function applyResolvedRoute() {
  if (!form.productId) return
  const r = await resolveRouteProduct({
    itemId: form.productId, orderType: props.orderType,
    outsourceFlag: props.outsourceFlag, packageFlag: props.packageFlag
  })
  const d = r.data || {}
  if (d.hardBlocked) {
    routeBlocked.value = true
    routeBlockMsg.value = d.message || '该产品未配置含外发工序的工艺路线，请先在工艺路线主数据配置'
    form.routeProductId = null
    form.routeCode = null
    form.routeName = null
  } else if (d.matched && d.routeProductId) {
    form.routeProductId = d.routeProductId
    const hit = routeOptions.value.find((o: any) => o.recordId === d.routeProductId)
    if (hit) { form.routeCode = hit.routeCode || null; form.routeName = hit._routeName || null }
  }
}

function onRouteChange(recordId: number | undefined) {
  const hit = routeOptions.value.find((o: any) => o.recordId === recordId)
  form.routeCode = hit?.routeCode || null
  form.routeName = hit?._routeName || null
}

/** 选物料后拉详情：取 extAttrs（物料扩展属性快照）+ 按分类拉 schema */
function loadExtAttrsByProduct(itemId: number, snapshot?: Record<string, any>) {
  getItem(itemId).then(r => {
    const item = r.data
    // 优先用传入快照（编辑已有行），否则用物料当前 extAttrs（新选料时快照）
    form.lineAttrs = snapshot && Object.keys(snapshot).length ? { ...snapshot } : { ...(item.extAttrs || {}) }
    if (item.itemTypeId) {
      getEffAttrSchema(item.itemTypeId).then(s => { effAttrSchema.value = s.data || [] })
    } else {
      effAttrSchema.value = []
    }
  })
}

function onProductSelected(row: any) {
  form.productId = row.itemId
  form.productCode = row.itemCode
  form.productName = row.itemName
  form.productSpc = row.specification
  form.unitOfMeasure = row.unitOfMeasure
  form.unitName = row.unitName
  if (row.itemId) {
    loadExtAttrsByProduct(row.itemId)
    loadRouteOptions(true)
  }
}
function calcAmount() {
  if (form.unitPrice != null && form.quantity != null) {
    form.lineAmount = Number((form.unitPrice * form.quantity).toFixed(2))
  }
}
async function confirm() {
  if (!form.productName) { emit('update:modelValue', false); return }
  if (routeBlocked.value) { ElMessage.error(routeBlockMsg.value || '外发路线未配置'); return }
  // 扩展属性必填校验
  if (extAttrFormRef.value) {
    try { await extAttrFormRef.value.validate() }
    catch (e: any) { ElMessage.error(e.message || '扩展属性校验失败'); return }
  }
  calcAmount()
  emit('confirm', { ...form })
  showFlag.value = false
}
function onClose() { emit('update:modelValue', false) }
</script>
