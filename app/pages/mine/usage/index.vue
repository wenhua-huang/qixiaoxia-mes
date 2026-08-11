<template>
  <view class="usage-container">
    <!-- 顶部简介 -->
    <view class="intro">
      <view class="intro-title">移动端使用说明</view>
      <view class="intro-desc">
        本说明介绍产线最常用的操作。如有疑问请联系系统管理员。
      </view>
    </view>

    <!-- 报工 -->
    <view class="section">
      <view class="section-title">
        <view class="iconfont icon-edit section-icon"></view>
        报工（最常用）
      </view>
      <view class="card">
        <view class="step">
          <view class="step-num">1</view>
          <view class="step-body">
            <view class="step-text">底部切换到「工作台」</view>
          </view>
        </view>
        <view class="step">
          <view class="step-num">2</view>
          <view class="step-body">
            <view class="step-text">点击「报工」</view>
          </view>
        </view>
        <view class="step">
          <view class="step-num">3</view>
          <view class="step-body">
            <view class="step-text">选择你的工单和工序（可扫码快速定位）</view>
          </view>
        </view>
        <view class="step">
          <view class="step-num">4</view>
          <view class="step-body">
            <view class="step-text">填写：完成数量、合格数量、不合格数量</view>
          </view>
        </view>
        <view class="step step-last">
          <view class="step-num">5</view>
          <view class="step-body">
            <view class="step-text">确认无误后提交</view>
          </view>
        </view>
        <view class="tip">
          注意：合格数 + 不合格数 必须等于 完成数，请核对后再提交。
        </view>
      </view>
    </view>

    <!-- 领料 -->
    <view class="section">
      <view class="section-title">
        <view class="iconfont icon-edit section-icon"></view>
        领料
      </view>
      <view class="card">
        <view class="step">
          <view class="step-num">1</view>
          <view class="step-body">
            <view class="step-text">在工作台找到「领料」入口</view>
          </view>
        </view>
        <view class="step">
          <view class="step-num">2</view>
          <view class="step-body">
            <view class="step-text">选择对应工单的领料单</view>
          </view>
        </view>
        <view class="step">
          <view class="step-num">3</view>
          <view class="step-body">
            <view class="step-text">核对物料明细和数量</view>
          </view>
        </view>
        <view class="step step-last">
          <view class="step-num">4</view>
          <view class="step-body">
            <view class="step-text">确认后提交</view>
          </view>
        </view>
      </view>
    </view>

    <!-- 账号 -->
    <view class="section">
      <view class="section-title">
        <view class="iconfont icon-user section-icon"></view>
        账号与安全
      </view>
      <view class="card card-list">
        <view class="list-row" @click="goPwd">
          <view class="row-label">修改密码</view>
          <view class="iconfont icon-right row-arrow"></view>
        </view>
        <view class="list-row" @click="goInfo">
          <view class="row-label">编辑个人资料</view>
          <view class="iconfont icon-right row-arrow"></view>
        </view>
        <view class="list-row list-row-last" @click="goSetting">
          <view class="row-label">退出登录</view>
          <view class="iconfont icon-right row-arrow"></view>
        </view>
      </view>
    </view>

    <!-- 常见问题 -->
    <view class="section">
      <view class="section-title">
        <view class="iconfont icon-help section-icon"></view>
        遇到问题？
      </view>
      <view class="card">
        <view class="qa">
          <view class="q">看不到工单？</view>
          <view class="a">可能未派产到你，请联系班组长确认派产。</view>
        </view>
        <view class="qa">
          <view class="q">报工提交后发现错了？</view>
          <view class="a">若已审核，请勿自行修改，联系管理员退回处理。</view>
        </view>
        <view class="qa qa-last">
          <view class="q">无法登录？</view>
          <view class="a">确认账号密码正确，仍不行请联系管理员重置。</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
  import { getCurrentInstance } from "vue"

  const { proxy } = getCurrentInstance()

  function goPwd() {
    proxy.$tab.navigateTo('/pages/mine/pwd/index')
  }

  function goInfo() {
    proxy.$tab.navigateTo('/pages/mine/info/edit')
  }

  function goSetting() {
    proxy.$tab.navigateTo('/pages/mine/setting/index')
  }
</script>

<style lang="scss" scoped>
  page {
    background-color: #f5f6f7;
  }

  .usage-container {
    padding: 24rpx 24rpx 80rpx;
  }

  .intro {
    background: linear-gradient(135deg, #3c96f3, #5ba8f5);
    color: #fff;
    border-radius: 16rpx;
    padding: 36rpx 30rpx;
    margin-bottom: 30rpx;

    .intro-title {
      font-size: 36rpx;
      font-weight: bold;
      margin-bottom: 12rpx;
    }

    .intro-desc {
      font-size: 26rpx;
      opacity: 0.9;
      line-height: 1.6;
    }
  }

  .section {
    margin-bottom: 30rpx;
  }

  .section-title {
    color: #303133;
    font-size: 30rpx;
    font-weight: bold;
    margin-left: 10rpx;
    margin-bottom: 16rpx;
    display: flex;
    align-items: center;

    .section-icon {
      font-size: 32rpx;
      margin-right: 12rpx;
      color: #3c96f3;
    }
  }

  .card {
    background: #ffffff;
    border-radius: 16rpx;
    box-shadow: 0 0 10rpx rgba(193, 193, 193, 0.2);
    padding: 8rpx 30rpx;
  }

  .step {
    display: flex;
    align-items: flex-start;
    padding: 24rpx 0;
    border-bottom: 1rpx solid #f2f3f5;

    .step-num {
      flex-shrink: 0;
      width: 40rpx;
      height: 40rpx;
      line-height: 40rpx;
      text-align: center;
      background: #3c96f3;
      color: #fff;
      border-radius: 50%;
      font-size: 24rpx;
      margin-right: 20rpx;
      margin-top: 4rpx;
    }

    .step-body {
      flex: 1;

      .step-text {
        font-size: 28rpx;
        color: #303133;
        line-height: 1.6;
      }
    }
  }

  .step-last {
    border-bottom: none;
  }

  .tip {
    background: #fff8e6;
    color: #b88230;
    font-size: 24rpx;
    padding: 16rpx 20rpx;
    border-radius: 8rpx;
    margin: 16rpx 0;
    line-height: 1.6;
  }

  .card-list {
    padding: 0;
  }

  .list-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 28rpx 30rpx;
    border-bottom: 1rpx solid #f2f3f5;

    .row-label {
      font-size: 28rpx;
      color: #303133;
    }

    .row-arrow {
      font-size: 24rpx;
      color: #c0c4cc;
    }
  }

  .list-row-last {
    border-bottom: none;
  }

  .qa {
    padding: 24rpx 0;
    border-bottom: 1rpx solid #f2f3f5;

    .q {
      font-size: 28rpx;
      color: #303133;
      font-weight: 600;
      margin-bottom: 10rpx;
    }

    .a {
      font-size: 26rpx;
      color: #606266;
      line-height: 1.6;
    }
  }

  .qa-last {
    border-bottom: none;
  }
</style>
