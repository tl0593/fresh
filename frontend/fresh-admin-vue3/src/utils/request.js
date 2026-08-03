import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

const request = axios.create({
  baseURL: '',
  timeout: 15000
})

request.interceptors.request.use((config) => {
  const userStore = useUserStore()
  if (userStore.token) {
    config.headers.Authorization = userStore.token
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const res = response.data
    const silent = response.config?.silent
    if (res && typeof res.code !== 'undefined') {
      if (res.code === 200) {
        return res.data
      }
      if (res.code === 401) {
        const userStore = useUserStore()
        userStore.logout()
        router.push('/login')
      }
      if (!silent) {
        ElMessage.error(res.msg || '请求失败')
      }
      return Promise.reject(new Error(res.msg || '请求失败'))
    }
    return res
  },
  (error) => {
    const status = error.response?.status
    const silent = error.config?.silent
    if (status === 401) {
      const userStore = useUserStore()
      userStore.logout()
      router.push('/login')
    }
    if (!silent) {
      ElMessage.error(error.response?.data?.msg || error.message || '网络异常')
    }
    return Promise.reject(error)
  }
)

export default request
