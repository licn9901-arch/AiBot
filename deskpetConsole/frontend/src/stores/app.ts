import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', {
  state: () => ({
    apiBase: import.meta.env.VITE_API_BASE ?? '',
    wsBase: import.meta.env.VITE_WS_BASE ?? ''
  })
})
