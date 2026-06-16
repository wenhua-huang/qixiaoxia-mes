<template>
  <div class="app-container">
    <el-row v-if="optType !== 'view'" :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="small"
          @click="handleAdd"
          v-hasPermi="['mes:cal:team:add']"
        >新增</el-button>
        <UserMultiSelect v-model:showFlag="userSelectVisible" @onSelected="userSelected" />
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
    </el-row>

    <el-table v-loading="loading" :data="memberList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="用户名" align="center" prop="userName" />
      <el-table-column label="用户昵称" align="center" prop="nickName" />
      <el-table-column label="电话" align="center" prop="tel" />
      <el-table-column label="操作" align="center" v-if="optType !== 'view'" class-name="small-padding fixed-width">
        <template #default="scope">
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, getCurrentInstance } from 'vue'
import { listMember, addMember, delMember } from '@/api/mes/cal/team-member'
import UserMultiSelect from '@/components/UserSelect/multi.vue'

const props = defineProps<{
  teamId: number | null
  optType: string | undefined
}>()

const { proxy } = getCurrentInstance() as any

const loading = ref(true)
const ids = ref<number[]>([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const memberList = ref<any[]>([])
const userSelectVisible = ref(false)

const queryParams = reactive<any>({
  pageNum: 1,
  pageSize: 10,
  teamId: props.teamId,
  userId: null,
  userName: null,
  nickName: null,
})

const form = reactive<any>({
  memberId: null,
  teamId: props.teamId,
  userId: null,
  userName: null,
  nickName: null,
  tel: null,
})

function getList() {
  loading.value = true
  listMember(queryParams).then((response: any) => {
    memberList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

function handleSelectionChange(selection: any[]) {
  ids.value = selection.map((item) => item.memberId)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

function handleAdd() {
  userSelectVisible.value = true
}

function userSelected(rows: any[]) {
  if (rows != null) {
    rows.forEach((user) => {
      form.teamId = props.teamId
      form.userId = user.userId
      form.userName = user.userName
      form.nickName = user.nickName
      form.tel = user.phonenumber
      addMember(form).then(() => {
        getList()
      })
    })
  }
}

function handleDelete(row: any) {
  const memberIds = row.memberId || ids.value
  proxy.$modal.confirm('是否确认删除班组成员？').then(() => {
    return delMember(memberIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

getList()
</script>
