<template>
  <n-layout style="min-height: 100vh;">
    <GlobalHeader />
    <n-layout-content style="flex: 1; padding: 24px;">
      <slot />
    </n-layout-content>
    <GlobalFooter />
  </n-layout>
</template>

<script setup lang="ts">
import GlobalHeader from '@/components/layouts/GlobalHeader.vue'
import GlobalFooter from '@/components/layouts/GlobalFooter.vue'
</script>

<style scoped>
:deep(body) {
  margin: 0;
  padding: 0;
}

:deep(#app) {
  margin: 0;
  padding: 0;
}
</style>
