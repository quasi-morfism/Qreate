<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { toast } from 'vue-sonner'
import {
  listAllChatHistoryByPageForAdmin,
  deleteChatHistoryByAppId,
} from '@/api/chatHistoryController'
import { useLoginUserStore } from '@/stores/loginUser'

const router = useRouter()
const message = {
  success: (t: string) => toast.success(t),
  error: (t: string) => toast.error(t),
  warning: (t: string) => toast.warning(t),
}
const loginUserStore = useLoginUserStore()

// State
const loading = ref(false)
const histories = ref<API.ChatHistory[]>([])
const pagination = reactive({
  page: 1,
  pageSize: 20,
  itemCount: 0,
})

// Search form
const searchForm = reactive<API.ChatHistoryQueryRequest>({
  pageNum: 1,
  pageSize: 20,
  sortField: 'createTime',
  sortOrder: 'desc',
  message: '',
  appId: undefined,
  userId: undefined,
})

// Load histories
const loadHistories = async () => {
  loading.value = true
  try {
    const res = await listAllChatHistoryByPageForAdmin({
      ...searchForm,
      pageNum: pagination.page,
      pageSize: pagination.pageSize,
    })

    if (res.data?.code === 0 && res.data.data) {
      histories.value = res.data.data.records || []
      pagination.itemCount = res.data.data.totalRow || 0
    } else {
      message.error(res.data?.message || 'Failed to load chat histories')
    }
  } catch (error) {
    console.error('Load chat histories error:', error)
    message.error('Failed to load chat histories')
  } finally {
    loading.value = false
  }
}

// Search
const handleSearch = () => {
  pagination.page = 1
  loadHistories()
}

// Reset search
const handleReset = () => {
  Object.assign(searchForm, {
    pageNum: 1,
    pageSize: 20,
    sortField: 'createTime',
    sortOrder: 'desc',
    message: '',
    appId: undefined,
    userId: undefined,
  })
  pagination.page = 1
  loadHistories()
}

// Pagination change
const handlePageChange = (page: number) => {
  pagination.page = Math.max(
    1,
    Math.min(page, Math.ceil(pagination.itemCount / pagination.pageSize) || 1),
  )
  loadHistories()
}

const handlePageSizeChange = (pageSize: number) => {
  pagination.pageSize = pageSize
  pagination.page = 1
  searchForm.pageSize = pageSize
  loadHistories()
}

// Delete all histories for an app
const handleDelete = async (history: API.ChatHistory) => {
  if (
    confirm(
      `Delete ALL chat histories for App ID "${history.appId}"? This action cannot be undone.`,
    )
  ) {
    try {
      const res = await deleteChatHistoryByAppId({ appId: history.appId as any })
      if (res.data?.code === 0) {
        message.success('Chat histories deleted successfully')
        loadHistories()
      } else {
        message.error(res.data?.message || 'Failed to delete chat histories')
      }
    } catch (error) {
      console.error('Delete chat histories error:', error)
      message.error('Failed to delete chat histories')
    }
  }
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

onMounted(() => {
  // Check admin permission
  if ((loginUserStore.loginUser.userRole || '').toLowerCase() !== 'admin') {
    message.error('Access denied. Admin permission required.')
    router.push('/')
    return
  }
  loadHistories()
})
</script>

<template>
  <div class="chat-manage-container">
    <div class="page-header">
      <h1>Chat Management</h1>
      <p>Manage all chat histories in the system</p>
    </div>

    <!-- Search Form -->
    <div class="search-card bg-white border rounded-xl p-4">
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div>
          <label class="block mb-1 text-sm">Message Content</label>
          <input
            v-model="searchForm.message"
            placeholder="Search by message content"
            class="w-full border rounded px-3 py-2"
          />
        </div>
        <div>
          <label class="block mb-1 text-sm">App ID</label>
          <input
            type="number"
            v-model="searchForm.appId"
            min="1"
            class="w-full border rounded px-3 py-2"
          />
        </div>
        <div>
          <label class="block mb-1 text-sm">User ID</label>
          <input
            type="number"
            v-model="searchForm.userId"
            min="1"
            class="w-full border rounded px-3 py-2"
          />
        </div>
        <div class="md:col-span-3 flex gap-2">
          <button class="px-3 py-2 rounded bg-emerald-600 text-white" @click="handleSearch">
            Search
          </button>
          <button class="px-3 py-2 rounded border" @click="handleReset">Reset</button>
        </div>
      </div>
    </div>

    <!-- Data Table -->
    <div class="table-card bg-white border rounded-xl p-4 mt-4">
      <div class="overflow-x-auto">
        <table class="min-w-full text-sm">
          <thead>
            <tr class="text-left">
              <th class="py-2 pr-4">ID</th>
              <th class="py-2 pr-4">Message</th>
              <th class="py-2 pr-4">Type</th>
              <th class="py-2 pr-4">App ID</th>
              <th class="py-2 pr-4">User ID</th>
              <th class="py-2 pr-4">Created</th>
              <th class="py-2">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in histories" :key="row.id" class="border-t">
              <td class="py-2 pr-4">{{ row.id }}</td>
              <td class="py-2 pr-4 message-cell">
                <div class="message-content">{{ row.message }}</div>
              </td>
              <td class="py-2 pr-4">
                <span
                  class="inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold"
                  :class="
                    row.messageType === 'user'
                      ? 'bg-blue-100 text-blue-700'
                      : 'bg-emerald-100 text-emerald-700'
                  "
                  >{{ row.messageType }}</span
                >
              </td>
              <td class="py-2 pr-4">{{ row.appId }}</td>
              <td class="py-2 pr-4">{{ row.userId }}</td>
              <td class="py-2 pr-4">{{ formatDate(row.createTime) }}</td>
              <td class="py-2">
                <div class="action-buttons">
                  <button class="px-2 py-1 rounded border" @click="handleDelete(row)">
                    Delete (All by App)
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="flex items-center justify-between mt-3">
        <div class="text-sm">Total: {{ pagination.itemCount }}</div>
        <div class="flex items-center gap-2">
          <button
            class="px-3 py-2 rounded border"
            @click="handlePageChange(pagination.page - 1)"
            :disabled="pagination.page <= 1"
          >
            Prev
          </button>
          <div class="px-2 py-2 text-sm">
            Page {{ pagination.page }} /
            {{ Math.max(1, Math.ceil(pagination.itemCount / pagination.pageSize)) }}
          </div>
          <button
            class="px-3 py-2 rounded border"
            @click="handlePageChange(pagination.page + 1)"
            :disabled="pagination.page >= Math.ceil(pagination.itemCount / pagination.pageSize)"
          >
            Next
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-manage-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  font-size: 28px;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 8px 0;
}

.page-header p {
  color: #6b7280;
  margin: 0;
  font-size: 16px;
}

.message-cell {
  max-width: 400px;
}

.message-content {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.action-buttons {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}
</style>
