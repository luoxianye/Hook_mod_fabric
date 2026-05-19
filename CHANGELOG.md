# Changelog

所有值得记录的项目变更都会写在这个文件中。

版本号建议遵循语义化版本思路：

- `MAJOR`：破坏性改动，例如物品 ID、槽位 ID、存档数据结构不兼容。
- `MINOR`：新增功能，但保持旧功能基本兼容。
- `PATCH`：错误修复、平衡性微调、文档或资源修正。

## v2.1.0 - 2026-05-19

### Added

- 添加勾爪功能模式系统。
- 添加 **模式 A：普通拉取**。
    - 保持原有勾爪逻辑。
    - 勾爪命中方块后将玩家拉向命中点，随后勾爪释放。
- 添加 **模式 B：固定悬挂**。
    - 勾爪命中方块后将玩家拉向命中点。
    - 玩家到达目标点附近后，勾爪不会立即松开。
    - 玩家会被固定在勾爪命中位置附近。
    - 玩家需要再次按 `R` 才能松开勾爪。
- 添加默认 `G` 键切换勾爪功能模式。
- 添加模式切换提示。
- 添加固定成功提示。
- 添加松开勾爪提示。
- 添加统一网络包 `HookActionPayload`，用于处理勾爪使用、松开与模式切换。
- 添加 `HookMode` 与 `HookModeManager`，用于维护玩家当前勾爪模式。
- 添加 `HookMessage`，用于统一发送 actionbar 或聊天提示。

### Changed

- `R` 键现在同时承担“使用勾爪”和“松开固定中勾爪”的功能。
- 勾爪使用入口推荐改为 `HookUseHandler.releaseOrUseEquippedHook(ServerPlayer player)`。
- 客户端按键注册推荐集中到 `HookModClient`。
- 网络注册推荐集中到 `HookNetworking`。
- 方块命中逻辑现在会根据玩家当前模式决定是否立即释放勾爪。
- `PlayerPullManager` 支持普通拉取任务与固定悬挂任务。
- 客户端按键 API 使用 `fabric-key-mapping-api-v1`。

### Fixed

- 修复在 Minecraft 26.1.2 / 当前映射环境下 `ServerPlayer#displayClientMessage(...)` 不可用导致的编译问题。
- 修复旧版 keybinding API 包名不匹配导致的客户端编译问题。
- 降低新旧按键网络包并存时导致重复触发或运行时崩溃的风险。

### Migration Notes

- 推荐移除或停用旧的 `HookKeyBindings`、`UseHookPayload`、`ModNetwork` 注册路径。
- 推荐统一使用：

```text
HookModClient
  -> HookActionPayload
  -> HookNetworking
  -> HookUseHandler / HookModeManager
```

- `fabric.mod.json` 建议显式添加：

```json
"fabric-key-mapping-api-v1": "*",
"fabric-networking-api-v1": "*"
```

- 如果本次功能尚未正式发布，可以先把标题改成：

```text
## v2.1.0 - Unreleased
```

## v2.0.0

### Added

- 添加 Trinkets Updated 支持。
- 添加 `offhand/hook` 勾爪装备槽。
- 添加默认 `R` 键使用勾爪。
- 添加可在控制设置中自定义的按键绑定。
- 添加客户端到服务端的网络包触发逻辑。
- 添加生存模式勾爪合成配方。

### Changed

- 勾爪不再通过右键触发。
- 玩家需要先装备勾爪，然后按勾爪按键使用。
- 方块拉取逻辑调整为更接近直线牵引。
- 勾爪投射速度提高。

### Removed

- 移除右键空气发射勾爪。
- 移除右键方块直接触发勾爪。

### Dependencies

- 新增依赖：Trinkets Updated `4.0.0-alpha.9+26.1`。

## v1.0.0

### Added

- 添加基础勾爪物品。
- 支持右键发射勾爪。
- 支持勾爪与方块、实体交互。
- 添加基础冷却、耐久与拉取配置。