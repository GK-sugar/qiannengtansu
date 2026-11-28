/**
 * 摘要计算：优先 Web Crypto（HTTPS / localhost），
 * 局域网 HTTP 等非安全上下文没有 crypto.subtle 时降级为确定性摘要，避免页面报错卡死。
 */
function fnv1a(s: string, seed: number): number {
  let h = (2166136261 ^ seed) >>> 0
  for (let i = 0; i < s.length; i++) {
    h ^= s.charCodeAt(i)
    h = Math.imul(h, 16777619) >>> 0
  }
  return h >>> 0
}

export function fallbackHashHex(text: string): string {
  let out = ''
  for (let i = 0; i < 8; i++) out += fnv1a(text, (i * 0x9e3779b9) >>> 0).toString(16).padStart(8, '0')
  return out
}

export async function sha256Hex(text: string): Promise<string> {
  try {
    if (globalThis.crypto?.subtle) {
      const buf = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(text))
      return Array.from(new Uint8Array(buf), (x) => x.toString(16).padStart(2, '0')).join('')
    }
  } catch { /* 非安全上下文，走降级摘要 */ }
  return fallbackHashHex(text)
}
