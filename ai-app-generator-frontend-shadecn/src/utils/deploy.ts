/**
 * 构造部署应用的预览URL
 */
export const buildPreviewUrl = (deployKey?: string | null): string => {
  if (!deployKey) return ''
  
  // 如果已经是完整的HTTP(S) URL，直接返回
  if (deployKey.startsWith('http://') || deployKey.startsWith('https://')) {
    return deployKey
  }
  
  // 如果包含域名（有点号），添加协议
  if (deployKey.includes('.')) {
    return `${window.location.protocol}//${deployKey}`
  }
  
  // 否则构造相对路径URL
  const cleanKey = deployKey.replace(/^\/+|\/+$/g, '') // 清理前后斜杠
  const deployBaseUrl = import.meta.env.VITE_DEPLOY_BASE_URL || window.location.origin
  
  return `${deployBaseUrl}/${cleanKey}`
}

/**
 * 构造带缓存清除参数的预览URL
 */
export const buildPreviewUrlWithCache = (deployKey?: string | null): string => {
  const baseUrl = buildPreviewUrl(deployKey)
  if (!baseUrl) return ''
  
  const separator = baseUrl.includes('?') ? '&' : '?'
  return `${baseUrl}${separator}t=${new Date().getTime()}`
}

/**
 * 打开预览窗口
 */
export const openPreview = (deployKey?: string | null): void => {
  const url = buildPreviewUrlWithCache(deployKey)
  if (url) {
    window.open(url, '_blank')
  }
}