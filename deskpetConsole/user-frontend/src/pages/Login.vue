<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { showToast, Form as VanForm, Field as VanField, Button as VanButton, CellGroup as VanCellGroup } from 'vant'
import { useResponsive } from '@/composables/useResponsive'
import { useUserStore } from '@/stores/user'

const { isMobile } = useResponsive()
const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const form = reactive({ username: '', password: '' })
const loading = ref(false)

async function handleLogin() {
  if (!form.username || !form.password) {
    const msg = '请输入用户名和密码'
    isMobile.value ? showToast(msg) : ElMessage.warning(msg)
    return
  }
  loading.value = true
  try {
    await userStore.login({ username: form.username, password: form.password })
    const redirect = (route.query.redirect as string) || '/home'
    router.push(redirect)
  } catch {
    // 错误已在 request.ts 中处理
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <!-- 移动端 -->
  <div v-if="isMobile">
    <VanForm @submit="handleLogin">
      <VanCellGroup inset>
        <VanField
          v-model="form.username"
          label="用户名"
          placeholder="请输入用户名"
          :rules="[{ required: true, message: '请输入用户名' }]"
        />
        <VanField
          v-model="form.password"
          type="password"
          label="密码"
          placeholder="请输入密码"
          :rules="[{ required: true, message: '请输入密码' }]"
        />
      </VanCellGroup>
      <div style="margin: 24px 16px;">
        <VanButton round block type="primary" native-type="submit" :loading="loading" color="var(--primary)">
          登录
        </VanButton>
      </div>
    </VanForm>
    <div style="text-align: center; margin-top: 12px;">
      <RouterLink to="/auth/register" style="color: var(--primary); font-size: 14px;">
        没有账号？去注册
      </RouterLink>
    </div>
  </div>

  <!-- PC 端 -->
  <div v-else>
    <el-form @submit.prevent="handleLogin" label-position="top">
      <el-form-item label="用户名">
        <el-input v-model="form.username" placeholder="请输入用户名" size="large" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model="form.password" type="password" placeholder="请输入密码" size="large" show-password />
      </el-form-item>
      <el-form-item>
        <el-button
          type="primary"
          native-type="submit"
          :loading="loading"
          size="large"
          style="width: 100%; border-radius: 999px; background: var(--primary); border-color: var(--primary);"
        >
          登录
        </el-button>
      </el-form-item>
    </el-form>
    <div style="text-align: center; margin-top: 8px;">
      <RouterLink to="/auth/register" style="color: var(--primary); font-size: 14px;">
        没有账号？去注册
      </RouterLink>
    </div>
  </div>
</template>
