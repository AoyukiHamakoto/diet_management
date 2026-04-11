<template>
  <div class="password-settings-page">
    <PageBackBar fallback="/settings" />
    <div class="form-card">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="当前密码" prop="oldPassword">
          <el-input
            v-model="form.oldPassword"
            type="password"
            placeholder="请输入当前密码"
            show-password
            clearable
            autocomplete="current-password"
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="form.newPassword"
            type="password"
            placeholder="6-20位字符"
            show-password
            clearable
            autocomplete="new-password"
            @input="onNewPasswordInput"
          />
          <div v-if="form.newPassword" class="strength-hint">
            密码强度：<span :class="strengthClass">{{ strengthLabel }}</span>
          </div>
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password
            clearable
            autocomplete="new-password"
          />
        </el-form-item>
        <el-button
          type="primary"
          class="submit-btn"
          :loading="submitting"
          @click="handleSubmit"
        >
          确认修改
        </el-button>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { updatePassword } from '../api/user'
import PageBackBar from '../components/PageBackBar.vue'

const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)
const form = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const strength = computed(() => {
  const p = form.value.newPassword
  if (!p || p.length === 0) return 0
  const hasDigit = /\d/.test(p)
  const hasLetter = /[a-zA-Z]/.test(p)
  const hasSpecial = /[^a-zA-Z0-9]/.test(p)
  const len = p.length
  if (hasDigit && hasLetter && hasSpecial && len >= 8) return 3
  if ((hasDigit && hasLetter) || (len >= 8 && (hasDigit || hasLetter))) return 2
  return 1
})

const strengthLabel = computed(() => {
  const s = strength.value
  return s === 3 ? '强' : s === 2 ? '中' : '弱'
})

const strengthClass = computed(() => {
  const s = strength.value
  return { weak: s === 1, medium: s === 2, strong: s === 3 }
})

function onNewPasswordInput() {}

const validateConfirm = (rule, value, callback) => {
  if (value !== form.value.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

async function handleSubmit() {
  await formRef.value?.validate().catch(() => {})
  if (!form.value.oldPassword || !form.value.newPassword || form.value.newPassword !== form.value.confirmPassword) {
    return
  }
  submitting.value = true
  try {
    await updatePassword({
      oldPassword: form.value.oldPassword,
      newPassword: form.value.newPassword,
      confirmPassword: form.value.confirmPassword
    })
    ElMessage.success('密码修改成功，请使用新密码登录')
    form.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
    router.push('/settings')
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '修改失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.password-settings-page {
  padding: 16px;
}

.form-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.strength-hint {
  font-size: 12px;
  margin-top: 4px;
  color: #909399;
}

.strength-hint .weak {
  color: #f56c6c;
}

.strength-hint .medium {
  color: #e6a23c;
}

.strength-hint .strong {
  color: #67c23a;
}

.submit-btn {
  width: 100%;
  margin-top: 16px;
}
</style>
