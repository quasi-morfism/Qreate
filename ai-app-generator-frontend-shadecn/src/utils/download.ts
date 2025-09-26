import { downloadAppCode as apiDownloadAppCode } from '@/api/appController'

export const downloadAppCode = async (appId: string | number): Promise<boolean> => {
  try {
    // Use string directly - Spring Boot will handle the conversion automatically
    const stringAppId = appId.toString()
    console.log('🔍 Downloading app with ID:', appId, 'as string:', stringAppId)

    const response = await apiDownloadAppCode({ appId: stringAppId }, { responseType: 'blob' })

    if (!response.data) {
      throw new Error('下载失败：未收到文件数据')
    }

    let filename = `app_${appId}.zip`

    const contentDisposition = response.headers?.['content-disposition']
    if (contentDisposition) {
      const filenameMatch = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/)
      if (filenameMatch && filenameMatch[1]) {
        filename = filenameMatch[1].replace(/['"]/g, '')
        if (!filename.endsWith('.zip')) {
          filename += '.zip'
        }
      }
    }

    const blob = new Blob([response.data], { type: 'application/zip' })
    const downloadUrl = window.URL.createObjectURL(blob)

    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = filename
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)

    window.URL.revokeObjectURL(downloadUrl)

    return true
  } catch (error: any) {
    console.error('Download error:', error)

    if (error.response?.status === 401) {
      throw new Error('请先登录后再下载')
    } else if (error.response?.status === 403) {
      throw new Error('无权限下载该应用代码')
    } else if (error.response?.status === 404) {
      throw new Error('应用代码不存在，请先生成代码')
    } else if (error.response?.data?.message) {
      throw new Error(error.response.data.message)
    } else {
      throw new Error(error.message || '下载失败')
    }
  }
}
