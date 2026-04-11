import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'
import { useUserStore } from '../stores/user'

const API_BASE = (import.meta.env.VITE_API_BASE || '/api').replace(/\/$/, '')

const request = axios.create({
  baseURL: API_BASE,
  timeout: 10000
})

let isRefreshing = false
let refreshSubscribers = []

function subscribeTokenRefresh(cb) {
  refreshSubscribers.push(cb)
}

function onTokenRefreshed(accessToken) {
  refreshSubscribers.forEach((cb) => cb(accessToken))
  refreshSubscribers = []
}

function onRefreshFailed() {
  refreshSubscribers = []
  useUserStore().logout()
  router.push('/login')
}

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('access_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (err) => Promise.reject(err)
)

request.interceptors.response.use(
  (res) => {
    const data = res.data
    const silent = !!res.config?.silent
    if (data.code && data.code !== 200) {
      if (!silent) ElMessage.error(data.message || '请求失败')
      const err = new Error(data.message)
      err.response = { status: data.code, data: { code: data.code, message: data.message } }
      err.silent = silent
      return Promise.reject(err)
    }
    return data
  },
  async (err) => {
    const originalRequest = err.config
    const silent = !!originalRequest?.silent

    if (err.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        return new Promise((resolve) => {
          subscribeTokenRefresh((accessToken) => {
            originalRequest.headers.Authorization = `Bearer ${accessToken}`
            resolve(request(originalRequest))
          })
        })
      }

      originalRequest._retry = true
      isRefreshing = true
      const refreshTokenVal = localStorage.getItem('refresh_token')

      if (!refreshTokenVal) {
        useUserStore().logout()
        router.push('/login')
        if (!silent) ElMessage.error('登录已过期，请重新登录')
        return Promise.reject(err)
      }

      try {
        const { data } = await axios.post(`${API_BASE}/auth/refresh`, { refreshToken: refreshTokenVal }, {
          timeout: 10000,
          headers: { 'Content-Type': 'application/json' }
        })
        if (data?.data?.accessToken) {
          useUserStore().updateTokens(data.data.accessToken, data.data.refreshToken)
          originalRequest.headers.Authorization = `Bearer ${data.data.accessToken}`
          onTokenRefreshed(data.data.accessToken)
          return request(originalRequest)
        }
      } catch (refreshErr) {
        onRefreshFailed()
        if (!silent) ElMessage.error('登录已过期，请重新登录')
        return Promise.reject(refreshErr)
      } finally {
        isRefreshing = false
      }
    }

    if (!silent) {
      if (err.response?.status === 403) {
        ElMessage.error(err.response?.data?.message || '没有权限访问')
      } else if (err.response?.status === 429) {
        ElMessage.warning(err.response?.data?.message || '操作过于频繁')
      } else if (err.response?.status !== 401) {
        ElMessage.error(err.response?.data?.message || err.message || '网络错误')
      }
    }

    return Promise.reject(err)
  }
)

export default request
