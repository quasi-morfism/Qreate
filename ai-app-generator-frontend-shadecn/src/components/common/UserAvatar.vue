<template>
  <div 
    :class="[
      'user-avatar',
      sizeClass,
      {
        'user-avatar--clickable': clickable,
        'user-avatar--border': showBorder
      }
    ]"
    @click="handleClick"
  >
    <img
      :src="avatarUrl"
      :alt="alt"
      class="avatar-image"
      loading="lazy"
      @error="handleImageError"
    />
    
    <!-- 在线状态指示器 -->
    <div 
      v-if="showOnlineStatus"
      :class="[
        'online-status',
        onlineStatus ? 'online-status--online' : 'online-status--offline'
      ]"
    />
    
    <!-- 角标 -->
    <div 
      v-if="badge"
      class="avatar-badge"
    >
      {{ badge }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { getAvatarUrl } from '@/utils'

type AvatarSize = 'xs' | 'sm' | 'md' | 'lg' | 'xl' | '2xl'

interface Props {
  /** 用户头像URL */
  userAvatar?: string | null
  /** 用户ID，用于生成默认头像 */
  userId?: string | number | null
  /** 用户名，用于alt属性 */
  userName?: string | null
  /** 头像尺寸 */
  size?: AvatarSize
  /** 是否可点击 */
  clickable?: boolean
  /** 是否显示边框 */
  showBorder?: boolean
  /** 是否显示在线状态 */
  showOnlineStatus?: boolean
  /** 在线状态 */
  onlineStatus?: boolean
  /** 角标文本 */
  badge?: string | number
}

interface Emits {
  (e: 'click'): void
  (e: 'imageError'): void
}

const props = withDefaults(defineProps<Props>(), {
  size: 'md',
  clickable: false,
  showBorder: false,
  showOnlineStatus: false,
  onlineStatus: false,
})

const emit = defineEmits<Emits>()

const imageError = ref(false)

// 尺寸映射
const sizeMap = {
  xs: 'w-6 h-6',
  sm: 'w-8 h-8',
  md: 'w-10 h-10',
  lg: 'w-12 h-12',
  xl: 'w-16 h-16',
  '2xl': 'w-20 h-20',
}

// 计算样式类
const sizeClass = computed(() => sizeMap[props.size])

// 计算头像URL
const avatarUrl = computed(() => {
  if (imageError.value) {
    return getAvatarUrl(null, props.userId)
  }
  return getAvatarUrl(props.userAvatar, props.userId)
})

// 计算alt属性
const alt = computed(() => {
  return props.userName ? `${props.userName}'s avatar` : 'User avatar'
})

// 处理点击
const handleClick = () => {
  if (props.clickable) {
    emit('click')
  }
}

// 处理图片加载错误
const handleImageError = () => {
  imageError.value = true
  emit('imageError')
}
</script>

<style scoped>
.user-avatar {
  @apply relative inline-block flex-shrink-0;
}

.user-avatar--clickable {
  @apply cursor-pointer transition-transform duration-200;
}

.user-avatar--clickable:hover {
  @apply scale-105;
}

.user-avatar--border .avatar-image {
  @apply ring-2 ring-white ring-offset-2;
}

.avatar-image {
  @apply w-full h-full object-cover rounded-full;
}

.online-status {
  @apply absolute bottom-0 right-0 w-3 h-3 rounded-full border-2 border-white;
}

.online-status--online {
  @apply bg-green-500;
}

.online-status--offline {
  @apply bg-gray-400;
}

.avatar-badge {
  @apply absolute -top-1 -right-1 bg-red-500 text-white text-xs rounded-full min-w-[1.25rem] h-5 flex items-center justify-center px-1;
  font-size: 0.625rem;
  line-height: 1;
}

/* 响应式调整在线状态大小 */
.user-avatar:has(.w-6) .online-status {
  @apply w-2 h-2;
}

.user-avatar:has(.w-8) .online-status {
  @apply w-2.5 h-2.5;
}

.user-avatar:has(.w-16) .online-status,
.user-avatar:has(.w-20) .online-status {
  @apply w-4 h-4;
}
</style>