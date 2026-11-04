/** 与后端出参一一对应的类型定义，可空字段都标了 null */

export interface LoginResponse {
  token: string
  tokenType: string
  expiresAt: string
  username: string
  nickname: string
  roles: string[]
}

export interface UserProfile {
  username: string
  nickname: string
  roles: string[]
  authorities: string[]
  enabled: boolean
}

export interface TaskView {
  id: number
  title: string
  content: string
  ownerId: number
  owner: string
  ownerNickname: string
  status: 'SUBMITTED' | 'REVIEWED'
  statusText: string
  score: number | null
  comment: string | null
  submittedAt: string
  reviewedAt: string | null
}
