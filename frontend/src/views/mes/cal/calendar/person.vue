<template>
  <div class="app-container">
    <el-container>
      <el-header height="40px">
        <el-form :model="form" size="small" :inline="true" label-width="100px">
          <el-form-item label="人员" prop="nickName">
            <el-input v-model="form.nickName" placeholder="请选择查询的人员" clearable readonly>
              <template #append>
                <el-button @click="handleQuery" icon="Search" />
              </template>
            </el-input>
            <UserSingleSelect v-model:showFlag="userSelectVisible" @onSelected="onUserSelected" />
          </el-form-item>
          <el-form-item label="日期" prop="date">
            <el-date-picker v-model="form.date" type="date" @change="changeDate" placeholder="选择日期" />
          </el-form-item>
        </el-form>
      </el-header>
      <el-main>
        <el-calendar v-loading="loading" v-model="date">
          <template #date-cell="{ data }">
            <div>
              <el-row>
                <el-col :span="6"><div class="solar">{{ data.day.split('-')[2] }}</div></el-col>
                <el-col :span="12"><div class="lunar" :class="{ festival: isFestival(data) }">{{ solarDate2lunar(data.day) }}</div></el-col>
                <el-col :span="6">
                  <el-tag v-if="holidayList.indexOf(data.day) === -1" effect="dark">班</el-tag>
                  <el-tag v-else effect="dark" type="success">休</el-tag>
                </el-col>
              </el-row>
              <el-row v-for="cd in calendarDayList" :key="cd.theDay">
                <el-col :span="24" v-if="cd.theDay === data.day && holidayList.indexOf(data.day) === -1">
                  <div v-for="ts in cd.teamShifts" :key="ts.orderNum" class="grid-content">
                    <el-button v-if="ts.orderNum === 1" type="success" icon="Sunrise">{{ ts.teamName }}</el-button>
                    <el-button v-if="ts.orderNum === 2 && cd.shiftType === 'SHIFT_THREE'" type="warning" icon="Sunny">{{ ts.teamName }}</el-button>
                    <el-button v-if="ts.orderNum === 2 && cd.shiftType === 'SHIFT_TWO'" type="info" icon="Moon">{{ ts.teamName }}</el-button>
                    <el-button v-if="ts.orderNum === 3 && cd.shiftType === 'SHIFT_THREE'" type="info" icon="Moon">{{ ts.teamName }}</el-button>
                  </div>
                </el-col>
              </el-row>
            </div>
          </template>
        </el-calendar>
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { listHoliday } from '@/api/mes/cal/holiday'
import { listCalendars } from '@/api/mes/cal/calendar'
import { solar2lunar } from '@/utils/cal/calendar'
import UserSingleSelect from '@/components/UserSelect/single.vue'

const form = reactive({ date: null as Date | null, nickName: '' as string })
const loading = ref(true)
const date = ref(new Date())
const holidayList = ref<string[]>([])
const workdayList = ref<string[]>([])
const calendarDayList = ref<any[]>([])
const userSelectVisible = ref(false)

const teamShiftQueryParams = reactive<any>({ queryType: 'USER', date: '' })
const queryParams = reactive({ theDay: null, holidayType: null, startTime: null, endTime: null })

function getList() {
  loading.value = true
  holidayList.value = []
  workdayList.value = []
  listHoliday(queryParams).then((response: any) => {
    if (response.data != null) {
      response.data.forEach((d: any) => {
        if (d.holidayType === 'HOLIDAY') holidayList.value.push(d.theDay)
        else workdayList.value.push(d.theDay)
      })
    }
    loading.value = false
  })
}

function changeDate(val: Date) { date.value = val }
function handleQuery() { userSelectVisible.value = true }

function onUserSelected(row: any) {
  form.nickName = row.nickName
  loading.value = true
  teamShiftQueryParams.userId = row.userId
  teamShiftQueryParams.date = fmtDate(date.value)
  listCalendars(teamShiftQueryParams).then((response: any) => {
    calendarDayList.value = response.data; loading.value = false
  })
}

function fmtDate(d: Date) {
  return d.getFullYear() + '-' + (d.getMonth() + 1) + '-' + d.getDate()
}

watch(date, (newVal) => {
  teamShiftQueryParams.date = fmtDate(newVal)
  loading.value = true
  listCalendars(teamShiftQueryParams).then((response: any) => {
    calendarDayList.value = response.data; loading.value = false
  })
})

function isFestival(slotData: any) {
  const arr = slotData.day.split('-')
  const ld = solar2lunar(arr[0], arr[1], arr[2])
  return !!(ld.festival || ld.lunarFestival || ld.Term)
}

function solarDate2lunar(solarDate: string) {
  const solar = solarDate.split('-')
  const ld = solar2lunar(solar[0], solar[1], solar[2])
  const fest = [ld.festival, ld.lunarFestival, ld.Term].filter(Boolean).join('')
  return fest || (ld.IMonthCn + ld.IDayCn)
}

getList()
</script>

<style>
.grid-content { padding: 5px 0; }
.el-group-list.el-radio-group { display: flex; flex-direction: column; align-items: stretch; }
.el-group-list.el-radio-group .el-radio-button:first-child .el-radio-button__inner,
.el-group-list.el-radio-group .el-radio-button:last-child .el-radio-button__inner,
.el-group-list.el-radio-group .el-radio-button__inner { border-radius: 0px !important; border: none !important; }
.el-group-list.el-radio-group .el-radio-button { border-bottom: 1px solid #F7F7F7 !important; }
.el-group-list.el-radio-group { border: 1px solid #dcdfe6; }
.el-group-list.el-radio-group > label > span { width: 100%; text-align: left; padding-left: 20px; }
.el-calendar-table .current:nth-last-child(-n+2) .solar { color: red; font-size: small; }
.el-calendar-table .current .lunar { color: #606266; font-size: small; }
.el-calendar-table .current .lunar.festival { color: green; font-size: small; }
.el-calendar-table .holiday { background-color: #88E325; }
/* Element Plus 默认 height:85px 定死 → 改为自适应 */
.el-calendar { --el-calendar-cell-width: auto; }
.el-calendar-table { table-layout: auto !important; }
.el-calendar-table .el-calendar-day { height: auto !important; min-height: 85px; }
</style>
