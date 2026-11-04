<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { login } from '../api'
import { useAuth } from '../store/auth'

const router = useRouter()
const route = useRoute()
const { setLogin, homePath } = useAuth()

const username = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

const quickFills = [
  { label: '学生 student01', username: 'student01', password: 'Student#2026' },
  { label: '教师 teacher01', username: 'teacher01', password: 'Teacher#2026' },
  { label: '管理员 admin', username: 'admin', password: 'Admin#2026' },
  { label: '停用 guest', username: 'guest', password: 'Guest#2026' }
]

function fill(account: { username: string; password: string }) {
  username.value = account.username
  password.value = account.password
}

async function handleLogin() {
  if (!username.value || !password.value) {
    error.value = '用户名和密码都要填'
    return
  }
  loading.value = true
  error.value = ''
  try {
    const data = await login(username.value, password.value)
    setLogin(data)
    // 被守卫拦下来过的，回原来那一页；否则按角色落到各自的首页（教师是待批改）
    const redirect = (route.query.redirect as string) || homePath()
    router.push(redirect)
  } catch (e: any) {
    // 业务失败：拦截器已弹过提示，登录页再用红色横幅强调一次
    error.value = e?.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-wrap">
    <div class="card login-card">
      <h1>实训任务提交与批改</h1>
      <p class="sub">登录后获取令牌，之后每个请求都会自动带上</p>
      <div class="error-banner" v-if="error">{{ error }}</div>
      <label class="field-label">用户名</label>
      <input v-model="username" placeholder="用户名" @keyup.enter="handleLogin" />
      <label class="field-label">密码</label>
      <input v-model="password" type="password" placeholder="密码" @keyup.enter="handleLogin" />
      <button class="btn-primary login-btn" :disabled="loading" @click="handleLogin">
        {{ loading ? '登录中…' : '登录' }}
      </button>
      <div class="divider">点一下直接填入，不用手打</div>
      <div class="quick">
        <button v-for="q in quickFills" :key="q.username" class="chip" @click="fill(q)">
          {{ q.label }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-wrap {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
}
.login-card {
  width: 380px;
  padding: 28px;
  box-shadow: 0 8px 30px rgba(31, 36, 48, 0.08);
}
h1 {
  font-size: 20px;
  margin-bottom: 6px;
}
.sub {
  color: #8a91a3;
  font-size: 13px;
  margin-bottom: 14px;
}
.error-banner {
  background: #fdecec;
  color: #e5484d;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 13px;
  margin-bottom: 12px;
}
.login-btn {
  width: 100%;
  margin-top: 20px;
  padding: 11px;
  font-size: 15px;
}
.divider {
  margin-top: 18px;
  padding-top: 14px;
  border-top: 1px dashed #e4e6ec;
  color: #8a91a3;
  font-size: 12px;
}
.quick {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}
.chip {
  background: #f2f3f7;
  color: #5a6172;
  border-radius: 14px;
  padding: 5px 12px;
  font-size: 12px;
}
.chip:hover {
  background: #eceefe;
  color: #3b5bfd;
}
</style>
