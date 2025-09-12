<template>
  <footer class="w-full border-t bg-white" style="height: 56px">
    <div class="footer-container h-full w-full flex items-center justify-center">
      <div class="text-[15px] leading-none colorful-text">
        <span v-for="(char, index) in footerText" :key="index" :style="{ color: getRandomColor() }">
          {{ char }}
        </span>
      </div>
    </div>
  </footer>
</template>

<script setup lang="ts">
const footerText = 'Qreate - your app, your way. No code.'

const colors = [
  '#9aa0a6', // 原来的灰色
  '#8b9499', // 深一点的灰色
  '#a8b1b7', // 浅一点的灰色
  '#9db5a1', // 灰绿色
  '#a1c2a7', // 浅灰绿色
  '#95b59a', // 柔和绿色
  '#88a88d', // 温和绿色
]

const getRandomColor = (): string => {
  return colors[Math.floor(Math.random() * colors.length)]
}
</script>

<style scoped>
.footer-container {
  max-width: 1400px;
  margin: 0 auto;
}
</style>
