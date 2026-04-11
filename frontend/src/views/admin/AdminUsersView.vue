<template>
  <div class="admin-users">
    <div class="filter-bar">
      <el-select v-model="filters.target" placeholder="健身目标" clearable style="width: 120px">
        <el-option label="减脂" value="LOSE_WEIGHT" />
        <el-option label="增肌" value="BUILD_MUSCLE" />
        <el-option label="维持" value="MAINTAIN" />
      </el-select>
      <el-select v-model="filters.bmiRange" placeholder="BMI范围" clearable style="width: 120px">
        <el-option label="偏瘦" value="偏瘦" />
        <el-option label="正常" value="正常" />
        <el-option label="偏胖" value="偏胖" />
        <el-option label="肥胖" value="肥胖" />
      </el-select>
      <el-button type="primary" @click="loadUsers">查询</el-button>
    </div>

    <el-table :data="users" v-loading="loading" style="width: 100%; margin-top: 16px">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="phoneMasked" label="手机号" width="140" />
      <el-table-column prop="nickname" label="昵称" min-width="120" />
      <el-table-column prop="createTime" label="注册时间" width="180" />
      <el-table-column prop="bmiRange" label="BMI范围" width="100" />
      <el-table-column prop="target" label="健身目标" width="100" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'danger'">{{ row.enabled ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="showDetail(row)">查看详情</el-button>
          <el-button type="primary" link size="small" @click="showResetPwd(row)">重置密码</el-button>
          <el-button
            v-if="row.enabled"
            type="danger"
            link
            size="small"
            @click="handleDisable(row)"
          >禁用</el-button>
          <el-button
            v-else
            type="success"
            link
            size="small"
            @click="handleEnable(row)"
          >启用</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="page"
      v-model:page-size="pageSize"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      style="margin-top: 16px"
      @current-change="loadUsers"
      @size-change="loadUsers"
    />

    <el-dialog v-model="detailVisible" title="用户详情" width="500px">
      <div v-if="detail" class="user-detail">
        <p><strong>ID:</strong> {{ detail.id }}</p>
        <p><strong>手机号:</strong> {{ detail.phoneMasked }}</p>
        <p><strong>昵称:</strong> {{ detail.nickname || '-' }}</p>
        <p><strong>角色:</strong> {{ detail.role }}</p>
        <p><strong>状态:</strong> {{ detail.enabled ? '启用' : '禁用' }}</p>
        <p><strong>注册时间:</strong> {{ detail.createTime }}</p>
        <template v-if="detail.healthProfile">
          <el-divider>健康档案</el-divider>
          <p><strong>身高:</strong> {{ detail.healthProfile.height }} cm</p>
          <p><strong>体重:</strong> {{ detail.healthProfile.weight }} kg</p>
          <p><strong>BMI:</strong> {{ detail.healthProfile.bmi }}</p>
          <p><strong>TDEE:</strong> {{ detail.healthProfile.tdee }} 千卡/天</p>
          <p><strong>目标:</strong> {{ targetLabel(detail.healthProfile.target) }}</p>
          <p><strong>运动频率:</strong> {{ freqLabel(detail.healthProfile.exerciseFrequency) }}</p>
        </template>
      </div>
    </el-dialog>

    <el-dialog v-model="resetPwdVisible" title="重置密码" width="400px">
      <el-form :model="resetForm" label-width="80px">
        <el-form-item label="新密码">
          <el-input v-model="resetForm.newPassword" type="password" placeholder="至少6位" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetPwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetting" @click="handleResetPwd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { listUsers, getUserDetail, resetUserPassword, disableUser, enableUser } from '../../api/admin'

const users = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const filters = reactive({ target: '', bmiRange: '' })
const detailVisible = ref(false)
const detail = ref(null)
const resetPwdVisible = ref(false)
const resetForm = reactive({ newPassword: '' })
const resetTarget = ref(null)
const resetting = ref(false)

function targetLabel(t) {
  return { LOSE_WEIGHT: '减脂', BUILD_MUSCLE: '增肌', MAINTAIN: '维持' }[t] || t
}

function freqLabel(f) {
  return { NONE: '无', LIGHT: '轻度', MODERATE: '中度', HIGH: '高强度' }[f] || f
}

async function loadUsers() {
  loading.value = true
  try {
    const res = await listUsers({
      page: page.value,
      size: pageSize.value,
      target: filters.target || undefined,
      bmiRange: filters.bmiRange || undefined
    })
    users.value = res.data?.records || []
    total.value = res.data?.total ?? 0
  } finally {
    loading.value = false
  }
}

async function showDetail(row) {
  try {
    const res = await getUserDetail(row.id)
    detail.value = res.data
    detailVisible.value = true
  } catch (_) {}
}

function showResetPwd(row) {
  resetTarget.value = row
  resetForm.newPassword = ''
  resetPwdVisible.value = true
}

async function handleResetPwd() {
  if (!resetForm.newPassword || resetForm.newPassword.length < 6) {
    ElMessage.warning('密码至少6位')
    return
  }
  resetting.value = true
  try {
    await resetUserPassword(resetTarget.value.id, resetForm.newPassword)
    ElMessage.success('密码已重置')
    resetPwdVisible.value = false
  } finally {
    resetting.value = false
  }
}

async function handleDisable(row) {
  try {
    await disableUser(row.id)
    ElMessage.success('已禁用')
    loadUsers()
  } catch (_) {}
}

async function handleEnable(row) {
  try {
    await enableUser(row.id)
    ElMessage.success('已启用')
    loadUsers()
  } catch (_) {}
}

watch([() => filters.target, () => filters.bmiRange], () => {
  page.value = 1
  loadUsers()
})

onMounted(() => loadUsers())
</script>

<style scoped>
.admin-users {
  padding: 0;
}

.filter-bar {
  display: flex;
  gap: 12px;
  align-items: center;
}

.user-detail p {
  margin: 8px 0;
}
</style>
