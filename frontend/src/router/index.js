import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '../layouts/MainLayout.vue'
import AdminLayout from '../layouts/AdminLayout.vue'
import { useUserStore } from '../stores/user'
import { logInfo, logWarn } from '../utils/logger'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/LoginView.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    component: MainLayout,
    redirect: '/plan',
    children: [
      { path: 'plan', name: 'Plan', component: () => import('../views/PlanView.vue') },
      { path: 'profile', name: 'Profile', component: () => import('../views/ProfileView.vue') },
      { path: 'profile/setup', name: 'ProfileSetup', component: () => import('../views/ProfileSetupView.vue') },
      { path: 'recipes', name: 'Recipes', component: () => import('../views/RecipesView.vue') },
      { path: 'community', name: 'Community', component: () => import('../views/CommunityView.vue') },
      { path: 'community/:id', name: 'PostDetail', component: () => import('../views/PostDetailView.vue') },
      { path: 'my-recipes', name: 'MyRecipes', component: () => import('../views/MyRecipesView.vue') },
      { path: 'recipe/create', name: 'RecipeCreate', component: () => import('../views/RecipeCreateView.vue') },
      { path: 'recipe/edit/:id', name: 'RecipeEdit', component: () => import('../views/RecipeEditView.vue') },
      { path: 'recipe/:id', name: 'RecipeDetail', component: () => import('../views/RecipeDetailView.vue') },
      { path: 'record', redirect: '/profile' },
      { path: 'mine', name: 'Mine', component: () => import('../views/MineView.vue') },
      { path: 'settings', name: 'Settings', component: () => import('../views/SettingsView.vue') },
      { path: 'settings/profile', name: 'ProfileSettings', component: () => import('../views/ProfileSettingsView.vue') },
      { path: 'settings/password', name: 'PasswordSettings', component: () => import('../views/PasswordSettingsView.vue') },
      { path: 'preferences', name: 'Preferences', component: () => import('../views/PreferencesView.vue') },
      { path: 'feedback-history', name: 'FeedbackHistory', component: () => import('../views/FeedbackHistoryView.vue') },
      { path: 'my-appeals', name: 'MyAppeals', component: () => import('../views/MyAppealsView.vue') },
      { path: 'appeal/submit', name: 'AppealSubmit', component: () => import('../views/AppealSubmitView.vue') }
    ]
  },
  {
    path: '/admin',
    component: AdminLayout,
    redirect: '/admin/dashboard',
    meta: { admin: true },
    children: [
      { path: 'dashboard', name: 'AdminDashboard', component: () => import('../views/admin/AdminDashboardView.vue') },
      { path: 'users', name: 'AdminUsers', component: () => import('../views/admin/AdminUsersView.vue') },
      { path: 'recipe-audit', name: 'AdminRecipeAudit', component: () => import('../views/admin/AdminRecipeAuditView.vue') },
      { path: 'post-audit', name: 'AdminPostAudit', component: () => import('../views/admin/AdminPostAuditView.vue') },
      { path: 'plans', name: 'AdminPlans', component: () => import('../views/admin/AdminPlansView.vue') },
      { path: 'plans/:id', name: 'AdminPlanDetail', component: () => import('../views/admin/AdminPlanDetailView.vue') },
      { path: 'orders', name: 'AdminOrders', component: () => import('../views/admin/AdminOrdersView.vue') },
      { path: 'logs', name: 'AdminLogs', component: () => import('../views/admin/AdminLogsView.vue') },
      { path: 'rules', name: 'AdminRules', component: () => import('../views/admin/AdminRulesView.vue') },
      { path: 'rules/edit', name: 'AdminRuleEdit', component: () => import('../views/admin/AdminRuleEditView.vue') },
      { path: 'statistics', name: 'AdminStatistics', component: () => import('../views/admin/AdminStatisticsView.vue') },
      { path: 'appeals', name: 'AdminAppeals', component: () => import('../views/admin/AdminAppealsView.vue') },
      { path: 'appeals/:id', name: 'AdminAppealDetail', component: () => import('../views/admin/AdminAppealDetailView.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  logInfo('ROUTER', 'route_enter', { from: from.fullPath, to: to.fullPath })
  if (!to.meta.public && !userStore.accessToken) {
    logWarn('ROUTER', 'no_token_redirect_login', { to: to.fullPath })
    next({ name: 'Login', query: { redirect: to.fullPath } })
  } else if (to.meta.admin && userStore.role !== 'ADMIN') {
    logWarn('ROUTER', 'non_admin_blocked', { role: userStore.role, to: to.fullPath })
    next({ name: 'Plan' })
  } else {
    next()
  }
})

export default router
