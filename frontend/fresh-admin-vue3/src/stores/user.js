import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { adminLogin } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('fresh_admin_token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('fresh_admin_user') || 'null'))

  const isLogin = computed(() => !!token.value)
  const displayName = computed(() => userInfo.value?.nickName || '管理员')

  async function login(username, password) {
    const data = await adminLogin({ username, password })
    token.value = data.token
    userInfo.value = data.userInfo
    localStorage.setItem('fresh_admin_token', data.token)
    localStorage.setItem('fresh_admin_user', JSON.stringify(data.userInfo || {}))
    return data
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('fresh_admin_token')
    localStorage.removeItem('fresh_admin_user')
  }

  return { token, userInfo, isLogin, displayName, login, logout }
})
