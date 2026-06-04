/**
 * 将后端时间字符串（如 "08:00_06-15"）解析为 Date 对象
 */
export function parseBackendTime(value) {
  if (!value) return null

  const normalized = value.replace('_', ' ')
  const [timePart, datePart] = normalized.split(' ')
  if (!timePart || !datePart) return null

  const [month, day] = datePart.split('-').map(Number)
  const [hour, minute] = timePart.split(':').map(Number)
  if ([month, day, hour, minute].some(Number.isNaN)) return null

  const year = new Date().getFullYear()
  return new Date(year, month - 1, day, hour, minute)
}

/**
 * 格式化为 "YYYY-MM-DD HH:mm"
 */
export function formatDateTime(date) {
  if (!(date instanceof Date) || Number.isNaN(date.getTime())) return ''
  const pad = (n) => `${n}`.padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

/**
 * 格式化为 "YYYY-MM-DD"
 */
export function formatDateOnly(date) {
  if (!(date instanceof Date) || Number.isNaN(date.getTime())) return ''
  const pad = (n) => `${n}`.padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

/**
 * 格式化首发时间（只取时间部分 "HH:mm"）
 */
export function formatStartTime(value) {
  if (!value) return '-'
  const [timePart] = String(value).split(/[_ ]/)
  return timePart || '-'
}

/**
 * 分钟数转可读时长，如 95 -> "1小时35分钟"
 */
export function formatDuration(minutes) {
  if (minutes === null || minutes === undefined || Number.isNaN(Number(minutes))) return '-'
  const total = Number(minutes)
  const hours = Math.floor(total / 60)
  const remainMinutes = total % 60
  if (hours === 0) return `${remainMinutes}分钟`
  return `${hours}小时${remainMinutes}分钟`
}

/**
 * 根据余票数量返回状态类型（用于 el-tag / 颜色标识）
 */
export function getSeatStatus(seatNum) {
  if (seatNum <= 0) return { type: 'danger', label: '无票', color: '#ef4444' }
  if (seatNum <= 5) return { type: 'warning', label: '紧张', color: '#f97316' }
  if (seatNum <= 20) return { type: '', label: '较少', color: '#eab308' }
  return { type: 'success', label: '充足', color: '#22c55e' }
}

/**
 * 构造后端需要的时间字符串格式 "HH:mm MM-DD"
 */
export function toBackendTime(date) {
  if (!(date instanceof Date) || Number.isNaN(date.getTime())) return ''
  const pad = (n) => `${n}`.padStart(2, '0')
  const time = `${pad(date.getHours())}:${pad(date.getMinutes())}`
  const dateStr = `${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
  return `${time} ${dateStr}`
}
