<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="编码" prop="vendorCode"><el-input v-model="queryParams.vendorCode" placeholder="请输入" clearable style="width:160px" @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item label="名称" prop="vendorName"><el-input v-model="queryParams.vendorName" placeholder="请输入" clearable style="width:160px" @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item label="类型" prop="vendorType">
        <el-select v-model="queryParams.vendorType" placeholder="类型" clearable style="width:160px">
          <el-option v-for="d in mes_vendor_type" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item><el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button><el-button icon="Refresh" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['mes:md:vendor:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate()" v-hasPermi="['mes:md:vendor:edit']">修改</el-button></el-col>
      <!-- 删除按钮已移除：用启停用(enableFlag)替代物理删除 -->
      <el-col :span="1.5"><el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['mes:md:vendor:export']">导出</el-button></el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="vendorList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="编码" align="center" prop="vendorCode" width="120" />
      <el-table-column label="全称" align="center" prop="vendorName" :show-overflow-tooltip="true" min-width="140" />
      <el-table-column label="类型" align="center" width="110">
        <template #default="s"><dict-tag :options="mes_vendor_type" :value="s.row.vendorType" /></template>
      </el-table-column>
      <el-table-column label="等级" align="center" prop="vendorLevel" width="60" />
      <el-table-column label="联系人" align="center" prop="contact1" width="80" />
      <el-table-column label="电话" align="center" prop="contact1Tel" width="120" />
      <el-table-column label="结算方式" align="center" width="90">
        <template #default="s"><dict-tag :options="mes_settlement_type" :value="s.row.settlementType" /></template>
      </el-table-column>
      <el-table-column label="付款条件" align="center" prop="paymentTerms" width="100" :show-overflow-tooltip="true" />
      <el-table-column label="合作状态" align="center" width="90">
        <template #default="s"><dict-tag :options="mes_coop_status" :value="s.row.coopStatus" /></template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="140" fixed="right">
        <template #default="s"><el-button link type="primary" icon="Edit" @click="handleUpdate(s.row)" v-hasPermi="['mes:md:vendor:edit']">修改</el-button></template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 对话框 -->
    <el-dialog :title="title" v-model="open" width="800px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row>
          <el-col :span="12"><el-form-item label="供应商编码" prop="vendorCode"><el-input v-model="form.vendorCode" placeholder="自动生成或手动输入" :disabled="optType === 'edit' || optType === 'view'" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label-width="70" v-if="optType === 'add'">
              <el-switch v-model="autoGenFlag" active-color="#13ce66" size="small" @change="handleAutoGenChange" /><span style="margin-left:6px;font-size:12px;color:#13ce66">自动生成</span>
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="供应商全称" prop="vendorName"><el-input v-model="form.vendorName" placeholder="请输入" /></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="12"><el-form-item label="供应商简称"><el-input v-model="form.vendorNick" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="英文名称"><el-input v-model="form.vendorEn" /></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="供应商类型">
              <el-select v-model="form.vendorType" style="width:100%">
                <el-option v-for="d in mes_vendor_type" :key="d.value" :label="d.label" :value="d.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="供应商等级">
              <el-select v-model="form.vendorLevel" style="width:100%" clearable>
                <el-option label="A-优秀" value="A" /><el-option label="B-良好" value="B" /><el-option label="C-一般" value="C" /><el-option label="D-待评估" value="D" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="外协工厂" v-if="form.vendorType==='OUTSOURCE'||form.vendorType==='BOTH'">
              <el-select v-model="form.outsourceFactoryId" placeholder="选择外协工厂" clearable style="width:100%">
                <el-option v-for="f in factoryOptions" :key="f.factoryId" :label="f.factoryName" :value="f.factoryId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="社会信用代码"><el-input v-model="form.creditCode" placeholder="统一社会信用代码" /></el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12"><el-form-item label="结算方式">
            <el-select v-model="form.settlementType" style="width:100%" clearable>
              <el-option v-for="d in mes_settlement_type" :key="d.value" :label="d.label" :value="d.value" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item label="付款条件"><el-input v-model="form.paymentTerms" placeholder="如：30天账期" /></el-form-item></el-col>
        </el-row>
        <el-divider content-position="left">联系人信息</el-divider>
        <el-row>
          <el-col :span="8"><el-form-item label="主要联系人"><el-input v-model="form.contact1" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="联系电话"><el-input v-model="form.contact1Tel" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="邮箱"><el-input v-model="form.contact1Email" /></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="8"><el-form-item label="备用联系人"><el-input v-model="form.contact2" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="备用电话"><el-input v-model="form.contact2Tel" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="备用邮箱"><el-input v-model="form.contact2Email" /></el-form-item></el-col>
        </el-row>
        <el-divider content-position="left">其他信息</el-divider>
        <el-form-item label="地址"><el-input v-model="form.address" /></el-form-item>
        <el-row>
          <el-col :span="8"><el-form-item label="官网"><el-input v-model="form.website" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="企业电话"><el-input v-model="form.tel" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="企业邮箱"><el-input v-model="form.email" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="简介/经营范围"><el-input v-model="form.vendorDes" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
        <el-row>
          <el-col :span="12">
            <el-form-item label="合作状态">
              <el-select v-model="form.coopStatus" style="width:100%">
                <el-option v-for="d in mes_coop_status" :key="d.value" :label="d.label" :value="d.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否启用">
              <el-radio-group v-model="form.enableFlag"><el-radio value="1">是</el-radio><el-radio value="0">否</el-radio></el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="Vendor">
import { ref, reactive, toRefs } from 'vue'
import { getCurrentInstance } from 'vue'
import { genSerialCode } from '@/api/mes/sys/autocoderule'
import type { MdVendor, VendorQueryParams } from '@/types/api/mes/md/vendor'
import { listVendor, getVendor, delVendor, addVendor, updateVendor } from '@/api/mes/md/vendor'
import { listAllFactory } from '@/api/mes/md/factory'

const { proxy } = getCurrentInstance() as any
const { sys_yes_no } = useDict('sys_yes_no')
const { mes_vendor_type, mes_settlement_type, mes_coop_status } = useDict('mes_vendor_type', 'mes_settlement_type', 'mes_coop_status')

const vendorList = ref<MdVendor[]>([]); const open = ref(false); const loading = ref(true); const showSearch = ref(true)
const ids = ref<number[]>([]); const single = ref(true); const multiple = ref(true); const total = ref(0); const title = ref('')
const autoGenFlag = ref(false)
const optType = ref<string | undefined>(undefined)
const factoryOptions = ref<any[]>([])

const data = reactive({
  form: {} as MdVendor,
  queryParams: { pageNum: 1, pageSize: 10 } as VendorQueryParams,
  rules: { vendorCode: [{ required: true, message: '编码不能为空', trigger: 'blur' }], vendorName: [{ required: true, message: '名称不能为空', trigger: 'blur' }] }
})
const { queryParams, form, rules } = toRefs(data)

function getList() { loading.value = true; listVendor(queryParams.value).then(r => { vendorList.value = r.rows; total.value = r.total; loading.value = false }) }
function cancel() { open.value = false; reset() }
function handleAutoGenChange(flag: boolean) {
  if (flag) genSerialCode('VENDOR_CODE').then((r: any) => { form.value.vendorCode = r.data })
  else form.value.vendorCode = ''
}
function reset() { optType.value = undefined; autoGenFlag.value = false; form.value = { enableFlag: '1', coopStatus: 'ACTIVE', vendorType: 'MATERIAL', vendorLevel: 'C' } as MdVendor; proxy.resetForm('formRef') }
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleSelectionChange(s: MdVendor[]) { ids.value = s.map(i => i.vendorId!); single.value = s.length !== 1; multiple.value = !s.length }
function handleAdd() { reset(); optType.value = 'add'; listAllFactory().then(r => { factoryOptions.value = r.data || [] }); open.value = true; title.value = '新增供应商' }
function handleUpdate(row?: MdVendor) { reset(); optType.value = 'edit'; listAllFactory().then(r => { factoryOptions.value = r.data || [] }); const id = row?.vendorId || ids.value[0]; getVendor(id).then(r => { form.value = r.data; open.value = true; title.value = '修改供应商' }) }
function submitForm() { proxy.$refs['formRef'].validate((v: boolean) => { if (v) { (form.value.vendorId ? updateVendor(form.value) : addVendor(form.value)).then(() => { proxy.$modal.msgSuccess('操作成功'); open.value = false; getList() }) } }) }
function handleDelete(row?: MdVendor) { const ids_ = row?.vendorId || ids.value; proxy.$modal.confirm('确认删除？').then(() => delVendor(ids_)).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') }).catch(() => {}) }
function handleExport() { proxy.download('mes/md/vendor/export', { ...queryParams.value }, `vendor_${Date.now()}.xlsx`) }
getList()
</script>

<style scoped>
:deep(.el-form-item__label) { padding-right: 16px !important; }
</style>
