<template>
  <n-layout-header bordered>
    <n-flex justify="space-between" align="center" style="height: 64px; padding: 0 24px">
      <n-flex align="center" :wrap="false">
        <img
          src="/src/assets/logo.png"
          alt="Logo"
          style="height: 40px; width: auto; margin-right: 12px"
        />
        <n-text
          tag="h1"
          :depth="1"
          style="margin: 0; font-size: 20px; font-weight: 600; letter-spacing: -0.02em"
        >
          Qreate
        </n-text>
        <n-menu
          mode="horizontal"
          :options="menuOptions"
          v-model:value="activeKey"
          style="margin-left: 48px"
          :root-props="{
            style: 'background-color: transparent;',
          }"
        />
      </n-flex>
      <n-flex align="center" :wrap="false">
        <n-button type="primary" size="medium"> Login </n-button>
      </n-flex>
    </n-flex>
  </n-layout-header>
</template>

<script setup lang="ts">
import { ref, h } from 'vue'

import { RouterLink } from 'vue-router'
import type { MenuOption } from 'naive-ui'

const activeKey = ref('home')

const menuOptions: MenuOption[] = [
  {
    label: () => h(RouterLink, { to: '/' }, { default: () => 'Home' }),
    key: 'home',
  },
  {
    label: () => h(RouterLink, { to: '/about' }, { default: () => 'About' }),
    key: 'about',
  },
]
</script>

<style scoped>
:deep(.n-menu-item-content) {
  display: flex;
  align-items: center;
  height: 100%;
}

:deep(.n-menu-item-content a) {
  text-decoration: none;
  color: inherit;
  font-weight: 700;
  font-size: 14px;
  line-height: 1;
}

:deep(.n-menu-item-content a:hover) {
  color: inherit;
}

:deep(.n-menu-item) {
  display: flex;
  align-items: center;
}
</style>
