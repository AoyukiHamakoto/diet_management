<template>
  <el-container class="main-layout">
    <!-- PC端侧边栏 -->
    <el-aside v-show="isPC" class="pc-sidebar" width="220px">
      <div class="sidebar-header">
        <span class="logo-text">健身饮食规划</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        class="sidebar-menu"
        background-color="#1a5f4a"
        text-color="#e8f5e9"
        active-text-color="#fff"
      >
        <el-menu-item index="/plan">
          <el-icon><Calendar /></el-icon>
          <span>饮食计划</span>
        </el-menu-item>
        <el-menu-item index="/profile">
          <el-icon><Document /></el-icon>
          <span>健康档案</span>
        </el-menu-item>
        <el-menu-item index="/recipes">
          <el-icon><Grid /></el-icon>
          <span>菜谱库</span>
        </el-menu-item>
        <el-menu-item index="/community">
          <el-icon><ChatDotRound /></el-icon>
          <span>社区</span>
        </el-menu-item>
        <el-menu-item index="/mine">
          <el-icon><User /></el-icon>
          <span>个人中心</span>
        </el-menu-item>
        <el-menu-item v-if="userStore.role === 'ADMIN'" index="/admin/dashboard">
          <el-icon><Setting /></el-icon>
          <span>管理后台</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container class="main-container" :class="{ 'has-sidebar': isPC }">
      <el-header class="pc-header" :class="{ 'mobile-header': !isPC }">
        <span class="header-title">{{ currentTitle }}</span>
        <div v-if="isPC" class="header-right">
          <span class="nickname">{{ userStore.nickname || '用户' }}</span>
          <el-button type="danger" size="small" @click="handleLogout">退出</el-button>
        </div>
      </el-header>

      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <!-- 帖子详情不参与缓存：避免 keep-alive 下路由/请求竞态导致进出页报错 -->
          <keep-alive exclude="PostDetail">
            <component :is="Component" />
          </keep-alive>
        </router-view>
      </el-main>

      <!-- 移动端底部导航 -->
      <div v-show="!isPC" class="mobile-tabbar">
        <div
          v-for="tab in mobileTabs"
          :key="tab.path"
          class="tab-item"
          :class="{ active: activeMenu === tab.path }"
          @click="$router.push(tab.path)"
        >
          <el-icon :size="22"><component :is="tab.icon" /></el-icon>
          <span>{{ tab.label }}</span>
        </div>
      </div>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Calendar, Document, Grid, User, Setting, ChatDotRound } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isPC = ref(true)

const mobileTabs = [
  { path: '/plan', label: '首页', icon: Calendar },
  { path: '/profile', label: '档案', icon: Document },
  { path: '/community', label: '社区', icon: ChatDotRound },
  { path: '/recipes', label: '菜谱', icon: Grid },
  { path: '/mine', label: '我的', icon: User }
]

const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/plan')) return '/plan'
  if (path.startsWith('/profile')) return '/profile'
  if (path.startsWith('/community')) return '/community'
  if (path.startsWith('/recipes') || path.startsWith('/recipe/')) return '/recipes'
  if (path.startsWith('/mine') || path.startsWith('/settings') || path.startsWith('/preferences') || path.startsWith('/feedback-history') || path.startsWith('/my-recipes') || path.startsWith('/my-appeals') || path.startsWith('/appeal/') || path.startsWith('/recipe/edit/')) return '/mine'
  if (path.startsWith('/admin')) return '/admin/dashboard'
  return '/plan'
})

const titleMap = {
  '/plan': '饮食计划',
  '/profile': '健康档案',
  '/community': '社区',
  '/recipes': '菜谱库',
  '/mine': '个人中心',
  '/admin/dashboard': '管理后台'
}

const currentTitle = computed(() => {
  const path = route.path
  if (path === '/settings') return '设置'
  if (path === '/settings/profile') return '个人资料'
  if (path === '/settings/password') return '修改密码'
  if (path === '/preferences') return '偏好设置'
  if (path === '/feedback-history') return '我的评价'
  if (path === '/my-recipes') return '我的菜谱'
  if (path === '/my-appeals') return '我的申诉'
  if (path === '/appeal/submit') return '提交申诉'
  if (path.startsWith('/recipe/edit/')) return '编辑菜谱'
  if (path.startsWith('/community/') && path !== '/community') return '帖子详情'
  return titleMap[activeMenu.value] || '健身饮食规划'
})

function handleLogout() {
  userStore.logout()
  router.push('/login')
}

function checkIsPC() {
  isPC.value = window.innerWidth >= 768
}

onMounted(() => {
  checkIsPC()
  window.addEventListener('resize', checkIsPC)
})

defineExpose({ checkIsPC })
</script>

<style scoped>
.main-layout {
  min-height: 100vh;
  background: #f5f7fa;
}

.pc-sidebar {
  background: #1a5f4a;
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  z-index: 100;
}

.sidebar-header {
  height: 60px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo-text {
  color: #fff;
  font-size: 1rem;
  font-weight: 600;
}

.sidebar-menu {
  border-right: none;
}

.sidebar-menu .el-menu-item {
  height: 48px;
  line-height: 48px;
}

.main-container {
  min-height: 100vh;
  flex-direction: column;
}

.main-container.has-sidebar {
  margin-left: 220px;
}

.pc-header {
  height: 60px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  position: sticky;
  top: 0;
  z-index: 90;
}

.mobile-header {
  height: 48px;
  padding: 0 16px;
  justify-content: center;
}

.header-title {
  font-size: 1.125rem;
  font-weight: 500;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.nickname {
  font-size: 14px;
  color: #606266;
}

.main-content {
  flex: 1;
  padding: 24px;
  padding-bottom: calc(80px + env(safe-area-inset-bottom, 0));
}

@media (min-width: 768px) {
  .main-content {
    padding-bottom: 24px;
  }
}

/* 移动端底部导航 + 安全区 */
.mobile-tabbar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 56px;
  padding-bottom: env(safe-area-inset-bottom, 0);
  background: #fff;
  display: flex;
  justify-content: space-around;
  align-items: center;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.06);
  z-index: 100;
}

.tab-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  font-size: 11px;
  color: #909399;
  padding: 8px 0;
  cursor: pointer;
  transition: color 0.2s;
}

.tab-item.active {
  color: #1a5f4a;
  font-weight: 500;
}

.tab-item .el-icon {
  font-size: 22px;
}
</style>
