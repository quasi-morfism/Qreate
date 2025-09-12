<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { marked } from 'marked'
import { getAppVoById, deployApp } from '@/api/appController'
import myAxios from '@/request'
import { useLoginUserStore } from '@/stores/loginUser'
import GlobalHeader from '@/components/layouts/GlobalHeader.vue'
import { UserAvatar } from '@/components/common'
import { useMessage, getAvatarUrl, buildPreviewUrlWithCache } from '@/utils'


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
const app = ref<any>({})
const loading = ref(true)
const messages = ref<Array<{ role: 'user' | 'assistant'; content: string; timestamp: number }>>([])
const newMessage = ref('')
const isGenerating = ref(false)
const isDeploying = ref(false)
const previewUrl = ref('')
const chatContainer = ref<HTMLElement>()
const eventSource = ref<EventSource | null>(null)
let deployTimer: ReturnType<typeof setTimeout> | null = null
let deployAttempts = 0

const clearDeployTimer = () => {
  if (deployTimer) {
    clearTimeout(deployTimer)
    deployTimer = null
  }
  deployAttempts = 0
}
type ErrorWithResponse = {
  response?: { data?: { message?: string } }
  message?: string
}

// Use adapt format from query (provided by Home page)
const codeFormat = ref<'html' | 'multi_file'>(
  (route.query.adapt as 'html' | 'multi_file') || 'html',
)

// Prevent body scroll when scrolling inside chat messages
const handleChatScroll = (event: WheelEvent) => {
  const element = chatContainer.value
  if (element) {
    const { scrollTop, scrollHeight, clientHeight } = element
    const isAtTop = scrollTop === 0
    const isAtBottom = scrollTop + clientHeight >= scrollHeight - 1 // -1 for buffer

    if ((event.deltaY < 0 && isAtTop) || (event.deltaY > 0 && isAtBottom)) {
      event.preventDefault()
    }
  }
}

// Persist and restore chat state
type ChatMessage = { role: 'user' | 'assistant'; content: string; timestamp: number }
type PersistedChatState = {
  messages: ChatMessage[]
  newMessage: string
  codeFormat?: 'html' | 'multi_file'
}

const saveChatState = () => {
  try {
    if (!storageKey.value) return
    const state: PersistedChatState = {
      messages: messages.value,
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
    if (Array.isArray(state?.messages)) {
      messages.value = state.messages
    }
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

    const response = await getAppVoById({ id: appId.value })
    const backend = response.data
    if (backend?.code !== 0) {
      message.error(`Failed to load app: ${backend?.message || 'System Error'}`)
      return
    }
    const appData = backend?.data
    app.value = backend

    // Set preview URL if app is already deployed
    if (appData?.deployKey) {
      previewUrl.value = buildPreviewUrlWithCache(appData.deployKey)
    }

    // Initialize storage key for this app
    storageKey.value = `gen_chat_${appId.value}`

    // Restore chat state if available; otherwise prefill but do not auto-send
    const restored = loadChatState()
    if (!restored && appData?.initPrompt && messages.value.length === 0) {
      newMessage.value = appData.initPrompt
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

  const baseURL = myAxios.defaults.baseURL || import.meta.env.VITE_API_BASE_URL || 'http://localhost:8100/api'
  const params = new URLSearchParams({
    appId: appId.value,
    message: currentMessage,
    adapt: codeFormat.value,
  })
  const url = `${baseURL}/app/chat/gen/code?${params}`

  eventSource.value = new EventSource(url, { withCredentials: true })
  // schedule deploy retries: 45s interval, up to 3 attempts
  ;(() => {
    clearDeployTimer()
    const attempt = async () => {
      deployAttempts += 1
      try {
        const res = await getAppVoById({ id: appId.value })
        const data = res?.data?.data
        if (data?.deployKey) {
          previewUrl.value = buildPreviewUrlWithCache(data.deployKey)
          clearDeployTimer()
          return
        }
      } catch {}
      const ok = await handleDeploy()
      if (!ok && deployAttempts < 3) {
        deployTimer = setTimeout(attempt, 45000)
      } else {
        clearDeployTimer()
      }
    }
    deployTimer = setTimeout(attempt, 45000)
  })()

  eventSource.value.onmessage = (event: MessageEvent) => {
    const currentAssistantMessage = messages.value.find(
      (m) => m.role === 'assistant' && m.timestamp === assistantMessageTimestamp,
    )

    try {
      const data = JSON.parse(event.data)
      const content = data.q
      if (content && currentAssistantMessage) {
        currentAssistantMessage.content += content
        isGenerating.value = false
        scrollToBottom()
      }
    } catch (e) {
      console.error('sse data parse error', e)
    }
  }

  eventSource.value.addEventListener('done', () => {
    console.log("SSE stream finished with 'done' event. Closing connection.")
    console.log('Triggering deployment via handleDeploy() in 1 second...')
    message.success('AI code generation complete. Deploying application in 1 second...')
    eventSource.value?.close()
    eventSource.value = null
    setTimeout(() => handleDeploy(), 1000)
    clearDeployTimer()
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
    eventSource.value?.close()
    eventSource.value = null
    isGenerating.value = false
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
  console.log(`Deploying app with ID: ${appId.value}`)
  try {
    // The parameter should be { appId: appId.value } according to the type definition
    const res = await deployApp({ appId: appId.value })
    console.log('Deploy response received (axios response):', res)

    // The backend's BaseResponse is nested in res.data
    const backendResponse = res.data

    if (backendResponse && typeof backendResponse.data === 'string') {
      const deployUrl = backendResponse.data
      const finalUrl = `${deployUrl}?t=${new Date().getTime()}`

      previewUrl.value = finalUrl
      message.success('应用部署成功！')

      if (app.value?.data) {
        const urlParts = deployUrl.split('/').filter(Boolean)
        const deployKey = urlParts[urlParts.length - 1]
        app.value.data.deployKey = deployKey
      }
      clearDeployTimer()
      return true
    } else {
      console.error(
        'Deployment failed: backendResponse.data is not a string.',
        'Received (backend response):',
        backendResponse,
      )
      message.error(`部署失败: ${backendResponse.message || '后端未返回有效的部署URL。'}`)
      return false
    }
  } catch (error: unknown) {
    console.error('Deploy request failed:', error)
    const e = error as ErrorWithResponse
    const errorMessage = e.response?.data?.message || e.message || '未知网络错误'
    message.error(`部署请求失败: ${errorMessage}`)
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

// Format time
const formatTime = (timestamp: number) => {
  return new Date(timestamp).toLocaleTimeString()
}

// Load app on mount

onMounted(() => {
  loadApp().then(() => {
    // Auto-send when coming from Home with `auto=1` and there is a draft message
    const shouldAuto = String(route.query.auto || '') === '1'
    if (shouldAuto && newMessage.value.trim() && messages.value.length === 0) {
      // small delay to ensure UI is ready
      setTimeout(() => sendMessage(), 250)
    }
  })
  chatContainer.value?.addEventListener('wheel', handleChatScroll)
  window.addEventListener('beforeunload', saveChatState)
})

onUnmounted(() => {
  if (eventSource.value) {
    eventSource.value.close()
  }
  chatContainer.value?.removeEventListener('wheel', handleChatScroll)
  window.removeEventListener('beforeunload', saveChatState)
})

// Persist on changes
watch(messages, saveChatState, { deep: true })
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
            <p v-if="app.data?.appDesc">{{ app.data.appDesc }}</p>
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
              <div
                class="message-text"
                v-if="message.content"
                v-html="marked(message.content)"
              ></div>
              <div
                v-else-if="message.role === 'assistant' && isGenerating"
                class="generating-content"
              >
                <span class="typewriter-text">Synthesizing...</span>
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
            <iframe :src="previewUrl" frameborder="0" class="preview-iframe"></iframe>
          </template>
          <div v-else-if="isGenerating" class="preview-generating">
            <div class="animate-pulse text-neutral-400">Generating your app...</div>
            <p>Generating your app...</p>
          </div>
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
  background: rgba(247, 247, 248, 0.8);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 20px;
  padding: 16px 20px;
  max-width: 100%;
  overflow-wrap: break-word;
  word-wrap: break-word;
  min-width: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.2s ease;
}

.message.user .message-content {
  background: linear-gradient(135deg, rgba(24, 160, 88, 0.95), rgba(54, 173, 106, 0.95));
  backdrop-filter: blur(15px);
  border: 1px solid rgba(255, 255, 255, 0.25);
  color: #ffffff;
  box-shadow: 0 4px 16px rgba(24, 160, 88, 0.12);
}

.message-text {
  font-size: 15px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-wrap: break-word;
}

/* Hover effects for modern feel */
.message-content:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.message.user .message-content:hover {
  box-shadow: 0 6px 24px rgba(24, 160, 88, 0.18);
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
  color: #777;
  font-size: 14px;
}

.typewriter-text {
  color: rgba(0, 0, 0, 0.5);
  overflow: hidden;
  border-right: 2px solid #18a058;
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
  gap: 10px;
  background: rgba(255, 255, 255, 0.3);
  -webkit-backdrop-filter: blur(8px) saturate(120%);
  backdrop-filter: blur(8px) saturate(120%);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 16px;
  padding: 8px 10px 8px 12px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.04);
  transition:
    background-color 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease;
  width: 100%;
}

.input-wrapper:focus-within {
  background: rgba(255, 255, 255, 0.8);
  border-color: #10b981;
  box-shadow:
    0 0 0 2px rgba(16, 185, 129, 0.18),
    0 6px 14px rgba(0, 0, 0, 0.06);
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
  border-radius: 9999px;
  border: 1px solid #e5e7eb;
  background: #ffffff;
  color: #10b981;
  transition:
    background-color 0.15s ease,
    border-color 0.15s ease,
    opacity 0.2s ease;
}
.send-button:hover {
  background: #f0fdf4;
  border-color: #10b981;
}
.send-button:disabled {
  opacity: 0.55;
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
</style>
