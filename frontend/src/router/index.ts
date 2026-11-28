import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

/**
 * 路由设计（五端 + 公开页）
 * - 需要登录：/enterprise、/gov、/agency、/admin（守卫校验 Token 与角色）
 * - 免登录：/login、/verify/:token（公开验真页）、/checkin/:taskId（移动执法 H5 打卡）
 */
const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { public: true, title: '登录 · 黔能碳溯' }
  },
  // 公开验真页（扫码 /verify/:token 免登录进入）
  {
    path: '/verify/:token',
    name: 'Verify',
    component: () => import('@/views/verify/index.vue'),
    meta: { public: true, title: '碳足迹护照 · 验真', wide: true }
  },
  // 移动执法 H5 打卡（核查员手机打开，免登录 / 带任务令牌）
  {
    path: '/checkin/:taskId',
    name: 'Checkin',
    component: () => import('@/views/checkin/index.vue'),
    meta: { public: true, title: '执法打卡', wide: true }
  },
  // ============ 企业端 ============
  {
    path: '/enterprise',
    component: () => import('@/layout/MainLayout.vue'),
    meta: { roles: ['enterprise'] },
    children: [
      { path: 'dashboard', name: 'EntDashboard', component: () => import('@/views/enterprise/dashboard/index.vue'), meta: { title: '数据看板' } },
      { path: 'data-entry', name: 'EntDataEntry', component: () => import('@/views/enterprise/data-entry/index.vue'), meta: { title: '碳数据填报' } },
      { path: 'lab-report', name: 'EntLabReport', component: () => import('@/views/enterprise/lab-report/index.vue'), meta: { title: '化验单识别' } },
      { path: 'calc-report', name: 'EntCalcReport', component: () => import('@/views/enterprise/calc-report/index.vue'), meta: { title: '碳排核算与报告' } },
      { path: 'rectify', name: 'EntRectify', component: () => import('@/views/enterprise/rectify/index.vue'), meta: { title: '整改计划与预算' } },
      { path: 'ai', name: 'EntAi', component: () => import('@/views/enterprise/ai/index.vue'), meta: { title: 'AI 碳管家' } },
      { path: 'passport', name: 'EntPassport', component: () => import('@/views/enterprise/passport/index.vue'), meta: { title: '护照申请' } },
      { path: 'supply', name: 'EntSupply', component: () => import('@/views/enterprise/supply/index.vue'), meta: { title: '供应链协同' } },
      { path: 'logistics', name: 'EntLogistics', component: () => import('@/views/enterprise/logistics/index.vue'), meta: { title: '物流追踪' } },
      { path: 'finance', name: 'EntFinance', component: () => import('@/views/enterprise/finance/index.vue'), meta: { title: '绿色金融' } },
      { path: 'export', name: 'EntExport', component: () => import('@/views/enterprise/export/index.vue'), meta: { title: '出海数据包' } }
    ]
  },
  // ============ 省厅监管端 ============
  {
    path: '/gov',
    component: () => import('@/layout/MainLayout.vue'),
    meta: { roles: ['gov'] },
    children: [
      { path: 'dashboard', name: 'GovDashboard', component: () => import('@/views/gov/dashboard/index.vue'), meta: { title: '监管工作台' } },
      { path: 'review', name: 'GovReview', component: () => import('@/views/gov/review/index.vue'), meta: { title: '智能初审台' } },
      { path: 'dispatch', name: 'GovDispatch', component: () => import('@/views/gov/dispatch/index.vue'), meta: { title: '核查派单' } },
      { path: 'factors', name: 'GovFactors', component: () => import('@/views/gov/factors/index.vue'), meta: { title: '因子库管理' } },
      { path: 'seal', name: 'GovSeal', component: () => import('@/views/gov/seal/index.vue'), meta: { title: '意见书回传' } },
      { path: 'city/:name', name: 'GovCityDetail', component: () => import('@/views/gov/city-detail/index.vue'), meta: { title: '市州碳排放详情' } }
    ]
  },
  // 态势大屏：独立全屏路由（无侧边栏）
  {
    path: '/gov/big-screen',
    name: 'GovBigScreen',
    component: () => import('@/views/gov/big-screen/index.vue'),
    meta: { roles: ['gov'], title: '贵州省碳排放态势感知大屏', fullscreen: true }
  },
  // ============ 核查机构端 ============
  {
    path: '/agency',
    component: () => import('@/layout/MainLayout.vue'),
    meta: { roles: ['agency'] },
    children: [
      { path: 'dashboard', name: 'AgeDashboard', component: () => import('@/views/agency/dashboard/index.vue'), meta: { title: '数据包接收' } },
      { path: 'review', name: 'AgeReview', component: () => import('@/views/agency/review/index.vue'), meta: { title: '国际标准复核' } },
      { path: 'passport', name: 'AgePassport', component: () => import('@/views/agency/passport/index.vue'), meta: { title: '护照签发' } }
    ]
  },
  // ============ 超级管理员端 ============
  // ============ 国家级审批部门端（终审发证 · 宏观监管） ============
  {
    path: '/national',
    component: () => import('@/layout/MainLayout.vue'),
    meta: { roles: ['national'] },
    children: [
      { path: 'issuance', name: 'NatIssuance', component: () => import('@/views/national/issuance/index.vue'), meta: { title: '终审与发证中心' } },
      { path: 'monitor', name: 'NatMonitor', component: () => import('@/views/national/monitor/index.vue'), meta: { title: '跨国互认监控' } },
      { path: 'forecast', name: 'NatForecast', component: () => import('@/views/national/forecast/index.vue'), meta: { title: '宏观预测驾驶舱' } },
      { path: 'tariff', name: 'NatTariff', component: () => import('@/views/national/tariff/index.vue'), meta: { title: '国际关税预警' } },
      { path: 'blacklist', name: 'NatBlacklist', component: () => import('@/views/national/blacklist/index.vue'), meta: { title: '黑名单通报系统' } }
    ]
  },  {
    path: '/admin',
    component: () => import('@/layout/MainLayout.vue'),
    meta: { roles: ['admin'] },
    children: [
      { path: 'dashboard', name: 'AdmDashboard', component: () => import('@/views/admin/dashboard/index.vue'), meta: { title: '系统监控看板' } },
      { path: 'rbac', name: 'AdmRbac', component: () => import('@/views/admin/rbac/index.vue'), meta: { title: 'RBAC 权限控制中心' } },
      { path: 'audit-center', name: 'AdmAuditCenter', component: () => import('@/views/admin/audit-center/index.vue'), meta: { title: '全链路操作审计与可信存证中心' } },
      { path: 'dict', name: 'AdmDict', component: () => import('@/views/admin/dict/index.vue'), meta: { title: '数据字典与基础配置' } },
      { path: 'monitor', name: 'AdmMonitor', component: () => import('@/views/admin/monitor/index.vue'), meta: { title: '系统运行与接口监控' } }
    ]
  },
  // ============ 个人信息（五端通用） ============
  {
    path: '/profile',
    component: () => import('@/layout/MainLayout.vue'),
    meta: { roles: ['enterprise', 'gov', 'agency', 'national', 'admin'], title: '个人信息' },
    children: [
      { path: '', name: 'Profile', component: () => import('@/views/profile/index.vue'), meta: { title: '个人信息' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/login' }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

// 全局守卫：未登录 → 登录页；角色不符 → 登录页（演示模式按 localStorage 用户判断）
router.beforeEach((to) => {
  const auth = useAuthStore()
  document.title = (to.meta.title ? to.meta.title + ' · ' : '') + '黔能碳溯'
  if (to.meta.public) return true
  if (!auth.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  const roles = to.matched.flatMap((r) => (r.meta?.roles as string[]) ?? [])
  if (roles.length && !roles.includes(auth.user?.key ?? '')) {
    // 角色无权访问：跳回该用户自己的首页
    return { path: auth.user?.homePath || '/login' }
  }
  return true
})

export default router