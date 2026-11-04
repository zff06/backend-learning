<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from './store/auth'
import { useToast } from './store/toast'

const router = useRouter()
const { state, logout, restore } = useAuth()
const { state: toast } = useToast()

// 应用启动时先读令牌再拉一次身份，别一上来就按未登录渲染
restore()

const isLogin = computed(() => router.currentRoute.value.path === '/login')

const roleText = computed(() => {
  if (state.roles.includes('ADMIN')) return 'ADMIN'
  if (state.roles.includes('TEACHER')) return 'TEACHER'
  return 'STUDENT'
})

const navLinks = computed(() => {
  const links: { path: string; label: string }[] = []
  if (state.roles.includes('STUDENT') || state.roles.includes('ADMIN')) {
    links.push({ path: '/tasks', label: '我的提交' })
  }
  if (state.roles.includes('TEACHER') || state.roles.includes('ADMIN')) {
    links.push({ path: '/review', label: '待批改' })
  }
  if (state.roles.includes('ADMIN')) {
    links.push({ path: '/users', label: '账号管理' })
  }
  return links
})

function handleLogout() {
  logout()
  router.push('/login')
}
</script>

<template>
  <div class="toast" v-if="toast.visible" :class="toast.kind">{{ toast.message }}</div>
  <header class="topbar" v-if="!isLogin">
    <div class="brand">实训任务提交与批改 <span class="module">09 · 认证与授权</span></div>
    <nav class="nav">
      <RouterLink v-for="link in navLinks" :key="link.path" :to="link.path">{{ link.label }}</RouterLink>
    </nav>
    <div class="right">
      <span class="name">{{ state.nickname }}</span>
      <span class="badge">{{ roleText }}</span>
      <button class="logout" @click="handleLogout">退出</button>
    </div>
  </header>
  <main class="main" :class="{ plain: isLogin }">
    <RouterView />
  </main>
</template>

<style scoped>
.toast {
  position: fixed;
  top: 16px;
  left: 50%;
  transform: translateX(-50%);
  background: #1f2430;
  color: #fff;
  padding: 10px 20px;
  border-radius: 8px;
  z-index: 999;
  font-size: 14px;
}
.toast.error {
  background: #e5484d;
}
.topbar {
  height: 56px;
  background: #fff;
  display: flex;
  align-items: center;
  padding: 0 24px;
  gap: 28px;
  box-shadow: 0 1px 4px rgba(31, 36, 48, 0.06);
  position: sticky;
  top: 0;
  z-index: 10;
}
.brand {
  font-weight: 700;
  font-size: 15px;
  white-space: nowrap;
}
.module {
  color: #8a91a3;
  font-weight: 400;
  font-size: 13px;
  margin-left: 4px;
}
.nav {
  display: flex;
  gap: 20px;
  flex: 1;
}
.nav a {
  color: #5a6172;
  text-decoration: none;
  font-size: 14px;
  padding: 4px 2px;
}
.nav a.router-link-active {
  color: #3b5bfd;
  font-weight: 600;
}
.right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.name {
  font-size: 14px;
}
.badge {
  background: #eceefe;
  color: #3b5bfd;
  border-radius: 10px;
  padding: 2px 10px;
  font-size: 12px;
  font-weight: 600;
}
.logout {
  background: none;
  color: #8a91a3;
  font-size: 13px;
}
.logout:hover {
  color: #e5484d;
}
.main {
  max-width: 1080px;
  margin: 0 auto;
  padding: 24px;
}
.main.plain {
  max-width: none;
}
</style>
