<template>
  <el-dialog
    title="新增用户工位绑定"
    :model-value="showFlag"
    @update:model-value="(v: boolean) => emit('update:showFlag', v)"
    width="640px"
    append-to-body
    :close-on-click-modal="false"
    :show-close="!submitting"
    :close-on-press-escape="!submitting"
    @close="onClose"
  >
    <el-form label-width="92px">
      <el-form-item label="绑定人员" required>
        <div class="pick-row">
          <el-button type="primary" plain icon="Plus" size="small" @click="userSelectVisible = true">
            选择人员
          </el-button>
          <span v-if="!selectedUsers.length" class="pick-hint">请选择（可多选）</span>
        </div>
        <div v-if="selectedUsers.length" class="tag-box">
          <el-tag
            v-for="u in selectedUsers"
            :key="u.userId"
            closable
            type="info"
            @close="removeUser(u.userId)"
          >
            {{ u.nickName || u.userName }}
          </el-tag>
        </div>
      </el-form-item>

      <el-form-item label="绑定工位" required>
        <el-select
          v-model="form.workstationIds"
          multiple
          filterable
          collapse-tags
          collapse-tags-tooltip
          placeholder="请选择工位（可多选）"
          style="width: 100%"
          :loading="optionsLoading"
        >
          <el-option
            v-for="w in options"
            :key="w.workstationId"
            :label="`${w.workstationCode || ''} ${w.workstationName || ''}`.trim()"
            :value="w.workstationId"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="备注">
        <el-input
          v-model="form.remark"
          type="textarea"
          :rows="2"
          maxlength="500"
          show-word-limit
          placeholder="选填，将写入本批绑定（含重新启用）记录"
        />
      </el-form-item>
    </el-form>

    <UserMultiSelect v-model:showFlag="userSelectVisible" @onSelected="onUsersSelected" />

    <template #footer>
      <el-button :disabled="submitting" @click="emit('update:showFlag', false)">取 消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">确 定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts" name="UserWorkstationBindDialog">
import { ref, reactive, watch, getCurrentInstance } from 'vue'
import { ElMessageBox } from 'element-plus'
import UserMultiSelect from '@/components/UserSelect/multi.vue'
import { workstationOptions, batchBindUserWorkstation } from '@/api/mes/pro/userworkstation'
import type { WorkstationOption, UserWorkstationBatchResult } from '@/types/api/mes/pro/userworkstation'

interface SelectedUser { userId: number; userName?: string; nickName?: string }

const props = defineProps<{ showFlag: boolean }>()
const emit = defineEmits<{
  (e: 'update:showFlag', val: boolean): void
  (e: 'success'): void
}>()

const { proxy } = getCurrentInstance() as any

const selectedUsers = ref<SelectedUser[]>([])
const options = ref<WorkstationOption[]>([])
const optionsLoading = ref(false)
const userSelectVisible = ref(false)
const submitting = ref(false)
const form = reactive<{ workstationIds: number[]; remark: string }>({
  workstationIds: [],
  remark: ''
})

watch(() => props.showFlag, async (v) => {
  if (!v) return
  if (!options.value.length) {
    optionsLoading.value = true
    try {
      const res: any = await workstationOptions()
      options.value = res.data || []
    } catch { proxy.$modal.msgError('工位选项加载失败') } finally { optionsLoading.value = false }
  }
}, { immediate: true })

function onUsersSelected(rows: SelectedUser[]) {
  const known = new Set(selectedUsers.value.map(u => u.userId))
  rows.forEach(u => { if (!known.has(u.userId)) selectedUsers.value.push(u) })
}

function removeUser(uid: number) {
  selectedUsers.value = selectedUsers.value.filter(u => u.userId !== uid)
}

function escapeHtml(s: string): string {
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;').replace(/'/g, '&#39;')
}

async function submit() {
  if (!selectedUsers.value.length) { proxy.$modal.msgError('请选择绑定人员'); return }
  if (!form.workstationIds.length) { proxy.$modal.msgError('请选择绑定工位'); return }
  submitting.value = true
  try {
    const res: any = await batchBindUserWorkstation({
      userIds: selectedUsers.value.map(u => u.userId),
      workstationIds: form.workstationIds,
      remark: form.remark || undefined
    })
    const r = (res.data || {}) as UserWorkstationBatchResult
    const msg = `新增 ${r.successCount || 0} 条，重新启用 ${r.reactivatedCount || 0} 条，跳过已绑定 ${r.skipCount || 0} 条`
    if (r.skips && r.skips.length) {
      ElMessageBox.alert(r.skips.slice(0, 20).map(escapeHtml).join('<br/>'), `${msg}（跳过明细）`, {
        dangerouslyUseHTMLString: true
      })
    } else {
      proxy.$modal.msgSuccess(msg)
    }
    emit('success')
    emit('update:showFlag', false)
  } finally { submitting.value = false }
}

function onClose() {
  selectedUsers.value = []
  form.workstationIds = []
  form.remark = ''
  userSelectVisible.value = false
}
</script>

<style scoped>
.pick-row { display: flex; align-items: center; gap: 10px; }
.pick-hint { color: #909399; font-size: 12px; }
.tag-box { margin-top: 8px; display: flex; flex-wrap: wrap; gap: 8px; }
</style>
