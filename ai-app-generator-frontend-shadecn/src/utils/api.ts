import { useMessage } from './message'

export interface ApiResponse<T = any> {
  code: number
  data: T
  message: string
}

export interface ApiHandlerOptions {
  successMessage?: string
  errorMessage?: string
  showSuccessMessage?: boolean
  showErrorMessage?: boolean
}

/**
 * 通用API调用处理器
 */
export const createApiHandler = <T>(
  apiCall: () => Promise<{ data: ApiResponse<T> }>,
  options: ApiHandlerOptions = {}
) => {
  const {
    successMessage,
    errorMessage = 'Operation failed',
    showSuccessMessage = false,
    showErrorMessage = true
  } = options
  
  const message = useMessage()
  
  return async (): Promise<{ success: boolean; data?: T }> => {
    try {
      const response = await apiCall()
      const result = response.data
      
      if (result?.code === 0) {
        if (showSuccessMessage && successMessage) {
          message.success(successMessage)
        }
        return { success: true, data: result.data }
      } else {
        if (showErrorMessage) {
          message.error(result?.message || errorMessage)
        }
        return { success: false }
      }
    } catch (error) {
      console.error('API call failed:', error)
      if (showErrorMessage) {
        const errorMsg = (error as any)?.response?.data?.message || errorMessage
        message.error(errorMsg)
      }
      return { success: false }
    }
  }
}

/**
 * 批量处理API调用
 */
export const handleBatchApi = async <T>(
  apiCalls: Array<() => Promise<{ data: ApiResponse<T> }>>,
  options: ApiHandlerOptions = {}
): Promise<Array<{ success: boolean; data?: T }>> => {
  const handlers = apiCalls.map(call => createApiHandler(call, options))
  return Promise.all(handlers.map(handler => handler()))
}