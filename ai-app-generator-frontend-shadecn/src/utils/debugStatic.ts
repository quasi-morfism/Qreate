/**
 * 静态文件调试工具
 */

/**
 * 列出所有可用的deployKey
 */
export const listAvailableDeployKeys = async () => {
  try {
    // 调用后端API获取所有应用
    const response = await fetch('/api/app/list/page', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        current: 1,
        pageSize: 100,
        sortField: 'createTime',
        sortOrder: 'desc'
      })
    })

    const result = await response.json()
    if (result.code === 0 && result.data?.records) {
      const deployKeys = result.data.records
        .filter((app: any) => app.deployKey)
        .map((app: any) => ({
          id: app.id,
          appName: app.appName,
          codeGenType: app.codeGenType,
          deployKey: app.deployKey,
          staticUrl: `/api/static/${app.deployKey}/`
        }))

      console.log('📋 Available deploy keys:', deployKeys)
      return deployKeys
    }
  } catch (error) {
    console.error('❌ Failed to fetch deploy keys:', error)
  }
  return []
}

/**
 * 测试静态文件访问
 */
export const testStaticFileAccess = async (deployKey: string) => {
  const url = `/api/static/${deployKey}/`
  console.log(`🔍 Testing static file access: ${url}`)

  try {
    const response = await fetch(url)
    console.log(`📊 Response status: ${response.status} ${response.statusText}`)
    console.log(`📊 Response headers:`, Object.fromEntries(response.headers.entries()))

    if (response.ok) {
      const contentType = response.headers.get('content-type')
      if (contentType?.includes('text/html')) {
        const html = await response.text()
        console.log(`✅ HTML content preview (first 200 chars):`, html.substring(0, 200))
        return { success: true, contentType, preview: html.substring(0, 200) }
      } else {
        console.log(`✅ Non-HTML content type: ${contentType}`)
        return { success: true, contentType }
      }
    } else {
      const errorText = await response.text()
      console.log(`❌ Error response:`, errorText)
      return { success: false, status: response.status, error: errorText }
    }
  } catch (error) {
    console.error(`❌ Network error:`, error)
    return { success: false, error: String(error) }
  }
}

/**
 * 自动调试当前页面的所有静态资源
 */
export const autoDebugStaticResources = async () => {
  console.log('🚀 Starting automatic static resource debug...')

  const deployKeys = await listAvailableDeployKeys()

  if (deployKeys.length === 0) {
    console.log('⚠️ No deploy keys found. Make sure you have deployed some apps.')
    return
  }

  console.log(`🔍 Found ${deployKeys.length} deploy keys, testing each...`)

  for (const item of deployKeys.slice(0, 5)) { // 只测试前5个
    console.log(`\n🧪 Testing ${item.appName} (${item.codeGenType}):`)
    const result = await testStaticFileAccess(item.deployKey)

    if (result.success) {
      console.log(`✅ ${item.appName}: Working!`)
    } else {
      console.log(`❌ ${item.appName}: Failed - ${result.error}`)
    }

    // 添加小延迟避免请求过快
    await new Promise(resolve => setTimeout(resolve, 500))
  }

  console.log('\n🏁 Debug complete!')
}

/**
 * 在浏览器控制台中运行的调试函数
 * 使用方法：在浏览器控制台中运行 window.debugStatic()
 */
export const setupDebugConsole = () => {
  // @ts-ignore
  window.debugStatic = autoDebugStaticResources
  // @ts-ignore
  window.testStatic = testStaticFileAccess
  // @ts-ignore
  window.listDeployKeys = listAvailableDeployKeys

  console.log('🛠️ Debug functions available:')
  console.log('  - window.debugStatic() - Run full debug')
  console.log('  - window.testStatic(deployKey) - Test specific deploy key')
  console.log('  - window.listDeployKeys() - List all available keys')
}
