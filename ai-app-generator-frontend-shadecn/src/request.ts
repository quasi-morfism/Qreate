import axios from 'axios'
import { toast } from 'vue-sonner'

// 创建 Axios 实例
const myAxios = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8100/api',
  timeout: 60000,
  withCredentials: true,
})

// 全局请求拦截器
myAxios.interceptors.request.use(
  function (config) {
    // Do something before request is sent
    return config
  },
  function (error) {
    // Do something with request error
    return Promise.reject(error)
  },
)

// 全局响应拦截器
myAxios.interceptors.response.use(
  function (response) {
    const { data } = response
    // 未登录
    if (data.code === 40100) {
      // 统一提示未登录，但不跳转到路由登录页（使用全局头部登录弹窗）
      if (!response.request.responseURL.includes('user/get/login')) {
        toast.warning('请先登录')
      }
    }
    return response
  },
  function (error) {
    // Any status codes that falls outside the range of 2xx cause this function to trigger
    // Do something with response error
    // Show a generic error toast if server provides message
    try {
      const msg = error?.response?.data?.message || error?.message
      if (msg) toast.error(msg)
    } catch {}
    return Promise.reject(error)
  },
)

export default myAxios
