<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="原始文件名" prop="orignalName">
        <el-input v-model="queryParams.orignalName" placeholder="请输入文件名" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="单据类型" prop="sourceDocType">
        <el-input v-model="queryParams.sourceDocType" placeholder="请输入单据类型" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['mes:sys:attachment:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()" v-hasPermi="['mes:sys:attachment:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="attachmentList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="原始文件名" align="center" prop="orignalName" show-overflow-tooltip />
      <el-table-column label="文件类型" align="center" prop="fileType" width="100" />
      <el-table-column label="文件大小(KB)" align="center" prop="fileSize" width="120" />
      <el-table-column label="单据类型" align="center" prop="sourceDocType" width="120" />
      <el-table-column label="单据ID" align="center" prop="sourceDocId" width="100" />
      <el-table-column label="备注" align="center" prop="remark" show-overflow-tooltip />
      <el-table-column label="操作" align="center" width="150">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:sys:attachment:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mes:sys:attachment:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="原始文件名" prop="orignalName">
          <el-input v-model="form.orignalName" placeholder="请输入原始文件名" />
        </el-form-item>
        <el-form-item label="文件URL" prop="fileUrl">
          <el-input v-model="form.fileUrl" placeholder="请输入文件URL" />
        </el-form-item>
        <el-form-item label="文件类型" prop="fileType">
          <el-input v-model="form.fileType" placeholder="如 png/pdf/xlsx" />
        </el-form-item>
        <el-form-item label="文件大小(KB)" prop="fileSize">
          <el-input-number v-model="form.fileSize" :min="0" :precision="2" style="width: 200px" />
        </el-form-item>
        <el-form-item label="单据类型" prop="sourceDocType">
          <el-input v-model="form.sourceDocType" placeholder="如 wm_item_recpt" />
        </el-form-item>
        <el-form-item label="单据ID" prop="sourceDocId">
          <el-input-number v-model="form.sourceDocId" :min="0" style="width: 200px" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
      </el-form>
      <template #footer><div class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="Attachment">
import { ref, reactive, toRefs } from 'vue'
import { getCurrentInstance } from 'vue'
import type { SysAttachment, AttachmentQueryParams } from '@/types/api/mes/sys/attachment'
import { listAttachment, getAttachment, addAttachment, updateAttachment, delAttachment } from '@/api/mes/sys/attachment'

const { proxy } = getCurrentInstance() as any

const attachmentList = ref<SysAttachment[]>([])
const open = ref<boolean>(false)
const loading = ref<boolean>(true)
const showSearch = ref<boolean>(true)
const ids = ref<number[]>([])
const single = ref<boolean>(true)
const multiple = ref<boolean>(true)
const total = ref<number>(0)
const title = ref<string>('')

const data = reactive({
  form: {} as SysAttachment,
  queryParams: { pageNum: 1, pageSize: 10 } as AttachmentQueryParams,
  rules: { orignalName: [{ required: true, message: '原始文件名不能为空', trigger: 'blur' }] }
})
const { queryParams, form, rules } = toRefs(data)

function getList() { loading.value = true; listAttachment(queryParams.value).then(r => { attachmentList.value = r.rows; total.value = r.total; loading.value = false }) }
function cancel() { open.value = false; reset() }
function reset() { form.value = {} as SysAttachment; proxy.resetForm('formRef') }
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleSelectionChange(s: SysAttachment[]) { ids.value = s.map(i => i.attachmentId!); single.value = s.length !== 1; multiple.value = !s.length }
function handleAdd() { reset(); open.value = true; title.value = '添加附件' }
function handleUpdate(row?: SysAttachment) { reset(); const id = row?.attachmentId || ids.value[0]; getAttachment(id).then(r => { form.value = r.data; open.value = true; title.value = '修改附件' }) }
function submitForm() {
  proxy.$refs['formRef'].validate((v: boolean) => {
    if (v) {
      if (form.value.attachmentId) updateAttachment(form.value).then(() => { proxy.$modal.msgSuccess('修改成功'); open.value = false; getList() })
      else addAttachment(form.value).then(() => { proxy.$modal.msgSuccess('新增成功'); open.value = false; getList() })
    }
  })
}
function handleDelete(row?: SysAttachment) {
  proxy.$modal.confirm('是否确认删除该附件？').then(() => delAttachment(row?.attachmentId || ids.value)).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') }).catch(() => {})
}
getList()
</script>
