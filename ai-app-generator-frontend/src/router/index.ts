import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import UserManagePage from '../views/admin/UserManagePage.vue'
import UserAccountSetupPage from '../views/user/UserAccountSetupPage.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
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
  ],
})

export default router
