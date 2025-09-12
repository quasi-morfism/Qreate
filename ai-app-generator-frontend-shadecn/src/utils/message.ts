import { toast } from 'vue-sonner'

export const useMessage = () => ({
  success: (text: string) => toast.success(text),
  error: (text: string) => toast.error(text),
  warning: (text: string) => toast.warning(text),
  info: (text: string) => toast.info(text),
})

export default useMessage