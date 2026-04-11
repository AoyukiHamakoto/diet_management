<template>
  <div class="my-recipes-page">
    <PageBackBar fallback="/mine" />

    <el-radio-group v-model="activeStatus" class="status-radios" size="default" @change="onStatusChange">
      <el-radio-button label="ALL">全部</el-radio-button>
      <el-radio-button label="PENDING">待审核</el-radio-button>
      <el-radio-button label="APPROVED">已通过</el-radio-button>
      <el-radio-button label="REJECTED">已拒绝</el-radio-button>
    </el-radio-group>

    <div v-if="!loading && list.length === 0 && !showDemo" class="empty-wrap">
      <el-empty description="您还没有提交过菜谱">
        <el-button type="primary" @click="$router.push('/recipes')">去提交</el-button>
      </el-empty>
    </div>

    <div v-else v-loading="loading" class="recipe-list-wrap">
      <div v-if="showDemo" class="demo-hint">
        以下为示例菜谱，提交并通过审核后将显示在此。
      </div>
      <div :key="listKey" class="recipe-list">
        <div
          v-for="item in displayList"
          :key="'r-' + item.id"
          class="recipe-card"
          :class="{ 'is-demo': item.isDemo }"
        >
          <div class="card-click" @click="onCardClick(item)">
            <img class="cover" :src="item.coverImage || '/vite.svg'" :alt="item.title" />
            <div class="card-body">
              <div class="title">{{ item.title }}</div>
              <div class="meta">
                <span>{{ categoryLabel(item.category) }}</span>
                <span>{{ item.calories != null ? `${item.calories} kcal` : '-- kcal' }}</span>
              </div>
              <div class="status-line">
                <el-tag size="small" :type="statusType(item.status)">{{ statusLabel(item.status) }}</el-tag>
                <span class="created-at">{{ formatDate(item.createdAt) }}</span>
              </div>
              <div v-if="item.status === 'REJECTED' && item.rejectReason" class="reject-reason">
                拒绝原因：{{ item.rejectReason }}
              </div>
              <div v-if="item.status === 'REJECTED'" class="appeal-entry">
                <el-button type="danger" link size="small" @click.stop="$router.push(`/appeal/submit?type=RECIPE&id=${item.id}`)">
                  对审核结果有异议？申诉
                </el-button>
              </div>
            </div>
          </div>

          <div class="actions">
            <template v-if="!item.isDemo && (item.status === 'PENDING' || item.status === 'REJECTED')">
              <el-button size="small" type="primary" @click.stop="$router.push(`/recipe/edit/${item.id}`)">编辑</el-button>
            </template>
            <el-button
              v-if="!item.isDemo"
              size="small"
              type="danger"
              plain
              :loading="deletingId === item.id"
              @click.stop="handleDelete(item)"
            >
              删除
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="total > 0 && !showDemo" class="pager">
      <el-pagination
        v-model:current-page="page"
        background
        layout="prev, pager, next"
        :page-size="size"
        :total="total"
        @current-change="onPageChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyRecipes, deleteRecipe } from '../api/recipe'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageBackBar from '../components/PageBackBar.vue'

const router = useRouter()

/** 无真实数据时展示的示例菜谱（与程序回档「部分用户 demo」一致） */
const DEMO_RECIPES = [
  {
    id: -1,
    title: '示例：低脂鸡胸便当',
    category: 'LUNCH',
    calories: 410,
    status: 'APPROVED',
    createdAt: new Date().toISOString(),
    coverImage: null,
    isDemo: true
  },
  {
    id: -2,
    title: '示例：燕麦酸奶杯',
    category: 'BREAKFAST',
    calories: 280,
    status: 'APPROVED',
    createdAt: new Date().toISOString(),
    coverImage: null,
    isDemo: true
  }
]

const loading = ref(false)
const deletingId = ref(null)
const list = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const activeStatus = ref('ALL')

const listKey = computed(() => `${activeStatus.value}-${page.value}-${total.value}`)

const showDemo = computed(
  () => !loading.value && list.value.length === 0 && activeStatus.value === 'ALL'
)

const displayList = computed(() => {
  if (showDemo.value) return DEMO_RECIPES
  return list.value
})

function onCardClick(item) {
  if (item.isDemo) {
    ElMessage.info('这是示例菜谱，请在菜谱库创建并提交您自己的菜谱')
    router.push('/recipes')
    return
  }
  router.push(`/recipe/${item.id}`)
}

function categoryLabel(v) {
  return ({ BREAKFAST: '早餐', LUNCH: '午餐', DINNER: '晚餐', SNACK: '加餐' }[v] || v || '-')
}

function statusLabel(v) {
  return ({ PENDING: '待审核', APPROVED: '已通过', REJECTED: '已拒绝' }[v] || v || '-')
}

function statusType(v) {
  if (v === 'PENDING') return 'warning'
  if (v === 'APPROVED') return 'success'
  if (v === 'REJECTED') return 'danger'
  return 'info'
}

function formatDate(v) {
  if (!v) return '-'
  return new Date(v).toLocaleDateString('zh-CN')
}

async function loadData() {
  loading.value = true
  try {
    const res = await getMyRecipes({
      page: page.value,
      size: size.value,
      status: activeStatus.value
    })
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function onStatusChange() {
  page.value = 1
  loadData()
}

function onPageChange(p) {
  page.value = p
  loadData()
}

async function handleDelete(item) {
  if (item.isDemo) return
  try {
    await ElMessageBox.confirm(
      '删除后，该菜谱将从您的列表中移除，其他用户也无法再查看。已生成的饮食计划中将显示「菜谱已下架」，但保留当时的营养记录。',
      '确认删除菜谱？',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    return
  }
  deletingId.value = item.id
  try {
    await deleteRecipe(item.id)
    ElMessage.success('删除成功')
    await loadData()
  } catch (e) {
    const msg = e.response?.data?.message || e.message || '删除失败'
    ElMessage.error(msg)
  } finally {
    deletingId.value = null
  }
}

onMounted(loadData)
</script>

<style scoped>
.my-recipes-page {
  padding-bottom: 80px;
}

.status-radios {
  display: flex;
  flex-wrap: wrap;
  gap: 0;
  margin-bottom: 16px;
  width: 100%;
}

.status-radios :deep(.el-radio-button__inner) {
  border-radius: 8px;
}

.recipe-list-wrap {
  min-height: 120px;
}

.recipe-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.recipe-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.card-click {
  display: flex;
  gap: 12px;
  padding: 12px;
  cursor: pointer;
}

.cover {
  width: 96px;
  height: 96px;
  object-fit: cover;
  border-radius: 8px;
}

.card-body {
  flex: 1;
  min-width: 0;
}

.title {
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.meta {
  color: #606266;
  font-size: 13px;
  display: flex;
  gap: 12px;
}

.status-line {
  margin-top: 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.created-at {
  color: #909399;
  font-size: 12px;
}

.reject-reason {
  margin-top: 8px;
  color: #f56c6c;
  font-size: 13px;
}

.actions {
  border-top: 1px solid #f2f2f2;
  padding: 10px 12px;
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.empty-wrap {
  background: #fff;
  border-radius: 12px;
  padding: 8px;
}

.demo-hint {
  font-size: 13px;
  color: #909399;
  margin-bottom: 12px;
  padding: 0 4px;
}

.recipe-card.is-demo {
  opacity: 0.95;
  border: 1px dashed #dcdfe6;
}

.pager {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}
</style>
