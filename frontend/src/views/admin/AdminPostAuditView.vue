<template>
  <div>
    <div class="toolbar">
      <el-button type="primary" @click="load">刷新</el-button>
    </div>

    <el-table :data="rows" border style="width: 100%">
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column prop="userId" label="用户ID" width="100" />
      <el-table-column prop="title" label="标题" min-width="220" />
      <el-table-column prop="createTime" label="提交时间" width="180" />
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag v-if="row.status === 'PENDING'" type="warning">待审核</el-tag>
          <el-tag v-else-if="row.status === 'APPROVED'" type="success">已通过</el-tag>
          <el-tag v-else type="danger">已驳回</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="success" @click="approve(row.id)">通过</el-button>
          <el-button size="small" type="danger" @click="openReject(row)">驳回</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!rows.length" description="暂无待审核帖子" />

    <el-drawer v-model="rejectVisible" title="驳回帖子" direction="rtl" size="420px">
      <el-form label-position="top">
        <el-form-item label="帖子标题">
          <el-input :model-value="current?.title || ''" disabled />
        </el-form-item>
        <el-form-item label="帖子内容">
          <el-input :model-value="current?.content || ''" type="textarea" :rows="6" disabled />
        </el-form-item>
        <el-form-item label="驳回理由">
          <el-input v-model="rejectReason" type="textarea" :rows="4" placeholder="请输入驳回理由" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmReject">确认驳回</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { approvePost, listPendingPosts, rejectPost } from '../../api/admin'
import { logError, logInfo } from '../../utils/logger'

const rows = ref([])
const rejectVisible = ref(false)
const rejectReason = ref('')
const current = ref(null)

async function load() {
  try {
    const res = await listPendingPosts({ page: 1, size: 50 })
    rows.value = res.data?.records || []
    logInfo('ADMIN_POST_AUDIT', 'load_pending_ok', { count: rows.value.length })
  } catch (e) {
    const msg = e?.response?.data?.message || e?.message || '加载失败'
    ElMessage.error(msg)
    logError('ADMIN_POST_AUDIT', 'load_pending_failed', e?.message)
  }
}

async function approve(id) {
  await approvePost(id)
  ElMessage.success('审核通过')
  logInfo('ADMIN_POST_AUDIT', 'approve_post', { id })
  await load()
}

function openReject(row) {
  current.value = row
  rejectReason.value = ''
  rejectVisible.value = true
}

async function confirmReject() {
  if (!current.value) return
  await rejectPost(current.value.id, rejectReason.value || '内容不符合规范')
  ElMessage.success('已驳回')
  logInfo('ADMIN_POST_AUDIT', 'reject_post', { id: current.value.id, reason: rejectReason.value })
  rejectVisible.value = false
  await load()
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  margin-bottom: 12px;
}
</style>

