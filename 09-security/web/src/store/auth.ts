import { reactive } from 'vue'
import type { LoginResponse } from '../types/api'

/**
 * 登录态：令牌存 localStorage，应用启动时先恢复，刷新不掉线
 */
interface AuthState {
  token: string
  username: string
  nickname: string
  roles: string[]
}

const STORAGE_KEY = 'security_token'

const state = reactive<AuthState>({
  token: localStorage.getItem(STORAGE_KEY) ?? '',
  username: '',
  nickname: '',
  roles: []
})

// 刷新时用令牌反查一次 /auth/me，把身份信息补回来
async function restore() {
  if (!state.token) return
  try {
    const res = await fetch('/auth/me', {
      headers: { Authorization: `Bearer ${state.token}` }
    })
    if (!res.ok) {
      logout()
      return
    }
    const body = await res.json()
    if (body.code === 200) {
      state.username = body.data.username
      state.nickname = body.data.nickname
      state.roles = body.data.roles
    } else {
      logout()
    }
  } catch {
    logout()
  }
}

function setLogin(data: LoginResponse) {
  state.token = data.token
  state.username = data.username
  state.nickname = data.nickname
  state.roles = data.roles
  localStorage.setItem(STORAGE_KEY, data.token)
}

function logout() {
  state.token = ''
  state.username = ''
  state.nickname = ''
  state.roles = []
  localStorage.removeItem(STORAGE_KEY)
}

function hasRole(role: string) {
  return state.roles.includes(role)
}

/**
 * 登录后 / 刷新后该落在哪一页
 * 教师最先看的是待批改列表，学生看我的提交，管理员先看任务总览
 * 注意顺序：ADMIN 也持有 task:review，所以要先把只有讲师才有的场景排前面
 */
function homePath(): string {
  if (state.roles.includes('TEACHER')) return '/review'
  if (state.roles.includes('ADMIN')) return '/review'
  return '/tasks'
}

export function useAuth() {
  return { state, setLogin, logout, restore, hasRole, homePath }
}
