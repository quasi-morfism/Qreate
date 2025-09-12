<script setup lang="ts">
import { onMounted, reactive, ref, h } from 'vue'
import { toast } from 'vue-sonner'
import {
  listUserVoByPage,
  addUser,
  updateUser,
  deleteUser,
  getUserById,
} from '@/api/userController'

const message = {
  success: (t: string) => toast.success(t),
  error: (t: string) => toast.error(t),
  warning: (t: string) => toast.warning(t),
}

const query = reactive<API.UserQueryRequest>({
  pageNum: 1,
  pageSize: 10,
  userName: '',
  userAccount: '',
})

const loading = ref(false)
const users = ref<API.UserVO[]>([])
const total = ref(0)

const editModalVisible = ref(false)
const isEditing = ref(false)
const editForm = reactive<Partial<API.UserUpdateRequest & API.UserAddRequest>>({
  id: undefined,
  userName: '',
  userAccount: '',
  userAvatar: '',
  userProfile: '',
  userRole: 'user',
})

const roleOptions = [
  { label: 'User', value: 'user' },
  { label: 'Admin', value: 'admin' },
]

function formatDate(value?: string | number) {
  if (!value) return ''
  const d = new Date(value)
  if (isNaN(d.getTime())) return value
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

type UserRow = API.UserVO & {
  id: number
  createTime?: string
  updateTime?: string
  editTime?: string
}

const columns = [
  { title: 'ID', key: 'id', width: 80 },
  { title: 'Account', key: 'userAccount' },
  { title: 'Name', key: 'userName' },
  {
    title: 'Created',
    key: 'createTime',
    width: 170,
    render: (row: UserRow) => formatDate(row.createTime),
  },
  {
    title: 'Updated',
    key: 'updateTime',
    width: 170,
    render: (row: UserRow) => formatDate(row.updateTime) || '-',
  },
  {
    title: 'Edited',
    key: 'editTime',
    width: 170,
    render: (row: UserRow) => formatDate(row.editTime) || '-',
  },
  { title: 'Role', key: 'userRole', width: 120 },
  {
    title: 'Actions',
    key: 'actions',
    render: (row: UserRow) =>
      h('div', { style: 'display:flex;gap:8px' }, [
        h('button', { class: 'px-2 py-1 rounded border', onClick: () => openEdit(row.id) }, 'Edit'),
        h(
          'button',
          { class: 'px-2 py-1 rounded border', onClick: () => confirmDelete(row.id) },
          'Delete',
        ),
      ]),
  },
]

async function fetchUsers() {
  loading.value = true
  try {
    const res = await listUserVoByPage({
      ...query,
    })
    if (res.data?.code === 0 && res.data.data) {
      const baseList = (res.data.data.records || []) as (API.UserVO & {
        updateTime?: string
        editTime?: string
      })[]
      total.value = res.data.data.totalRow || 0

      // Enrich each row with update/edit times from detail API if needed
      const enriched = await Promise.all(
        baseList.map(async (u) => {
          if (!u.id) return u
          try {
            const detail = await getUserById({ id: u.id })
            if (detail.data?.code === 0 && detail.data.data) {
              const d = detail.data.data
              const detailUser = d as API.User & { editTime?: string; updateTime?: string }
              return {
                ...u,
                updateTime: detailUser.updateTime,
                editTime: detailUser.editTime,
              }
            }
          } catch {}
          return u
        }),
      )
      users.value = enriched
    } else {
      message.error(res.data?.message || 'Failed to load users')
    }
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  fetchUsers()
}

function handlePageChange(page: number) {
  query.pageNum = page
  fetchUsers()
}

function handlePageSizeChange(size: number) {
  query.pageSize = size
  query.pageNum = 1
  fetchUsers()
}

function openAdd() {
  isEditing.value = false
  Object.assign(editForm, {
    id: undefined,
    userName: '',
    userAccount: '',
    userAvatar: '',
    userProfile: '',
    userRole: 'user',
  })
  editModalVisible.value = true
}

async function openEdit(id: number | undefined) {
  if (!id) return
  isEditing.value = true
  const res = await getUserById({ id })
  if (res.data?.code === 0 && res.data.data) {
    Object.assign(editForm, {
      id: res.data.data.id,
      userName: res.data.data.userName,
      userAccount: res.data.data.userAccount,
      userAvatar: res.data.data.userAvatar,
      userProfile: res.data.data.userProfile,
      userRole: res.data.data.userRole,
    })
    editModalVisible.value = true
  } else {
    message.error(res.data?.message || 'Failed to load user')
  }
}

async function saveUser() {
  if (isEditing.value) {
    const res = await updateUser({
      id: editForm.id as number,
      userName: editForm.userName,
      userAvatar: editForm.userAvatar,
      userProfile: editForm.userProfile,
      userRole: editForm.userRole,
    })
    if (res.data?.code === 0) {
      message.success('Updated')
      editModalVisible.value = false
      // Optimistically update the edited row including edited time for display
      const updatedAt = new Date().toISOString()
      users.value = (users.value || []).map(
        (u: API.UserVO & { updateTime?: string; editTime?: string }) =>
          u.id === editForm.id
            ? {
                ...u,
                userName: editForm.userName ?? u.userName,
                userAvatar: editForm.userAvatar ?? u.userAvatar,
                userProfile: editForm.userProfile ?? u.userProfile,
                userRole: (editForm.userRole as string) ?? u.userRole,
                updateTime: updatedAt,
                editTime: updatedAt,
              }
            : u,
      )
      // Re-fetch to get backend authoritative timestamps
      fetchUsers()
    } else {
      message.error(res.data?.message || 'Update failed')
    }
  } else {
    const res = await addUser({
      userName: editForm.userName,
      userAccount: editForm.userAccount,
      userAvatar: editForm.userAvatar,
      userProfile: editForm.userProfile,
      userRole: editForm.userRole,
    })
    if (res.data?.code === 0) {
      message.success('Created')
      editModalVisible.value = false
      fetchUsers()
    } else {
      message.error(res.data?.message || 'Create failed')
    }
  }
}

async function confirmDelete(id: number | undefined) {
  if (!id) return
  if (confirm('Delete user?')) {
    const res = await deleteUser({ id: id.toString() })
    if (res.data?.code === 0) {
      message.success('Deleted')
      fetchUsers()
    } else {
      message.error(res.data?.message || 'Delete failed')
    }
  }
}

onMounted(fetchUsers)
</script>

<template>
  <div>
    <div class="bg-white border rounded-xl p-4">
      <div style="font-weight: 700; margin-bottom: 16px">User Management</div>
      <div class="flex items-end gap-3 mb-6">
        <div>
          <label class="block mb-1 text-sm">Username</label>
          <input
            v-model="query.userName"
            placeholder="Search by name"
            class="border rounded px-3 py-2"
          />
        </div>
        <div>
          <label class="block mb-1 text-sm">Account</label>
          <input
            v-model="query.userAccount"
            placeholder="Search by account"
            class="border rounded px-3 py-2"
          />
        </div>
        <div class="flex gap-2">
          <button class="px-3 py-2 rounded bg-emerald-600 text-white" @click="handleSearch">
            Search
          </button>
          <button class="px-3 py-2 rounded border" @click="openAdd">Add User</button>
        </div>
      </div>

      <div class="overflow-x-auto">
        <table class="min-w-full text-sm">
          <thead>
            <tr class="text-left">
              <th class="py-2 pr-4">ID</th>
              <th class="py-2 pr-4">Account</th>
              <th class="py-2 pr-4">Name</th>
              <th class="py-2 pr-4">Created</th>
              <th class="py-2 pr-4">Updated</th>
              <th class="py-2 pr-4">Edited</th>
              <th class="py-2 pr-4">Role</th>
              <th class="py-2">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="u in users" :key="u.id" class="border-t">
              <td class="py-2 pr-4">{{ u.id }}</td>
              <td class="py-2 pr-4">{{ u.userAccount }}</td>
              <td class="py-2 pr-4">{{ u.userName }}</td>
              <td class="py-2 pr-4">{{ formatDate((u as any).createTime) }}</td>
              <td class="py-2 pr-4">{{ formatDate((u as any).updateTime) || '-' }}</td>
              <td class="py-2 pr-4">{{ formatDate((u as any).editTime) || '-' }}</td>
              <td class="py-2 pr-4">{{ u.userRole }}</td>
              <td class="py-2">
                <div class="flex gap-2">
                  <button class="px-2 py-1 rounded border" @click="openEdit(u.id)">Edit</button>
                  <button class="px-2 py-1 rounded border" @click="confirmDelete(u.id)">
                    Delete
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="flex justify-end gap-2 mt-3">
        <button
          class="px-3 py-2 rounded border"
          @click="handlePageChange((query.pageNum || 1) - 1)"
          :disabled="(query.pageNum || 1) <= 1"
        >
          Prev
        </button>
        <div class="px-2 py-2 text-sm">
          Page {{ query.pageNum }} / {{ Math.max(1, Math.ceil(total / (query.pageSize || 10))) }}
        </div>
        <button
          class="px-3 py-2 rounded border"
          @click="handlePageChange((query.pageNum || 1) + 1)"
          :disabled="(query.pageNum || 1) >= Math.ceil(total / (query.pageSize || 10))"
        >
          Next
        </button>
      </div>
    </div>

    <div
      v-if="editModalVisible"
      class="fixed inset-0 bg-black/40 flex items-center justify-center p-4"
    >
      <div class="bg-white rounded-xl p-4 w-[520px] max-w-full">
        <div class="text-lg font-semibold mb-4">{{ isEditing ? 'Edit User' : 'Add User' }}</div>
        <div class="space-y-3">
          <div v-if="isEditing">
            <label class="block mb-1 text-sm">ID</label>
            <input
              :value="String(editForm.id || '')"
              disabled
              class="w-full border rounded px-3 py-2"
            />
          </div>
          <div>
            <label class="block mb-1 text-sm">Account</label>
            <input
              v-model="editForm.userAccount as string"
              :disabled="isEditing"
              class="w-full border rounded px-3 py-2"
            />
          </div>
          <div>
            <label class="block mb-1 text-sm">Name</label>
            <input v-model="editForm.userName as string" class="w-full border rounded px-3 py-2" />
          </div>
          <div>
            <label class="block mb-1 text-sm">Avatar URL</label>
            <input
              v-model="editForm.userAvatar as string"
              class="w-full border rounded px-3 py-2"
            />
          </div>
          <div>
            <label class="block mb-1 text-sm">Profile</label>
            <textarea
              v-model="editForm.userProfile as string"
              class="w-full border rounded px-3 py-2"
            />
          </div>
          <div>
            <label class="block mb-1 text-sm">Role</label>
            <select v-model="editForm.userRole as any" class="w-full border rounded px-3 py-2">
              <option v-for="r in roleOptions" :key="r.value" :value="r.value">
                {{ r.label }}
              </option>
            </select>
          </div>
        </div>
        <div class="flex justify-end gap-2 mt-4">
          <button class="px-3 py-2 rounded border" @click="editModalVisible = false">Cancel</button>
          <button class="px-3 py-2 rounded bg-emerald-600 text-white" @click="saveUser">
            Save
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
