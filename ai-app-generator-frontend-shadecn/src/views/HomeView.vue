<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { addApp, listMyAppVoByPage, listGoodAppVoByPage } from '@/api/appController'
import { Button } from '@/components/ui/button'
import { Avatar, AvatarImage, AvatarFallback } from '@/components/ui/avatar'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Pagination } from '@/components/ui/pagination'
import { AppCard, SearchInput, EmptyState, LoadingSpinner } from '@/components/common'
import { useMessage } from '@/utils'
import { useLoginUserStore } from '@/stores/loginUser'

const router = useRouter()
const loginUserStore = useLoginUserStore()

const message = useMessage()

const heroSection = ref<HTMLElement>()

// State
const searchText = ref('')
const placeholderText = ref('')
const isCreating = ref(false)
const myApps = ref<API.AppVO[]>([])
const selectedApps = ref<API.AppVO[]>([])
const myAppsLoading = ref(false)
const selectedAppsLoading = ref(false)
const myAppsPage = ref(1)
const selectedAppsPage = ref(1)
const myAppsTotal = ref(0)
const selectedAppsTotal = ref(0)
const myAppsSearchText = ref('')
const selectedAppsSearchText = ref('')

// Quick action helper to set preset prompt into the hero textarea
const setHeroPrompt = (text: string) => {
  searchText.value = text
}

// Format selector for generation
const adaptFormat = ref<'html' | 'multi_file'>('html')

// Create app
const handleCreateApp = async () => {
  if (!searchText.value.trim()) {
    message.warning('Please enter your app idea')
    return
  }

  if (!loginUserStore.loginUser.id) {
    message.warning('Please login first')
    return
  }

  isCreating.value = true
  try {
    const res = await addApp({
      initPrompt: searchText.value,
      codeGenType: adaptFormat.value,
    })

    if (res.data?.code === 0 && res.data.data) {
      message.success('App created successfully!')
      router.push({
        path: `/app/generate/${res.data.data}`,
        query: { adapt: adaptFormat.value, auto: '1' },
      })
    } else {
      message.error(res.data?.message || 'Failed to create app')
    }
  } catch (error) {
    message.error('Failed to create app')
    console.error('Create app error:', error)
  } finally {
    isCreating.value = false
  }
}

// Load my apps
const loadMyApps = async () => {
  if (!loginUserStore.loginUser.id) return

  myAppsLoading.value = true
  try {
    const res = await listMyAppVoByPage({
      pageNum: myAppsPage.value,
      pageSize: 20,
      appName: myAppsSearchText.value || undefined,
    })

    if (res.data?.code === 0 && res.data.data) {
      myApps.value = res.data.data.records || []
      myAppsTotal.value = res.data.data.totalRow || 0
    }
  } catch (error) {
    console.error('Load my apps error:', error)
  } finally {
    myAppsLoading.value = false
  }
}

// Load selected apps
const loadSelectedApps = async () => {
  selectedAppsLoading.value = true
  try {
    const res = await listGoodAppVoByPage({
      pageNum: selectedAppsPage.value,
      pageSize: 20,
      appName: selectedAppsSearchText.value || undefined,
    })

    if (res.data?.code === 0 && res.data.data) {
      selectedApps.value = res.data.data.records || []
      selectedAppsTotal.value = res.data.data.totalRow || 0
    }
  } catch (error) {
    console.error('Load selected apps error:', error)
  } finally {
    selectedAppsLoading.value = false
  }
}

// Handle app click
const handleAppClick = (app: API.AppVO) => {
  router.push(`/app/detail/${app.id}`)
}

// Search handlers
const handleMyAppsSearch = () => {
  myAppsPage.value = 1
  loadMyApps()
}

const handleSelectedAppsSearch = () => {
  selectedAppsPage.value = 1
  loadSelectedApps()
}

// Pagination handlers
const handleMyAppsPageChange = (page: number) => {
  myAppsPage.value = page
  loadMyApps()
}

const handleSelectedAppsPageChange = (page: number) => {
  selectedAppsPage.value = page
  loadSelectedApps()
}

// Clear handlers for search inputs
const handleMyAppsClear = () => {
  if (myAppsSearchText.value) {
    myAppsPage.value = 1
    loadMyApps()
  }
}

const handleSelectedAppsClear = () => {
  if (selectedAppsSearchText.value) {
    selectedAppsPage.value = 1
    loadSelectedApps()
  }
}

// Focus search input
const focusSearchInput = () => {
  const searchInput = document.querySelector('.search-textarea') as HTMLTextAreaElement
  if (searchInput) {
    searchInput.focus()
  }
}

// Typewriter effect for placeholder
const typewriterText = 'Start Qreating Here...'
let typewriterIndex = 0
let typewriterTimeout: ReturnType<typeof setTimeout>

const typewriterEffect = () => {
  if (typewriterIndex < typewriterText.length) {
    placeholderText.value += typewriterText.charAt(typewriterIndex)
    typewriterIndex++
    typewriterTimeout = setTimeout(typewriterEffect, 150)
  } else {
    setTimeout(() => {
      placeholderText.value = ''
      typewriterIndex = 0
      typewriterTimeout = setTimeout(typewriterEffect, 500)
    }, 3000)
  }
}

// Watch for login status changes
watch(
  () => loginUserStore.loginUser.id,
  (newId, oldId) => {
    if (newId && newId !== oldId) {
      // User just logged in, refresh My Works
      loadMyApps()
    }
  },
)

onMounted(() => {
  loadMyApps()
  loadSelectedApps()
  typewriterEffect()
})

onUnmounted(() => {
  if (typewriterTimeout) {
    clearTimeout(typewriterTimeout)
  }
})
</script>

<template>
  <div class="home-container">
    <!-- Hero Section -->
    <div ref="heroSection" class="hero-section">
      <!-- Floating Orbs inside Hero -->
      <div class="floating-orbs">
        <div class="orb orb-1"></div>
        <div class="orb orb-2"></div>
        <div class="orb orb-3"></div>
        <div class="orb orb-4"></div>
        <div class="orb orb-5"></div>
      </div>
      <div class="hero-content">
        <div class="hero-icon">
          <Avatar class="w-20 h-20">
            <AvatarImage src="/logo.png" />
            <AvatarFallback>Q</AvatarFallback>
          </Avatar>
        </div>
        <h1 class="hero-title">Qreate Anything You Want</h1>
        <p class="hero-subtitle">Chat with AI to easily build applications and websites</p>

        <!-- Search Input -->
        <div class="search-container">
          <textarea
            v-model="searchText"
            :placeholder="placeholderText"
            rows="3"
            class="search-textarea"
            @keydown.ctrl.enter="handleCreateApp"
          ></textarea>
          <div class="search-actions">
            <div class="search-hints">
              <Button variant="ghost" size="sm" class="hint-button">📄 Upload</Button>
              <Button variant="ghost" size="sm" class="hint-button">💡 Optimize</Button>
            </div>
            <div class="right-actions">
              <Select v-model="adaptFormat">
                <SelectTrigger class="format-trigger">
                  <SelectValue placeholder="Format" />
                </SelectTrigger>
                <SelectContent align="end">
                  <SelectItem value="html">HTML</SelectItem>
                  <SelectItem value="multi_file">Multi-file</SelectItem>
                </SelectContent>
              </Select>
              <Button
                size="lg"
                :disabled="isCreating"
                @click="handleCreateApp"
                class="create-button"
                :aria-busy="isCreating ? 'true' : 'false'"
              >
                <span v-if="!isCreating">Generate</span>
                <span v-else class="wave" aria-hidden="true">
                  <span class="char" style="animation-delay: 0s">G</span>
                  <span class="char" style="animation-delay: 0.06s">e</span>
                  <span class="char" style="animation-delay: 0.12s">n</span>
                  <span class="char" style="animation-delay: 0.18s">e</span>
                  <span class="char" style="animation-delay: 0.24s">r</span>
                  <span class="char" style="animation-delay: 0.3s">a</span>
                  <span class="char" style="animation-delay: 0.36s">t</span>
                  <span class="char" style="animation-delay: 0.42s">e</span>
                </span>
              </Button>
            </div>
          </div>
        </div>

        <!-- Quick Actions -->
        <div class="quick-actions">
          <Button
            variant="secondary"
            class="quick-action"
            @click="
              setHeroPrompt(
                'Design a wave-themed landing page with animated SVG waves, hero headline, CTA button, feature grid, responsive layout, subtle gradients, and modern typography for a creative product.',
              )
            "
          >
            Wave Style Homepage
          </Button>
          <Button
            variant="secondary"
            class="quick-action"
            @click="
              setHeroPrompt(
                'Create a professional corporate website with hero banner, services overview, about section, case studies, testimonials, contact form, accessibility, SEO-friendly, responsive navigation.',
              )
            "
          >
            Corporate Website
          </Button>
          <Button
            variant="secondary"
            class="quick-action"
            @click="
              setHeroPrompt(
                'Build an e-commerce admin dashboard for product CRUD, inventory tracking, order management, user roles, analytics charts, filters, pagination, responsive tables, dark mode.',
              )
            "
          >
            E-commerce Backend
          </Button>
          <Button
            variant="secondary"
            class="quick-action"
            @click="
              setHeroPrompt(
                'Develop a tech community site with posts, tags, comments, user profiles, upvotes, search, pagination, markdown support, moderation tools, responsive, clean UI.',
              )
            "
          >
            Cool Tech Community
          </Button>
        </div>
      </div>
    </div>

    <!-- My Apps Section -->
    <div class="apps-section" v-if="loginUserStore.loginUser.id">
      <div class="section-header">
        <h2 class="section-title">My Works</h2>
        <div class="section-search">
          <SearchInput
            v-model="myAppsSearchText"
            placeholder="Search my apps..."
            @search="handleMyAppsSearch"
            @clear="handleMyAppsClear"
          />
        </div>
      </div>

      <div class="apps-grid" v-if="!myAppsLoading && myApps.length > 0">
        <AppCard
          v-for="app in myApps" 
          :key="app.id" 
          :app="app"
          :show-time="true"
          @click="handleAppClick"
        />
      </div>

      <div v-else-if="myAppsLoading" class="loading-container">
        <LoadingSpinner text="Loading your apps..." center />
      </div>

      <EmptyState 
        v-else 
        type="apps"
        title="No apps created yet"
        description="Start building your first application with AI assistance"
        action-text="Create New App"
        @action="focusSearchInput"
      />

      <div v-if="myAppsTotal > 20" class="pagination-container">
        <Pagination
          v-model:page="myAppsPage"
          :total="myAppsTotal"
          :items-per-page="20"
          :sibling-count="1"
          @update:page="handleMyAppsPageChange"
        />
      </div>
    </div>

    <!-- Selected Apps Section -->
    <div class="apps-section">
      <div class="section-header">
        <h2 class="section-title">Featured Examples</h2>
        <div class="section-search">
          <SearchInput
            v-model="selectedAppsSearchText"
            placeholder="Search featured apps..."
            @search="handleSelectedAppsSearch"
            @clear="handleSelectedAppsClear"
          />
        </div>
      </div>

      <div class="apps-grid" v-if="!selectedAppsLoading && selectedApps.length > 0">
        <AppCard
          v-for="app in selectedApps" 
          :key="app.id" 
          :app="app"
          variant="featured"
          :show-author="true"
          @click="handleAppClick"
        />
      </div>

      <div v-else-if="selectedAppsLoading" class="loading-container">
        <LoadingSpinner text="Loading featured apps..." center />
      </div>

      <EmptyState 
        v-else 
        type="apps"
        title="No featured apps available"
        description="Check back later for new featured applications"
      />

      <div v-if="selectedAppsTotal > 20" class="pagination-container">
        <Pagination
          v-model:page="selectedAppsPage"
          :total="selectedAppsTotal"
          :items-per-page="20"
          :sibling-count="1"
          @update:page="handleSelectedAppsPageChange"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.home-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  position: relative;
}

.floating-orbs {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 1;
}

.orb {
  position: absolute;
  border-radius: 50%;
  background: linear-gradient(45deg, rgba(34, 197, 94, 0.08), rgba(16, 185, 129, 0.12));
  filter: blur(1px);
}

.orb-1 {
  width: 120px;
  height: 120px;
  top: 15%;
  left: 10%;
  animation: drift-1 20s ease-in-out infinite;
}

.orb-2 {
  width: 80px;
  height: 80px;
  top: 30%;
  right: 15%;
  animation: drift-2 25s ease-in-out infinite;
}

.orb-3 {
  width: 60px;
  height: 60px;
  bottom: 20%;
  left: -30px;
  animation: drift-3 18s ease-in-out infinite;
}

.orb-4 {
  width: 100px;
  height: 100px;
  top: 60%;
  right: 25%;
  animation: drift-4 22s ease-in-out infinite;
}

.orb-5 {
  width: 40px;
  height: 40px;
  top: 45%;
  left: 50%;
  animation: drift-5 15s ease-in-out infinite;
}

.hero-section {
  text-align: center;
  padding: 80px 0;
  background: linear-gradient(135deg, #f0fdf4 0%, #ecfdf5 50%, #f0fdf4 100%);
  border-radius: 24px;
  margin-bottom: 80px;
  position: relative;
  overflow: hidden;
}

.hero-content {
  position: relative;
  z-index: 2;
}

.hero-icon {
  margin-bottom: 24px;
}

.hero-title {
  font-size: 48px;
  font-weight: 700;
  margin-bottom: 16px;
  background: linear-gradient(135deg, #064e3b, #059669, #10b981, #34d399);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  line-height: 1.2;
  background-size: 200% 200%;
  animation: shimmer 3s ease-in-out infinite;
}

@keyframes shimmer {
  0% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0% 50%;
  }
}

.hero-subtitle {
  font-size: 20px;
  color: #6b7280;
  margin-bottom: 48px;
  font-weight: 400;
}

.search-container {
  max-width: 800px;
  margin: 0 auto 32px;
  position: relative;
  background-color: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 28px;
  padding: 20px 24px;
  box-shadow:
    0 8px 24px rgba(0, 0, 0, 0.04),
    0 4px 12px rgba(0, 0, 0, 0.02);
  transition:
    transform 0.25s ease,
    box-shadow 0.25s ease,
    border-color 0.25s ease,
    background-color 0.25s ease;
}
.search-container:hover {
  transform: translateY(-1px);
  box-shadow:
    0 12px 32px rgba(0, 0, 0, 0.08),
    0 6px 16px rgba(0, 0, 0, 0.04);
}
.search-container:focus-within {
  background-color: #ffffff;
  border-color: rgba(24, 160, 88, 0.3);
  box-shadow:
    0 0 0 3px rgba(24, 160, 88, 0.12),
    0 12px 32px rgba(0, 0, 0, 0.06),
    0 8px 16px rgba(24, 160, 88, 0.04);
  transform: translateY(-2px);
}

.search-textarea {
  width: 100%;
  margin-bottom: 16px;
  background-color: transparent;
  border: none;
  outline: none;
  resize: none;
  font-size: 16px;
  line-height: 1.5;
  font-family: inherit;
  color: inherit;
  padding: 8px 0;
  border-radius: 20px;
  min-height: 72px;
}

.search-textarea::placeholder {
  color: rgba(0, 0, 0, 0.4);
}

.search-textarea:focus {
  outline: none;
  border: none;
}

.search-actions {
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: center;
  gap: 12px;
}

.right-actions {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.format-select {
  --n-border-radius: 18px !important;
}
.format-select :deep(.n-base-selection) {
  border-radius: 18px !important;
}
.format-select :deep(.n-base-selection-label) {
  padding: 0 12px !important;
}
.format-select :deep(.n-base-selection .n-base-selection__border),
.format-select :deep(.n-base-selection .n-base-selection__state-border) {
  border-radius: 18px !important;
}

.search-hints {
  display: flex;
  gap: 12px;
}

.search-hints {
  display: inline-flex;
  gap: 12px;
}

.hint-button {
  color: #6b7280;
  font-size: 14px;
}

.format-trigger {
  width: 140px;
}

.create-button {
  padding: 0 32px;
  height: 44px;
  border-radius: 22px;
  font-weight: 600;
  background: linear-gradient(135deg, #22c55e, #16a34a);
  border: none;
  box-shadow: 0 4px 14px 0 rgba(34, 197, 94, 0.3);
  transition: all 0.2s;
  min-width: 140px; /* lock width to avoid jitter between states */
  display: inline-flex;
  align-items: center;
  justify-content: center;
  white-space: nowrap;
}

.create-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px 0 rgba(34, 197, 94, 0.4);
}

.wave {
  display: inline-flex;
  gap: 1px;
}
.wave .char {
  display: inline-block;
  transform-origin: center 80%;
  animation: wave-bounce 0.9s ease-in-out infinite;
}
@keyframes wave-bounce {
  0%,
  100% {
    transform: translateY(0);
    opacity: 1;
  }
  50% {
    transform: translateY(-2.5px);
    opacity: 0.95;
  }
}

.quick-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
  flex-wrap: wrap;
}

.quick-action {
  padding: 8px 16px;
  border-radius: 20px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.18), rgba(255, 255, 255, 0.08));
  border: 1px solid rgba(255, 255, 255, 0.35);
  color: #374151;
  font-size: 14px;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease,
    background 0.2s ease;
  backdrop-filter: blur(10px) saturate(150%);
  -webkit-backdrop-filter: blur(10px) saturate(150%);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.45),
    0 8px 20px rgba(17, 24, 39, 0.08);
}

.quick-action:hover {
  background: #ffffff;
  border-color: rgba(24, 160, 88, 0.35);
  transform: translateY(-1px);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.5),
    0 12px 28px rgba(17, 24, 39, 0.12);
}

.apps-section {
  margin-bottom: 80px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
}

.section-title {
  font-size: 32px;
  font-weight: 700;
  color: #1f2937;
  margin: 0;
}

.section-search {
  width: 300px;
}

.apps-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
  margin-bottom: 32px;
}

.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}

@media (max-width: 768px) {
  .home-container {
    padding: 0 16px;
  }

  .hero-section {
    padding: 40px 20px;
    margin-bottom: 40px;
  }

  .hero-title {
    font-size: 32px;
  }

  .hero-subtitle {
    font-size: 16px;
  }

  .section-header {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }

  .section-search {
    width: 100%;
  }

  .apps-grid {
    grid-template-columns: 1fr;
  }

  .quick-actions {
    gap: 8px;
  }

  .quick-action {
    font-size: 12px;
    padding: 6px 12px;
  }
}

@keyframes drift-1 {
  0%,
  100% {
    transform: translate(0, 0) rotate(0deg);
  }
  25% {
    transform: translate(30px, -20px) rotate(90deg);
  }
  50% {
    transform: translate(-20px, 40px) rotate(180deg);
  }
  75% {
    transform: translate(50px, 20px) rotate(270deg);
  }
}

@keyframes drift-2 {
  0%,
  100% {
    transform: translate(0, 0) scale(1);
  }
  33% {
    transform: translate(-40px, 30px) scale(1.1);
  }
  66% {
    transform: translate(20px, -50px) scale(0.9);
  }
}

@keyframes drift-3 {
  0%,
  100% {
    transform: translate(0, 0);
    opacity: 0.6;
  }
  50% {
    transform: translate(60px, -30px);
    opacity: 1;
  }
}

@keyframes drift-4 {
  0%,
  100% {
    transform: translate(0, 0) rotate(0deg) scale(1);
  }
  25% {
    transform: translate(-30px, -40px) rotate(45deg) scale(1.2);
  }
  50% {
    transform: translate(40px, 30px) rotate(90deg) scale(0.8);
  }
  75% {
    transform: translate(-50px, 10px) rotate(135deg) scale(1.1);
  }
}

@keyframes drift-5 {
  0%,
  100% {
    transform: translate(0, 0);
  }
  20% {
    transform: translate(25px, -15px);
  }
  40% {
    transform: translate(-30px, -25px);
  }
  60% {
    transform: translate(35px, 20px);
  }
  80% {
    transform: translate(-20px, 30px);
  }
}
</style>
