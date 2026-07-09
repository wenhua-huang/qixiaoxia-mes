/**
 * 全局注册 uni-ui 组件
 *
 * 背景：HBuilderX 发行 H5 时 easycom 自动引入失效（alpha 版本 bug），
 *      `<uni-xxx>` 标签编译成运行时 `_resolveComponent("uni-xxx")` 却找不到组件。
 *
 * 双保险策略：
 *   1. 页面级组件 —— 各页面已用显式 import（编译时绑定，见各页面 import 注释）
 *   2. 组件内部依赖 —— uni-ui 组件内部互相引用（如 uni-easyinput 内部的 uni-icons、
 *      uni-list-item 内部的 uni-badge/uni-icons、uni-data-checkbox 内部的 uni-load-more）
 *      仍走 `_resolveComponent`，靠这里的全局注册兜底，确保运行时能解析到。
 *
 * 新增 uni-ui 组件时：在此处补一个 import + 注册即可（页面无需改动）。
 */
import UniBadge from '@/uni_modules/uni-badge/components/uni-badge/uni-badge.vue'
import UniCard from '@/uni_modules/uni-card/components/uni-card/uni-card.vue'
import UniDataCheckbox from '@/uni_modules/uni-data-checkbox/components/uni-data-checkbox/uni-data-checkbox.vue'
import UniEasyInput from '@/uni_modules/uni-easyinput/components/uni-easyinput/uni-easyinput.vue'
import UniForms from '@/uni_modules/uni-forms/components/uni-forms/uni-forms.vue'
import UniFormsItem from '@/uni_modules/uni-forms/components/uni-forms-item/uni-forms-item.vue'
import UniGrid from '@/uni_modules/uni-grid/components/uni-grid/uni-grid.vue'
import UniGridItem from '@/uni_modules/uni-grid/components/uni-grid-item/uni-grid-item.vue'
import UniIcons from '@/uni_modules/uni-icons/components/uni-icons/uni-icons.vue'
import UniLink from '@/uni_modules/uni-link/components/uni-link/uni-link.vue'
import UniList from '@/uni_modules/uni-list/components/uni-list/uni-list.vue'
import UniListItem from '@/uni_modules/uni-list/components/uni-list-item/uni-list-item.vue'
import UniLoadMore from '@/uni_modules/uni-load-more/components/uni-load-more/uni-load-more.vue'
import UniNumberBox from '@/uni_modules/uni-number-box/components/uni-number-box/uni-number-box.vue'
import UniSection from '@/components/uni-section/uni-section.vue'
import UniSwiperDot from '@/uni_modules/uni-swiper-dot/components/uni-swiper-dot/uni-swiper-dot.vue'
import UniTag from '@/uni_modules/uni-tag/components/uni-tag/uni-tag.vue'
import UniTitle from '@/uni_modules/uni-title/components/uni-title/uni-title.vue'

export function registerUniUI(app) {
  app.component('uni-badge', UniBadge)
  app.component('uni-card', UniCard)
  app.component('uni-data-checkbox', UniDataCheckbox)
  app.component('uni-easyinput', UniEasyInput)
  app.component('uni-forms', UniForms)
  app.component('uni-forms-item', UniFormsItem)
  app.component('uni-grid', UniGrid)
  app.component('uni-grid-item', UniGridItem)
  app.component('uni-icons', UniIcons)
  app.component('uni-link', UniLink)
  app.component('uni-list', UniList)
  app.component('uni-list-item', UniListItem)
  app.component('uni-load-more', UniLoadMore)
  app.component('uni-number-box', UniNumberBox)
  app.component('uni-section', UniSection)
  app.component('uni-swiper-dot', UniSwiperDot)
  app.component('uni-tag', UniTag)
  app.component('uni-title', UniTitle)
}
