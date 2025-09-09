<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useMessage } from 'naive-ui'
import { useLoginUserStore } from '@/stores/loginUser'
import { updateUser, getLoginUser } from '@/api/userController'

const message = useMessage()
const loginUserStore = useLoginUserStore()

const saving = ref(false)
const form = reactive<{
  id?: number
  userName: string
  userAvatar: string
  userProfile: string
  userAccount?: string
}>({
  id: undefined,
  userName: '',
  userAvatar: '',
  userProfile: '',
  userAccount: '',
})

async function loadCurrentUser() {
  // Prefer store if already loaded
  if (loginUserStore.loginUser?.id) {
    const u = loginUserStore.loginUser as API.LoginUserVO
    form.id = u.id
    form.userName = u.userName || ''
    form.userAvatar = u.userAvatar || ''
    form.userProfile = u.userProfile || ''
    form.userAccount = u.userAccount || ''
    return
  }
  // Otherwise fetch
  const res = await getLoginUser()
  if (res.data?.code === 0 && res.data.data) {
    const u = res.data.data
    loginUserStore.setLoginUser(u)
    form.id = u.id
    form.userName = u.userName || ''
    form.userAvatar = u.userAvatar || ''
    form.userProfile = u.userProfile || ''
    form.userAccount = u.userAccount || ''
  }
}

async function save() {
  if (!form.id) {
    message.warning('Please log in first')
    return
  }
  saving.value = true
  try {
    const res = await updateUser({
      id: form.id,
      userName: form.userName,
      userAvatar: form.userAvatar,
      userProfile: form.userProfile,
    })
    if (res.data?.code === 0) {
      message.success('Profile updated')
      await loginUserStore.fetchLoginUser()
    } else {
      message.error(res.data?.message || 'Update failed')
    }
  } catch {
    message.error('Update failed')
  } finally {
    saving.value = false
  }
}

onMounted(loadCurrentUser)
</script>

<template>
  <div>
    <n-card :bordered="false">
      <template #header>
        <div style="font-weight: 700">Account Settings</div>
      </template>

      <n-form label-placement="left" label-width="120">
        <n-form-item label="Account">
          <n-input :value="form.userAccount || ''" disabled />
        </n-form-item>
        <n-form-item label="Name">
          <n-input v-model:value="form.userName" placeholder="Your name" />
        </n-form-item>
        <n-form-item label="Avatar URL">
          <n-input v-model:value="form.userAvatar" placeholder="https://..." />
        </n-form-item>
        <n-form-item label="Profile">
          <n-input v-model:value="form.userProfile" type="textarea" placeholder="About you" />
        </n-form-item>
        <div style="display: flex; justify-content: flex-end; gap: 12px">
          <n-button type="primary" :loading="saving" @click="save">Save</n-button>
        </div>
      </n-form>
    </n-card>
  </div>
</template>
