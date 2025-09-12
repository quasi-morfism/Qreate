// 导出所有公共组件
export { default as AppCard } from './AppCard.vue'
export { default as UserAvatar } from './UserAvatar.vue'
export { default as SearchInput } from './SearchInput.vue'
export { default as EmptyState } from './EmptyState.vue'
export { default as LoadingSpinner } from './LoadingSpinner.vue'

// 类型定义
export interface AppCardProps {
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

export interface UserAvatarProps {
  userAvatar?: string | null
  userId?: string | number | null
  userName?: string | null
  size?: 'xs' | 'sm' | 'md' | 'lg' | 'xl' | '2xl'
  clickable?: boolean
  showBorder?: boolean
  showOnlineStatus?: boolean
  onlineStatus?: boolean
  badge?: string | number
}