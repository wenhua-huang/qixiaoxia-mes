<template>
  <div class="app-container">
    <!-- 搜索 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="凭证名称" prop="name">
        <el-input v-model="queryParams.name" placeholder="请输入凭证名称" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="enabled">
        <el-select v-model="queryParams.enabled" placeholder="凭证状态" clearable style="width: 160px">
          <el-option label="启用" value="Y" />
          <el-option label="停用" value="N" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 工具栏 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['system:apikey:add']">生成凭证</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 列表 -->
    <el-table v-loading="loading" :data="apiKeyList">
      <el-table-column label="ID" align="center" prop="id" width="70" />
      <el-table-column label="凭证名称" align="center" prop="name" :show-overflow-tooltip="true" />
      <el-table-column label="绑定工厂" align="center" prop="factoryId" width="140">
        <template #default="scope">{{ factoryName(scope.row.factoryId) }}</template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="80">
        <template #default="scope">
          <el-tag :type="scope.row.enabled === 'Y' ? 'success' : 'info'">{{ scope.row.enabled === 'Y' ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="过期时间" align="center" prop="expiresAt" width="170">
        <template #default="scope">{{ scope.row.expiresAt ? parseTime(scope.row.expiresAt) : '永不过期' }}</template>
      </el-table-column>
      <el-table-column label="创建人" align="center" prop="createBy" width="110" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="170">
        <template #default="scope">{{ parseTime(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" @click="handleToggle(scope.row)" v-hasPermi="['system:apikey:edit']">
            {{ scope.row.enabled === 'Y' ? '停用' : '启用' }}
          </el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:apikey:remove']">吊销</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 生成凭证弹窗 -->
    <el-dialog title="生成 API Key" v-model="dialogVisible" width="480px" append-to-body>
      <el-form ref="genRef" :model="genForm" :rules="genRules" label-width="90px">
        <el-form-item label="凭证名称" prop="name">
          <el-input v-model="genForm.name" placeholder="如：CRM 生产环境" />
        </el-form-item>
        <el-form-item label="绑定工厂" prop="factoryId">
          <el-select v-model="genForm.factoryId" placeholder="选择工厂" style="width: 100%">
            <el-option v-for="f in factoryList" :key="f.factoryId" :label="f.factoryName" :value="f.factoryId" />
          </el-select>
        </el-form-item>
        <el-form-item label="过期时间" prop="expiresAt">
          <el-date-picker v-model="genForm.expiresAt" type="datetime" placeholder="不填则永不过期" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="genForm.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" :loading="submitting" @click="submitGen">确 定</el-button>
        <el-button @click="dialogVisible = false">取 消</el-button>
      </template>
    </el-dialog>

    <!-- 明文 Key 展示弹窗 -->
    <el-dialog title="✅ 凭证生成成功" v-model="keyResultVisible" width="520px" :close-on-click-modal="false" :close-on-press-escape="false" append-to-body>
      <el-alert type="warning" :closable="false" show-icon style="margin-bottom: 16px">
        <template #title>明文 API Key 仅本次显示，请立即复制并妥善保存，关闭后无法再次查看。</template>
      </el-alert>
      <div class="key-display">
        <el-input v-model="plaintextKey" readonly>
          <template #append>
            <el-button :icon="CopyDocument" @click="copyKey" />
          </template>
        </el-input>
      </div>
      <div style="margin-top: 12px; color: #909399; font-size: 13px">
        调用接口时在请求头携带：<code style="color: #409eff">X-API-Key: {{ plaintextKey ? plaintextKey.substring(0, 8) + '...' : '' }}</code>
      </div>
      <template #footer>
        <el-button type="primary" @click="keyResultVisible = false">我已保存，关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="ApiKey">
import { listApiKey, genApiKey, toggleApiKey, delApiKey } from '@/api/system/apikey'
import { listAllFactory } from '@/api/mes/md/factory'
import type { SysApiKey, ApiKeyQueryParams, GenApiKeyParams } from '@/types/api/system/apikey'
import { CopyDocument } from '@element-plus/icons-vue'

const { proxy } = getCurrentInstance() as any

const apiKeyList = ref<SysApiKey[]>([])
const factoryList = ref<any[]>([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const dialogVisible = ref(false)
const keyResultVisible = ref(false)
const submitting = ref(false)
const plaintextKey = ref('')

const data = reactive({
  queryParams: { pageNum: 1, pageSize: 10, name: undefined, enabled: undefined } as ApiKeyQueryParams,
  genForm: { name: '', factoryId: undefined as number | undefined, expiresAt: '', remark: '' } as GenApiKeyParams & { expiresAt: string },
  genRules: {
    name: [{ required: true, message: '凭证名称不能为空', trigger: 'blur' }],
    factoryId: [{ required: true, message: '请选择绑定工厂', trigger: 'change' }]
  }
})
const { queryParams, genForm, genRules } = toRefs(data)

/** 工厂 ID → 名称映射 */
function factoryName(factoryId?: number): string {
  if (!factoryId) return ''
  const f = factoryList.value.find(x => x.factoryId === factoryId)
  return f ? f.factoryName : `工厂#${factoryId}`
}

/** 加载工厂列表（用于下拉和名称映射） */
function loadFactories() {
  listAllFactory().then(res => { factoryList.value = res.data || [] })
}

/** 查询列表 */
function getList() {
  loading.value = true
  listApiKey(queryParams.value).then(res => {
    apiKeyList.value = res.rows
    total.value = res.total
    loading.value = false
  })
}

function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }

/** 生成凭证 */
function handleAdd() {
  genForm.value = { name: '', factoryId: undefined, expiresAt: '', remark: '' }
  proxy.resetForm('genRef')
  dialogVisible.value = true
}

function submitGen() {
  proxy.$refs['genRef'].validate((valid: boolean) => {
    if (!valid) return
    submitting.value = true
    const params: GenApiKeyParams = {
      name: genForm.value.name,
      factoryId: genForm.value.factoryId!,
      expiresAt: genForm.value.expiresAt || undefined,
      remark: genForm.value.remark || undefined
    }
    genApiKey(params).then(res => {
      plaintextKey.value = res.data?.apiKey || ''
      dialogVisible.value = false
      keyResultVisible.value = true
      getList()
    }).finally(() => { submitting.value = false })
  })
}

/** 复制 Key 到剪贴板 */
function copyKey() {
  navigator.clipboard.writeText(plaintextKey.value).then(() => {
    proxy.$modal.msgSuccess('已复制到剪贴板')
  }).catch(() => {
    proxy.$modal.msgWarning('复制失败，请手动选择文本复制')
  })
}

/** 启用/停用 */
function handleToggle(row: SysApiKey) {
  const next = row.enabled === 'Y' ? 'N' : 'Y'
  const action = next === 'Y' ? '启用' : '停用'
  proxy.$modal.confirm(`确认${action}凭证「${row.name}」？`).then(() => {
    return toggleApiKey(row.id!, next)
  }).then(() => {
    proxy.$modal.msgSuccess(`${action}成功`)
    getList()
  }).catch(() => {})
}

/** 吊销（删除） */
function handleDelete(row: SysApiKey) {
  proxy.$modal.confirm(`确认吊销凭证「${row.name}」？吊销后该 Key 将立即失效，无法恢复。`).then(() => {
    return delApiKey(row.id!)
  }).then(() => {
    proxy.$modal.msgSuccess('吊销成功')
    getList()
  }).catch(() => {})
}

loadFactories()
getList()
</script>

<style lang="scss" scoped>
.key-display {
  :deep(.el-input__wrapper) {
    background: #f5f7fa;
  }
  code {
    background: #f0f2f5;
    padding: 2px 6px;
    border-radius: 3px;
    font-size: 12px;
  }
}
</style>
