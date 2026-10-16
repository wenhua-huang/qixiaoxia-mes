<template>
  <div ref="scrollRef" class="msg-scroll">
    <!-- 空会话欢迎语 + 快捷问题 -->
    <div v-if="!messages.length && !streamingText" class="welcome">
      <div class="welcome-title">👋 你好，我是企小侠MES助手</div>
      <div class="welcome-sub">问我系统怎么用，比如：</div>
      <div v-for="q in QUICK_QUESTIONS" :key="q" class="quick-chip" @click="$emit('ask', q)">{{ q }}</div>
    </div>

    <div v-for="m in messages" :key="m.msgId" :class="['msg-row', m.role === 'USER' ? 'right' : 'left']">
      <div :class="['bubble', m.role === 'USER' ? 'bubble-user' : 'bubble-ai']">
        <div v-if="m.role === 'USER'" class="plain">{{ m.content }}</div>
        <div v-else class="markdown-body ai-md" v-html="renderMd(m.content)"></div>
      </div>
    </div>

    <!-- 流式输出中的气泡 -->
    <div v-if="answering" class="msg-row left">
      <div class="bubble bubble-ai">
        <div v-if="streamingText" class="markdown-body ai-md" v-html="renderMd(streamingText)"></div>
        <div v-else class="thinking">思考中<span class="dot">…</span></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import type { AiChatMessage } from '@/api/mes/ai/chat'

const QUICK_QUESTIONS = ['怎么报生产异常？', '质检不合格怎么处理？', '怎么新建生产工单？']

const props = defineProps<{
  messages: AiChatMessage[]
  streamingText: string
  answering: boolean
}>()

defineEmits<{ (e: 'ask', question: string): void }>()

const scrollRef = ref<HTMLElement>()

/** AI 输出经 marked 渲染 + DOMPurify 消毒后再上屏，防提示注入 XSS */
function renderMd(text: string): string {
  return DOMPurify.sanitize(marked.parse(text) as string)
}

/** 新消息/新流式片段时滚到底部 */
watch(
  () => [props.messages.length, props.streamingText.length],
  async () => {
    await nextTick()
    if (scrollRef.value) scrollRef.value.scrollTop = scrollRef.value.scrollHeight
  }
)
</script>

<style scoped>
.msg-scroll { flex: 1; overflow-y: auto; padding: 16px; background: #f5f6f7; }
.welcome { padding: 24px 8px; }
.welcome-title { font-size: 16px; font-weight: 600; color: #303133; margin-bottom: 8px; }
.welcome-sub { font-size: 13px; color: #909399; margin-bottom: 12px; }
.quick-chip {
  display: inline-block; margin: 0 8px 8px 0; padding: 6px 12px; font-size: 13px;
  background: #fff; border: 1px solid #dcdfe6; border-radius: 14px; cursor: pointer; color: #606266;
}
.quick-chip:hover { border-color: #409eff; color: #409eff; }
.msg-row { display: flex; margin-bottom: 12px; }
.msg-row.right { justify-content: flex-end; }
.bubble { max-width: 85%; padding: 10px 14px; border-radius: 10px; font-size: 14px; line-height: 1.6; word-break: break-word; }
.bubble-user { background: #409eff; color: #fff; border-top-right-radius: 2px; }
.bubble-ai { background: #fff; color: #303133; border-top-left-radius: 2px; }
.plain { white-space: pre-wrap; }
.thinking { color: #909399; }
.ai-md :deep(p) { margin: 0 0 8px; }
.ai-md :deep(p:last-child) { margin-bottom: 0; }
.ai-md :deep(ol), .ai-md :deep(ul) { padding-left: 20px; margin: 4px 0; }
.ai-md :deep(table) { font-size: 13px; }
.ai-md { background: transparent; font-size: 14px; }
</style>
