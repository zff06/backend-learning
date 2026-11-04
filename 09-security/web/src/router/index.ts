import { createRouter, createWebHistory } from 'vue-router'
import { useAuth } from '../store/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    // 根路径不在 routes 里决定去哪：redirect 是在守卫之前同步求值的，
    // 那时 roles 还没恢复，会把教师/管理员误判成学生。
    // 所以这里留空组件，交给下面的守卫按角色分发。
    { path: '/', component: { render: () => null } },
    { path: '/login', component: () => import('../views/Login.vue') },
    { path: '/tasks', component: () => import('../views/TaskList.vue') },
    {
      path: '/review',
      component: () => import('../views/ReviewList.vue'),
      meta: { roles: ['TEACHER', 'ADMIN'] }
    },
    {
      path: '/users',
      component: () => import('../views/Users.vue'),
      meta: { roles: ['ADMIN'] }
    },
    { path: '/:pathMatch(.*)*', redirect: '/' }
  ]
})

// 路由守卫：未登录进不去业务页，角色不对进不去别人那页
router.beforeEach(async (to) => {
  const { state, restore, homePath } = useAuth()

  // 刷新后 roles 是空的，先进业务的这一下会把教师误判成学生，
  // 所以拿着令牌但身份还没恢复时，先等一次 /auth/me
  if (state.token && state.roles.length === 0) {
    await restore()
  }

  if (to.path === '/login') return true
  if (!state.token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  // 首页统一按角色分发
  if (to.path === '/') {
    return { path: homePath() }
  }
  const required = to.meta.roles as string[] | undefined
  if (required && !required.some((r) => state.roles.includes(r))) {
    // 角色不对挡回自己该看的页面，不报白屏
    return { path: homePath() }
  }
  return true
})

export default router
