<template>
  <div class="qn-layout">
    <!-- ======== 侧边栏：深碳底 + 翡翠绿高亮 ======== -->
    <aside class="qn-aside" :class="{ collapsed: asideCollapsed }">
      <div class="aside-brand" @click="$router.push(user?.homePath || '/')">
        <img src="@/assets/logo/logo-mark.svg" alt="logo" class="brand-logo" />
        <div v-show="!asideCollapsed" class="brand-text">
          <div class="brand-name">黔能碳溯</div>
          <div class="brand-sub">GUZHOU · CARBON TRACE</div>
        </div>
      </div>

      <div class="aside-role" v-show="!asideCollapsed">
        <el-tag :color="user?.color + '22'" :style="{ color: user?.color, borderColor: user?.color + '55' }" size="small" round>
          {{ user?.label }}
        </el-tag>
        <span class="role-org">{{ user?.orgName }}</span>
      </div>

      <el-scrollbar class="aside-menu">
        <template v-for="m in menus" :key="m.path">
          <div class="menu-item"
               :class="{ active: isActive(m.path) }"
               @click="onClickMenu(m)">
            <el-icon :size="18"><component :is="m.icon" /></el-icon>
            <span v-show="!asideCollapsed" class="menu-label">{{ m.title }}</span>
            <span v-show="!asideCollapsed && m.badge" class="menu-badge">{{ m.badge }}</span>
          </div>
        </template>
      </el-scrollbar>

      <div class="aside-footer">
        <div class="user-chip" @click="router.push('/profile')">
          <img :src="`https://api.dicebear.com/9.x/initials/svg?seed=${encodeURIComponent(user?.realName || 'Q')}&backgroundColor=0b1524`"
               class="user-avatar" alt="" />
          <div v-show="!asideCollapsed" class="user-meta">
            <div class="user-name">{{ user?.realName }}</div>
            <div class="user-title">{{ user?.title }}</div>
          </div>
          <el-dropdown trigger="click" @command="onUserCmd" @click.stop>
            <el-icon class="user-more" :size="16"><MoreFilled /></el-icon>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人信息</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </aside>

    <!-- ======== 右侧主体 ======== -->
    <div class="qn-main">
      <header class="qn-header">
        <div class="header-left">
          <el-icon class="collapse-btn" :size="18" @click="asideCollapsed = !asideCollapsed">
            <Expand v-if="asideCollapsed" /><Fold v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item class="crumb-label">{{ user?.label }}</el-breadcrumb-item>
            <el-breadcrumb-item class="crumb-now">{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-popover placement="bottom-end" :width="360" trigger="click" @show="loadNotifications">
            <template #reference>
              <div class="notify-bell" title="站内通知">
                <el-badge :value="unread" :hidden="unread === 0" :max="99">
                  <el-icon :size="18"><Bell /></el-icon>
                </el-badge>
              </div>
            </template>
            <div class="notify-head">
              <b>站内通知</b>
              <el-button link type="primary" size="small" :disabled="unread === 0" @click="markAllRead">全部已读</el-button>
            </div>
            <div v-loading="notifyLoading" class="notify-list">
              <div v-if="!notifications.length" class="notify-empty">暂无通知</div>
              <div v-for="n in notifications" :key="n.id" class="notify-item"
                   :class="{ unread: n.readFlag === 0 }" @click="readOne(n)">
                <div class="n-title">{{ n.title }}</div>
                <div class="n-content">{{ n.content }}</div>
                <div class="n-time">{{ fmtNotifyTime(n.createTime) }}</div>
              </div>
            </div>
          </el-popover>
          <FormulaHelp />
          <div class="header-clock">
            <span class="hc-label"><i class="hc-dot"></i>此刻</span>
            <span class="hc-time">{{ liveTime }}</span>
            <span class="hc-div"></span>
            <span class="hc-date">{{ liveDate }}</span>
          </div>
          <span class="env-pill"><i class="dot"></i>{{ mockTag }}</span>
        </div>
      </header>
      <main class="qn-content">
        <router-view v-slot="{ Component }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Bell } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { ROLE_MENUS, findMenuTitle } from '@/config/menus'
import FormulaHelp from '@/components/FormulaHelp.vue'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const asideCollapsed = ref(false)
const user = computed(() => auth.user)
const menus = computed(() => ROLE_MENUS[auth.roleKey as keyof typeof ROLE_MENUS] ?? [])
const currentTitle = computed(() => findMenuTitle(user.value?.key, route.path) || (route.meta.title as string) || '')
const liveTime = ref('')
const liveDate = ref('')
let clockTimer = 0
function tickLiveClock() {
  const now = new Date()
  liveTime.value = now.toLocaleTimeString('zh-CN', { hour12: false })
  liveDate.value = `${now.getMonth() + 1}月${now.getDate()}日 周${'日一二三四五六'[now.getDay()]}`
}
const mockTag = import.meta.env.VITE_USE_MOCK !== 'false' ? '数据服务 · 在线' : '生产环境'

/* ---------- 站内通知中心（全站顶栏铃铛） ---------- */
interface NotifyItem {
  id: number
  bizType: string
  title: string
  content: string
  readFlag: number
  createTime: string
}
const unread = ref(0)
const notifications = ref<NotifyItem[]>([])
const notifyLoading = ref(false)
let notifyTimer: ReturnType<typeof setInterval> | null = null

async function loadUnread() {
  try {
    const res: any = await request.get('/v1/notify/unread-count')
    unread.value = res?.data?.count ?? 0
  } catch {
    unread.value = 0
  }
}
async function loadNotifications() {
  notifyLoading.value = true
  try {
    const res: any = await request.get('/v1/notify/list', { params: { page: 1, size: 8 } })
    notifications.value = res?.data?.records ?? []
    unread.value = notifications.value.filter((n) => n.readFlag === 0).length
  } catch {
    notifications.value = []
  } finally {
    notifyLoading.value = false
  }
}
async function readOne(n: NotifyItem) {
  if (n.readFlag === 1) return
  n.readFlag = 1
  unread.value = Math.max(0, unread.value - 1)
  try {
    await request.post(`/v1/notify/${n.id}/read`)
  } catch {
    /* 已读状态本地先行，失败不打断阅读 */
  }
}
async function markAllRead() {
  notifications.value.forEach((n) => (n.readFlag = 1))
  unread.value = 0
  try {
    await request.post('/v1/notify/read-all')
  } catch {
    /* ignore */
  }
}
function fmtNotifyTime(t?: string) {
  return t ? String(t).replace('T', ' ').slice(0, 16) : ''
}

function isActive(path: string) {
  return route.path === path || route.path.startsWith(path + '/')
}
function onClickMenu(m: { path: string; title: string; fullscreen?: boolean }) {
  if (m.fullscreen) {
    const city = route.query.city
    window.open(m.path + (city ? '?city=' + encodeURIComponent(String(city)) : ''), '_blank')
    return
  }
  router.push(m.path)
}
async function onUserCmd(cmd: string) {
  if (cmd === 'logout') {
    await auth.logout()
    router.push('/login')
  } else if (cmd === 'profile') {
    router.push('/profile')
  }
}

onMounted(() => {
  tickLiveClock()
  clockTimer = window.setInterval(tickLiveClock, 1000)
  loadUnread()
  notifyTimer = setInterval(loadUnread, 60_000)
})
onBeforeUnmount(() => {
  clearInterval(clockTimer)
  if (notifyTimer) clearInterval(notifyTimer)
})
</script>

<style scoped lang="scss">
.qn-layout {
  display: flex;
  min-height: 100vh;
  background: var(--qn-bg);
}
/* ---------- 侧边栏 ---------- */
.qn-aside {
  width: 232px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  color: #c9d4e3;
  background: linear-gradient(180deg, #071222 0%, #060d1b 100%);
  border-right: 1px solid rgba(255, 255, 255, 0.06);
  transition: width 0.25s ease;
  position: sticky;
  top: 0;
  height: 100vh;
  z-index: 10;
  &.collapsed { width: 68px; }
}
.aside-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 18px 16px;
  cursor: pointer;
  .brand-logo { width: 36px; height: 36px; filter: drop-shadow(0 0 10px rgba(0, 181, 129, 0.55)); }
  .brand-name {
    font-size: 18px;
    font-weight: 700;
    letter-spacing: 3px;
    background: linear-gradient(90deg, #2bd8a2, #00d1ff);
    -webkit-background-clip: text;
    background-clip: text;
    color: transparent;
  }
  .brand-sub { font-size: 9px; letter-spacing: 1.5px; color: #4d607a; margin-top: 2px; }
}
.aside-role {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 16px 14px;
  .role-org { font-size: 11px; color: #5c6e8a; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
}
.aside-menu { flex: 1; padding: 0 10px; }
.menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 42px;
  padding: 0 12px;
  margin: 4px 0;
  border-radius: 10px;
  cursor: pointer;
  color: #8da0ba;
  font-size: 13.5px;
  transition: all 0.2s ease;
  position: relative;
  &:hover { color: #fff; background: rgba(0, 181, 129, 0.08); }
  &.active {
    color: #fff;
    background: linear-gradient(120deg, rgba(0, 181, 129, 0.22), rgba(0, 209, 255, 0.14));
    box-shadow: inset 0 0 0 1px rgba(0, 209, 255, 0.25);
    &::before {
      content: '';
      position: absolute;
      left: -10px;
      top: 50%;
      transform: translateY(-50%);
      width: 3px;
      height: 20px;
      border-radius: 3px;
      background: var(--qn-gradient);
    }
  }
  .menu-label { flex: 1; white-space: nowrap; }
  .menu-badge {
    min-width: 18px;
    height: 18px;
    line-height: 18px;
    text-align: center;
    font-size: 11px;
    border-radius: 9px;
    color: #fff;
    background: #ff4d4f;
  }
}
.aside-footer { padding: 12px; border-top: 1px solid rgba(255, 255, 255, 0.06); }
.user-chip {
  display: flex;
  align-items: center;
  gap: 10px;
  border-radius: 10px;
  padding: 8px;
  cursor: pointer;
  &:hover { background: rgba(255, 255, 255, 0.04); }
  .user-avatar { width: 34px; height: 34px; border-radius: 50%; border: 1px solid rgba(0, 209, 255, 0.4); flex-shrink: 0; }
  .user-meta { flex: 1; min-width: 0; }
  .user-name { font-size: 13px; color: #fff; }
  .user-title { font-size: 11px; color: #5c6e8a; }
  .user-more { color: #5c6e8a; }
}
/* ---------- 主体 ---------- */
.qn-main { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.qn-header {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--qn-border);
  position: sticky;
  top: 0;
  z-index: 9;
}
.header-left { display: flex; align-items: center; gap: 14px; }
.collapse-btn { color: var(--qn-text-2); cursor: pointer; &:hover { color: var(--qn-primary); } }
.crumb-label { color: var(--qn-text-3); }
.crumb-now { color: var(--qn-text); font-weight: 600; }
.header-right { display: flex; align-items: center; gap: 10px; flex-wrap: nowrap; min-width: 0; }
.header-right > * { flex: none; }
.header-clock {
  display: inline-flex; align-items: center; gap: 10px;
  height: 34px; padding: 0 13px; border-radius: 11px;
  background: linear-gradient(150deg, #0d3040, #08222d);
  border: 1px solid rgba(126, 240, 194, 0.22);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08), 0 6px 16px rgba(2, 16, 20, 0.16);
  color: #eafff7; white-space: nowrap; font-variant-numeric: tabular-nums;
  .hc-label { display: inline-flex; align-items: center; gap: 5px; font-size: 10px; letter-spacing: 1.5px; color: #8fe5c6; }
  .hc-dot { width: 6px; height: 6px; border-radius: 50%; background: #2bd8a2; animation: env-pulse 2s ease infinite; }
  .hc-time {
    font-family: ui-monospace, Consolas, monospace;
    font-size: 16px; font-weight: 700; letter-spacing: .5px;
    background: linear-gradient(120deg, #ffffff 15%, #b7ffe4 100%);
    -webkit-background-clip: text; background-clip: text; color: transparent;
  }
  .hc-div { width: 1px; height: 16px; background: linear-gradient(180deg, transparent, rgba(126, 240, 194, 0.45), transparent); }
  .hc-date { font-size: 11px; color: #a9d9c8; }
}
.env-pill {
  display: inline-flex; align-items: center; gap: 7px;
  padding: 6px 12px; border-radius: 999px;
  font-size: 12px; font-weight: 700; letter-spacing: .2px;
  color: #00795c; background: linear-gradient(135deg, #e9fbf4, #eefaff);
  border: 1px solid #b8ebd9;
  white-space: nowrap;
  .dot { width: 7px; height: 7px; border-radius: 50%; background: #00b581; box-shadow: 0 0 0 0 rgba(0,181,129,.45); animation: env-pulse 1.8s ease infinite; flex: none; }
}
@keyframes env-pulse { 0%,100% { box-shadow: 0 0 0 0 rgba(0,181,129,.45); } 50% { box-shadow: 0 0 0 5px rgba(0,181,129,0); } }
.notify-bell {
  display: flex; align-items: center; justify-content: center; width: 30px; height: 30px;
  border-radius: 8px; color: var(--qn-text-2); cursor: pointer; transition: background .2s, color .2s;
  &:hover { background: rgba(0, 209, 255, 0.08); color: var(--qn-primary); }
}
.notify-head {
  display: flex; align-items: center; justify-content: space-between; padding-bottom: 8px;
  border-bottom: 1px solid #eef2f7; margin-bottom: 4px; font-size: 13px; color: var(--qn-text);
}
.notify-list { max-height: 360px; overflow-y: auto; }
.notify-empty { padding: 26px 0; text-align: center; font-size: 12.5px; color: var(--qn-text-3); }
.notify-item {
  padding: 10px 8px; border-radius: 8px; cursor: pointer; transition: background .2s;
  &:hover { background: #f5f9fd; }
  &.unread .n-title::before {
    content: ''; display: inline-block; width: 6px; height: 6px; border-radius: 50%;
    background: #00b581; margin-right: 6px; vertical-align: 1px;
  }
  .n-title { font-size: 12.5px; font-weight: 600; color: var(--qn-text); }
  .n-content {
    font-size: 11.5px; color: var(--qn-text-3); margin-top: 3px; line-height: 1.55;
    display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
  }
  .n-time { font-size: 10.5px; color: #9aa9bb; margin-top: 4px; }
}
.qn-content { flex: 1; padding: 20px 24px 32px; max-width: 1520px; width: 100%; margin: 0 auto; }
@media (max-width: 1100px) {
  .header-clock .hc-date, .header-clock .hc-div { display: none; }
}
@media (max-width: 900px) {
  .crumb-label { display: none; }
}
@media (max-width: 768px) {
  .qn-aside { position: fixed; transform: translateX(-100%); &.collapsed { transform: none; width: 68px; } }
}
@media (max-width: 700px) {
  .env-pill { display: none; }
  .qn-content { padding: 16px 14px 26px; }
}
@media (max-width: 560px) {
  .header-clock .hc-label { display: none; }
  .header-right { gap: 8px; }
  :deep(.fh-trigger.el-button) { height: 30px; padding: 0 8px; font-size: 12px; }
  .crumb-label { display: none; }
  .qn-header { padding: 0 14px; }
}
</style>