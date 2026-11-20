# Snishaper-Droid-Native

SniShaper Android 客户端的**原生前端**版本：后端不变（snishaper-core gomobile AAR +
Kotlin 引擎宿主 + VpnService），前端由 React/WebView 重写为 Jetpack Compose。

> M0 骨架已完成并通过本地构建验证（assembleDebug / lint / test）。模块划分、依赖规则与桥接契约迁移映射
> 见 **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)**（施工契约，先评审后动工）。

姊妹项目：[Snishaper-Droid](https://github.com/SnishaperTeam/Snishaper-Droid)（WebView 版，
功能对齐的参照实现）、[snishaper-core](https://github.com/SnishaperTeam/snishaper-core)（代理引擎）。

## 许可

[GNU Affero General Public License v3.0](LICENSE)（AGPL-3.0）
