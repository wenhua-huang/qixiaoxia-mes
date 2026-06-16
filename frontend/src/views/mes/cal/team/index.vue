<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryFormRef" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="班组类型" prop="teamType">
        <el-select v-model="queryParams.teamType" placeholder="请选择班组类型" clearable>
          <el-option
            v-for="item in dictData.mes_calendar_type"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="班组编号" prop="teamCode">
        <el-input
          v-model="queryParams.teamCode"
          placeholder="请输入班组编号"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="班组名称" prop="teamName">
        <el-input
          v-model="queryParams.teamName"
          placeholder="请输入班组名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="small" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="small"
          @click="handleAdd"
          v-hasPermi="['mes:cal:team:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="small"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['mes:cal:team:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="small"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['mes:cal:team:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="small"
          @click="handleExport"
          v-hasPermi="['mes:cal:team:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="teamList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="班组编号" align="center" prop="teamCode">
        <template #default="scope">
          <el-button
            link
            @click="handleView(scope.row)"
            v-hasPermi="['mes:cal:team:query']"
          >{{ scope.row.teamCode }}</el-button>
        </template>
      </el-table-column>
      <el-table-column label="班组名称" align="center" prop="teamName" />
      <el-table-column label="班组类型" align="center" prop="teamType">
        <template #default="scope">
          <dict-tag :options="dictData.mes_calendar_type" :value="scope.row.teamType" />
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button
            size="small"
            link
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['mes:cal:team:edit']"
          >修改</el-button>
          <el-button
            size="small"
            link
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['mes:cal:team:remove']"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:current-page="queryParams.pageNum"
      v-model:page-size="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加/修改/查看班组对话框 -->
    <el-dialog :title="title" v-model="open" width="960px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="8">
            <el-form-item label="班组编号" prop="teamCode">
              <el-input v-model="form.teamCode" placeholder="请输入班组编号" />
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label-width="0">
              <el-switch
                v-model="autoGenFlag"
                active-color="#13ce66"
                active-text="自动生成"
                @change="handleAutoGenChange"
                v-if="optType !== 'view'"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="班组名称" prop="teamName">
              <el-input v-model="form.teamName" placeholder="请输入班组名称" />
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label="班组类型" prop="teamType">
              <el-select v-model="form.teamType" placeholder="请选择班组类型">
                <el-option
                  v-for="item in dictData.mes_calendar_type"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <el-divider v-if="form.teamId != null" content-position="center">项目组成员</el-divider>
      <Member v-if="form.teamId != null" :optType="optType" :teamId="form.teamId" />

      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm" v-if="optType !== 'view'">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, getCurrentInstance } from 'vue'
import { listTeam, getTeam, delTeam, addTeam, updateTeam } from '@/api/mes/cal/team'
import { genSerialCode } from '@/api/mes/sys/autocoderule'
import { useDict } from '@/utils/dict'
import Member from './member.vue'

const { proxy } = getCurrentInstance() as any
const { mes_calendar_type } = useDict('mes_calendar_type')

const dictData = reactive({ mes_calendar_type })

const autoGenFlag = ref(false)
const optType = ref<string | undefined>(undefined)
const loading = ref(true)
const ids = ref<number[]>([])
const single = ref(true)
const multiple = ref(true)
const showSearch = ref(true)
const total = ref(0)
const teamList = ref<any[]>([])
const title = ref('')
const open = ref(false)
const queryFormRef = ref()
const formRef = ref()

const queryParams = reactive<any>({
  pageNum: 1,
  pageSize: 10,
  teamCode: null,
  teamName: null,
  teamType: null,
})

const form = reactive<any>({
  teamId: null,
  teamCode: null,
  teamName: null,
  teamType: null,
  remark: null,
})

const rules = {
  teamCode: [
    { required: true, message: '班组编号不能为空', trigger: 'blur' },
    { max: 64, message: '字段过长', trigger: 'blur' },
  ],
  teamName: [
    { required: true, message: '班组名称不能为空', trigger: 'blur' },
    { max: 100, message: '字段过长', trigger: 'blur' },
  ],
  teamType: [
    { required: true, message: '请选择班组类型', trigger: 'blur' },
  ],
  remark: [
    { max: 250, message: '长度必须小于250个字符', trigger: 'blur' },
  ],
}

function getList() {
  loading.value = true
  listTeam(queryParams).then((response: any) => {
    teamList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

function cancel() {
  open.value = false
  reset()
}

function reset() {
  form.teamId = null
  form.teamCode = null
  form.teamName = null
  form.teamType = null
  form.remark = null
  autoGenFlag.value = false
  proxy.resetForm('formRef')
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryFormRef')
  handleQuery()
}

function handleSelectionChange(selection: any[]) {
  ids.value = selection.map((item) => item.teamId)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

function handleAdd() {
  reset()
  open.value = true
  title.value = '添加班组'
  optType.value = 'add'
}

function handleView(row: any) {
  reset()
  const teamId = row.teamId || ids.value
  getTeam(teamId).then((response: any) => {
    Object.assign(form, response.data)
    open.value = true
    title.value = '查看班组'
    optType.value = 'view'
  })
}

function handleUpdate(row: any) {
  reset()
  const teamId = row.teamId || ids.value
  getTeam(teamId).then((response: any) => {
    Object.assign(form, response.data)
    open.value = true
    title.value = '修改班组'
    optType.value = 'edit'
  })
}

function submitForm() {
  formRef.value?.validate((valid: boolean) => {
    if (valid) {
      if (form.teamId != null) {
        updateTeam(form).then(() => {
          proxy.$modal.msgSuccess('修改成功')
          open.value = false
          getList()
        })
      } else {
        addTeam(form).then(() => {
          proxy.$modal.msgSuccess('新增成功')
          open.value = false
          getList()
        })
      }
    }
  })
}

function handleDelete(row: any) {
  const teamIds = row.teamId || ids.value
  proxy.$modal.confirm('是否确认删除班组编号为"' + teamIds + '"的数据项？').then(() => {
    return delTeam(teamIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

function handleExport() {
  proxy.download('mes/cal/team/export', {
    ...queryParams,
  }, `team_${new Date().getTime()}.xlsx`)
}

function handleAutoGenChange(flag: boolean) {
  if (flag) {
    genSerialCode('CAL_TEAM_CODE').then((response: any) => {
      form.teamCode = response.data
    })
  } else {
    form.teamCode = null
  }
}

getList()
</script>
