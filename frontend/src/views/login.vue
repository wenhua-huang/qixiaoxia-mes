<template>
  <div class="login">
    <!-- 暗蓝背景装饰层 -->
    <div class="login-bg">
      <div class="bg-overlay"></div>
      <div class="bg-pattern"></div>
      <!-- 装饰性几何元素 -->
      <div class="bg-shapes">
        <div class="shape shape-1"></div>
        <div class="shape shape-2"></div>
        <div class="shape shape-3"></div>
      </div>
    </div>

    <!-- 登录卡片 -->
    <div class="login-card">
      <div class="card-header">
        <div class="logo-wrapper">
          <img src="@/assets/logo/logo.png" class="brand-logo" />
        </div>
        <h2 class="brand-title">企小侠平台</h2>
        <p class="brand-subtitle">助力中小企业，完成数字化转型</p>
      </div>

      <el-form ref="loginRef" :model="loginForm" :rules="loginRules" class="login-form">
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            type="text"
            size="large"
            auto-complete="off"
            placeholder="请输入账号"
          >
            <template #prefix>
              <svg-icon icon-class="user" class="input-icon" />
            </template>
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            size="large"
            auto-complete="off"
            placeholder="请输入密码"
            @keyup.enter="handleLogin"
          >
            <template #prefix>
              <svg-icon icon-class="password" class="input-icon" />
            </template>
          </el-input>
        </el-form-item>
        <el-form-item prop="code" v-if="captchaEnabled">
          <el-input
            v-model="loginForm.code"
            size="large"
            auto-complete="off"
            placeholder="验证码"
            style="width: 63%"
            @keyup.enter="handleLogin"
          >
            <template #prefix>
              <svg-icon icon-class="validCode" class="input-icon" />
            </template>
          </el-input>
          <div class="login-code">
            <img :src="codeUrl" @click="getCode" class="login-code-img"/>
          </div>
        </el-form-item>
        <el-checkbox v-model="loginForm.rememberMe" style="margin:0px 0px 25px 0px;color:#666;">记住密码</el-checkbox>
        <el-form-item style="width:100%;">
          <el-button
            :loading="loading"
            size="large"
            type="primary"
            class="login-btn"
            @click.prevent="handleLogin"
          >
            <span v-if="!loading">登 录</span>
            <span v-else>登 录 中...</span>
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 底部版权 -->
    <div class="login-footer">
      <span>{{ footerContent }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { getCodeImg } from "@/api/login"
import Cookies from "js-cookie"
import { encrypt, decrypt } from "@/utils/jsencrypt"
import useUserStore from '@/store/modules/user'
import defaultSettings from '@/settings'
import type { CaptchaInfoResult } from '@/types/api/login'
import type { LoginForm } from '@/types/api/login'

const title = import.meta.env.VITE_APP_TITLE
const footerContent = defaultSettings.footerContent
const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance()

const loginForm = ref<LoginForm>({
  username: "admin",
  password: "admin123",
  rememberMe: false,
  code: "",
  uuid: ""
})

const loginRules = {
  username: [{ required: true, trigger: "blur", message: "请输入您的账号" }],
  password: [{ required: true, trigger: "blur", message: "请输入您的密码" }],
  code: [{ required: true, trigger: "change", message: "请输入验证码" }]
}

const codeUrl = ref("")
const loading = ref(false)
const captchaEnabled = ref(true)
const register = ref(false)
const redirect = ref<string | undefined>(undefined)

watch(route, (newRoute: any) => {
    redirect.value = (newRoute.query && newRoute.query.redirect) as string | undefined
}, { immediate: true })

function handleLogin(): void {
  proxy.$refs.loginRef.validate((valid: boolean) => {
    if (valid) {
      loading.value = true
      if (loginForm.value.rememberMe) {
        Cookies.set("username", loginForm.value.username, { expires: 30 })
        Cookies.set("password", encrypt(loginForm.value.password), { expires: 30 })
        Cookies.set("rememberMe", loginForm.value.rememberMe, { expires: 30 })
      } else {
        Cookies.remove("username")
        Cookies.remove("password")
        Cookies.remove("rememberMe")
      }
      userStore.login(loginForm.value).then(() => {
        const query = route.query
        const otherQueryParams = Object.keys(query).reduce((acc: Record<string, any>, cur) => {
          if (cur !== "redirect") {
            acc[cur] = query[cur]
          }
          return acc
        }, {})
        router.push({ path: redirect.value || "/", query: otherQueryParams })
      }).catch(() => {
        loading.value = false
        if (captchaEnabled.value) {
          getCode()
        }
      })
    }
  })
}

function getCode(): void {
  getCodeImg().then(res => {
    captchaEnabled.value = res.captchaEnabled === undefined ? true : res.captchaEnabled
    if (captchaEnabled.value) {
      codeUrl.value = "data:image/gif;base64," + res.img
      loginForm.value.uuid = res.uuid
    }
  })
}

function getCookie(): void {
  const username = Cookies.get("username")
  const password = Cookies.get("password")
  const rememberMe = Cookies.get("rememberMe")
  loginForm.value = {
    username: username === undefined ? loginForm.value.username : username,
    password: password === undefined ? loginForm.value.password : decrypt(password),
    rememberMe: rememberMe === undefined ? false : Boolean(rememberMe)
  }
}

getCode()
getCookie()
</script>

<style lang='scss' scoped>
// ==========================================
// 企小侠 MES 暗蓝专业版 — 登录页
// ==========================================

.login {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #0cd7bd 0%, #50c3f7 100%);
}

// 背景层
.login-bg {
  position: absolute;
  inset: 0;
  z-index: 0;

  .bg-overlay {
    position: absolute;
    inset: 0;
    background: radial-gradient(ellipse at 50% 30%, rgba(80, 195, 247, 0.4) 0%, rgba(12, 215, 189, 0.3) 70%),
                linear-gradient(180deg, #0cd7bd 0%, #3db8e8 50%, #50c3f7 100%);
  }

  .bg-pattern {
    position: absolute;
    inset: 0;
    opacity: 0.03;
    background-image:
      repeating-linear-gradient(0deg, transparent, transparent 2px, rgba(255,255,255,.5) 2px, rgba(255,255,255,.5) 3px),
      repeating-linear-gradient(90deg, transparent, transparent 2px, rgba(255,255,255,.5) 2px, rgba(255,255,255,.5) 3px);
  }

  .bg-shapes {
    position: absolute;
    inset: 0;

    .shape {
      position: absolute;
      border-radius: 50%;
      background: rgba(255, 255, 255, 0.08);
    }
    .shape-1 {
      width: 600px;
      height: 600px;
      top: -200px;
      right: -100px;
    }
    .shape-2 {
      width: 400px;
      height: 400px;
      bottom: -100px;
      left: -80px;
      background: rgba(255, 255, 255, 0.1);
    }
    .shape-3 {
      width: 300px;
      height: 300px;
      top: 40%;
      left: 55%;
      background: rgba(255, 255, 255, 0.06);
    }
  }
}

// 登录卡片
.login-card {
  position: relative;
  z-index: 1;
  width: 420px;
  background: rgba(255, 255, 255, 0.97);
  border-radius: 12px;
  padding: 40px 36px 28px 36px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.4),
              0 0 1px rgba(201, 169, 110, 0.3);
  backdrop-filter: blur(10px);
}

.card-header {
  text-align: center;
  margin-bottom: 28px;

  .logo-wrapper {
    margin-bottom: 16px;

    .brand-logo {
      width: 64px;
      height: 64px;
      border-radius: 14px;
      box-shadow: 0 4px 16px rgba(47, 103, 169, 0.3);
    }
  }

  .brand-title {
    margin: 0;
    font-size: 22px;
    font-weight: 700;
    color: #ffffff;
    letter-spacing: 2px;
  }

  .brand-subtitle {
    margin: 6px 0 0 0;
    font-size: 13px;
    color: rgba(255, 255, 255, 0.8);
    letter-spacing: 1px;
  }
}

// 登录表单
.login-form {
  .el-input {
    height: 44px;

    :deep(.el-input__wrapper) {
      border-radius: 6px;
      box-shadow: 0 0 0 1px #d9dde2 inset;
      transition: box-shadow 0.25s;

      &:hover {
        box-shadow: 0 0 0 1px #0cd7bd inset;
      }
      &.is-focus {
        box-shadow: 0 0 0 1px #50c3f7 inset;
      }
    }

    input {
      height: 44px;
      font-size: 14px;
    }

    .input-icon {
      height: 42px;
      width: 15px;
      margin-left: 0px;
    }
  }

  // 登录按钮
  .login-btn {
    width: 100%;
    height: 46px;
    font-size: 16px;
    font-weight: 600;
    letter-spacing: 4px;
    border-radius: 6px;
    background: linear-gradient(135deg, #0cd7bd 0%, #50c3f7 100%) !important;
    border: none !important;
    box-shadow: 0 4px 14px rgba(12, 215, 189, 0.35);
    transition: all 0.3s;

    &:hover {
      background: linear-gradient(135deg, #50c3f7 0%, #0cd7bd 100%) !important;
      box-shadow: 0 6px 20px rgba(12, 215, 189, 0.5);
      transform: translateY(-1px);
    }

    &:active {
      transform: translateY(0);
    }
  }
}

.login-tip {
  font-size: 13px;
  text-align: center;
  color: #bfbfbf;
}

.login-code {
  width: 33%;
  height: 44px;
  float: right;
  img {
    cursor: pointer;
    vertical-align: middle;
  }
}

.login-code-img {
  height: 44px;
  padding-left: 12px;
}

// 底部版权
.login-footer {
  position: fixed;
  bottom: 0;
  width: 100%;
  height: 44px;
  line-height: 44px;
  text-align: center;
  color: rgba(255, 255, 255, 0.45);
  font-family: Arial;
  font-size: 12px;
  letter-spacing: 1px;
  z-index: 1;
}

// ==========================================
// 暗黑模式覆盖
// ==========================================
html.dark .login {
  background: linear-gradient(135deg, #0a3d36 0%, #1a3a4a 100%);

  .login-bg {
    .bg-overlay {
      background: radial-gradient(ellipse at 50% 30%, rgba(12, 215, 189, 0.2) 0%, rgba(0, 0, 0, 0.95) 70%),
                  linear-gradient(180deg, #0a2a25 0%, #141e28 50%, #0d1a20 100%);
    }
  }

  .login-card {
    background: rgba(29, 30, 31, 0.97);
    box-shadow: 0 20px 60px rgba(0, 0, 0, 0.6),
                0 0 1px rgba(12, 215, 189, 0.2);

    .brand-title {
      color: #5b9bd5;
    }
    .brand-subtitle {
      color: #909399;
    }
  }

  .login-form .el-input {
    :deep(.el-input__wrapper) {
      background-color: #262727;
      box-shadow: 0 0 0 1px #434343 inset;
    }
  }

  .login-footer {
    color: rgba(255, 255, 255, 0.25);
  }
}
</style>
