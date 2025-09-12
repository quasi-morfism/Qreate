const AVATAR_BASE_URL = 'https://avatar.iran.liara.run/public'

export const getDefaultAvatar = (userId?: string | number | null): string => {
  if (!userId) return AVATAR_BASE_URL
  
  // Generate consistent seed based on user ID
  const seed = String(userId)
    .split('')
    .reduce((acc, char) => acc + char.charCodeAt(0), 0)
  
  return `${AVATAR_BASE_URL}/${(seed % 100) + 1}`
}

export const getAvatarUrl = (userAvatar?: string | null, userId?: string | number | null): string => {
  return userAvatar || getDefaultAvatar(userId)
}