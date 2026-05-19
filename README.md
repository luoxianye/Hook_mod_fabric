# Hook Mod Fabric

一个基于 Fabric 的 Minecraft 勾爪 Mod。玩家可以将勾爪装备到 Trinkets 的 `offhand/hook` 槽位中，并通过可自定义按键发射勾爪、切换勾爪模式、释放固定中的勾爪。

当前文档适用于 **v2.1.0**。

## 当前版本特性

- 添加物品：`hook:hook`
- 支持生存模式合成勾爪
- 支持将勾爪装备到 Trinkets 装备槽
- 装备槽位：`offhand/hook`
- 默认按 `R` 使用勾爪 / 松开固定中的勾爪
- 默认按 `G` 切换勾爪功能模式
- `R` 与 `G` 均支持在游戏控制设置中自定义
- 支持两种勾爪功能模式：
    - **模式 A：普通拉取**  
      保持原本逻辑。勾爪命中方块后将玩家拉向命中点，随后勾爪释放。
    - **模式 B：固定悬挂**  
      勾爪命中方块后将玩家拉向命中点，勾爪不会立即松开。玩家到达后会被固定在勾爪命中位置附近，需要再次按 `R` 才能松开。
- 切换功能模式时会显示模式提示
- 固定悬挂与松开勾爪时会显示状态提示
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

确保添加 Fabric API 与 Trinkets Updated 依赖：

```gradle
dependencies {
    modImplementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_api_version}"
    implementation "eu.pb4:trinkets:${project.trinkets_version}"
}
```

### `fabric.mod.json`

建议显式声明按键映射、网络与 Trinkets 依赖：

```json
{
  "depends": {
    "fabricloader": ">=0.19.2",
    "minecraft": "~26.1.2",
    "java": ">=25",
    "fabric-api": "*",
    "fabric-key-mapping-api-v1": "*",
    "fabric-networking-api-v1": "*",
    "trinkets_updated": ">=4.0.0-alpha.9"
  }
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

1. 进入游戏后获取或合成 `hook:hook`。
2. 打开 Trinkets 装备界面。
3. 将勾爪放入 `offhand/hook` 槽位。
4. 关闭背包，按默认按键 `R` 发射勾爪。
5. 按默认按键 `G` 在模式 A 与模式 B 之间切换。
6. 在模式 B 中，勾爪命中方块并将玩家拉到目标点后会保持固定；再次按 `R` 可以松开。
7. 可以在 `选项 -> 控制 -> 按键绑定 -> Hook Mod` 中修改 `R` 与 `G` 对应按键。

当前版本中，右键不会再触发勾爪。没有装备勾爪时按下按键不会显示提示信息。

## 勾爪模式

### 模式 A：普通拉取

模式 A 是默认模式，保持早期版本的主要逻辑：

```text
R 发射勾爪
  -> 命中方块
  -> 将玩家拉向命中点
  -> 勾爪实体释放 / 消失
```

适合快速位移、跨越地形与常规移动。

### 模式 B：固定悬挂

模式 B 是新增的固定模式：

```text
G 切换到模式 B
  -> R 发射勾爪
  -> 命中方块
  -> 将玩家拉向命中点
  -> 勾爪保持固定
  -> 玩家被固定在命中点附近
  -> 再按 R 松开
```

适合悬停、停留在高处、挂在墙面附近或后续扩展攀爬玩法。

### 默认按键

| 按键 | 功能                        |
| ---- | --------------------------- |
| `R`  | 使用勾爪 / 松开固定中的勾爪 |
| `G`  | 切换勾爪模式                |

## 合成配方

生存模式下可以通过有序合成获得勾爪：

```text
[空] [铁锭] [铁锭]
[空] [皮革] [铁锭]
[铁粒] [空] [空]
```

数据文件路径：

```text
src/main/resources/data/hook/recipe/hook.json
```

配方内容：

```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": [
    " II",
    " LI",
    "N "
  ],
  "key": {
    "I": "minecraft:iron_ingot",
    "L": "minecraft:leather",
    "N": "minecraft:iron_nugget"
  },
  "result": {
    "id": "hook:hook",
    "count": 1
  }
}
```

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

## 按键绑定与网络流程

当前版本推荐使用统一的网络包处理使用与切换：

```text
HookModClient
  -> ClientPlayNetworking.send(new HookActionPayload(...))
  -> HookNetworking
  -> HookUseHandler.releaseOrUseEquippedHook(player)
       或 HookModeManager.toggleMode(player)
```

按键逻辑建议集中在：

```text
src/client/java/com/lxy/hook/client/HookModClient.java
```

网络逻辑建议集中在：

```text
src/main/java/com/lxy/hook/network/HookActionPayload.java
src/main/java/com/lxy/hook/network/HookNetworking.java
```

如果项目中仍保留旧的 `HookKeyBindings`、`UseHookPayload`、`ModNetwork`，需要避免它们继续注册或发送旧网络包，否则可能造成重复触发或运行时崩溃。

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
src/main/resources/assets/hook/textures/entity/hook.png
src/main/resources/assets/hook/textures/gui/sprites/container/slots/hook.png
src/main/resources/assets/hook/lang/en_us.json
src/main/resources/assets/hook/lang/zh_cn.json
```

语言文件建议包含：

```json
{
  "key.category.hook.controls": "Hook Mod",
  "key.hook.use": "使用 / 松开勾爪",
  "key.hook.toggle_mode": "切换勾爪模式"
}
```

## 主要代码结构

```text
src/main/java/com/lxy/hook/HookMod.java
src/main/java/com/lxy/hook/config/HookConfig.java
src/main/java/com/lxy/hook/item/HookItem.java
src/main/java/com/lxy/hook/item/ModItems.java
src/main/java/com/lxy/hook/entity/HookProjectileEntity.java
src/main/java/com/lxy/hook/entity/ModEntityTypes.java
src/main/java/com/lxy/hook/network/HookActionPayload.java
src/main/java/com/lxy/hook/network/HookNetworking.java
src/main/java/com/lxy/hook/util/HookEquipment.java
src/main/java/com/lxy/hook/util/HookMessage.java
src/main/java/com/lxy/hook/util/HookMode.java
src/main/java/com/lxy/hook/util/HookModeManager.java
src/main/java/com/lxy/hook/util/HookUseHandler.java
src/main/java/com/lxy/hook/util/PlayerPullManager.java
src/client/java/com/lxy/hook/client/HookModClient.java
src/client/java/com/lxy/hook/client/render/HookRopeRenderer.java
```

## 开发说明

### 勾爪使用入口

当前版本不再通过 `HookItem.use()` 或 `HookItem.useOn()` 使用勾爪。右键逻辑应保持 `PASS`，避免和按键触发逻辑冲突。

勾爪真正的使用入口是：

```java
HookUseHandler.releaseOrUseEquippedHook(ServerPlayer player)
```

该方法会：

1. 如果玩家正在被模式 B 固定，则松开勾爪。
2. 如果玩家没有被固定，则检查玩家是否装备了勾爪。
3. 检查勾爪是否处于冷却。
4. 添加冷却。
5. 发射 `HookProjectileEntity`。

### 模式切换入口

功能模式由服务端维护，推荐入口为：

```java
HookModeManager.toggleMode(ServerPlayer player)
```

模式状态建议按玩家 UUID 保存，默认模式为 `HookMode.NORMAL`。

### 装备检查

装备检查由 `HookEquipment` 完成。当前推荐逻辑是只检查 Trinkets 装备槽：

```java
TrinketsApi.getAttachment(player)
        .findFirst(ModItems.HOOK)
        .map(TrinketSlotAccess::get)
        .orElse(ItemStack.EMPTY);
```

### 拉取与固定逻辑

方块拉取推荐使用持续 tick 牵引，避免玩家因为 Minecraft 重力而形成明显抛物线。当前拉取流程建议由 `PlayerPullManager` 维护，每 tick 将玩家速度修正到玩家当前位置指向命中点的方向。

模式 B 中，`PlayerPullManager` 还需要维护固定状态：

```text
pulling -> arrive -> anchored -> release
```

当玩家再次按 `R` 时，应清除固定任务并移除仍留在命中点的勾爪实体。

## 发布建议

如果本次更新只添加功能切换与固定悬挂，推荐版本号为：

```text
v2.1.0
```

如果后续还加入了较大破坏性改动，例如重写存档数据、改动物品 ID、改动槽位 ID，则建议升级到：

```text
v3.0.0
```

## License

本项目当前使用仓库中声明的许可证。