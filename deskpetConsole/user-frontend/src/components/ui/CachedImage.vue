<script setup lang="ts">
import { onBeforeUnmount, ref, useAttrs, watch } from 'vue'
import { fetchAssetBlob, readCachedAsset, writeCachedAsset } from '@/utils/assetCache'

defineOptions({
  inheritAttrs: false,
})

const props = defineProps<{
  src?: string | null
  cacheKey: string
  alt: string
}>()

const attrs = useAttrs()
const resolvedSrc = ref<string | null>(null)
const showFallback = ref(false)

let activeUrl: string | null = null
let requestToken = 0

function revokeActiveUrl() {
  if (activeUrl) {
    URL.revokeObjectURL(activeUrl)
    activeUrl = null
  }
}

function applyBlob(blob: Blob) {
  revokeActiveUrl()
  activeUrl = URL.createObjectURL(blob)
  resolvedSrc.value = activeUrl
  showFallback.value = false
}

watch(
  () => [props.src, props.cacheKey],
  async () => {
    requestToken += 1
    const token = requestToken
    showFallback.value = false

    const cachedEntry = await readCachedAsset(props.cacheKey)
    if (token !== requestToken) return

    if (cachedEntry) {
      applyBlob(cachedEntry.blob)
    } else {
      revokeActiveUrl()
      resolvedSrc.value = props.src || null
    }

    if (!props.src) {
      if (!cachedEntry) {
        showFallback.value = true
      }
      return
    }

    if (cachedEntry && cachedEntry.sourceUrl === props.src) {
      return
    }

    const freshBlob = await fetchAssetBlob(props.src)
    if (token !== requestToken || !freshBlob) return

    await writeCachedAsset(props.cacheKey, freshBlob, props.src)
    if (token !== requestToken) return
    applyBlob(freshBlob)
  },
  { immediate: true },
)

function handleError() {
  if (resolvedSrc.value === props.src) {
    showFallback.value = true
  }
}

onBeforeUnmount(() => {
  revokeActiveUrl()
})
</script>

<template>
  <img
    v-if="resolvedSrc && !showFallback"
    v-bind="attrs"
    :src="resolvedSrc"
    :alt="alt"
    @error="handleError"
  >
  <slot v-else name="fallback" />
</template>
