import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import UserManagePage from '../views/admin/UserManagePage.vue'
import UserAccountSetupPage from '../views/user/UserAccountSetupPage.vue'
import AppGeneratePage from '../views/AppGeneratePage.vue'
import AppDetailPage from '../views/AppDetailPage.vue'
import AppEditPage from '../views/AppEditPage.vue'
import AppManagePage from '../views/admin/AppManagePage.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/app/generate/:id',
      name: 'appGenerate',
      component: AppGeneratePage,
    },
    {
      path: '/app/detail/:id',
      name: 'appDetail',
      component: AppDetailPage,
    },
    {
      path: '/app/edit/:id',
      name: 'appEdit',
      component: AppEditPage,
    },
    {
      path: '/user/account-setup',
      name: 'userAccountSetup',
      component: UserAccountSetupPage,
    },
    {
      path: '/admin/user-manage',
      name: 'userManage',
      component: UserManagePage,
    },
    {
      path: '/admin/app-manage',
      name: 'appManage',
      component: AppManagePage,
    },
    {
      path: '/admin/app',
      name: 'admin-app',
      component: () => import('@/views/admin/AppManagePage.vue'),
      meta: {
        access: 'admin',
      },
    },
    {
      path: '/admin/chat',
      name: 'admin-chat',
      component: () => import('@/views/admin/ChatManagePage.vue'),
      meta: {
        access: 'admin',
      },
    },
    {
      path: '/admin/user',
      name: 'admin-user',
      component: () => import('@/views/admin/UserManagePage.vue'),
      meta: {
        access: 'admin',
      },
    },
    // 重定向 /app/ 到首页
    {
      path: '/app/',
      redirect: '/',
    },
    // 捕获所有未匹配的路由
    {
      path: '/:pathMatch(.*)*',
      name: 'notFound',
      redirect: '/',
    },
  ],
})

export default router
