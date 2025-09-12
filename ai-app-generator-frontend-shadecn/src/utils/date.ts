export const formatDate = (dateString?: string | null): string => {
  if (!dateString) return 'N/A'
  
  try {
    return new Date(dateString).toLocaleDateString([], {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    })
  } catch {
    console.warn('Invalid date format:', dateString)
    return 'Invalid Date'
  }
}

export const formatRelativeTime = (dateString?: string | null): string => {
  if (!dateString) return 'N/A'
  
  try {
    const now = new Date()
    const date = new Date(dateString)
    const diffInSeconds = Math.floor((now.getTime() - date.getTime()) / 1000)
    
    if (diffInSeconds < 60) return 'Just now'
    if (diffInSeconds < 3600) return `${Math.floor(diffInSeconds / 60)} minutes ago`
    if (diffInSeconds < 86400) return `${Math.floor(diffInSeconds / 3600)} hours ago`
    if (diffInSeconds < 2592000) return `${Math.floor(diffInSeconds / 86400)} days ago`
    
    return formatDate(dateString)
  } catch {
    return formatDate(dateString)
  }
}