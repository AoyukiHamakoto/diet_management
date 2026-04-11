<template>
  <div class="login-page">
    <div class="login-banner">
      <div class="banner-content">
        <h1>健身饮食规划系统</h1>
        <p>科学饮食，健康生活</p>
        <p class="banner-desc">智能规划每日营养，助你达成健身目标</p>
      </div>
    </div>

    <div class="login-form-wrapper">
      <el-watermark :content="['饮食健康']" :gap="[100, 100]" :z-index="1">
        <div class="login-card">
          <div class="login-header">
            <h2>欢迎使用</h2>
          </div>

          <el-tabs v-model="activeTab" class="login-tabs">
            <el-tab-pane label="登录" name="login">
              <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" @submit.prevent="handleLogin">
                <el-form-item prop="phone">
                  <el-input
                    v-model="loginForm.phone"
                    placeholder="手机号"
                    size="large"
                    :prefix-icon="Iphone"
                    maxlength="11"
                    inputmode="numeric"
                    pattern="[0-9]*"
                    clearable
                  />
                </el-form-item>
                <el-form-item prop="password">
                  <el-input
                    v-model="loginForm.password"
                    type="password"
                    placeholder="密码"
                    size="large"
                    :prefix-icon="Lock"
                    show-password
                    @keyup.enter="handleLogin"
                  />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" size="large" :loading="loading" class="submit-btn" @click="handleLogin">
                    登录
                  </el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>
            <el-tab-pane label="注册" name="register">
              <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" @submit.prevent="handleRegister">
                <el-form-item prop="phone">
                  <el-input
                    v-model="registerForm.phone"
                    placeholder="手机号"
                    size="large"
                    :prefix-icon="Iphone"
                    maxlength="11"
                    inputmode="numeric"
                    pattern="[0-9]*"
                    clearable
                  />
                </el-form-item>
                <el-form-item prop="smsCode">
                  <el-input
                    v-model="registerForm.smsCode"
                    placeholder="短信验证码（测试请输入123456）"
                    size="large"
                    :prefix-icon="Message"
                    maxlength="6"
                    inputmode="numeric"
                    clearable
                  />
                </el-form-item>
                <el-form-item prop="password">
                  <el-input
                    v-model="registerForm.password"
                    type="password"
                    placeholder="密码（8位以上，含大小写字母、数字、特殊字符）"
                    size="large"
                    :prefix-icon="Lock"
                    show-password
                  />
                </el-form-item>
                <el-form-item prop="nickname">
                  <el-input
                    v-model="registerForm.nickname"
                    placeholder="昵称（可选）"
                    size="large"
                    :prefix-icon="User"
                    clearable
                  />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" size="large" :loading="loading" class="submit-btn" @click="handleRegister">
                    注册
                  </el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>
          </el-tabs>
        </div>
      </el-watermark>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Iphone, Lock, User, Message } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { login as loginApi, register as registerApi } from '../api/auth'
import { useUserStore } from '../stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const activeTab = ref('login')
const loading = ref(false)
const loginFormRef = ref(null)
const registerFormRef = ref(null)

const loginForm = reactive({
  phone: '',
  password: ''
})

const registerForm = reactive({
  phone: '',
  smsCode: '',
  password: '',
  nickname: ''
})

const validatePhone = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入手机号'))
  } else if (!/^1[3-9]\d{9}$/.test(value)) {
    callback(new Error('手机号格式不正确'))
  } else {
    callback()
  }
}

const loginRules = {
  phone: [{ required: true, validator: validatePhone, trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ]
}

const registerRules = {
  phone: [{ required: true, validator: validatePhone, trigger: 'blur' }],
  smsCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { min: 6, max: 6, message: '测试环境请输入123456', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, message: '密码至少8位', trigger: 'blur' },
    { pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z\d]).{8,64}$/, message: '需包含大小写字母、数字和特殊字符', trigger: 'blur' }
  ]
}

async function handleLogin() {
  await loginFormRef.value?.validate()
  loading.value = true
  try {
    const res = await loginApi(loginForm)
    const { accessToken, refreshToken, user } = res.data
    userStore.setUser({
      accessToken,
      refreshToken,
      userId: user.id,
      nickname: user.nickname,
      avatar: user.avatar,
      role: user.role
    })
    ElMessage.success('登录成功')
    router.push(route.query.redirect || '/')
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  await registerFormRef.value?.validate()
  loading.value = true
  try {
    await registerApi(registerForm)
    loginForm.phone = registerForm.phone
    activeTab.value = 'login'
    ElMessage.success('注册成功，请登录')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  background: #f5f7fa;
}

.login-banner {
  flex: 1;
  display: none;
  background: linear-gradient(135deg, #1a5f4a 0%, #2d8f6f 50%, #3eb489 100%);
  align-items: center;
  justify-content: center;
  padding: 48px;
}

.banner-content {
  color: #fff;
  max-width: 400px;
}

.banner-content h1 {
  font-size: 2rem;
  margin-bottom: 16px;
}

.banner-content p {
  font-size: 1.1rem;
  opacity: 0.95;
}

.banner-desc {
  margin-top: 24px;
  opacity: 0.85;
}

.login-form-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  min-height: 100vh;
}

.login-card {
  width: 100%;
  max-width: 420px;
  background: #fff;
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
}

.login-header {
  text-align: center;
  margin-bottom: 28px;
}

.login-header h2 {
  font-size: 1.5rem;
  color: #1a5f4a;
  margin-bottom: 0;
}

.submit-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
}

.login-tabs :deep(.el-tabs__header) {
  margin-bottom: 20px;
}

.login-tabs :deep(.el-tabs__item) {
  font-size: 15px;
}

@media (min-width: 768px) {
  .login-banner {
    display: flex;
  }
  .login-form-wrapper {
    flex: 1;
    max-width: 480px;
  }
}

@media (max-width: 767px) {
  .login-page {
    flex-direction: column;
    background: linear-gradient(135deg, #1a5f4a 0%, #2d8f6f 100%);
  }
  .login-form-wrapper {
    padding: 20px 16px;
  }
  .login-card {
    padding: 28px 24px;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  }
  .submit-btn {
    height: 52px;
    font-size: 17px;
  }
}
</style>
