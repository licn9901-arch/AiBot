const ASSET_CACHE_NAME = 'cubee-asset-cache-v1'
const ASSET_CACHE_PREFIX = 'https://cubee.local/__asset_cache__/'

function buildCacheRequest(cacheKey: string): Request {
  return new Request(`${ASSET_CACHE_PREFIX}${encodeURIComponent(cacheKey)}`)
}

async function openAssetCache(): Promise<Cache | null> {
  if (typeof window === 'undefined' || !('caches' in window)) {
    return null
  }

  return window.caches.open(ASSET_CACHE_NAME)
}

export interface CachedAssetEntry {
  blob: Blob
  sourceUrl: string | null
}

export async function readCachedAsset(cacheKey: string): Promise<CachedAssetEntry | null> {
  const cache = await openAssetCache()
  if (!cache) return null

  const response = await cache.match(buildCacheRequest(cacheKey))
  if (!response) return null

  return {
    blob: await response.blob(),
    sourceUrl: response.headers.get('X-Source-Url'),
  }
}

export async function writeCachedAsset(cacheKey: string, blob: Blob, sourceUrl: string): Promise<void> {
  const cache = await openAssetCache()
  if (!cache) return

  const headers = new Headers()
  headers.set('Content-Type', blob.type || 'application/octet-stream')
  headers.set('X-Source-Url', sourceUrl)

  await cache.put(buildCacheRequest(cacheKey), new Response(blob, { headers }))
}

export async function fetchAssetBlob(sourceUrl: string, signal?: AbortSignal): Promise<Blob | null> {
  try {
    const response = await fetch(sourceUrl, {
      mode: 'cors',
      credentials: 'omit',
      signal,
    })

    if (!response.ok) {
      return null
    }

    return await response.blob()
  } catch {
    return null
  }
}
