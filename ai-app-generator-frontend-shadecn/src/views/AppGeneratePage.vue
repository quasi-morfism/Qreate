<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { marked } from 'marked'
import { getAppVoById, deployApp } from '@/api/appController'
import { listAppChatHistory } from '@/api/chatHistoryController'
import myAxios from '@/request'
import { useLoginUserStore } from '@/stores/loginUser'
import GlobalHeader from '@/components/layouts/GlobalHeader.vue'
import { LoadingSpinner, UserAvatar } from '@/components/common'
import { useMessage, buildPreviewUrlWithCache } from '@/utils'

const route = useRoute()
const router = useRouter()
const message = useMessage()
const loginUserStore = useLoginUserStore()

// Simple marked configuration without highlighting
marked.setOptions({
  breaks: true,
  gfm: true,
})

// State
const appId = ref<string>(route.params.id as string)
const storageKey = ref<string>('')
const app = ref<API.BaseResponseAppVO>({} as API.BaseResponseAppVO)
const loading = ref(true)
const messages = ref<Array<any>>([])
const newMessage = ref('')
const isGenerating = ref(false)
const isDeploying = ref(false)
const previewUrl = ref('')
const chatContainer = ref<HTMLElement>()
const eventSource = ref<EventSource | null>(null)

// Chat History State
const historyLoading = ref(false)
const hasMoreHistory = ref(false)
const lastCreateTime = ref<string | undefined>()
const PAGE_SIZE = 10

// Debounce function
const debounce = (func: Function, delay: number) => {
  let timeoutId: ReturnType<typeof setTimeout>
  return (...args: any[]) => {
    clearTimeout(timeoutId)
    timeoutId = setTimeout(() => {
      func(...args)
    }, delay)
  }
}

type ErrorWithResponse = {
  response?: { data?: { message?: string, code?: number } }
  message?: string
}

// Use adapt format from query (provided by Home page)
const codeFormat = ref<'html' | 'multi_file' | 'vue_project'>(
  (route.query.adapt as 'html' | 'multi_file' | 'vue_project') || 'html',
)

// Persist and restore chat state (only for draft message now)
type PersistedChatState = {
  newMessage: string
  codeFormat?: 'html' | 'multi_file' | 'vue_project'
}

const saveChatState = () => {
  try {
    if (!storageKey.value) return
    const state: PersistedChatState = {
      newMessage: newMessage.value,
      codeFormat: codeFormat.value,
    }
    localStorage.setItem(storageKey.value, JSON.stringify(state))
  } catch {}
}

const loadChatState = (): boolean => {
  try {
    if (!storageKey.value) return false
    const raw = localStorage.getItem(storageKey.value)
    if (!raw) return false
    const state = JSON.parse(raw) as PersistedChatState
    if (typeof state?.newMessage === 'string') {
      newMessage.value = state.newMessage
    }
    if (state?.codeFormat) {
      codeFormat.value = state.codeFormat
    }
    return true
  } catch {
    return false
  }
}
// Load chat history
const loadChatHistory = async (cursor?: string) => {
  if (historyLoading.value) return
  historyLoading.value = true

  const container = chatContainer.value
  const oldScrollHeight = container?.scrollHeight || 0

  // Enforce a minimum display time for the spinner to avoid flickering
  const minSpinnerTime = new Promise((resolve) => setTimeout(resolve, 300))
  const historyPromise = listAppChatHistory({
    appId: safeParseId(appId.value),
    pageSize: PAGE_SIZE,
    lastCreateTime: cursor,
  })

  try {
    const [res] = await Promise.all([historyPromise, minSpinnerTime])

    if (res.data?.code === 0 && res.data.data?.records) {
      const rawRecords = res.data.data.records

      // Update cursor with the oldest message's time BEFORE reversing for display
      if (rawRecords.length > 0) {
        // API returns newest first, so the last item is the oldest and becomes the cursor
        lastCreateTime.value = rawRecords[rawRecords.length - 1].createTime
      }
      hasMoreHistory.value = rawRecords.length === PAGE_SIZE

      // Reverse the records to display in ascending order (oldest first)
      const historyRecords = rawRecords.reverse().map((item) => {
        const message = {
          role: item.messageType === 'user' ? 'user' : 'assistant',
          content: item.message || '',
          timestamp: new Date(item.createTime as string).getTime(),
        }

        // Process tool calls for assistant messages
        if (message.role === 'assistant' && message.content) {
          // Parse tool calls from history
          const toolCalls = parseHistoryToolCalls(message.content)
          message.toolCalls = toolCalls
          
          // Remove tool call markers from display content
          let cleanContent = message.content
          toolCalls.forEach(toolCall => {
            cleanContent = cleanContent.replace(toolCall.fullMatch, '')
          })
          message.content = cleanContent.trim()
        }

        return message
      })

      // Prepend the chronologically sorted older messages
      messages.value = [...historyRecords, ...messages.value]

      // Preserve scroll position after loading more history
      if (cursor && container) {
        await nextTick()
        const newScrollHeight = container.scrollHeight
        container.scrollTop = newScrollHeight - oldScrollHeight
      }
    }
  } catch (error) {
    console.error('Failed to load chat history:', error)
    message.error('Failed to load chat history')
  } finally {
    historyLoading.value = false
  }
}

// Load app data
const loadApp = async () => {
  try {
    // Ensure login status is fetched and confirmed before proceeding
    await loginUserStore.fetchLoginUser()

    if (!loginUserStore.loginUser.id) {
      message.error('Please log in to access this page')
      router.push('/')
      return
    }

    console.log('🔍 Requesting app with ID:', appId.value, 'type:', typeof appId.value)
    const response = await getAppVoById({ id: safeParseId(appId.value) })
    const backend = response.data
    console.log('🔍 Backend response:', backend)
    if (backend?.code !== 0) {
      message.error(`Failed to load app: ${backend?.message || 'System Error'}`)
      return
    }
    const appData = backend?.data
    console.log('🔍 App data:', appData)
    app.value = backend

    // Initialize storage key for this app
    storageKey.value = `gen_chat_${appId.value}`
    // Restore draft message
    loadChatState()

    // Load initial chat history
    await loadChatHistory()

    // Scroll to bottom after initial load to show the latest messages
    scrollToBottom()

    // Auto-send initPrompt only if auto=1 from home page AND no existing messages
    const shouldAutoSend = route.query.auto === '1' && messages.value.length === 0

    if (shouldAutoSend && appData?.initPrompt) {
      newMessage.value = appData.initPrompt
      // small delay to ensure UI is ready
      setTimeout(() => sendMessage(), 250)
    }

    // Set preview URL if app is deployed and has history
    if (appData?.deployKey && messages.value.length >= 2) {
      // Add timestamp to bust cache and force reload
      previewUrl.value = `${buildPreviewUrlWithCache(appData.deployKey)}?t=${new Date().getTime()}`
    }
  } catch (error) {
    console.error('Failed to load app:', error)
    message.error('Failed to load app')
  } finally {
    loading.value = false
  }
}

// Send message
const sendMessage = async () => {
  if (!newMessage.value.trim() || isGenerating.value) {
    return
  }

  if (!loginUserStore.loginUser.id) {
    message.error('Please log in to use the AI chat feature')
    router.push('/')
    return
  }

  const userMessage = {
    role: 'user' as const,
    content: newMessage.value.trim(),
    timestamp: Date.now(),
  }
  messages.value.push(userMessage)
  const currentMessage = newMessage.value
  newMessage.value = ''

  const assistantMessage = {
    role: 'assistant' as const,
    content: '',
    timestamp: Date.now(),
  }
  messages.value.push(assistantMessage)
  const assistantMessageTimestamp = assistantMessage.timestamp
  saveChatState()

  isGenerating.value = true

  // Close previous connection if it exists
  if (eventSource.value) {
    eventSource.value.close()
  }

  const baseURL =
    myAxios.defaults.baseURL || import.meta.env.VITE_API_BASE_URL || 'http://localhost:8100/api'
  const params = new URLSearchParams({
    appId: appId.value,
    message: currentMessage,
    adapt: codeFormat.value,
  })
  const url = `${baseURL}/app/chat/gen/code?${params}`

  eventSource.value = new EventSource(url, { withCredentials: true })

  eventSource.value.onmessage = (event: MessageEvent) => {
    const currentAssistantMessage = messages.value.find(
      (m) => m.role === 'assistant' && m.timestamp === assistantMessageTimestamp,
    )

    try {
      const data = JSON.parse(event.data)
      const newContent = data.q
      if (newContent && currentAssistantMessage) {
        // Process streaming content with bracket detection
        const displayContent = processStreamingContent(newContent, currentAssistantMessage)
        
        // Add display content to message
        if (displayContent) {
          currentAssistantMessage.content += displayContent
        }
        
        // Update tool calls
        currentAssistantMessage.toolCalls = [...pendingToolCalls.value]
        
        scrollToBottom()
      }
    } catch (e) {
      console.error('sse data parse error', e)
    }
  }

  eventSource.value.addEventListener('done', () => {
    console.log("SSE stream finished with 'done' event. Closing connection.")
    
    // Process any remaining buffer content
    const currentAssistantMessage = messages.value.find(
      (m) => m.role === 'assistant' && m.timestamp === assistantMessageTimestamp,
    )
    if (currentAssistantMessage && streamingBuffer.value) {
      currentAssistantMessage.content += streamingBuffer.value
    }
    
    // Clear streaming state
    streamingBuffer.value = ''
    pendingToolCalls.value = []
    
    isGenerating.value = false
    eventSource.value?.close()
    eventSource.value = null

    // For Vue projects, wait longer to allow build process to complete
    if (codeFormat.value === 'vue_project') {
      console.log('Vue project detected, waiting 10 seconds for build to complete...')
      message.success('Vue project generation complete. Building and deploying in 10 seconds...')
      setTimeout(() => handleDeploy(), 10000) // Wait 10 seconds for Vue build
    } else {
      console.log('Triggering deployment via handleDeploy() in 1 second...')
      message.success('AI code generation complete. Deploying application in 1 second...')
      setTimeout(() => handleDeploy(), 1000)
    }
  })

  eventSource.value.onerror = (err: Event) => {
    console.error('EventSource failed:', err)
    message.error('连接异常，请检查网络或联系管理员。')
    if (assistantMessage) {
      // The original code had `assistantMessage.isError = true` which is not defined.
      // Assuming it was meant to be `assistantMessage.content = 'Error connecting to the server. Please check your connection and try again.'`
      // or similar, but the original code had `isError` which is not a property of the message object.
      // I will remove the line as it's not part of the original file's state.
    }
    isGenerating.value = false
    eventSource.value?.close()
    eventSource.value = null
    saveChatState()
  }
}

const openInNewTab = (url: string | null) => {
  if (url) {
    window.open(url, '_blank')
  }
}

// Deploy app
const handleDeploy = async (): Promise<boolean> => {
  if (!appId.value) {
    message.error('应用ID不存在，无法部署')
    return false
  }
  isDeploying.value = true
  console.log(`🚀 Starting deployment for app ID: ${appId.value}`)
  try {
    // The parameter should be { appId: appId.value } according to the type definition
    const res = await deployApp({ appId: safeParseId(appId.value) })
    console.log('📦 Deploy response received (axios response):', res)

    // The backend's BaseResponse is nested in res.data
    const backendResponse = res.data
    console.log('🔍 Backend response:', backendResponse)

    if (backendResponse && backendResponse.code === 0 && typeof backendResponse.data === 'string') {
      const deployUrl = backendResponse.data
      const finalUrl = `${deployUrl}?t=${new Date().getTime()}`

      console.log('✅ Deploy successful! URL:', deployUrl)
      console.log('🔗 Final preview URL:', finalUrl)

      previewUrl.value = finalUrl
      message.success('应用部署成功！')

      if (app.value?.data) {
        const urlParts = deployUrl.split('/').filter(Boolean)
        const deployKey = urlParts[urlParts.length - 1]
        app.value.data.deployKey = deployKey
        console.log('💾 Updated deployKey:', deployKey)
      }
      return true
    } else {
      console.error(
        'Deployment failed: backendResponse.data is not a string.',
        'Received (backend response):',
        backendResponse,
      )

      // Special handling for Vue projects that might need more build time
      if (codeFormat.value === 'vue_project' && backendResponse.message?.includes('Application code does not exist')) {
        console.log('🔄 Vue project build might still be in progress, will retry in 15 seconds...')
        message.warning('Vue项目可能还在构建中，15秒后自动重试部署...')
        setTimeout(() => {
          console.log('🔄 Retrying Vue project deployment...')
          handleDeploy()
        }, 15000)
        return false
      }

      message.error(`部署失败: ${backendResponse.message || '后端未返回有效的部署URL。'}`)
      return false
    }
  } catch (error: unknown) {
    console.error('Deploy request failed:', error)
    const e = error as ErrorWithResponse
    const errorMessage = e.response?.data?.message || e.message || '未知网络错误'

    // 特殊处理认证错误
    if (e.response?.data?.code === 40100) {
      message.error('请先登录后再尝试部署')
      // 可以考虑跳转到登录页面或显示登录弹窗
    } else if (e.response?.data?.code === 40300) {
      message.error('没有权限部署此应用')
    } else {
      message.error(`部署请求失败: ${errorMessage}`)
    }

    console.log('🔍 Full error response:', e.response?.data)
    return false
  } finally {
    isDeploying.value = false
  }
}

// Scroll to bottom
const scrollToBottom = () => {
  nextTick(() => {
    if (chatContainer.value) {
      chatContainer.value.scrollTop = chatContainer.value.scrollHeight
    }
  })
}

// Handle scroll to top to load more history
const handleHistoryScroll = async () => {
  if (chatContainer.value?.scrollTop === 0 && hasMoreHistory.value && !historyLoading.value) {
    await loadChatHistory(lastCreateTime.value)
  }
}
const debouncedHandleHistoryScroll = debounce(handleHistoryScroll, 100)

// Format time
const formatTime = (timestamp: number) => {
  return new Date(timestamp).toLocaleTimeString()
}

// Get tool display name
const getToolDisplayName = (toolType: string) => {
  const displayNames = {
    'FILE_WRITE_SUCCESS': 'File written',
    'FILE_WRITE_FAILED': 'Write failed',
    'GENERATION_COMPLETE': 'Complete'
  }
  return displayNames[toolType] || toolType
}

// Safe ID conversion for large integers
const safeParseId = (idStr: string): any => {
  const parsed = parseInt(idStr)
  if (Number.isSafeInteger(parsed)) {
    return parsed
  } else {
    console.log('⚠️ Using string ID due to large integer:', idStr)
    return idStr as any // Cast to any to bypass TypeScript type checking
  }
}


// Streaming content buffer to handle partial tool calls
const streamingBuffer = ref('')
const pendingToolCalls = ref([])

// Process streaming content with bracket detection
const processStreamingContent = (newContent: string, currentAssistantMessage: any) => {
  streamingBuffer.value += newContent
  let displayContent = ''
  let buffer = streamingBuffer.value
  
  // Look for complete tool call patterns
  const toolCallRegex = /\[(FILE_WRITE_SUCCESS|FILE_WRITE_FAILED|GENERATION_COMPLETE)(?::([^\]]*))?\]/g
  let lastProcessedIndex = 0
  let match
  
  while ((match = toolCallRegex.exec(buffer)) !== null) {
    // Add content before the tool call to display
    displayContent += buffer.substring(lastProcessedIndex, match.index)
    
    // Extract tool call info
    const [fullMatch, toolType, fileName] = match
    const toolCall = {
      type: toolType,
      fileName: fileName ? fileName.trim() : null,
      fullMatch: fullMatch
    }
    
    // Add to pending tool calls
    if (!pendingToolCalls.value.some(tc => tc.fullMatch === fullMatch)) {
      pendingToolCalls.value.push(toolCall)
    }
    
    lastProcessedIndex = match.index + fullMatch.length
  }
  
  // Check if there's an incomplete bracket sequence at the end
  const remainingBuffer = buffer.substring(lastProcessedIndex)
  const openBracketIndex = remainingBuffer.lastIndexOf('[')
  
  if (openBracketIndex !== -1) {
    // There might be an incomplete tool call, don't display from the '[' onwards
    displayContent += remainingBuffer.substring(0, openBracketIndex)
    streamingBuffer.value = '[' + remainingBuffer.substring(openBracketIndex + 1)
  } else {
    // No incomplete brackets, display everything
    displayContent += remainingBuffer
    streamingBuffer.value = ''
  }
  
  return displayContent
}

// Simple tool call parsing for history messages
const parseHistoryToolCalls = (content: string) => {
  const toolCalls = []
  const toolCallRegex = /\[(FILE_WRITE_SUCCESS|FILE_WRITE_FAILED|GENERATION_COMPLETE)(?::([^\]]*))?\]/g
  let match
  
  while ((match = toolCallRegex.exec(content)) !== null) {
    const [fullMatch, toolType, fileName] = match
    toolCalls.push({
      type: toolType,
      fileName: fileName ? fileName.trim() : null,
      fullMatch: fullMatch
    })
  }
  
  return toolCalls
}

// Load app on mount

onMounted(() => {
  console.log('🎯 Component mounted, starting loadApp()')
  loadApp()
  chatContainer.value?.addEventListener('scroll', debouncedHandleHistoryScroll)
  window.addEventListener('beforeunload', saveChatState)
})

onUnmounted(() => {
  if (eventSource.value) {
    eventSource.value.close()
  }
  chatContainer.value?.removeEventListener('scroll', debouncedHandleHistoryScroll)
  window.removeEventListener('beforeunload', saveChatState)
})

// Persist on changes
watch(newMessage, saveChatState)
watch(codeFormat, saveChatState)
</script>

<template>
  <div class="app-generate-page">
    <GlobalHeader />
    <!-- Main Content -->
    <div class="main-content">
      <!-- Chat Section -->
      <div class="chat-section">
        <div class="chat-messages" ref="chatContainer">
          <!-- History Loading Spinner with Transition -->
          <Transition name="fade">
            <div v-if="historyLoading" class="history-loading-spinner">
              <LoadingSpinner />
            </div>
          </Transition>

          <!-- Initial Welcome Message -->
          <div v-if="messages.length === 0 && !isGenerating" class="welcome-message">
            <div class="w-14 h-14 rounded-full overflow-hidden border">
              <img
                :src="app.data?.cover || '/logo.png'"
                alt="cover"
                class="w-full h-full object-cover"
              />
            </div>
            <h2>{{ app.data?.appName || 'AI App Generator' }}</h2>
            <p v-if="app.data?.initPrompt">{{ app.data.initPrompt }}</p>
            <p v-else>Start building your application by typing in the box below.</p>
          </div>

          <div v-for="(message, index) in messages" :key="index" :class="['message', message.role]">
            <div class="message-avatar">
              <UserAvatar
                v-if="message.role === 'user'"
                :user-avatar="loginUserStore.loginUser.userAvatar"
                :user-id="loginUserStore.loginUser.id"
                :user-name="loginUserStore.loginUser.userName"
                size="md"
                show-border
              />
              <div v-else class="ai-avatar">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="#18a058">
                  <path
                    d="M12 2a2 2 0 0 0-2 2v1H8a4 4 0 0 0-4 4v6a4 4 0 0 0 4 4h8a4 4 0 0 0 4-4V9a4 4 0 0 0-4-4h-2V4a2 2 0 0 0-2-2zm-3 9a1 1 0 1 1 0-2 1 1 0 0 1 0 2zm6 0a1 1 0 1 1 0-2 1 1 0 0 1 0 2z"
                  />
                </svg>
              </div>
            </div>
            <div class="message-content">
              <!-- Message Content -->
              <div
                class="message-text"
                v-if="message.content"
                v-html="marked(message.content)"
              ></div>
              <div
                v-else-if="message.role === 'assistant' && isGenerating"
                :class="['generating-content', { 'vue-project': codeFormat === 'vue_project' }]"
              >
                <span class="typewriter-text">
                  {{ codeFormat === 'vue_project' ? 'Building Vue project...' : 'Synthesizing...' }}
                </span>
              </div>

              <!-- Tool Calls Display -->
              <div v-if="message.role === 'assistant' && message.toolCalls?.length > 0" class="inline-tool-calls">
                <div v-for="(toolCall, idx) in message.toolCalls" :key="idx" class="inline-tool-item">
                  <svg v-if="toolCall.type === 'FILE_WRITE_SUCCESS'" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="12" height="12" fill="currentColor" class="inline-tool-icon success">
                    <path d="M9,20.42L2.79,14.21L5.62,11.38L9,14.77L18.88,4.88L21.71,7.71L9,20.42Z" />
                  </svg>
                  <svg v-else-if="toolCall.type === 'FILE_WRITE_FAILED'" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="12" height="12" fill="currentColor" class="inline-tool-icon error">
                    <path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z" />
                  </svg>
                  <svg v-else xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="12" height="12" fill="currentColor" class="inline-tool-icon complete">
                    <path d="M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2M12,17A5,5 0 0,1 7,12A5,5 0 0,1 12,7A5,5 0 0,1 17,12A5,5 0 0,1 12,17Z" />
                  </svg>
                  <span class="inline-tool-text">{{ toolCall.fileName || getToolDisplayName(toolCall.type) }}</span>
                </div>
              </div>

              <div class="message-time">{{ formatTime(message.timestamp) }}</div>
            </div>
          </div>
        </div>
        <!-- Chat Input Area -->
        <div class="chat-input-area">
          <div class="input-wrapper">
            <!-- format is controlled from Home page. No selector here. -->
            <textarea
              v-model="newMessage"
              rows="1"
              placeholder="Describe the app you want to build..."
              class="chat-input"
              @keydown.enter.prevent="sendMessage"
            />
            <button
              :disabled="isGenerating || !newMessage.trim()"
              @click="sendMessage"
              class="send-button"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                viewBox="0 0 24 24"
                width="18"
                height="18"
                fill="currentColor"
              >
                <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z" />
              </svg>
            </button>
          </div>
        </div>
      </div>

      <!-- Preview Section -->
      <div class="preview-section">
        <div class="preview-header">
          <div style="font-weight: 700; font-size: 16px">Live Preview</div>
          <div class="preview-header-actions">
            <a
              :href="previewUrl || '#'"
              target="_blank"
              @click.prevent="openInNewTab(previewUrl)"
              class="text-sm text-emerald-700 disabled:opacity-50 link-button"
              :class="{ 'pointer-events-none opacity-50': !previewUrl }"
              >Open in New Tab</a
            >
            <button
              class="px-3 py-1 rounded-md bg-emerald-600 text-white text-sm disabled:opacity-50"
              :disabled="isDeploying"
              @click="handleDeploy"
            >
              {{ app.data?.deployKey ? 'Deploy' : 'Deploy' }}
            </button>
          </div>
        </div>
        <!-- Preview URL Debug Display -->

        <!-- Live Preview Area -->
        <div class="preview-content">
          <template v-if="previewUrl">
            <iframe
              :src="previewUrl"
              frameborder="0"
              class="preview-iframe"
              @load="console.log('🖼️ Iframe loaded successfully:', previewUrl)"
              @error="console.log('❌ Iframe failed to load:', previewUrl)"
            ></iframe>
          </template>
          <div v-else class="preview-placeholder">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              viewBox="0 0 24 24"
              width="48"
              height="48"
              fill="#ccc"
            >
              <path
                d="M19 3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V5a2 2 0 0 0-2-2m-5 14H7v-2h7zm3-4H7v-2h10zm0-4H7V7h10z"
              />
            </svg>
            <p>Your generated app will appear here</p>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<style scoped>
.app-generate-page {
  min-height: 100vh;
  width: 100%;
  display: flex;
  flex-direction: column;
}

.main-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  height: calc(100vh - 64px); /* header 64px */
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
  padding: 24px;
}

.chat-section,
.preview-section {
  background-color: white;
  border: 1px solid #efefef;
  border-radius: 20px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  height: 100%; /* Make section fill the grid cell height */
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.04);
  position: relative;
}

.welcome-message {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 48px;
  color: #666;
  height: 100%;
}

.welcome-message h2 {
  font-size: 24px;
  font-weight: 600;
  margin: 16px 0 8px;
  color: #333;
}

.welcome-message p {
  font-size: 16px;
  max-width: 400px;
}

.chat-messages {
  flex-grow: 1;
  overflow-y: auto;
  padding: 24px;
  padding-bottom: 120px; /* space for floating input */
  display: flex;
  flex-direction: column;
  gap: 24px;
  border-radius: 20px 20px 0 0; /* Match parent's top corners */
}

.message {
  display: flex;
  gap: 16px;
  max-width: 80%;
  align-self: flex-start;
  width: auto;
}

.message.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
}

.ai-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #f0fdf4, #dcfce7);
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid rgba(24, 160, 88, 0.2);
  box-shadow: 0 2px 8px rgba(24, 160, 88, 0.15);
}

.message-content {
  background: var(--color-background-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-2xl);
  padding: var(--space-4) var(--space-5);
  max-width: 100%;
  overflow-wrap: break-word;
  word-wrap: break-word;
  min-width: 0;
  box-shadow: var(--shadow-xs);
  transition: all var(--transition-normal);
  animation: fadeInUp 0.3s ease-out;
}

.message.user .message-content {
  background: linear-gradient(135deg, var(--color-primary-500), var(--color-primary-600));
  border: 1px solid var(--color-primary-400);
  color: var(--color-text-inverse);
  box-shadow: var(--shadow-sm);
}

.message-text {
  font-size: 15px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.message-text.has-file-list-above {
  margin-top: var(--space-2);
}

/* Hover effects for modern feel */
.message-content:hover {
  transform: translateY(-1px);
  box-shadow: var(--shadow-sm);
  border-color: var(--color-border-strong);
}

.message.user .message-content:hover {
  background: linear-gradient(135deg, var(--color-primary-600), var(--color-primary-700));
  border-color: var(--color-primary-500);
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}

.message-time {
  font-size: 11px;
  color: rgba(0, 0, 0, 0.4);
  margin-top: 8px;
  text-align: right;
  font-weight: 500;
  opacity: 0.8;
  transition: opacity 0.2s ease;
}

.message.user .message-time {
  text-align: left;
  color: rgba(255, 255, 255, 0.8) !important;
}

:deep(.message-text pre) {
  background: linear-gradient(135deg, #0d1117, #161b22);
  color: #e6edf3;
  padding: 16px 20px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.05);
  overflow-x: auto;
  font-family: 'SF Mono', 'Monaco', 'Cascadia Code', 'Roboto Mono', 'Courier New', monospace;
  white-space: pre;
  word-break: normal;
  line-height: 1.45;
  max-width: 100%;
  width: 0;
  min-width: 100%;
  box-sizing: border-box;
  box-shadow:
    inset 0 2px 8px rgba(0, 0, 0, 0.4),
    inset 0 1px 4px rgba(0, 0, 0, 0.2);
  backdrop-filter: blur(10px);
  margin: 12px 0;
}

:deep(.message-text code) {
  font-family: 'SF Mono', 'Monaco', 'Cascadia Code', 'Roboto Mono', 'Courier New', monospace;
  background: rgba(0, 0, 0, 0.08);
  padding: 2px 6px;
  border-radius: 6px;
  font-size: 13px;
  border: 1px solid rgba(0, 0, 0, 0.1);
}

/* Inline code in user messages */
.message.user :deep(.message-text code) {
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: rgba(255, 255, 255, 0.95);
}

/* Reduce heading sizes in AI messages */
:deep(.message-text h1) {
  font-size: 1.25em;
  margin-top: 0.5em;
  margin-bottom: 0.3em;
  font-weight: 600;
  line-height: 1.3;
}

:deep(.message-text h2) {
  font-size: 1.15em;
  margin-top: 0.5em;
  margin-bottom: 0.25em;
  font-weight: 600;
  line-height: 1.3;
}

:deep(.message-text h3) {
  font-size: 1.05em;
  margin-top: 0.4em;
  margin-bottom: 0.2em;
  font-weight: 600;
  line-height: 1.3;
}

:deep(.message-text h4) {
  font-size: 1em;
  margin-top: 0.4em;
  margin-bottom: 0.2em;
  font-weight: 600;
  line-height: 1.3;
}

.generating-content {
  display: flex;
  align-items: center;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  padding: var(--space-3);
  background: var(--color-background-secondary);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border-light);
  animation: pulse 2s ease-in-out infinite;
}

.typewriter-text {
  color: var(--color-text-secondary);
  overflow: hidden;
  border-right: 2px solid var(--color-primary-500);
  white-space: nowrap;
  animation:
    typewriter 2s steps(15, end) infinite,
    blink 1s step-end infinite;
}

@keyframes typewriter {
  0% {
    width: 0;
  }
  50% {
    width: 100%;
  }
  100% {
    width: 100%;
  }
}

@keyframes blink {
  0%,
  50% {
    border-color: #18a058;
  }
  51%,
  100% {
    border-color: transparent;
  }
}

/* New Minimalist Chat Input Styling */
.chat-input-area {
  position: absolute;
  left: 16px;
  right: 16px;
  bottom: 16px;
  padding: 0;
  border-top: none;
  background-color: transparent;
}

.input-wrapper {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  background: var(--color-background-glass);
  -webkit-backdrop-filter: blur(8px) saturate(120%);
  backdrop-filter: blur(8px) saturate(120%);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-xl);
  padding: var(--space-2) var(--space-3);
  box-shadow: var(--shadow-sm);
  transition: all var(--transition-normal);
  width: 100%;
  position: relative;
  overflow: hidden;
}

.input-wrapper::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: left 0.5s ease;
}

.input-wrapper:hover::before {
  left: 100%;
}

.input-wrapper:focus-within {
  background: var(--color-background);
  border-color: var(--color-primary-500);
  box-shadow:
    var(--shadow-md),
    0 0 0 3px rgba(34, 197, 94, 0.1);
  transform: translateY(-1px);
}

.chat-input {
  flex-grow: 1;
  margin: 0;
  padding: 8px 10px;
  min-height: 44px;
  max-height: 160px;
  overflow-y: auto;
  border: none;
  outline: none;
  resize: none;
  background-color: transparent;
  font-size: 15px;
  line-height: 1.6;
  color: #111827;
  caret-color: #10b981;
}
.chat-input::placeholder {
  color: #9aa0a6;
}

.send-button {
  width: 42px;
  height: 42px;
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-full);
  border: 1px solid var(--color-border);
  background: var(--color-background);
  color: var(--color-primary-600);
  transition: all var(--transition-normal);
  position: relative;
  overflow: hidden;
}

.send-button::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 0;
  height: 0;
  background: var(--color-primary-100);
  border-radius: var(--radius-full);
  transition: all var(--transition-normal);
  transform: translate(-50%, -50%);
}

.send-button:hover {
  background: var(--color-primary-50);
  border-color: var(--color-primary-500);
  transform: scale(1.05);
  box-shadow: var(--shadow-md);
}

.send-button:hover::before {
  width: 100%;
  height: 100%;
}

.send-button:active {
  transform: scale(0.95);
}

.send-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

.send-button:disabled:hover {
  transform: none;
  background: var(--color-background);
  border-color: var(--color-border);
}

.preview-header {
  padding: 12px 16px;
  border-bottom: 1px solid #efefef;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
  border-radius: 20px 20px 0 0; /* Match parent's top corners */
}

.preview-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.preview-header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.preview-header-actions :deep(.n-button--text-type) {
  border-radius: 20px;
}

.preview-header-actions :deep(.n-button--primary-type) {
  border-radius: 20px;
  box-shadow: 0 6px 16px rgba(24, 160, 88, 0.22);
}

.preview-content {
  flex-grow: 1;
  position: relative;
  border-radius: 0 0 20px 20px; /* Match parent's bottom corners */
  overflow: hidden; /* Ensure content doesn't overflow rounded corners */
}

.preview-placeholder,
.preview-generating {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #999;
}


.preview-placeholder p,
.preview-generating p {
  margin-top: 16px;
}

.preview-url-display {
  position: absolute;
  top: 0;
  left: 0;
  background-color: #f0f0f0;
  padding: 4px 8px;
  border-radius: 10px;
  font-size: 12px;
  font-family: monospace;
  z-index: 10;
  margin: 12px;
  border: 1px solid #ccc;
}

.preview-iframe-container {
  width: 100%;
  height: 100%;
  position: relative;
}

.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
  border-radius: 0 0 20px 20px; /* Match parent's bottom corners */
}

/* AI response markdown content adjustments */
.chat-message.assistant .content-inner {
  background: linear-gradient(135deg, #2f855a, #38a169);
  color: white;
  padding: 10px 15px;
  border-radius: 16px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.chat-message.assistant .content-inner :deep(h1) {
  font-size: 1.5em;
  margin-top: 0.8em;
  margin-bottom: 0.4em;
  color: white; /* Ensure headings are also white on gradient */
}
.chat-message.assistant .content-inner :deep(h2) {
  font-size: 1.3em;
  margin-top: 0.7em;
  margin-bottom: 0.35em;
  color: white;
}
.chat-message.assistant .content-inner :deep(h3) {
  font-size: 1.15em;
  margin-top: 0.6em;
  margin-bottom: 0.3em;
  color: white;
}

/* File write status in chat messages */
.message-content.has-file-writes {
  position: relative;
  overflow: hidden;
}

.message-content.has-file-writes::before {
  content: '';
  position: absolute;
  top: 0;
  left: -4px;
  width: 4px;
  height: 100%;
  background: linear-gradient(135deg, #10b981, #3b82f6);
  border-radius: 0 2px 2px 0;
  animation: file-write-glow 2s ease-in-out infinite;
}

@keyframes file-write-glow {
  0%, 100% {
    opacity: 0.6;
    transform: scaleY(1);
  }
  50% {
    opacity: 1;
    transform: scaleY(1.02);
  }
}

/* File success list styling */
.file-success-list {
  margin-bottom: var(--space-3);
  padding: var(--space-3);
  background: var(--color-background);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-inset-sm);
  animation: file-status-appear 0.5s ease-out;
}

.file-success-header {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: var(--space-2);
  font-weight: 600;
  color: var(--color-text-primary);
  font-size: var(--font-size-sm);
}

.file-icon {
  color: var(--color-primary-500);
}

.file-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.file-item {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-1) var(--space-2);
  background: var(--color-background-secondary);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  transition: all var(--transition-fast);
}

.file-item:hover {
  background: var(--color-primary-50);
  color: var(--color-primary-700);
}

.check-icon {
  color: var(--color-primary-500);
  flex-shrink: 0;
}

.file-name {
  font-family: 'Fira Code', 'Monaco', 'Consolas', monospace;
  font-size: var(--font-size-xs);
  word-break: break-all;
}


@keyframes file-status-appear {
  0% {
    opacity: 0;
    transform: translateY(10px);
  }
  100% {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 简化的内联工具调用样式 */
.inline-tool-calls {
  margin: 8px 0;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.inline-tool-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 8px;
  background: rgba(16, 185, 129, 0.1);
  border: 1px solid rgba(16, 185, 129, 0.2);
  border-radius: 8px;
  font-size: 11px;
  color: #065f46;
  animation: tool-tag-appear 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  transform-origin: left center;
}

.inline-tool-icon.success {
  color: #22c55e;
}

.inline-tool-icon.error {
  color: #ef4444;
}

.inline-tool-icon.complete {
  color: #3b82f6;
}

.inline-tool-text {
  font-family: 'Monaco', 'Consolas', monospace;
  font-size: 10px;
}


@keyframes dot-bounce {
  0%, 80%, 100% {
    background: #d1d5db;
    transform: scale(1);
  }
  40% {
    background: #10b981;
    transform: scale(1.2);
  }
}

@keyframes tool-tag-appear {
  0% {
    opacity: 0;
    transform: scale(0.8) translateX(-8px);
  }
  60% {
    transform: scale(1.05) translateX(0);
  }
  100% {
    opacity: 1;
    transform: scale(1) translateX(0);
  }
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@keyframes bounce {
  from {
    transform: translateY(0);
  }
  to {
    transform: translateY(-4px);
  }
}


</style>
