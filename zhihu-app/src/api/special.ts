import axios from 'axios'

/**
 * 后端地址统一在这里配置，不要再在各个组件里写死。
 * - 默认 http://localhost:8080
 * - 需要改的话在 zhihu-app 根目录建 .env.local，写 VITE_API_BASE_URL=http://localhost:18080
 */
const baseURL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

const api = axios.create({
    baseURL,
    timeout: 10000
})

/** 专题列表 */
export const fetchAllSpecials = () => api.get('/api/v1/special/all')

/** 专题分页 + 按标题模糊搜索 */
export const fetchSpecialPage = (params: { page: number; size: number; keyword?: string }) =>
    api.get('/api/v1/special/page', { params })

/** 专题详情 */
export const fetchSpecialById = (id: string) => api.get(`/api/v1/special/${id}`)

/** 关注 / 取消关注 */
export const updateFollow = (id: string, follow: boolean) =>
    api.put(`/api/v1/special/${id}/follow`, { follow })
