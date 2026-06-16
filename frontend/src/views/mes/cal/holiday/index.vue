<template>
    <div class="app-container">
        <el-calendar v-model="date">
            <template #date-cell="{ data }">
                <div @contextmenu.prevent="onRightClick(data)">
                    <el-row>
                        <el-col :span="16">
                            <div class="solar">
                                {{ data.day.split('-')[2] }}
                            </div>
                        </el-col>
                        <el-col :span="8">
                            <el-tag v-if="holidayList.indexOf(data.day) == -1" effect="dark">班</el-tag>
                            <el-tag v-else effect="dark" type="success">休</el-tag>
                        </el-col>
                    </el-row>
                    <el-row>
                        <el-col :span="24">
                            <div class="lunar" :class="{ festival: isFestival(data) }">{{ solarDate2lunar(data.day) }}</div>
                        </el-col>
                    </el-row>
                </div>
            </template>
        </el-calendar>

        <!-- 节假日设置对话框 -->
        <el-dialog :title="title" v-model="open" width="500px" append-to-body>
            <el-form ref="form" :model="form" :rules="rules" label-width="80px">
                <el-row>
                    <el-col :span="12">
                        <el-form-item label="日期" prop="holidayDate">
                            <el-input v-model="form.holidayDate" readonly></el-input>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="类型" prop="holidayType">
                            <el-radio-group v-model="form.holidayType">
                                <el-radio label="HOLIDAY">假</el-radio>
                                <el-radio label="WORKDAY">班</el-radio>
                            </el-radio-group>
                        </el-form-item>
                    </el-col>
                </el-row>
            </el-form>
            <template #footer>
                <div class="dialog-footer">
                    <el-button type="primary" @click="submitForm">确 定</el-button>
                    <el-button @click="cancel">取 消</el-button>
                </div>
            </template>
        </el-dialog>
    </div>
</template>

<script>
import { listHoliday, addHoliday, updateHoliday } from "@/api/mes/cal/holiday"
import { solar2lunar } from '@/utils/cal/calendar'

export default {
    name: "Holiday",
    data() {
        return {
            loading: true,
            date: new Date(),
            holidayList: [],
            workdayList: [],
            holidayMap: {},  // date → full record for looking up existing records
            title: "节假日设置",
            open: false,
            form: {},
            queryParams: {
                theDay: null,
                holidayType: null,
                startTime: null,
                endTime: null,
            },
            rules: {
                holidayType: [
                    { required: true, message: "请选择类型", trigger: "blur" }
                ],
            }
        }
    },
    created() {
        this.getList()
    },
    methods: {
        getList() {
            this.loading = true
            this.holidayList = []
            this.workdayList = []
            this.holidayMap = {}
            listHoliday({ pageNum: 1, pageSize: 9999 }).then(response => {
                if (response.rows != null) {
                    response.rows.forEach(row => {
                        this.holidayMap[row.holidayDate] = row  // store full record for lookup
                        if (row.holidayType == 'HOLIDAY') {
                            this.holidayList.push(row.holidayDate)
                        } else {
                            this.workdayList.push(row.holidayDate)
                        }
                    })
                    // WORKDAY 优先：如果同一日期同时有假和班，以班为准
                    this.workdayList.forEach(day => {
                        const idx = this.holidayList.indexOf(day)
                        if (idx !== -1) this.holidayList.splice(idx, 1)
                    })
                }
                this.loading = false
            })
        },
        onRightClick(data) {
            this.open = true
            this.reset()
            this.form.holidayDate = data.day
            // 如果该日期已有记录，预填现有数据用于更新
            const existing = this.holidayMap[data.day]
            if (existing) {
                this.form.holidayId = existing.holidayId
                this.form.holidayType = existing.holidayType
                this.form.holidayName = existing.holidayName
            }
        },
        submitForm() {
            this.$refs["form"].validate(valid => {
                if (valid) {
                    // 根据类型自动生成节假日名称
                    if (!this.form.holidayName) {
                        this.form.holidayName = this.form.holidayType === 'HOLIDAY' ? '节假日' : '调休工作日'
                    }
                    const action = this.form.holidayId ? updateHoliday(this.form) : addHoliday(this.form)
                    action.then(() => {
                        this.$modal.msgSuccess("设置成功")
                        this.open = false
                        this.getList()
                    })
                }
            })
        },
        cancel() {
            this.open = false
            this.reset()
        },
        reset() {
            this.form = {
                holidayId: null,
                holidayDate: null,
                holidayType: 'HOLIDAY',
                startTime: new Date().setHours(0),
                endTime: new Date().setHours(23)
            }
            this.resetForm("form")
        },
        isFestival(slotData) {
            const solarDayArr = slotData.day.split('-')
            const lunarDay = solar2lunar(solarDayArr[0], solarDayArr[1], solarDayArr[2])
            const festAndTerm = [
                lunarDay.festival || '',
                lunarDay.lunarFestival || '',
                lunarDay.Term || ''
            ].join('')
            return festAndTerm !== ''
        },
        solarDate2lunar(solarDate) {
            const solar = solarDate.split('-')
            const lunar = solar2lunar(solar[0], solar[1], solar[2])
            const lunarMD = lunar.IMonthCn + lunar.IDayCn
            const festAndTerm = [
                lunar.festival || '',
                lunar.lunarFestival || '',
                lunar.Term || ''
            ].join('')
            return festAndTerm === '' ? lunarMD : festAndTerm
        }
    }
}
</script>

<style>
/**本月周末设置为红色*/
.el-calendar-table .current:nth-last-child(-n+2) .solar {
    color: red;
}
/**本月农历设置为灰色*/
.el-calendar-table .current .lunar {
    color: #606266;
    font-size: small;
}
/**本月农历节日设置为绿色*/
.el-calendar-table .current .lunar.festival {
    color: green;
    font-size: small;
}
/**节假日背景设置为绿色 */
.el-calendar-table .holiday {
    background-color: #88E325;
}
</style>
