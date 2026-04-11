<template>
  <div class="admin-layout">
    <div v-if="!isPC" class="mobile-block">
      <el-result
        icon="warning"
        title="请使用电脑访问管理后台"
        sub-title="管理后台仅支持PC端访问，请使用电脑浏览器打开"
      />
    </div>
    <el-container v-else class="admin-container">
      <el-aside class="admin-sidebar" width="220px">
        <div class="sidebar-header">
          <span class="logo-text">管理后台</span>
        </div>
        <el-menu
          :default-active="activeMenu"
          router
          class="sidebar-menu"
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409EFF"
        >
          <el-menu-item index="/admin/dashboard">
            <el-icon><DataAnalysis /></el-icon>
            <span>仪表盘</span>
          </el-menu-item>
          <el-menu-item index="/admin/users">
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/recipe-audit">
            <el-icon><Document /></el-icon>
            <span>菜谱审核</span>
          </el-menu-item>
          <el-menu-item index="/admin/post-audit">
            <el-icon><Document /></el-icon>
            <span>帖子审核</span>
          </el-menu-item>
          <el-menu-item index="/admin/logs">
            <el-icon><DocumentCopy /></el-icon>
            <span>系统日志</span>
          </el-menu-item>
          <el-menu-item index="/admin/appeals">
            <el-icon><ChatDotRound /></el-icon>
            <span>申诉处理</span>
          </el-menu-item>
          <el-menu-item index="/admin/rules">
            <el-icon><Setting /></el-icon>
            <span>规则引擎</span>
          </el-menu-item>
          <el-menu-item index="/admin/statistics">
            <el-icon><PieChart /></el-icon>
            <span>数据统计</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-main class="admin-main">
        <div class="admin-header">
          <span class="page-title">{{ pageTitle }}</span>
          <el-button type="primary" size="small" @click="backToApp">返回应用</el-button>
        </div>
        <div class="admin-content">
          <router-view />
        </div>
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { DataAnalysis, User, Document, Setting, PieChart, ChatDotRound, DocumentCopy } from '@element-plus/icons-vue'
import { logInfo } from '../utils/logger'

const route = useRoute()
const router = useRouter()
const isPC = ref(true)

const activeMenu = computed(() => route.path)

const titleMap = {
  '/admin/dashboard': '仪表盘',
  '/admin/users': '用户管理',
  '/admin/recipe-audit': '菜谱审核',
  '/admin/post-audit': '帖子审核',
  '/admin/logs': '系统日志',
  '/admin/appeals': '申诉处理',
  '/admin/rules': '规则引擎',
  '/admin/statistics': '数据统计'
}

const pageTitle = computed(() => {
  if (route.path.startsWith('/admin/appeals/')) return '申诉详情'
  return titleMap[route.path] || '管理后台'
})

function backToApp() {
  logInfo('ADMIN', 'back_to_app')
  router.push('/plan')
}

function checkIsPC() {
  isPC.value = window.innerWidth >= 768
}

onMounted(() => {
  checkIsPC()
  window.addEventListener('resize', checkIsPC)
})
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
  background: #f0f2f5;
}

.mobile-block {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  padding: 20px;
}

.admin-container {
  min-height: 100vh;
}

.admin-sidebar {
  background: #304156;
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
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
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

.admin-main {
  margin-left: 220px;
  padding: 20px;
  min-height: 100vh;
}

.admin-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: #303133;
}

.admin-content {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}
</style>
