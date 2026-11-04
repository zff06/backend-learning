import http from './request'
import type { LoginResponse, TaskView, UserProfile } from '../types/api'

/** 登录 */
export function login(username: string, password: string): Promise<LoginResponse> {
  return http.post('/auth/login', { username, password })
}

/** 退出（前端删令牌即可，JWT 无状态） */
export function logout() {
  return http.post('/auth/logout')
}

/** 当前账号信息 */
export function fetchMe(): Promise<UserProfile> {
  return http.get('/auth/me')
}

/** 学生提交任务 */
export function submitTask(title: string, content: string): Promise<TaskView> {
  return http.post('/api/tasks', { title, content })
}

/** 我的提交 */
export function fetchMyTasks(): Promise<TaskView[]> {
  return http.get('/api/tasks/mine')
}

/** 任务详情 */
export function fetchTask(id: number): Promise<TaskView> {
  return http.get(`/api/tasks/${id}`)
}

/** 全部提交（教师/管理员） */
export function fetchAllTasks(): Promise<TaskView[]> {
  return http.get('/api/tasks')
}

/** 批改 */
export function reviewTask(id: number, score: number, comment: string): Promise<TaskView> {
  return http.post(`/api/tasks/${id}/review`, { score, comment })
}

/** 账号列表（管理员） */
export function fetchUsers(): Promise<UserProfile[]> {
  return http.get('/admin/users')
}

/** 新增账号（管理员） */
export function createUser(data: {
  username: string
  password: string
  nickname: string
  roles: string[]
}): Promise<UserProfile> {
  return http.post('/admin/users', data)
}

/** 删除账号（管理员） */
export function deleteUser(username: string): Promise<void> {
  return http.delete(`/admin/users/${username}`)
}
