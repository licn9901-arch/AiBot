import { onMounted, onUnmounted, ref } from 'vue'

export function usePolling(fn: () => Promise<any> | void, interval = 5000) {
  const timer = ref<number | null>(null)
  const isPolling = ref(false)

  const startPolling = () => {
    if (timer.value) return
    isPolling.value = true
    execute()
    timer.value = window.setInterval(execute, interval)
  }

  const stopPolling = () => {
    if (timer.value) {
      clearInterval(timer.value)
      timer.value = null
    }
    isPolling.value = false
  }

  const execute = async () => {
    if (document.hidden) return
    try {
      await fn()
    } catch (e) {
      console.error('Polling error:', e)
    }
  }

  const handleVisibilityChange = () => {
    if (document.hidden) {
      stopPolling()
    } else {
      execute()
      startPolling()
    }
  }

  onMounted(() => {
    startPolling()
    document.addEventListener('visibilitychange', handleVisibilityChange)
  })

  onUnmounted(() => {
    stopPolling()
    document.removeEventListener('visibilitychange', handleVisibilityChange)
  })

  return { isPolling, startPolling, stopPolling }
}
