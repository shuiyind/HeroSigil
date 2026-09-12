# 任务 1.8: NBT 版本管理

## 任务目标
为 Hero Sigil 的 NBT 数据添加版本管理功能，支持未来数据结构的平滑升级。

## 涉及文件
- [HeroSigilData.java](file:///c:/Users/shuiy/Desktop/CodeSYD/HeroSigil/src/main/java/com/hero/sigil/buffs/HeroSigilData.java) - 主要修改
- [CapabilityEvents.java](file:///c:/Users/shuiy/Desktop/CodeSYD/HeroSigil/src/main/java/com/hero/sigil/persistence/CapabilityEvents.java) - 次要修改
- [HeroSigil.java](file:///c:/Users/shuiy/Desktop/CodeSYD/HeroSigil/src/main/java/com/hero/sigil/HeroSigil.java) - 可选：添加版本常量

## 当前代码分析

### HeroSigilData.java 当前实现
- `onSave()` 方法 (第 37-52 行): 保存 buff 状态到 NBT，但没有版本号
- `onLoad()` 方法 (第 57-81 行): 加载 NBT 数据，假设数据结构固定

**问题**: 当未来添加新 buff 或修改数据结构时，旧玩家数据可能不兼容，没有版本检测机制。

## 实现步骤

### 步骤 1: 添加版本常量
在 `HeroSigilData` 类中添加版本管理常量：

```java
// 在类的顶部添加
private static final int CURRENT_DATA_VERSION = 1;
private static final String VERSION_TAG = "dataVersion";
```

### 步骤 2: 修改 onSave() 方法
在 `onSave()` 方法中保存版本号：

**修改位置**: HeroSigilData.java 第 37-52 行

**当前代码**:
```java
public static void onSave(Player player, CompoundTag nbt) {
    CompoundTag sigilData = new CompoundTag();
    
    java.util.List<BuffEffect> buffs = getAllBuffs();
    for (int i = 0; i < Math.min(buffs.size(), 3); i++) {
        BuffEffect buff = buffs.get(i);
        String slotKey = "slot_" + (i + 1);
        
        CompoundTag slotData = new CompoundTag();
        slotData.putBoolean("unlocked", buff.isUnlocked());
        slotData.putBoolean("active", buff.isActive());
        sigilData.setTag(slotKey, slotData);
    }
    
    nbt.put(TAG_KEY, sigilData);
}
```

**修改为**:
```java
public static void onSave(Player player, CompoundTag nbt) {
    CompoundTag sigilData = new CompoundTag();
    
    // 保存版本号
    sigilData.putInt(VERSION_TAG, CURRENT_DATA_VERSION);
    
    java.util.List<BuffEffect> buffs = getAllBuffs();
    for (int i = 0; i < Math.min(buffs.size(), 3); i++) {
        BuffEffect buff = buffs.get(i);
        String slotKey = "slot_" + (i + 1);
        
        CompoundTag slotData = new CompoundTag();
        slotData.putBoolean("unlocked", buff.isUnlocked());
        slotData.putBoolean("active", buff.isActive());
        sigilData.setTag(slotKey, slotData);
    }
    
    nbt.put(TAG_KEY, sigilData);
}
```

### 步骤 3: 修改 onLoad() 方法
在 `onLoad()` 方法中检测版本号并处理不同版本：

**修改位置**: HeroSigilData.java 第 57-81 行

**当前代码**:
```java
public static void onLoad(Player player, CompoundTag nbt) {
    if (nbt.contains(TAG_KEY)) {
        CompoundTag sigilData = nbt.getCompound(TAG_KEY);
        
        java.util.List<BuffEffect> buffs = getAllBuffs();
        for (int i = 0; i < Math.min(buffs.size(), 3); i++) {
            BuffEffect buff = buffs.get(i);
            String slotKey = "slot_" + (i + 1);
            
            if (sigilData.contains(slotKey)) {
                CompoundTag slotData = sigilData.getCompound(slotKey);
                buff.setUnlocked(slotData.getBoolean("unlocked"));
                buff.setActive(slotData.getBoolean("active"));
                
                if (buff.isActive() && buff.isUnlocked()) {
                    buff.applyBuff(player);
                }
            }
        }
    } else {
        applyDefaultBuffsForPlayer(player);
    }
}
```

**修改为**:
```java
public static void onLoad(Player player, CompoundTag nbt) {
    if (nbt.contains(TAG_KEY)) {
        CompoundTag sigilData = nbt.getCompound(TAG_KEY);
        
        // 获取并处理版本号
        int dataVersion = sigilData.getInt(VERSION_TAG);
        if (dataVersion < CURRENT_DATA_VERSION) {
            sigilData = migrateData(dataVersion, sigilData, player);
        }
        
        java.util.List<BuffEffect> buffs = getAllBuffs();
        for (int i = 0; i < Math.min(buffs.size(), 3); i++) {
            BuffEffect buff = buffs.get(i);
            String slotKey = "slot_" + (i + 1);
            
            if (sigilData.contains(slotKey)) {
                CompoundTag slotData = sigilData.getCompound(slotKey);
                buff.setUnlocked(slotData.getBoolean("unlocked"));
                buff.setActive(slotData.getBoolean("active"));
                
                if (buff.isActive() && buff.isUnlocked()) {
                    buff.applyBuff(player);
                }
            }
        }
        
        // 更新为最新版本
        sigilData.putInt(VERSION_TAG, CURRENT_DATA_VERSION);
    } else {
        applyDefaultBuffsForPlayer(player);
    }
}
```

### 步骤 4: 添加数据迁移方法
在 `HeroSigilData` 类中添加迁移方法：

**添加位置**: 在 `onLoad()` 方法之后添加

```java
/**
 * 根据旧版本迁移数据到当前版本
 */
private static CompoundTag migrateData(int oldVersion, CompoundTag data, Player player) {
    HeroSigil.LOGGER.info("Migrating Hero Sigil data from version {} to {}", oldVersion, CURRENT_DATA_VERSION);
    
    // 当前版本为 1，暂无迁移逻辑
    // 未来版本升级时在此添加迁移规则
    // 示例：
    // if (oldVersion == 0) {
    //     // 从 v0 迁移到 v1 的规则
    // }
    
    // 迁移完成后更新版本号
    data.putInt(VERSION_TAG, CURRENT_DATA_VERSION);
    
    HeroSigil.LOGGER.info("Data migration completed successfully");
    return data;
}
```

### 步骤 5: 添加版本检查日志
在 `onLoad()` 方法中添加版本检查日志：

**修改位置**: HeroSigilData.java 第 57-81 行附近

添加以下日志代码：
```java
// 在加载前添加日志
int dataVersion = sigilData.getInt(VERSION_TAG);
HeroSigil.LOGGER.info("Loading Hero Sigil data version {} for player {}", dataVersion, player.getScoreboardName());
```

## 验收标准
- [ ] `onSave()` 方法中保存了 `dataVersion` 字段
- [ ] `onLoad()` 方法中读取并验证版本号
- [ ] 版本号小于当前版本时调用迁移方法
- [ ] 迁移方法正确更新版本号
- [ ] 加载和迁移过程有日志记录
- [ ] 首次创建的数据版本号为 1
- [ ] 代码注释清晰，说明版本号用途

## 注意事项
1. 版本号常量 `CURRENT_DATA_VERSION` 初始值为 1
2. 迁移方法应使用 `HeroSigil.LOGGER` 记录日志
3. 迁移过程中不要改变玩家的 buff 状态
4. 保持向后兼容：新版本必须能读取旧版本数据

## 测试建议
1. 启动游戏，创建新玩家，检查 NBT 数据中是否有 `dataVersion: 1`
2. 修改 `CURRENT_DATA_VERSION = 2`，重启游戏，检查迁移日志
3. 验证迁移后 buff 状态正确加载
