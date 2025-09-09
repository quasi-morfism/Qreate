<script setup lang="ts">
import { onMounted, reactive, ref, h } from 'vue'
import { useMessage, useDialog, NButton, NSpace } from 'naive-ui'
import {
  listUserVoByPage,
  addUser,
  updateUser,
  deleteUser,
  getUserById,
} from '@/api/userController'

const message = useMessage()
const dialog = useDialog()

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
      h(
        NSpace,
        { size: 8 },
        {
          default: () => [
            h(
              NButton,
              { quaternary: true, size: 'tiny', onClick: () => openEdit(row.id) },
              { default: () => 'Edit' },
            ),
            h(
              NButton,
              { quaternary: true, size: 'tiny', onClick: () => confirmDelete(row.id) },
              { default: () => 'Delete' },
            ),
          ],
        },
      ),
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

function confirmDelete(id: number | undefined) {
  if (!id) return
  dialog.warning({
    title: 'Delete user',
    content: 'Are you sure you want to delete this user?',
    positiveText: 'Delete',
    negativeText: 'Cancel',
    onPositiveClick: async () => {
      const res = await deleteUser({ id })
      if (res.data?.code === 0) {
        message.success('Deleted')
        fetchUsers()
      } else {
        message.error(res.data?.message || 'Delete failed')
      }
    },
  })
}

onMounted(fetchUsers)
</script>

<template>
  <div>
    <n-card :bordered="false">
      <template #header>
        <div style="font-weight: 700">User Management</div>
      </template>
      <n-form inline :show-feedback="false" style="margin-bottom: 32px">
        <n-form-item label="Username">
          <n-input v-model:value="query.userName" placeholder="Search by name" />
        </n-form-item>
        <n-form-item label="Account">
          <n-input v-model:value="query.userAccount" placeholder="Search by account" />
        </n-form-item>
        <n-form-item>
          <n-button type="primary" @click="handleSearch">Search</n-button>
        </n-form-item>
        <n-form-item>
          <n-button @click="openAdd">Add User</n-button>
        </n-form-item>
      </n-form>

      <n-data-table
        :loading="loading"
        :columns="columns"
        :data="users"
        :pagination="false"
        :bordered="false"
      />

      <div style="display: flex; justify-content: flex-end; margin-top: 12px">
        <n-pagination
          :page="query.pageNum || 1"
          :page-size="query.pageSize || 10"
          :page-count="Math.ceil(total / (query.pageSize || 10))"
          :page-sizes="[10, 20, 50]"
          show-size-picker
          @update:page="handlePageChange"
          @update:page-size="handlePageSizeChange"
        />
      </div>
    </n-card>

    <n-modal
      v-model:show="editModalVisible"
      preset="card"
      :title="isEditing ? 'Edit User' : 'Add User'"
    >
      <n-form label-placement="left" label-width="100">
        <n-form-item v-if="isEditing" label="ID">
          <n-input :value="String(editForm.id || '')" disabled />
        </n-form-item>
        <n-form-item label="Account">
          <n-input v-model:value="editForm.userAccount as string" :disabled="isEditing" />
        </n-form-item>
        <n-form-item label="Name">
          <n-input v-model:value="editForm.userName as string" />
        </n-form-item>
        <n-form-item label="Avatar URL">
          <n-input v-model:value="editForm.userAvatar as string" />
        </n-form-item>
        <n-form-item label="Profile">
          <n-input v-model:value="editForm.userProfile as string" type="textarea" />
        </n-form-item>
        <n-form-item label="Role">
          <n-select v-model:value="editForm.userRole as any" :options="roleOptions" />
        </n-form-item>
        <div style="display: flex; gap: 12px; justify-content: flex-end">
          <n-button @click="editModalVisible = false">Cancel</n-button>
          <n-button type="primary" @click="saveUser">Save</n-button>
        </div>
      </n-form>
    </n-modal>
  </div>
</template>
