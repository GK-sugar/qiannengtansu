import type { RoleKey } from '@/utils/demo'

export interface MenuItem {
  path: string
  title: string
  icon: string
  badge?: number
  fullscreen?: boolean
  children?: MenuItem[]
}

/** 四端侧边导航（由角色驱动，侧边栏 + 面包屑共用） */
export const ROLE_MENUS: Record<RoleKey, MenuItem[]> = {
  enterprise: [
    { path: '/enterprise/dashboard', title: '数据看板', icon: 'Odometer' },
    { path: '/enterprise/data-entry', title: '碳数据填报', icon: 'EditPen' },
    { path: '/enterprise/lab-report', title: '化验单识别', icon: 'Camera' },
    { path: '/enterprise/calc-report', title: '碳排核算与报告', icon: 'TrendCharts' },
    { path: '/enterprise/rectify', title: '整改计划与预算', icon: 'DocumentChecked' },
    { path: '/enterprise/ai', title: 'AI 碳管家', icon: 'ChatDotRound' },
    { path: '/enterprise/passport', title: '护照申请', icon: 'Stamp' },
    { path: '/enterprise/supply', title: '供应链协同', icon: 'Connection' },
    { path: '/enterprise/logistics', title: '物流追踪', icon: 'Van' },
    { path: '/enterprise/finance', title: '绿色金融', icon: 'Money' },
    { path: '/enterprise/export', title: '出海数据包', icon: 'Download' }
  ],
  gov: [
    { path: '/gov/dashboard', title: '监管工作台', icon: 'Monitor' },
    { path: '/gov/big-screen', title: '态势感知大屏', icon: 'DataBoard', fullscreen: true },
    { path: '/gov/review', title: '智能初审台', icon: 'Warning' },
    { path: '/gov/dispatch', title: '核查派单', icon: 'Position' },
    { path: '/gov/factors', title: '因子库管理', icon: 'Collection' },
    { path: '/gov/seal', title: '电子签章', icon: 'Stamp' }
  ],
  agency: [
    { path: '/agency/dashboard', title: '数据包接收', icon: 'Box' },
    { path: '/agency/review', title: '国际标准复核', icon: 'Checked' },
    { path: '/agency/passport', title: '护照签发', icon: 'Medal' }
  ],
  national: [
    { path: '/national/issuance', title: '终审与发证中心', icon: 'Medal' },
    { path: '/national/monitor', title: '跨国互认监控', icon: 'Connection' },
    { path: '/national/forecast', title: '宏观预测驾驶舱', icon: 'TrendCharts' },
    { path: '/national/tariff', title: '国际关税预警', icon: 'Warning' },
    { path: '/national/blacklist', title: '黑名单通报系统', icon: 'Lock' }
  ],  admin: [
    { path: '/admin/dashboard', title: '系统监控看板', icon: 'Odometer' },
    { path: '/admin/rbac', title: 'RBAC 权限控制中心', icon: 'Lock' },
    { path: '/admin/audit-center', title: '全链路操作审计与可信存证中心', icon: 'Document' },
    { path: '/admin/dict', title: '数据字典与基础配置', icon: 'Collection' },
    { path: '/admin/monitor', title: '系统运行与接口监控', icon: 'Cpu' }
  ]
}

export function findMenuTitle(role: RoleKey | undefined, path: string): string {
  for (const m of ROLE_MENUS[role ?? 'enterprise'] ?? []) {
    if (m.path === path) return m.title
    for (const c of m.children ?? []) if (c.path === path) return c.title
  }
  return ''
}