# Hook Mod Fabric

一个基于 Fabric 的 Minecraft 勾爪 Mod。当前版本为玩家添加了可装备的勾爪物品，玩家需要将勾爪装备到 Trinkets 的 `offhand/hook` 槽位中，然后通过可自定义按键发射勾爪。

## 当前版本特性

- 添加物品：`hook:hook`
- 支持将勾爪装备到 Trinkets 装备槽
- 装备槽位移动到 `offhand/hook`
- 默认按 `R` 发射勾爪
- 按键支持在游戏控制设置中自定义
- 右键不再触发勾爪
- 未装备勾爪时按键不会显示提示信息
- 勾爪投射物支持命中方块与实体
- 命中方块后将玩家沿直线拉向目标点
- 支持冷却、耐久消耗、最大距离、拉力等配置
- 支持方块抓取白名单与黑名单标签

## 运行环境

| 项目             | 版本               |
| ---------------- | ------------------ |
| Minecraft        | 26.1.2             |
| Fabric Loader    | 0.19.2             |
| Fabric API       | 0.149.0+26.1.2     |
| Java             | 25                 |
| Trinkets Updated | 4.0.0-alpha.9+26.1 |

## 依赖配置

### `gradle.properties`

```properties
minecraft_version=26.1.2
loader_version=0.19.2
fabric_api_version=0.149.0+26.1.2
trinkets_version=4.0.0-alpha.9+26.1
```

### `build.gradle`

确保添加 Nucleoid Maven 仓库：

```gradle
repositories {
    maven {
        name = "Nucleoid"
        url = "https://maven.nucleoid.xyz/releases"
    }
}
```

确保添加 Trinkets Updated 依赖：

```gradle
dependencies {
    modImplementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_api_version}"
    implementation "eu.pb4:trinkets:${project.trinkets_version}"
}
```

## 安装与运行

克隆项目后，在项目根目录执行：

```powershell
./gradlew clean build
```

开发环境启动客户端：

```powershell
./gradlew runClient
```

如果依赖没有刷新，可以执行：

```powershell
./gradlew --refresh-dependencies
```

## 使用方式

1. 进入游戏后获取 `hook:hook`。
2. 打开 Trinkets 装备界面。
3. 将勾爪放入 `offhand/hook` 槽位。
4. 关闭背包，按默认按键 `R` 发射勾爪。
5. 可以在 `选项 -> 控制 -> 按键绑定 -> Hook Mod -> 使用勾爪` 中修改按键。

当前版本中，右键不会再触发勾爪。没有装备勾爪时按下按键不会显示提示信息。

## Trinkets 槽位数据

### 玩家槽位声明

文件路径：

```text
src/main/resources/data/trinkets/entities/hook.json
```

内容：

```json
{
  "entities": [
    "minecraft:player"
  ],
  "slots": [
    "offhand/hook"
  ]
}
```

### 槽位定义

文件路径：

```text
src/main/resources/data/trinkets/slots/offhand/hook.json
```

内容：

```json
{
  "icon": "hook:container/slots/hook",
  "order": 10,
  "amount": 1,
  "drop_rule": "default",
  "is_hidden": false
}
```

### 允许放入槽位的物品

文件路径：

```text
src/main/resources/data/trinkets/tags/item/offhand/hook.json
```

内容：

```json
{
  "replace": false,
  "values": [
    "hook:hook"
  ]
}
```

如当前 Trinkets 版本需要复数路径，也可以同步保留：

```text
src/main/resources/data/trinkets/tags/items/offhand/hook.json
```

内容相同。

## 按键绑定

默认按键为：

```text
R
```

按键注册在客户端侧，触发流程为：

```text
HookKeyBindings
    -> ClientPlayNetworking.send(new UseHookPayload(true))
    -> ModNetwork
    -> HookUseHandler.useEquippedHook(player)
    -> HookProjectileEntity.shoot(level, player)
```

玩家可以在游戏内控制设置中自定义按键。

## 配置文件

配置文件会生成在：

```text
run/config/hook.json
```

常见配置项包括：

```json
{
  "maxDistance": 32.0,
  "minDistance": 2.0,
  "useCooldownTicks": 20,
  "blockCooldownTicks": 20,
  "entityCooldownTicks": 25,
  "blockPullStrength": 2.8,
  "entityPullStrength": 1.6,
  "blockVerticalBoost": 0.0,
  "entityVerticalBoost": 0.8,
  "maxPullVelocity": 6.5,
  "maxHorizontalVelocity": 6.5,
  "maxVerticalVelocity": 6.5,
  "reduceFallDamage": true,
  "allowPullPlayers": true,
  "allowPullBosses": false,
  "durabilityCost": 1
}
```

如果添加了投射物速度配置，也可以包含：

```json
{
  "projectileSpeed": 3.8
}
```

## 方块标签

### 可抓取方块白名单

```text
src/main/resources/data/hook/tags/block/hook_grabbable.json
```

### 不可抓取方块黑名单

```text
src/main/resources/data/hook/tags/block/hook_blacklist.json
```

黑名单优先级高于白名单。默认情况下，如果方块有碰撞体，则可以被勾爪抓取。

## 资源文件结构

常见资源路径：

```text
src/main/resources/assets/hook/models/item/hook.json
src/main/resources/assets/hook/textures/item/hook.png
src/main/resources/assets/hook/textures/gui/sprites/container/slots/hook.png
src/main/resources/assets/hook/lang/en_us.json
src/main/resources/assets/hook/lang/zh_cn.json
```

## 主要代码结构

```text
src/main/java/com/lxy/hook/HookMod.java
src/main/java/com/lxy/hook/config/HookConfig.java
src/main/java/com/lxy/hook/item/HookItem.java
src/main/java/com/lxy/hook/item/ModItems.java
src/main/java/com/lxy/hook/entity/HookProjectileEntity.java
src/main/java/com/lxy/hook/entity/ModEntityTypes.java
src/main/java/com/lxy/hook/network/UseHookPayload.java
src/main/java/com/lxy/hook/network/ModNetwork.java
src/main/java/com/lxy/hook/util/HookEquipment.java
src/main/java/com/lxy/hook/util/HookUseHandler.java
src/main/java/com/lxy/hook/util/PlayerPullManager.java
src/client/java/com/lxy/hook/client/HookModClient.java
src/client/java/com/lxy/hook/client/HookKeyBindings.java
src/client/java/com/lxy/hook/client/render/HookRopeRenderer.java
```

## 开发说明

### 勾爪使用入口

当前版本不再通过 `HookItem.use()` 或 `HookItem.useOn()` 使用勾爪。右键逻辑应保持 `PASS`，避免和按键触发逻辑冲突。

勾爪真正的使用入口是：

```java
HookUseHandler.useEquippedHook(ServerPlayer player)
```

该方法会：

1. 检查玩家是否装备了勾爪。
2. 检查勾爪是否处于冷却。
3. 添加冷却。
4. 发射 `HookProjectileEntity`。

### 装备检查

装备检查由 `HookEquipment` 完成。当前推荐逻辑是只检查 Trinkets 装备槽：

```java
TrinketsApi.getAttachment(player)
        .findFirst(ModItems.HOOK)
        .map(TrinketSlotAccess::get)
        .orElse(ItemStack.EMPTY);
```

### 拉取逻辑

方块拉取推荐使用持续 tick 牵引，避免玩家因为 Minecraft 重力而形成明显抛物线。当前拉取流程建议由 `PlayerPullManager` 维护，每 tick 将玩家速度修正到玩家当前位置指向命中点的方向。

## License

本项目当前使用仓库中声明的许可证。