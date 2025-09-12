<template>
  <header
    :class="[
      'w-full',
      'header-glass',
      'sticky',
      'top-0',
      'z-50',
      { 'header-scrolled': isScrolled },
    ]"
  >
    <div class="h-16 px-6 flex items-center justify-between">
      <div
        class="logo-container flex items-center gap-3"
        @click="goHome"
        role="button"
        aria-label="Go Home"
        title="Home"
      >
        <Avatar class="logo-avatar">
          <AvatarImage src="/logo.png" alt="logo" />
          <AvatarFallback>Q</AvatarFallback>
        </Avatar>
        <h1 class="logo-text">Qreate</h1>
      </div>
      <div class="flex items-center gap-3 header-actions">
        <template v-if="loginUserStore.loginUser.id">
          <div class="user-dropdown-container" @mouseenter="showDropdown = true" @mouseleave="showDropdown = false">
            <div class="flex items-center gap-2 user-trigger">
              <Avatar class="user-avatar">
                <AvatarImage
                  :src="
                    loginUserStore.loginUser.userAvatar ||
                    getDefaultAvatar(loginUserStore.loginUser.id)
                  "
                />
                <AvatarFallback>U</AvatarFallback>
              </Avatar>
              <span class="user-name">{{ loginUserStore.loginUser.userName }}</span>
            </div>
            <Transition name="dropdown" appear>
              <div v-if="showDropdown" class="dropdown-content">
                <div class="dropdown-item" @click="goProfile">Profile</div>
                <template v-if="(loginUserStore.loginUser.userRole || '').toLowerCase() === 'admin'">
                  <div class="dropdown-separator"></div>
                  <div class="dropdown-item" @click="goAdmin">User Management</div>
                  <div class="dropdown-item" @click="goAppManage">App Management</div>
                </template>
                <div class="dropdown-separator"></div>
                <div class="dropdown-item" @click="handleLogout">Logout</div>
              </div>
            </Transition>
          </div>
        </template>
        <template v-else>
          <Button @click="showLoginModal = true">Login</Button>
        </template>
      </div>
    </div>
  </header>

  <Dialog v-model:open="showLoginModal">
    <DialogContent class="sm:max-w-[380px]">
      <div class="py-6 text-center">
        <Avatar class="mx-auto mb-3 w-14 h-14">
          <AvatarImage src="/logo.png" />
          <AvatarFallback>Q</AvatarFallback>
        </Avatar>
        <h2 class="text-2xl font-semibold mb-8">Qreate</h2>
        <div class="flex flex-col gap-5 text-left">
          <div>
            <label class="block mb-2 text-sm">Username</label>
            <Input v-model="loginForm.userAccount" placeholder="Enter username" />
          </div>
          <div>
            <label class="block mb-2 text-sm">Password</label>
            <Input v-model="loginForm.userPassword" type="password" placeholder="Enter password" />
          </div>
          <Button class="mt-1 w-full" :disabled="isLoading" @click="handleLogin">Login</Button>
          <div class="text-center text-sm text-neutral-500">
            Don't have an account?
            <span class="text-emerald-600 cursor-pointer" @click="openRegisterModal"
              >Sign up here</span
            >
          </div>
        </div>
      </div>
    </DialogContent>
  </Dialog>

  <Dialog v-model:open="showRegisterModal">
    <DialogContent class="sm:max-w-[380px]">
      <div class="py-6 text-center">
        <Avatar class="mx-auto mb-3 w-14 h-14">
          <AvatarImage src="/logo.png" />
          <AvatarFallback>Q</AvatarFallback>
        </Avatar>
        <h2 class="text-2xl font-semibold mb-8">Create account</h2>
        <div class="flex flex-col gap-5 text-left">
          <div>
            <label class="block mb-2 text-sm">Username</label>
            <Input v-model="registerForm.userAccount" placeholder="Enter username" />
          </div>
          <div>
            <label class="block mb-2 text-sm">Password</label>
            <Input
              v-model="registerForm.userPassword"
              type="password"
              placeholder="Enter password"
            />
          </div>
          <div>
            <label class="block mb-2 text-sm">Confirm password</label>
            <Input
              v-model="registerForm.checkPassword"
              type="password"
              placeholder="Re-enter password"
            />
          </div>
          <Button class="mt-1 w-full" :disabled="isRegisterLoading" @click="handleRegister"
            >Create account</Button
          >
          <div class="text-center text-sm text-neutral-500">
            Already have an account?
            <span class="text-emerald-600 cursor-pointer" @click="openLoginModal">Log in</span>
          </div>
        </div>
      </div>
    </DialogContent>
  </Dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch, onMounted, onUnmounted, Transition } from 'vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/loginUser'
import { userLogin, register } from '@/api/userController'
import { showGlobalLoginModal, redirectAfterLogin } from '@/access'
import { toast } from 'vue-sonner'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Avatar, AvatarImage, AvatarFallback } from '@/components/ui/avatar'
import { Dialog, DialogContent } from '@/components/ui/dialog'

// Generate consistent default avatar based on user ID
const getDefaultAvatar = (userId: string | number | undefined) => {
  if (!userId) return 'https://avatar.iran.liara.run/public'

  // Use user ID to generate a consistent seed
  const seed = String(userId)
    .split('')
    .reduce((acc, char) => acc + char.charCodeAt(0), 0)
  return `https://avatar.iran.liara.run/public/${(seed % 100) + 1}`
}

const showLoginModal = ref(false)
const showRegisterModal = ref(false)
const isLoading = ref(false)
const isRegisterLoading = ref(false)
const showDropdown = ref(false)
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
    toast.warning('Please enter username and password')
    return
  }

  isLoading.value = true
  try {
    const res = await userLogin({
      userAccount: loginForm.userAccount,
      userPassword: loginForm.userPassword,
    })

    if (res.data?.data) {
      toast.success('Login successful!')
      loginUserStore.setLoginUser(res.data.data)
      showLoginModal.value = false
      loginForm.userAccount = ''
      loginForm.userPassword = ''
      handleLoginSuccess()
    } else {
      toast.error(res.data?.message || 'Login failed')
    }
  } catch (error) {
    toast.error('Login failed. Please try again.')
    console.error('Login error:', error)
  } finally {
    isLoading.value = false
  }
}

const handleLogout = async () => {
  await loginUserStore.logout()
  toast.success('Logged out')
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
    toast.warning('Please fill in all fields')
    return
  }
  if (registerForm.userPassword !== registerForm.checkPassword) {
    toast.warning('Passwords do not match')
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
      toast.success('Registration successful! Please log in.')
      // Prefill username in login form for convenience
      loginForm.userAccount = registerForm.userAccount || ''
      // reset register form
      registerForm.userAccount = ''
      registerForm.userPassword = ''
      registerForm.checkPassword = ''
      openLoginModal()
    } else {
      toast.error(res.data?.message || 'Registration failed')
    }
  } catch {
    toast.error('Registration failed. Please try again.')
    // console.error('Register error:', error)
  } finally {
    isRegisterLoading.value = false
  }
}

const goAdmin = () => {
  router.push('/admin/user-manage')
}

const goAppManage = () => {
  router.push('/admin/app-manage')
}

const goProfile = () => {
  router.push('/user/account-setup')
}

const goHome = () => {
  router.push('/')
}

// Glass header scroll shadow
const isScrolled = ref(false)
onMounted(() => {
  const onScroll = () => {
    isScrolled.value = window.scrollY > 4
  }
  onScroll()
  window.addEventListener('scroll', onScroll, { passive: true })
  onUnmounted(() => {
    window.removeEventListener('scroll', onScroll)
  })
})

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

</script>

<style scoped>
.header-glass {
  background: rgba(255, 255, 255, 0.1);
  -webkit-backdrop-filter: blur(8px) saturate(120%);
  backdrop-filter: blur(8px) saturate(120%);
  border-bottom: 0;
  box-shadow: none;
  transition:
    background-color 0.2s ease,
    box-shadow 0.2s ease;
}
.header-scrolled {
  background: rgba(255, 255, 255, 0.68);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.06);
}

.logo-container {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 4px 8px;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  gap: 12px;
}

.logo-container:hover {
  transform: translateY(-2px);
}

.logo-avatar {
  flex-shrink: 0;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
}

.logo-avatar::before {
  content: '';
  position: absolute;
  top: -6px;
  left: -6px;
  right: -6px;
  bottom: -6px;
  background: conic-gradient(from 0deg, #18a058, #36ad6a, #4ade80, #22c55e, #18a058);
  border-radius: 50%;
  opacity: 0;
  transition: all 0.4s ease;
  animation: rotate 2s linear infinite;
  animation-play-state: paused;
  filter: blur(2px);
}

.logo-avatar::after {
  content: '';
  position: absolute;
  top: -3px;
  left: -3px;
  right: -3px;
  bottom: -3px;
  background: radial-gradient(circle, transparent 60%, rgba(24, 160, 88, 0.2) 70%, transparent 80%);
  border-radius: 50%;
  opacity: 0;
  transition: all 0.4s ease;
}

.logo-container:hover .logo-avatar::before {
  opacity: 0.3;
  animation-play-state: running;
}

.logo-container:hover .logo-avatar::after {
  opacity: 1;
}

.logo-container:hover .logo-avatar {
  transform: scale(1.15) rotate(8deg);
  z-index: 2;
  filter: drop-shadow(0 0 10px rgba(24, 160, 88, 0.3));
}

.logo-text {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  letter-spacing: -0.02em;
  color: #333;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
}

.logo-container:hover .logo-text {
  color: #18a058;
  transform: translateX(-3px) scale(1.02);
  text-shadow: 0 0 20px rgba(24, 160, 88, 0.4);
  filter: brightness(1.1);
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 768px) {
  :deep(.n-menu) {
    display: none !important;
  }

  .logo-text {
    font-size: 20px;
  }
}

.user-trigger:hover .user-name {
  color: #18a058;
}

.user-trigger:hover :deep(.n-avatar) {
  box-shadow: 0 0 0 2px #18a058;
}

.user-dropdown-container {
  position: relative;
  padding: 8px 12px;
  margin: -8px -12px;
}

.dropdown-content {
  position: absolute;
  top: calc(100% - 8px);
  right: 12px;
  margin-top: 4px;
  min-width: 200px;
  background: rgba(255, 255, 255, 0.3);
  -webkit-backdrop-filter: blur(8px) saturate(120%);
  backdrop-filter: blur(8px) saturate(120%);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  padding: 8px;
  z-index: 1000;
}

.dropdown-item {
  padding: 8px 12px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  color: #333;
  transition: all 0.2s ease;
}

.dropdown-item:hover {
  background: rgba(255, 255, 255, 0.5);
  color: #18a058;
}

.dropdown-separator {
  height: 1px;
  background: rgba(0, 0, 0, 0.1);
  margin: 4px 0;
}

/* Vue Transition 动画 */
.dropdown-enter-active,
.dropdown-leave-active {
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.dropdown-enter-from {
  opacity: 0;
  transform: translateY(-8px) scale(0.95);
}

.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-8px) scale(0.95);
}

.dropdown-enter-to,
.dropdown-leave-from {
  opacity: 1;
  transform: translateY(0) scale(1);
}
</style>
