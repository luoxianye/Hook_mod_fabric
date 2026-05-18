# Hook Mod

[![License](https://img.shields.io/badge/license-CC0--1.0-green.svg)](LICENSE)

A Fabric mod that adds a **grappling hook** (勾爪) item to Minecraft. Use it to pull yourself toward blocks or pull entities toward you — a versatile mobility and combat tool.

---

## Features

- **Grapple to blocks** — Right-click a block to pull yourself toward it
- **Shoot projectile** — Right-click into the air to fire a hook projectile; it travels until it hits a block or entity
- **Entity interaction** — Pull mobs, animals, and other entities toward you
- **Rope rendering** — A visible rope connects the flying hook projectile to the player
- **Configurable** — Nearly all parameters are adjustable via `config/hook.json`
- **Block tag system** — Whitelist / blacklist which blocks can be grappled
- **Sound & particle effects** — Audio and visual feedback for success, failure, and hits
- **Multi-language** — English (`en_us`) and Chinese (`zh_cn`) translations included
- **Durability & cooldown** — Balanced gameplay with item durability (384) and usage cooldowns

---

## Supported Versions

| Dependency      | Version            |
| --------------- | ------------------ |
| Minecraft       | 26.1.2             |
| Fabric Loader   | 0.19.2+            |
| Fabric API      | 0.149.0+26.1.2     |
| Java            | 25                 |

---

## How to Use

### Controls

| Action                  | Input                               |
| ----------------------- | ----------------------------------- |
| Grapple to block        | Right-click a block with the Hook   |
| Shoot hook projectile   | Right-click into the air            |

- **Block grapple (right-click a block)**: You are pulled toward the clicked block's surface.
- **Air grapple (right-click into the air)**: The hook flies forward as a projectile. If it hits a block, you are pulled to that point. If it hits an entity, the entity is pulled toward you.

### Crafting Recipe

```
  I I
  L I
N
```

| Slot | Ingredient         |
| ---- | ------------------ |
| I    | Iron Ingot         |
| L    | Leather            |
| N    | Iron Nugget        |

---

## Configuration

All settings are stored in `config/hook.json` (auto-generated with defaults on first run).

| Setting                 | Default | Description                                      |
| ----------------------- | ------- | ------------------------------------------------ |
| `maxDistance`           | 32.0    | Maximum grapple distance (blocks)                |
| `minDistance`           | 2.0     | Minimum effective distance                       |
| `blockPullStrength`     | 3.6     | Pull strength toward blocks                      |
| `entityPullStrength`    | 2.4     | Pull strength on entities                        |
| `blockVerticalBoost`    | 1.35    | Extra vertical boost when grappling blocks       |
| `entityVerticalBoost`   | 0.75    | Extra vertical boost when grappling entities     |
| `maxPullVelocity`       | 7.5     | Maximum total pull velocity                      |
| `maxHorizontalVelocity` | 6.6     | Maximum horizontal pull velocity                 |
| `maxVerticalVelocity`   | 3.6     | Maximum vertical pull velocity                   |
| `projectileSpeed`       | 3.8     | Hook projectile speed                            |
| `useCooldownTicks`      | 20      | Cooldown after any use (1s = 20 ticks)           |
| `blockCooldownTicks`    | 20      | Cooldown after block grapple                     |
| `entityCooldownTicks`   | 25      | Cooldown after entity grapple                    |
| `failCooldownTicks`     | 5       | Cooldown after a failed attempt                  |
| `durabilityCost`        | 1       | Durability consumed per use                      |
| `allowPullPlayers`      | false   | Allow pulling other players                      |
| `allowPullBosses`       | false   | Allow pulling boss entities                      |
| `reduceFallDamage`      | true    | Reset fall distance after grappling              |

---

## Block Tags

Control which blocks can be grappled via data pack tags (located at `data/hook/tags/block/`):

- **`#hook:hook_grabbable`** — Whitelist: blocks that can be grappled (default: all `#minecraft:mineable/pickaxe`, `#minecraft:mineable/axe`, `#minecraft:mineable/shovel`)
- **`#hook:hook_blacklist`** — Blacklist: blocks that can never be grappled (default: fire, soul fire, water, lava, grass, air)

---

## Project Structure

```
src/
├── main/java/com/lxy/hook/
│   ├── HookMod.java                  # Mod initializer
│   ├── config/
│   │   └── HookConfig.java           # JSON-based configuration system
│   ├── entity/
│   │   ├── ModEntityTypes.java       # Entity type registration
│   │   └── HookProjectileEntity.java # Hook projectile: flight, block/entity hit, pull logic
│   ├── item/
│   │   ├── ModItems.java             # Item registration + creative tab
│   │   └── HookItem.java             # Hook item: right-click block, right-click air, tooltips
│   ├── tag/
│   │   └── ModBlockTags.java         # Block tag definitions (hook_grabbable, hook_blacklist)
│   └── util/
│       ├── HookMath.java             # Velocity calculation, distance factor, clamping
│       ├── HookRaycast.java          # Raycast utility (block + entity)
│       └── PlayerPullManager.java    # Per-tick player pull system with arrival detection
│
├── client/java/com/lxy/hook/client/
│   ├── HookModClient.java            # Client entry point: renderer registration
│   ├── HookModDataGenerator.java     # Data generation entry point
│   ├── mixin/
│   │   └── ExampleClientMixin.java   # Client mixin template
│   └── render/
│       └── HookRopeRenderer.java     # Rope rendering between hook projectile & player
│
└── resources/
    ├── fabric.mod.json               # Mod metadata (id, version, entrypoints, dependencies)
    ├── hook.mixins.json              # Mixin configuration
    ├── assets/hook/
    │   ├── lang/en_us.json           # English translations
    │   ├── lang/zh_cn.json           # Chinese translations
    │   ├── models/item/hook.json     # Item model
    │   ├── items/hook.json           # Item definition
    │   └── textures/
    │       ├── item/hook.png         # Item texture
    │       └── entity/rope.png       # Rope texture
    └── data/hook/
        ├── recipe/hook.json          # Crafting recipe
        └── tags/block/
            ├── hook_grabbable.json   # Grabbable block whitelist
            └── hook_blacklist.json   # Grabbable block blacklist
```

---

## Build

```bash
# Build the mod (output in build/libs/)
./gradlew build

# Run the client for testing
./gradlew runClient
```

---

## License

[CC0-1.0](LICENSE)