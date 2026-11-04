<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { fetchMyTasks, submitTask } from '../api'
import type { TaskView } from '../types/api'
import { useToast } from '../store/toast'

const { show } = useToast()

const tasks = ref<TaskView[]>([])
const title = ref('')
const content = ref('')
const filter = ref<'ALL' | 'SUBMITTED' | 'REVIEWED'>('ALL')
const expanded = ref<number | null>(null)

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
  if (filter.value === 'ALL') return tasks.value
  return tasks.value.filter((t) => t.status === filter.value)
})

async function load() {
  tasks.value = await fetchMyTasks()
}

async function handleSubmit() {
  if (!title.value || !content.value) {
    show('标题和完成情况都要填', 'error')
    return
  }
  await submitTask(title.value, content.value)
  show('提交成功')
  title.value = ''
  content.value = ''
  await load()
}

function formatDate(iso: string) {
  return iso ? iso.replace('T', ' ').slice(0, 16) : '—'
}

onMounted(load)
</script>

<template>
  <div class="stats">
    <div class="card stat"><div class="label">已提交</div><div class="num">{{ stats.total }} 份</div></div>
    <div class="card stat"><div class="label">待批改</div><div class="num warn">{{ stats.pending }} 份</div></div>
    <div class="card stat"><div class="label">已批改</div><div class="num ok">{{ stats.reviewed }} 份</div></div>
    <div class="card stat"><div class="label">平均分</div><div class="num">{{ stats.avg }} {{ stats.avg === '—' ? '' : '分' }}</div></div>
  </div>

  <div class="grid">
    <div class="card form">
      <h2>提交实训任务</h2>
      <p class="hint">提交人由后端令牌写入，请求里没有用户名字段</p>
      <label class="field-label">任务标题</label>
      <input v-model="title" placeholder="例如：实训六：分页查询与条件筛选" />
      <label class="field-label">完成情况</label>
      <textarea v-model="content" placeholder="做了哪些、卡在哪里"></textarea>
      <button class="btn-primary submit-btn" @click="handleSubmit">提交</button>
    </div>

    <div class="card list">
      <h2>我的提交</h2>
      <p class="hint">接口是 /api/tasks/mine，URL 里没有用户名，改也改不出别人的数据</p>
      <div class="toolbar">
        <div class="tabs">
          <button :class="{ active: filter === 'ALL' }" @click="filter = 'ALL'">全部 {{ stats.total }}</button>
          <button :class="{ active: filter === 'SUBMITTED' }" @click="filter = 'SUBMITTED'">待批改 {{ stats.pending }}</button>
          <button :class="{ active: filter === 'REVIEWED' }" @click="filter = 'REVIEWED'">已批改 {{ stats.reviewed }}</button>
        </div>
        <span class="count">共 {{ filtered.length }} 条</span>
      </div>
      <div class="rows">
        <div v-for="t in filtered" :key="t.id" class="row" @click="expanded = expanded === t.id ? null : t.id">
          <div class="row-head">
            <span class="row-title">{{ t.title }}</span>
            <span class="tag" :class="t.status === 'REVIEWED' ? 'ok' : 'warn'">{{ t.statusText }}</span>
            <span class="score" :class="{ ok: t.score !== null }">{{ t.score ?? '—' }}</span>
            <span class="time">{{ formatDate(t.submittedAt) }}</span>
            <button class="btn-small">查看</button>
          </div>
          <div class="comment" v-if="t.comment">评语：{{ t.comment }}</div>
          <div class="detail" v-if="expanded === t.id">
            <p>{{ t.content }}</p>
            <p class="meta">提交于 {{ formatDate(t.submittedAt) }}<template v-if="t.reviewedAt"> · 批改于 {{ formatDate(t.reviewedAt) }}</template></p>
            <div class="review-box" v-if="t.status === 'REVIEWED'">
              得分 {{ t.score }}<br />评语 {{ t.comment }}
            </div>
            <div class="review-box pending" v-else>教师还没批</div>
            <button class="btn-plain fold">收起</button>
          </div>
        </div>
        <p class="empty" v-if="filtered.length === 0">还没有提交记录</p>
      </div>
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
.grid {
  display: grid;
  grid-template-columns: 340px 1fr;
  gap: 14px;
  align-items: start;
}
h2 {
  font-size: 16px;
  margin-bottom: 4px;
}
.hint {
  color: #8a91a3;
  font-size: 12px;
  margin-bottom: 8px;
}
.submit-btn {
  width: 100%;
  margin-top: 18px;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 10px 0;
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
.count {
  color: #8a91a3;
  font-size: 12px;
}
.row {
  border-top: 1px solid #f0f1f5;
  padding: 12px 0;
  cursor: pointer;
}
.row-head {
  display: flex;
  align-items: center;
  gap: 12px;
}
.row-title {
  flex: 1;
  font-weight: 500;
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
.score {
  width: 36px;
  text-align: center;
  font-weight: 600;
}
.score.ok {
  color: #2e9e5b;
}
.time {
  color: #8a91a3;
  font-size: 13px;
  width: 130px;
}
.comment {
  color: #8a91a3;
  font-size: 12px;
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.detail {
  margin-top: 10px;
  background: #f8f9fc;
  border-radius: 10px;
  padding: 14px;
}
.detail p {
  font-size: 13px;
  line-height: 1.7;
}
.meta {
  color: #8a91a3;
  font-size: 12px;
  margin-top: 6px;
}
.review-box {
  margin-top: 10px;
  background: #e6f6ec;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 13px;
  color: #227343;
  line-height: 1.7;
}
.review-box.pending {
  background: #fdf1e7;
  color: #b96a24;
}
.fold {
  margin-top: 10px;
  padding: 4px 14px;
}
.empty {
  text-align: center;
  color: #8a91a3;
  padding: 24px 0;
}
@media (max-width: 900px) {
  .grid {
    grid-template-columns: 1fr;
  }
  .stats {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
