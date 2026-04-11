<template>
  <Transition name="slide-down">
    <div v-if="showBanner" class="pwa-install-banner">
      <div class="banner-content">
        <span class="banner-text">添加到主屏幕，获得更好体验</span>
        <div class="banner-actions">
          <el-button size="small" @click="dismiss">暂不</el-button>
          <el-button type="primary" size="small" @click="install">添加</el-button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const showBanner = ref(false)
let deferredPrompt = null

onMounted(() => {
  if (localStorage.getItem('pwa-install-dismissed')) return
  window.addEventListener('beforeinstallprompt', (e) => {
    e.preventDefault()
    deferredPrompt = e
    showBanner.value = true
  })
  if (window.matchMedia('(display-mode: standalone)').matches || window.navigator.standalone) {
    showBanner.value = false
  }
})

function dismiss() {
  showBanner.value = false
  localStorage.setItem('pwa-install-dismissed', '1')
}

async function install() {
  if (!deferredPrompt) return
  deferredPrompt.prompt()
  const { outcome } = await deferredPrompt.userChoice
  if (outcome === 'accepted') {
    showBanner.value = false
  }
  deferredPrompt = null
}
</script>

<style scoped>
.pwa-install-banner {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 9999;
  background: linear-gradient(135deg, #1a5f4a 0%, #2d8f6f 100%);
  color: #fff;
  padding: 12px 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.banner-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: 600px;
  margin: 0 auto;
}

.banner-text {
  font-size: 14px;
}

.banner-actions {
  display: flex;
  gap: 8px;
}

.slide-down-enter-active,
.slide-down-leave-active {
  transition: transform 0.3s ease;
}

.slide-down-enter-from,
.slide-down-leave-to {
  transform: translateY(-100%);
}
</style>
