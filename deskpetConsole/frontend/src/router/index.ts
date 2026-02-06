import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', name: 'dashboard', component: () => import('../pages/Dashboard.vue') },
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

export default router
