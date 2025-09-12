import { ref } from 'vue'
import { defineStore } from 'pinia'
import { getLoginUser, userLogout } from '@/api/userController.ts'

export const useLoginUserStore = defineStore('loginUser', () => {
  const loginUser = ref<API.LoginUserVO>({
    userName: 'not logged in',
  })

  //fetch user info
  async function fetchLoginUser() {
    const res = await getLoginUser()
    if (res.data.code === 0 && res.data.data) {
      loginUser.value = res.data.data
    }
  }
  function setLoginUser(user: API.LoginUserVO) {
    loginUser.value = user
  }

  async function logout() {
    try {
      await userLogout()
    } catch {
      // ignore logout failure; proceed to clear local state
    } finally {
      loginUser.value = { userName: 'not logged in' }
    }
  }

  return { loginUser, fetchLoginUser, setLoginUser, logout }
})
