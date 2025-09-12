import type { ClassValue } from 'clsx'
import type { Ref } from 'vue'
import { clsx } from 'clsx'
import { twMerge } from 'tailwind-merge'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function valueUpdater(updaterOrValue: any, ref: Ref<any>) {
  if (typeof updaterOrValue === 'function') {
    // call with current value
    // eslint-disable-next-line @typescript-eslint/ban-types
    const fn = updaterOrValue as Function
    ref.value = fn(ref.value)
  } else {
    ref.value = updaterOrValue
  }
}
