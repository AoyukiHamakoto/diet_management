import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const accessToken = ref(localStorage.getItem('access_token') || '')
  const refreshToken = ref(localStorage.getItem('refresh_token') || '')
  const userId = ref(localStorage.getItem('userId') || null)
  const nickname = ref(localStorage.getItem('nickname') || '')
  const avatar = ref(localStorage.getItem('avatar') || '')
  const role = ref(localStorage.getItem('role') || 'USER')

  function setUser(data) {
    accessToken.value = data.accessToken
    refreshToken.value = data.refreshToken
    userId.value = data.userId
    nickname.value = data.nickname || ''
    avatar.value = data.avatar || ''
    role.value = data.role || 'USER'
    localStorage.setItem('access_token', data.accessToken)
    localStorage.setItem('refresh_token', data.refreshToken)
    localStorage.setItem('userId', data.userId)
    localStorage.setItem('nickname', data.nickname || '')
    localStorage.setItem('avatar', data.avatar || '')
    localStorage.setItem('role', data.role || 'USER')
  }

  function updateTokens(accessTokenVal, refreshTokenVal) {
    accessToken.value = accessTokenVal
    refreshToken.value = refreshTokenVal
    localStorage.setItem('access_token', accessTokenVal)
    localStorage.setItem('refresh_token', refreshTokenVal)
  }

  function logout() {
    accessToken.value = ''
    refreshToken.value = ''
    userId.value = null
    nickname.value = ''
    avatar.value = ''
    role.value = 'USER'
    localStorage.removeItem('access_token')
    localStorage.removeItem('refresh_token')
    localStorage.removeItem('userId')
    localStorage.removeItem('nickname')
    localStorage.removeItem('avatar')
    localStorage.removeItem('role')
  }

  const token = ref(accessToken)
  const isLoggedIn = () => !!accessToken.value

  return {
    accessToken,
    refreshToken,
    token: accessToken,
    userId,
    nickname,
    avatar,
    role,
    setUser,
    updateTokens,
    logout,
    isLoggedIn
  }
})
