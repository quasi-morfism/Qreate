<template>
  <div :class="['empty-state', sizeClass]">
    <!-- 图标 -->
    <div class="empty-icon">
      <slot name="icon">
        <svg 
          :width="iconSize" 
          :height="iconSize" 
          viewBox="0 0 24 24" 
          fill="currentColor"
          class="text-gray-300"
        >
          <path v-if="type === 'search'" d="M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/>
          <path v-else-if="type === 'apps'" d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
          <path v-else-if="type === 'users'" d="M12 2C13.1 2 14 2.9 14 4C14 5.1 13.1 6 12 6C10.9 6 10 5.1 10 4C10 2.9 10.9 2 12 2ZM21 9V7L15 1V3H9V1L3 7V9H4V20H10V14H14V20H20V9H21Z"/>
          <path v-else d="M19 3H5C3.9 3 3 3.9 3 5V19C3 20.1 3.9 21 5 21H19C20.1 21 21 20.1 21 19V5C21 3.9 20.1 3 19 3ZM19 19H5V5H19V19Z"/>
        </svg>
      </slot>
    </div>
    
    <!-- 标题 -->
    <h3 v-if="title" class="empty-title">
      {{ title }}
    </h3>
    
    <!-- 描述 -->
    <p v-if="description" class="empty-description">
      {{ description }}
    </p>
    
    <!-- 操作按钮 -->
    <div v-if="$slots.action || actionText" class="empty-action">
      <slot name="action">
        <Button 
          v-if="actionText"
          :variant="actionVariant"
          @click="handleAction"
        >
          {{ actionText }}
        </Button>
      </slot>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Button } from '@/components/ui/button'

type EmptyType = 'default' | 'search' | 'apps' | 'users'
type EmptySize = 'sm' | 'md' | 'lg'

interface Props {
  /** 空状态类型 */
  type?: EmptyType
  /** 尺寸 */
  size?: EmptySize
  /** 标题 */
  title?: string
  /** 描述 */
  description?: string
  /** 操作按钮文本 */
  actionText?: string
  /** 操作按钮变体 */
  actionVariant?: 'default' | 'destructive' | 'outline' | 'secondary' | 'ghost' | 'link'
}

interface Emits {
  (e: 'action'): void
}

const props = withDefaults(defineProps<Props>(), {
  type: 'default',
  size: 'md',
  actionVariant: 'default',
})

const emit = defineEmits<Emits>()

// 尺寸映射
const sizeConfig = {
  sm: {
    container: 'py-8',
    icon: 32,
    title: 'text-base',
    description: 'text-sm'
  },
  md: {
    container: 'py-12',
    icon: 48,
    title: 'text-lg',
    description: 'text-base'
  },
  lg: {
    container: 'py-16',
    icon: 64,
    title: 'text-xl',
    description: 'text-lg'
  }
}

const sizeClass = computed(() => sizeConfig[props.size].container)
const iconSize = computed(() => sizeConfig[props.size].icon)
const titleClass = computed(() => sizeConfig[props.size].title)
const descriptionClass = computed(() => sizeConfig[props.size].description)

const handleAction = () => {
  emit('action')
}
</script>

<style scoped>
.empty-state {
  @apply flex flex-col items-center justify-center text-center;
}

.empty-icon {
  @apply mb-4;
}

.empty-title {
  @apply font-semibold text-gray-900 mb-2;
}

.empty-description {
  @apply text-gray-600 max-w-sm mx-auto leading-relaxed;
}

.empty-action {
  @apply mt-6;
}
</style>