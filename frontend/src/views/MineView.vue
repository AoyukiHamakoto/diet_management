<template>
  <div class="page-placeholder mine-page">
    <div class="user-card">
      <el-avatar :size="64" class="avatar" :src="userStore.avatar || ''">{{ userStore.nickname?.charAt(0) || '我' }}</el-avatar>
      <div class="user-info">
        <div class="nickname">{{ userStore.nickname || '用户' }}</div>
        <div class="phone">个人中心</div>
      </div>
    </div>
    <div class="menu-list">
      <div class="menu-item" @click="$router.push('/settings')">
        <span>设置</span>
        <el-icon><ArrowRight /></el-icon>
      </div>
      <div class="menu-item" @click="$router.push('/profile')">
        <span>健康档案</span>
        <el-icon><ArrowRight /></el-icon>
      </div>
      <div class="menu-item" @click="$router.push('/community')">
        <span>社区</span>
        <el-icon><ArrowRight /></el-icon>
      </div>
      <div class="menu-item" @click="$router.push('/preferences')">
        <span>偏好设置</span>
        <el-icon><ArrowRight /></el-icon>
      </div>
      <div class="menu-item" @click="$router.push('/feedback-history')">
        <span>我的评价</span>
        <el-icon><ArrowRight /></el-icon>
      </div>
      <div class="menu-item" @click="$router.push('/my-recipes')">
        <span>我的菜谱</span>
        <el-icon><ArrowRight /></el-icon>
      </div>
      <div class="menu-item" @click="$router.push('/my-appeals')">
        <span>我的申诉</span>
        <el-icon><ArrowRight /></el-icon>
      </div>
    </div>
    <el-button type="danger" plain style="margin-top: 24px" @click="handleLogout">退出登录</el-button>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ArrowRight } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'
const router = useRouter()
const userStore = useUserStore()

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.mine-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 24px;
}

.user-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.avatar {
  background: #1a5f4a;
}

.user-info .nickname {
  font-size: 1.125rem;
  font-weight: 500;
}

.user-info .phone {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

.menu-list {
  width: 100%;
  max-width: 400px;
  margin-top: 20px;
}

.menu-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  margin-bottom: 12px;
}

.menu-item:hover {
  background: #f5f7fa;
}
</style>
