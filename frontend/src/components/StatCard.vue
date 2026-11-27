<template>
  <div class="stat-card" :style="{ '--tint': tint }">
    <div class="stat-icon">
      <el-icon :size="22"><component :is="icon" /></el-icon>
    </div>
    <div class="stat-body">
      <div class="stat-label">{{ label }}</div>
      <div class="stat-value num-roll">
        {{ prefix }}<span v-if="animated">{{ display }}</span><span v-else>{{ value }}</span>{{ suffix }}
      </div>
      <div class="stat-extra" v-if="extra">
        <span :class="['trend', trendType]">{{ trend }}<el-icon v-if="trend" :size="12"><CaretTop v-if="trendType==='up'" /><CaretBottom v-else /></el-icon></span>
        <span class="extra-text">{{ extra }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'

const props = withDefaults(
  defineProps<{
    label: string
    value: number
    icon: string
    tint?: string
    prefix?: string
    suffix?: string
    extra?: string
    trend?: string
    trendType?: 'up' | 'down'
    animated?: boolean
    decimals?: number
  }>(),
  { tint: '#00b581', suffix: '', prefix: '', decimals: 0, animated: true, trendType: 'up' }
)

const display = ref(0)
function roll(from: number, to: number) {
  const start = performance.now()
  const dur = 900
  const step = (t: number) => {
    const p = Math.min((t - start) / dur, 1)
    const ease = 1 - Math.pow(1 - p, 3)
    display.value = from + (to - from) * ease
    if (p < 1) requestAnimationFrame(step)
  }
  requestAnimationFrame(step)
}
onMounted(() => roll(0, props.value))
watch(
  () => props.value,
  (v, old) => roll(old ?? 0, v)
)
</script>

<style scoped lang="scss">
.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  background: var(--qn-card);
  border: 1px solid var(--qn-border);
  border-radius: var(--qn-radius);
  padding: 20px;
  box-shadow: var(--qn-shadow);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  position: relative;
  overflow: hidden;
  &:hover { transform: translateY(-3px); box-shadow: var(--qn-shadow-lg); }
  &::after {
    content: '';
    position: absolute;
    right: -30px;
    top: -30px;
    width: 110px;
    height: 110px;
    border-radius: 50%;
    background: radial-gradient(circle, color-mix(in srgb, var(--tint) 16%, transparent) 0%, transparent 70%);
  }
}
.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--tint);
  background: color-mix(in srgb, var(--tint) 12%, #fff);
  flex-shrink: 0;
}
.stat-label { font-size: 13px; color: var(--qn-text-2); margin-bottom: 4px; }
.stat-value { font-size: 26px; font-weight: 700; color: var(--qn-text); letter-spacing: 0.5px; }
.stat-extra { display: flex; align-items: center; gap: 8px; margin-top: 4px; font-size: 12px; }
.trend { display: inline-flex; align-items: center; gap: 2px; font-weight: 600;
  &.up { color: #00b581; }
  &.down { color: #ff6b6b; }
}
.extra-text { color: var(--qn-text-3); }
</style>