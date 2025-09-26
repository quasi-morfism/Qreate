import { CodeGenTypeEnum } from './codeGenTypes'

/**
 * 获取静态预览URL
 * @param codeGenType 代码生成类型
 * @param appId 应用ID
 * @returns 静态预览URL
 */
export const getStaticPreviewUrl = (codeGenType: string, appId: string): string => {
  if (!appId) return ''

  // 构建部署key，匹配后端目录命名规则
  let deployKey = ''
  const normalized = String(codeGenType).toUpperCase()
  if (normalized === CodeGenTypeEnum.VUE_PROJECT) {
    deployKey = `vue_project_${appId}`
  } else if (normalized === CodeGenTypeEnum.HTML) {
    deployKey = `html_${appId}`
  } else {
    deployKey = `${String(codeGenType).toLowerCase()}_${appId}`
  }

  // 使用API静态资源路径，确保同源访问，避免跨域问题
  const baseUrl = `/api/static/${deployKey}/`

  // 如果是Vue项目，指向dist目录（由服务器处理index.html和路由回退）
  if (normalized === CodeGenTypeEnum.VUE_PROJECT) {
    return `${baseUrl}dist/index.html`
  }

  // 其他类型项目直接返回基础路径
  return baseUrl
}

/**
 * 获取带缓存清除参数的静态预览URL
 * @param codeGenType 代码生成类型
 * @param appId 应用ID
 * @returns 带缓存清除参数的静态预览URL
 */
export const getStaticPreviewUrlWithCache = (codeGenType: string, appId: string): string => {
  const baseUrl = getStaticPreviewUrl(codeGenType, appId)
  if (!baseUrl) return ''

  const separator = baseUrl.includes('?') ? '&' : '?'
  return `${baseUrl}${separator}t=${new Date().getTime()}`
}
