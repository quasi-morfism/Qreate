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

  // 开发环境下，使用代理路径
  if (import.meta.env.DEV) {
    const cleanKey = deployKey.replace(/^\/+|\/+$/g, '') // 清理前后斜杠
    // Vite dev server runs on a different port, requests should be proxied
    return `/deployurl/${cleanKey}`
  }

  // 否则构造相对路径URL
  const cleanKey = deployKey.replace(/^\/+|\/+$/g, '') // 清理前后斜杠
  const deployBaseUrl = import.meta.env.VITE_DEPLOY_BASE_URL || window.location.origin

  return `${deployBaseUrl}/${cleanKey}`
}

/**
 * 构造用于iframe的同源预览URL（解决跨域问题）
 * 后端StaticResourceController映射: /static/{deployKey}/**
 * 前端通过 /api 代理访问: /api/static/{deployKey}/**
 */
export const buildIframePreviewUrl = (deployKey?: string | null): string => {
  console.log('🔧 buildIframePreviewUrl called with deployKey:', deployKey)

  if (!deployKey) {
    console.log('❌ No deployKey provided')
    return ''
  }

  // 如果已经是完整的HTTP(S) URL，直接返回
  if (deployKey.startsWith('http://') || deployKey.startsWith('https://')) {
    console.log('🌐 Full URL detected, returning as-is:', deployKey)
    return deployKey
  }

  // 如果包含域名（有点号），添加协议
  if (deployKey.includes('.')) {
    const url = `${window.location.protocol}//${deployKey}`
    console.log('🏠 Domain detected, adding protocol:', url)
    return url
  }

  const cleanKey = deployKey.replace(/^\/+|\/+$/g, '') // 清理前后斜杠
  console.log('🧹 Clean deployKey:', cleanKey)

  // 使用静态资源服务器路径
  // 后端会自动处理根路径重定向到index.html（对于所有项目包括Vue）
  let finalUrl = `/api/static/${cleanKey}/`

  // 检查Vue项目类型，需要指向dist目录
  if (cleanKey.includes('vue_project_')) {
    finalUrl = `/api/static/${cleanKey}/dist/`
    console.log('🟢 Vue project detected, URL:', finalUrl)
    console.log('🟢 Backend will auto-redirect "/" to "/index.html"')
  } else {
    console.log('📄 Other project type, URL:', finalUrl)
  }

  console.log('🎯 Final iframe URL:', finalUrl)
  return finalUrl
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
 * 构造带缓存清除参数的iframe预览URL
 */
export const buildIframePreviewUrlWithCache = (deployKey?: string | null): string => {
  const baseUrl = buildIframePreviewUrl(deployKey)
  if (!baseUrl) return ''

  const separator = baseUrl.includes('?') ? '&' : '?'
  const finalUrl = `${baseUrl}${separator}t=${new Date().getTime()}`
  console.log('🎯 Final iframe URL with cache:', finalUrl)
  return finalUrl
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
