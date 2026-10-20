<template>
  <div class="container">
    <div class="toolbar">
      <input
          v-model="keyword"
          class="search"
          type="text"
          placeholder="按标题搜索，回车确认"
          @keyup.enter="doSearch"
      />
      <button class="search-btn" @click="doSearch">搜索</button>
      <button v-if="keyword" class="clear-btn" @click="clearSearch">清空</button>
    </div>

    <p v-if="loading" class="tip">加载中...</p>
    <p v-else-if="error" class="tip error">{{ error }}</p>
    <p v-else-if="!list.length" class="tip">没有匹配的专题</p>

    <div
        v-for="item in list"
        :key="item.id"
        class="card"
        @click="goDetail(item.id)"
    >
      <div class="left">
        <img :src="item.banner" alt="" />
      </div>
      <div class="right">
        <h3>{{ item.title }}</h3>
        <p>
          <span class="updated">{{ formatTime(item.updated) }}</span>
          <span>{{ formatCount(item.viewCount) }}次浏览</span>
        </p>
        <p class="intro">{{ item.introduction }}</p>
      </div>
      <button
          class="follow"
          :class="item.isFollowing ? 'followed' : 'unfollow'"
          :disabled="toggling === item.id"
          @click.stop="toggleFollow(item)"
      >
        {{ item.isFollowing ? '已关注' : '关注' }}
      </button>
    </div>

    <div v-if="pages > 1" class="pager">
      <button :disabled="page <= 1" @click="goPage(page - 1)">上一页</button>
      <span class="page-info">{{ page }} / {{ pages }}　共 {{ total }} 条</span>
      <button :disabled="page >= pages" @click="goPage(page + 1)">下一页</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter, type LocationQuery } from 'vue-router'
import { fetchSpecialPage, updateFollow } from '../api/special'
import { formatCount, formatTime, normalize, type PageResult, type Special, type SpecialRaw } from '../types/special'

const PAGE_SIZE = 5

const route = useRoute()
const router = useRouter()

/** 从 URL 里读初始状态：刷新、分享链接、浏览器后退都能还原 */
const readQuery = (q: LocationQuery): { page: number; keyword: string } => {
  const p = Number(q.page)
  return {
    page: Number.isFinite(p) && p > 0 ? p : 1,
    keyword: typeof q.keyword === 'string' ? q.keyword : ''
  }
}

const initial = readQuery(route.query)

const list = ref<Special[]>([])
const loading = ref(true)
const error = ref('')
/** 正在提交关注请求的 id，用来禁用按钮防连点 */
const toggling = ref<string | null>(null)

const keyword = ref(initial.keyword)
/** 已生效的搜索词，和输入框的 keyword 分开：输入框改了但没回车，不该影响翻页 */
const activeKeyword = ref(initial.keyword)
const page = ref(initial.page)
const total = ref(0)
const pages = ref(0)

const goDetail = (id: string): void => {
  router.push({ name: 'specialDetail', params: { id } })
}

const toggleFollow = async (item: Special): Promise<void> => {
  if (toggling.value) return
  toggling.value = item.id
  try {
    const res = await updateFollow(item.id, !item.isFollowing)
    // 后端成功才改本地状态，避免请求失败但界面已翻转
    if (res.data.code === 200) {
      item.isFollowing = !item.isFollowing
    }
  } catch (err) {
    console.log(err)
  } finally {
    toggling.value = null
  }
}

/** 把当前分页/搜索状态写进 URL，刷新后不丢，链接也能直接分享 */
const syncQuery = (): void => {
  router.replace({
    query: {
      ...(activeKeyword.value ? { keyword: activeKeyword.value } : {}),
      ...(page.value > 1 ? { page: String(page.value) } : {})
    }
  })
}

const fetchPage = async (): Promise<void> => {
  loading.value = true
  error.value = ''
  try {
    const res = await fetchSpecialPage({
      page: page.value,
      size: PAGE_SIZE,
      keyword: activeKeyword.value || undefined
    })
    if (res.data.code !== 200) {
      error.value = res.data.msg ?? '专题列表加载失败'
      return
    }
    const data = res.data.data as PageResult<SpecialRaw>
    list.value = data.records.map(normalize)
    total.value = data.total
    pages.value = data.pages
    // 后端开了 overflow 兜底，页码可能被纠正回第一页，以返回值为准
    page.value = Number(data.page)
    syncQuery()
  } catch (err) {
    console.log(err)
    error.value = '网络异常，请确认后端已启动（http://localhost:8080）'
  } finally {
    loading.value = false
  }
}

const goPage = (target: number): void => {
  if (target < 1 || (pages.value && target > pages.value)) return
  page.value = target
  fetchPage()
}

const doSearch = (): void => {
  activeKeyword.value = keyword.value.trim()
  page.value = 1
  fetchPage()
}

const clearSearch = (): void => {
  keyword.value = ''
  activeKeyword.value = ''
  page.value = 1
  fetchPage()
}

// 浏览器前进/后退改了 URL 时跟着刷新；自己触发的那次 replace 因状态已一致会被挡掉，不会重复请求
watch(
    () => route.query,
    (q) => {
      const next = readQuery(q)
      if (next.page === page.value && next.keyword === activeKeyword.value) return
      page.value = next.page
      activeKeyword.value = next.keyword
      keyword.value = next.keyword
      fetchPage()
    }
)

onMounted(fetchPage)
</script>

<style scoped>
.container {
  width: 70%;
  margin: 0 auto;
  padding-bottom: 30px;
}

.toolbar {
  display: flex;
  gap: 8px;
  margin: 16px 0;
}

.search {
  flex: 1;
  padding: 7px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 14px;
  outline: none;
}

.search:focus {
  border-color: #3670ee;
}

.search-btn,
.clear-btn {
  padding: 0 16px;
  border: 1px solid #3670ee;
  background-color: #3670ee;
  color: #fff;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.clear-btn {
  background-color: #fff;
  color: #3670ee;
}

.card {
  display: flex;
  height: 160px;
  margin-bottom: 15px;
  border: 1px solid #ccc;
  box-shadow: 10px 5px 5px #ddd;
  border-radius: 8px;
  position: relative;
  cursor: pointer;
  transition: box-shadow 0.2s;
}

.card:hover {
  box-shadow: 10px 5px 5px #bbb;
}

.left {
  flex: 1;
}

.right {
  flex: 4;
  padding: 10px;
}

.left img {
  width: 100%;
  height: 100%;
  border-top-left-radius: 8px;
  border-bottom-left-radius: 8px;
}

h3 {
  margin: 0 0 8px;
  padding-right: 90px;
}

p {
  margin: 4px 0;
}

p span {
  font-size: 13px;
  color: #333;
}

.updated {
  margin-right: 20px;
}

.intro {
  font-size: 13px;
  color: #666;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.follow {
  position: absolute;
  right: 20px;
  top: 20px;
  padding: 5px 10px;
  font-size: 14px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.follow:disabled {
  cursor: default;
  opacity: 0.6;
}

.followed {
  background-color: #f6f6f6;
  color: #716c6c;
}

.unfollow {
  background-color: #eff4fe;
  color: #3670ee;
}

.pager {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin: 20px 0;
}

.pager button {
  padding: 6px 16px;
  border: 1px solid #d9d9d9;
  background-color: #fff;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.pager button:disabled {
  color: #bbb;
  cursor: default;
  border-color: #eee;
}

.page-info {
  font-size: 13px;
  color: #666;
}

.tip {
  color: #666;
  font-size: 14px;
  text-align: center;
  margin: 40px 0;
}

.tip.error {
  color: #d03050;
}
</style>
