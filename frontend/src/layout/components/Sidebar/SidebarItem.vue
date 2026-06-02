<template>
  <div v-if="!item.hidden">
    <template v-if="hasOneShowingChild(item.children, item) && (!onlyOneChild.children || onlyOneChild.noShowingChildren) && !item.alwaysShow">
      <app-link v-if="onlyOneChild.meta" :to="resolvePath(onlyOneChild.path, onlyOneChild.query)">
        <el-menu-item :index="resolvePath(onlyOneChild.path)" :class="{ 'submenu-title-noDropdown': !isNest }">
          <el-icon v-if="getElIcon(onlyOneChild.meta.icon || (item.meta && item.meta.icon))">
            <component :is="getElIcon(onlyOneChild.meta.icon || (item.meta && item.meta.icon))" />
          </el-icon>
          <svg-icon v-else :icon-class="onlyOneChild.meta.icon || (item.meta && item.meta.icon)"/>
          <template #title><span class="menu-title" :title="hasTitle(onlyOneChild.meta.title)">{{ onlyOneChild.meta.title }}</span></template>
        </el-menu-item>
      </app-link>
    </template>

    <el-sub-menu v-else ref="subMenu" :index="resolvePath(item.path)" teleported>
      <template v-if="item.meta" #title>
        <el-icon v-if="getElIcon(item.meta && item.meta.icon)">
            <component :is="getElIcon(item.meta && item.meta.icon)" />
          </el-icon>
          <svg-icon v-else :icon-class="item.meta && item.meta.icon" />
        <span class="menu-title" :title="hasTitle(item.meta.title)">{{ item.meta.title }}</span>
      </template>

      <sidebar-item
        v-for="(child, index) in item.children"
        :key="child.path + index"
        :is-nest="true"
        :item="child"
        :base-path="resolvePath(child.path)"
        class="nest-menu"
      />
    </el-sub-menu>
  </div>
</template>

<script setup lang="ts">
import { isExternal } from '@/utils/validate'
import AppLink from './Link.vue'
import { getNormalPath } from '@/utils/ruoyi'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// Common SVG icon names mapped to Element Plus icon components
const iconMap: Record<string, string> = {
  'user': 'User', 'peoples': 'UserFilled', 'tree-table': 'Grid',
  'tree': 'List', 'table': 'Grid', 'form': 'Document',
  'build': 'Setting', 'component': 'Setting', 'chart': 'DataAnalysis',
  'monitor': 'Monitor', 'job': 'Timer', 'log': 'Tickets',
  'online': 'Connection', 'server': 'Cpu', 'cache': 'Coin',
  'redis': 'Coin', 'dict': 'Collection', 'config': 'Tools',
  'post': 'Stamp', 'dept': 'OfficeBuilding', 'menu': 'Menu',
  'role': 'Avatar', 'system': 'HomeFilled', 'logininfor': 'Reading',
  'operlog': 'Notebook', 'documentation': 'Document', 'link': 'Link',
  'password': 'Lock', 'validCode': 'PictureFilled', 'search': 'Search',
  'date': 'Calendar', 'number': 'Sort', 'cascader': 'Operation',
  'color': 'Brush', 'edit': 'Edit', 'message': 'Message',
  'star': 'Star', 'phone': 'Phone', 'email': 'Message',
  'eye': 'View', 'eye-open': 'View', 'list': 'List',
  'example': 'Files', 'guide': 'Guide', 'tab': 'Tickets',
  'education': 'Reading', 'international': 'Promotion',
  'dashboard': 'Odometer', 'home': 'HomeFilled',
  'nested': 'FolderOpened', 'bug': 'WarningFilled',
  'drag': 'Rank', 'clipboard': 'CopyDocument', 'excel': 'Upload',
  'pdf': 'Picture', 'zip': 'Folder', 'tool': 'Tools',
  'lock': 'Lock', 'shopping': 'ShoppingCart', 'skill': 'MagicStick',
  'time': 'Timer', 'range': 'Clock', 'exit': 'Back',
  'size': 'Switch', 'fullscreen': 'FullScreen', 'theme': 'Moon',
  'language': 'ChatDotRound', 'wechat': 'ChatLineSquare',
}

function getElIcon(iconName: string): any {
  if (!iconName) return null
  const elIconName = iconMap[iconName]
  if (elIconName) {
    return (ElementPlusIconsVue as any)[elIconName] || null
  }
  const capitalized = iconName.charAt(0).toUpperCase() + iconName.slice(1)
  return (ElementPlusIconsVue as any)[capitalized] || null
}

const props = defineProps({
  // route object
  item: {
    type: Object,
    required: true
  },
  isNest: {
    type: Boolean,
    default: false
  },
  basePath: {
    type: String,
    default: ''
  }
})

const onlyOneChild = ref({})

function hasOneShowingChild(children: any[] = [], parent: any) {
  if (!children) {
    children = []
  }
  const showingChildren = children.filter(item => {
    if (item.hidden) {
      return false
    }
    onlyOneChild.value = item
    return true
  })

  // When there is only one child router, the child router is displayed by default
  if (showingChildren.length === 1) {
    return true
  }

  // Show parent if there are no child router to display
  if (showingChildren.length === 0) {
    onlyOneChild.value = { ...parent, path: '', noShowingChildren: true }
    return true
  }

  return false
}

function resolvePath(routePath: string, routeQuery?: string): string | { path: string; query: Record<string, any> } {
  if (isExternal(routePath)) {
    return routePath
  }
  if (isExternal(props.basePath)) {
    return props.basePath
  }
  if (routeQuery) {
    const query = JSON.parse(routeQuery)
    return { path: getNormalPath(props.basePath + '/' + routePath), query: query }
  }
  return getNormalPath(props.basePath + '/' + routePath)
}

function hasTitle(title: string): string {
  if (title.length > 5) {
    return title
  } else {
    return ""
  }
}
</script>
