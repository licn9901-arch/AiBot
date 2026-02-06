<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { showToast, Form as VanForm, Field as VanField, Button as VanButton, CellGroup as VanCellGroup } from 'vant'
import { useResponsive } from '@/composables/useResponsive'
import { register } from '@/api/auth'

const { isMobile } = useResponsive()
const router = useRouter()

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  phone: ''
})
const loading = ref(false)

async function handleRegister() {
  if (!form.username || !form.password) {
    const msg = '请输入用户名和密码'
    isMobile.value ? showToast(msg) : ElMessage.warning(msg)
    return
  }
  if (form.password !== form.confirmPassword) {
    const msg = '两次输入的密码不一致'
    isMobile.value ? showToast(msg) : ElMessage.warning(msg)
    return
  }
  loading.value = true
  try {
    await register({
      username: form.username,
      password: form.password,
      email: form.email || undefined,
      phone: form.phone || undefined
    })
    const msg = '注册成功，请登录'
    isMobile.value ? showToast(msg) : ElMessage.success(msg)
    router.push('/auth/login')
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
    <VanForm @submit="handleRegister">
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
        <VanField
          v-model="form.confirmPassword"
          type="password"
          label="确认密码"
          placeholder="请再次输入密码"
          :rules="[{ required: true, message: '请确认密码' }]"
        />
        <VanField v-model="form.email" label="邮箱" placeholder="选填" />
        <VanField v-model="form.phone" label="手机" placeholder="选填" />
      </VanCellGroup>
      <div style="margin: 24px 16px;">
        <VanButton round block type="primary" native-type="submit" :loading="loading" color="var(--primary)">
          注册
        </VanButton>
      </div>
    </VanForm>
    <div style="text-align: center; margin-top: 12px;">
      <RouterLink to="/auth/login" style="color: var(--primary); font-size: 14px;">
        已有账号？去登录
      </RouterLink>
    </div>
  </div>

  <!-- PC 端 -->
  <div v-else>
    <el-form @submit.prevent="handleRegister" label-position="top">
      <el-form-item label="用户名">
        <el-input v-model="form.username" placeholder="请输入用户名" size="large" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model="form.password" type="password" placeholder="请输入密码" size="large" show-password />
      </el-form-item>
      <el-form-item label="确认密码">
        <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" size="large" show-password />
      </el-form-item>
      <el-form-item label="邮箱（选填）">
        <el-input v-model="form.email" placeholder="请输入邮箱" size="large" />
      </el-form-item>
      <el-form-item label="手机（选填）">
        <el-input v-model="form.phone" placeholder="请输入手机号" size="large" />
      </el-form-item>
      <el-form-item>
        <el-button
          type="primary"
          native-type="submit"
          :loading="loading"
          size="large"
          style="width: 100%; border-radius: 999px; background: var(--primary); border-color: var(--primary);"
        >
          注册
        </el-button>
      </el-form-item>
    </el-form>
    <div style="text-align: center; margin-top: 8px;">
      <RouterLink to="/auth/login" style="color: var(--primary); font-size: 14px;">
        已有账号？去登录
      </RouterLink>
    </div>
  </div>
</template>
