<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="客户编码" prop="clientCode"><el-input v-model="queryParams.clientCode" placeholder="请输入" clearable style="width:180px" @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item label="客户名称" prop="clientName"><el-input v-model="queryParams.clientName" placeholder="请输入" clearable style="width:180px" @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item label="客户类型" prop="clientType"><el-select v-model="queryParams.clientType" placeholder="类型" clearable style="width:180px"><el-option v-for="d in mes_client_type" :key="d.value" :label="d.label" :value="d.value" /></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button><el-button icon="Refresh" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['mes:md:client:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate()" v-hasPermi="['mes:md:client:edit']">修改</el-button></el-col>
      <!-- 删除按钮已移除：用启停用(enableFlag)替代物理删除 -->
      <el-col :span="1.5"><el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['mes:md:client:export']">导出</el-button></el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>
    <el-table v-loading="loading" :data="clientList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="编码" align="center" prop="clientCode" />
      <el-table-column label="全称" align="center" prop="clientName" :show-overflow-tooltip="true" />
      <el-table-column label="简称" align="center" prop="clientNick" />
      <el-table-column label="类型" align="center" width="80">
        <template #default="s"><dict-tag :options="mes_client_type" :value="s.row.clientType" /></template>
      </el-table-column>
      <el-table-column label="业务员" align="center" prop="salesperson" width="80" />
      <el-table-column label="联系人" align="center" prop="contact1" />
      <el-table-column label="联系电话" align="center" prop="contact1Tel" />
      <el-table-column label="操作" align="center" width="150"><template #default="s"><el-button link type="primary" icon="Edit" @click="handleUpdate(s.row)" v-hasPermi="['mes:md:client:edit']">修改</el-button></template></el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
    <el-dialog :title="title" v-model="open" width="750px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="12"><el-form-item label="客户编码" prop="clientCode"><el-input v-model="form.clientCode" :disabled="optType === 'edit' || optType === 'view'" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label-width="70" v-if="optType === 'add'">
              <el-switch v-model="autoGenFlag" active-color="#13ce66" size="small" @change="handleAutoGenChange" /><span style="margin-left:6px;font-size:12px;color:#13ce66">自动生成</span>
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="客户全称" prop="clientName"><el-input v-model="form.clientName" /></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="12"><el-form-item label="客户简称"><el-input v-model="form.clientNick" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="英文名"><el-input v-model="form.clientEn" /></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="12"><el-form-item label="客户类型"><el-select v-model="form.clientType" style="width:100%"><el-option v-for="d in mes_client_type" :key="d.value" :label="d.label" :value="d.value" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="业务员"><el-input v-model="form.salesperson" /></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="12"><el-form-item label="联系人"><el-input v-model="form.contact1" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="联系电话"><el-input v-model="form.contact1Tel" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="收货地址"><el-input v-model="form.shippingAddress" type="textarea" /></el-form-item>
        <el-form-item label="地址"><el-input v-model="form.address" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
        <el-form-item label="是否启用"><el-radio-group v-model="form.enableFlag"><el-radio value="1">是</el-radio><el-radio value="0">否</el-radio></el-radio-group></el-form-item>
      </el-form>
      <template #footer><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="Client">
import { ref, reactive, toRefs } from 'vue'
import { getCurrentInstance } from 'vue'
import { genSerialCode } from '@/api/mes/sys/autocoderule'
import type { MdClient, ClientQueryParams } from '@/types/api/mes/md/client'
import { listClient, getClient, delClient, addClient, updateClient } from '@/api/mes/md/client'
const { proxy } = getCurrentInstance() as any; const { sys_yes_no } = useDict('sys_yes_no')
const { mes_client_type } = useDict('mes_client_type')
const clientList = ref<MdClient[]>([]); const open = ref(false); const loading = ref(true); const showSearch = ref(true)
const ids = ref<number[]>([]); const single = ref(true); const multiple = ref(true); const total = ref(0); const title = ref('')
const autoGenFlag = ref(false)
const optType = ref<string | undefined>(undefined)
const data = reactive({ form: {} as MdClient, queryParams: { pageNum: 1, pageSize: 10 } as ClientQueryParams, rules: { clientCode: [{ required: true, message: '编码不能为空' }], clientName: [{ required: true, message: '名称不能为空' }] } })
const { queryParams, form, rules } = toRefs(data)
function getList() { loading.value = true; listClient(queryParams.value).then(r => { clientList.value = r.rows; total.value = r.total; loading.value = false }) }
function cancel() { open.value = false; reset() }
function handleAutoGenChange(flag: boolean) {
  if (flag) genSerialCode('CLIENT_CODE').then((r: any) => { form.value.clientCode = r.data })
  else form.value.clientCode = ''
}
function reset() { optType.value = undefined; autoGenFlag.value = false; form.value = { enableFlag: '1' } as MdClient; proxy.resetForm('formRef') }
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleSelectionChange(s: MdClient[]) { ids.value = s.map(i => i.clientId!); single.value = s.length !== 1; multiple.value = !s.length }
function handleAdd() { reset(); optType.value = 'add'; open.value = true; title.value = '新增客户' }
function handleUpdate(row?: MdClient) { reset(); optType.value = 'edit'; const id = row?.clientId || ids.value[0]; getClient(id).then(r => { form.value = r.data; open.value = true; title.value = '修改客户' }) }
function submitForm() { proxy.$refs['formRef'].validate((v: boolean) => { if (v) { (form.value.clientId ? updateClient(form.value) : addClient(form.value)).then(() => { proxy.$modal.msgSuccess('操作成功'); open.value = false; getList() }) } }) }
function handleDelete(row?: MdClient) { const ids_ = row?.clientId || ids.value; proxy.$modal.confirm('确认删除？').then(() => delClient(ids_)).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') }).catch(() => {}) }
function handleExport() { proxy.download('mes/md/client/export', { ...queryParams.value }, `client_${Date.now()}.xlsx`) }
getList()
</script>

<style scoped>
:deep(.el-form-item__label) { padding-right: 16px !important; }
</style>
