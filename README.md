# Hero Sigil (勇者之证) - NeoForge 1.21.1 Accessory Mod

## 📋 模组简介

一个成长型饰品模组，玩家通过完成成就来解锁不同的增益效果（Buff）。装备"勇者之证"后，可以根据完成的成就可以开启不同的 buff 槽位。

## 🏗️ 项目结构

```
HeroSigil/
├── src/main/java/com/hero/sigil/
│   ├── HeroSigil.java              # 模组主类
│   ├── Config.java                  # 配置文件
│   ├── item/
│   │   └── HeroSigilItem.java      # 勇者之证饰品核心逻辑
│   ├── achievement/
│   │   └── AchievementTracker.java # 成就追踪系统
│   └── registry/
│       └── ModRegistries.java      # 注册表统一管理
├── src/main/resources/
│   ├── META-INF/neoforge.mods.toml # NeoForge 模组配置
│   ├── assets/herosigil/lang/      # 本地化文件（中英文）
│   └── data/herosigil/curios/      # Curios API 饰品槽位定义
├── build.gradle                    # Gradle 构建配置
└── gradle.properties               # 项目属性配置
```

## 🔧 技术栈

- **Minecraft**: 1.21.1
- **NeoForge**: 21.1.235+
- **Curios API**: 9.5.1 (可选依赖，增强饰品槽位支持)
- **Java**: 21
- **Gradle**: 构建工具

## 📦 核心功能模块

### 1. HeroSigilItem（勇者之证饰品）
- 通过 Curios API 装备在自定义槽位
- 提供基础 passive effects
- GUI 管理 buff 解锁界面

### 2. AchievementTracker（成就追踪系统）
- 监听玩家行为事件（击杀、探索等）
- 记录进度并自动解锁 buff 槽位
- 支持多阶段成就链

### 3. ModRegistries（注册表管理）
- 统一管理物品、 Creative Tab、配置
- 使用 NeoForge DeferredRegister API

## 🎮 游戏内玩法流程

1. **获取饰品**：通过合成或探索获得"勇者之证"
2. **装备饰品**：在 Curios 界面中装备到指定槽位
3. **完成成就**：击败 Boss、探索群系等完成任务
4. **解锁 Buff**：每个成就会解锁一个增益效果
5. **自定义搭配**：玩家可选择激活哪些 buff

## 📝 待实现功能（TODO）

- [ ] GUI 界面开发（Buff 选择与管理）
- [ ] 完整的成就列表与进度追踪
- [ ] 具体 Buff 效果实现（饱和、力量等）
- [ ] 饰品模型与渲染（3D 模型 + 粒子特效）
- [ ] 数据持久化（NBT/Advancement API）
- [ ] 网络同步（客户端 - 服务器数据一致）
- [ ] 配置文件支持（调整 buff 强度、成就条件等）

## 🚀 构建与运行

```bash
# 清理并构建项目
./gradlew clean build

# 运行开发环境客户端
./gradlew runClient

# 运行开发环境服务器
./gradlew runServer

# 生成资源文件（模型、语言包等）
./gradlew data
```

## 🔗 依赖说明

### 必需依赖
- NeoForge 21.1.235+

### 可选依赖
- Curios API 9.5.1+ (增强饰品槽位支持)

如果未安装 Curios，模组仍然可以运行但会使用默认的装备系统。

## 📄 License

LGPL-3.0-only
