<template>
  <div class="admin-recipe-audit">
    <el-tabs v-model="activeTab" class="audit-tabs">
      <el-tab-pane label="待审核" name="pending">
        <el-row :gutter="20">
          <el-col :span="10">
            <el-card shadow="hover">
              <template #header>
                <span>待审核列表</span>
                <el-tag type="warning" style="margin-left: 8px">{{ pendingList.length }}</el-tag>
              </template>
              <el-table
                :data="pendingList"
                v-loading="loading"
                highlight-current-row
                @current-change="onSelectPending"
              >
                <el-table-column prop="id" label="ID" width="60" />
                <el-table-column prop="title" label="菜名" show-overflow-tooltip />
                <el-table-column prop="category" label="分类" width="80" />
                <el-table-column label="阶段" width="110">
                  <template #default="{ row }">
                    <el-tag size="small" :type="row.reviewStage === 'FIRST_APPROVED' ? 'warning' : 'info'">
                      {{ stageLabel(row.reviewStage) }}
                    </el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-col>
          <el-col :span="14">
            <el-card v-if="selected" shadow="hover">
              <template #header>
                <div class="preview-header">
                  <span>预览</span>
                  <div class="preview-actions">
                    <el-button type="danger" plain size="small" :loading="deletingId === selected.id" @click="confirmDelete(selected)">
                      删除
                    </el-button>
                    <el-button type="danger" size="small" @click="showReject(selected)">拒绝</el-button>
                    <el-button type="success" size="small" @click="handleApprove(selected)">
                      {{ approveButtonLabel(selected) }}
                    </el-button>
                  </div>
                </div>
              </template>
              <div class="recipe-preview">
                <div v-if="selected.coverImage" class="cover">
                  <img :src="selected.coverImage" alt="封面" />
                </div>
                <p><strong>菜名:</strong> {{ selected.title }}</p>
                <p><strong>分类:</strong> {{ selected.category }}</p>
                <p><strong>烹饪时长:</strong> {{ selected.cookingTime }} 分钟</p>
                <p><strong>难度:</strong> {{ selected.difficulty }}</p>
                <p><strong>匹配标签:</strong> {{ (selected.matchTags || []).join(', ') || '-' }}</p>
                <p><strong>审核阶段:</strong> {{ stageLabel(selected.reviewStage) }}</p>
              </div>
            </el-card>
            <el-empty v-else description="请从左侧选择待审核菜谱" />
          </el-col>
        </el-row>
      </el-tab-pane>

      <el-tab-pane label="全部菜谱" name="all">
        <el-table :data="allList" v-loading="allLoading" style="width: 100%">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="title" label="菜名" min-width="160" show-overflow-tooltip />
          <el-table-column prop="category" label="分类" width="100" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="提交时间" width="180" />
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button type="danger" link size="small" :loading="deletingId === row.id" @click="confirmDelete(row)">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="all-pagination">
          <el-pagination
            v-model:current-page="allPage"
            v-model:page-size="allSize"
            :total="allTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            background
            @size-change="loadAllRecipes"
            @current-change="loadAllRecipes"
          />
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="rejectDialogVisible" title="拒绝菜谱" width="400px">
      <el-input v-model="rejectReason" type="textarea" placeholder="请输入拒绝原因" :rows="3" />
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="rejecting" @click="handleReject">确定拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPendingRecipes, approveRecipe, rejectRecipe, getAdminRecipeList, adminDeleteRecipe } from '../../api/recipe'

const activeTab = ref('pending')
const pendingList = ref([])
const loading = ref(false)
const selected = ref(null)
const rejectDialogVisible = ref(false)
const rejectReason = ref('')
const rejecting = ref(false)
const rejectTarget = ref(null)
const deletingId = ref(null)

const allList = ref([])
const allLoading = ref(false)
const allPage = ref(1)
const allSize = ref(20)
const allTotal = ref(0)

const stageLabel = (s) =>
  ({
    PENDING: '待初审',
    FIRST_APPROVED: '待终审',
    FINAL_APPROVED: '终审通过',
    REJECTED: '已拒绝'
  }[s] || '待初审')

function statusLabel(s) {
  return { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已拒绝' }[s] || s
}

function statusType(s) {
  return { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }[s] || 'info'
}

const approveButtonLabel = (row) => {
  return row?.reviewStage === 'FIRST_APPROVED' ? '终审通过' : '初审通过'
}

async function loadPending() {
  loading.value = true
  try {
    const res = await getPendingRecipes()
    pendingList.value = res.data || []
    if (selected.value && !pendingList.value.find((r) => r.id === selected.value.id)) {
      selected.value = null
    }
  } finally {
    loading.value = false
  }
}

async function loadAllRecipes() {
  allLoading.value = true
  try {
    const res = await getAdminRecipeList({ page: allPage.value, size: allSize.value })
    const data = res.data || {}
    allList.value = data.records || []
    allTotal.value = data.total ?? 0
  } finally {
    allLoading.value = false
  }
}

function onSelectPending(row) {
  selected.value = row
}

async function handleApprove(row) {
  try {
    const res = await approveRecipe(row.id)
    ElMessage.success(res?.data?.message || '审核通过')
    loadPending()
  } catch (_) {}
}

function showReject(row) {
  rejectTarget.value = row
  rejectReason.value = ''
  rejectDialogVisible.value = true
}

async function handleReject() {
  if (!rejectReason.value?.trim()) {
    ElMessage.warning('请输入拒绝原因')
    return
  }
  rejecting.value = true
  try {
    await rejectRecipe(rejectTarget.value.id, rejectReason.value)
    ElMessage.success('已拒绝')
    rejectDialogVisible.value = false
    loadPending()
  } finally {
    rejecting.value = false
  }
}

async function confirmDelete(row) {
  if (!row?.id) return
  try {
    await ElMessageBox.confirm(
      `确定删除菜谱「${row.title || row.id}」吗？删除后为逻辑删除，用户端将不再展示。`,
      '删除确认',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    return
  }
  deletingId.value = row.id
  try {
    await adminDeleteRecipe(row.id)
    ElMessage.success('已删除')
    if (selected.value?.id === row.id) {
      selected.value = null
    }
    await loadPending()
    if (activeTab.value === 'all') {
      await loadAllRecipes()
    }
  } catch (_) {
  } finally {
    deletingId.value = null
  }
}

watch(activeTab, (v) => {
  if (v === 'pending') loadPending()
  else loadAllRecipes()
})

onMounted(() => {
  loadPending()
})
</script>

<style scoped>
.admin-recipe-audit {
  padding: 0;
}

.audit-tabs :deep(.el-tabs__content) {
  padding-top: 16px;
}

.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.preview-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.recipe-preview .cover {
  margin-bottom: 16px;
}

.recipe-preview .cover img {
  max-width: 100%;
  max-height: 200px;
  border-radius: 8px;
}

.recipe-preview p {
  margin: 8px 0;
}

.all-pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
