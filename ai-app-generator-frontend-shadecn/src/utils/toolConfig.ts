/**
 * Dynamic Tool Configuration Management
 * Fetches tool information from backend and automatically configures frontend processing logic
 */

export interface ToolInfo {
  toolName: string        // English name, e.g., "writeFile"
  displayName: string     // Display name, e.g., "Write File"
  operationType: string   // Operation type, e.g., "FILE_WRITE"
  successIcon: string     // Success icon SVG
  failedIcon: string      // Failed icon SVG
}

// Global tool configuration
let toolsConfig: ToolInfo[] = []

// Load tool configuration from backend
export const loadToolsConfig = async (): Promise<ToolInfo[]> => {
  try {
    const response = await fetch('/api/tool/list')
    const result = await response.json()
    if (result.code === 0) {
      toolsConfig = result.data
      console.log('Tool configuration loaded successfully:', toolsConfig)
      return toolsConfig
    } else {
      console.error('Failed to get tool configuration:', result.message)
      return []
    }
  } catch (error) {
    console.error('Error occurred while loading tool configuration:', error)
    return []
  }
}

// Get tool information by operation type
export const getToolInfoByOperation = (operationType: string): ToolInfo | null => {
  return toolsConfig.find(tool => tool.operationType === operationType) || null
}

// Dynamically generate tool call regex
export const generateToolCallRegex = (): RegExp => {
  if (toolsConfig.length === 0) {
    // If configuration is not loaded yet, use a fallback that includes GENERATION_COMPLETE
    return /\[(.*?)(?::([^\]]*))?\]/g
  }

  // Dynamically generate all possible statuses
  const allStatuses: string[] = []
  toolsConfig.forEach(tool => {
    allStatuses.push(`${tool.operationType}_SUCCESS`)
    allStatuses.push(`${tool.operationType}_FAILED`)
  })
  allStatuses.push('GENERATION_COMPLETE')

  const pattern = `\\[(${allStatuses.join('|')})(?::([^\\]]*))?\\]`
  return new RegExp(pattern, 'g')
}

// Dynamically get tool icon
export const getToolIcon = (toolType: string): string => {
  const isSuccess = toolType.includes('SUCCESS')
  const operationType = toolType.replace(/_SUCCESS|_FAILED/, '')

  const toolInfo = toolsConfig.find(tool => tool.operationType === operationType)
  if (toolInfo) {
    return isSuccess ? toolInfo.successIcon : toolInfo.failedIcon
  }

  // Fallback icon for unknown tools or GENERATION_COMPLETE
  return '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor"><circle cx="12" cy="12" r="8"/></svg>'
}

// Dynamically get tool display name
export const getToolDisplayName = (toolType: string): string => {
  if (toolType === 'GENERATION_COMPLETE') {
    return 'Generation Complete'
  }

  const isSuccess = toolType.includes('SUCCESS')
  const isFailed = toolType.includes('FAILED')
  const operationType = toolType.replace(/_SUCCESS|_FAILED/, '')

  const toolInfo = toolsConfig.find(tool => tool.operationType === operationType)
  if (toolInfo) {
    if (isSuccess) {
      return `${toolInfo.displayName} Success`
    } else if (isFailed) {
      return `${toolInfo.displayName} Failed`
    } else {
      return toolInfo.displayName
    }
  }

  // Fallback for unknown tools
  return toolType.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase())
}

// Initialize tool configuration (called when app starts)
export const initToolsConfig = async () => {
  await loadToolsConfig()
}
