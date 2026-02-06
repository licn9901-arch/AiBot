import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { DeviceResponse } from '@/types/device'
import { getMyDevices } from '@/api/device'

export const useDeviceStore = defineStore('device', () => {
  const devices = ref<DeviceResponse[]>([])
  const loading = ref(false)

  async function fetchDevices() {
    loading.value = true
    try {
      devices.value = await getMyDevices()
    } finally {
      loading.value = false
    }
  }

  return { devices, loading, fetchDevices }
})
