<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { createUser, deleteUser, fetchUsers } from '../api'
import type { UserProfile } from '../types/api'
import { useAuth } from '../store/auth'
import { useToast } from '../store/toast'

const { state } = useAuth()
const { show } = useToast()

const users = ref<UserProfile[]>([])
const filter = ref<'ALL' | 'STUDENT' | 'TEACHER' | 'ADMIN'>('ALL')
const keyword = ref('')
const form = ref({ username: '', nickname: '', password: '', roles: 'STUDENT' })

const stats = computed(() => ({
  total: users.value.length,
  students: users.value.filter((u) => u.roles.includes('STUDENT')).length,
  teachers: users.value.filter((u) => u.roles.includes('TEACHER')).length,
  disabled: users.value.filter((u) => !u.enabled).length
}))

const filtered = computed(() => {
  let rows = users.value
  if (filter.value !== 'ALL') rows = rows.filter((u) => u.roles.includes(filter.value))
  const kw = keyword.value.trim()
  if (kw) rows = rows.filter((u) => u.username.includes(kw) || u.nickname.includes(kw))
  return rows
})

async function load() {
  users.value = await fetchUsers()
}

async function handleCreate() {
  const f = form.value
  if (!f.username || !f.nickname || !f.password) {
    show('用户名、姓名、初始密码都要填', 'error')
    return
  }
  await createUser({ username: f.username, nickname: f.nickname, password: f.password, roles: [f.roles] })
  show('账号已创建，响应里应该没有密码字段')
  form.value = { username: '', nickname: '', password: '', roles: 'STUDENT' }
  await load()
}

async function handleDelete(user: UserProfile) {
  // 后端规则：删自己返回 30005，这里直接挡一次省一次请求
  if (user.username === state.username) {
    show('不能删除当前登录账号', 'error')
    return
  }
  await deleteUser(user.username)
  show('已删除 ' + user.username)
  await load()
}

onMounted(load)
</script>

<template>
  <div class="stats">
    <div class="card stat"><div class="label">账号总数</div><div class="num">{{ stats.total }} 个</div></div>
    <div class="card stat"><div class="label">学生</div><div class="num">{{ stats.students }} 个</div></div>
    <div class="card stat"><div class="label">教师</div><div class="num">{{ stats.teachers }} 个</div></div>
    <div class="card stat"><div class="label">停用</div><div class="num warn">{{ stats.disabled }} 个</div></div>
  </div>

  <div class="grid">
    <div class="card form">
      <h2>新增账号</h2>
      <p class="hint">只有 ADMIN 能调，教师访问 /admin/users 会拿到 403</p>
      <label class="field-label">用户名</label>
      <input v-model="form.username" placeholder="student11" />
      <label class="field-label">姓名</label>
      <input v-model="form.nickname" placeholder="马嘉懿" />
      <label class="field-label">初始密码</label>
      <input v-model="form.password" placeholder="Student#2026" />
      <label class="field-label">角色</label>
      <select v-model="form.roles">
        <option value="STUDENT">学生 STUDENT</option>
        <option value="TEACHER">教师 TEACHER</option>
        <option value="ADMIN">管理员 ADMIN</option>
      </select>
      <button class="btn-primary create-btn" @click="handleCreate">创建</button>
    </div>

    <div class="card list">
      <h2>账号列表</h2>
      <p class="hint">响应里没有密码字段，出参要压住不带它</p>
      <div class="toolbar">
        <div class="tabs">
          <button :class="{ active: filter === 'ALL' }" @click="filter = 'ALL'">全部 {{ stats.total }}</button>
          <button :class="{ active: filter === 'STUDENT' }" @click="filter = 'STUDENT'">学生 {{ stats.students }}</button>
          <button :class="{ active: filter === 'TEACHER' }" @click="filter = 'TEACHER'">教师 {{ stats.teachers }}</button>
          <button :class="{ active: filter === 'ADMIN' }" @click="filter = 'ADMIN'">管理员</button>
        </div>
        <input class="search" v-model="keyword" placeholder="按用户名或姓名搜" />
      </div>
      <div class="table-head">
        <span class="col-user">用户名</span>
        <span class="col-name">姓名</span>
        <span class="col-role">角色</span>
        <span class="col-status">状态</span>
        <span class="col-op"></span>
      </div>
      <div v-for="u in filtered" :key="u.username" class="table-row">
        <span class="col-user">{{ u.username }}</span>
        <span class="col-name">{{ u.nickname }}</span>
        <span class="col-role">{{ u.roles.join(' / ') }}</span>
        <span class="col-status">
          <span class="tag" :class="u.enabled ? 'ok' : 'warn'">{{ u.enabled ? '启用' : '停用' }}</span>
        </span>
        <span class="col-op">
          <button class="btn-danger" @click="handleDelete(u)">删除</button>
        </span>
      </div>
      <p class="empty" v-if="filtered.length === 0">筛出 0 个</p>
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
.grid {
  display: grid;
  grid-template-columns: 320px 1fr;
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
.create-btn {
  width: 100%;
  margin-top: 18px;
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
  flex-wrap: wrap;
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
  grid-template-columns: 120px 100px 110px 80px 70px;
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
.empty {
  text-align: center;
  color: #8a91a3;
  padding: 24px 0;
}
@media (max-width: 900px) {
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>
