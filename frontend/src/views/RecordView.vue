<template>
  <div class="page-placeholder">
    <PageBackBar fallback="/plan" />
    <el-empty description="打卡记录" />
  </div>
</template>

<script setup>
import PageBackBar from '../components/PageBackBar.vue'
</script>

<style scoped>
.page-placeholder {
  min-height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border-radius: 12px;
  padding: 24px;
}
</style>
