import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', name: 'login', component: () => import('../pages/Login.vue'), meta: { guest: true } },
  { path: '/', name: 'dashboard', component: () => import('../pages/Dashboard.vue') },
  { path: '/licenses', name: 'licenses', component: () => import('../pages/Licenses.vue') },
  { path: '/products', name: 'products', component: () => import('../pages/Products.vue') },
  { path: '/products/:productKey', name: 'thing-model', component: () => import('../pages/ThingModel.vue') },
  { path: '/devices', name: 'devices', component: () => import('../pages/Devices.vue') },
  { path: '/devices/:id', name: 'device-detail', component: () => import('../pages/DeviceDetail.vue') },
  { path: '/commands', name: 'commands', component: () => import('../pages/Commands.vue') },
  { path: '/telemetry', name: 'telemetry', component: () => import('../pages/Telemetry.vue') },
  { path: '/logs', name: 'logs', component: () => import('../pages/Logs.vue') },
  { path: '/settings', name: 'settings', component: () => import('../pages/Settings.vue') },
  { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('../pages/NotFound.vue') }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')

  if (to.meta.guest) {
    // 已登录用户访问登录页，跳转首页
    token ? next({ name: 'dashboard' }) : next()
  } else {
    // 未登录用户访问受保护页面，跳转登录页
    token ? next() : next({ name: 'login', query: { redirect: to.fullPath } })
  }
})

export default router
