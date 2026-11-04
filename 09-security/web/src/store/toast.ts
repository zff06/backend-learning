import { reactive } from 'vue'

/**
 * 轻量全局提示：顶部一条，2.5 秒自动消失
 */
interface ToastState {
  message: string
  visible: boolean
  kind: 'info' | 'error'
}

const state = reactive<ToastState>({ message: '', visible: false, kind: 'info' })
let timer: ReturnType<typeof setTimeout> | null = null

function show(message: string, kind: 'info' | 'error' = 'info') {
  state.message = message
  state.kind = kind
  state.visible = true
  if (timer) clearTimeout(timer)
  timer = setTimeout(() => {
    state.visible = false
  }, 2500)
}

export function useToast() {
  return { state, show }
}
