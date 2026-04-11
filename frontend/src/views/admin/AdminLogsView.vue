<template>
  <div class="logs-page">
    <div class="top">
      <el-input v-model="keyword" placeholder="搜索关键词（如 登录 / 生成计划 / 审核 ）" clearable style="max-width: 420px" />
      <el-button type="primary" @click="load">刷新日志</el-button>
      <el-button type="danger" plain @click="clear">清空日志</el-button>
    </div>

    <el-table v-if="filtered.length" :data="filtered" border style="width: 100%">
      <el-table-column prop="time" label="时间" width="220" />
      <el-table-column prop="level" label="级别" width="90" />
      <el-table-column prop="module" label="模块" width="140" />
      <el-table-column prop="message" label="内容" />
    </el-table>

    <el-empty v-else description="系统日志（原型扩展功能，占位）" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { clearAppLogs, getAppLogs, logInfo } from '../../utils/logger'

const keyword = ref('')
const rows = ref([])

const filtered = computed(() => {
  const k = keyword.value?.trim()
  if (!k) return rows.value
  return rows.value.filter(r =>
    String(r.time).includes(k) ||
    String(r.level).includes(k) ||
    String(r.module).includes(k) ||
    String(r.message).includes(k)
  )
})

function load() {
  rows.value = getAppLogs().slice().reverse()
  logInfo('ADMIN_LOGS', 'load_logs', { count: rows.value.length })
}

function clear() {
  clearAppLogs()
  load()
}

onMounted(() => {
  load()
})
</script>

<style scoped>
.top {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
</style>

