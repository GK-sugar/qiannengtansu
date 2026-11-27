/**
 * 黔能碳溯 · 静态素材生成器（零版权，全部程序化自绘）
 * 输出：frontend/src/assets/seal/*.svg / frontend/src/assets/empty/*.svg
 * 用法：node scripts/gen-assets.mjs
 *
 * 座标系约定（SVG y 轴向下）：
 *  - 頂部弧排字：字符位于极角 a 处，rotate = a + 90
 *  - 底部弧排字：字符位于极角 a 处，rotate = a - 90
 * 由此保证公章上下两行文字均为“正读”效果。
 */
import { writeFileSync, mkdirSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, join } from 'node:path'

const root = join(dirname(fileURLToPath(import.meta.url)), '..')
const sealDir = join(root, 'frontend', 'src', 'assets', 'seal')
const emptyDir = join(root, 'frontend', 'src', 'assets', 'empty')
mkdirSync(sealDir, { recursive: true })
mkdirSync(emptyDir, { recursive: true })

const rad = (d) => (d * Math.PI) / 180
const px = (n) => Math.round(n * 100) / 100

/** 沿圆弧排字（返回 <text> 串） */
function arcChars(text, R, startAng, step, size, color, mode, cx = 110, cy = 110) {
  const chars = Array.from(text)
  return chars
    .map((ch, i) => {
      const a = startAng + i * step
      const x = cx + R * Math.cos(rad(a))
      const y = cy + R * Math.sin(rad(a))
      const rot = mode === 'top' ? a + 90 : a - 90
      return `<text transform="translate(${px(x)} ${px(y)}) rotate(${px(rot)})" text-anchor="middle" dominant-baseline="central" font-family="'STZhongsong','SimSun','Songti SC','Noto Serif SC',serif" font-size="${size}" font-weight="700" fill="${color}">${ch}</text>`
    })
    .join('\n  ')
}

/** 五角星（中心在 cx,cy+dy，外接半径 R） */
function star(cx, cy, R, dy, color) {
  const pts = []
  for (let k = 0; k < 10; k++) {
    const r = k % 2 === 0 ? R : R * 0.42
    const a = -90 + k * 36
    pts.push(`${px(cx + r * Math.cos(rad(a)))},${px(cy + dy + r * Math.sin(rad(a)))}`)
  }
  return `<polygon points="${pts.join(' ')}" fill="${color}" />`
}

/** 环形公章模板 */
function seal({
  color, top, bottom, center, file
}) {
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 220 220" width="220" height="220">
  <circle cx="110" cy="110" r="101.5" fill="none" stroke="${color}" stroke-width="8"/>
  <circle cx="110" cy="110" r="76.5" fill="none" stroke="${color}" stroke-width="2.4"/>
  ${top ? arcChars(top.txt, top.R, top.start, top.step, top.size, color, 'top') : ''}
  ${bottom ? arcChars(bottom.txt, bottom.R, bottom.start, bottom.step, bottom.size, color, 'bottom') : ''}
  ${center}
</svg>
`
  writeFileSync(join(sealDir, file), svg, 'utf8')
  console.log('seal ->', file)
}

/* ---------------- 省厅监管电子签章（红） ---------------- */
const govTop = { txt: '贵州省生态环境厅', R: 90, start: -143.9, step: 15.4, size: 16 }
const govBottom = { txt: '应对气候变化处', R: 90, start: 52.2, step: 12.6, size: 14.5 }
seal({
  color: '#d92b2b',
  top: govTop,
  bottom: govBottom,
  center: `
  <text x="${px(110 + 78 * Math.cos(rad(6)))}" y="${px(110 + 78 * Math.sin(rad(6)))}" text-anchor="middle" dominant-baseline="central" font-size="13" fill="#d92b2b">★</text>
  <text x="${px(110 + 78 * Math.cos(rad(174)))}" y="${px(110 + 78 * Math.sin(rad(174)))}" text-anchor="middle" dominant-baseline="central" font-size="13" fill="#d92b2b">★</text>
  ${star(110, 110, 29, 3, '#d92b2b')}`,
  file: 'gov-seal.svg'
})

/* ---------------- 核查机构国际核查专用章（蓝） ---------------- */
seal({
  color: '#1e5fd8',
  top: { txt: '中环低碳国际核查中心', R: 90, start: -148.5, step: 13, size: 14.5 },
  bottom: { txt: '国际碳核查认证', R: 90, start: 54, step: 12, size: 14 },
  center: `
  ${star(110, 110, 23, -8, '#1e5fd8')}
  <text x="110" y="143" text-anchor="middle" dominant-baseline="central" font-family="'Arial','Helvetica',sans-serif" font-size="10.5" font-weight="700" letter-spacing="1.5" fill="#1e5fd8">CG-2026</text>`,
  file: 'agency-seal.svg'
})

/* ---------------- 企业碳管理专用章（红） ---------------- */
seal({
  color: '#c62f2f',
  top: { txt: '贵州黔能矿业有限责任公司', R: 90, start: -154, step: 12.8, size: 14.2 },
  bottom: { txt: '碳管理专用章', R: 90, start: 56.25, step: 13.5, size: 14.5 },
  center: `
  <text x="${px(110 + 78 * Math.cos(rad(15)))}" y="${px(110 + 78 * Math.sin(rad(15)))}" text-anchor="middle" dominant-baseline="central" font-size="12" fill="#c62f2f">★</text>
  <text x="${px(110 + 78 * Math.cos(rad(165)))}" y="${px(110 + 78 * Math.sin(rad(165)))}" text-anchor="middle" dominant-baseline="central" font-size="12" fill="#c62f2f">★</text>
  ${star(110, 110, 25, 4, '#c62f2f')}
  <text x="110" y="147" text-anchor="middle" dominant-baseline="central" font-family="'Arial','Helvetica',sans-serif" font-size="9.5" font-weight="700" letter-spacing="2" fill="#c62f2f">QN-CT</text>`,
  file: 'ent-seal.svg'
})

/* ---------------- 空状态：默认（文件 + 绿芽） ---------------- */
writeFileSync(join(emptyDir, 'empty-default.svg'), `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 160 160" width="160" height="160">
  <defs>
    <radialGradient id="gd" cx="50%" cy="42%" r="60%">
      <stop offset="0%" stop-color="#ccf5e6"/>
      <stop offset="100%" stop-color="#eefaf6" stop-opacity="0"/>
    </radialGradient>
  </defs>
  <circle cx="80" cy="76" r="62" fill="url(#gd)"/>
  <ellipse cx="80" cy="143" rx="52" ry="7" fill="#dfe9f1"/>
  <g transform="rotate(-9 62 88)">
    <rect x="24" y="42" width="62" height="84" rx="8" fill="#ffffff" stroke="#cedee8" stroke-width="1.5"/>
  </g>
  <g transform="rotate(5 92 84)">
    <rect x="60" y="46" width="70" height="92" rx="8" fill="#ffffff" stroke="#c9dbe6" stroke-width="1.5"/>
    <rect x="72" y="70" width="46" height="5" rx="2.5" fill="#e7f3ee"/>
    <rect x="72" y="84" width="38" height="5" rx="2.5" fill="#eef3f7"/>
    <rect x="72" y="98" width="30" height="5" rx="2.5" fill="#eef3f7"/>
    <path d="M96 120 q2 -16 16 -30" fill="none" stroke="#00b581" stroke-width="3" stroke-linecap="round"/>
    <ellipse cx="101" cy="102" rx="8" ry="4.5" fill="#00d1ff" transform="rotate(-32 101 102)"/>
    <ellipse cx="111" cy="96" rx="8" ry="4.5" fill="#00b581" transform="rotate(24 111 96)"/>
  </g>
  <circle cx="32" cy="36" r="3" fill="#ffd166"/>
  <circle cx="130" cy="30" r="2.5" fill="#00d1ff"/>
  <path d="M124 48 l6 0 m-3 -3 l0 6" stroke="#00b581" stroke-width="2" stroke-linecap="round"/>
</svg>
`, 'utf8')

/* ---------------- 空状态：搜索（放大镜 + 文件） ---------------- */
writeFileSync(join(emptyDir, 'empty-search.svg'), `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 160 160" width="160" height="160">
  <defs>
    <radialGradient id="gs" cx="50%" cy="42%" r="60%">
      <stop offset="0%" stop-color="#d6e9ff"/>
      <stop offset="100%" stop-color="#f0f7ff" stop-opacity="0"/>
    </radialGradient>
  </defs>
  <circle cx="80" cy="76" r="62" fill="url(#gs)"/>
  <ellipse cx="80" cy="143" rx="52" ry="7" fill="#dfe9f1"/>
  <g transform="rotate(6 98 84)">
    <rect x="76" y="64" width="56" height="67" rx="8" fill="#ffffff" stroke="#c9dbe6" stroke-width="1.5"/>
    <rect x="86" y="82" width="36" height="5" rx="2.5" fill="#e7f1fb"/>
    <rect x="86" y="94" width="28" height="5" rx="2.5" fill="#eef3f7"/>
    <circle cx="90" cy="114" r="4" fill="#00b581"/>
    <rect x="98" y="111.5" width="24" height="5" rx="2.5" fill="#eef3f7"/>
  </g>
  <circle cx="64" cy="68" r="26" fill="rgba(255,255,255,0.72)" stroke="#4da3ff" stroke-width="6"/>
  <circle cx="64" cy="68" r="9" stroke="#4da3ff" stroke-width="3" fill="none"/>
  <line x1="84" y1="88" x2="103" y2="107" stroke="#4da3ff" stroke-width="7" stroke-linecap="round"/>
  <circle cx="128" cy="34" r="3" fill="#00d1ff"/>
  <path d="M30 34 q6 -10 14 -10 q8 0 14 10" fill="none" stroke="#00b581" stroke-width="3" stroke-linecap="round"/>
  <path d="M44 34 l0 -6 m-4 3 l8 0" stroke="#ffd166" stroke-width="2" stroke-linecap="round"/>
</svg>
`, 'utf8')
console.log('empty -> empty-default.svg / empty-search.svg')
console.log('done.')