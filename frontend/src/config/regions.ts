/** 贵州 9 个市州的演示数据（大屏热力与市级下钻共用） */
export interface CityStat {
  name: string
  short: string
  emission: number
  warnings: number
}

export const CITY_STATS: CityStat[] = [
  { name: '贵阳市', short: '贵阳', emission: 12460, warnings: 1 },
  { name: '六盘水市', short: '六盘水', emission: 14730, warnings: 3 },
  { name: '遵义市', short: '遵义', emission: 10820, warnings: 1 },
  { name: '安顺市', short: '安顺', emission: 5210, warnings: 1 },
  { name: '毕节市', short: '毕节', emission: 13680, warnings: 2 },
  { name: '铜仁市', short: '铜仁', emission: 4620, warnings: 1 },
  { name: '黔西南布依族苗族自治州', short: '黔西南', emission: 6140, warnings: 1 },
  { name: '黔东南苗族侗族自治州', short: '黔东南', emission: 3880, warnings: 1 },
  { name: '黔南布依族苗族自治州', short: '黔南', emission: 5760, warnings: 1 }
]

export const cityStatMap: Record<string, CityStat> = Object.fromEntries(CITY_STATS.map((c) => [c.name, c]))
export const provinceTotalEmission = CITY_STATS.reduce((s, c) => s + c.emission, 0)