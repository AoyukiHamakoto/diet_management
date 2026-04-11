<template>
  <div class="admin-rule-edit">
    <el-page-header @back="goBack" title="返回" :content="pageTitle" />

    <el-alert v-if="!ruleName" type="warning" title="请从规则列表选择要编辑的文件" show-icon class="top-alert" />

    <template v-else>
      <div class="file-banner">
        <div class="banner-icon" :style="{ background: meta.color }">
          <el-icon :size="22"><SetUp /></el-icon>
        </div>
        <div class="banner-text">
          <h2>{{ meta.title }}</h2>
          <p>{{ meta.description }}</p>
        </div>
        <div class="banner-actions">
          <el-button type="primary" :loading="saving" @click="save">保存并发布</el-button>
          <el-button :loading="validating" @click="validate">校验</el-button>
          <el-button @click="goBack">返回列表</el-button>
        </div>
      </div>

      <el-tabs v-model="activeTab" class="main-tabs" type="border-card">
        <el-tab-pane label="可视化概览" name="visual">
          <el-alert type="success" :closable="false" show-icon class="visual-tip">
            <template #title>从当前源码自动解析</template>
            展示每条规则的「条件（when）」与「执行（then）」摘要。保存仍以「规则源码」为准。
          </el-alert>

          <div v-if="parsed.rules.length" class="rule-count">
            共解析到 <strong>{{ parsed.rules.length }}</strong> 条规则
          </div>

          <el-collapse v-if="parsed.rules.length" v-model="openNames" class="rule-collapse">
            <el-collapse-item v-for="(r, idx) in parsed.rules" :key="idx" :name="String(idx)">
              <template #title>
                <span class="collapse-title">
                  <el-tag size="small" type="info">#{{ idx + 1 }}</el-tag>
                  <span class="rule-name">{{ r.name }}</span>
                  <el-tag v-if="r.parseError" size="small" type="danger">解析不完整</el-tag>
                </span>
              </template>

              <div class="rule-flow">
                <div class="flow-block when-block">
                  <div class="flow-label">
                    <el-icon><CircleCheck /></el-icon>
                    条件（When）
                  </div>
                  <pre class="flow-pre">{{ r.whenText || '（无）' }}</pre>
                </div>
                <div class="flow-arrow" aria-hidden="true">
                  <el-icon><Bottom /></el-icon>
                  <span>则</span>
                </div>
                <div class="flow-block then-block">
                  <div class="flow-label">
                    <el-icon><Lightning /></el-icon>
                    执行（Then）
                  </div>
                  <pre class="flow-pre">{{ r.thenText || '（无）' }}</pre>
                </div>
              </div>
            </el-collapse-item>
          </el-collapse>

          <el-empty v-else description="未解析到 rule 块，请检查源码格式" />

          <el-divider content-position="left">文件头（package / import）</el-divider>
          <pre v-if="parsed.header" class="header-snippet">{{ parsed.header }}</pre>
          <p v-else class="muted">无独立文件头或已全部为规则体。</p>
        </el-tab-pane>

        <el-tab-pane label="规则源码（DRL）" name="source">
          <p class="source-hint">以下为 Drools 语法；修改后点击上方「保存并发布」。</p>
          <el-input
            v-model="content"
            type="textarea"
            :rows="22"
            placeholder="Drools 规则（.drl）"
            class="rule-textarea"
            spellcheck="false"
          />
        </el-tab-pane>
      </el-tabs>

      <div v-if="validateResult !== null" class="validate-result">
        <el-alert
          :title="validateResult.valid ? '校验通过' : '校验失败'"
          :type="validateResult.valid ? 'success' : 'error'"
          :description="
            validateResult.valid
              ? ''
              : validateResult.errors && validateResult.errors.length
                ? validateResult.errors.join('\n')
                : '编译错误'
          "
          show-icon
        />
      </div>

      <el-divider>备份与恢复</el-divider>
      <div class="backup-section">
        <el-button size="small" :loading="loadingBackups" @click="loadBackups">刷新备份列表</el-button>
        <ul v-if="backups.length" class="backup-list">
          <li v-for="b in backups" :key="b.filename">
            <span>{{ b.filename }}</span>
            <el-button type="primary" link size="small" @click="restore(b.filename)">恢复</el-button>
          </li>
        </ul>
        <p v-else-if="!loadingBackups" class="tip">暂无备份，保存时会自动生成备份。</p>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { SetUp, Bottom, CircleCheck, Lightning } from '@element-plus/icons-vue'
import {
  getRuleContent,
  saveRule,
  validateRule,
  listRuleBackups,
  restoreRule
} from '../../api/admin'
import { parseDrlRules } from '../../utils/drlVisual'
import { getRuleMeta } from '../../data/ruleFileMeta'

const route = useRoute()
const router = useRouter()

const ruleName = computed(() => route.query.name || '')
const meta = computed(() => getRuleMeta(ruleName.value))
const pageTitle = computed(() => (ruleName.value ? `${meta.value.title} · 编辑` : '规则编辑'))

const content = ref('')
const activeTab = ref('visual')
const openNames = ref(['0'])
const saving = ref(false)
const validating = ref(false)
const validateResult = ref(null)
const backups = ref([])
const loadingBackups = ref(false)

const parsed = computed(() => parseDrlRules(content.value))

async function loadContent() {
  if (!ruleName.value) return
  try {
    const res = await getRuleContent(ruleName.value)
    const data = res.data
    content.value = (data && data.content) != null ? data.content : ''
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '加载规则内容失败')
  }
}

async function save() {
  if (!ruleName.value) return
  saving.value = true
  validateResult.value = null
  try {
    await saveRule({ name: ruleName.value, content: content.value })
    ElMessage.success('保存并发布成功')
    loadBackups()
  } catch (e) {
    const msg = e.response?.data?.message || e.message || '保存失败'
    ElMessage.error(msg)
  } finally {
    saving.value = false
  }
}

async function validate() {
  if (!ruleName.value) return
  validating.value = true
  validateResult.value = null
  try {
    const res = await validateRule({ name: ruleName.value, content: content.value })
    const data = res.data
    validateResult.value = data || { valid: false, errors: [] }
  } catch (e) {
    validateResult.value = { valid: false, errors: [e.response?.data?.message || e.message || '请求失败'] }
  } finally {
    validating.value = false
  }
}

async function loadBackups() {
  if (!ruleName.value) return
  loadingBackups.value = true
  try {
    const res = await listRuleBackups(ruleName.value)
    backups.value = Array.isArray(res.data) ? res.data : []
  } catch {
    backups.value = []
  } finally {
    loadingBackups.value = false
  }
}

async function restore(backupFilename) {
  if (!ruleName.value) return
  try {
    await restoreRule({ name: ruleName.value, backupFilename })
    ElMessage.success('已从备份恢复')
    await loadContent()
    loadBackups()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '恢复失败')
  }
}

function goBack() {
  router.push({ name: 'AdminRules' })
}

watch(ruleName, (name) => {
  if (name) {
    loadContent()
    loadBackups()
    openNames.value = ['0']
  } else {
    content.value = ''
    backups.value = []
  }
}, { immediate: true })

watch(
  () => parsed.value.rules.length,
  (n) => {
    if (n > 0 && openNames.value.length === 0) openNames.value = ['0']
  }
)

onMounted(() => {
  if (ruleName.value) {
    loadContent()
    loadBackups()
  }
})
</script>

<style scoped>
.admin-rule-edit {
  padding: 0;
}
.top-alert {
  margin-top: 16px;
}
.file-banner {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  margin: 20px 0 16px;
  padding: 16px 18px;
  background: linear-gradient(135deg, var(--el-fill-color-light) 0%, var(--el-bg-color) 100%);
  border-radius: 12px;
  border: 1px solid var(--el-border-color-lighter);
  flex-wrap: wrap;
}
.banner-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}
.banner-text {
  flex: 1;
  min-width: 200px;
}
.banner-text h2 {
  margin: 0 0 6px;
  font-size: 18px;
  font-weight: 600;
}
.banner-text p {
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
  color: var(--el-text-color-secondary);
}
.banner-actions {
  margin-left: auto;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}
@media (max-width: 768px) {
  .banner-actions {
    width: 100%;
    margin-left: 0;
  }
}
.main-tabs {
  margin-top: 8px;
}
.main-tabs :deep(.el-tabs__content) {
  padding: 16px;
}
.visual-tip {
  margin-bottom: 16px;
}
.rule-count {
  margin-bottom: 12px;
  font-size: 14px;
  color: var(--el-text-color-regular);
}
.rule-collapse {
  border: none;
}
.rule-collapse :deep(.el-collapse-item__header) {
  font-weight: 500;
}
.collapse-title {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.rule-name {
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
}
.rule-flow {
  padding: 4px 0 8px;
}
.flow-block {
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid var(--el-border-color-lighter);
}
.when-block {
  background: var(--el-color-info-light-9);
}
.then-block {
  background: var(--el-color-success-light-9);
}
.flow-label {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  border-bottom: 1px solid var(--el-border-color-extra-light);
}
.flow-pre {
  margin: 0;
  padding: 12px 14px;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 12px;
  line-height: 1.55;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 280px;
  overflow: auto;
  background: transparent;
}
.flow-arrow {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 8px 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.header-snippet {
  margin: 0;
  padding: 12px 14px;
  font-size: 12px;
  line-height: 1.5;
  font-family: 'Consolas', 'Monaco', monospace;
  background: var(--el-fill-color-light);
  border-radius: 8px;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 200px;
  overflow: auto;
}
.muted {
  color: var(--el-text-color-placeholder);
  font-size: 13px;
}
.source-hint {
  margin: 0 0 12px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.rule-textarea {
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
}
.rule-textarea :deep(textarea) {
  font-family: inherit;
}
.validate-result {
  margin-top: 16px;
}
.backup-section {
  margin-top: 8px;
}
.backup-list {
  margin: 8px 0 0;
  padding-left: 20px;
}
.backup-list li {
  margin: 4px 0;
}
.tip {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  margin: 8px 0 0;
}
</style>
