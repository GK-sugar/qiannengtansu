/** F1–F13 平台内置公式编号：页面标签、悬浮提示、全局「指标说明」共用一份定义 */
export interface FormulaItem {
  code: string
  name: string
  expr: string
  desc: string
  where: string
}

export const FORMULAS: FormulaItem[] = [
  { code: 'F1', name: '碳排放基本公式', expr: 'E = AD × EF × GWP', desc: '排放量 = 活动数据 × 排放因子 × 全球变暖潜势（因子已含 GWP 时取 1）。', where: '企业端 · 碳数据填报 / 碳排核算' },
  { code: 'F2', name: '化石燃料燃烧排放', expr: 'E = FC × NCV × CC × OF × 44/12', desc: 'IPCC 完全氧化法：燃料消耗量 × 低位发热量 × 单位热值含碳量 × 碳氧化率 × 分子量比。NCV 来自化验单识别结果。', where: '范围一 · 自用煤、焦炭等' },
  { code: 'F3', name: '外购电力隐含排放', expr: 'E = 用电量 × 电网因子 ÷ 1000', desc: '范围二排放：用电量（kWh）× 省级电网平均排放因子（kgCO2e/kWh），因子由省厅因子库版本化管理。', where: '范围二 · 购电' },
  { code: 'F4', name: '运输环节排放', expr: 'E = Σ(载重 × 里程) × 运输因子', desc: '范围三排放：货物周转量（t·km）× 公路 / 铁路 / 水运对应因子；物流追踪按轨迹累计里程。', where: '范围三 · 物流追踪' },
  { code: 'F5', name: '总量与碳强度', expr: 'E总 = Σ范围1 + Σ范围2 + Σ范围3；I = E总 ÷ Q产品', desc: '总碳足迹为三范围之和；碳强度 = 总排放 ÷ 产品产量，用于横向对标与护照核证数值。', where: '核算报告 / 大屏排行 / 护照' },
  { code: 'F6', name: '智能初审异常波动', expr: 'r = |X本期 − X基准| ÷ max(X基准, ε)', desc: '相对基期（历史均值 / 行业基准）的偏离率；默认 ±20% 判异常，超 3σ 或强超阈值升为高等级预警。', where: '省厅 · 智能初审台（异常标红）' },
  { code: 'F7', name: '绿色金融信誉分', expr: 'S = 40×完整度 + 25×及时性 + 20×稳定性 + 15×合规', desc: '四项加权信用评分（0~100），衡量企业碳数据可信度。', where: '企业端 · 绿色金融雷达' },
  { code: 'F8', name: '预授信额度', expr: 'Q = Q基准 × k等级', desc: '基准额度按行业规模分档，信誉等级系数：S≥90→1.0、80~89→0.85、70~79→0.7、<70→0.5。', where: '企业端 · 授信申请单' },
  { code: 'F9', name: '打卡离场判定', expr: 'd = 2R·arcsin(√(sin²(Δφ/2) + cosφ1·cosφ2·sin²(Δλ/2)))', desc: 'Haversine 球面距离（R=6371km）：核查员 GPS 与核查点距离 ≤ 半径判有效打卡，否则标记离场。', where: '移动执法 H5 打卡' },
  { code: 'F10', name: '哈希链防篡改', expr: 'hᵢ = SHA256(hᵢ₋₁ ‖ dataᵢ ‖ salt)', desc: '每个存证区块 = 上一块哈希 ‖ 本块数据快照 ‖ 平台盐；任意篡改导致整链断裂，可一键验链。', where: '核算 / 报告 / 护照签发存证' },
  { code: 'F11', name: '护照验真签名', expr: 'token = SHA256(passport_no + ":" + site_salt) 取前 16 位', desc: '以护照编号与平台私盐生成防伪令牌，二维码公开验真页比对令牌与哈希链，输出「通过 / 已被篡改」。', where: '护照签发 · 扫码验真' },
  { code: 'F12', name: '大屏热力分级', expr: '等级 = f(市州排放 ÷ 全省总量)，五级分位着色', desc: '把各市州排放映射为地图颜色等级与热力强度，随数据轮播动态变化。', where: '省厅 · 态势感知大屏' },
  { code: 'F13', name: 'CBAM 关税影响测算', expr: '影响额 ≈ Q出口 × I产品 × (1 − f免费配额) × R税率', desc: '欧盟碳关税成本：年出口量 × 单位产品碳排放 × 未获免费配额比例 × 当期税率（€/tCO₂e）。', where: '国家级 · 国际关税预警' }
]

export const formulaMap: Record<string, FormulaItem> = Object.fromEntries(FORMULAS.map((f) => [f.code, f]))

export function formulaHint(code: string): string {
  const f = formulaMap[code.toUpperCase()]
  return f ? `${f.code} ${f.name}：${f.expr}` : code
}