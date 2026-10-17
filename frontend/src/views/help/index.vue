<template>
  <div class="help-wrap">
    <el-container class="help-container">
      <!-- 左侧目录 -->
      <el-aside width="240px" class="help-aside">
        <div class="aside-header">
          <el-icon class="aside-icon"><Document /></el-icon>
          <span>帮助中心</span>
        </div>
        <el-scrollbar class="aside-scroll">
          <el-menu :default-active="activeFile" @select="handleSelect">
            <template v-for="group in docTree" :key="group.title">
              <el-menu-item-group :title="group.title">
                <el-menu-item
                  v-for="doc in group.items"
                  :key="doc.file"
                  :index="doc.file"
                >
                  <el-icon><Reading /></el-icon>
                  <span>{{ doc.title }}</span>
                </el-menu-item>
              </el-menu-item-group>
            </template>
          </el-menu>
        </el-scrollbar>
      </el-aside>

      <!-- 右侧内容 -->
      <el-main class="help-main">
        <div v-loading="loading" class="loading-mask-wrap">
          <div v-if="loadError" class="load-error">
            <el-empty :description="loadError">
              <el-button type="primary" @click="loadDoc(activeFile)">重试</el-button>
            </el-empty>
          </div>
          <div v-else-if="currentTitle" class="doc-crumb">{{ currentTitle }}</div>
          <!-- markdown-body 是 github-markdown-css 的样式作用域 -->
          <div class="markdown-body help-content" v-html="html" @click="handleContentClick"></div>
        </div>
      </el-main>
    </el-container>

    <!-- AI 助手：右下角悬浮入口 + 聊天抽屉 -->
    <div class="ai-float-btn" @click="aiChatVisible = true">
      <el-icon :size="18"><ChatDotRound /></el-icon>
      <span>AI 助手</span>
    </div>
    <AiChatPanel v-model="aiChatVisible" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { marked } from 'marked'
import { Document, Reading, ChatDotRound } from '@element-plus/icons-vue'
import AiChatPanel from './components/AiChatPanel.vue'
// 用纯浅色版：自适应版在系统深色模式下会把文字切为浅色，白底气泡/文档区将不可读
import 'github-markdown-css/github-markdown-light.css'

interface DocItem {
  file: string
  title: string
}

interface DocGroup {
  title: string
  items: DocItem[]
}

/**
 * 文档目录树。
 * 新增文档步骤：
 *   1. 在 public/help/ 下新建 xxx.md
 *   2. 在下面数组对应分组加一条 { file: 'xxx.md', title: '标题' }
 *   3. 完成，无需改路由/菜单/构建
 */
const docTree = ref<DocGroup[]>([
  {
    title: '入门',
    items: [
      { file: 'quick-start.md', title: '快速上手' },
      { file: 'pro-workflow.md', title: '订单到交付：销售订单→生产→发货' },
      { file: 'app-guide.md', title: '手机端使用指南' }
    ]
  },
  {
    title: '主数据管理',
    items: [
      { file: 'md-master-data.md', title: '物料、客户、供应商等基础档案' }
    ]
  },
  {
    title: '仓库管理',
    items: [
      { file: 'wm-issue.md', title: '生产领料与退料' },
      { file: 'wm-stock-taking.md', title: '库存盘点（方案与任务）' },
      { file: 'wm-transfer.md', title: '转移调拨与杂项出入库' },
      { file: 'wm-outsource.md', title: '外协管理' }
    ]
  },
  {
    title: '生产管理',
    items: [
      { file: 'pro-workorder.md', title: '工单/排产/报工/流转卡操作手册' },
      { file: 'pro-exception.md', title: '生产异常：上报与处理' }
    ]
  },
  {
    title: '质量管理',
    items: [
      { file: 'qc-quality-inspection.md', title: '来料/过程/出货/退料检验（IQC/IPQC/OQC/RQC）' }
    ]
  },
  {
    title: '采购管理',
    items: [
      { file: 'pur-order.md', title: '采购订单、入库与退货' }
    ]
  },
  {
    title: '日历管理',
    items: [
      { file: 'cal-schedule.md', title: '班组、排班与节假日' }
    ]
  },
  {
    title: '设备与工装',
    items: [
      { file: 'dv-machinery.md', title: '设备类型与设备档案' },
      { file: 'tm-tool.md', title: '工装类型与清单' }
    ]
  },
  {
    title: '系统管理',
    items: [
      { file: 'sys-admin.md', title: '账号、编码与平台维护（管理员）' }
    ]
  },
  {
    title: '操作指南',
    items: [
      { file: 'qr-code-scan.md', title: '二维码扫码作业指南' }
    ]
  },
  {
    title: '开放接口',
    items: [
      { file: 'crm-order-api.md', title: '销售订单 API 对接指南' }
    ]
  }
])

const activeFile = ref<string>('quick-start.md')
const aiChatVisible = ref<boolean>(false)
const html = ref<string>('')
const loading = ref(false)
const loadError = ref('')
const currentTitle = ref('')

/** 在目录树中查找文件对应的标题 */
function findTitle(file: string): string {
  for (const group of docTree.value) {
    const hit = group.items.find((d) => d.file === file)
    if (hit) return hit.title
  }
  return ''
}

/** 拉取并渲染一篇文档 */
async function loadDoc(file: string) {
  if (!file) return
  loading.value = true
  loadError.value = ''
  try {
    const resp = await fetch(`/help/${file}`)
    if (!resp.ok) {
      throw new Error(`文档加载失败（${resp.status}）`)
    }
    const md = await resp.text()
    html.value = await marked.parse(md)
    currentTitle.value = findTitle(file)
  } catch (e: any) {
    html.value = ''
    currentTitle.value = ''
    loadError.value = e?.message || '文档加载失败'
  } finally {
    loading.value = false
  }
}

/** 目录选中事件 */
function handleSelect(index: string) {
  activeFile.value = index
  loadDoc(index)
}

/**
 * 拦截文档内的相对 .md 链接（如 [xx](pro-workflow.md)），
 * 改为在当前页切换文档，避免跳出 SPA 直接下载/打开原始 md。
 */
function handleContentClick(e: MouseEvent) {
  const link = (e.target as HTMLElement)?.closest?.('a')
  if (!link) return
  const href = link.getAttribute('href') || ''
  // 去掉 hash 片段后再判断是否为站内 md 文档链接
  const bareHref = href.split('#')[0]
  if (!bareHref.endsWith('.md') || /^(https?:)?\/\//.test(href)) return
  e.preventDefault()
  const file = bareHref.split('/').pop() as string
  if (findTitle(file)) {
    handleSelect(file)
  } else {
    loadDoc(file)
  }
}

onMounted(() => {
  loadDoc(activeFile.value)
})
</script>

<style lang="scss" scoped>
.help-wrap {
  height: calc(100vh - 84px);
}

.help-container {
  height: 100%;
  background: #f5f7fa;
}

/* AI 助手悬浮入口 */
.ai-float-btn {
  position: fixed;
  right: 32px;
  bottom: 32px;
  z-index: 2000;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 12px 20px;
  background: #409eff;
  color: #fff;
  font-size: 14px;
  border-radius: 24px;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.4);
  transition: transform 0.15s;

  &:hover {
    transform: translateY(-2px);
    background: #337ecc;
  }
}

.help-aside {
  background: #fff;
  border-right: 1px solid #e4e7ed;

  .aside-header {
    height: 50px;
    display: flex;
    align-items: center;
    padding: 0 18px;
    font-size: 16px;
    font-weight: 600;
    color: #303133;
    border-bottom: 1px solid #ebeef5;

    .aside-icon {
      font-size: 18px;
      margin-right: 8px;
      color: var(--el-color-primary);
    }
  }

  .aside-scroll {
    height: calc(100% - 50px);
  }

  :deep(.el-menu) {
    border-right: none;
  }
}

.help-main {
  padding: 0;
  overflow: auto;

  .loading-mask-wrap {
    min-height: 100%;
    background: #fff;
  }

  .doc-crumb {
    padding: 16px 40px 0;
    font-size: 13px;
    color: #909399;
  }

  .help-content {
    padding: 16px 40px 60px;
    max-width: 960px;
  }

  .load-error {
    padding: 80px 0;
  }
}
</style>
