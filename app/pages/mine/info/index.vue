<template>
  <view class="container">
    <uni-list>
      <uni-list-item showExtraIcon="true" :extraIcon="{type: 'person-filled'}" title="昵称" :rightText="user.nickName" />
      <uni-list-item showExtraIcon="true" :extraIcon="{type: 'phone-filled'}" title="手机号码" :rightText="user.phonenumber" />
      <uni-list-item showExtraIcon="true" :extraIcon="{type: 'email-filled'}" title="邮箱" :rightText="user.email" />
      <uni-list-item showExtraIcon="true" :extraIcon="{type: 'auth-filled'}" title="岗位" :rightText="postGroup" />
      <uni-list-item showExtraIcon="true" :extraIcon="{type: 'staff-filled'}" title="角色" :rightText="roleGroup" />
      <uni-list-item showExtraIcon="true" :extraIcon="{type: 'calendar-filled'}" title="创建日期" :rightText="user.createTime" />
    </uni-list>
  </view>
</template>

<script setup>
  import { getUserProfile } from "@/api/system/user"
  import { ref, reactive } from "vue"
  // 显式引入 uni-ui 组件（绕过 HBuilderX 发行 H5 时 easycom 失效）
  import UniList from '@/uni_modules/uni-list/components/uni-list/uni-list.vue'
  import UniListItem from '@/uni_modules/uni-list/components/uni-list-item/uni-list-item.vue'
  import UniBadge from '@/uni_modules/uni-badge/components/uni-badge/uni-badge.vue'
  import UniIcons from '@/uni_modules/uni-icons/components/uni-icons/uni-icons.vue'

  const user = ref({})
  const roleGroup = ref("")
  const postGroup = ref("")

  function getUser() {
    getUserProfile().then(response => {
      user.value = response.data
      roleGroup.value = response.roleGroup
      postGroup.value = response.postGroup
    })
  }

  getUser()
</script>

<style lang="scss">
  page {
    background-color: #ffffff;
  }
</style>
