# Snishaper-Droid-Native 架构与模块边界

> 状态：**边界设计稿 v1（待评审）**。本文档是本仓库的施工契约：先定边界，再动代码。
> 后端（gomobile AAR + Kotlin 引擎宿主 + VpnService）**原样迁移、行为不变**；前端由
> React 19 + MUI（WebView 渲染）重写为 **Jetpack Compose 原生 UI**。

---

## 1. 目标与不变量

| 项 | 决定 |
|---|---|
| 后端引擎 | snishaper-core 经 gomobile bind 为 AAR，**不改动**（沿用上游 v0.8.8-ksu.2 补丁线） |
| Kotlin 宿主层 | `SnishaperEngine` / `TunVpnService` / `VpnInstaller` / `AssetSeeder` 原样迁移 |
| 前端 | 全部重写为 Compose（Material 3），**不再有 WebView / Wails 桥** |
| 配置兼容 | 设置与规则仍存于引擎（settings/rules JSON），导出格式与桌面版互通 |
| 最低系统 | Android 7.0（API 24），与现仓库一致 |
| 语言 | Kotlin 2.x + 协程 Flow；UI 线程模型遵循 Compose 规范 |

**关键不变量（来自现仓库 EngineBridge 的教训）**：gomobile 暴露的是桌面版完整字段形状，
`*JSON` 方法返回的字符串必须**原样透传、只在唯一解析点反序列化**，禁止中途重编码丢字段。

---

## 2. 模块清单与依赖规则

```text
:app ──────────────► :feature:* ──────────────► :core:data ──► :core:engine ──► (AAR)
  │                      │                          │                          │
  ▼                      ▼                          ▼                          ▼
导航宿主/DI 装配    :core:design  :core:model    :core:model（唯一解析点）   snishaper-core
                       ▲              ▲
                       │              │
                  所有 feature     所有层
```

### 2.1 core 层（自底向上，禁止反向依赖）

| 模块 | 职责 | 依赖 | 禁止 |
|---|---|---|---|
| `:core:model` | 引擎 JSON → Kotlin 数据模型的**唯一解析点**；事件与状态的数据类 | 无（纯 Kotlin） | 不得依赖 Android/引擎 |
| `:core:engine` | gomobile AAR 接入、`SnishaperEngine` 单例、Listener→Flow 转换 | `:core:model` | 不得出现业务逻辑 |
| `:core:data` | 按领域封装引擎调用的仓库层；**全仓库唯一允许调用 engine 的模块** | engine, model | 不得依赖 UI/feature |
| `:core:design` | 设计系统：6 套配色 × 明暗主题、字体、通用组件、图标 | 无 | 不得依赖 data/engine |
| `:core:common` | 结果封装、调度器注入、DataStore（教程标记等纯前端状态） | 无 | — |

### 2.2 feature 层（页面域，互相之间禁止依赖）

| 模块 | 对应旧页面 | 主要仓库依赖 | 规模预估 |
|---|---|---|---|
| `:feature:dashboard` | Dashboard + Welcome 引导入口 | proxy, tun, config, notices | 中 |
| `:feature:connections` | Connections | connections | 小 |
| `:feature:rules` | Rules + RuleForm（最大单页） | rules | 大 |
| `:feature:dns` | DNS（节点 + ECH + NAT64 + 迁移服务器） | dns, ech, nat64, migration | 中 |
| `:feature:proxies` | Proxies（Cloudflare + 多 CDN 池） | ippool | 中 |
| `:feature:routing` | Routing（站点分组） | rules | 小 |
| `:feature:evolution` | Evolution | evolution | 中 |
| `:feature:logs` | Logs | logs | 小 |
| `:feature:profiles` | Profiles | profile, config | 小 |
| `:feature:settings` | Settings + 证书 | settings, cert, theme | 中 |
| `:feature:onboarding` | Welcome + Tutorial + About | appinfo, common | 小 |

每个 feature 模块内部固定三层：`ui/`（Compose 页面 + 组件）、`vm/`（ViewModel，状态用
StateFlow）、`navigation/`（对外暴露的路由常量）。feature 之间跳转只走 `:app` 的导航图，
跨模块数据只经 `:core:model` 的共享类型。

### 2.3 `:app` 壳

- `SnishaperApplication`：初始化引擎、装配 `AppContainer`（手动 DI，见 §5）。
- `MainActivity`：单 Activity + Navigation Compose；持有前台服务通知逻辑。
- 导航图：底部/侧边导航 11 个目的地，路由常量从各 feature 收集。

---

## 3. 数据流（单向）

```text
gomobile App（AAR）
   │  *JSON 字符串 / Listener 回调
   ▼
:core:engine  SnishaperEngine（原样迁移）
   │  Flow<EngineStatus> / Flow<RouteEvent> / Flow<EvolutionTask>
   ▼
:core:data    XxxRepository（挂起函数 + Flow，错误统一 Result 封装）
   │  领域模型（:core:model）
   ▼
feature vm    ViewModel（StateFlow<UiState>）
   ▼
feature ui    Compose（无状态渲染，事件上抛）
```

事件迁移映射（旧 → 新）：

| 旧 Wails 事件 | 新 Flow | 说明 |
|---|---|---|
| `app:state` / `app:state_changed` | `SnishaperEngine.status: StateFlow` | 引擎状态 |
| `app:route` | `SnishaperEngine.routes: SharedFlow` | 实时连接命中 |
| `evolution:progress` / `evolution:complete` | `SnishaperEngine.evolution: StateFlow` | 进化测试 |

---

## 4. 桥接契约迁移映射（bindings.ts → 仓库接口）

以下为旧前端 `bindings.ts` 全部方法的归属。迁移时逐条核对参数与返回 JSON 形状。

| 仓库（:core:data） | 方法 |
|---|---|
| `SettingsRepository` | GetLanguage, SetLanguage, GetTheme, SetTheme, GetAppearanceMode, SetAppearanceMode, GetAutoUpdateRules, SetAutoUpdateRules, UpdateRules |
| `AppInfoRepository` | GetAppVersion, GetBuildInfo |
| `NoticesRepository` | GetNotices |
| `ProxyRepository` | GetListenPort, SetListenPort, GetProxyMode, IsProxyRunning, StartProxy, StopProxy, GetStats, GetAutoRoutingStatus |
| `TunRepository` | GetTUNConfig, UpdateTUNConfig, GetTUNStatus, StartTUN, StopTUN, GetIPv6Available, RefreshIPv6Check |
| `RulesRepository` | GetSiteGroups, AddSiteGroup, UpdateSiteGroup, DeleteSiteGroup, GetRulesLayerInfo, RevertSiteGroupToUpstream |
| `ConfigRepository` | ExportConfig, ImportConfigWithSummary |
| `DnsRepository` | GetDNSNodes, AddDNSNode, UpdateDNSNode, DeleteDNSNode, SetDNSNodePriority, TestDNSNode |
| `EchRepository` | GetECHProfiles, UpsertECHProfile, DeleteECHProfile, FetchECHConfig |
| `Nat64Repository` | GetNAT64Profiles, AddNAT64Profile, UpdateNAT64Profile, DeleteNAT64Profile, TestNAT64Profile |
| `MigrationRepository` | GetMigrationServer, SetMigrationServer, TestMigration |
| `IpPoolRepository` | GetCloudflareConfig, UpdateCloudflareConfig, GetCloudflareIPStats, ForceFetchCloudflareIPs, TriggerCFHealthCheck, RemoveInvalidCFIPs, GetIPPools, UpdateIPPools, GetIPPoolStats, ForceFetchIPPoolIPs, RefreshIPPool, RemoveInvalidIPPoolIPs, TriggerIPPoolHealthCheck |
| `ConnectionsRepository` | GetActiveConnections, ExplainMatch |
| `LogsRepository` | GetRecentLogs, ClearLogs |
| `EvolutionRepository` | StartEvolutionTest, StopEvolutionTest, ApplyEvolutionRule, GetEvolutionTestStatus |
| `CertRepository` | InstallCA, RegenerateCert, UninstallCert, GetCAInstallStatus, GetInstalledCerts, GetCACertPEM |
| `ProfileRepository` | ListProfiles, GetActiveProfile, CreateProfile, RenameProfile, DuplicateProfile, SwitchProfile, ExportProfile, ImportProfile, GetLastMigrationReport |
| 平台能力（保留原 Kotlin 实现） | SaveFile, ExportDiagnostics → `FileExporter`；OpenURL → Intent |
| 原生自有（不再走引擎） | GetTutorialDone, SetTutorialDone → DataStore（原为 WebView localStorage） |

**边界要点**：

1. 语言/主题/外观模式**必须继续走引擎存取**——它们在 settings JSON 里，是配置导出的
   一部分；原生端自行存 DataStore 会导致导出配置丢主题（明确禁止）。
2. 教程完成标记原为 localStorage，属纯前端状态，迁移到 DataStore，不进引擎。
3. `StartTUN` 在原生端由 `TunVpnService`（原样迁移）发起，仓库只转发引擎调用与状态。

---

## 5. 技术选型（决策记录）

| 决策点 | 选择 | 理由 |
|---|---|---|
| UI 框架 | Jetpack Compose + Material 3 | "原生前端"的标准答案；声明式 UI 与现 React 心智模型对齐 |
| DI | 手动 `AppContainer` | 引擎为单例、依赖面窄；避免 Hilt 的构建复杂度，与现仓库一致 |
| 异步 | 协程 + Flow | 引擎 Listener 已是回调，转 Flow 成本最低 |
| 序列化 | kotlinx.serialization（`ignoreUnknownKeys`） | 唯一解析点集中；字段形状照抄桌面版 |
| 持久化（纯前端态） | DataStore Preferences | 仅教程标记等，不碰引擎配置 |
| i18n | Android strings.xml（zh/en/ru） | 词条约 25KB×3，随 feature 模块拆分放置 |
| 构建 | AGP 9.x + Kotlin 2.x + 版本目录（libs.versions.toml） | 与现仓库工具链对齐，CI 可复用 |
| AAR 产物 | 沿用现仓库 `gomobile bind` 流水线，产物作为本仓库依赖输入 | 后端不变 |

---

## 6. 里程碑（每步可独立验收）

| 阶段 | 内容 | 验收标准 |
|---|---|---|
| M0 骨架 | Gradle 多模块 + 依赖规则 + 空壳可编译 | CI `assembleDebug` 绿；feature 互依即编译失败 |
| M1 引擎贯通 | core:engine/data 迁移 + Dashboard 最小闭环（启动/停止/状态） | 真机可启停代理，状态实时刷新 |
| M2 核心页 | Connections / Logs / Rules / Routing | 与旧版功能对齐清单逐条勾 |
| M3 网络配置 | DNS / ECH / NAT64 / IP 池 / 迁移服务器 | 测试按钮行为与旧版一致 |
| M4 档案与设置 | Profiles / Settings / 证书 / 导入导出 | 配置导出与桌面版互通 |
| M5 收尾 | Evolution / Onboarding / 主题×6 / 三语 / 诊断包 | 全功能对齐，发布 CI |

---

## 7. 待确认决策点

1. **仓库/应用名**：暂定 `Snishaper-Droid-Native`，可改。
2. **applicationId**：建议保持 `com.snishaper.android`（作为正式替代，覆盖安装升级）；
   debug 构建加 `.debug` 后缀便于与旧版并行测试。若需两版共存请提出。
3. **feature 模块粒度**：目前 11 个 feature 模块按页面域拆分；如嫌碎可合并为
   3~4 个大域（network / config / system）。
4. **UI 语言**：Compose + Material 3 动态取色仅 Android 12+，12 以下回退静态主题——已按此设计。
