<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="消息标题" prop="title">
        <el-input v-model="queryParams.title" placeholder="请输入消息标题" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="消息类型" prop="messageType">
        <el-select v-model="queryParams.messageType" placeholder="消息类型" clearable style="width: 200px">
          <el-option label="系统消息" value="SYSTEM" />
          <el-option label="审批通知" value="APPROVAL" />
          <el-option label="预警通知" value="WARNING" />
          <el-option label="公告" value="NOTICE" />
        </el-select>
      </el-form-item>
      <el-form-item label="是否已读" prop="readFlag">
        <el-select v-model="queryParams.readFlag" placeholder="是否已读" clearable style="width: 200px">
          <el-option label="未读" value="0" />
          <el-option label="已读" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['mes:sys:message:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Check" :disabled="multiple" @click="handleMarkAsRead()" v-hasPermi="['mes:sys:message:edit']">标记已读</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()" v-hasPermi="['mes:sys:message:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="messageList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="消息标题" align="center" prop="title" show-overflow-tooltip />
      <el-table-column label="消息类型" align="center" prop="messageType" width="100">
        <template #default="scope">
          <el-tag v-if="scope.row.messageType === 'SYSTEM'">系统消息</el-tag>
          <el-tag v-else-if="scope.row.messageType === 'APPROVAL'" type="warning">审批通知</el-tag>
          <el-tag v-else-if="scope.row.messageType === 'WARNING'" type="danger">预警通知</el-tag>
          <el-tag v-else-if="scope.row.messageType === 'NOTICE'" type="info">公告</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="是否已读" align="center" prop="readFlag" width="80">
        <template #default="scope">
          <el-tag :type="scope.row.readFlag === '1' ? 'success' : 'info'">{{ scope.row.readFlag === '1' ? '已读' : '未读' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="接收人ID" align="center" prop="userId" width="80" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="180" />
      <el-table-column label="操作" align="center" width="200">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleView(scope.row)">查看</el-button>
          <el-button link type="primary" icon="Check" @click="handleMarkAsRead(scope.row)" v-if="scope.row.readFlag === '0'">标记已读</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mes:sys:message:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="700px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="接收用户ID" prop="userId">
          <el-input-number v-model="form.userId" :min="1" style="width: 200px" />
        </el-form-item>
        <el-form-item label="消息标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入消息标题" />
        </el-form-item>
        <el-form-item label="消息类型" prop="messageType">
          <el-select v-model="form.messageType" placeholder="请选择" style="width: 100%">
            <el-option label="系统消息" value="SYSTEM" /><el-option label="审批通知" value="APPROVAL" />
            <el-option label="预警通知" value="WARNING" /><el-option label="公告" value="NOTICE" />
          </el-select>
        </el-form-item>
        <el-form-item label="消息内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="4" placeholder="请输入消息内容" />
        </el-form-item>
        <el-form-item label="关联单据类型" prop="sourceDocType">
          <el-input v-model="form.sourceDocType" placeholder="如 pro_workorder" />
        </el-form-item>
        <el-form-item label="关联单据ID" prop="sourceDocId">
          <el-input-number v-model="form.sourceDocId" :min="0" style="width: 200px" />
        </el-form-item>
      </el-form>
      <template #footer><div class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div></template>
    </el-dialog>

    <!-- 查看消息详情 -->
    <el-dialog title="消息详情" v-model="viewOpen" width="600px" append-to-body>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="标题">{{ viewForm.title }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ viewForm.messageType }}</el-descriptions-item>
        <el-descriptions-item label="内容">{{ viewForm.content }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ viewForm.readFlag === '1' ? '已读' : '未读' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ viewForm.createTime }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="Message">
import { ref, reactive, toRefs } from 'vue'
import { getCurrentInstance } from 'vue'
import type { SysMessage, MessageQueryParams } from '@/types/api/mes/sys/message'
import { listMessage, getMessage, addMessage, delMessage, markAsRead } from '@/api/mes/sys/message'

const { proxy } = getCurrentInstance() as any

const messageList = ref<SysMessage[]>([])
const open = ref<boolean>(false)
const viewOpen = ref<boolean>(false)
const loading = ref<boolean>(true)
const showSearch = ref<boolean>(true)
const ids = ref<number[]>([])
const single = ref<boolean>(true)
const multiple = ref<boolean>(true)
const total = ref<number>(0)
const title = ref<string>('')
const viewForm = ref<SysMessage>({} as SysMessage)

const data = reactive({
  form: {} as SysMessage,
  queryParams: { pageNum: 1, pageSize: 10 } as MessageQueryParams,
  rules: {
    title: [{ required: true, message: '消息标题不能为空', trigger: 'blur' }],
    messageType: [{ required: true, message: '消息类型不能为空', trigger: 'change' }],
    userId: [{ required: true, message: '接收用户ID不能为空', trigger: 'blur' }]
  }
})
const { queryParams, form, rules } = toRefs(data)

function getList() { loading.value = true; listMessage(queryParams.value).then(r => { messageList.value = r.rows; total.value = r.total; loading.value = false }) }
function cancel() { open.value = false; reset() }
function reset() { form.value = { messageType: 'SYSTEM' } as SysMessage; proxy.resetForm('formRef') }
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleSelectionChange(s: SysMessage[]) { ids.value = s.map(i => i.messageId!); single.value = s.length !== 1; multiple.value = !s.length }
function handleAdd() { reset(); open.value = true; title.value = '添加消息' }
function handleView(row: SysMessage) { viewForm.value = row; viewOpen.value = true }
function submitForm() {
  proxy.$refs['formRef'].validate((v: boolean) => {
    if (v) { addMessage(form.value).then(() => { proxy.$modal.msgSuccess('新增成功'); open.value = false; getList() }) }
  })
}
function handleMarkAsRead(row?: SysMessage) {
  const msgIds = row ? [row.messageId!] : ids.value
  markAsRead(msgIds).then(() => { proxy.$modal.msgSuccess('标记已读成功'); getList() })
}
function handleDelete(row?: SysMessage) {
  proxy.$modal.confirm('是否确认删除该消息？').then(() => delMessage(row?.messageId || ids.value)).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') }).catch(() => {})
}
getList()
</script>
