<template>
  <div class="page">
    <div class="hero-banner">
      <div class="title">个人信息</div>
      <div class="sub">{{ user?.label }} · {{ user?.orgName }} ｜ 账号资料与登录安全</div>
    </div>

    <div class="pf-grid">
      <!-- 身份名片 -->
      <div class="pf-card">
        <div class="pf-cover"><span class="pf-cover-mark">QIANNENG · CARBON TRACE</span></div>
        <div class="pf-body">
          <div class="pf-head">
            <div class="pf-avatar">
              <span>{{ initial }}</span>
              <i class="pf-online"></i>
            </div>
            <div class="pf-id">
              <div class="pf-name">{{ user?.realName }}</div>
              <div class="pf-subline">
                <span class="pf-title">{{ user?.title }}</span>
                <el-tag round size="small" :color="(user?.color || '#00b581') + '1f'" :style="{ color: user?.color, borderColor: (user?.color || '#00b581') + '66' }">
                  {{ user?.label }}
                </el-tag>
              </div>
            </div>
          </div>

          <div class="pf-rows">
            <div class="pf-row"><span>登录账号</span><b>{{ user?.username }}</b></div>
            <div class="pf-row"><span>所属单位</span><b>{{ user?.orgName }}</b></div>
            <div class="pf-row"><span>岗位</span><b>{{ user?.title }}</b></div>
            <div class="pf-row"><span>数据范围</span><b>{{ scopeText }}</b></div>
            <div class="pf-row"><span>默认工作台</span><b>{{ homeText }}</b></div>
          </div>

          <div class="pf-actions">
            <el-button type="primary" @click="router.push(user?.homePath || '/')">返回工作台</el-button>
            <el-button plain type="danger" @click="onLogout">退出登录</el-button>
          </div>
        </div>
      </div>

      <div class="pf-side">
        <!-- 账户安全 -->
        <div class="page-card">
          <div class="page-card-title"><h3>账户与登录安全</h3></div>
          <div class="sec-row">
            <div class="sr-main">
              <div class="sr-name">登录密码</div>
              <div class="sr-desc">建议每 90 天更换一次，修改后需重新登录</div>
            </div>
            <el-button size="small" plain @click="pwdVisible = true">修改密码</el-button>
          </div>
          <div class="sec-row">
            <div class="sr-main">
              <div class="sr-name">当前登录</div>
              <div class="sr-desc">本机 · 浏览器会话 · {{ loginTime }}</div>
            </div>
            <el-tag size="small" type="success" round>在线</el-tag>
          </div>
          <div class="sec-row">
            <div class="sr-main">
              <div class="sr-name">登录凭证</div>
              <div class="sr-desc">账号密码登录，签发 JWT；退出后本地凭证即清除</div>
            </div>
            <el-tag size="small" type="info" round>JWT</el-tag>
          </div>
          <div class="sec-note"><el-icon><Lock /></el-icon>关键操作（核算、签发、派单）均写入操作审计与哈希链存证。</div>
        </div>
      </div>
    </div>

    <el-dialog v-model="pwdVisible" title="修改登录密码" width="460px">
      <el-form label-width="90px">
        <el-form-item label="原密码"><el-input v-model="pwd.old" type="password" show-password /></el-form-item>
        <el-form-item label="新密码"><el-input v-model="pwd.next" type="password" show-password /></el-form-item>
        <el-form-item label="确认新密码"><el-input v-model="pwd.confirm" type="password" show-password /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" @click="onChangePwd">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { ROLE_MENUS } from '@/config/menus'
import type { RoleKey } from '@/utils/demo'

const router = useRouter()
const auth = useAuthStore()
const user = computed(() => auth.user)

const initial = computed(() => (user.value?.realName || 'Q').slice(0, 1))
const menus = computed(() => ROLE_MENUS[(auth.roleKey as RoleKey) || 'enterprise'] ?? [])

const scopeMap: Record<string, string> = {
  enterprise: '企业级 · 本企业全量数据',
  gov: '省级 · 全省 2,384 家企业',
  agency: '受委托范围 · 待复核数据包',
  national: '国家级 · 全国汇总与终审',
  admin: '全平台 · 运维与安全'
}
const scopeText = computed(() => scopeMap[user.value?.key ?? ''] ?? '按角色授权')
const homeText = computed(() => {
  const m = menus.value.find((x) => x.path === user.value?.homePath)
  return m?.title ?? user.value?.homePath ?? '-'
})
const loginTime = computed(() => new Date().toLocaleString('zh-CN', { hour12: false }))

async function onLogout() {
  await auth.logout()
  router.push('/login')
}

const pwdVisible = ref(false)
const pwd = reactive({ old: '', next: '', confirm: '' })
function onChangePwd() {
  if (!pwd.old || !pwd.next || !pwd.confirm) return ElMessage.warning('请完整填写原密码与新密码')
  if (pwd.next !== pwd.confirm) return ElMessage.warning('两次输入的新密码不一致')
  if (pwd.next.length < 6) return ElMessage.warning('新密码至少 6 位')
  pwdVisible.value = false
  pwd.old = pwd.next = pwd.confirm = ''
  ElMessage.success('密码修改已提交：下次登录使用新密码（演示环境不写库）')
}
</script>

<style scoped lang="scss">
.pf-grid { display: grid; grid-template-columns: minmax(0, 4fr) minmax(0, 7fr); gap: 16px; align-items: start; }
.pf-side { display: flex; flex-direction: column; gap: 16px; min-width: 0; }

.pf-card {
  min-width: 0;
  background: var(--qn-card);
  border: 1px solid var(--qn-border);
  border-radius: var(--qn-radius);
  box-shadow: var(--qn-shadow);
  overflow: hidden;
}
.pf-cover {
  position: relative;
  height: 108px;
  overflow: hidden;
  background:
    radial-gradient(300px 170px at 88% -40%, rgba(0, 209, 255, 0.36), transparent 66%),
    linear-gradient(118deg, #04222c 0%, #06483e 52%, #05604e 100%);
  &::before {
    content: '';
    position: absolute; inset: 0;
    background-image:
      linear-gradient(rgba(255, 255, 255, 0.05) 1px, transparent 1px),
      linear-gradient(90deg, rgba(255, 255, 255, 0.05) 1px, transparent 1px);
    background-size: 42px 42px;
    mask-image: radial-gradient(ellipse 80% 130% at 72% -10%, #000 15%, transparent 78%);
    -webkit-mask-image: radial-gradient(ellipse 80% 130% at 72% -10%, #000 15%, transparent 78%);
  }
  &::after {
    content: '';
    position: absolute; right: -34px; top: -96px; width: 236px; height: 236px; border-radius: 50%;
    border: 1px solid rgba(126, 240, 194, 0.32);
    box-shadow: inset 0 0 70px rgba(0, 209, 255, 0.14), 0 0 0 24px rgba(126, 240, 194, 0.045);
    animation: pf-halo 9s ease-in-out infinite;
  }
}
@keyframes pf-halo {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}
.pf-cover-mark {
  position: absolute; right: 20px; bottom: 12px; z-index: 1;
  font-size: 9.5px; letter-spacing: 2.5px; color: rgba(190, 255, 232, 0.5);
}
.pf-body {
  position: relative;
  z-index: 1;
  padding: 0 24px 24px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
}
.pf-head {
  width: 100%;
  display: flex;
  align-items: flex-start;
  gap: 18px;
}
.pf-avatar {
  position: relative;
  flex: none;
  margin-top: -46px;
  width: 90px; height: 90px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  border: 4px solid #fff;
  background: linear-gradient(145deg, #00d1ff 0%, #00b581 58%, #008f6b 100%);
  box-shadow: 0 12px 26px rgba(4, 54, 42, 0.22);
  span { font-size: 34px; font-weight: 800; color: #fff; line-height: 1; text-shadow: 0 2px 6px rgba(0, 60, 48, 0.35); }
  .pf-online {
    position: absolute; right: 3px; bottom: 3px; width: 14px; height: 14px; border-radius: 50%;
    background: #2bd8a2; border: 2.5px solid #fff;
  }
}
.pf-id { min-width: 0; padding-bottom: 4px; }
.pf-name { font-size: 22px; font-weight: 700; color: var(--qn-text); letter-spacing: 0.5px; }
.pf-subline { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; margin-top: 6px; }
.pf-title { font-size: 12.5px; color: var(--qn-text-2); }
.pf-rows { width: 100%; margin-top: 14px; border-top: 1px dashed var(--qn-border); }
.pf-row {
  display: flex; justify-content: space-between; gap: 14px;
  padding: 9px 0; border-bottom: 1px dashed var(--qn-border); font-size: 13px;
  span { color: var(--qn-text-3); white-space: nowrap; }
  b { color: var(--qn-text); text-align: right; font-weight: 600; }
}
.pf-actions { width: 100%; margin-top: 18px; display: flex; gap: 10px; }

.sec-row {
  display: flex; align-items: center; justify-content: space-between; gap: 16px;
  padding: 12px 0; border-bottom: 1px dashed var(--qn-border);
  &:last-of-type { border-bottom: none; }
  .sr-name { font-size: 13.5px; font-weight: 600; color: var(--qn-text); }
  .sr-desc { font-size: 11.5px; color: #64748b; margin-top: 3px; }
}
.sec-note {
  margin-top: 10px; padding: 10px 12px; border-radius: 10px;
  display: flex; align-items: center; gap: 8px;
  font-size: 11.5px; color: #64748b;
  background: #f2f5f9; border: 1px dashed var(--qn-border);
}
@media (max-width: 1100px) {
  .pf-grid { grid-template-columns: minmax(0, 1fr); }
}
</style>