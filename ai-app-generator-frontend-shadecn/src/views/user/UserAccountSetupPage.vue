<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { toast } from 'vue-sonner'
import { useLoginUserStore } from '@/stores/loginUser'
import { updateUser, getLoginUser } from '@/api/userController'

const message = {
  success: (t: string) => toast.success(t),
  error: (t: string) => toast.error(t),
  warning: (t: string) => toast.warning(t),
}
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
    <div class="bg-white border rounded-xl p-4">
      <div style="font-weight: 700; margin-bottom: 16px">Account Settings</div>
      <div class="space-y-4">
        <div>
          <label class="block mb-1 text-sm">Account</label>
          <input :value="form.userAccount || ''" disabled class="w-full border rounded px-3 py-2" />
        </div>
        <div>
          <label class="block mb-1 text-sm">Name</label>
          <input
            v-model="form.userName"
            placeholder="Your name"
            class="w-full border rounded px-3 py-2"
          />
        </div>
        <div>
          <label class="block mb-1 text-sm">Avatar URL</label>
          <input
            v-model="form.userAvatar"
            placeholder="https://..."
            class="w-full border rounded px-3 py-2"
          />
        </div>
        <div>
          <label class="block mb-1 text-sm">Profile</label>
          <textarea
            v-model="form.userProfile"
            placeholder="About you"
            class="w-full border rounded px-3 py-2"
          />
        </div>
        <div class="flex justify-end">
          <button
            class="px-4 py-2 rounded bg-emerald-600 text-white disabled:opacity-50"
            :disabled="saving"
            @click="save"
          >
            Save
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
