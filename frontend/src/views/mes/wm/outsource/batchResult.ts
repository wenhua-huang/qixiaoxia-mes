import { ElMessage, ElMessageBox } from 'element-plus'

/** 批量操作结果：全成功提示成功数；有失败弹窗列出单号+原因（纯文本，不渲染 HTML） */
export interface BatchResult {
  success: number
  failed: number
  failures?: { orderId?: number; orderCode?: string; reason?: string }[]
}

export async function showBatchResult(promise: Promise<{ data: BatchResult }>, successLabel: string) {
  const res = await promise
  const r = res.data || { success: 0, failed: 0, failures: [] }
  if (r.failed > 0) {
    const lines = (r.failures || []).map(f => `${f.orderCode || '#' + f.orderId}：${f.reason}`).join('\n')
    return ElMessageBox.alert(`成功 ${r.success} 张，失败 ${r.failed} 张：\n${lines}`, '批量操作结果', {
      type: 'warning',
      customStyle: { whiteSpace: 'pre-wrap' }
    })
  }
  ElMessage.success(`批量${successLabel}成功，共 ${r.success} 张`)
}
