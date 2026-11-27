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
        <div class="user-chip">
          <img :src="`https://api.dicebear.com/9.x/initials/svg?seed=${encodeURIComponent(user?.realName || 'Q')}&backgroundColor=0b1524`"
               class="user-avatar" alt="" />
          <div v-show="!asideCollapsed" class="user-meta">
            <div class="user-name">{{ user?.realName }}</div>
            <div class="user-title">{{ user?.title }}</div>
          </div>
          <el-dropdown trigger="click" @command="onUserCmd">
            <el-icon class="user-more" :size="16"><MoreFilled /></el-icon>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="switch">切换角色</el-dropdown-item>
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
          <span class="header-date">{{ today }}</span>
          <el-tag size="small" effect="plain" round class="env-tag">{{ mockTag }}</el-tag>
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
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ROLE_MENUS, findMenuTitle } from '@/config/menus'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const asideCollapsed = ref(false)
const user = computed(() => auth.user)
const menus = computed(() => ROLE_MENUS[auth.roleKey as keyof typeof ROLE_MENUS] ?? [])
const currentTitle = computed(() => findMenuTitle(user.value?.key, route.path) || (route.meta.title as string) || '')
const today = new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })
const mockTag = import.meta.env.VITE_USE_MOCK !== 'false' ? '数据服务 · 在线' : '生产环境'

function isActive(path: string) {
  return route.path === path || route.path.startsWith(path + '/')
}
function onClickMenu(m: { path: string; title: string; fullscreen?: boolean }) {
  if (m.fullscreen) {
    window.open(m.path, '_blank')
    return
  }
  router.push(m.path)
}
async function onUserCmd(cmd: string) {
  if (cmd === 'logout') {
    await auth.logout()
    router.push('/login')
  } else if (cmd === 'switch') {
    await auth.logout()
    router.push('/login?switch=1')
  }
}
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
.header-right { display: flex; align-items: center; gap: 14px; }
.header-date { font-size: 12.5px; color: var(--qn-text-2); }
.env-tag { font-size: 11px; }
.qn-content { flex: 1; padding: 20px 24px 32px; max-width: 1520px; width: 100%; margin: 0 auto; }
@media (max-width: 768px) {
  .qn-aside { position: fixed; transform: translateX(-100%); &.collapsed { transform: none; width: 68px; } }
  .header-date { display: none; }
}
</style>