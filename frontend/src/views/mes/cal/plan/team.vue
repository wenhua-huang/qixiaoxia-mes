<template>
  <div class="app-container">
    <el-row>
      <el-col :span="12">
        <el-card class="box-card">
          <template #header>
            <div class="clearfix">
              <span>班组</span>
              <el-button style="float: right; padding: 3px 0" v-if="optType !== 'view'" @click="handleAdd" link>添加</el-button>
            </div>
          </template>
          <el-table v-loading="loading" :data="teamList" @row-click="onRowClick">
            <el-table-column label="班组编号" align="center" prop="teamCode" />
            <el-table-column label="班组名称" align="center" prop="teamName" />
            <el-table-column label="操作" align="center" v-if="optType !== 'view'" class-name="small-padding fixed-width">
              <template #default="scope">
                <el-button
                  size="small"
                  link
                  icon="el-icon-delete"
                  @click="handleDelete(scope.row)"
                  v-hasPermi="['mes:cal:plan:edit']"
                >删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <CalTeamMultiSelect v-model:showFlag="teamSelectVisible" @onSelected="onTeamSelected" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="box-card">
          <template #header>
            <div class="clearfix">
              <span>成员</span>
            </div>
          </template>
          <el-table v-loading="memberLoading" :data="memberList">
            <el-table-column label="用户名" align="center" prop="userName" />
            <el-table-column label="用户昵称" align="center" prop="nickName" />
            <el-table-column label="电话" align="center" prop="tel" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, getCurrentInstance } from 'vue'
import { listTeam as listPlanteam, addTeam as addPlanteam, delTeam as delPlanteam } from '@/api/mes/cal/plan-team'
import { listMember } from '@/api/mes/cal/team-member'
import CalTeamMultiSelect from '@/components/calTeamSelect/multi.vue'

const props = defineProps<{
  planId: number | null
  optType: string | undefined
  calendarType: string | null
}>()

const { proxy } = getCurrentInstance() as any

const loading = ref(true)
const memberLoading = ref(false)
const teamList = ref<any[]>([])
const memberList = ref<any[]>([])
const teamSelectVisible = ref(false)
const selectedTeamRow = ref<any>(null)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 999,
  planId: props.planId,
  teamId: null as number | null,
  teamCode: null as string | null,
  teamName: null as string | null,
  calendarType: props.calendarType,
})

function getTeamList() {
  loading.value = true
  listPlanteam(queryParams).then((response: any) => {
    teamList.value = response.rows
    loading.value = false
  })
}

function getMemberList(teamId: number) {
  memberLoading.value = true
  listMember({ teamId, pageNum: 1, pageSize: 999 }).then((response: any) => {
    memberList.value = response.rows
    memberLoading.value = false
  })
}

function onRowClick(row: any) {
  selectedTeamRow.value = row
  getMemberList(row.teamId)
}

function handleAdd() {
  teamSelectVisible.value = true
}

function onTeamSelected(rows: any[]) {
  rows.forEach((team) => {
    const data = {
      teamId: team.teamId,
      teamCode: team.teamCode,
      teamName: team.teamName,
      planId: props.planId,
      calendarType: props.calendarType,
    }
    addPlanteam(data).then(() => {
      getTeamList()
    })
  })
}

function handleDelete(row: any) {
  const recordId = row.recordId
  proxy.$modal.confirm('是否确认删除班组？').then(() => {
    return delPlanteam(recordId)
  }).then(() => {
    getTeamList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

getTeamList()
</script>
