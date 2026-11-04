import axios from 'axios'
import { useAuth } from '../store/auth'
import { useToast } from '../store/toast'

/**
 * 请求层：请求拦截器加令牌，响应拦截器拆外壳、分流错误
 * 约定：业务失败是 HTTP 200 + code；只有 401/403 用真实状态码
 */
const http = axios.create({ baseURL: '' })

http.interceptors.request.use((config) => {
  const { state } = useAuth()
  if (state.token) {
    config.headers.Authorization = `Bearer ${state.token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const { logout } = useAuth()
    const { show } = useToast()
    const body = response.data
    // 拿不到外壳的响应（理论不会出现）原样返回
    if (body == null || typeof body.code === 'undefined') {
      return body
    }
    if (body.code === 200) {
      return body.data
    }
    // 业务失败统一弹提示
    show(body.message || '请求失败', 'error')
    // 令牌失效也按未认证处理：清登录态跳登录页
    if (body.code === 401) {
      logout()
      const redirect = encodeURIComponent(location.pathname + location.search)
      location.href = `/login?redirect=${redirect}`
    }
    return Promise.reject(body)
  },
  (error) => {
    const { logout } = useAuth()
    const { show } = useToast()
    const status = error.response?.status
    if (status === 401) {
      // 未认证：清登录态、跳登录页并记住来处
      logout()
      show('登录已过期，请重新登录', 'error')
      const redirect = encodeURIComponent(location.pathname + location.search)
      location.href = `/login?redirect=${redirect}`
    } else if (status === 403) {
      // 越权：只弹提示，人留在原地
      show('无权限访问该功能', 'error')
    } else {
      show(error.response?.data?.message || '网络异常，请稍后再试', 'error')
    }
    return Promise.reject(error)
  }
)

export default http
