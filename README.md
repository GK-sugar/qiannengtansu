# 黔能碳溯 · 贵州省能源碳足迹溯源与全流程可信存证平台

前后端分离的碳足迹「双轨合规 + 多级溯源」智能系统，覆盖 **碳数据采集 → 智能核算 → 区块链式哈希验真 → 电子护照 → 绿色金融** 全链路，面向贵州省重点产业（煤矿 / 新能源材料工厂），按 **企业端 / 省生态环境厅端 / 第三方核查机构端 / 国家级审批部门端 / 超级管理员端** 五端隔离，数据按国家 / 省 / 市 / 企业四级权限管控。

> 需求文档指定技术栈为 Spring Boot 3 + JDK 17（单体）与 Vue 3 + Vite + Element Plus + ECharts + Pinia + qrcode.vue（仅 Web 端，不做小程序）；本项目**严格按需求技术栈实现**，为适配两周极速开发周期采用极简单机部署，不引入微服务。任何外部能力（大模型 / 视觉 / 地图 / OSS）未配 Key 时**自动降级本地实现**，零 Key 也能预览全站。

- 项目性质：两周极速开发课程实践 · 企业级 / 政务级 SaaS 演示平台
- 配套文档：《黔能碳溯_需求整合.docx》（需求）、`docs/01~04`（架构 / 目录 / 外部 API 清单 / 公式算法）、`功能模块树形图.png`
- 数据库设计：`sql/init.sql`（52 张表全量 DDL + 演示数据）

## 功能概览

系统按角色隔离功能入口，五端 + 一个公开验真页：

- **企业端**（煤矿 / 新能源材料工厂，11 页）：碳数据填报（按范围一/二/三存储）、化验单识别（多模态读图抽出灰分/硫分/低位发热量等字段，人工确认后入库）、碳排核算与报告（F1~F5 汇总，ECharts 饼/柱/趋势图，**国标 / 行标 / 团标切换全页联动重算**，一键生成可下载报告并写哈希链）、AI 碳管家（带企业上下文的流式问答）、护照申请（状态机：填报 → 省审 → 复核 → 终审 → 发证）、供应链协同、物流追踪（高德真实底图绘轨迹，未配 Key 自动切 Canvas 自绘，F4 折算范围三排放）、绿色金融（信誉分雷达图 + 预授信额度）、出海数据包（按 CBAM 模板拼装 Excel）、整改计划与预算（AI 生成整改措施 + 分项预算，含 10% 预备费）、数据看板。
- **省厅监管端**（6 页 + 移动执法 H5）：态势感知大屏（贵州 GeoJSON 热力图 F12、滚动数字、行业排行）、智能初审台（与历史/行业基准比对，异常标红 + 预警原因 F6）、核查派单、因子库管理（省级因子版本）、电子签章（盖章叠审批单）、移动执法 H5（GPS 定位校验是否在企业范围内，打卡 F9）。
- **第三方核查机构端**（国际合规复核，3 页）：出海数据包接收与下载、国际标准复核（核对方法学/因子/边界，通过或退回）、复核结论提交（推送国家级终审）。
- **国家级审批部门端**（终审发证，5 页）：终审与发证中心（护照编号 → 哈希链 F10 → 防伪二维码 F11）、跨国互认监控（海外扫码回流趋势/来源国 Top5/异常令牌预警）、宏观预测驾驶舱（全国排放历史回放 + 2030 情景内生外推 + 省域排行 + 达峰结论）、国际关税预警（欧盟 CBAM 规则库与影响测算 F13）、企业黑名单通报（跨省通报、权限冻结/解除、留痕审计）。
- **超级管理员端**（系统运营，5 页）：RBAC 权限控制中心（用户/角色/菜单，四级数据隔离 `data_scope`）、全链路操作审计与可信存证中心（全量操作日志 + 修改前后字段级快照比对 + SHA-256 哈希校验异常标红）、系统数据字典与基础配置、系统运行与接口监控（服务器负载/API 频率/健康度）、运营看板。
- **公开验真页**（无需登录）：扫码验真，展示护照信息、核算汇总、存证哈希、是否被篡改。

## 技术栈

| 层次 | 技术 | 版本 | 选型理由 |
| --- | --- | --- | --- |
| 运行环境 | JDK（Temurin） | 17 | 需求指定，Spring Boot 3 最低要求 |
| 构建工具 | Maven | 3.9.16 | 依赖管理与可执行 jar 打包 |
| 后端框架 | Spring Boot | 3.3.5 | 单体应用，一个进程一个端口（8082） |
| Web / 校验 / SSE | Spring MVC + Validation | 随 Boot | REST 接口、DTO 校验、AI 流式回答 |
| ORM | MyBatis-Plus | 3.5.7 | 单表零 SQL + 分页插件，两周出活最快 |
| 数据库 | MySQL（connector-j 随 Boot） | 8.0 | 库名 `qnct`，字符集 `utf8mb4` |
| 缓存 | Redis（spring-data-redis） | 7（可选） | 验证码 / Token / 字典缓存，未启动自动降级 |
| 鉴权 | Spring Security + JWT（JJWT） | 0.12.6 | 无状态令牌 + URL 级角色隔离 |
| 密码加密 | BCrypt | 随 Security | `sys_user` 密码哈希 |
| 哈希存证 | SHA-256 | JDK 自带 | F10 哈希链 / F11 验真签名，零第三方库 |
| 对象存储 | 阿里云 OSS SDK | 3.18.1 | 可选，未配 Key 降级本机磁盘 |
| 代码简化 | Lombok | 随 Boot | `@Data`、`@Slf4j` 减少样板代码 |
| 运行监控 | Spring Actuator | 随 Boot | `/actuator/health` 健康检查 |
| 前端运行环境 | Node.js + npm | 18+（实测 24） | Vite 运行与依赖安装 |
| 前端框架 | Vue + Vite | 3.5.13 / 6.0.7 | 组合式 API，开发体验好 |
| 开发语言 | TypeScript | 5.7.2 | 类型安全 |
| 路由 / 状态 | Vue Router + Pinia | 4.5.0 / 2.3.0 | `meta.roles` 守卫 + Token/用户/权限码 |
| 请求 | Axios | 1.7.9 | 拦截器注入 JWT、统一错误、401 跳登录 |
| UI / 图表 | Element Plus + ECharts | 2.9.3 / 5.5.1 | 表单表格组件齐全 + 全部图表/大屏热力 |
| 图标 | @element-plus/icons-vue | 2.3.1 | Element Plus 图标组件 |
| 二维码 | qrcode.vue | 3.6.0 | 护照页 / 验真页二维码 |
| 样式 | Sass | 1.83.0 | SCSS 变量统一主题 |
| 地图 | 高德地图 JS API | 2.0 | 物流真实底图与划线，未配 Key 降级 Canvas |
| 大模型 / 视觉 | 阿里云百炼 DashScope | qwen-plus / qwen3-vl-plus | AI 碳管家对话 / 化验单多模态读图 |
| 部署 | Nginx + Docker Compose | — | 托管 dist + `/api` 反代 + SSE 长连接 |

## 目录结构

```text
黔能碳溯/
├─ backend/                         Spring Boot 3.3 单体服务（端口 8082）
│  ├─ pom.xml
│  ├─ .env                          真实密钥（DASHSCOPE_API_KEY 等，已被 Git 忽略）
│  └─ src/main/
│     ├─ java/com/qnct/
│     │  ├─ controller/             30 个控制器、91 个接口（五端 + 公开）
│     │  ├─ service/                业务服务（核算引擎 / 哈希链 / AI 网关 / 视觉网关 / 降级）
│     │  ├─ mapper/ entity/ dto/    MyBatis-Plus 数据访问（52 张表）
│     │  ├─ config/                 SecurityConfig（JWT + URL 角色隔离 + SSE ASYNC 放行）
│     │  ├─ security/               JwtAuthFilter / LoginUser
│     │  └─ common/ task/ util/     统一响应体 / 定时采集 / 工具
│     └─ resources/
│        ├─ application.yml         公共配置（${ENV} 占位，入库）
│        ├─ application-dev.yml     开发配置（演示默认值，入库）
│        ├─ application-prod.yml    生产配置（仅环境变量注入，入库）
│        ├─ application-local.yml   本机真实数据库密码（已被 Git 忽略）
│        └─ assets/seal/            签章素材
├─ frontend/                        Vue 3 + Vite + TS 前端（端口 5175）
│  ├─ package.json  vite.config.ts  /api 代理到 8082
│  ├─ .env.development / .env.production   VITE_USE_MOCK / VITE_AMAP_JS_KEY
│  └─ src/
│     ├─ views/                     33 个页面（企业 11 + 省厅 6 + 核查 3 + 国家 5 + 超管 5 + H5/登录/验真 3）
│     ├─ components/ layout/        StatCard / EmptyState / 主布局
│     ├─ router/ stores/ utils/     路由守卫 / Pinia / Axios 封装 / 演示账号
│     ├─ api/ config/ styles/       接口封装 / 配置 / 全局样式
│     └─ assets/                    自绘 Logo / 签章 / 插画（SVG/Canvas，零版权）
├─ sql/init.sql                     MySQL 8 全量 DDL（52 表）+ 演示数据
├─ docs/                            01 架构 / 02 目录 / 03 外部 API 清单 / 04 公式算法
├─ deploy/                          docker-compose.yml + nginx.conf + .env.example
├─ scripts/                         gen-assets.mjs（素材生成）/ start-backend.ps1
├─ 黔能碳溯_需求整合.docx            需求文档
├─ 功能模块树形图.png
└─ README.md
```

## 环境要求

JDK 17 · Maven 3.9+ · Node.js 18+（推荐 20/24 LTS）· MySQL 8.0+ · Redis 7（可选）· 端口 8082（后端）/ 5175（前端）

> MySQL / Redis 未启动时后端业务自动降级为演示数据，不影响看界面；外部 AI / 地图 / OSS 未配 Key 时同样自动降级。

## 快速开始

### 1. 初始化数据库

```bash
mysql -uroot -p --default-character-set=utf8mb4 < sql/init.sql
```

`init.sql` 建 `qnct` 库、52 张表并写入演示数据（含整改事项 / 审计日志 / 哈希链）。**不建库也能启动**，系统全站演示数据兜底。

### 2. 配置密钥（可选，不填走降级）

```bash
# 后端真实密钥：复制模板为 backend/.env（已被 .gitignore 忽略，不要提交）
cp deploy/.env.example backend/.env
# 按需回填 DASHSCOPE_API_KEY / AMAP_WEB_KEY / OSS_* 等
```

- 本机数据库密码写在 `backend/src/main/resources/application-local.yml`（已被 Git 忽略），或用环境变量 `MYSQL_PASSWORD` 注入。
- 前端高德 JS Key 在 `frontend/.env.development` 的 `VITE_AMAP_JS_KEY`（浏览器公开 Key，可入库；建议在控制台配域名白名单）。
- 所有填 Key 位置均带中文注释 `【此处需填入API Key】`，全局搜索即定位。

### 3. 启动后端

```bash
cd backend
# Windows 显式指定 Temurin17，不要用系统默认 JDK
export JAVA_HOME='D:\jdk\temurin-17\jdk-17.0.20.1+1'
mvn -DskipTests package          # → target/qnct-backend.jar
java -jar target/qnct-backend.jar
```

或直接在 IDEA 运行 `com.qnct.QnctApplication`（`application.yml` 已配 `spring.config.import` 自动读取 `backend/.env`，无需手动配环境变量）。服务：http://127.0.0.1:8082 ，健康检查 `/actuator/health`。

> **同一时间只能运行一个后端**：IDEA Run 与后台常驻实例（`scripts/start-backend.ps1`）共用 8082，端口冲突时先停掉另一个。

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev        # http://127.0.0.1:5175
```

Dev 代理自动把 `/api` 转发到后端 8082。生产构建：`npm run build`，产物 `frontend/dist`，由 Nginx 托管并将 `/api` 反代到 8082。

## 数据库与 SQL 脚本

| 文件 | 用途 | 说明 |
| --- | --- | --- |
| `sql/init.sql` | 建库建表（52 张表）+ 演示数据 | 首次初始化或重置时执行 |

52 张表按域划分：系统域 `sys_*`（组织/用户/角色/权限/字典/配置/通知/文件/审计/哈希链/服务器指标/接口调用/验真日志）、企业域 `ent_*`（产品/设施/活动数据/物流/整改事项/整改预算）、化验 `lab_report*`、供应链 `sc_*`、因子 `emission_factor`、核算 `carbon_*`、数据包 `data_package*`、护照 `passport_application` / `carbon_passport`、AI `ai_*` / `ocr_task`、省厅 `gov_*`、核查 `agency_review`、金融 `fin_*`、国家级 `nat_*`。

## 演示账号

| 端 | 账号 | 密码 | 首页 |
| --- | --- | --- | --- |
| 超级管理员（系统运营端） | `superadmin` | `admin888` | `/admin/dashboard` |
| 省生态环境厅（监管端） | `gov` | `gov888` | `/gov/big-screen` |
| 第三方核查机构（国际核查端） | `agency` | `agency888` | `/agency/dashboard` |
| 国家级审批部门（终审端） | `national` | `nat888` | `/national/issuance` |
| 企业（贵州黔能矿业） | `enterprise` | `ent888` | `/enterprise/dashboard` |

> 演示模式（`VITE_USE_MOCK=true`）下后端在线时优先真实登录拿 JWT，后端不可达时降级本地校验，保证化验单读图/入库等业务接口不会因假 Token 而 401 跳登录。

## 角色与界面入口

三层权限控制：前端路由 `meta.roles` → 侧边栏菜单过滤 → 后端 Spring Security URL 级角色隔离（`/api/v1/{admin,gov,agency,enterprise,national}/**`）+ JWT 无状态鉴权；数据按 `data_scope` / `org_id` 做国家—省—市—企业四级隔离。

- **企业端**：数据看板、碳数据填报、化验单识别、碳排核算与报告、AI 碳管家、护照申请、供应链协同、物流追踪、绿色金融、出海数据包、整改计划与预算。
- **省厅监管端**：态势大屏、智能初审台、核查派单、因子库管理、电子签章、移动执法 H5（`/checkin`）。
- **核查机构端**：数据看板、数据包接收、国际标准复核。
- **国家级审批部门端**：终审与发证中心、跨国互认监控、宏观预测驾驶舱、国际关税预警、企业黑名单通报。
- **超级管理员端**：运营看板、RBAC 权限控制、审计与可信存证中心、数据字典、系统运行与接口监控。
- **公开页**：扫码验真（`/verify`，无需登录）。

## 推荐演示路线

1. **填报 → 识别 → 核算**：`enterprise` 登录 → 碳数据填报月度活动数据 → 化验单识别上传照片（qwen3-vl-plus 读图抽出灰分/硫分/低位发热量）→ 人工确认入库 → 碳排核算与报告查看 F1~F5 明细。
2. **标准切换**：核算报告页切换「国家标准 / 行业标准 / 团体标准」→ EF、排放量、占比、图表、报告口径全页联动重算。
3. **AI 碳管家**：自然语言提问 → 带企业上下文流式（SSE）回答，政策解读 / 核算方法问答。
4. **物流追踪**：登记车牌与起终点 → 规划轨迹（高德真实底图绘线，未配 Key 自动 Canvas）→ 自动累计里程并按 F4 折算范围三排放。
5. **护照申请 → 省审**：企业提交护照申请（状态机填报 → 省审）→ `gov` 智能初审台比对历史/基准，异常标红 + 预警原因 → 电子签章盖初审意见书。
6. **核查派单 → 复核**：省厅对异常企业派单 → 移动执法 H5 GPS 打卡（F9 离场判定）→ `agency` 接收出海数据包、国际标准复核 → 提交复核结论推送国家级。
7. **终审发证 → 验真**：`national` 终审通过 → 生成护照编号、写哈希链（F10）、生成防伪二维码（F11）→ 公开 `/verify` 扫码验真，展示存证哈希与是否被篡改。
8. **绿色金融 / 整改 / 出海**：企业端绿色金融看信誉分雷达 + 预授信（F7/F8）；整改计划与预算 AI 生成措施 + 10% 预备费；出海数据包按 CBAM 模板导出 Excel。
9. **国家级宏观监管**：宏观预测驾驶舱 2030 情景外推 + 达峰结论；国际关税预警 CBAM 影响测算（F13，CN¥/€ 双币种）；跨国互认监控海外扫码回流；黑名单跨省通报与权限冻结。
10. **超管治理**：`superadmin` RBAC 配置四级隔离 → 审计与可信存证中心看操作日志、修改前后快照比对、SHA-256 一键验链异常标红 → 系统运行与接口监控。

## 核心规则与算法

后端全部由 Java 实现（JDK 自带 SHA-256，核算引擎为纯 Java 计算），详见 `docs/04-公式与算法说明.md`：

- **F1 基本公式**：`E = AD × EF × GWP`，活动数据 × 排放因子 × 全球变暖潜势。
- **F2 化石燃料燃烧**（IPCC 完全氧化法）：`E = FC × NCV × CC × OF × 44/12`，NCV 来自化验单识别字段，体现「识别 → 核算」闭环。
- **F3 外购电力**：`E = AD电 × EF电网 / 1000`，省级因子库版本管理（Scope 2）。
- **F4 运输**：`E = Σ(载重 t × 里程 km) × EF运输`，物流轨迹求和里程（Scope 3）。
- **F5 总量与碳强度**：`E总 = Σ范围1+2+3`，`I = E总 ÷ Q产品`（功能单位 1t/1kWh/1kg）。
- **F6 智能初审波动**：`r = |X本期 − X基准| ÷ max(X基准, ε)`，超 ±20% 判异常。
- **F7 绿色金融信誉分**：`S = 40×完整度 + 25×及时性 + 20×稳定性 + 15×历史合规`。
- **F8 预授信额度**：`Q = Q基准 × k等级`（S≥90→1.0，80~89→0.85，70~79→0.7，<70→0.5）。
- **F9 打卡离场判定**：Haversine 球面距离，`d ≤ radius_m` 判有效打卡。
- **F10 哈希链防篡改**：`h_i = SHA256(h_{i−1} ‖ data_i ‖ salt)`，链式牵制，任意篡改整链断裂。
- **F11 护照验真签名**：`token = SHA256(passport_no + ":" + site_salt)` 取前 16 位，二维码防伪。
- **F12 大屏热力分级**：市州排放 ÷ 全省总量映射五级着色（ECharts GeoJSON + visualMap）。
- **F13 CBAM 关税影响**：`影响额 ≈ Q出口 × I产品 × (1 − f免费配额) × R税率`，EU ETS 基准 71.50 €/tCO₂e。

## API 一览

共 **30 个控制器、91 个接口**，统一前缀 `/api/v1`，按端做 URL 级角色隔离：

| 端 | 控制器 | 前缀 | 接口数 | 主要职责 |
| --- | --- | --- | --- | --- |
| 认证 / 公开 | AuthController | `/auth` | 3 | 登录 / 验证码 / 当前用户 |
| 认证 / 公开 | PublicController | `/public` | 3 | 扫码验真（免登录） |
| 企业端 | EnergyDataController | `/enterprise/energy-data` | 3 | 碳数据填报 |
| 企业端 | LabVisionController | `/enterprise/lab-report` | 2 | 化验单多模态识别 / 确认入库 |
| 企业端 | CarbonCalcController | `/enterprise/calc` | 2 | 碳排核算 |
| 企业端 | ReportController | `/enterprise/report` | 2 | 核算报告生成 / 存证 |
| 企业端 | AiController | `/enterprise/ai` | 1 | AI 碳管家流式问答（SSE） |
| 企业端 | PassportController | `/enterprise/passport` | 2 | 护照申请状态机 |
| 企业端 | SupplierController | `/enterprise/supply` | 3 | 供应链协同 |
| 企业端 | LogisticsController | `/enterprise/logistics` | 3 | 物流轨迹 / 里程 / F4 |
| 企业端 | FinanceController | `/enterprise/finance` | 2 | 绿色金融信誉分 / 授信 |
| 企业端 | PackageController | `/enterprise/package` | 2 | 出海数据包（CBAM） |
| 企业端 | RectifyController | `/enterprise/rectify` | 4 | 整改计划 / 预算预估 / 提交 |
| 省厅端 | ScreenController | `/gov/screen` | 4 | 态势大屏 |
| 省厅端 | ReviewController | `/gov/review` | 4 | 智能初审 / 预警 |
| 省厅端 | InspectionController | `/gov/inspection` | 2 | 核查派单 |
| 省厅端 | CheckinController | `/gov/checkin` | 1 | 移动执法 GPS 打卡 |
| 省厅端 | FactorController | `/gov/factors` | 2 | 省级因子库管理 |
| 省厅端 | SealController | `/gov/seal` | 1 | 电子签章 |
| 核查机构端 | AgencyPackageController | `/agency/packages` | 2 | 数据包接收 / 下载 |
| 核查机构端 | AgencyReviewController | `/agency/review` | 3 | 国际标准复核 / 结论提交 |
| 国家级端 | NationalFinalIssueController | `/national/issuance` | 3 | 终审发证 / 哈希链 / 二维码 |
| 国家级端 | NationalMonitorController | `/national/monitor` | 3 | 跨国互认监控 |
| 国家级端 | NationalForecastController | `/national/forecast` | 2 | 宏观预测驾驶舱 |
| 国家级端 | NationalTariffController | `/national/tariff` | 3 | 国际关税预警（CBAM） |
| 国家级端 | NationalBlacklistController | `/national/blacklist` | 4 | 企业黑名单通报 |
| 超管端 | RbacController | `/admin/rbac` | 7 | 用户 / 角色 / 菜单 / 四级隔离 |
| 超管端 | AuditCenterController | `/admin/audit-center` | 5 | 操作审计 / 快照比对 / 验链 |
| 超管端 | DictController | `/admin/dict` | 8 | 数据字典 / 基础配置 |
| 超管端 | MonitorController | `/admin/monitor` | 5 | 系统运行 / 接口监控 |

统一响应体：`{ code, msg, data }`，`code=0` 成功；错误码：401 未登录 / Token 过期、403 越权、400 参数/业务校验、500 服务器异常。SSE/异步 dispatch 已放行二次鉴权，避免流式回答中途被截断。

## 构建与部署

```bash
# 后端打包
cd backend && mvn clean package -DskipTests

# 前端构建
cd frontend && npm run build      # 产物 frontend/dist

# Docker Compose 一键部署
cd deploy
cp .env.example .env              # 按需回填 API Key（不填也能起）
docker compose --env-file .env up -d --build
```

- 访问 http://localhost/ （Nginx 托管 `frontend/dist` + `/api` 反代后端）。
- `deploy/nginx.conf` 已针对 **SSE 长连接（AI 碳管家流式回答）** 关闭代理缓冲、放大超时。
- MySQL 首次启动自动执行 `sql/init.sql`（全量表结构 + 演示数据）。
- 回滚：保留上一版 jar 与 dist，切换后重启即可；数据库升级前必须 `mysqldump` 备份。

## 测试

- **关键链路浏览器实测**：真实 JWT 登录 → 化验单上传识别（qwen3-vl-plus，字段精确匹配）→ 确认入库（不再跳登录）→ AI 碳管家流式回答 → 核算标准切换（国标 12864.6 / 行标 13028.5 / 团标 12703.5 联动）→ 物流地图真实底图渲染。
- **接口冒烟**：91 个接口 × 五端角色权限矩阵，未登录 / 越权访问受保护接口返回 401 / 403。
- **降级验证**：MySQL / Redis / 大模型 / 地图 / OSS 任一不可用时自动降级演示数据，全站不白屏。
- **存证回归**：哈希链一键验链，篡改任意区块导致整链断裂并标红；公开验真页比对 token 与存证。
- **页面走查**：五端 33 个页面全角色回归（推荐演示路线即回归清单）。

## AI 使用说明

本项目开发过程中使用 AI 编程助手（Qoder 等）辅助：① 样板代码生成（CRUD 骨架、Vue 页面骨架、Excel/CSV 导出）；② 文档与 README 编写；③ 问题排查（SSE 异步鉴权截断、Mock Token 401 跳登录、高德底图白屏、核算标准联动）。核心算法（F1~F13 核算/哈希链/信誉分/CBAM 测算）按需求文档设计、AI 辅助实现，均经人工评审与浏览器实测验证。

## Git 提交说明

分支策略：`main`（可发布）+ 功能分支 `feature/<模块>`；提交信息格式 `type(scope): 摘要`，type ∈ feat/fix/docs/style/refactor/test/chore。

**不入库**（`.gitignore` 已排除）：`backend/.env`、`application-local.yml`（真实数据库密码 / API Key）、`node_modules/`、`dist/`、`backend/target/`、`uploads/`、`backend/data/`、`*.log`、`.idea/`、`.vscode/`。`application.yml` / `-dev.yml` / `-prod.yml` 仅含 `${ENV}` 占位或演示默认值，可安全入库；前端 `VITE_AMAP_JS_KEY` 为浏览器公开 Key，可入库（建议配域名白名单）。

## 常见问题

- **Windows 下 npm 命令找不到**：PowerShell 用 `npm.cmd run dev`（Git Bash 用 `npm` 即可）。
- **后端编译报 JDK 版本错误**：必须显式指定 Temurin17，系统默认 JDK 不可用。
- **IDEA 报「Port 8082 was already in use」**：已有另一个后端实例占用 8082，先停掉它（同一时间只能跑一个后端）。
- **接口报 500 / 无数据**：MySQL 未启动属预期降级，启动 MySQL 并导入 `sql/init.sql` 后自动切换真实数据。
- **AI 回答是固定话术**：未填 `DASHSCOPE_API_KEY`，自动走本地知识库降级；填入后即 qwen-plus 实时回答。旧模型 `qwen-vl-plus` 已下线，视觉统一用 `qwen3-vl-plus`。
- **物流地图白屏**：已修复——Canvas 先绘保底，高德脚本加载 + 底图 `complete` 双重门控成功后才升级真实底图，失败自动回退 Canvas 并提示，绝不白屏。
- **化验单入库后跳登录**：已修复——演示模式后端在线时优先真实登录拿 JWT，401 跳登录排除 `/auth/login`，识别/入库接口正确解包 `{code,msg,data}`。