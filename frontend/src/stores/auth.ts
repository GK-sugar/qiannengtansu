import { defineStore } from 'pinia'
import request from '@/utils/request'
import { demoUserMap, getPassword } from '@/utils/demo'
import type { DemoUser, RoleKey } from '@/utils/demo'

const TOKEN_KEY = 'qnct_token'
const USER_KEY = 'qnct_user'

interface AuthState {
  token: string
  user: DemoUser | null
}

/** 认证与用户信息。演示模式（VITE_USE_MOCK=true）本地校验；对接后端后走 /api/v1/auth/login。 */
export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  }),
  getters: {
    isLoggedIn: (s) => !!s.token && !!s.user,
    roleKey: (s): RoleKey | '' => s.user?.key ?? ''
  },
  actions: {
    async login(username: string, password: string) {
      const useMock = import.meta.env.VITE_USE_MOCK !== 'false'
      if (useMock) {
        // 后端在线时优先真实登录拿 JWT，否则化验单读图/入库等业务接口会 401 跳回登录页
        try {
          return await this.realLogin(username, password)
        } catch (e: any) {
          const status = e?.response?.status
          if (status === 401 || status === 400) throw new Error('账号或密码错误，请使用下方演示账号快速登录')
          // 后端未启动/不可达：降级本地 mock 校验
        }
        const user = Object.values(demoUserMap).find(
          (u) => u.username === username && getPassword(u.username) === password
        )
        if (!user) throw new Error('账号或密码错误，请使用下方演示账号快速登录')
        this.setSession('mock-token-' + user.key + Date.now(), user)
        return user
      }
      return this.realLogin(username, password)
    },
    async realLogin(username: string, password: string) {
      // 真实后端：Spring Boot /api/v1/auth/login 返回 { token, user: UserInfoDto }
      // 后端 user 字段：id/username/realName/roleKey/orgId/orgName/dataScope/homePath
      const env = await request.post('/v1/auth/login', { username, password }) as { data?: { token: string; user: Record<string, unknown> }; token?: string; user?: Record<string, unknown> }
      const res = env?.data ?? env
      const raw = res?.user ?? {}
      const roleKey = ((raw.roleKey as string) || (raw.key as string) || 'enterprise') as RoleKey
      const demo = demoUserMap[roleKey]
      const user: DemoUser = demo
        ? {
            ...demo,
            realName: (raw.realName as string) || demo.realName,
            orgName: (raw.orgName as string) || demo.orgName,
            homePath: (raw.homePath as string) || demo.homePath
          }
        : {
            key: roleKey,
            label: roleKey,
            desc: '',
            username,
            password,
            realName: (raw.realName as string) || username,
            orgName: (raw.orgName as string) || '',
            title: '',
            homePath: (raw.homePath as string) || '/',
            color: '#00b581'
          }
      this.setSession(res.token, user)
      return user
    },
    setSession(token: string, user: DemoUser) {
      this.token = token
      this.user = user
      localStorage.setItem(TOKEN_KEY, token)
      localStorage.setItem(USER_KEY, JSON.stringify(user))
    },
    async logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    }
  }
})