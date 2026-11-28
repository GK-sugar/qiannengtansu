/** 市州详情页演示数据（仅服务单市页面，与全省数据隔离） */
export interface CityCompany {
  name: string
  industry: string
  emission: number
  status: 'normal' | 'warn' | 'high'
}

export interface CityProfile {
  enterprises: number
  intensity: number
  growth: number
  industries: { name: string; ratio: number; color: string }[]
  companies: CityCompany[]
}

export const CITY_DETAIL: Record<string, CityProfile> = {
  贵阳市: {
    enterprises: 486,
    intensity: 0.86,
    growth: 3.2,
    industries: [
      { name: '大数据与电子信息', ratio: 34, color: '#00b581' },
      { name: '先进装备制造', ratio: 26, color: '#00d1ff' },
      { name: '新能源材料', ratio: 22, color: '#a06bff' },
      { name: '现代服务业', ratio: 18, color: '#ffb020' }
    ],
    companies: [
      { name: '中伟新材料股份有限公司', industry: '新能源材料', emission: 3820, status: 'normal' },
      { name: '贵州振华新材料股份有限公司', industry: '新能源材料', emission: 1260, status: 'normal' },
      { name: '贵州轮胎股份有限公司', industry: '先进装备制造', emission: 980, status: 'warn' },
      { name: '贵州益佰制药股份有限公司', industry: '医药健康', emission: 420, status: 'normal' },
      { name: '贵阳海信电子有限公司', industry: '电子信息', emission: 310, status: 'normal' }
    ]
  },
  六盘水市: {
    enterprises: 312,
    intensity: 2.94,
    growth: 5.1,
    industries: [
      { name: '煤炭开采与洗选', ratio: 46, color: '#ff6b6b' },
      { name: '电力热力', ratio: 27, color: '#ffb020' },
      { name: '煤化工', ratio: 16, color: '#00d1ff' },
      { name: '建材', ratio: 11, color: '#a06bff' }
    ],
    companies: [
      { name: '六盘水黔能矿业有限公司', industry: '煤炭开采', emission: 4860, status: 'high' },
      { name: '贵州盘江精煤股份有限公司', industry: '煤炭开采', emission: 3920, status: 'warn' },
      { name: '六盘水恒鼎实业有限公司', industry: '煤炭开采', emission: 2140, status: 'normal' },
      { name: '贵州黔桂发电有限责任公司', industry: '电力热力', emission: 1860, status: 'warn' },
      { name: '首钢水城钢铁（集团）有限责任公司', industry: '冶金', emission: 1320, status: 'normal' }
    ]
  },
  遵义市: {
    enterprises: 402,
    intensity: 1.72,
    growth: 2.4,
    industries: [
      { name: '有色金属', ratio: 38, color: '#00d1ff' },
      { name: '白酒酿造', ratio: 24, color: '#ffb020' },
      { name: '化工', ratio: 21, color: '#a06bff' },
      { name: '装备制造', ratio: 17, color: '#00b581' }
    ],
    companies: [
      { name: '遵义铝业股份有限公司', industry: '有色金属', emission: 3240, status: 'warn' },
      { name: '贵州茅台酒股份有限公司', industry: '白酒酿造', emission: 1980, status: 'normal' },
      { name: '遵义钛业股份有限公司', industry: '有色金属', emission: 1120, status: 'normal' },
      { name: '贵州赤天化股份有限公司', industry: '化工', emission: 940, status: 'warn' },
      { name: '贵州钢绳股份有限公司', industry: '装备制造', emission: 860, status: 'normal' }
    ]
  },
  安顺市: {
    enterprises: 168,
    intensity: 2.31,
    growth: 1.8,
    industries: [
      { name: '新型建材', ratio: 32, color: '#ffb020' },
      { name: '航空装备', ratio: 26, color: '#00d1ff' },
      { name: '电力', ratio: 24, color: '#ff6b6b' },
      { name: '农产品加工', ratio: 18, color: '#00b581' }
    ],
    companies: [
      { name: '贵州安顺发电有限公司', industry: '电力', emission: 1180, status: 'normal' },
      { name: '贵州安大航空锻造有限责任公司', industry: '航空装备', emission: 680, status: 'normal' },
      { name: '安顺永城建材有限公司', industry: '新型建材', emission: 520, status: 'warn' },
      { name: '贵州百灵企业集团制药股份有限公司', industry: '医药健康', emission: 260, status: 'normal' }
    ]
  },
  毕节市: {
    enterprises: 296,
    intensity: 2.48,
    growth: 4.3,
    industries: [
      { name: '煤炭', ratio: 41, color: '#ff6b6b' },
      { name: '电力热力', ratio: 28, color: '#ffb020' },
      { name: '化工', ratio: 18, color: '#a06bff' },
      { name: '农产品加工', ratio: 13, color: '#00b581' }
    ],
    companies: [
      { name: '贵州毕节华电发电有限公司', industry: '电力热力', emission: 2460, status: 'warn' },
      { name: '贵州黔西能源开发有限公司', industry: '煤炭', emission: 1820, status: 'high' },
      { name: '毕节市力帆骏马车辆有限公司', industry: '装备制造', emission: 480, status: 'normal' },
      { name: '贵州金沙窖酒酒业有限公司', industry: '白酒酿造', emission: 320, status: 'normal' }
    ]
  },
  铜仁市: {
    enterprises: 184,
    intensity: 1.36,
    growth: 2.9,
    industries: [
      { name: '锰系材料', ratio: 36, color: '#a06bff' },
      { name: '新能源电池', ratio: 27, color: '#00d1ff' },
      { name: '旅游服务', ratio: 20, color: '#00b581' },
      { name: '特色农业', ratio: 17, color: '#ffb020' }
    ],
    companies: [
      { name: '贵州大龙汇成新材料有限公司', industry: '新能源电池', emission: 740, status: 'warn' },
      { name: '贵州红星发展大龙锰业有限责任公司', industry: '锰系材料', emission: 620, status: 'normal' },
      { name: '铜仁市碧江新型建材有限公司', industry: '建材', emission: 350, status: 'normal' },
      { name: '贵州梵净山生态农业股份有限公司', industry: '特色农业', emission: 90, status: 'normal' }
    ]
  },
  黔西南布依族苗族自治州: {
    enterprises: 176,
    intensity: 2.05,
    growth: 3.6,
    industries: [
      { name: '电力', ratio: 34, color: '#ffb020' },
      { name: '煤炭', ratio: 28, color: '#ff6b6b' },
      { name: '黄金冶炼', ratio: 21, color: '#00d1ff' },
      { name: '特色农业', ratio: 17, color: '#00b581' }
    ],
    companies: [
      { name: '贵州兴义电力发展有限公司', industry: '电力', emission: 1520, status: 'normal' },
      { name: '贵州晴隆盘江煤电有限公司', industry: '煤炭', emission: 980, status: 'warn' },
      { name: '贵州紫金矿业股份有限公司', industry: '黄金冶炼', emission: 640, status: 'normal' },
      { name: '黔西南州山地特色农业有限公司', industry: '特色农业', emission: 120, status: 'normal' }
    ]
  },
  黔东南苗族侗族自治州: {
    enterprises: 154,
    intensity: 0.92,
    growth: 1.5,
    industries: [
      { name: '生态旅游', ratio: 31, color: '#00b581' },
      { name: '林产品加工', ratio: 26, color: '#ffb020' },
      { name: '特色食品', ratio: 23, color: '#00d1ff' },
      { name: '清洁能源', ratio: 20, color: '#a06bff' }
    ],
    companies: [
      { name: '贵州凯里供电局', industry: '电力', emission: 520, status: 'normal' },
      { name: '贵州青酒厂股份有限公司', industry: '白酒酿造', emission: 210, status: 'normal' },
      { name: '黔东南州林业投资开发有限公司', industry: '林产品加工', emission: 160, status: 'normal' },
      { name: '贵州雷公山生态食品有限公司', industry: '特色食品', emission: 80, status: 'normal' }
    ]
  },
  黔南布依族苗族自治州: {
    enterprises: 206,
    intensity: 1.58,
    growth: 2.7,
    industries: [
      { name: '磷化工', ratio: 39, color: '#a06bff' },
      { name: '新能源材料', ratio: 25, color: '#00d1ff' },
      { name: '装备制造', ratio: 21, color: '#00b581' },
      { name: '特色农业', ratio: 15, color: '#ffb020' }
    ],
    companies: [
      { name: '贵州瓮福（集团）有限责任公司', industry: '磷化工', emission: 2240, status: 'warn' },
      { name: '贵州川恒化工股份有限公司', industry: '磷化工', emission: 880, status: 'normal' },
      { name: '贵州裕能新能源电池材料有限公司', industry: '新能源材料', emission: 760, status: 'normal' },
      { name: '都匀供电局', industry: '电力', emission: 540, status: 'normal' }
    ]
  }
}