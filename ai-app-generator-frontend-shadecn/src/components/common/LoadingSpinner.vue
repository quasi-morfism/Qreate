<template>
  <div :class="['loading-spinner', sizeClass, centerClass]">
    <div class="spinner-container">
      <!-- 旋转圆环 -->
      <div v-if="type === 'spinner'" class="spinner-ring">
        <div></div>
        <div></div>
        <div></div>
        <div></div>
      </div>
      
      <!-- 跳动的点 -->
      <div v-else-if="type === 'dots'" class="spinner-dots">
        <div></div>
        <div></div>
        <div></div>
      </div>
      
      <!-- 脉冲效果 -->
      <div v-else-if="type === 'pulse'" class="spinner-pulse">
        <div class="pulse-circle"></div>
      </div>
      
      <!-- 默认旋转图标 -->
      <div v-else class="spinner-icon">
        <svg class="animate-spin" viewBox="0 0 24 24" fill="none">
          <circle 
            class="opacity-25" 
            cx="12" 
            cy="12" 
            r="10" 
            stroke="currentColor" 
            stroke-width="4"
          />
          <path 
            class="opacity-75" 
            fill="currentColor" 
            d="m4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
          />
        </svg>
      </div>
    </div>
    
    <!-- 加载文本 -->
    <div v-if="text" :class="['loading-text', textClass]">
      {{ text }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

type SpinnerType = 'default' | 'spinner' | 'dots' | 'pulse'
type SpinnerSize = 'xs' | 'sm' | 'md' | 'lg' | 'xl'

interface Props {
  /** 加载动画类型 */
  type?: SpinnerType
  /** 尺寸 */
  size?: SpinnerSize
  /** 加载文本 */
  text?: string
  /** 是否居中显示 */
  center?: boolean
  /** 颜色主题 */
  color?: string
}

const props = withDefaults(defineProps<Props>(), {
  type: 'default',
  size: 'md',
  center: false,
  color: 'text-emerald-600',
})

// 尺寸映射
const sizeConfig = {
  xs: { icon: 'w-4 h-4', text: 'text-xs', gap: 'gap-2' },
  sm: { icon: 'w-5 h-5', text: 'text-sm', gap: 'gap-2' },
  md: { icon: 'w-6 h-6', text: 'text-base', gap: 'gap-3' },
  lg: { icon: 'w-8 h-8', text: 'text-lg', gap: 'gap-3' },
  xl: { icon: 'w-10 h-10', text: 'text-xl', gap: 'gap-4' },
}

const sizeClass = computed(() => {
  const config = sizeConfig[props.size]
  return `${config.gap} ${props.color}`
})

const iconClass = computed(() => sizeConfig[props.size].icon)
const textClass = computed(() => sizeConfig[props.size].text)
const centerClass = computed(() => props.center ? 'justify-center items-center min-h-[200px]' : '')
</script>

<style scoped>
.loading-spinner {
  @apply flex flex-col items-center;
}

.spinner-container {
  @apply relative;
}

/* 默认旋转图标样式 */
.spinner-icon {
  @apply flex items-center justify-center;
}

.spinner-icon svg {
  width: var(--spinner-size, 1.5rem);
  height: var(--spinner-size, 1.5rem);
}

/* 圆环旋转器 */
.spinner-ring {
  @apply relative;
  width: var(--spinner-size, 1.5rem);
  height: var(--spinner-size, 1.5rem);
}

.spinner-ring div {
  @apply absolute border-2 border-current border-t-transparent rounded-full animate-spin;
  width: 100%;
  height: 100%;
}

.spinner-ring div:nth-child(1) {
  animation-delay: -0.45s;
}

.spinner-ring div:nth-child(2) {
  animation-delay: -0.3s;
}

.spinner-ring div:nth-child(3) {
  animation-delay: -0.15s;
}

/* 跳动的点 */
.spinner-dots {
  @apply flex gap-1;
}

.spinner-dots div {
  @apply w-2 h-2 bg-current rounded-full animate-bounce;
}

.spinner-dots div:nth-child(1) {
  animation-delay: -0.32s;
}

.spinner-dots div:nth-child(2) {
  animation-delay: -0.16s;
}

/* 脉冲效果 */
.spinner-pulse {
  @apply relative flex items-center justify-center;
  width: var(--spinner-size, 1.5rem);
  height: var(--spinner-size, 1.5rem);
}

.pulse-circle {
  @apply absolute w-full h-full bg-current rounded-full animate-pulse opacity-75;
}

.pulse-circle::before,
.pulse-circle::after {
  @apply absolute w-full h-full bg-current rounded-full animate-pulse;
  content: '';
  top: 0;
  left: 0;
}

.pulse-circle::before {
  animation-delay: 0.5s;
}

.pulse-circle::after {
  animation-delay: 1s;
}

.loading-text {
  @apply text-gray-600 font-medium;
}

/* 设置CSS变量用于不同尺寸 */
.loading-spinner:has(.w-4) {
  --spinner-size: 1rem;
}

.loading-spinner:has(.w-5) {
  --spinner-size: 1.25rem;
}

.loading-spinner:has(.w-6) {
  --spinner-size: 1.5rem;
}

.loading-spinner:has(.w-8) {
  --spinner-size: 2rem;
}

.loading-spinner:has(.w-10) {
  --spinner-size: 2.5rem;
}
</style>