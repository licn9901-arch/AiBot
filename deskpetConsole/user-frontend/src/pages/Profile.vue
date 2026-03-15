<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { showToast } from 'vant'
import AppStatusBadge from '@/components/ui/AppStatusBadge.vue'
import CachedImage from '@/components/ui/CachedImage.vue'
import SectionHeader from '@/components/ui/SectionHeader.vue'
import { useResponsive } from '@/composables/useResponsive'
import { uploadImage } from '@/api/file'
import { getMyLicenses } from '@/api/license'
import { updateCurrentUser } from '@/api/user'
import { useUserStore } from '@/stores/user'
import type { LicenseCodeResponse, LicenseStatus } from '@/types/license'
import { writeCachedAsset } from '@/utils/assetCache'
import { formatTime } from '@/utils/format'

const { isMobile } = useResponsive()
const userStore = useUserStore()

const loading = ref(false)
const saving = ref(false)
const avatarUploading = ref(false)
const licenses = ref<LicenseCodeResponse[]>([])
const avatarInput = ref<HTMLInputElement | null>(null)
const localAvatarPreviewUrl = ref<string | null>(null)
const editForm = reactive({
  email: '',
  phone: '',
  avatarKey: '',
  avatarUrl: '',
})

const userSummary = computed(() => userStore.userInfo)

function notify(message: string, type: 'success' | 'warning' | 'error' = 'success') {
  if (isMobile.value) {
    showToast(message)
    return
  }

  if (type === 'success') ElMessage.success(message)
  if (type === 'warning') ElMessage.warning(message)
  if (type === 'error') ElMessage.error(message)
}

function syncForm() {
  editForm.email = userStore.userInfo?.email ?? ''
  editForm.phone = userStore.userInfo?.phone ?? ''
  editForm.avatarKey = userStore.userInfo?.avatarKey ?? ''
  editForm.avatarUrl = userStore.userInfo?.avatar ?? ''
  clearLocalAvatarPreview()
}

function clearLocalAvatarPreview() {
  if (localAvatarPreviewUrl.value) {
    URL.revokeObjectURL(localAvatarPreviewUrl.value)
    localAvatarPreviewUrl.value = null
  }
}

function setLocalAvatarPreview(file: Blob) {
  clearLocalAvatarPreview()
  localAvatarPreviewUrl.value = URL.createObjectURL(file)
}

function statusLabel(status: LicenseStatus) {
  if (status === 'ACTIVATED') return '已激活'
  if (status === 'REVOKED') return '已撤销'
  return '未使用'
}

function statusTone(status: LicenseStatus) {
  if (status === 'ACTIVATED') return 'success'
  if (status === 'REVOKED') return 'danger'
  return 'warning'
}

function mobileBindingLabel(deviceId?: string | null) {
  return deviceId ? '已绑定设备' : '未绑定设备'
}

async function fetchLicenses() {
  loading.value = true
  try {
    licenses.value = await getMyLicenses()
  } finally {
    loading.value = false
  }
}

async function saveProfile() {
  saving.value = true
  try {
    await updateCurrentUser({
      email: editForm.email || undefined,
      phone: editForm.phone || undefined,
      avatar: editForm.avatarKey,
    })
    await userStore.fetchUser()
    syncForm()
    notify('资料保存成功')
  } catch {
    notify('资料保存失败，请稍后重试', 'error')
  } finally {
    saving.value = false
  }
}

async function handleAvatarSelected(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  setLocalAvatarPreview(file)
  avatarUploading.value = true
  try {
    const response = await uploadImage(file, 'avatar')
    await writeCachedAsset(`avatar:${response.objectKey}`, file, response.url)
    editForm.avatarKey = response.objectKey
    editForm.avatarUrl = response.url
    notify('头像上传成功')
  } catch {
    notify('头像上传失败，请重试', 'error')
  } finally {
    avatarUploading.value = false
    input.value = ''
  }
}

function triggerAvatarUpload() {
  avatarInput.value?.click()
}

function clearAvatar() {
  clearLocalAvatarPreview()
  editForm.avatarKey = ''
  editForm.avatarUrl = ''
}

onMounted(async () => {
  await userStore.fetchUser()
  syncForm()
  await fetchLicenses()
})

onBeforeUnmount(() => {
  clearLocalAvatarPreview()
})
</script>

<template>
  <div v-if="isMobile" class="mobile-profile-page">
    <section class="mobile-page-heading">
      <div>
        <h1 class="mobile-page-title">个人中心</h1>
      </div>
      <button type="button" class="mobile-inline-button" @click="fetchLicenses">
        {{ loading ? '刷新中' : '刷新授权码' }}
      </button>
    </section>

    <section class="mobile-panel-card mobile-profile-card">
      <form class="mobile-form-stack" @submit.prevent="saveProfile">
        <input
          ref="avatarInput"
          type="file"
          accept="image/png,image/jpeg,image/webp,image/gif"
          style="display: none"
          @change="handleAvatarSelected"
        >
        <button type="button" class="mobile-profile-summary mobile-profile-summary-trigger" :disabled="avatarUploading" @click="triggerAvatarUpload">
          <div class="mobile-profile-avatar">
            <img
              v-if="localAvatarPreviewUrl"
              :src="localAvatarPreviewUrl"
              alt="头像预览"
              class="mobile-profile-avatar-image"
            >
            <CachedImage
              v-else-if="editForm.avatarUrl && editForm.avatarKey"
              :src="editForm.avatarUrl"
              :cache-key="`avatar:${editForm.avatarKey}`"
              alt="头像预览"
              class="mobile-profile-avatar-image"
            >
              <template #fallback>
                <span>{{ userSummary?.username?.charAt(0).toUpperCase() || 'C' }}</span>
              </template>
            </CachedImage>
            <span v-else>{{ userSummary?.username?.charAt(0).toUpperCase() || 'C' }}</span>
          </div>
          <div class="mobile-profile-summary-copy">
            <div class="mobile-profile-name">{{ userSummary?.username || 'Cubee 主账号' }}</div>
            <div class="mobile-profile-subtitle">{{ avatarUploading ? '头像上传中...' : '点击头像更换图片' }}</div>
          </div>
        </button>

        <div class="ui-field">
          <label class="ui-field-label" for="mobile-profile-email">邮箱</label>
          <input id="mobile-profile-email" v-model="editForm.email" class="ui-input mobile-auth-input" type="email" placeholder="请输入邮箱地址">
        </div>
        <div class="ui-field">
          <label class="ui-field-label" for="mobile-profile-phone">手机号</label>
          <input id="mobile-profile-phone" v-model="editForm.phone" class="ui-input mobile-auth-input" type="tel" placeholder="请输入手机号">
        </div>
        <div v-if="editForm.avatarUrl" class="mobile-profile-inline-actions">
          <button type="button" class="mobile-secondary-button" @click="clearAvatar">清空头像</button>
        </div>

        <button type="submit" class="mobile-submit-button" :disabled="saving">
          {{ saving ? '保存中...' : '保存资料' }}
        </button>

        <div class="mobile-profile-inline-actions">
          <button type="button" class="mobile-text-button" @click="syncForm">重置</button>
          <button type="button" class="mobile-text-button" @click="userStore.logout()">退出登录</button>
        </div>
      </form>
    </section>

    <section class="mobile-license-section">
      <h2 class="mobile-section-title">我的授权码</h2>
      <div v-if="loading && licenses.length === 0" class="mobile-empty-card">
        <div class="ui-empty-emoji">📧</div>
        <div>正在同步授权码状态...</div>
      </div>
      <div v-else-if="licenses.length === 0" class="mobile-empty-card">
        <div class="ui-empty-emoji">🪪</div>
        <div>当前账号还没有授权码记录。</div>
      </div>
      <div v-else class="mobile-license-list">
        <article v-for="license in licenses" :key="license.id" class="mobile-license-card">
          <div class="mobile-license-top">
            <span class="mobile-license-label">当前授权码</span>
            <AppStatusBadge :label="statusLabel(license.status)" :tone="statusTone(license.status)" />
          </div>
          <div class="mobile-license-code">{{ license.code }}</div>
          <div class="mobile-license-pills">
            <span class="mobile-license-mini" :class="license.deviceId ? 'is-success' : 'is-danger'">
              {{ mobileBindingLabel(license.deviceId) }}
            </span>
            <span class="mobile-license-mini">{{ license.deviceId || '等待绑定' }}</span>
          </div>
          <div class="mobile-license-details">
            <div class="mobile-license-detail-item">
              <span class="mobile-license-detail-label">激活时间</span>
              <span class="mobile-license-detail-value">{{ formatTime(license.activatedAt) || '尚未激活' }}</span>
            </div>
            <div class="mobile-license-detail-item">
              <span class="mobile-license-detail-label">过期时间</span>
              <span class="mobile-license-detail-value">{{ formatTime(license.expiresAt) || '长期有效' }}</span>
            </div>
          </div>
        </article>
      </div>
    </section>
  </div>

  <div v-else class="page-stack">
    <section class="page-title-block">
      <div>
        <h1 class="page-title">个人中心</h1>
        <p class="page-subtitle">更新你的联系信息，并查看当前账号下的授权码使用情况。</p>
      </div>
      <button type="button" class="ui-button secondary" @click="fetchLicenses">
        {{ loading ? '刷新中...' : '刷新授权码' }}
      </button>
    </section>

    <section class="ui-grid-2">
      <article class="ui-card ui-section-card">
        <SectionHeader title="资料编辑" />
        <form class="page-stack" @submit.prevent="saveProfile">
          <input
            ref="avatarInput"
            type="file"
            accept="image/png,image/jpeg,image/webp,image/gif"
            style="display: none"
            @change="handleAvatarSelected"
          >
          <div class="ui-field">
            <label class="ui-field-label">头像</label>
            <div style="display: flex; gap: 16px; align-items: center; flex-wrap: wrap;">
              <img
                v-if="localAvatarPreviewUrl"
                :src="localAvatarPreviewUrl"
                alt="头像预览"
                style="width: 72px; height: 72px; border-radius: 20px; object-fit: cover; border: 1px solid rgba(148, 163, 184, 0.24);"
              >
              <CachedImage
                v-else-if="editForm.avatarUrl && editForm.avatarKey"
                :src="editForm.avatarUrl"
                :cache-key="`avatar:${editForm.avatarKey}`"
                alt="头像预览"
                style="width: 72px; height: 72px; border-radius: 20px; object-fit: cover; border: 1px solid rgba(148, 163, 184, 0.24);"
              >
                <template #fallback>
                  <div
                    style="width: 72px; height: 72px; border-radius: 20px; display: flex; align-items: center; justify-content: center; background: rgba(148, 163, 184, 0.12); color: var(--text-secondary);"
                  >
                    无头像
                  </div>
                </template>
              </CachedImage>
              <div
                v-else
                style="width: 72px; height: 72px; border-radius: 20px; display: flex; align-items: center; justify-content: center; background: rgba(148, 163, 184, 0.12); color: var(--text-secondary);"
              >
                无头像
              </div>
              <div style="display: flex; gap: 12px; flex-wrap: wrap;">
                <button type="button" class="ui-button ghost" :disabled="avatarUploading" @click="triggerAvatarUpload">
                  {{ avatarUploading ? '上传中...' : '上传头像' }}
                </button>
                <button v-if="editForm.avatarUrl" type="button" class="ui-button ghost" @click="clearAvatar">清空头像</button>
              </div>
            </div>
            <div class="ui-meta">支持 JPG、PNG、WEBP、GIF，大小不超过 5MB。</div>
          </div>
          <div class="ui-field">
            <label class="ui-field-label" for="profile-username">用户名</label>
            <input id="profile-username" class="ui-input" :value="userSummary?.username || ''" disabled>
          </div>
          <div class="ui-field">
            <label class="ui-field-label" for="profile-email">邮箱</label>
            <input id="profile-email" v-model="editForm.email" class="ui-input" type="email" placeholder="请输入邮箱地址">
          </div>
          <div class="ui-field">
            <label class="ui-field-label" for="profile-phone">手机号</label>
            <input id="profile-phone" v-model="editForm.phone" class="ui-input" type="tel" placeholder="请输入手机号">
          </div>
          <div style="display: flex; gap: 12px; flex-wrap: wrap;">
            <button type="submit" class="ui-button primary" :disabled="saving">
              {{ saving ? '保存中...' : '保存资料' }}
            </button>
            <button type="button" class="ui-button ghost" @click="syncForm">重置</button>
            <button type="button" class="ui-button ghost" @click="userStore.logout()">退出登录</button>
          </div>
          <div class="ui-meta">注册时间：{{ formatTime(userSummary?.createdAt) }}</div>
        </form>
      </article>

      <article class="ui-card ui-section-card">
        <SectionHeader title="我的授权码" />
        <div v-if="loading && licenses.length === 0" class="ui-empty">
          <div class="ui-empty-emoji">📧</div>
          <div>正在同步授权码状态...</div>
        </div>
        <div v-else-if="licenses.length === 0" class="ui-empty">
          <div class="ui-empty-emoji">🪪</div>
          <div>当前账号还没有授权码记录。</div>
        </div>
        <div v-else class="table-like">
          <div v-for="license in licenses" :key="license.id" class="table-like-row">
            <div class="table-like-top">
              <strong>{{ license.code }}</strong>
              <AppStatusBadge :label="statusLabel(license.status)" :tone="statusTone(license.status)" />
            </div>
            <div class="ui-meta">绑定设备：{{ license.deviceId || '未绑定' }}</div>
            <div class="ui-meta">激活时间：{{ formatTime(license.activatedAt) }}</div>
            <div class="ui-meta">过期时间：{{ formatTime(license.expiresAt) }}</div>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>
