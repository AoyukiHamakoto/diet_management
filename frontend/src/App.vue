<template>
  <div id="app-wrap">
    <PwaInstallBanner />
    <router-view />
  </div>
</template>

<script setup>
import PwaInstallBanner from './components/PwaInstallBanner.vue'
</script>

<style>
/* 响应式断点: Mobile <768px | Tablet 768-1024px | Desktop >1024px */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
html {
  font-size: 16px;
}
@media (max-width: 767px) {
  html {
    font-size: calc(100vw / 375 * 16);
  }
  body {
    font-size: 14px;
  }
}
html, body, #app, #app-wrap {
  min-height: 100vh;
  min-height: 100dvh;
}
/* 底部安全区 - iPhone 刘海屏 */
@supports (padding-bottom: env(safe-area-inset-bottom)) {
  .mobile-tabbar,
  .mobile-bottom-safe {
    padding-bottom: calc(env(safe-area-inset-bottom) + 8px);
  }
}
</style>
