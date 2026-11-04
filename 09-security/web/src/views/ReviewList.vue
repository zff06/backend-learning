<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { fetchAllTasks, reviewTask } from '../api'
import type { TaskView } from '../types/api'
import { useToast } from '../store/toast'

const { show } = useToast()

const tasks = ref<TaskView[]>([])
const filter = ref<'ALL' | 'SUBMITTED' | 'REVIEWED'>('ALL')
const keyword = ref('')
const editing = ref<TaskView | null>(null)
const score = ref<number | null>(null)
const comment = ref('')

const stats = computed(() => {
  const done = tasks.value.filter((t) => t.status === 'REVIEWED')
  const scores = done.map((t) => t.score ?? 0)
  return {
    total: tasks.value.length,
    pending: tasks.value.filter((t) => t.status === 'SUBMITTED').length,
    reviewed: done.length,
    avg: scores.length ? (scores.reduce((a, b) => a + b, 0) / scores.length).toFixed(1) : '—'
  }
})

const filtered = computed(() => {
  let rows = tasks.value
  if (filter.value !== 'ALL') rows = rows.filter((t) => t.status === filter.value)
  const kw = keyword.value.trim()
  if (kw) {
    rows = rows.filter(
      (t) => t.title.includes(kw) || t.owner.includes(kw) || t.ownerNickname.includes(kw)
    )
  }
  return rows
})

async function load() {
  tasks.value = await fetchAllTasks()
}

function openReview(task: TaskView) {
  editing.value = task
  score.value = null
  comment.value = ''
}

function cancelReview() {
  editing.value = null
}

async function saveReview() {
  if (!editing.value) return
  if (score.value === null || score.value < 0 || score.value > 100) {
    show('分数要在 0 到 100 之间', 'error')
    return
  }
  const updated = await reviewTask(editing.value.id, score.value, comment.value)
  // 同一份不能批两次：后端 30012 拦，前端改完直接用返回值更新行
  const idx = tasks.value.findIndex((t) => t.id === updated.id)
  if (idx >= 0) tasks.value[idx] = updated
  show('批改已保存')
  editing.value = null
}

function formatDate(iso: string) {
  return iso ? iso.replace('T', ' ').slice(0, 16) : '—'
}

onMounted(load)
</script>

<template>
  <div class="stats">
    <div class="card stat"><div class="label">全部提交</div><div class="num">{{ stats.total }} 份</div></div>
    <div class="card stat"><div class="label">待批改</div><div class="num warn">{{ stats.pending }} 份</div></div>
    <div class="card stat"><div class="label">已批改</div><div class="num ok">{{ stats.reviewed }} 份</div></div>
    <div class="card stat"><div class="label">平均分</div><div class="num">{{ stats.avg }} {{ stats.avg === '—' ? '' : '分' }}</div></div>
  </div>

  <div class="card">
    <h2>全部提交</h2>
    <p class="hint">接口 /api/tasks 需要 task:review 权限点，学生调这个接口会拿到 403</p>
    <div class="toolbar">
      <div class="tabs">
        <button :class="{ active: filter === 'ALL' }" @click="filter = 'ALL'">全部 {{ stats.total }}</button>
        <button :class="{ active: filter === 'SUBMITTED' }" @click="filter = 'SUBMITTED'">待批改 {{ stats.pending }}</button>
        <button :class="{ active: filter === 'REVIEWED' }" @click="filter = 'REVIEWED'">已批改 {{ stats.reviewed }}</button>
      </div>
      <input class="search" v-model="keyword" placeholder="按姓名、账号或标题搜" />
    </div>
    <div class="table-head">
      <span class="col-id">#</span>
      <span class="col-title">标题</span>
      <span class="col-owner">提交人</span>
      <span class="col-status">状态</span>
      <span class="col-score">分数</span>
      <span class="col-time">提交时间</span>
      <span class="col-op"></span>
    </div>
    <div v-for="t in filtered" :key="t.id" class="table-row">
      <span class="col-id">{{ t.id }}</span>
      <span class="col-title">
        <div class="title-line">{{ t.title }}</div>
        <div class="comment" v-if="t.comment">评语：{{ t.comment }}</div>
      </span>
      <span class="col-owner">
        <div>{{ t.ownerNickname }}</div>
        <div class="sub-owner">{{ t.owner }}</div>
      </span>
      <span class="col-status">
        <span class="tag" :class="t.status === 'REVIEWED' ? 'ok' : 'warn'">{{ t.statusText }}</span>
      </span>
      <span class="col-score" :class="{ ok: t.score !== null }">{{ t.score ?? '—' }}</span>
      <span class="col-time">{{ formatDate(t.submittedAt) }}</span>
      <span class="col-op">
        <button v-if="t.status === 'SUBMITTED'" class="btn-small" @click="openReview(t)">批改</button>
        <span v-else class="done">已批</span>
      </span>
    </div>
    <p class="empty" v-if="filtered.length === 0">筛出 0 条</p>
  </div>

  <div class="card review-card" v-if="editing">
    <h2>批改：{{ editing.title }}</h2>
    <p class="hint">
      {{ editing.ownerNickname }}（{{ editing.owner }}）提交于 {{ formatDate(editing.submittedAt) }} ·
      同一份不能改两次，后端会返回业务码 30012
    </p>
    <div class="origin-content">{{ editing.content }}</div>
    <label class="field-label">分数（0 到 100）</label>
    <input class="score-input" v-model.number="score" type="number" min="0" max="100" />
    <label class="field-label">评语</label>
    <textarea v-model="comment" placeholder="写清哪里对、哪里要改"></textarea>
    <div class="actions">
      <button class="btn-primary" @click="saveReview">保存批改</button>
      <button class="btn-plain" @click="cancelReview">取消</button>
    </div>
  </div>
</template>

<style scoped>
.stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 18px;
}
.stat .label {
  color: #8a91a3;
  font-size: 13px;
}
.stat .num {
  font-size: 26px;
  font-weight: 700;
  margin-top: 4px;
}
.num.warn {
  color: #e58a3d;
}
.num.ok {
  color: #2e9e5b;
}
h2 {
  font-size: 16px;
  margin-bottom: 4px;
}
.hint {
  color: #8a91a3;
  font-size: 12px;
  margin-bottom: 10px;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}
.tabs {
  display: flex;
  gap: 8px;
}
.tabs button {
  background: #f2f3f7;
  border-radius: 16px;
  padding: 5px 14px;
  font-size: 13px;
  color: #5a6172;
}
.tabs button.active {
  background: #eceefe;
  color: #3b5bfd;
  font-weight: 600;
}
.search {
  width: 220px;
}
.table-head,
.table-row {
  display: grid;
  grid-template-columns: 40px 1fr 110px 80px 60px 150px 70px;
  gap: 8px;
  align-items: center;
  padding: 10px 0;
}
.table-head {
  color: #8a91a3;
  font-size: 12px;
  border-bottom: 1px solid #f0f1f5;
}
.table-row {
  border-bottom: 1px solid #f0f1f5;
  font-size: 13px;
}
.title-line {
  font-weight: 500;
}
.comment {
  color: #8a91a3;
  font-size: 12px;
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.sub-owner {
  color: #8a91a3;
  font-size: 12px;
}
.tag {
  border-radius: 10px;
  padding: 2px 10px;
  font-size: 12px;
}
.tag.warn {
  background: #fdf1e7;
  color: #e58a3d;
}
.tag.ok {
  background: #e6f6ec;
  color: #2e9e5b;
}
.col-score {
  font-weight: 600;
}
.col-score.ok {
  color: #2e9e5b;
}
.col-time {
  color: #8a91a3;
  font-size: 12px;
}
.done {
  color: #b6bcc8;
  font-size: 13px;
}
.empty {
  text-align: center;
  color: #8a91a3;
  padding: 24px 0;
}
.review-card {
  margin-top: 18px;
}
.origin-content {
  background: #f8f9fc;
  border-radius: 10px;
  padding: 12px 14px;
  font-size: 13px;
  line-height: 1.7;
}
.score-input {
  width: 200px;
}
.actions {
  margin-top: 16px;
  display: flex;
  gap: 10px;
}
</style>
