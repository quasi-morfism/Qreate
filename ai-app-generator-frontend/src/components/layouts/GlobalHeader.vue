<template>
  <n-layout-header bordered>
    <n-flex justify="space-between" align="center" style="height: 64px; padding: 0 16px">
      <n-flex align="center" :wrap="false">
        <n-avatar round src="/logo.png" :size="32" style="margin-right: 8px; flex-shrink: 0" />
        <n-text
          tag="h1"
          :depth="1"
          style="margin: 0; font-size: 18px; font-weight: 600; letter-spacing: -0.02em"
        >
          Qreate
        </n-text>
        <n-menu
          mode="horizontal"
          :options="menuOptions"
          v-model:value="activeKey"
          style="margin-left: 24px"
          :root-props="{
            style: 'background-color: transparent;',
          }"
        />
      </n-flex>
      <n-flex align="center" :wrap="false">
        <template v-if="loginUserStore.loginUser.id">
          <n-popover trigger="hover" placement="bottom">
            <template #trigger>
              <n-flex align="center" :size="8" style="cursor: pointer" class="user-trigger">
                <n-avatar
                  round
                  :src="
                    loginUserStore.loginUser.userAvatar || 'https://avatar.iran.liara.run/public'
                  "
                  :size="32"
                  class="user-avatar"
                />
                <n-text class="user-name">{{ loginUserStore.loginUser.userName }}</n-text>
              </n-flex>
            </template>
            <div style="display: flex; flex-direction: column; gap: 6px; min-width: 160px">
              <n-button quaternary block size="small" @click="goProfile">Profile</n-button>
              <template v-if="(loginUserStore.loginUser.userRole || '').toLowerCase() === 'admin'">
                <n-divider style="margin: 4px 0" />
                <n-button quaternary block size="small" @click="goAdmin">Admin</n-button>
              </template>
              <n-divider style="margin: 4px 0" />
              <n-button quaternary block size="small" @click="handleLogout">Logout</n-button>
            </div>
          </n-popover>
        </template>
        <template v-else>
          <n-button type="primary" @click="showLoginModal = true"> Login </n-button>
        </template>
      </n-flex>
    </n-flex>
  </n-layout-header>

  <n-modal v-model:show="showLoginModal" preset="card" style="width: 320px" title="">
    <div style="padding: 24px 0; text-align: center">
      <n-avatar round src="/logo.png" :size="56" style="margin-bottom: 12px" />
      <h2 style="margin: 0 0 32px 0; font-size: 24px">Qreate</h2>

      <div style="display: flex; flex-direction: column; gap: 20px">
        <div>
          <label style="display: block; margin-bottom: 8px; font-size: 14px">Username</label>
          <n-input
            v-model:value="loginForm.userAccount"
            placeholder="Enter username"
            size="large"
          />
        </div>
        <div>
          <label style="display: block; margin-bottom: 8px; font-size: 14px">Password</label>
          <n-input
            v-model:value="loginForm.userPassword"
            type="password"
            placeholder="Enter password"
            size="large"
          />
        </div>
        <n-button
          type="primary"
          block
          size="large"
          style="margin-top: 8px"
          :loading="isLoading"
          @click="handleLogin"
        >
          Login
        </n-button>
        <div style="text-align: center; font-size: 14px; color: #666">
          Don't have an account?
          <span style="color: #18a058; cursor: pointer" @click="openRegisterModal"
            >Sign up here</span
          >
        </div>
      </div>
    </div>
  </n-modal>

  <n-modal v-model:show="showRegisterModal" preset="card" style="width: 320px" title="">
    <div style="padding: 24px 0; text-align: center">
      <n-avatar round src="/logo.png" :size="56" style="margin-bottom: 12px" />
      <h2 style="margin: 0 0 32px 0; font-size: 24px">Create account</h2>

      <div style="display: flex; flex-direction: column; gap: 20px">
        <div>
          <label style="display: block; margin-bottom: 8px; font-size: 14px">Username</label>
          <n-input
            v-model:value="registerForm.userAccount"
            placeholder="Enter username"
            size="large"
          />
        </div>
        <div>
          <label style="display: block; margin-bottom: 8px; font-size: 14px">Password</label>
          <n-input
            v-model:value="registerForm.userPassword"
            type="password"
            placeholder="Enter password"
            size="large"
          />
        </div>
        <div>
          <label style="display: block; margin-bottom: 8px; font-size: 14px"
            >Confirm password</label
          >
          <n-input
            v-model:value="registerForm.checkPassword"
            type="password"
            placeholder="Re-enter password"
            size="large"
          />
        </div>
        <n-button
          type="primary"
          block
          size="large"
          style="margin-top: 8px"
          :loading="isRegisterLoading"
          @click="handleRegister"
        >
          Create account
        </n-button>
        <div style="text-align: center; font-size: 14px; color: #666">
          Already have an account?
          <span style="color: #18a058; cursor: pointer" @click="openLoginModal">Log in</span>
        </div>
      </div>
    </div>
  </n-modal>
</template>

<script setup lang="ts">
import { ref, h, reactive, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/loginUser'
import { userLogin, register } from '@/api/userController'
import { useMessage } from 'naive-ui'
import { showGlobalLoginModal, redirectAfterLogin } from '@/access'

const activeKey = ref('home')
const showLoginModal = ref(false)
const showRegisterModal = ref(false)
const isLoading = ref(false)
const isRegisterLoading = ref(false)
const message = useMessage()
const loginUserStore = useLoginUserStore()
const router = useRouter()

const loginForm = reactive({
  userAccount: '',
  userPassword: '',
})

const registerForm = reactive<API.UserRegisterRequest>({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
})

// loginUserStore.fetchLoginUser()

const handleLogin = async () => {
  if (!loginForm.userAccount || !loginForm.userPassword) {
    message.warning('Please enter username and password')
    return
  }

  isLoading.value = true
  try {
    const res = await userLogin({
      userAccount: loginForm.userAccount,
      userPassword: loginForm.userPassword,
    })

    if (res.data?.data) {
      message.success('Login successful!')
      loginUserStore.setLoginUser(res.data.data)
      showLoginModal.value = false
      loginForm.userAccount = ''
      loginForm.userPassword = ''
      handleLoginSuccess()
    } else {
      message.error(res.data?.message || 'Login failed')
    }
  } catch (error) {
    message.error('Login failed. Please try again.')
    console.error('Login error:', error)
  } finally {
    isLoading.value = false
  }
}

const handleLogout = async () => {
  await loginUserStore.logout()
  message.success('Logged out')
  showLoginModal.value = false
  showRegisterModal.value = false
  router.push('/')
}

const openRegisterModal = () => {
  showLoginModal.value = false
  showRegisterModal.value = true
}

const openLoginModal = () => {
  showRegisterModal.value = false
  showLoginModal.value = true
}

const handleRegister = async () => {
  if (!registerForm.userAccount || !registerForm.userPassword || !registerForm.checkPassword) {
    message.warning('Please fill in all fields')
    return
  }
  if (registerForm.userPassword !== registerForm.checkPassword) {
    message.warning('Passwords do not match')
    return
  }

  isRegisterLoading.value = true
  try {
    const res = await register({
      userAccount: registerForm.userAccount,
      userPassword: registerForm.userPassword,
      checkPassword: registerForm.checkPassword,
    })
    if (res.data?.code === 0) {
      message.success('Registration successful! Please log in.')
      // Prefill username in login form for convenience
      loginForm.userAccount = registerForm.userAccount || ''
      // reset register form
      registerForm.userAccount = ''
      registerForm.userPassword = ''
      registerForm.checkPassword = ''
      openLoginModal()
    } else {
      message.error(res.data?.message || 'Registration failed')
    }
  } catch {
    message.error('Registration failed. Please try again.')
    // console.error('Register error:', error)
  } finally {
    isRegisterLoading.value = false
  }
}

const goAdmin = () => {
  router.push('/admin/user-manage')
}

const goProfile = () => {
  router.push('/user/account-setup')
}

// 监听全局登录弹窗状态
watch(showGlobalLoginModal, (show) => {
  if (show) {
    showLoginModal.value = true
    showGlobalLoginModal.value = false
  }
})

// 登录成功后重定向
const handleLoginSuccess = () => {
  if (redirectAfterLogin.value) {
    router.push(redirectAfterLogin.value)
    redirectAfterLogin.value = ''
  }
}

const menuOptions = [
  {
    label: () => h(RouterLink, { to: '/' }, { default: () => 'Home' }),
    key: 'home',
  },
  {
    label: () => h(RouterLink, { to: '/about' }, { default: () => 'About' }),
    key: 'about',
  },
]
</script>

<style scoped>
@media (max-width: 768px) {
  :deep(.n-menu) {
    display: none !important;
  }
}

.user-trigger:hover .user-name {
  color: #18a058;
}

.user-trigger:hover :deep(.n-avatar) {
  box-shadow: 0 0 0 2px #18a058;
}
</style>
