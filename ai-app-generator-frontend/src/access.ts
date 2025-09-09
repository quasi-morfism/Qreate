import { useLoginUserStore } from '@/stores/loginUser'
import { createDiscreteApi } from 'naive-ui'
import router from '@/router'
import { ref } from 'vue'

// 是否为首次获取登录用户
let firstFetchLoginUser = true

/**
 * 全局权限校验
 */
const { message } = createDiscreteApi(['message'])

// 全局状态：是否显示登录弹窗
export const showGlobalLoginModal = ref(false)
export const redirectAfterLogin = ref('')

router.beforeEach(async (to, from, next) => {
  const loginUserStore = useLoginUserStore()
  let loginUser = loginUserStore.loginUser
  // 确保页面刷新，首次加载时，能够等后端返回用户信息后再校验权限
  if (firstFetchLoginUser) {
    await loginUserStore.fetchLoginUser()
    loginUser = loginUserStore.loginUser
    firstFetchLoginUser = false
  }
  const toUrl = to.fullPath
  if (toUrl.startsWith('/admin')) {
    if (!loginUser || loginUser.userRole !== 'admin') {
      message.error('no permission')
      redirectAfterLogin.value = to.fullPath
      showGlobalLoginModal.value = true
      next('/')
      return
    }
  }
  if (toUrl.startsWith('/user')) {
    if (!loginUser || !loginUser.id) {
      message.error('Please log in first')
      redirectAfterLogin.value = to.fullPath
      showGlobalLoginModal.value = true
      next('/')
      return
    }
  }
  next()
})
