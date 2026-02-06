import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/auth',
      component: () => import('@/layouts/AuthLayout.vue'),
      meta: { guest: true },
      children: [
        { path: 'login', name: 'login', component: () => import('@/pages/Login.vue') },
        { path: 'register', name: 'register', component: () => import('@/pages/Register.vue') }
      ]
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
        { path: 'profile', name: 'profile', component: () => import('@/pages/Profile.vue') }
      ]
    },
    { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('@/pages/NotFound.vue') }
  ],
  scrollBehavior() {
    return { top: 0 }
  }
})

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')

  if (to.matched.some(r => r.meta.requiresAuth) && !token) {
    next({ name: 'login', query: { redirect: to.fullPath } })
  } else if (to.matched.some(r => r.meta.guest) && token) {
    next({ name: 'home' })
  } else {
    next()
  }
})

export default router
