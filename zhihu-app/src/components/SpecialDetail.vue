<template>
  <div class="detail">
    <a class="back" @click="goBack">← 返回列表</a>

    <p v-if="loading" class="tip">加载中...</p>
    <p v-else-if="error" class="tip error">{{ error }}</p>

    <template v-else-if="special">
      <img class="banner" :src="special.banner" :alt="special.title" />

      <h1 class="title">{{ special.title }}</h1>

      <div class="meta">
        <span>{{ formatTime(special.updated) }}</span>
        <span>{{ formatCount(special.viewCount) }}次浏览</span>
        <span>{{ formatCount(special.followersCount) }}人关注</span>
        <button
            class="follow"
            :class="special.isFollowing ? 'followed' : 'unfollow'"
            :disabled="toggling"
            @click="toggleFollow"
        >
          {{ special.isFollowing ? '已关注' : '关注' }}
        </button>
      </div>

      <p class="intro">{{ special.introduction }}</p>

      <div class="sections">
        <h2>专题章节</h2>
        <p v-if="!special.sectionList.length" class="tip">暂无章节</p>
        <ul v-else>
          <li v-for="(sec, index) in special.sectionList" :key="sec.sectionId">
            <span class="index">{{ index + 1 }}</span>
            <span class="section-title">{{ sec.sectionTitle }}</span>
            <span class="section-id">ID: {{ sec.sectionId }}</span>
          </li>
        </ul>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchSpecialById, updateFollow } from '../api/special'
import { formatCount, formatTime, normalize, type Special, type SpecialRaw } from '../types/special'

const route = useRoute()
const router = useRouter()

const special = ref<Special | null>(null)
const loading = ref(false)
const error = ref('')
const toggling = ref(false)

const toggleFollow = async (): Promise<void> => {
  if (!special.value || toggling.value) return
  toggling.value = true
  try {
    const next = !special.value.isFollowing
    const res = await updateFollow(special.value.id, next)
    if (res.data.code === 200) {
      special.value.isFollowing = next
      // 关注数跟着变，取消关注时也不该出现负数
      special.value.followersCount = Math.max(0, special.value.followersCount + (next ? 1 : -1))
    }
  } catch (err) {
    console.log(err)
  } finally {
    toggling.value = false
  }
}

const goBack = (): void => {
  // 直接进来的（没有历史记录）回列表页，否则退回上一页
  if (window.history.length > 1) router.back()
  else router.push({ name: 'specialList' })
}

const fetchDetail = async (id: string): Promise<void> => {
  loading.value = true
  error.value = ''
  special.value = null
  try {
    const res = await fetchSpecialById(id)
    // 后端统一响应体：HTTP 200，靠 code 区分成功失败
    if (res.data.code !== 200) {
      error.value = res.data.msg ?? '专题加载失败'
      return
    }
    special.value = normalize(res.data.data as SpecialRaw)
  } catch (err) {
    console.log(err)
    error.value = '网络异常，请确认后端已启动（http://localhost:8080）'
  } finally {
    loading.value = false
  }
}

// 同一个组件复用时（列表页互相跳转）参数会变，用 watch 而不是 onMounted
watch(
    () => route.params.id,
    (id) => {
      if (typeof id === 'string' && id) fetchDetail(id)
    },
    { immediate: true }
)
</script>

<style scoped>
.detail {
  width: 70%;
  margin: 0 auto;
  padding-bottom: 40px;
}

.back {
  display: inline-block;
  margin: 16px 0;
  color: #3670ee;
  cursor: pointer;
  font-size: 14px;
}

.banner {
  width: 100%;
  max-height: 320px;
  object-fit: cover;
  border-radius: 8px;
}

.title {
  font-size: 24px;
  margin: 16px 0 8px;
}

.meta {
  display: flex;
  gap: 16px;
  align-items: center;
  font-size: 13px;
  color: #666;
}

.followed,
.unfollow {
  padding: 3px 10px;
  border-radius: 4px;
  font-size: 13px;
}

.follow {
  border: none;
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

.intro {
  margin: 16px 0;
  font-size: 15px;
  line-height: 1.7;
  color: #333;
}

.sections h2 {
  font-size: 18px;
  border-left: 3px solid #3670ee;
  padding-left: 8px;
  margin: 24px 0 12px;
}

.sections ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.sections li {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  margin-bottom: 10px;
  border: 1px solid #e5e5e5;
  border-radius: 6px;
}

.index {
  width: 22px;
  height: 22px;
  line-height: 22px;
  text-align: center;
  border-radius: 50%;
  background-color: #eff4fe;
  color: #3670ee;
  font-size: 12px;
  flex-shrink: 0;
}

.section-title {
  flex: 1;
  font-size: 15px;
}

.section-id {
  font-size: 12px;
  color: #999;
}

.tip {
  color: #666;
  font-size: 14px;
}

.tip.error {
  color: #d03050;
}
</style>
