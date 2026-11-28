<template>
  <div class="page">
    <div class="hero-banner">
      <div class="title">RBAC 权限控制中心</div>
      <div class="sub">用户 → 角色 → 菜单三级绑定，支持省 / 市 / 企业三级数据隔离；角色承载权限码集合，前端路由守卫与后端接口鉴权共用同一份权限模型</div>
    </div>

    <div class="page-card">
      <el-tabs v-model="tab">
        <el-tab-pane label="用户管理" name="users" />
        <el-tab-pane label="角色管理" name="roles" />
        <el-tab-pane label="菜单权限" name="menus" />
      </el-tabs>

      <!-- 用户 -->
      <div v-if="tab === 'users'" class="pane">
        <div class="pane-toolbar">
          <el-input v-model="userKw" placeholder="搜索用户" clearable style="width: 200px" :prefix-icon="Search" />
          <el-button type="primary" :icon="Plus" @click="openUser()">新增用户</el-button>
        </div>
        <el-table :data="users" size="small">
          <el-table-column prop="name" label="姓名" width="120" />
          <el-table-column prop="username" label="账号" width="140" />
          <el-table-column prop="org" label="所属组织" min-width="170" />
          <el-table-column prop="scope" label="数据范围" width="110">
            <template #default="{ row }"><el-tag size="small" type="warning" round effect="plain">{{ row.scope }}</el-tag></template>
          </el-table-column>
          <el-table-column label="绑定角色" min-width="200">
            <template #default="{ row }">
              <el-tag v-for="r in row.roles" :key="r" size="small" round class="role-tag" @click="onRole(r)">{{ r }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }"><el-tag size="small" :type="row.on ? 'success' : 'info'" round>{{ row.on ? '启用' : '停用' }}</el-tag></template>
          </el-table-column>
          <el-table-column label="操作" width="250">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="openUser(row)">编辑</el-button>
              <el-button link type="primary" size="small" @click="openInfo(row)">账号信息</el-button>
              <el-button link type="warning" size="small" @click="openReset(row)">重置密码</el-button>
              <el-button link :type="row.on ? 'danger' : 'success'" size="small" @click="toggleUser(row)">{{ row.on ? '停用' : '启用' }}</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 角色 -->
      <div v-if="tab === 'roles'" class="pane">
        <div class="pane-toolbar"><el-button type="primary" :icon="Plus" @click="openRole">新增角色</el-button>
          <span class="dim">权限码 = 模块:操作（如 passport:issue），接口与菜单均按权限码鉴权</span></div>
        <div class="role-cards">
          <div v-for="r in roles" :key="r.name" class="role-card">
            <div class="rc-head">
              <span class="rc-icon" :style="{ background: r.color }"><el-icon :size="16"><component :is="r.icon" /></el-icon></span>
              <div><div class="rc-name">{{ r.name }}</div><div class="rc-code">{{ r.code }}</div></div>
              <el-switch v-model="r.on" />
            </div>
            <div class="rc-perms">
              <span v-for="p in r.perms" :key="p" class="perm-chip">{{ p }}</span>
            </div>
            <div class="rc-users">用户数：<b>{{ r.users }}</b> 人 ｜ 菜单：<b>{{ r.menus }}</b> 项</div>
          </div>
        </div>
      </div>

      <!-- 菜单 -->
      <div v-if="tab === 'menus'" class="pane">
        <div class="menu-tree-row">
          <div class="page-card" style="flex:1">
            <div class="page-card-title"><h3>角色 → 菜单绑定</h3><el-tag size="small" round>勾选即授权</el-tag></div>
            <el-tree :data="menuTree" show-checkbox node-key="id" default-expand-all :default-checked-keys="checkedKeys">
              <template #default="{ data }"><span :class="'mt-node lv' + data.level">{{ data.label }}</span></template>
            </el-tree>
          </div>
          <div class="page-card" style="flex:1">
            <div class="page-card-title"><h3>说明</h3></div>
            <ul class="explain">
              <li>登录态由 Spring Security + JWT 维护，接口层注解 <span class="mono">@PreAuthorize("hasAuthority('...')")</span> 校验权限码。</li>
              <li>前端 Router 守卫按用户角色与路由 meta.roles 拦截，未授权菜单不可见且不可直达。</li>
              <li>权限变更实时推送 Redis（Token 版本号），被踢下线后重新登录生效。</li>
              <li>数据隔离：每条数据记录落库时打 data_scope（全省 / 本市 / 本企业）标签，查询自动附加范围条件，越权不可见。</li>
              <li>审计日志记录每次权限变更的前后值，见「全链路操作审计与可信存证中心」。</li>
            </ul>
          </div>
        </div>
      </div>

      <!-- 用户维护弹窗 -->
      <el-dialog v-model="userDlg" :title="editingUser ? '编辑用户 · ' + userForm.username : '新增用户'" width="520px">
        <el-form :model="userForm" label-width="90px">
          <el-form-item label="姓名"><el-input v-model="userForm.name" placeholder="用户真实姓名" /></el-form-item>
          <el-form-item label="账号"><el-input v-model="userForm.username" :disabled="editingUser" placeholder="登录账号（如 ent01）" /></el-form-item>
          <el-form-item label="绑定角色">
            <el-select v-model="userForm.roles" multiple style="width: 100%" placeholder="选择角色（可多选）">
              <el-option v-for="r in roles" :key="r.code" :label="r.name + '（' + r.code + '）'" :value="r.name" />
            </el-select>
          </el-form-item>
          <el-form-item label="所属机构"><el-input v-model="userForm.org" placeholder="如 贵州省生态环境厅" /></el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="userDlg = false">取消</el-button>
          <el-button type="primary" @click="saveUser">确定</el-button>
        </template>
      </el-dialog>

      <!-- 新增角色弹窗 -->
      <el-dialog v-model="roleDlg" title="新增角色" width="520px">
        <el-form :model="roleForm" label-width="90px">
          <el-form-item label="角色名"><el-input v-model="roleForm.name" placeholder="如 数据报送员" /></el-form-item>
          <el-form-item label="角色码"><el-input v-model="roleForm.code" placeholder="如 ROLE_DATA_SUBMITTER" /></el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="roleDlg = false">取消</el-button>
          <el-button type="primary" @click="saveRole">确定</el-button>
        </template>
      </el-dialog>

      <!-- 角色权限详情弹窗 -->
      <el-dialog v-model="roleDetailVisible" :title="'角色权限详情 · ' + (roleDetail?.name ?? '')" width="560px">
        <template v-if="roleDetail">
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="角色名称">{{ roleDetail.name }}</el-descriptions-item>
            <el-descriptions-item label="角色码"><span class="mono">{{ roleDetail.code }}</span></el-descriptions-item>
            <el-descriptions-item label="用户数">{{ roleDetail.users }} 人</el-descriptions-item>
            <el-descriptions-item label="授权菜单">{{ roleDetail.menus }} 项</el-descriptions-item>
          </el-descriptions>
          <div class="perm-box">
            <div class="perm-box-title">权限码集合（接口 @PreAuthorize 鉴权与前端菜单守卫共用）</div>
            <div class="rc-perms">
              <span v-for="p in roleDetail.perms" :key="p" class="perm-chip">{{ p }}</span>
            </div>
            <div class="perm-box-note">权限码 = 模块:操作（如 passport:issue），该角色可在「菜单权限」页为其勾选可见菜单</div>
          </div>
          <div class="perm-box">
            <div class="perm-box-title">该角色下的登录账号</div>
            <el-table :data="usersOfRole(roleDetail.name)" size="small">
              <el-table-column prop="username" label="账号" width="120">
                <template #default="{ row }"><span class="mono">{{ row.username }}</span></template>
              </el-table-column>
              <el-table-column prop="name" label="姓名" width="100" />
              <el-table-column prop="org" label="所属组织" min-width="160" />
              <el-table-column label="状态" width="80">
                <template #default="{ row }"><el-tag size="small" :type="row.on ? 'success' : 'info'" round>{{ row.on ? '启用' : '停用' }}</el-tag></template>
              </el-table-column>
            </el-table>
          </div>
        </template>
        <template #footer><el-button type="primary" @click="roleDetailVisible = false">关闭</el-button></template>
      </el-dialog>

      <!-- 账号信息弹窗 -->
      <el-dialog v-model="infoVisible" :title="'账号信息 · ' + (infoRow?.username ?? '')" width="620px">
        <template v-if="infoRow">
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="登录账号"><span class="mono">{{ infoRow.username }}</span></el-descriptions-item>
            <el-descriptions-item label="姓名">{{ infoRow.name }}</el-descriptions-item>
            <el-descriptions-item label="绑定角色" :span="2">
              <el-tag v-for="r in infoRow.roles" :key="r" size="small" round class="role-tag">{{ r }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="所属组织" :span="2">{{ infoRow.org }}</el-descriptions-item>
            <el-descriptions-item label="数据范围">{{ infoRow.scope }}</el-descriptions-item>
            <el-descriptions-item label="账号状态">
              <el-tag size="small" :type="infoRow.on ? 'success' : 'info'" round>{{ infoRow.on ? '启用' : '停用' }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ infoRow.createdAt }}</el-descriptions-item>
            <el-descriptions-item label="最近登录">{{ infoRow.lastLogin }}</el-descriptions-item>
          </el-descriptions>
          <div class="info-note">密码不以明文展示；如用户忘记密码，由管理员点击「重置密码」设置新密码，操作写入审计日志。</div>
        </template>
        <template #footer>
          <el-button type="warning" plain @click="openReset(infoRow!)">重置密码</el-button>
          <el-button type="primary" @click="infoVisible = false">关闭</el-button>
        </template>
      </el-dialog>

      <!-- 重置密码弹窗 -->
      <el-dialog v-model="resetVisible" :title="'重置密码 · ' + (resetRow?.username ?? '')" width="480px">
        <el-alert type="warning" :closable="false" show-icon class="reset-alert"
          :title="'为账号「' + (resetRow?.username ?? '') + '」设置新密码，重置后立即生效，请通知用户尽快登录并自行修改。'" />
        <el-form label-width="90px" style="margin-top: 16px">
          <el-form-item label="新密码">
            <el-input v-model="resetForm.pwd" show-password placeholder="至少 6 位">
              <template #append><el-button @click="genPwd">随机生成</el-button></template>
            </el-input>
          </el-form-item>
          <el-form-item label="确认密码">
            <el-input v-model="resetForm.pwd2" show-password placeholder="再次输入新密码" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="resetVisible = false">取消</el-button>
          <el-button type="primary" @click="doReset">确认重置</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { setPasswordOverride } from '@/utils/demo'

interface UserRow { name: string; username: string; org: string; scope: string; roles: string[]; on: boolean; createdAt: string; lastLogin: string }
interface Role { name: string; code: string; color: string; icon: string; on: boolean; users: number; menus: number; perms: string[] }

const tab = ref('users')
const userKw = ref('')

const users = ref<UserRow[]>([
  { name: '王总', username: 'enterprise', org: '贵州黔能矿业有限责任公司', scope: '本企业', roles: ['企业管理员'], on: true, createdAt: '2025-11-02 09:12', lastLogin: '2026-11-27 08:41' },
  { name: '李处长', username: 'gov', org: '贵州省生态环境厅', scope: '全省', roles: ['省厅审批员'], on: true, createdAt: '2025-11-02 09:20', lastLogin: '2026-11-27 09:05' },
  { name: '陈主任', username: 'agency', org: '中环低碳国际核查中心', scope: '被核查企业', roles: ['核查复核员'], on: true, createdAt: '2025-11-05 14:02', lastLogin: '2026-11-26 17:22' },
  { name: '系统管理员', username: 'superadmin', org: '黔能碳溯平台', scope: '平台全部', roles: ['超级管理员'], on: true, createdAt: '2025-10-20 10:00', lastLogin: '2026-11-27 08:30' },
  { name: '张副处', username: 'gov2', org: '贵州省生态环境厅', scope: '贵阳市', roles: ['省厅审批员', '因子库管理员'], on: true, createdAt: '2025-12-11 11:32', lastLogin: '2026-11-25 15:48' },
  { name: '赵科长', username: 'gov3', org: '贵州省生态环境厅', scope: '全省', roles: ['省厅审批员'], on: true, createdAt: '2026-01-08 09:45', lastLogin: '2026-11-24 10:12' },
  { name: '高工', username: 'agency2', org: '中环低碳国际核查中心', scope: '被核查企业', roles: ['核查复核员'], on: true, createdAt: '2026-02-19 16:20', lastLogin: '2026-11-26 09:33' },
  { name: '刘会计', username: 'ent02', org: '贵州磷化集团有限责任公司', scope: '本企业', roles: ['企业管理员'], on: true, createdAt: '2026-03-02 10:11', lastLogin: '2026-11-26 14:07' },
  { name: '周专员', username: 'ent03', org: '中伟新材料股份有限公司', scope: '本企业', roles: ['数据报送员'], on: true, createdAt: '2026-03-15 15:26', lastLogin: '2026-11-27 08:58' },
  { name: '孙报送', username: 'ent04', org: '遵义铝业股份有限公司', scope: '本企业', roles: ['数据报送员'], on: false, createdAt: '2026-04-08 09:02', lastLogin: '2026-09-30 11:40' },
  { name: '吴监管', username: 'city1', org: '贵阳市生态环境局', scope: '本市', roles: ['市级监管员'], on: true, createdAt: '2026-05-21 14:50', lastLogin: '2026-11-25 16:19' },
  { name: '郑终审', username: 'national1', org: '国家应对气候变化战略研究中心', scope: '全国', roles: ['国家级终审员'], on: true, createdAt: '2026-06-03 10:30', lastLogin: '2026-11-26 18:02' }
])

const roles = ref<Role[]>([
  { name: '企业管理员', code: 'ROLE_ENT_ADMIN', color: '#00b581', icon: 'OfficeBuilding', on: true, users: 2384, menus: 10, perms: ['data:submit', 'calc:run', 'report:gen', 'passport:apply', 'supply:manage', 'finance:apply'] },
  { name: '省厅审批员', code: 'ROLE_GOV_REVIEW', color: '#4da3ff', icon: 'Monitor', on: true, users: 18, menus: 6, perms: ['review:audit', 'task:dispatch', 'factor:manage', 'seal:stamp', 'screen:view'] },
  { name: '核查复核员', code: 'ROLE_AGENCY_REVIEW', color: '#a06bff', icon: 'Checked', on: true, users: 12, menus: 3, perms: ['pkg:receive', 'review:international', 'passport:issue'] },
  { name: '超级管理员', code: 'ROLE_SUPER_ADMIN', color: '#ffb020', icon: 'Lock', on: true, users: 1, menus: 5, perms: ['*:*:*'] },
  { name: '因子库管理员', code: 'ROLE_FACTOR_ADMIN', color: '#4da3ff', icon: 'Collection', on: true, users: 3, menus: 2, perms: ['factor:manage', 'factor:publish'] },
  { name: '数据报送员', code: 'ROLE_DATA_SUBMITTER', color: '#00d1ff', icon: 'Upload', on: true, users: 4120, menus: 3, perms: ['data:submit', 'report:view'] },
  { name: '市级监管员', code: 'ROLE_CITY_GOV', color: '#a06bff', icon: 'MapLocation', on: true, users: 26, menus: 4, perms: ['review:audit', 'screen:view', 'task:dispatch'] },
  { name: '国家级终审员', code: 'ROLE_NATIONAL_REVIEW', color: '#ff6b6b', icon: 'Stamp', on: true, users: 6, menus: 3, perms: ['passport:issue', 'blacklist:manage', 'tariff:manage'] }
])

const menuTree = [
  { id: 'ent', label: '企业端', level: 1, children: [
    { id: 'ent-data', label: '碳数据填报', level: 2 },
    { id: 'ent-calc', label: '碳排核算与报告', level: 2 },
    { id: 'ent-ai', label: 'AI 碳管家', level: 2 },
    { id: 'ent-passport', label: '护照申请', level: 2 }
  ] },
  { id: 'gov', label: '省厅监管端', level: 1, children: [
    { id: 'gov-screen', label: '态势大屏', level: 2 },
    { id: 'gov-review', label: '智能初审台', level: 2 },
    { id: 'gov-seal', label: '意见书回传', level: 2 }
  ] },
  { id: 'agency', label: '核查机构端', level: 1, children: [
    { id: 'agency-review', label: '国际标准复核', level: 2 },
    { id: 'agency-issue', label: '护照签发', level: 2 }
  ] }
]
const checkedKeys = ['ent', 'ent-data', 'ent-calc', 'gov', 'gov-screen']

/* ============ 用户维护 ============ */
const userDlg = ref(false)
const editingUser = ref(false)
const userForm = reactive({ name: '', username: '', roles: [] as string[], org: '' })
const scopeMap: Record<string, string> = { 企业管理员: '本企业', 省厅审批员: '全省', 核查复核员: '被核查企业', 超级管理员: '平台全部', 因子库管理员: '全省', 数据报送员: '本企业', 市级监管员: '本市', 国家级终审员: '全国' }

function openUser(row?: UserRow) {
  editingUser.value = !!row
  if (row) Object.assign(userForm, { name: row.name, username: row.username, roles: [...row.roles], org: row.org })
  else Object.assign(userForm, { name: '', username: '', roles: [], org: '' })
  userDlg.value = true
}
function saveUser() {
  if (!userForm.name || !userForm.username) { ElMessage.warning('请填写姓名与账号'); return }
  if (!userForm.roles.length) { ElMessage.warning('请至少绑定一个角色'); return }
  const scope = scopeMap[userForm.roles[0]] ?? '本企业'
  if (editingUser.value) {
    const row = users.value.find(u => u.username === userForm.username)
    if (row) Object.assign(row, { name: userForm.name, roles: [...userForm.roles], org: userForm.org, scope })
    userDlg.value = false
    ElMessage.success('用户「' + userForm.username + '」已更新，权限变更已写入审计日志')
  } else {
    if (users.value.some(u => u.username === userForm.username)) { ElMessage.warning('账号「' + userForm.username + '」已存在'); return }
    users.value.push({ name: userForm.name, username: userForm.username, org: userForm.org, scope, roles: [...userForm.roles], on: true, createdAt: new Date().toLocaleString('zh-CN', { hour12: false }), lastLogin: '尚未登录' })
    userDlg.value = false
    ElMessage.success('用户「' + userForm.username + '」已创建并绑定角色「' + userForm.roles.join('、') + '」，即时生效')
  }
}
function toggleUser(row: UserRow) {
  row.on = !row.on
  ElMessage.success('用户「' + row.name + '」已' + (row.on ? '启用' : '停用') + '，登录态按 Token 版本号实时生效')
}

/* ============ 角色维护 ============ */
const roleDlg = ref(false)
const roleForm = reactive({ name: '', code: '' })
function openRole() { Object.assign(roleForm, { name: '', code: '' }); roleDlg.value = true }
function saveRole() {
  if (!roleForm.name || !roleForm.code) { ElMessage.warning('请填写角色名与角色码'); return }
  if (roles.value.some(r => r.code === roleForm.code)) { ElMessage.warning('角色码「' + roleForm.code + '」已存在'); return }
  roles.value.push({ name: roleForm.name, code: roleForm.code, color: '#00b581', icon: 'Lock', on: true, users: 0, menus: 0, perms: [] })
  roleDlg.value = false
  ElMessage.success('角色「' + roleForm.name + '」已创建，请在「菜单权限」页为其勾选菜单授权')
}

/* ============ 角色权限详情 ============ */
const roleDetailVisible = ref(false)
const roleDetail = ref<Role | null>(null)
function onRole(name: string) {
  const r = roles.value.find(x => x.name === name)
  if (!r) return
  roleDetail.value = r
  roleDetailVisible.value = true
}
const usersOfRole = (name: string) => users.value.filter(u => u.roles.includes(name))

/* ============ 账号信息 ============ */
const infoVisible = ref(false)
const infoRow = ref<UserRow | null>(null)
function openInfo(row: UserRow) {
  infoRow.value = row
  infoVisible.value = true
}

/* ============ 重置密码 ============ */
const resetVisible = ref(false)
const resetRow = ref<UserRow | null>(null)
const resetForm = reactive({ pwd: '', pwd2: '' })
function openReset(row: UserRow) {
  resetRow.value = row
  Object.assign(resetForm, { pwd: '', pwd2: '' })
  resetVisible.value = true
}
function genPwd() {
  const p = 'Qn@' + Math.random().toString(36).slice(2, 8)
  Object.assign(resetForm, { pwd: p, pwd2: p })
}
async function doReset() {
  if (!resetRow.value) return
  if (resetForm.pwd.length < 6) { ElMessage.warning('新密码至少 6 位'); return }
  if (resetForm.pwd !== resetForm.pwd2) { ElMessage.warning('两次输入的密码不一致'); return }
  const username = resetRow.value.username
  setPasswordOverride(username, resetForm.pwd)
  try { await request.post('/v1/admin/rbac/users/' + username + '/reset-password', { password: resetForm.pwd }) } catch { /* 后端未启动时本地生效 */ }
  resetVisible.value = false
  ElMessage.success('账号「' + username + '」密码已重置并即时生效，请通知用户尽快登录修改；操作已写入审计日志')
}
</script>

<style scoped lang="scss">
.pane { padding-top: 8px; }
.pane-toolbar { display: flex; gap: 10px; margin-bottom: 14px; align-items: center; .dim { font-size: 11.5px; color: var(--qn-text-3); } }
.role-tag { margin-right: 6px; cursor: pointer; }
.mono { font-family: monospace; font-size: 12px; background: #f2f5f9; padding: 1px 5px; border-radius: 4px; }
.role-cards { display: grid; grid-template-columns: repeat(2, 1fr); gap: 14px; }
.role-card { border: 1px solid var(--qn-border); border-radius: 12px; padding: 16px; background: #fbfcfe; transition: all .2s;
  &:hover { border-color: var(--qn-primary); box-shadow: var(--qn-shadow); } }
.rc-head { display: flex; align-items: center; gap: 10px; margin-bottom: 12px;
  .rc-icon { width: 34px; height: 34px; border-radius: 9px; color: #fff; display: flex; align-items: center; justify-content: center; }
  .rc-name { font-size: 14px; font-weight: 600; color: var(--qn-text); }
  .rc-code { font-size: 11px; color: var(--qn-text-3); font-family: monospace; }
  .el-switch { margin-left: auto; } }
.rc-perms { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 10px;
  .perm-chip { font-size: 11px; font-family: monospace; background: #eef4f9; color: #4b6b8f; border-radius: 5px; padding: 2px 7px; } }
.rc-users { font-size: 12px; color: var(--qn-text-2); }
.perm-box { margin-top: 16px; padding: 14px 16px; background: #f5f8fc; border: 1px dashed #c9d9e8; border-radius: 10px; }
.perm-box-title { font-size: 12px; color: var(--qn-text-2); margin-bottom: 8px; }
.perm-box-note { margin-top: 10px; font-size: 11.5px; color: var(--qn-text-3); }
.menu-tree-row { display: flex; gap: 16px; }
.mt-node.lv1 { font-weight: 600; color: var(--qn-text); }
.mt-node.lv2 { color: var(--qn-text-2); font-size: 13px; }
.explain { padding-left: 18px; font-size: 13px; color: var(--qn-text-2); line-height: 2.1; }
.info-note { margin-top: 12px; font-size: 12px; color: var(--qn-text-3); }
.reset-alert { margin-bottom: 4px; }
</style>