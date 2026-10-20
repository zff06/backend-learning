/** 专题相关类型与格式化工具，列表页和详情页共用，避免两边各写一份 */

export interface Section {
    sectionId: string
    sectionTitle: string
}

export interface Special {
    id: string
    title: string
    banner: string
    introduction: string
    isFollowing: boolean
    followersCount: number
    viewCount: number
    /** 秒级时间戳，后端已按字符串下发（Long -> String，避免 JS 丢精度） */
    updated: string
    sectionList: Section[]
}

/** 后端原始返回：is_following 在库里是 varchar，存的是 Python 风格的 "True"/"False" */
export interface SpecialRaw extends Omit<Special, 'isFollowing'> {
    isFollowing: string
}

/** 后端分页返回结构，对应 dto/PageResult */
export interface PageResult<T> {
    records: T[]
    total: number
    page: number
    size: number
    pages: number
}

export const normalize = (raw: SpecialRaw): Special => ({
    ...raw,
    isFollowing: String(raw.isFollowing).toLowerCase() === 'true',
    sectionList: raw.sectionList ?? []
})

/** 秒级时间戳 -> 2026/9/24 */
export const formatTime = (ts: string): string =>
    ts ? new Date(Number(ts) * 1000).toLocaleDateString('zh-CN') : ''

/** 8477160 -> 847.7万 */
export const formatCount = (n: number): string =>
    n >= 10000 ? `${(n / 10000).toFixed(1)}万` : String(n)
