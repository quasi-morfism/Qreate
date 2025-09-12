// 按钮样式常量
export const BUTTON_STYLES = {
  primary: 'px-4 py-2 rounded bg-emerald-600 text-white hover:bg-emerald-700 disabled:opacity-50 transition-colors',
  secondary: 'px-4 py-2 rounded border border-gray-300 text-gray-700 hover:bg-gray-50 disabled:opacity-50 transition-colors',
  danger: 'px-4 py-2 rounded bg-red-600 text-white hover:bg-red-700 disabled:opacity-50 transition-colors',
  ghost: 'px-4 py-2 rounded text-gray-600 hover:bg-gray-100 disabled:opacity-50 transition-colors',
  success: 'px-4 py-2 rounded bg-green-600 text-white hover:bg-green-700 disabled:opacity-50 transition-colors',
} as const

// 徽章样式常量
export const BADGE_STYLES = {
  featured: 'inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold bg-emerald-100 text-emerald-700',
  deployed: 'inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold bg-emerald-100 text-emerald-700',
  notDeployed: 'inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold bg-gray-100 text-gray-700',
  primary: 'inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold bg-blue-100 text-blue-700',
} as const

// API端点常量
export const API_ENDPOINTS = {
  DEPLOY_BASE_URL: import.meta.env.VITE_DEPLOY_BASE_URL || 'http://localhost',
  API_BASE_URL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8100/api',
  AVATAR_BASE_URL: 'https://avatar.iran.liara.run/public',
} as const

// 主题颜色常量
export const THEME_COLORS = {
  primary: '#10b981',      // emerald-500
  primaryDark: '#059669',  // emerald-600
  success: '#22c55e',      // green-500
  warning: '#f59e0b',      // amber-500
  error: '#ef4444',        // red-500
  info: '#3b82f6',         // blue-500
} as const

// 分页配置常量
export const PAGINATION_CONFIG = {
  DEFAULT_PAGE_SIZE: 20,
  PAGE_SIZE_OPTIONS: [10, 20, 50, 100],
  SIBLING_COUNT: 1,
} as const

// 应用卡片配置
export const APP_CARD_CONFIG = {
  PLACEHOLDER_IMAGE: '/placeholder-app.png',
  ASPECT_RATIO: '4/3',
} as const