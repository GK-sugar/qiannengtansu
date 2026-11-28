/**
 * 对外可访问地址：桌面浏览器用 localhost 时，手机扫码打不开 127.0.0.1，
 * 因此开发环境自动向 Vite 请求本机局域网 IP（/__lan），拼成 http://192.168.x.x:端口。
 * 生产环境域名直连，直接返回 location.origin。管理员也可手动覆盖（localStorage）。
 */
const LOCAL_HOSTS = ['localhost', '127.0.0.1', '0.0.0.0', '::1']
const OVERRIDE_KEY = 'qnct_public_origin'

export const isLocalOrigin = LOCAL_HOSTS.includes(location.hostname)

let cached: Promise<string> | null = null

export function publicOrigin(): Promise<string> {
  const override = (localStorage.getItem(OVERRIDE_KEY) || '').trim()
  if (override) return Promise.resolve(override.replace(/\/+$/, ''))
  if (!isLocalOrigin) return Promise.resolve(location.origin)
  if (!cached) {
    cached = fetch('/__lan', { cache: 'no-store' })
      .then((r) => (r.ok ? r.json() : Promise.reject(new Error('lan unavailable'))))
      .then((j: { ip?: string; port?: number }) => (j.ip ? `http://${j.ip}:${location.port || j.port || 5175}` : location.origin))
      .catch(() => location.origin)
  }
  return cached
}

export function setPublicOrigin(value: string) {
  const v = value.trim().replace(/\/+$/, '')
  if (v) localStorage.setItem(OVERRIDE_KEY, v)
  else localStorage.removeItem(OVERRIDE_KEY)
  cached = null
}

export function getPublicOverride(): string {
  return (localStorage.getItem(OVERRIDE_KEY) || '').trim()
}