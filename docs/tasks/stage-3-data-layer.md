# 阶段三：数据层改造 - 工作提示词

## 📌 任务概述
实现 NBT 版本管理和数据迁移机制，确保数据存储的兼容性和可靠性。

## 🎯 子代理需要处理的任务

### 任务 1.8: NBT 版本管理
**涉及文件**:
- `HeroSigilData.java` - 添加版本字段
- `DataSerializer.java` - NBT 序列化/反序列化
- `BuffEffect.java` - 版本兼容性处理

**关键修改点**:
1. 在 `HeroSigilData` 中添加 `version` 字段
2. 定义版本常量（VERSION_CURRENT = 1）
3. 修改 NBT 序列化，保存版本信息
4. 修改 NBT 反序列化，读取版本信息
5. 添加版本号检查逻辑

**验收标准**:
- [ ] NBT 数据包含版本号
- [ ] 正确读取和保存版本信息
- [ ] 版本号检查正常工作

### 任务 1.9: 数据迁移
**涉及文件**:
- `DataMigrator.java` - 迁移逻辑
- `HeroSigilData.java` - 迁移触发
- `DataSerializer.java` - 数据序列化

**关键修改点**:
1. 创建 `DataMigrator` 类
2. 定义迁移方法（按版本号）
3. 在反序列化后调用迁移逻辑
4. 实现版本升级时的数据转换
5. 添加迁移日志记录

**验收标准**:
- [ ] 旧版本数据能正确迁移到最新版本
- [ ] 迁移过程不丢失数据
- [ ] 迁移日志完整

## 🔧 执行步骤

### 步骤 1: 阅读任务文档
仔细阅读 `task-1.8-nbt-version.md`、`task-1.9-data-migration.md`

### 步骤 2: 阅读当前代码
阅读涉及的源文件，理解现有代码结构：
- [HeroSigilData.java](file:///c:/Users/shuiy/Desktop/CodeSYD/HeroSigil/src/main/java/com/hero/sigil/buffs/HeroSigilData.java)
- [DataSerializer.java](file:///c:/Users/shuiy/Desktop/CodeSYD/HeroSigil/src/main/java/com/hero/sigil/data/DataSerializer.java)

### 步骤 3: 按顺序实现
1. 先实现任务 1.8（版本管理）
2. 再实现任务 1.9（数据迁移）

### 步骤 4: 验证与测试
- [ ] 编译检查通过
- [ ] 版本管理正常工作
- [ ] 数据迁移正确

## ⚠️ 注意事项
1. 版本号应从 1 开始递增
2. 迁移逻辑应在反序列化后立即执行
3. 添加充分的日志记录
4. 确保数据迁移的原子性（成功或回滚）
5. 遵循内存优化原则，避免不必要的数据复制
