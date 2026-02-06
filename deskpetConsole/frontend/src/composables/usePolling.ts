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
        // Skip if page is hidden to save resources
        if (document.hidden) return
        try {
            await fn()
        } catch (e) {
            console.error('Polling error:', e)
            // Optionally stop polling on error? 
            // For now, keep retrying as transient network errors shouldn't stop polling permanently
        }
    }

    const handleVisibilityChange = () => {
        if (document.hidden) {
            stopPolling()
        } else {
            // Resume polling immediately when tab becomes visible
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

    return {
        isPolling,
        startPolling,
        stopPolling
    }
}
