<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="规则ID" prop="ruleId">
        <el-input-number v-model="queryParams.ruleId" placeholder="请输入规则ID" style="width: 200px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()" v-hasPermi="['mes:sys:autocoderesult:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="resultList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="规则ID" align="center" prop="ruleId" width="80" />
      <el-table-column label="生成日期" align="center" prop="genDate" width="120" />
      <el-table-column label="总次数" align="center" prop="genIndex" width="80" />
      <el-table-column label="最后流水号" align="center" prop="lastSerialNo" width="100" />
      <el-table-column label="最后编码值" align="center" prop="lastResult" show-overflow-tooltip />
      <el-table-column label="传入字符" align="center" prop="lastInputChar" width="120" />
      <el-table-column label="操作" align="center" width="80">
        <template #default="scope">
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mes:sys:autocoderesult:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup lang="ts" name="AutoCodeResult">
import { ref, reactive, toRefs } from 'vue'
import { getCurrentInstance } from 'vue'
import type { SysAutoCodeResult, AutoCodeResultQueryParams } from '@/types/api/mes/sys/autocoderesult'
import { listAutoCodeResult, delAutoCodeResult } from '@/api/mes/sys/autocoderesult'

const { proxy } = getCurrentInstance() as any

const resultList = ref<SysAutoCodeResult[]>([])
const loading = ref<boolean>(true)
const showSearch = ref<boolean>(true)
const ids = ref<number[]>([])
const single = ref<boolean>(true)
const multiple = ref<boolean>(true)
const total = ref<number>(0)

const data = reactive({ queryParams: { pageNum: 1, pageSize: 10 } as AutoCodeResultQueryParams })
const { queryParams } = toRefs(data)

function getList() { loading.value = true; listAutoCodeResult(queryParams.value).then(r => { resultList.value = r.rows; total.value = r.total; loading.value = false }) }
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleSelectionChange(s: SysAutoCodeResult[]) { ids.value = s.map(i => i.codeId!); multiple.value = !s.length }
function handleDelete(row?: SysAutoCodeResult) {
  proxy.$modal.confirm('是否确认删除该记录？').then(() => delAutoCodeResult(row?.codeId || ids.value)).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') }).catch(() => {})
}
getList()
</script>
