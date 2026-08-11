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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { marked } from 'marked'
import { Document, Reading } from '@element-plus/icons-vue'
import 'github-markdown-css'

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
      { file: 'pro-workflow.md', title: '生产主流程：工单→报工→入库' }
    ]
  }
])

const activeFile = ref<string>('quick-start.md')
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
  // 只处理站内相对 md 链接，外链/锚点保持默认行为
  if (!href.endsWith('.md') || /^(https?:)?\/\//.test(href)) return
  e.preventDefault()
  const file = href.split('/').pop() as string
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
