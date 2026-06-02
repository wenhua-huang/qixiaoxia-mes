<template>
  <div class="sidebar-logo-container" :class="{ 'collapse': collapse }">
    <transition name="sidebarLogoFade">
      <router-link v-if="collapse" key="collapse" class="sidebar-logo-link" to="/">
        <AppLogo v-if="true" class="sidebar-logo" />
        <h1 v-else class="sidebar-title">{{ title }}</h1>
      </router-link>
      <router-link v-else key="expand" class="sidebar-logo-link" to="/">
        <AppLogo v-if="true" class="sidebar-logo" />
        <div class="sidebar-brand">
          <h1 class="sidebar-title">企小侠</h1>
          <span class="sidebar-subtitle">MES 智能执行系统</span>
        </div>
      </router-link>
    </transition>
  </div>
</template>

<script setup lang="ts">
import AppLogo from '@/assets/logo/AppLogo.vue'
import useSettingsStore from '@/store/modules/settings'
import variables from '@/assets/styles/variables.module.scss'

defineProps({
  collapse: {
    type: Boolean,
    required: true
  }
})

const title = import.meta.env.VITE_APP_TITLE
const settingsStore = useSettingsStore()
const sideTheme = computed(() => settingsStore.sideTheme)

// Logo 背景色 — MES 暗蓝版
const getLogoBackground = computed(() => {
  if (settingsStore.isDark) {
    return 'var(--sidebar-bg)'
  }
  if (settingsStore.navType == 3) {
    return variables.menuLightBg
  }
  return sideTheme.value === 'theme-dark' ? '#0c2a2a' : '#ffffff'
})

// Logo 文字颜色
const getLogoTextColor = computed(() => {
  if (settingsStore.isDark) {
    return 'var(--sidebar-logo-text)'
  }
  if (settingsStore.navType == 3) {
    return variables.menuLightText
  }
  return sideTheme.value === 'theme-dark' ? '#fff' : '#262626'
})
</script>

<style lang="scss" scoped>
.sidebarLogoFade-enter-active {
  transition: opacity 1.5s;
}

.sidebarLogoFade-enter,
.sidebarLogoFade-leave-to {
  opacity: 0;
}

.sidebar-logo-container {
  position: relative;
  height: 50px;
  line-height: normal;
  background: v-bind(getLogoBackground);
  text-align: center;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid #f0f0f0;

  & .sidebar-logo-link {
    height: 100%;
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    text-decoration: none;

    & .sidebar-logo {
      width: 32px;
      height: 32px;
      vertical-align: middle;
      margin-right: 10px;
      border-radius: 7px;
      flex-shrink: 0;
    }

    & .sidebar-brand {
      display: flex;
      flex-direction: column;
      align-items: flex-start;
      line-height: 1.2;

      & .sidebar-title {
        margin: 0;
        color: v-bind(getLogoTextColor);
        font-weight: 700;
        font-size: 14px;
        letter-spacing: 1px;
      }

      & .sidebar-subtitle {
        font-size: 10px;
        color: #8c8c8c;
        letter-spacing: 0.5px;
      }
    }

    & .sidebar-title {
      display: inline-block;
      margin: 0;
      color: v-bind(getLogoTextColor);
      font-weight: 600;
      line-height: 50px;
      font-size: 14px;
      font-family: Avenir, Helvetica Neue, Arial, Helvetica, sans-serif;
      vertical-align: middle;
    }
  }

  &.collapse {
    .sidebar-logo {
      margin-right: 0px;
    }
  }
}
</style>
