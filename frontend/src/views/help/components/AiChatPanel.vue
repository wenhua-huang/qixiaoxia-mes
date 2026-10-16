<template>
  <el-drawer :model-value="modelValue" size="520px" :with-header="false" destroy-on-close
    @update:model-value="$emit('update:modelValue', $event)" @open="handleOpen">
    <div class="chat-wrap">
      <div class="chat-header">
        <span class="chat-title">🤖 AI 助手</span>
        <span class="chat-tip">只回答系统使用问题</span>
        <el-button size="small" type="primary" plain @click="newSession">新建会话</el-button>
      </div>
      <div class="chat-body">
        <!-- 会话列表（多会话，仅本人的） -->
        <div class="session-col">
          <el-scrollbar>
            <div v-for="s in sessions" :key="s.sessionId"
              :class="['session-item', s.sessionId === activeSessionId ? 'active' : '']"
              @click="selectSession(s.sessionId)">
              <span class="session-title">{{ s.title || '新会话' }}</span>
              <el-icon class="session-del" @click.stop="removeSession(s.sessionId)"><Close /></el-icon>
            </div>
            <el-empty v-if="!sessions.length" description="暂无会话" :image-size="40" />
          </el-scrollbar>
        </div>
        <!-- 消息区 + 输入 -->
        <div class="chat-main">
          <AiChatMessages :messages="messages" :streaming-text="streamingText" :answering="answering" @ask="quickAsk" />
          <div class="input-bar">
            <el-input v-model="question" type="textarea" :autosize="{ minRows: 1, maxRows: 4 }"
              placeholder="输入问题，Enter 发送，Shift+Enter 换行" :disabled="answering"
              @keydown.enter.exact.prevent="send" />
            <el-button type="primary" :disabled="!question.trim() || answering" :loading="answering" @click="send">
              发送
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Close } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AiChatMessages from './AiChatMessages.vue'
import {
  listSessions, createSession, deleteSession, listMessages, askStream,
  type AiChatSession, type AiChatMessage
} from '@/api/mes/ai/chat'

defineProps<{ modelValue: boolean }>()
defineEmits<{ (e: 'update:modelValue', v: boolean): void }>()

const sessions = ref<AiChatSession[]>([])
const activeSessionId = ref<number | null>(null)
const messages = ref<AiChatMessage[]>([])
const question = ref('')
const answering = ref(false)
const streamingText = ref('')

/** 抽屉打开时加载会话列表，默认选中最近一个 */
async function handleOpen() {
  const res: any = await listSessions()
  sessions.value = res.data || []
  if (sessions.value.length && !activeSessionId.value) {
    selectSession(sessions.value[0].sessionId)
  }
}

/** 新建会话并选中 */
async function newSession() {
  const res: any = await createSession()
  const session: AiChatSession = res.data
  sessions.value.unshift(session)
  activeSessionId.value = session.sessionId
  messages.value = []
}

async function selectSession(sessionId: number) {
  if (answering.value) return
  activeSessionId.value = sessionId
  const res: any = await listMessages(sessionId)
  messages.value = res.data || []
}

async function removeSession(sessionId: number) {
  await ElMessageBox.confirm('删除该会话及其聊天记录？', '提示', { type: 'warning' })
  await deleteSession(sessionId)
  sessions.value = sessions.value.filter(s => s.sessionId !== sessionId)
  if (activeSessionId.value === sessionId) {
    activeSessionId.value = sessions.value[0]?.sessionId ?? null
    messages.value = activeSessionId.value ? (await listMessages(activeSessionId.value) as any).data || [] : []
  }
}

/** 欢迎页快捷问题 */
function quickAsk(q: string) {
  question.value = q
  send()
}

/** 发送：无会话时先建会话；本地先上屏，再流式接收 */
async function send() {
  const q = question.value.trim()
  if (!q || answering.value) return
  if (!activeSessionId.value) await newSession()
  question.value = ''
  messages.value.push(localMessage('USER', q))
  answering.value = true
  streamingText.value = ''
  await askStream(activeSessionId.value!, q, {
    onDelta: (text) => { streamingText.value += text },
    onDone: () => finalizeAnswer(),
    onError: (msg) => finalizeAnswer('⚠️ ' + msg)
  })
}

/** 回答结束：气泡落为正式消息；首问后刷新会话标题 */
function finalizeAnswer(errorText?: string) {
  const content = errorText || streamingText.value || '⚠️ 没有收到回答，请重试'
  messages.value.push(localMessage('ASSISTANT', content))
  streamingText.value = ''
  answering.value = false
  refreshSessionTitle()
}

async function refreshSessionTitle() {
  const res: any = await listSessions()
  sessions.value = res.data || []
}

/** 本地临时消息（msgId 负数避免与库内主键冲突） */
let localId = -1
function localMessage(role: 'USER' | 'ASSISTANT', content: string): AiChatMessage {
  return { msgId: localId--, sessionId: activeSessionId.value ?? 0, role, content, createTime: '' }
}
</script>

<style scoped>
.chat-wrap { display: flex; flex-direction: column; height: 100%; }
.chat-header {
  display: flex; align-items: center; gap: 12px; padding: 14px 16px;
  border-bottom: 1px solid #ebeef5; background: #fff;
}
.chat-title { font-size: 16px; font-weight: 600; }
.chat-tip { flex: 1; font-size: 12px; color: #909399; }
.chat-body { flex: 1; display: flex; min-height: 0; }
.session-col { width: 148px; border-right: 1px solid #ebeef5; background: #fafafa; }
.session-item {
  display: flex; align-items: center; padding: 10px 10px; cursor: pointer;
  font-size: 13px; color: #606266; border-bottom: 1px solid #f0f0f0;
}
.session-item.active { background: #ecf5ff; color: #409eff; }
.session-title { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.session-del { visibility: hidden; }
.session-item:hover .session-del { visibility: visible; }
.chat-main { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.input-bar { display: flex; gap: 8px; padding: 12px; border-top: 1px solid #ebeef5; background: #fff; }
</style>
