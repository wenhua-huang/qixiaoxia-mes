const PREFIX = 'QXX'
export type QrType = 'CARD' | 'MAT' | 'ROLL' | 'WO' | 'PKG'
const ALL_TYPES: QrType[] = ['CARD', 'MAT', 'ROLL', 'WO', 'PKG']

export function buildQrPayload(type: QrType, code: string): string {
  return `${PREFIX}|${type}|${code}`
}

export function parseQrPayload(raw: string): { type: QrType; code: string } | null {
  if (!raw) return null
  const parts = raw.split('|')
  if (parts.length < 3 || parts[0] !== PREFIX) return null
  if (!ALL_TYPES.includes(parts[1] as QrType)) return null
  const code = parts.slice(2).join('|')
  if (!code) return null
  return { type: parts[1] as QrType, code }
}

export const buildCardPayload = (cardCode: string) => buildQrPayload('CARD', cardCode)

export const buildMatPayload = (batchCode: string) => buildQrPayload('MAT', batchCode)
