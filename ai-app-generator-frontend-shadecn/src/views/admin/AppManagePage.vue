<script setup lang="ts">
import { ref, onMounted, reactive, h } from 'vue'
import { useRouter } from 'vue-router'
import { toast } from 'vue-sonner'
import { listAppVoByPageByAdmin, deleteAppByAdmin, updateApp } from '@/api/appController'
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
const apps = ref<API.AppVO[]>([])
const pagination = reactive({
  page: 1,
  pageSize: 20,
  itemCount: 0,
})

// Search form
const searchForm = reactive<API.AppQueryRequest>({
  pageNum: 1,
  pageSize: 20,
  sortField: 'createTime',
  sortOrder: 'desc',
  appName: '',
  userId: undefined,
  priority: undefined,
})

// Edit modal
const showEditModal = ref(false)
const editForm = reactive<API.AppUpdateAdminRequest>({
  id: undefined,
  appName: '',
  cover: '',
  priority: undefined,
})

// Load apps
const loadApps = async () => {
  loading.value = true
  try {
    const res = await listAppVoByPageByAdmin({
      ...searchForm,
      pageNum: pagination.page,
      pageSize: pagination.pageSize,
    })

    if (res.data?.code === 0 && res.data.data) {
      apps.value = res.data.data.records || []
      pagination.itemCount = res.data.data.totalRow || 0
    } else {
      message.error(res.data?.message || 'Failed to load apps')
    }
  } catch (error) {
    console.error('Load apps error:', error)
    message.error('Failed to load apps')
  } finally {
    loading.value = false
  }
}

// Search
const handleSearch = () => {
  pagination.page = 1
  loadApps()
}

// Reset search
const handleReset = () => {
  Object.assign(searchForm, {
    pageNum: 1,
    pageSize: 20,
    sortField: 'createTime',
    sortOrder: 'desc',
    appName: '',
    userId: undefined,
    priority: undefined,
  })
  pagination.page = 1
  loadApps()
}

// Pagination change
const handlePageChange = (page: number) => {
  pagination.page = Math.max(
    1,
    Math.min(page, Math.ceil(pagination.itemCount / pagination.pageSize) || 1),
  )
  loadApps()
}

const handlePageSizeChange = (pageSize: number) => {
  pagination.pageSize = pageSize
  pagination.page = 1
  searchForm.pageSize = pageSize
  loadApps()
}

// Edit app
const handleEdit = (app: API.AppVO) => {
  Object.assign(editForm, {
    id: app.id,
    appName: app.appName,
    cover: app.cover,
    priority: app.priority,
  })
  showEditModal.value = true
}

// Save edit
const handleSaveEdit = async () => {
  try {
    const res = await updateApp(editForm)
    if (res.data?.code === 0) {
      message.success('App updated successfully')
      showEditModal.value = false
      loadApps()
    } else {
      message.error(res.data?.message || 'Failed to update app')
    }
  } catch (error) {
    console.error('Update app error:', error)
    message.error('Failed to update app')
  }
}

// Delete app
const handleDelete = async (app: API.AppVO) => {
  if (confirm(`Delete "${app.appName}"? This action cannot be undone.`)) {
    try {
      const res = await deleteAppByAdmin({ id: app.id })
      if (res.data?.code === 0) {
        message.success('App deleted successfully')
        loadApps()
      } else {
        message.error(res.data?.message || 'Failed to delete app')
      }
    } catch (error) {
      console.error('Delete app error:', error)
      message.error('Failed to delete app')
    }
  }
}

// Set as featured
const handleSetFeatured = async (app: API.AppVO) => {
  try {
    const res = await updateApp({
      id: app.id,
      appName: app.appName,
      cover: app.cover,
      priority: 99,
    })
    if (res.data?.code === 0) {
      message.success('App set as featured successfully')
      loadApps()
    } else {
      message.error(res.data?.message || 'Failed to set as featured')
    }
  } catch (error) {
    console.error('Set featured error:', error)
    message.error('Failed to set as featured')
  }
}

// View app details
const handleView = (app: API.AppVO) => {
  router.push(`/app/detail/${app.id}`)
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

// Table columns (for reference / not used in template table)
const columns = [{ title: 'ID', key: 'id', width: 80 }]

onMounted(() => {
  // Check admin permission
  if ((loginUserStore.loginUser.userRole || '').toLowerCase() !== 'admin') {
    message.error('Access denied. Admin permission required.')
    router.push('/')
    return
  }
  loadApps()
})
</script>

<template>
  <div class="app-manage-container">
    <div class="page-header">
      <h1>App Management</h1>
      <p>Manage all applications in the system</p>
    </div>

    <!-- Search Form -->
    <div class="search-card bg-white border rounded-xl p-4">
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div>
          <label class="block mb-1 text-sm">App Name</label>
          <input
            v-model="searchForm.appName"
            placeholder="Search by app name"
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
        <div>
          <label class="block mb-1 text-sm">Priority</label>
          <input
            type="number"
            v-model="searchForm.priority"
            min="0"
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
              <th class="py-2 pr-4">App Name</th>
              <th class="py-2 pr-4">Creator</th>
              <th class="py-2 pr-4">Type</th>
              <th class="py-2 pr-4">Status</th>
              <th class="py-2 pr-4">Priority</th>
              <th class="py-2 pr-4">Created</th>
              <th class="py-2">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in apps" :key="row.id" class="border-t">
              <td class="py-2 pr-4">{{ row.id }}</td>
              <td class="py-2 pr-4">
                <div class="app-name-cell">
                  <div class="app-name">{{ row.appName }}</div>
                  <span
                    v-if="row.priority && row.priority >= 99"
                    class="inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold bg-emerald-100 text-emerald-700"
                    >Featured</span
                  >
                </div>
              </td>
              <td class="py-2 pr-4">{{ row.user?.userName || 'Unknown' }}</td>
              <td class="py-2 pr-4">
                <span
                  class="inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold bg-neutral-100 text-neutral-700"
                  >{{ row.codeGenType || 'React' }}</span
                >
              </td>
              <td class="py-2 pr-4">
                <span
                  :class="
                    'inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold ' +
                    (row.deployedTime
                      ? 'bg-emerald-100 text-emerald-700'
                      : 'bg-neutral-100 text-neutral-700')
                  "
                >
                  {{ row.deployedTime ? 'Deployed' : 'Not Deployed' }}
                </span>
              </td>
              <td class="py-2 pr-4">{{ row.priority || 0 }}</td>
              <td class="py-2 pr-4">{{ formatDate(row.createTime) }}</td>
              <td class="py-2">
                <div class="action-buttons">
                  <button class="px-2 py-1 rounded border" @click="handleView(row)">View</button>
                  <button class="px-2 py-1 rounded border" @click="handleEdit(row)">Edit</button>
                  <button
                    class="px-2 py-1 rounded border disabled:opacity-50"
                    :disabled="!!(row.priority && row.priority >= 99)"
                    @click="handleSetFeatured(row)"
                  >
                    Feature
                  </button>
                  <button class="px-2 py-1 rounded border" @click="handleDelete(row)">
                    Delete
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

    <!-- Edit Modal -->
    <div
      v-if="showEditModal"
      class="fixed inset-0 bg-black/40 flex items-center justify-center p-4"
    >
      <div class="bg-white rounded-xl p-4 w-[520px] max-w-full">
        <div class="text-lg font-semibold mb-4">Edit App</div>
        <div class="space-y-3">
          <div>
            <label class="block mb-1 text-sm">App Name</label>
            <input
              v-model="editForm.appName"
              placeholder="Enter app name"
              class="w-full border rounded px-3 py-2"
            />
          </div>
          <div>
            <label class="block mb-1 text-sm">Cover Image URL</label>
            <input
              v-model="editForm.cover"
              placeholder="Enter cover image URL"
              class="w-full border rounded px-3 py-2"
            />
          </div>
          <div>
            <label class="block mb-1 text-sm">Priority</label>
            <input
              type="number"
              min="0"
              max="999"
              v-model="editForm.priority as any"
              placeholder="Enter priority (99 for featured)"
              class="w-full border rounded px-3 py-2"
            />
            <div class="text-sm text-neutral-500 mt-1">
              Set to 99 or higher to make this app featured
            </div>
          </div>
        </div>
        <div class="flex justify-end gap-2 mt-4">
          <button class="px-3 py-2 rounded border" @click="showEditModal = false">Cancel</button>
          <button class="px-3 py-2 rounded bg-emerald-600 text-white" @click="handleSaveEdit">
            Save
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.app-manage-container {
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

.app-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.app-name {
  flex: 1;
  min-width: 0;
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
