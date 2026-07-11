<template>
  <view class="work-container">
    <!-- 轮播图 -->
    <uni-swiper-dot class="uni-swiper-dot-box" :info="data" :current="current" field="content">
      <swiper class="swiper-box" :current="swiperDotIndex" @change="changeSwiper">
        <swiper-item v-for="(item, index) in data" :key="index">
          <view class="swiper-item" @click="clickBannerItem(item)">
            <image :src="item.image" mode="aspectFill" :draggable="false" />
          </view>
        </swiper-item>
      </swiper>
    </uni-swiper-dot>

    <!-- 宫格组件 — 仓储管理 -->
    <uni-section title="仓储管理" type="line"></uni-section>
    <view class="grid-body">
      <uni-grid :column="4" :showBorder="false" @change="changeWmGrid">
        <uni-grid-item>
          <view class="grid-item-box">
            <uni-icons type="paperplane-filled" size="30" color="#409eff"></uni-icons>
            <text class="text">领料单</text>
          </view>
        </uni-grid-item>
        <uni-grid-item>
          <view class="grid-item-box">
            <uni-icons type="scan" size="30" color="#67c23a"></uni-icons>
            <text class="text">扫码发料</text>
          </view>
        </uni-grid-item>
        <uni-grid-item>
          <view class="grid-item-box">
            <uni-icons type="search" size="30" color="#e6a23c"></uni-icons>
            <text class="text">扫码查库存</text>
          </view>
        </uni-grid-item>
      </uni-grid>
    </view>

    <!-- 宫格组件 — 采购管理 -->
    <uni-section title="采购管理" type="line"></uni-section>
    <view class="grid-body">
      <uni-grid :column="4" :showBorder="false" @change="changeGrid">
        <uni-grid-item @click="goReceipt">
          <view class="grid-item-box">
            <uni-icons type="checkbox-filled" size="30"></uni-icons>
            <text class="text">采购收货</text>
          </view>
        </uni-grid-item>
        <uni-grid-item @click="goHistory">
          <view class="grid-item-box">
            <uni-icons type="list" size="30"></uni-icons>
            <text class="text">收货历史</text>
          </view>
        </uni-grid-item>
      </uni-grid>
    </view>

    <!-- 宫格组件 — 生产管理 -->
    <uni-section title="生产管理" type="line"></uni-section>
    <view class="grid-body">
      <uni-grid :column="4" :showBorder="false">
        <uni-grid-item @click="goReport">
          <view class="grid-item-box">
            <uni-icons type="compose" size="30" color="#409eff"></uni-icons>
            <text class="text">生产报工</text>
          </view>
        </uni-grid-item>
        <uni-grid-item @click="goFeedbackHistory">
          <view class="grid-item-box">
            <uni-icons type="list" size="30" color="#67c23a"></uni-icons>
            <text class="text">报工历史</text>
          </view>
        </uni-grid-item>
      </uni-grid>
    </view>

    <!-- 宫格组件 — 系统管理 -->
    <uni-section title="系统管理" type="line"></uni-section>
    <view class="grid-body">
      <uni-grid :column="4" :showBorder="false" @change="changeGrid">
        <uni-grid-item>
          <view class="grid-item-box">
            <uni-icons type="person-filled" size="30"></uni-icons>
            <text class="text">用户管理</text>
          </view>
        </uni-grid-item>
        <uni-grid-item>
          <view class="grid-item-box">
            <uni-icons type="staff-filled" size="30"></uni-icons>
            <text class="text">角色管理</text>
          </view>
        </uni-grid-item>
        <uni-grid-item>
          <view class="grid-item-box">
            <uni-icons type="color" size="30"></uni-icons>
            <text class="text">菜单管理</text>
          </view>
        </uni-grid-item>
        <uni-grid-item>
          <view class="grid-item-box">
            <uni-icons type="settings-filled" size="30"></uni-icons>
            <text class="text">部门管理</text>
          </view>
        </uni-grid-item>
        <uni-grid-item>
          <view class="grid-item-box">
            <uni-icons type="heart-filled" size="30"></uni-icons>
            <text class="text">岗位管理</text>
          </view>
        </uni-grid-item>
        <uni-grid-item>
          <view class="grid-item-box">
            <uni-icons type="bars" size="30"></uni-icons>
            <text class="text">字典管理</text>
          </view>
        </uni-grid-item>
        <uni-grid-item>
          <view class="grid-item-box">
            <uni-icons type="gear-filled" size="30"></uni-icons>
            <text class="text">参数设置</text>
          </view>
        </uni-grid-item>
        <uni-grid-item>
          <view class="grid-item-box">
            <uni-icons type="chat-filled" size="30"></uni-icons>
            <text class="text">通知公告</text>
          </view>
        </uni-grid-item>
        <uni-grid-item>
          <view class="grid-item-box">
            <uni-icons type="wallet-filled" size="30"></uni-icons>
            <text class="text">日志管理</text>
          </view>
        </uni-grid-item>
      </uni-grid>
    </view>
  </view>
</template>

<script setup>
  import { ref, getCurrentInstance } from "vue"
  // 显式引入 uni-ui 组件（绕过 HBuilderX 发行 H5 时 easycom 失效，导致组件未打包）
  import UniSwiperDot from '@/uni_modules/uni-swiper-dot/components/uni-swiper-dot/uni-swiper-dot.vue'
  import UniSection from '@/components/uni-section/uni-section.vue'
  import UniGrid from '@/uni_modules/uni-grid/components/uni-grid/uni-grid.vue'
  import UniGridItem from '@/uni_modules/uni-grid/components/uni-grid-item/uni-grid-item.vue'
  import UniIcons from '@/uni_modules/uni-icons/components/uni-icons/uni-icons.vue'

  const { proxy } = getCurrentInstance()
  const current = ref(0)
  const swiperDotIndex = ref(0)
  const data = ref([{ image: '/static/images/banner/banner01.jpg' }, { image: '/static/images/banner/banner02.jpg' }, { image: '/static/images/banner/banner03.jpg' }])

  function clickBannerItem(item) {
    console.info(item)
  }

  function changeSwiper(e) {
    current.value = e.detail.current
  }

  function goReceipt() {
    proxy.$tab.navigateTo('/pages/mes/pur/receipt')
  }
  function goHistory() {
    proxy.$tab.navigateTo('/pages/mes/pur/history')
  }
  function changeGrid(e) {
    proxy.$modal.showToast('模块建设中~')
  }

  // 仓储管理宫格：0=领料单列表, 1=扫码发料(先选领料单), 2=扫码查库存
  function changeWmGrid(e) {
    const idx = e.detail.index
    if (idx === 0) proxy.$tab.navigateTo('/pages/mes/wm/issue/list')
    else if (idx === 1) proxy.$tab.navigateTo('/pages/mes/wm/issue/list?from=scan')
    else if (idx === 2) proxy.$tab.navigateTo('/pages/mes/wm/issue/scan-query')
  }

  // 生产管理入口
  function goReport() {
    proxy.$tab.navigateTo('/pages/mes/pro/report')
  }
  function goFeedbackHistory() {
    proxy.$tab.navigateTo('/pages/mes/pro/history')
  }
</script>

<style lang="scss" scoped>
  /* #ifndef APP-NVUE */
  page {
    display: flex;
    flex-direction: column;
    box-sizing: border-box;
    background-color: #fff;
    min-height: 100%;
    height: auto;
  }

  view {
    font-size: 14px;
    line-height: inherit;
  }
  /* #endif */

  .text {
    text-align: center;
    font-size: 26rpx;
    margin-top: 10rpx;
  }

  .grid-item-box {
    flex: 1;
    /* #ifndef APP-NVUE */
    display: flex;
    /* #endif */
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 15px 0;
  }

  .uni-margin-wrap {
    width: 690rpx;
    width: 100%;
    ;
  }

  .swiper {
    height: 300rpx;
  }

  .swiper-box {
    height: 150px;
  }

  .swiper-item {
    /* #ifndef APP-NVUE */
    display: flex;
    /* #endif */
    flex-direction: column;
    justify-content: center;
    align-items: center;
    color: #fff;
    height: 300rpx;
    line-height: 300rpx;
  }

  @media screen and (min-width: 500px) {
    .uni-swiper-dot-box {
      width: 400px;
      /* #ifndef APP-NVUE */
      margin: 0 auto;
      /* #endif */
      margin-top: 8px;
    }

    .image {
      width: 100%;
    }
  }
</style>
