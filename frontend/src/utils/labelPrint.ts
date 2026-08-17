/**
 * 标签打印公共模块（批次标签 / 纸卷标签 / 流转卡标签共用）
 *
 * 双通道策略：
 *  1. C-Lodop 打印控件（推荐，车间电脑安装一次）：检测到本机控件服务时
 *     直接发送到系统默认打印机，一键出标签、不弹对话框；
 *  2. 浏览器打印降级：未安装控件时按标签纸规格（@page size 精确到 mm）
 *     排版，在系统打印对话框中选中标签打印机即可，页面尺寸与标签纸一致。
 *
 * 标签规格默认 60×40mm，可在打印入口切换（记住在浏览器本地）。
 */
import { ref, watch } from 'vue'
import QRCode from 'qrcode'
import { ElMessage } from 'element-plus'

export interface LabelSpec {
  key: string
  label: string
  widthMm: number
  heightMm: number
}

/** 一张标签的内容：二维码载荷 + 标题（一般是单号）+ 属性行 */
export interface LabelItem {
  payload: string
  headline: string
  fields: (string | null | undefined)[]
}

export type PrintChannel = 'clodop' | 'browser'

export const LABEL_SPECS: LabelSpec[] = [
  { key: '60x40', label: '60×40mm', widthMm: 60, heightMm: 40 },
  { key: '50x30', label: '50×30mm', widthMm: 50, heightMm: 30 },
  { key: '70x50', label: '70×50mm', widthMm: 70, heightMm: 50 },
  { key: '40x30', label: '40×30mm', widthMm: 40, heightMm: 30 }
]

const DEFAULT_SPEC_KEY = '60x40'
const SPEC_STORAGE_KEY = 'qxx:label-spec'

function loadSpecKey(): string {
  const saved = localStorage.getItem(SPEC_STORAGE_KEY)
  return LABEL_SPECS.some(s => s.key === saved) ? saved! : DEFAULT_SPEC_KEY
}

/** 全局标签规格（跨页面共享，改动即持久化），供打印入口的下拉框 v-model 使用 */
export const labelSpecKey = ref<string>(loadSpecKey())

watch(labelSpecKey, v => localStorage.setItem(SPEC_STORAGE_KEY, v))

export function currentSpec(): LabelSpec {
  return LABEL_SPECS.find(s => s.key === labelSpecKey.value) || LABEL_SPECS[0]
}

function esc(s: unknown): string {
  return String(s ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

/** 尺寸随标签高度等比缩放，保证小规格上依然放得下二维码 + 标题 + 属性行 */
function labelCss(spec: LabelSpec): string {
  const qrMm = Math.round(spec.heightMm * 0.66)
  const headMm = (spec.heightMm * 0.085).toFixed(1)
  const fieldMm = (spec.heightMm * 0.068).toFixed(1)
  return `*{margin:0;padding:0}
body{font-family:sans-serif;-webkit-print-color-adjust:exact}
.lb{width:100%;height:100%;border-collapse:collapse;table-layout:fixed}
.c{width:${qrMm + 3}mm;text-align:center;vertical-align:middle}
.c img{width:${qrMm}mm;height:${qrMm}mm}
.t{vertical-align:middle;padding:0 1.5mm;overflow:hidden;word-break:break-all}
.h{font-weight:bold;font-size:${headMm}mm;margin-bottom:0.8mm}
.f{font-size:${fieldMm}mm;color:#222;line-height:1.3}`
}

async function buildLabelBlock(item: LabelItem, spec: LabelSpec): Promise<string> {
  const dataUrl = await QRCode.toDataURL(item.payload, { width: 240, margin: 1 })
  const fields = item.fields
    .filter(f => f != null && String(f).trim() !== '')
    .map(f => `<div class="f">${esc(f)}</div>`)
    .join('')
  return `<div class="label" style="width:${spec.widthMm}mm;height:${spec.heightMm}mm;box-sizing:border-box;padding:1.5mm;overflow:hidden">
    <table class="lb"><tr>
      <td class="c"><img src="${dataUrl}"/></td>
      <td class="t"><div class="h">${esc(item.headline)}</div>${fields}</td>
    </tr></table>
  </div>`
}

// ==================== C-Lodop 控件检测 ====================

declare global {
  interface Window {
    getCLodop?: () => any
  }
}

/** 控件服务地址：C-Lodop 本机 HTTP 服务，https 站点下 localhost 豁免混合内容拦截 */
const C_LODOP_URLS = [
  'http://localhost:8000/CLodopfuncs.js?priority=1',
  'https://localhost:8443/CLodopfuncs.js?priority=2'
]

function loadScript(url: string, timeoutMs = 800): Promise<void> {
  return new Promise((resolve, reject) => {
    const el = document.createElement('script')
    const timer = setTimeout(() => { el.remove(); reject(new Error('timeout')) }, timeoutMs)
    el.src = url
    el.onload = () => { clearTimeout(timer); resolve() }
    el.onerror = () => { clearTimeout(timer); el.remove(); reject(new Error('load error')) }
    document.head.appendChild(el)
  })
}

let lodopProbe: Promise<any> | null = null

function probeCLodop(): Promise<any> {
  // 两个端口并行探测，谁先成功用谁；全部失败（未安装控件）则很快返回 null
  return Promise.any(
    C_LODOP_URLS.map(async url => {
      await loadScript(url)
      if (typeof window.getCLodop === 'function') {
        const lodop = window.getCLodop()
        if (lodop) return lodop
      }
      throw new Error('not available')
    })
  ).catch(() => null)
}

export function getCLodop(): Promise<any> {
  lodopProbe ??= probeCLodop()
  return lodopProbe
}

// ==================== 打印通道 ====================

function printViaClodop(lodop: any, title: string, spec: LabelSpec, blocks: string[]): void {
  lodop.PRINT_INIT(title)
  // 页宽页高单位 0.1mm；1 = 纵向按指定宽高
  lodop.SET_PRINT_PAGESIZE(1, spec.widthMm * 10, spec.heightMm * 10, '')
  blocks.forEach((html, i) => {
    if (i) lodop.NEWPAGEA()
    lodop.ADD_PRINT_HTM(0, 0, '100%', '100%', `<style>${labelCss(spec)}</style>${html}`)
  })
  lodop.PRINT()
}

async function printViaBrowser(spec: LabelSpec, blocks: string[]): Promise<void> {
  // 用新窗口打印（离屏 iframe 在部分 Chrome 配置下 print() 不弹对话框，会表现为"点了没反应"）。
  // 窗口在用户点击链路内同步打开，避免被弹窗拦截。
  const w = window.open('', '_blank')
  if (!w) throw new Error('浏览器拦截了打印窗口，请允许本站弹出窗口后重试')

  const html = `<html><head><title>标签打印</title><style>
@page{size:${spec.widthMm}mm ${spec.heightMm}mm;margin:0}
${labelCss(spec)}
.label{page-break-after:always}
.label:last-child{page-break-after:auto}
</style></head><body>${blocks.join('')}</body></html>`

  w.document.open()
  w.document.write(html)
  w.document.close()

  // 等二维码（data URL）解码完成再触发打印，避免打出空图
  await Promise.all(
    Array.from(w.document.images).map(img =>
      img.complete ? Promise.resolve() : new Promise<void>(r => { img.onload = () => r(); img.onerror = () => r() })
    )
  )

  // print() 在 Chrome/Edge 中会阻塞到对话框关闭，之后关闭这个临时窗口；Safari 异步则延迟关闭
  setTimeout(() => {
    try {
      w.focus()
      w.print()
      setTimeout(() => !w.closed && w.close(), 500)
    } catch {
      /* 用户可在已打开的标签页手动打印 */
    }
  }, 150)
}

/**
 * 打印二维码标签。不抛异常：失败时弹错误提示并返回 null。
 * @returns 实际使用的通道（'clodop' 一键静默 / 'browser' 浏览器打印对话框），失败返回 null
 */
export async function printQrLabels(opts: {
  title: string
  items: LabelItem[]
  spec?: LabelSpec
}): Promise<PrintChannel | null> {
  const spec = opts.spec || currentSpec()
  if (!opts.items.length) {
    ElMessage.warning('没有可打印的标签')
    return null
  }
  try {
    const blocks = await Promise.all(opts.items.map(i => buildLabelBlock(i, spec)))
    const lodop = await getCLodop()
    if (lodop) {
      printViaClodop(lodop, opts.title, spec, blocks)
      return 'clodop'
    }
    await printViaBrowser(spec, blocks)
    return 'browser'
  } catch (e: any) {
    ElMessage.error(e?.message || '打印失败')
    return null
  }
}
