import { createRouter, createWebHistory } from 'vue-router'
import ZhiHu from '../components/ZhiHu.vue'

const router = createRouter({
    history: createWebHistory(),
    routes: [
        {
            path: '/',
            name: 'specialList',
            component: ZhiHu,
            meta: { title: '知乎专题' }
        },
        {
            path: '/special/:id',
            name: 'specialDetail',
            // 详情页懒加载：首屏只下载列表页代码，点进去才拉这个 chunk
            component: () => import('../components/SpecialDetail.vue'),
            meta: { title: '专题详情' }
        },
        {
            // 兜底：未匹配的路径（history 模式下用户手输错地址）给个 404 页面，别白屏
            path: '/:pathMatch(.*)*',
            name: 'notFound',
            component: () => import('../components/NotFound.vue'),
            meta: { title: '页面不存在' }
        }
    ],
    // 前进/后退回到列表页时，位置还原到之前滚动的地方
    scrollBehavior(_to, _from, savedPosition) {
        return savedPosition ?? { top: 0 }
    }
})

// 每个页面用自己的标题，浏览器标签页和后退历史里都能分清
router.afterEach((to) => {
    const title = to.meta.title
    document.title = typeof title === 'string' ? `${title} - 知乎专题` : '知乎专题'
})

export default router
