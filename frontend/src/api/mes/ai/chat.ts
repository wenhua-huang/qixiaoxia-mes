import request from '@/utils/request'
import { getToken } from '@/utils/auth'

/** AI 问答会话 */
export interface AiChatSession {
  sessionId: number
  userId: number
  title: string
  createTime: string
  updateTime: string
}

/** AI 问答消息 */
export interface AiChatMessage {
  msgId: number
  sessionId: number
  role: 'USER' | 'ASSISTANT'
  content: string
  createTime: string
}

/** 流式回答回调 */
export interface AskStreamHandlers {
  onDelta: (text: string) => void
  onDone: () => void
  onError: (msg: string) => void
}

/** 我的会话列表 */
export function listSessions() {
  return request({ url: '/mes/ai/chat/sessions', method: 'get' })
}

/** 新建会话 */
export function createSession() {
  return request({ url: '/mes/ai/chat/sessions', method: 'post', headers: { repeatSubmit: false } })
}

/** 删除会话 */
export function deleteSession(sessionId: number) {
  return request({ url: '/mes/ai/chat/sessions/' + sessionId, method: 'delete' })
}

/** 会话历史消息 */
export function listMessages(sessionId: number) {
  return request({ url: '/mes/ai/chat/messages/' + sessionId, method: 'get' })
}

/**
 * 提问（SSE 流式）。axios 不支持流式读取，用 fetch + ReadableStream 手解 SSE。
 * 后端事件统一 {"type":"delta|done|error","content":"..."}，按空行分包。
 */
export async function askStream(sessionId: number, question: string, handlers: AskStreamHandlers) {
  try {
    const resp = await fetch(import.meta.env.VITE_APP_BASE_API + '/mes/ai/chat/ask', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + getToken()
      },
      body: JSON.stringify({ sessionId, question })
    })
    // 校验类失败（限流/会话不存在）走 RuoYi JSON 错误体，不是 SSE 流
    const contentType = resp.headers.get('content-type') || ''
    if (!resp.ok || contentType.includes('application/json')) {
      const err = await resp.json().catch(() => null)
      handlers.onError((err && err.msg) || '请求失败（HTTP ' + resp.status + '）')
      return
    }
    await readSseBody(resp, handlers)
  } catch (e: any) {
    handlers.onError('网络异常，请稍后重试（' + (e && e.message ? e.message : e) + '）')
  }
}

/**
 * 逐行读取 SSE 流并按 data: 包分发。
 * 完成/出错以「事件」为准而非连接关闭：经 Vite/nginx 代理时后端 socket 关闭
 * 不一定被代理转发为响应结束，只靠 reader done 会卡住；收到 done/error 事件
 * 立即收尾并主动 cancel 连接。finish 保证只收尾一次。
 */
async function readSseBody(resp: Response, handlers: AskStreamHandlers) {
  const reader = resp.body!.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  let finished = false
  const finish = (cb: () => void) => {
    if (finished) return
    finished = true
    cb()
  }
  for (;;) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    let idx
    while ((idx = buffer.indexOf('\n\n')) !== -1) {
      const packet = buffer.slice(0, idx)
      buffer = buffer.slice(idx + 2)
      dispatchPacket(packet, handlers, finish)
    }
    if (finished) {
      await reader.cancel().catch(() => undefined)
      return
    }
  }
  if (buffer.trim()) dispatchPacket(buffer, handlers, finish)
  finish(handlers.onDone)
}

/** 解析单个 SSE 包（可能多行，只取 data: 行）；done/error 为终止事件 */
function dispatchPacket(packet: string, handlers: AskStreamHandlers, finish: (cb: () => void) => void) {
  for (const line of packet.split('\n')) {
    if (!line.startsWith('data:')) continue
    try {
      const evt = JSON.parse(line.slice(5).trim())
      if (evt.type === 'delta') handlers.onDelta(evt.content)
      else if (evt.type === 'done') finish(handlers.onDone)
      else if (evt.type === 'error') finish(() => handlers.onError(evt.content))
    } catch {
      // 半包/坏包忽略，下一段流会继续
    }
  }
}
