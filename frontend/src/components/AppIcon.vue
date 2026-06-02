<template>
  <el-icon v-if="elIcon" :size="size" :color="color">
    <component :is="elIcon" />
  </el-icon>
  <svg-icon v-else :icon-class="iconClass" :class-name="className" :color="color" />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import SvgIcon from '@/components/SvgIcon/index.vue'

const props = withDefaults(defineProps<{
  iconClass: string
  className?: string
  color?: string
  size?: string | number
}>(), {
  className: '',
  color: '',
  size: undefined
})

// Map of SVG icon names → Element Plus icon component names
const iconMap: Record<string, string> = {
  'user': 'User',
  'peoples': 'UserFilled',
  'password': 'Lock',
  'validCode': 'PictureFilled',
  'phone': 'Phone',
  'email': 'Message',
  'tree': 'List',
  'date': 'Calendar',
  'search': 'Search',
  'enter': 'Enter',
  'bell': 'Bell',
  'sunny': 'Sunny',
  'moon': 'Moon',
  'fullscreen': 'FullScreen',
  'exit-fullscreen': 'FullScreen',
  'size': 'Switch',
  'github': 'Link',
  'question': 'QuestionFilled',
  'table': 'Grid',
  'tree-table': 'Grid',
  'component': 'Setting',
  'monitor': 'Monitor',
  'chart': 'DataAnalysis',
  'eye': 'View',
  'eye-open': 'View',
  'message': 'Message',
  'log': 'Tickets',
  'job': 'Timer',
  'online': 'Connection',
  'server': 'Cpu',
  'cache': 'Coin',
  'redis': 'Coin',
  'dict': 'Collection',
  'config': 'Tools',
  'post': 'Stamp',
  'dept': 'OfficeBuilding',
  'menu': 'Menu',
  'role': 'Avatar',
  'system': 'HomeFilled',
  'logininfor': 'Reading',
  'operlog': 'Notebook',
  'documentation': 'Document',
  'link': 'Link',
  'edit': 'Edit',
  'star': 'Star',
  'drag': 'Rank',
  'clipboard': 'CopyDocument',
  'excel': 'Upload',
  'pdf': 'Picture',
  'zip': 'Folder',
  'tool': 'Tools',
  'lock': 'Lock',
  'shopping': 'ShoppingCart',
  'skill': 'MagicStick',
  'time': 'Timer',
  'nested': 'FolderOpened',
  'bug': 'WarningFilled',
  'guide': 'Guide',
  'education': 'Reading',
  'international': 'Promotion',
  'dashboard': 'Odometer',
  'home': 'HomeFilled',
  'language': 'ChatDotRound',
  'wechat': 'ChatLineSquare',
  'color': 'Brush',
  'number': 'Sort',
  'upload': 'Upload',
  'download': 'Download',
  'slider': 'Switch',
  'checkbox': 'Select',
  'radio': 'CircleCheck',
  'select': 'ArrowDown',
  'rate': 'StarFilled',
  'date-range': 'Calendar',
  'time-range': 'Timer',
  'input': 'EditPen',
  'textarea': 'Document',
  'switch': 'Switch',
  'button': 'SwitchButton',
  'code': 'Code',
  'icon': 'Picture',
  'druid': 'Monitor',
  'swagger': 'Document',
  'row': 'Operation',
  'cascader': 'Operation',
  'tab': 'Tickets',
  'form': 'Document',
  'build': 'Setting',
  'list': 'List',
  'more-up': 'ArrowUp',
  'qq': 'ChatLineRound',
  'money': 'Money',
}

const elIcon = computed(() => {
  if (!props.iconClass) return null
  // Try direct mapping
  const mapped = iconMap[props.iconClass]
  if (mapped) return (ElementPlusIconsVue as any)[mapped] || null
  // Try capitalized
  const cap = props.iconClass.charAt(0).toUpperCase() + props.iconClass.slice(1)
  return (ElementPlusIconsVue as any)[cap] || null
})
</script>
