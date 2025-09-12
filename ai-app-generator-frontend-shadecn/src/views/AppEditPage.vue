<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { toast } from 'vue-sonner'
import { getAppVoById, updateMyApp, updateApp } from '@/api/appController'
import { useLoginUserStore } from '@/stores/loginUser'

const route = useRoute()
const router = useRouter()
const message = {
  success: (t: string) => toast.success(t),
  error: (t: string) => toast.error(t),
  warning: (t: string) => toast.warning(t),
}
const loginUserStore = useLoginUserStore()

// State
const appId = ref<string>(route.params.id as string)
const app = ref<API.AppVO>({})
const loading = ref(true)
const saving = ref(false)
const isAdmin = ref(false)

// Form data
const formData = reactive<{
  appName: string
  cover?: string
  priority?: number
}>({
  appName: '',
  cover: '',
  priority: undefined,
})

// Load app data
const loadApp = async () => {
  try {
    const res = await getAppVoById({ id: appId.value })
    if (res.data?.code === 0 && res.data.data) {
      app.value = res.data.data

      // Check permissions
      const userRole = (loginUserStore.loginUser.userRole || '').toLowerCase()
      isAdmin.value = userRole === 'admin'

      if (!isAdmin.value && app.value.userId !== loginUserStore.loginUser.id) {
        message.error('You do not have permission to edit this app')
        router.push('/')
        return
      }

      // Fill form data
      formData.appName = app.value.appName || ''
      formData.cover = app.value.cover || ''
      formData.priority = app.value.priority
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

// Save changes
const handleSave = async () => {
  if (saving.value) return

  if (!formData.appName.trim()) {
    message.warning('Please enter app name')
    return
  }

  saving.value = true
  try {
    let res
    if (isAdmin.value) {
      // Admin can update all fields
      res = await updateApp({
        id: appId.value,
        appName: formData.appName,
        cover: formData.cover,
        priority: formData.priority,
      })
    } else {
      // Regular user can only update app name
      res = await updateMyApp({
        id: appId.value,
        appName: formData.appName,
      })
    }

    if (res.data?.code === 0) {
      message.success('App updated successfully')
      router.push(`/app/detail/${appId.value}`)
    } else {
      message.error(res.data?.message || 'Failed to update app')
    }
  } catch (error) {
    console.error('Update app error:', error)
    message.error('Failed to update app')
  } finally {
    saving.value = false
  }
}

// Cancel editing
const handleCancel = () => {
  router.push(`/app/detail/${appId.value}`)
}

// Format date
const formatDate = (dateString?: string) => {
  if (!dateString) return 'N/A'
  return new Date(dateString).toLocaleDateString([], {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

onMounted(async () => {
  // 首先获取最新的登录用户信息
  await loginUserStore.fetchLoginUser()

  if (!loginUserStore.loginUser.id) {
    message.warning('Please login first')
    router.push('/')
    return
  }
  loadApp()
})
</script>

<template>
  <div class="app-edit-container" v-if="!loading">
    <!-- Header -->
    <div class="edit-header">
      <div class="header-left">
        <button @click="handleCancel" class="back-button">
          <span style="display: inline-flex; align-items: center; gap: 6px">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
              <path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z" />
            </svg>
            Back to App
          </span>
        </button>
      </div>
      <div class="header-right">
        <div class="flex gap-2">
          <button class="px-3 py-2 rounded-md border" @click="handleCancel">Cancel</button>
          <button
            class="px-3 py-2 rounded-md bg-emerald-600 text-white disabled:opacity-50"
            :disabled="saving"
            @click="handleSave"
          >
            Save Changes
          </button>
        </div>
      </div>
    </div>

    <!-- Main Content -->
    <div class="edit-content">
      <div class="content-wrapper">
        <!-- App Info Card -->
        <div class="info-card bg-white border rounded-xl p-4">
          <div class="card-header">
            <h2>App Information</h2>
            <span
              v-if="isAdmin"
              class="inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold bg-amber-100 text-amber-700"
              >Admin Mode</span
            >
          </div>

          <div class="app-meta">
            <div class="meta-row">
              <span class="meta-label">App ID:</span>
              <span class="meta-value">{{ app.id }}</span>
            </div>
            <div class="meta-row">
              <span class="meta-label">Creator:</span>
              <span class="meta-value">{{ app.user?.userName || 'Unknown' }}</span>
            </div>
            <div class="meta-row">
              <span class="meta-label">Created:</span>
              <span class="meta-value">{{ formatDate(app.createTime) }}</span>
            </div>
            <div class="meta-row">
              <span class="meta-label">Type:</span>
              <span class="meta-value">{{ app.codeGenType || 'React' }}</span>
            </div>
            <div class="meta-row" v-if="app.deployedTime">
              <span class="meta-label">Deployed:</span>
              <span class="meta-value">{{ formatDate(app.deployedTime) }}</span>
            </div>
          </div>
        </div>

        <!-- Edit Form -->
        <div class="form-card bg-white border rounded-xl p-4">
          <h2 class="mb-4">Edit Details</h2>

          <form @submit.prevent>
            <!-- App Name -->
            <div class="mb-4">
              <label class="block mb-2 text-sm">App Name</label>
              <input
                v-model="formData.appName"
                placeholder="Enter a descriptive name for your app"
                maxlength="100"
                class="w-full border rounded-md px-3 py-2"
              />
            </div>

            <!-- Admin-only fields -->
            <template v-if="isAdmin">
              <div class="mb-4">
                <label class="block mb-2 text-sm">Cover Image URL</label>
                <input
                  v-model="formData.cover"
                  placeholder="Enter cover image URL (optional)"
                  class="w-full border rounded-md px-3 py-2"
                />
                <div v-if="formData.cover" class="cover-preview">
                  <img
                    :src="formData.cover"
                    alt="Cover preview"
                    @error="
                      (e: Event) => {
                        const target = e.target as HTMLImageElement
                        if (target) target.style.display = 'none'
                      }
                    "
                  />
                </div>
              </div>

              <div class="mb-2">
                <label class="block mb-2 text-sm">Priority</label>
                <input
                  type="number"
                  v-model="formData.priority"
                  placeholder="Set priority (99+ for featured)"
                  class="w-full border rounded-md px-3 py-2"
                />
                <div class="priority-info text-sm text-neutral-500 mt-1">
                  Set to 99 or higher to make this app featured on the homepage
                </div>
              </div>
            </template>

            <!-- Original Prompt (Read-only) -->
            <div class="mt-4">
              <label class="block mb-2 text-sm">Original Prompt</label>
              <textarea
                :value="app.initPrompt"
                rows="4"
                readonly
                placeholder="No original prompt available"
                class="w-full border rounded-md px-3 py-2"
              ></textarea>
              <div class="text-sm text-neutral-500 mt-1">
                This is the original prompt used to create the app. It cannot be modified.
              </div>
            </div>
          </form>
        </div>

        <!-- Preview Card (if cover exists) -->
        <div v-if="formData.cover" class="preview-card bg-white border rounded-xl p-4">
          <h3 class="mb-2">Cover Preview</h3>
          <div class="cover-preview-large">
            <img :src="formData.cover" :alt="formData.appName" />
          </div>
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
.app-edit-container {
  min-height: 100vh;
  background: #f8fafc;
}

.edit-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: white;
  border-bottom: 1px solid #e5e7eb;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1);
}

.header-left .back-button {
  color: #6b7280;
  font-size: 14px;
}

.edit-content {
  padding: 24px;
}

.content-wrapper {
  max-width: 800px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.card-header h2,
.form-card h2,
.preview-card h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
}

.preview-card h3 {
  font-size: 18px;
}

.app-meta {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.meta-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f3f4f6;
}

.meta-row:last-child {
  border-bottom: none;
}

.meta-label {
  font-weight: 500;
  color: #6b7280;
  min-width: 100px;
}

.meta-value {
  color: #1f2937;
  font-weight: 500;
}

.form-card :deep(.n-form) {
  gap: 24px;
}

.cover-preview {
  margin-top: 8px;
}

.cover-preview img,
.cover-preview-large img {
  max-width: 100%;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
}

.cover-preview img {
  max-height: 100px;
}

.cover-preview-large {
  display: flex;
  justify-content: center;
}

.cover-preview-large img {
  max-height: 300px;
  width: auto;
}

.priority-info {
  margin-top: 4px;
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

@media (max-width: 768px) {
  .edit-header {
    padding: 12px 16px;
  }

  .edit-content {
    padding: 16px;
  }

  .content-wrapper {
    gap: 16px;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .meta-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }

  .meta-label {
    min-width: auto;
    font-size: 14px;
  }
}
</style>
