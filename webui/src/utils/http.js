import axios from 'axios'
import { useStore } from '../store'
import router from '../router'
import { ElMessage } from 'element-plus'

const http = axios.create({
  baseURL: '',
  timeout: 15000
})

// 请求拦截器：自动注入 Authorization
http.interceptors.request.use(
  (config) => {
    const store = useStore()
    if (store.sessionId) {
      config.headers = config.headers || {}
      config.headers.Authorization = `Bearer ${store.sessionId}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：统一处理 401 / 网络错误
http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const store = useStore()
      store.logout()
      router.push('/login')
      ElMessage.error('登录已过期，请重新登录')
    }
    return Promise.reject(error)
  }
)

export default http
