<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="出库单编码" prop="salesCode">
        <el-input v-model="queryParams.salesCode" placeholder="请输入" clearable style="width:180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="出库单名称" prop="salesName">
        <el-input v-model="queryParams.salesName" placeholder="请输入" clearable style="width:180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="客户编码" prop="clientCode">
        <el-input v-model="queryParams.clientCode" placeholder="请输入" clearable style="width:180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['mes:wm:product_sales:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate()" v-hasPermi="['mes:wm:product_sales:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()" v-hasPermi="['mes:wm:product_sales:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['mes:wm:product_sales:export']">导出</el-button></el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="productsalesList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="出库单ID" align="center" prop="salesId" width="100" :show-overflow-tooltip="true" />
      <el-table-column label="出库单编码" align="center" prop="salesCode" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="出库单名称" align="center" prop="salesName" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="客户编码" align="center" prop="clientCode" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="客户名称" align="center" prop="clientName" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="客户订单号" align="center" prop="clientOrderCode" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="160">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:wm:product_sales:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mes:wm:product_sales:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="工厂ID" prop="factoryId">
          <el-input v-model="form.factoryId" placeholder="请输入" clearable />
        </el-form-item>
        <el-row>
          <el-col :span="16">
            <el-form-item label="出库单号" prop="salesCode">
              <el-input v-model="form.salesCode" placeholder="请输入" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label-width="70">
              <el-switch v-model="autoGenFlag" active-color="#13ce66" active-text="自动生成" @change="handleAutoGenChange" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="出库单名称" prop="salesName">
          <el-input v-model="form.salesName" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="客户ID" prop="clientId">
          <el-input v-model="form.clientId" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="客户编码" prop="clientCode">
          <el-input v-model="form.clientCode" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="客户名称" prop="clientName">
          <el-input v-model="form.clientName" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="客户订单号" prop="clientOrderCode">
          <el-input v-model="form.clientOrderCode" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="业务员" prop="salesperson">
          <el-input v-model="form.salesperson" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="出货仓库ID" prop="warehouseId">
          <el-input v-model="form.warehouseId" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="仓库编码" prop="warehouseCode">
          <el-input v-model="form.warehouseCode" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="仓库名称" prop="warehouseName">
          <el-input v-model="form.warehouseName" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="出库日期" prop="salesDate">
          <el-date-picker v-model="form.salesDate" type="datetime" placeholder="选择日期" clearable style="width:100%" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="出库总数量" prop="totalQuantity">
          <el-input v-model="form.totalQuantity" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="总箱数" prop="totalBox">
          <el-input v-model="form.totalBox" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="总体积" prop="totalVolume">
          <el-input v-model="form.totalVolume" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="总重量" prop="totalWeight">
          <el-input v-model="form.totalWeight" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="物流公司名称" prop="logisticsCompany">
          <el-input v-model="form.logisticsCompany" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="快递/物流单号" prop="trackingNo">
          <el-input v-model="form.trackingNo" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="物流费用" prop="logisticsFee">
          <el-input v-model="form.logisticsFee" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="收货详细地址" prop="shippingAddress">
          <el-input v-model="form.shippingAddress" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="收货人姓名" prop="receiverName">
          <el-input v-model="form.receiverName" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="收货人电话" prop="receiverTel">
          <el-input v-model="form.receiverTel" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="是否打托盘" prop="palletFlag">
          <el-input v-model="form.palletFlag" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="箱唛/唛头描述" prop="boxLabel">
          <el-input v-model="form.boxLabel" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="销售类型:FOREIGN-外贸,DOMESTIC-内贸,SPOT-现货" prop="salesType">
          <el-input v-model="form.salesType" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="出货检验单ID" prop="oqcId">
          <el-input v-model="form.oqcId" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="出货检验单编码" prop="oqcCode">
          <el-input v-model="form.oqcCode" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="状态:DRAFT-草稿,CONFIRMED-已确认,POSTED-已过账,SHIPPED-已发货" prop="status">
          <el-input v-model="form.status" placeholder="请输入" clearable />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="WmProductSales">
import { ref, reactive, toRefs, getCurrentInstance } from 'vue'
import type { WmProductSalesQueryParams, WmProductSales } from '@/types/api/mes/wm/product_sales'
import { listWmProductSales, getWmProductSales, delWmProductSales, addWmProductSales, updateWmProductSales } from '@/api/mes/wm/product_sales'
import { genSerialCode } from '@/api/mes/sys/autocoderule'

const { proxy } = getCurrentInstance() as any
const dicts = proxy.useDict('sys_yes_no')

const productsalesList = ref<WmProductSales[]>([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref<number[]>([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref('')
const autoGenFlag = ref(false)

const data = reactive({
  form: {} as WmProductSales,
  queryParams: { pageNum: 1, pageSize: 10 } as WmProductSalesQueryParams,
  rules: {}
})
const { queryParams, form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listWmProductSales(queryParams.value).then(r => {
    productsalesList.value = r.rows
    total.value = r.total
    loading.value = false
  })
}

function cancel() { open.value = false; reset() }
function handleAutoGenChange(flag: boolean) {
  if (flag) genSerialCode('SALES_NO').then((r: any) => { form.value.salesCode = r.data })
  else form.value.salesCode = ''
}
function reset() { autoGenFlag.value = false; form.value = {} as WmProductSales; proxy.resetForm('formRef') }
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleSelectionChange(s: any[]) { ids.value = s.map(i => i.salesId); single.value = s.length !== 1; multiple.value = !s.length }
function handleAdd() { reset(); open.value = true; title.value = '新增销售出库单表' }
function handleUpdate(row?: WmProductSales) {
  reset()
  const _id = row?.salesId || ids.value[0]
  getWmProductSales(_id).then(r => { form.value = r.data; open.value = true; title.value = '修改销售出库单表' })
}
function submitForm() {
  proxy.$refs['formRef'].validate((v: boolean) => {
    if (v) {
      const fn = form.value.salesId ? updateWmProductSales : addWmProductSales
      fn(form.value).then(() => { proxy.$modal.msgSuccess('操作成功'); open.value = false; getList() })
    }
  })
}
function handleDelete(row?: WmProductSales) {
  const _ids = row?.salesId ? [row.salesId] : ids.value
  proxy.$modal.confirm('是否确认删除？').then(() => delWmProductSales(_ids)).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') })
}
function handleExport() { proxy.download('/mes/wm/product_sales/export', { ...queryParams.value }, `productsales_${Date.now()}.xlsx`) }

getList()
</script>