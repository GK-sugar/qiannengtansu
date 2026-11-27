/**
 * 演示模式：五端角色与账号配置
 * 说明：当前阶段前端以内置 Mock 数据运行，真实对接后端后切换 VITE_USE_MOCK=false；
 *       登录接口将走 /api/v1/auth/login（后端已按 MySQL DDL 建好用户体系）。
 */
export type RoleKey = 'enterprise' | 'gov' | 'agency' | 'national' | 'admin'

export interface DemoUser {
  key: RoleKey
  label: string
  desc: string
  username: string
  password: string
  realName: string
  orgName: string
  title: string
  homePath: string
  color: string
}

export const DEMO_USERS: DemoUser[] = [
  {
    key: 'enterprise',
    label: '企业端',
    desc: '煤矿 / 新能源材料工厂',
    username: 'enterprise',
    password: 'ent888',
    realName: '王总',
    orgName: '贵州黔能矿业有限责任公司',
    title: '碳管理部负责人',
    homePath: '/enterprise/dashboard',
    color: '#00b581'
  },
  {
    key: 'gov',
    label: '省厅监管端',
    desc: '省级审批部门',
    username: 'gov',
    password: 'gov888',
    realName: '李处长',
    orgName: '贵州省生态环境厅',
    title: '应对气候变化处',
    homePath: '/gov/big-screen',
    color: '#4da3ff'
  },
  {
    key: 'agency',
    label: '核查机构端',
    desc: '国际合规复核',
    username: 'agency',
    password: 'agency888',
    realName: '陈主任',
    orgName: '中环低碳国际核查中心',
    title: '审核部',
    homePath: '/agency/dashboard',
    color: '#a06bff'
  },
  {
    key: 'national',
    label: '国家级审批部门',
    desc: '终审发证 · 跨国互认',
    username: 'national',
    password: 'nat888',
    realName: '周司长',
    orgName: '国家应对气候变化战略中心',
    title: '终审发证与宏观监管处',
    homePath: '/national/issuance',
    color: '#ff5f56'
  },
  {
    key: 'admin',
    label: '超级管理员',
    desc: '平台运维与安全',
    username: 'superadmin',
    password: 'admin888',
    realName: '系统管理员',
    orgName: '黔能碳溯平台',
    title: '超级管理员',
    homePath: '/admin/dashboard',
    color: '#ffb020'
  }
]

export const demoUserMap: Record<RoleKey, DemoUser> = {
  enterprise: DEMO_USERS[0],
  gov: DEMO_USERS[1],
  agency: DEMO_USERS[2],
  national: DEMO_USERS[3],
  admin: DEMO_USERS[4]
}

/* ============ 管理员重置密码覆盖表（localStorage 持久化，重置后登录立即生效） ============ */
const PWD_KEY = 'qnct_pwd_overrides'

function readOverrides(): Record<string, string> {
  try { return JSON.parse(localStorage.getItem(PWD_KEY) || '{}') } catch { return {} }
}

export function getPassword(username: string): string | undefined {
  const overrides = readOverrides()
  if (overrides[username]) return overrides[username]
  return Object.values(DEMO_USERS).find((u) => u.username === username)?.password
}

export function setPasswordOverride(username: string, password: string) {
  const overrides = readOverrides()
  overrides[username] = password
  localStorage.setItem(PWD_KEY, JSON.stringify(overrides))
}