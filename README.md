<div align="center">
  <h1>⛽ 油耗记录 · Fuel Tracker</h1>
  <p>Android 平台油耗管理应用 · Kotlin + Jetpack Compose</p>
</div>

## 📱 功能

- **加油记录** — 记录里程、油量、油价、费用，支持双向自动计算
- **多车辆管理** — 小踏板、大摩托、轿车、SUV 等车型，独立续航设置
- **漏记检测** — 根据车型续航自动判断是否漏记，历史记录 ⚠️ 标记
- **统计图表** — 油耗走势、月度费用、加油量、油价趋势（Canvas 绘制）
- **CSV 导入/导出** — 支持 Excel 打开，批量导入历史数据
- **一键分享截图** — 生成风格化统计图片分享
- **暗色模式** — 跟随系统自动切换

## 🛠 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Kotlin |
| UI | Jetpack Compose + Material 3 |
| 数据库 | Room（4 次迁移，v4）|
| 图表 | 自定义 Canvas 绘制（零外部依赖）|
| 构建 | Gradle + R8 混淆 |
| 最低版本 | Android 8.0 (API 26) |

## 📦 项目结构

```
app/src/main/java/com/fuelapp/
├── data/              # Room 实体 + DAO + 数据库
│   ├── FuelRecord.kt     加油记录表
│   ├── Vehicle.kt        车辆表
│   ├── FuelDao.kt        加油记录 DAO
│   ├── VehicleDao.kt     车辆 DAO
│   ├── FuelDatabase.kt   数据库（v4）
│   └── FuelRepository.kt 仓库层
├── ui/
│   ├── screens/          三个主页面
│   │   ├── AddRecordScreen.kt     添加记录
│   │   ├── HistoryScreen.kt       历史记录
│   │   ├── StatisticsScreen.kt    统计图表
│   │   └── ChartComponents.kt     图表组件
│   ├── components/       可复用组件
│   │   └── VehicleComponents.kt   车辆选择/管理
│   ├── navigation/       导航
│   │   └── NavGraph.kt
│   └── theme/            Material 3 主题
├── network/              （已移除云同步）
└── MainActivity.kt      入口
```

## 🚀 构建

```bash
# Debug APK
./gradlew assembleDebug

# Release APK（下载安装）
./gradlew assembleRelease
```

## 📄 隐私政策

本应用**不上传任何数据**到服务器，所有数据仅保存在本地设备 SQLite 数据库中。

## 📝 许可证

MIT License
