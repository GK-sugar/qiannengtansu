import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * 统一请求封装
 * 演示阶段（VITE_USE_MOCK=true）大部分页面使用本地 Mock；
 * 对接后端后，所有 /api 请求经 vite 代理到 Spring Boot(8082)，生产环境由 Nginx 反代。
 */
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('qnct_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

request.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const status = error?.response?.status
    const msg = error?.response?.data?.message || error?.response?.data?.detail || error.message || '请求失败'
    if (status === 401 && !error?.config?.url?.includes('/auth/login')) {
      localStorage.removeItem('qnct_token')
      window.location.href = '/login'
    }
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

export default request