<script setup lang="ts">
import { RouterView } from 'vue-router'
import BasicLayout from '@/layouts/BasicLayout.vue'
import { useLoginUserStore } from '@/stores/loginUser'

const loginUserStore = useLoginUserStore()
loginUserStore.fetchLoginUser()
</script>

<template>
  <n-message-provider>
    <n-dialog-provider>
      <BasicLayout>
        <RouterView />
      </BasicLayout>
    </n-dialog-provider>
  </n-message-provider>
</template>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  margin: 0;
  padding: 0;
  font-family:
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
