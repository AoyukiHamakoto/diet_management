<template>
  <div class="page-back-bar">
    <el-button text type="primary" class="back-btn" @click="goBack">
      <el-icon><ArrowLeft /></el-icon>
      <span>返回</span>
    </el-button>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'

const props = defineProps({
  /** 无历史记录时的回退路径，默认 /mine */
  fallback: { type: String, default: '/mine' }
})

const router = useRouter()

function goBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push(props.fallback)
  }
}
</script>

<style scoped>
.page-back-bar {
  margin-bottom: 12px;
}
.back-btn {
  padding-left: 0;
  font-size: 15px;
}
</style>
