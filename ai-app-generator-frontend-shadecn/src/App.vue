<script setup lang="ts">
import { RouterView, useRoute } from 'vue-router'
import BasicLayout from '@/layouts/BasicLayout.vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { computed } from 'vue'
import { Toaster } from '@/components/ui/sonner'

const loginUserStore = useLoginUserStore()
loginUserStore.fetchLoginUser()

const route = useRoute()
const useBasicLayout = computed(() => {
  return route.name !== 'appGenerate'
})
</script>

<template>
  <BasicLayout v-if="useBasicLayout">
    <RouterView />
  </BasicLayout>
  <RouterView v-else />
  <!-- Global Toaster for notifications -->
  <Toaster />
</template>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

/* 为中文字符定义专用字体 */
@font-face {
  font-family: 'CustomFont';
  src: local('PingFang SC'), local('Hiragino Sans GB'), local('Microsoft YaHei'), local('微软雅黑');
  unicode-range:
    U+4E00- U+9FFF,
    U+3400- U+4DBF,
    U+20000- U+2A6DF,
    U+2A700- U+2B73F,
    U+2B740- U+2B81F,
    U+2B820- U+2CEAF,
    U+F900- U+FAFF,
    U+2F800- U+2FA1F;
}

body {
  margin: 0;
  padding: 0;
  font-family:
    'CustomFont',
    v-sans,
    system-ui,
    -apple-system,
    BlinkMacSystemFont,
    'Segoe UI',
    sans-serif,
    'Apple Color Emoji',
    'Segoe UI Emoji',
    'Segoe UI Symbol';
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

#app {
  margin: 0;
  padding: 0;
  min-height: 100vh;
}
</style>
