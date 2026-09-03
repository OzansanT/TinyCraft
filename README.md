# TinyCraft

TinyCraft is a small Android-native voxel building game built with **Kotlin + libGDX**. The target loop is simple and mobile-first: explore, mine, collect, place, build, save, and continue.

## Technical direction

- Android-first native application; no WebView and no JavaScript game runtime.
- Kotlin owns game code.
- libGDX provides the game/rendering layer.
- Android-specific integrations stay inside `android/`.
- Game state and gameplay remain inside `core/`.
- World data is chunk-based.
- Base terrain is deterministic and seed-driven.
- Rendering reads game state; rendering never owns or mutates authoritative gameplay state.
- UI produces actions; UI does not directly mutate the world.
- Shared visual values live in root theme files instead of being duplicated.

## Current project map

```text
TinyCraft/
├── android/                                  # Android launcher/platform integration only
├── assets/                                   # Textures, audio, shaders, fonts and data
├── core/
│   └── src/main/kotlin/com/tinycraft/
│       ├── TinyCraftGame.kt                  # Core application entry
│       ├── blocks/                           # Block definitions and registry
│       ├── config/                           # Gameplay/world/render configuration roots
│       ├── input/                            # Platform-neutral game actions/input state
│       ├── player/                           # Player-owned state and systems
│       ├── rendering/
│       │   ├── BlockRenderPalette.kt         # Block ID -> centralized theme color
│       │   ├── ChunkFaceBuilder.kt           # Visible-face extraction
│       │   ├── ChunkMeshBuilder.kt           # Visible faces -> GPU mesh
│       │   ├── ChunkRenderCache.kt           # Revision-based GPU mesh cache
│       │   ├── FaceDirection.kt
│       │   ├── VisibleFace.kt
│       │   └── WorldRenderer.kt              # Camera/shader/world presentation
│       ├── screens/                          # Game/menu screen composition
│       ├── theme/                            # Shared colors/dimensions/theme values
│       └── world/
│           ├── Chunk.kt                      # Compact block storage + mesh revision
│           ├── ChunkPosition.kt
│           ├── World.kt                      # World/chunk coordinate access
│           └── generation/
│               ├── TerrainNoise.kt           # Deterministic value noise
│               └── WorldGenerator.kt         # Seeded terrain generation
├── build.gradle
├── gradle.properties
└── settings.gradle
```

# Update Rules

These rules are mandatory for future TinyCraft development, including AI-assisted updates.

## 1. One responsibility per file

Every meaningful component, system, model, controller, renderer, configuration object, or reusable UI element belongs in its own file.

Do not grow a single `GameScreen.kt`, `GameManager.kt`, `Utils.kt`, or similar file into a dumping ground.

Preferred naming includes:

- `SomethingScreen`
- `SomethingRenderer`
- `SomethingSystem`
- `SomethingController`
- `SomethingState`
- `SomethingConfig`
- `SomethingRegistry`
- `SomethingRepository`
- `SomethingFactory`

Avoid vague names such as `Stuff`, `General`, `Misc`, `Utils2`, `NewManager`, or `FinalManager`.

## 2. Feature files must be distributed by capability

The web-development idea of splitting HTML/CSS/JavaScript is adapted to native game development as follows:

```text
Visual component  -> its own Kotlin component file
Input behavior    -> controller/action file
Gameplay behavior -> system file
Persistent data   -> model/state file
Rendering         -> renderer/mesh file
Constants         -> config/theme file
```

Example for a future hotbar:

```text
ui/hotbar/Hotbar.kt
ui/hotbar/HotbarSlot.kt
ui/hotbar/HotbarRenderer.kt
ui/hotbar/HotbarController.kt
```

A button may have its own component file, but its gameplay logic must not live inside the button.

## 3. UI never directly changes gameplay state

Correct flow:

```text
MineButton -> GameAction.MINE -> PlayerMiningSystem -> World -> chunk revision -> renderer cache rebuild
```

Incorrect flow:

```text
MineButton -> World.removeBlock()
```

UI is responsible for presentation and intent only.

## 4. Central theme roots are mandatory

Shared visual values must come from:

```text
theme/GameColors.kt
theme/GameDimensions.kt
theme/GameTheme.kt
```

Do not scatter arbitrary UI colors, HUD sizes, joystick sizes, paddings, or common dimensions across feature files.

A new globally reused color must be added to `GameColors.kt` first.

## 5. Central configuration roots are mandatory

Gameplay constants belong in domain configuration files such as:

```text
config/WorldConfig.kt
config/PlayerConfig.kt
config/RenderingConfig.kt
```

Do not scatter magic values such as chunk size, gravity, movement speed, render distance, terrain amplitude, reach distance, or field of view across unrelated classes.

## 6. Dependency direction

Preferred dependency direction:

```text
UI -> Input -> Gameplay Systems -> World/Data
                         |
                         +-> Rendering reads resulting state
```

Forbidden dependencies include:

```text
World -> UI
Chunk -> Joystick
Block -> Android Activity
World -> Android platform code
Renderer -> authoritative World mutation
```

Lower-level world/data code must not know about higher-level UI or Android platform code.

## 7. Android-specific code stays isolated

`android/` is for Android launcher and platform integrations such as:

- Google Play services
- billing
- advertisements
- Android haptics
- platform storage adapters
- permissions
- Android lifecycle integration

Core movement, blocks, chunks, terrain, inventory, combat, entities, and rendering rules belong in `core/`.

## 8. One owner for each piece of state

Each state value must have one authoritative owner.

Examples:

- Player position -> `PlayerState`
- Blocks -> `Chunk` / `World`
- Inventory contents -> future `InventoryState`
- Selected hotbar slot -> future hotbar/inventory state

Do not duplicate the same mutable state in several screens/controllers and attempt to synchronize it manually.

## 9. World data is chunk-based

Do not represent the voxel world as thousands of permanent scene objects.

World storage is chunk-based. Chunk dimensions and generation constants are centralized in `WorldConfig`.

Rendering builds one GPU mesh per loaded chunk for the current prototype and omits hidden block faces.

## 10. Renderer is read-only toward gameplay

Renderers may read state and maintain GPU/render caches. They may not decide gameplay outcomes or reset/mutate authoritative world/player state.

Chunk mesh freshness uses a monotonic `meshRevision` owned by world/chunk code:

```text
Gameplay/world mutation
-> chunk meshRevision advances
-> renderer notices revision mismatch
-> visible faces rebuilt
-> chunk GPU mesh replaced
```

A renderer must never reset a dirty flag on gameplay data to declare itself synchronized.

## 11. Blocks use a registry

Block properties must be defined centrally through `BlockDefinition` and `BlockRegistry`.

Adding a block should not require conditionals across unrelated systems.

Future block metadata can include:

- texture/atlas region
- hardness
- solid/collision
- transparency
- mining sound
- placement sound
- drop
- light properties

## 12. Input is action-based

Touch controls, keyboard controls, gamepads, or future platform controls should resolve to platform-neutral `GameAction` values.

Gameplay should not care whether `MINE` came from a touchscreen button, mouse, keyboard, or controller.

## 13. Asset organization

Assets are grouped by capability:

```text
assets/
├── textures/
│   ├── blocks/
│   ├── entities/
│   ├── environment/
│   └── ui/
├── audio/
│   ├── blocks/
│   ├── entities/
│   ├── player/
│   └── ui/
├── fonts/
├── shaders/
└── data/
```

As asset count grows, introduce an `AssetRegistry` and stop referencing repeated literal asset paths throughout gameplay code.

## 14. Save files are versioned

Every persistent world/save format must include a save version from its first implementation.

Future structure should include at minimum:

```text
version
worldSeed
generationVersion
player
chunks/world modifications
inventory
```

Never change serialized save structure without considering migration/backward compatibility.

## 15. World generation must be deterministic

World generation must be seed-driven. The same seed and generation version must produce the same base terrain.

Do not use uncontrolled global random calls for persistent terrain generation.

Persistent player edits should eventually be stored as modifications layered over generated base terrain rather than requiring the full untouched world to be serialized.

## 16. Performance rules

TinyCraft targets stable mobile performance.

Do not:

- create one permanent render mesh/draw call per block;
- rebuild unchanged chunks every frame;
- render internal block faces;
- allocate avoidable temporary objects every frame;
- bind gameplay simulation speed directly to frame rate;
- use unlimited render distance/entity counts.

Prefer:

- chunk meshes;
- revision-based chunk rebuilding;
- visible-face culling;
- reusable temporary math objects where appropriate;
- deterministic fixed/controlled simulation timing;
- explicit mobile budgets.

## 17. File growth rule

Guideline:

- Under 300 lines: normal.
- 300-500 lines: review responsibility boundaries.
- Over 500 lines: strongly consider splitting before adding more behavior.

Line count is not the only criterion. If a file performs two unrelated jobs, split it regardless of size.

## 18. No shortcut placement

Never place a new feature in an unrelated existing file merely because that file is convenient.

Before adding a feature, identify its correct domain/module. Create the necessary file/folder when the responsibility is new.

## 19. Feature implementation sequence

For a new feature, prefer this order:

```text
1. Define state/data ownership
2. Define gameplay/system logic
3. Define rendering if needed
4. Define input/UI if needed
5. Compose the feature into the screen/game
6. Add or update tests
7. Update this architecture map when structure changes
```

## 20. Keep configuration and dependency versions centralized

Project/library versions belong in `gradle.properties` or a future version catalog, not repeated across module files.

## 21. Changes must remain build-oriented

Do not knowingly leave unresolved imports, duplicate class names, broken package paths, or placeholder calls to nonexistent APIs on merged branches.

If a feature is intentionally incomplete, keep the incomplete portion isolated behind a compilable interface/stub and document the next step.

## 22. Tests accompany foundational logic

Pure data structures, deterministic generation, visibility/culling rules, and gameplay logic should gain unit tests as they are introduced. Renderer/device behavior can use later integration/device tests.

## 23. Commit/PR scope

Prefer coherent development slices. A PR should implement one architectural layer or feature slice rather than unrelated changes across the project.

## 24. Architecture principle

> **UI displays and produces actions. Systems perform gameplay. Models own state. Renderers render. Platform code handles Android. One layer must not absorb another layer's responsibilities.**

# Milestones

## 0.1 Foundation — complete

- Android/libGDX project scaffold
- Central theme and configuration roots
- Chunk/world data model
- Block registry
- Platform-neutral input actions
- Minimal game screen and Android launcher

## 0.2 Terrain + chunk rendering — current

- Deterministic value-noise terrain generation
- Grass/dirt/stone/sand/water layers
- Seeded initial chunk neighborhood
- World-coordinate block access across chunks
- Visible-face extraction across chunk boundaries
- One GPU mesh per chunk
- Revision-based mesh cache invalidation
- Perspective camera + simple voxel shader
- Unit coverage for terrain determinism and face culling

## Next milestone: 0.3 player camera + mobile controls

1. Introduce player-owned position/yaw/pitch movement state.
2. Add first-person/third-person camera controller boundary.
3. Add touch look controller.
4. Add virtual movement joystick.
5. Add jump, mine, and place action buttons as separate UI components.
6. Route every control through `GameAction` instead of directly mutating the world.
7. Add voxel raycast targeting for mining/placing.
