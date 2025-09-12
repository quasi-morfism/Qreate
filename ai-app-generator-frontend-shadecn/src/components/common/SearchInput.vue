<template>
  <div class="search-input">
    <Input
      :model-value="modelValue"
      :placeholder="placeholder"
      class="search-field"
      @update:model-value="handleInput"
      @keydown.enter="handleSearch"
    >
      <template #suffix>
        <Button 
          variant="ghost" 
          size="sm"
          class="search-button"
          :disabled="loading"
          @click="handleSearch"
        >
          <div v-if="loading" class="animate-spin">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 2v4l2.5-2.5L16 5l-4 4-4-4 1.5-1.5L12 6V2z"/>
              <path opacity="0.5" d="M12 22v-4l-2.5 2.5L8 19l4-4 4 4-1.5 1.5L12 18v4z"/>
            </svg>
          </div>
          <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
            <path
              d="M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"
            />
          </svg>
        </Button>
      </template>
    </Input>
  </div>
</template>

<script setup lang="ts">
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'

interface Props {
  modelValue: string
  placeholder?: string
  loading?: boolean
}

interface Emits {
  (e: 'update:modelValue', value: string): void
  (e: 'search'): void
  (e: 'clear'): void
}

const props = withDefaults(defineProps<Props>(), {
  placeholder: 'Search...',
  loading: false,
})

const emit = defineEmits<Emits>()

const handleInput = (value: string | number) => {
  const stringValue = String(value)
  emit('update:modelValue', stringValue)
  // 如果清空了输入，触发清空事件
  if (!stringValue.trim()) {
    emit('clear')
  }
}

const handleSearch = () => {
  if (!props.loading) {
    emit('search')
  }
}
</script>

<style scoped>
.search-input {
  @apply relative;
}

.search-field {
  @apply pr-12;
}

.search-button {
  @apply text-gray-500 hover:text-gray-700;
}
</style>