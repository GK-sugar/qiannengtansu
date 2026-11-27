# 黔能碳溯 · 外部 API 集成与资源清单（第三步产出 · v2 对齐 Spring Boot 3）

> 本文档对齐「第三块产出」要求，逐项回答：
> ① 需要哪些外部 API；② 用在哪个业务模块；③ 你去哪些平台申请 Key；
> ④ 需要哪些静态素材、放在哪个目录。
>
> **两条铁律**
> 1. 所有 Key 只进 `.env` / 环境变量 / `application.yml` 占位，代码中显著中文注释 `【此处需填入API Key】`，**项目没有 Key 也能完整演示**（每个外部能力都内置本地降级/演示模式）。
> 2. 静态素材**全部由平台代生成**（Logo / 三枚印章 / 头像 / 空状态插画 / 大屏背景均以零版权 SVG / Canvas 代码自绘），你**无需提供任何图片**；如后续想换成正式品牌素材，按下表目录替换同名文件即可。

---

## 一、需要接入的外部 API

| 编号 | 能力 | 推荐服务 | 用在哪个业务模块（对应最新文档） | 申请平台 | .env 变量 | 未配置 Key 时的降级方案 |
| --- | --- | --- | --- | --- | --- | --- |
| API-1 | LLM 大模型对话 / 报告摘要 / 初审理由 | 阿里云百炼 **qwen-plus**（OpenAI 兼容协议）；备选 DeepSeek / 智谱 | 企业端「AI碳管家」多轮问答；企业端「整改计划与预算」AI 生成整改措施与预算项；省厅「智能初审台」生成预警原因；核查机构「复核意见」润色 | [阿里云百炼控制台](https://bailian.console.aliyun.com/) | `DASHSCOPE_API_KEY`、`LLM_MODEL=qwen-plus`、`LLM_BASE_URL` | 本地规则引擎 + 碳知识话术库，前端生成流式假回答 |
| API-2 | **多模态视觉大模型（化验单读图）** | 阿里云百炼 **qwen3-vl-plus** | 企业端「化验单识别」：化验单照片 → 灰分 / 硫分 / 低位发热量等结构化字段 | 同百炼控制台开通视觉模型 | `DASHSCOPE_API_KEY`（与 LLM 共用）、`VL_MODEL=qwen3-vl-plus` | 本地演示：返回内置样例化验单字段并标注“演示数据” |
| API-3 | 地理编码 / 逆地理编码 / Web 服务 | **高德开放平台 Web 服务 API** | 企业端「物流追踪」轨迹里程校准；省厅「移动执法 H5」GPS 坐标转中文地址留痕 | [高德开放平台](https://lbs.amap.com/) | `AMAP_WEB_KEY` | 仅使用浏览器 Geolocation 原始经纬度 |
| API-4 | 地图底图与轨迹绘制 | **高德地图 JS API 2.0** | 企业端「物流追踪」地图底图 + 模拟轨迹、起终点标绘 | 同上（Web 端 JS API Key） | 前端内置 **Canvas/SVG 自绘轨迹图**（演示模式），无需任何 Key 即可预览 |
| API-5 | 对象存储 | 阿里云 OSS（备选 MinIO 自建） | 化验单照片、执法打卡照片、核算报告、CBAM 数据包等文件 | [阿里云 OSS 控制台](https://oss.console.aliyun.com/) | `OSS_ACCESS_KEY_ID`、`OSS_ACCESS_KEY_SECRET`、`OSS_BUCKET`、`OSS_ENDPOINT` | 降级本机磁盘（`STORAGE_TYPE=local`） |
| API-6 | 短信通知（可选） | 阿里云短信 | 核查任务派发短信、护照签发结果通知 | [阿里云短信控制台](https://dysms.console.aliyun.com/) | `ALIYUN_SMS_ACCESS_KEY_ID`、`ALIYUN_SMS_ACCESS_KEY_SECRET`、`SMS_SIGN_NAME`、`SMS_TEMPLATE_CODE` | 仅站内消息中心 |

### 无需申请 Key 的能力（内置实现，避免误会）

| 能力 | 实现方式 |
| --- | --- |
| 贵州地图 / 市州热力图 | ECharts + 贵州省市州 GeoJSON **离线渲染**（已内置 `frontend/public/map/guizhou.json`，来源阿里 DataV.GeoAtlas 公开数据） |
| H5 打卡 GPS | 浏览器 `navigator.geolocation`，距离校验用文档 F9 Haversine 公式 |
| 防伪二维码 | 前端 `qrcode.vue` + 后端 `SHA-256`（JDK 自带），扫码直达自有公开验真页 |
| 双轨 / 电子签章 | 本地 SVG 综合成章（零版权印章图像，不需要 CA 机构，二期可接 CA） |
| 哈希链 | JDK 自带 `MessageDigest` 实现，不引第三方 |
| 国际关税预警（CBAM） | 内置规则推演：影响额 ≈ 出口量 × 单位产品碳排放 × (1 − 免费配额%) × 税率（docs/04 F13），EU ETS 均价为内置基准，无需外部行情 API |
| 全国宏观预测 | 内生线性外推自研趋势模型（2030 情景推演），不调用外部 AI/预测接口 |
| 跨国互认监控 | 护照海外扫码查验为自有链路数据回流（`nat_mutual_scan`），不依赖第三方 API |

---

## 二、Key 申请路线图（拿到后回填即用）

1. **阿里云账号**（一个账号覆盖百炼 / OSS / 短信）→ 实名认证 → 各产品控制台开通。
2. **百炼 qwen-plus + qwen3-vl-plus**：百炼控制台 → 模型广场开通 qwen-plus、qwen3-vl-plus → API-KEY 创建（`sk-***`）。→ 回填 `DASHSCOPE_API_KEY`。
3. **高德**：lbs.amap.com 注册开发者 → 应用管理创建「Web 端(JS API)」Key 与「Web 服务」Key。→ 回填 `AMAP_WEB_KEY`（前端 `frontend/.env` 的 `VITE_AMAP_JS_KEY`）。
4. **OSS**：创建 Bucket（如 `qnct-files`）→ AccessKey。→ 回填 4 个 OSS 变量。
5. **短信（可选）**：申请签名「黔能碳溯」+ 模板。→ 回填 4 个 SMS 变量。

> 代码侧所有 Key 用法都已在 `backend/src/main/resources/application.yml`、`frontend/.env.development` 用 `【此处需填入API Key】` 标注，搜注释即可定位全部填 Key 位置。

---

## 三、静态素材清单（全部由本平台代生成，零版权、矢量 SVG 任意分辨率清晰）

| 素材 | 规格 | 放置目录（相对项目根） | 用途 |
| --- | --- | --- | --- |
| 品牌 Logo（翡翠峰 + 碳原子环） | SVG 矢量 ×3（深底 / 白字 / 彩色标） | `frontend/src/assets/logo/`（`logo.svg`、`logo-white.svg`、`logo-mark.svg`） | 登录页、侧边栏、报告封面、护照封页 |
| 省厅监管章（五星红章·贵州省生态环境厅） | SVG 矢量红章 | `frontend/src/assets/seal/gov-seal.svg` + 镜像 `backend/src/main/resources/assets/seal/gov-seal.svg` | 电子签章（初审意见书） |
| 核查机构章（国际核查中心） | SVG 矢量 | `frontend/src/assets/seal/agency-seal.svg` + 镜像副本 | 护照签发盖章 |
| 企业章（贵州黔能矿业） | SVG 矢量 | `frontend/src/assets/seal/ent-seal.svg` + 镜像副本 | 报告封皮 / 数据包盖章 |
| 登录页动态背景 | Canvas 代码绘制（粒子星空 + 流动极光），**无图片文件** | 代码位于 `frontend/src/views/login/index.vue` | 登录页动态视觉 |
| 态势大屏背景 | 深色渐变 + 网格 + 粒子星场（CSS/Canvas 代码） | 代码位于 `frontend/src/views/gov/big-screen/index.vue` | 省厅态势感知大屏 |
| 空状态插画 | SVG 矢量（萤光极简风） | `frontend/src/assets/empty/`（`empty-default.svg`、`empty-search.svg`） | 各列表空状态 |
| 默认头像 | SVG 矢量 | `frontend/src/assets/avatar/default.svg` | 用户头像兜底 |
| 网站 Favicon | SVG 矢量 | `frontend/public/favicon.svg` | 浏览器标签图标 |
| 贵州地图 GeoJSON | JSON（130KB，DataV 公开数据） | `frontend/public/map/guizhou.json` | 大屏热力分布 |

> 若后续换成官方正式 Logo/印章，按上表目录 **覆盖同名文件** 即可，全站自动生效，代码零改动。

---

## 四、环境变量总览（填 Key 位置速查）

`backend/src/main/resources/application.yml` 引用的变量（同时给出 `deploy/.env.example` 模板）：

```yaml
# ========== 数据源 ==========
MYSQL_HOST=127.0.0.1
MYSQL_PORT=3306
MYSQL_USER=root
MYSQL_PASSWORD=qnct123456
MYSQL_DB=qnct
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
# ========== JWT ==========
JWT_SECRET=please-change-me-to-a-long-random-string
# ========== LLM / 多模态视觉【此处需填入API Key】 ==========
DASHSCOPE_API_KEY=
LLM_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
LLM_MODEL=qwen-plus
VL_MODEL=qwen3-vl-plus
# ========== 高德【此处需填入API Key】 ==========
AMAP_WEB_KEY=
# ========== OSS（可选）【此处需填入API Key】 ==========
OSS_ACCESS_KEY_ID=
OSS_ACCESS_KEY_SECRET=
OSS_BUCKET=
OSS_ENDPOINT=
STORAGE_TYPE=local           # local | oss
# ========== 短信（可选）【此处需填入API Key】 ==========
ALIYUN_SMS_ACCESS_KEY_ID=
ALIYUN_SMS_ACCESS_KEY_SECRET=
SMS_SIGN_NAME=
SMS_TEMPLATE_CODE=
```

前端 `frontend/.env.development`：

```bash
VITE_API_BASE_URL=/api
VITE_USE_MOCK=true            # 演示模式：无 Key、无后端也能跑全站
# 【此处需填入API Key】高德 JS API：填写后物流追踪显示真实地图，不填自动切换自绘轨迹图
VITE_AMAP_JS_KEY=
```