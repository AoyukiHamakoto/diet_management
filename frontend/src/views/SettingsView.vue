<template>
  <div class="settings-page">
    <PageBackBar fallback="/mine" />
    <div class="menu-list">
      <div class="menu-item" @click="$router.push('/settings/profile')">
        <span>个人资料</span>
        <el-icon><ArrowRight /></el-icon>
      </div>
      <div class="menu-item" @click="$router.push('/settings/password')">
        <span>修改密码</span>
        <el-icon><ArrowRight /></el-icon>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ArrowRight } from '@element-plus/icons-vue'
import PageBackBar from '../components/PageBackBar.vue'
</script>

<style scoped>
.settings-page {
  padding: 16px;
}

.menu-list {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.menu-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
}

.menu-item:last-child {
  border-bottom: none;
}
</style>
