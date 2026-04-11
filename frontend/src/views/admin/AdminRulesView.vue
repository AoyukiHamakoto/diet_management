<template>
  <div class="admin-rules">
    <el-alert class="intro-alert" type="info" show-icon :closable="false">
      <template #title>规则引擎（Drools）</template>
      <p class="intro-text">
        规则决定「给用户打什么饮食标签」「计划热量如何建议」等逻辑。下方以卡片说明各文件职责；编辑时可用
        <strong>可视化概览</strong>快速读懂条件与动作，必要时再改源码。
      </p>
    </el-alert>

    <el-row :gutter="20" v-loading="loading" class="card-row">
      <el-col v-for="row in enrichedList" :key="row.name" :xs="24" :md="12">
        <el-card class="rule-card" shadow="hover" :style="{ '--accent': row.meta.color }">
          <div class="card-head">
            <span class="accent-bar" />
            <div class="card-titles">
              <h3>{{ row.meta.title }}</h3>
              <p class="sub">{{ row.meta.subtitle }}</p>
            </div>
            <el-tag size="small" effect="plain" class="file-tag">{{ row.meta.tag }}</el-tag>
          </div>
          <p class="desc">{{ row.meta.description }}</p>
          <div class="file-line">
            <el-icon><Document /></el-icon>
            <code>{{ row.name }}</code>
          </div>
          <div class="path-line">
            <span class="label">路径</span>
            <span class="path">{{ row.path }}</span>
          </div>
          <el-button type="primary" class="edit-btn" @click="goEdit(row.name)">打开编辑器</el-button>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-if="!loading && enrichedList.length === 0" description="暂无规则文件" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Document } from '@element-plus/icons-vue'
import { getRulesList } from '../../api/admin'
import { getRuleMeta } from '../../data/ruleFileMeta'

const router = useRouter()
const list = ref([])
const loading = ref(false)

const enrichedList = computed(() =>
  list.value.map((row) => ({
    ...row,
    meta: getRuleMeta(row.name)
  }))
)

async function load() {
  loading.value = true
  try {
    const res = await getRulesList()
    list.value = Array.isArray(res.data) ? res.data : []
  } finally {
    loading.value = false
  }
}

function goEdit(name) {
  router.push({ name: 'AdminRuleEdit', query: { name } })
}

onMounted(() => load())
</script>

<style scoped>
.admin-rules {
  padding: 0;
}
.intro-alert {
  margin-bottom: 20px;
}
.intro-text {
  margin: 8px 0 0;
  font-size: 14px;
  line-height: 1.6;
  color: var(--el-text-color-regular);
}
.card-row {
  margin-top: 4px;
}
.rule-card {
  margin-bottom: 20px;
  border-radius: 12px;
  border: 1px solid var(--el-border-color-lighter);
  overflow: hidden;
}
.card-head {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 12px;
}
.accent-bar {
  width: 4px;
  min-height: 48px;
  border-radius: 2px;
  background: var(--accent, #1a5f4a);
  flex-shrink: 0;
}
.card-titles {
  flex: 1;
  min-width: 0;
}
.card-titles h3 {
  margin: 0 0 4px;
  font-size: 18px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}
.sub {
  margin: 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.file-tag {
  flex-shrink: 0;
}
.desc {
  margin: 0 0 16px;
  font-size: 14px;
  line-height: 1.65;
  color: var(--el-text-color-regular);
}
.file-line {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-bottom: 8px;
}
.file-line code {
  font-size: 12px;
  background: var(--el-fill-color-light);
  padding: 2px 8px;
  border-radius: 4px;
}
.path-line {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  margin-bottom: 16px;
  word-break: break-all;
}
.path-line .label {
  margin-right: 8px;
  color: var(--el-text-color-secondary);
}
.edit-btn {
  width: 100%;
}
</style>
