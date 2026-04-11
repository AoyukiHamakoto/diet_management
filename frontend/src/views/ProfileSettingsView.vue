<template>
  <div class="profile-settings-page">
    <PageBackBar fallback="/settings" />
    <div v-if="loading" class="loading-wrap">
      <el-skeleton :rows="4" animated />
    </div>
    <template v-else>
      <!-- 头像 -->
      <div class="avatar-section">
        <input
          ref="fileInputRef"
          type="file"
          accept="image/jpeg,image/png,image/jpg"
          class="file-input"
          @change="onFileChange"
        />
        <div class="avatar-wrap" @click="triggerFileInput">
          <el-avatar :size="96" :src="avatarPreview" class="avatar">
            {{ (form.nickname || '我').charAt(0) }}
          </el-avatar>
          <div v-if="uploading" class="avatar-mask">
            <el-icon class="is-loading"><Loading /></el-icon>
          </div>
        </div>
        <div class="avatar-hint">点击更换头像</div>
      </div>

      <!-- 基本信息 -->
      <div class="form-card">
        <el-form label-position="top">
          <el-form-item label="手机号">
            <el-input :model-value="profile?.phone" disabled />
          </el-form-item>
          <el-form-item label="昵称">
            <el-input
              v-model="form.nickname"
              placeholder="2-20个字符，支持中文/英文/数字"
              maxlength="20"
              show-word-limit
              clearable
              @input="markDirty"
            />
          </el-form-item>
          <el-form-item label="注册时间">
            <el-input :model-value="createdAtDisplay" disabled />
          </el-form-item>
        </el-form>
        <el-button
          type="primary"
          class="save-btn"
          :disabled="!dirty"
          :loading="saving"
          @click="handleSave"
        >
          保存
        </el-button>
        <div class="password-entry">
          <span @click="$router.push('/settings/password')">修改密码</span>
          <el-icon><ArrowRight /></el-icon>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading, ArrowRight } from '@element-plus/icons-vue'
import { getProfile, updateProfile, uploadAvatar } from '../api/user'
import PageBackBar from '../components/PageBackBar.vue'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const saving = ref(false)
const uploading = ref(false)
const dirty = ref(false)
const profile = ref(null)
const form = ref({ nickname: '', avatar: '' })
const fileInputRef = ref(null)

const avatarPreview = computed(() => {
  const url = form.value.avatar || profile.value?.avatar
  if (!url) return ''
  return url.startsWith('http') || url.startsWith('/') ? url : ''
})

const createdAtDisplay = computed(() => {
  const raw = profile.value?.createdAt
  if (!raw) return '--'
  try {
    const d = new Date(raw)
    return isNaN(d.getTime()) ? raw : d.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch {
    return raw
  }
})

function markDirty() {
  dirty.value = true
}

function triggerFileInput() {
  fileInputRef.value?.click()
}

async function onFileChange(e) {
  const file = e.target?.files?.[0]
  if (!file) return
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过2MB')
    return
  }
  const ok = ['image/jpeg', 'image/png', 'image/jpg'].includes(file.type)
  if (!ok) {
    ElMessage.warning('仅支持 jpg、png 格式')
    return
  }
  uploading.value = true
  e.target.value = ''
  try {
    const res = await uploadAvatar(file)
    const url = res.data?.avatarUrl
    if (url) {
      form.value.avatar = url
      dirty.value = true
    }
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

async function loadProfile() {
  loading.value = true
  try {
    const res = await getProfile()
    profile.value = res.data
    form.value.nickname = res.data?.nickname ?? ''
    form.value.avatar = res.data?.avatar ?? ''
  } catch (_) {
    profile.value = null
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  if (!dirty.value) return
  saving.value = true
  try {
    await updateProfile({
      nickname: form.value.nickname || undefined,
      avatar: form.value.avatar || undefined
    })
    userStore.setUser({
      accessToken: userStore.accessToken,
      refreshToken: userStore.refreshToken,
      userId: userStore.userId,
      nickname: form.value.nickname,
      avatar: form.value.avatar,
      role: userStore.role
    })
    ElMessage.success('保存成功')
    dirty.value = false
    router.push('/settings')
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(loadProfile)
</script>

<style scoped>
.profile-settings-page {
  padding: 16px;
}

.loading-wrap {
  padding: 20px;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px;
  background: #fff;
  border-radius: 12px;
  margin-bottom: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.file-input {
  display: none;
}

.avatar-wrap {
  position: relative;
  cursor: pointer;
}

.avatar {
  background: #1a5f4a;
}

.avatar-mask {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: #fff;
}

.avatar-hint {
  margin-top: 8px;
  font-size: 13px;
  color: #909399;
}

.form-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.save-btn {
  width: 100%;
  margin-top: 16px;
}

.password-entry {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
  color: #1a5f4a;
  cursor: pointer;
  font-size: 14px;
}
</style>
