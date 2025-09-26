<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getAppVoById, deleteMyApp, deployApp } from '@/api/appController'
import { useLoginUserStore } from '@/stores/loginUser'
import { useMessage, formatDate, openPreview, downloadAppCode } from '@/utils'

const route = useRoute()
const router = useRouter()
const message = useMessage()
const loginUserStore = useLoginUserStore()

// State
const appId = ref<string>(route.params.id as string)
const app = ref<API.AppVO>({})
const loading = ref(true)
const isDeploying = ref(false)
const isDeleting = ref(false)
const isDownloading = ref(false)

// Load app data
const loadApp = async () => {
  try {
    const res = await getAppVoById({ id: appId.value })
    if (res.data?.code === 0 && res.data.data) {
      app.value = res.data.data
    } else {
      message.error('Failed to load app')
      router.push('/')
    }
  } catch (error) {
    console.error('Load app error:', error)
    message.error('Failed to load app')
    router.push('/')
  } finally {
    loading.value = false
  }
}

// Check if user can edit this app
const canEdit = () => {
  return (
    app.value.userId === loginUserStore.loginUser.id ||
    (loginUserStore.loginUser.userRole || '').toLowerCase() === 'admin'
  )
}

// Start generation/chat
const startGeneration = () => {
  router.push(`/app/generate/${appId.value}`)
}

// Edit app
const editApp = () => {
  router.push(`/app/edit/${appId.value}`)
}

// Deploy app
const handleDeploy = async () => {
  if (isDeploying.value) return

  isDeploying.value = true
  try {
    const res = await deployApp({ appId: appId.value })
    if (res.data?.code === 0 && res.data.data) {
      message.success('App deployed successfully!')
      // Reload app data to get updated deploy info
      await loadApp()
      // Open deployed URL in new tab
      window.open(res.data.data, '_blank')
    } else {
      message.error(res.data?.message || 'Failed to deploy app')
    }
  } catch (error) {
    console.error('Deploy error:', error)
    message.error('Failed to deploy app')
  } finally {
    isDeploying.value = false
  }
}

// Delete app
const handleDelete = async () => {
  if (confirm('Delete this app? This action cannot be undone.')) {
    isDeleting.value = true
    try {
      const res = await deleteMyApp({ id: appId.value })
      if (res.data?.code === 0) {
        message.success('App deleted successfully')
        router.push('/')
      } else {
        message.error(res.data?.message || 'Failed to delete app')
      }
    } catch (error) {
      console.error('Delete error:', error)
      message.error('Failed to delete app')
    } finally {
      isDeleting.value = false
    }
  }
}

// Download app code
const handleDownload = async () => {
  if (isDownloading.value || !appId.value) return

  isDownloading.value = true
  try {
    await downloadAppCode(appId.value)
    message.success('Code downloaded successfully!')
  } catch (error: any) {
    console.error('Download error:', error)
    message.error(error.message || 'Failed to download code')
  } finally {
    isDownloading.value = false
  }
}

// Preview app
const previewApp = async () => {
  // If already deployed, just open the URL
  if (app.value.deployKey) {
    openPreview(app.value.deployKey)
    return
  }

  // If not deployed, deploy first then open
  try {
    const res = await deployApp({ appId: appId.value })
    if (res.data?.code === 0 && res.data.data) {
      // Process the deploy URL for nginx
      const deployKey = res.data.data
      let finalDeployUrl = ''
      if (deployKey.startsWith('http')) {
        finalDeployUrl = deployKey
      } else if (deployKey.includes('.')) {
        finalDeployUrl = `${window.location.protocol}//${deployKey}`
      } else {
        const cleanKey = deployKey.replace(/\//g, '') // Remove any slashes
        const deployBaseUrl = import.meta.env.VITE_DEPLOY_BASE_URL || window.location.origin
        finalDeployUrl = `${deployBaseUrl}/${cleanKey}`
      }

      // Add timestamp to bust cache
      const finalUrl = `${finalDeployUrl}?t=${new Date().getTime()}`
      window.open(finalUrl, '_blank')
      message.success('App deployed and opened for preview!')
      // Reload app data to reflect deployment
      await loadApp()
    } else {
      message.error('Failed to deploy app for preview')
    }
  } catch (error) {
    console.error('Preview error:', error)
    message.error('Failed to deploy app for preview')
  }
}

onMounted(() => {
  loadApp()
})
</script>

<template>
  <div class="app-detail-container" v-if="!loading">
    <!-- Header -->
    <div class="detail-header">
      <div class="header-left">
        <button @click="router.push('/')" class="back-button">
          <svg
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="currentColor"
            style="margin-right: 6px"
          >
            <path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z" />
          </svg>
          Back to Home
        </button>
      </div>
    </div>

    <!-- App Info -->
    <div class="app-info-section">
      <div class="app-preview">
        <div class="preview-container">
          <img v-if="app.cover" :src="app.cover" :alt="app.appName" class="app-cover" />
          <div v-else class="app-placeholder">
            <svg width="80" height="80" viewBox="0 0 24 24" fill="currentColor">
              <path
                d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"
              />
            </svg>
          </div>
          <div class="preview-overlay">
            <button class="px-4 py-3 rounded-md bg-emerald-600 text-white" @click="previewApp">
              <span style="display: inline-flex; align-items: center; gap: 6px">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                  <path
                    d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"
                  />
                </svg>
                Preview
              </span>
            </button>
          </div>
        </div>
      </div>

      <div class="app-details">
        <div class="app-header-info">
          <h1 class="app-name">{{ app.appName }}</h1>
          <div class="app-badges">
            <span
              v-if="app.priority && app.priority >= 99"
              class="inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold bg-emerald-100 text-emerald-700"
            >
              Featured
            </span>
            <span
              :class="[
                'inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold',
                app.deployedTime
                  ? 'bg-emerald-100 text-emerald-700'
                  : 'bg-neutral-100 text-neutral-700',
              ]"
            >
              {{ app.deployedTime ? 'Deployed' : 'Not Deployed' }}
            </span>
          </div>
        </div>

        <div class="app-meta">
          <div class="meta-item">
            <span class="meta-label">Created by:</span>
            <span class="meta-value">{{ app.user?.userName || 'Unknown' }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">Created at:</span>
            <span class="meta-value">{{ formatDate(app.createTime) }}</span>
          </div>
          <div class="meta-item" v-if="app.deployedTime">
            <span class="meta-label">Deployed at:</span>
            <span class="meta-value">{{ formatDate(app.deployedTime) }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">Type:</span>
            <span class="meta-value">{{ app.codeGenType || 'React' }}</span>
          </div>
        </div>

        <div class="app-description" v-if="app.initPrompt">
          <h3>Description</h3>
          <p>{{ app.initPrompt }}</p>
        </div>

        <!-- Actions -->
        <div class="app-actions">
          <button class="px-4 py-3 rounded-md bg-emerald-600 text-white" @click="startGeneration">
            <span style="display: inline-flex; align-items: center; gap: 6px">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                <path d="M20 6L9 17l-5-5 1.41-1.41L9 14.17 18.59 4.59 20 6z" />
              </svg>
              Continue Building
            </span>
          </button>

          <button
            class="px-4 py-3 rounded-md border border-emerald-600 text-emerald-700 disabled:opacity-50"
            :disabled="isDeploying"
            @click="handleDeploy"
          >
            {{ app.deployedTime ? 'Redeploy' : 'Deploy' }}
          </button>

          <button
            class="px-4 py-3 rounded-md border border-blue-600 text-blue-700 disabled:opacity-50"
            :disabled="isDownloading"
            @click="handleDownload"
          >
            <span style="display: inline-flex; align-items: center; gap: 6px">
              <svg
                v-if="!isDownloading"
                width="16"
                height="16"
                viewBox="0 0 24 24"
                fill="currentColor"
              >
                <path d="M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z" />
              </svg>
              <span v-if="isDownloading">Downloading...</span>
              <span v-else>Download</span>
            </span>
          </button>

          <button class="px-4 py-3 rounded-md border" @click="previewApp">Preview</button>

          <template v-if="canEdit()">
            <button class="px-4 py-3 rounded-md border" @click="editApp">Edit</button>

            <button
              class="px-4 py-3 rounded-md border border-red-600 text-red-700 disabled:opacity-50"
              :disabled="isDeleting"
              @click="handleDelete"
            >
              Delete
            </button>
          </template>
        </div>
      </div>
    </div>
  </div>

  <!-- Loading State -->
  <div v-else class="loading-container">
    <div class="animate-pulse text-neutral-400">Loading...</div>
    <p>Loading app details...</p>
  </div>
</template>

<style scoped>
.app-detail-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.detail-header {
  margin-bottom: 32px;
}

.back-button {
  color: #6b7280;
  font-size: 14px;
}

.app-info-section {
  display: grid;
  grid-template-columns: 400px 1fr;
  gap: 48px;
  align-items: start;
}

.app-preview {
  position: sticky;
  top: 24px;
}

.preview-container {
  position: relative;
  width: 100%;
  aspect-ratio: 4/3;
  border-radius: 16px;
  overflow: hidden;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  cursor: pointer;
  transition: all 0.3s ease;
}

.preview-container:hover {
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.15);
  transform: translateY(-2px);
}

.app-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 16px;
}

.app-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
}

.preview-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.preview-container:hover .preview-overlay {
  opacity: 1;
}

.app-details {
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.app-header-info {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.app-name {
  font-size: 36px;
  font-weight: 700;
  color: #1f2937;
  margin: 0;
  line-height: 1.2;
}

.app-badges {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.app-meta {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  padding: 24px;
  background: #f9fafb;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.meta-label {
  font-size: 14px;
  font-weight: 500;
  color: #6b7280;
}

.meta-value {
  font-size: 16px;
  color: #1f2937;
  font-weight: 500;
}

.app-description h3 {
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 16px 0;
}

.app-description p {
  font-size: 16px;
  line-height: 1.6;
  color: #4b5563;
  margin: 0;
  white-space: pre-wrap;
}

.app-actions {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.app-actions .n-button {
  height: 48px;
  padding: 0 24px;
  font-weight: 500;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 60vh;
  gap: 16px;
  color: #6b7280;
}

@media (max-width: 1024px) {
  .app-info-section {
    grid-template-columns: 1fr;
    gap: 32px;
  }

  .app-preview {
    position: static;
  }

  .preview-container {
    max-width: 500px;
    margin: 0 auto;
  }
}

@media (max-width: 768px) {
  .app-detail-container {
    padding: 16px;
  }

  .app-name {
    font-size: 28px;
  }

  .app-header-info {
    flex-direction: column;
    gap: 16px;
  }

  .app-meta {
    grid-template-columns: 1fr;
    gap: 12px;
    padding: 16px;
  }

  .app-actions {
    flex-direction: column;
  }

  .app-actions .n-button {
    width: 100%;
  }
}
</style>
