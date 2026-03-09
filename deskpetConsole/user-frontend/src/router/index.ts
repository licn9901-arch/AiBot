import { createRouter, createWebHistory } from 'vue-router'

const authRedirectWhenLoggedIn = new Set(['login', 'register'])

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/auth',
      component: () => import('@/layouts/AuthLayout.vue'),
      meta: { guest: true },
      children: [
        { path: 'login', name: 'login', component: () => import('@/pages/Login.vue') },
        { path: 'register', name: 'register', component: () => import('@/pages/Register.vue') },
        { path: 'activate', name: 'account-activate', component: () => import('@/pages/ActivateAccount.vue') },
        { path: 'forgot-password', name: 'forgot-password', component: () => import('@/pages/ForgotPassword.vue') },
        { path: 'reset-password', name: 'reset-password', component: () => import('@/pages/ResetPassword.vue') },
        { path: 'check-email', name: 'check-email', component: () => import('@/pages/CheckEmail.vue') },
      ],
    },
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        { path: '', redirect: '/home' },
        { path: 'home', name: 'home', component: () => import('@/pages/Home.vue') },
        { path: 'devices', name: 'devices', component: () => import('@/pages/MyDevices.vue') },
        { path: 'devices/:deviceId', name: 'device-control', component: () => import('@/pages/DeviceControl.vue') },
        { path: 'activate', name: 'activate', component: () => import('@/pages/ActivateLicense.vue') },
        { path: 'profile', name: 'profile', component: () => import('@/pages/Profile.vue') },
      ],
    },
    { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('@/pages/NotFound.vue') },
  ],
  scrollBehavior() {
    return { top: 0 }
  },
})

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')

  if (to.matched.some((record) => record.meta.requiresAuth) && !token) {
    next({ name: 'login', query: { redirect: to.fullPath } })
    return
  }

  if (token && typeof to.name === 'string' && authRedirectWhenLoggedIn.has(to.name)) {
    next({ name: 'home' })
    return
  }

  next()
})

export default router
