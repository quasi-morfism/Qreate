<template>
  <div 
    :class="[
      'app-card',
      {
        'app-card--featured': variant === 'featured',
        'app-card--clickable': clickable
      }
    ]"
    @click="handleClick"
  >
    <!-- 应用预览图 -->
    <div class="app-preview">
      <img 
        v-if="app?.cover" 
        :src="app.cover" 
        :alt="app.appName" 
        class="app-cover"
        loading="lazy"
      />
      <div v-else class="app-placeholder">
        <svg width="40" height="40" viewBox="0 0 24 24" fill="currentColor">
          <path
            d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"
          />
        </svg>
      </div>
      
      <!-- 预览遮罩层（仅在可点击时显示） -->
      <div v-if="showPreviewOverlay" class="preview-overlay">
        <button class="preview-button" @click.stop="handlePreview">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
            <path
              d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"
            />
          </svg>
          Preview
        </button>
      </div>
    </div>
    
    <!-- 应用信息 -->
    <div class="app-info">
      <div class="app-header">
        <h3 class="app-title">{{ app?.appName || 'Untitled App' }}</h3>
        
        <!-- 徽章 -->
        <div v-if="badges.length > 0" class="app-badges">
          <span 
            v-for="badge in badges" 
            :key="badge.text"
            :class="badge.className"
          >
            {{ badge.text }}
          </span>
        </div>
      </div>
      
      <!-- 元信息 -->
      <div class="app-meta">
        <div v-if="showTime && app?.createTime" class="meta-item">
          <span class="meta-text">Created {{ formatDate(app.createTime) }}</span>
        </div>
        
        <div v-if="showAuthor && app?.user" class="meta-item">
          <span class="meta-text">by {{ app.user.userName }}</span>
        </div>
        
        <div v-if="showType && app?.codeGenType" class="meta-item">
          <span class="meta-text">{{ app.codeGenType }}</span>
        </div>
      </div>
      
      <!-- 描述 -->
      <div v-if="showDescription && app?.initPrompt" class="app-description">
        <p>{{ truncatedDescription }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { formatDate } from '@/utils'
import { BADGE_STYLES } from '@/utils/constants'
import { openPreview } from '@/utils/deploy'

interface Badge {
  text: string
  className: string
}

interface Props {
  app: API.AppVO
  variant?: 'default' | 'featured'
  clickable?: boolean
  showPreviewOverlay?: boolean
  showTime?: boolean
  showAuthor?: boolean
  showType?: boolean
  showDescription?: boolean
  descriptionMaxLength?: number
}

interface Emits {
  (e: 'click', app: API.AppVO): void
  (e: 'preview', app: API.AppVO): void
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'default',
  clickable: true,
  showPreviewOverlay: false,
  showTime: true,
  showAuthor: false,
  showType: false,
  showDescription: false,
  descriptionMaxLength: 100,
})

const emit = defineEmits<Emits>()

// 计算徽章
const badges = computed<Badge[]>(() => {
  const result: Badge[] = []
  
  // Featured 徽章
  if (props.variant === 'featured' || (props.app?.priority && props.app.priority >= 99)) {
    result.push({
      text: 'Featured',
      className: BADGE_STYLES.featured
    })
  }
  
  // 部署状态徽章
  if (props.app?.deployedTime) {
    result.push({
      text: 'Deployed',
      className: BADGE_STYLES.deployed
    })
  } else {
    result.push({
      text: 'Not Deployed',
      className: BADGE_STYLES.notDeployed
    })
  }
  
  return result
})

// 截断描述
const truncatedDescription = computed(() => {
  if (!props.app?.initPrompt) return ''
  
  const desc = props.app.initPrompt
  if (desc.length <= props.descriptionMaxLength) return desc
  
  return desc.substring(0, props.descriptionMaxLength) + '...'
})

// 点击处理
const handleClick = () => {
  if (props.clickable) {
    emit('click', props.app)
  }
}

// 预览处理
const handlePreview = () => {
  if (props.app?.deployKey) {
    openPreview(props.app.deployKey)
  }
  emit('preview', props.app)
}
</script>

<style scoped>
.app-card {
  @apply bg-white rounded-2xl p-5 transition-all duration-300 border border-gray-200;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1);
}

.app-card--clickable {
  @apply cursor-pointer;
}

.app-card--clickable:hover {
  @apply -translate-y-1 border-emerald-500;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.15);
}

.app-card--featured {
  @apply border-emerald-500 bg-gradient-to-br from-white to-emerald-50;
}

.app-preview {
  @apply w-full h-40 rounded-xl overflow-hidden mb-4 bg-gray-50 relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.app-cover {
  @apply w-full h-full object-cover;
}

.app-placeholder {
  @apply text-gray-400;
}

.preview-overlay {
  @apply absolute inset-0 bg-black bg-opacity-50 flex items-center justify-center opacity-0 transition-opacity duration-300;
}

.app-card--clickable:hover .preview-overlay {
  @apply opacity-100;
}

.preview-button {
  @apply px-4 py-2 bg-emerald-600 text-white rounded-md text-sm font-medium;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.preview-button:hover {
  @apply bg-emerald-700;
}

.app-info {
  @apply text-left;
}

.app-header {
  @apply flex justify-between items-start gap-3 mb-2;
}

.app-title {
  @apply text-lg font-semibold text-gray-900 m-0 leading-tight;
  flex: 1;
  min-width: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.app-badges {
  @apply flex flex-col gap-1 flex-shrink-0;
}

.app-meta {
  @apply space-y-1 mb-3;
}

.meta-item {
  @apply text-sm text-gray-600;
}

.meta-text {
  @apply block;
}

.app-description {
  @apply text-sm text-gray-500 leading-relaxed;
}

.app-description p {
  @apply m-0;
}

/* Responsive adjustments */
@media (max-width: 640px) {
  .app-card {
    @apply p-4;
  }
  
  .app-preview {
    @apply h-32;
  }
  
  .app-title {
    @apply text-base;
  }
  
  .app-badges {
    @apply flex-row gap-2;
  }
}
</style>