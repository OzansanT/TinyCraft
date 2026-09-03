# TinyCraft

TinyCraft is a small Android-native voxel building game built with **Kotlin + libGDX**. The target loop is mobile-first: explore, mine, collect, place, build, save, and continue.

## Technical direction

- Android-first native application; no WebView and no JavaScript game runtime.
- Kotlin owns game code.
- libGDX provides the game/rendering layer.
- Android-specific integrations stay inside `android/`.
- Game state and gameplay remain inside `core/`.
- World data is chunk-based and base terrain is deterministic/seed-driven.
- Player pose is owned by `PlayerState`; camera state is derived from it.
- Inventory state is owned by `PlayerInventoryState`.
- Rendering reads game state; rendering never owns or mutates authoritative gameplay state.
- UI produces actions; UI does not directly mutate gameplay/world state.
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
│       ├── input/
│       │   ├── InputState.kt                 # Platform-neutral continuous/transient intents
│       │   ├── GameInputController.kt        # Input component composition
│       │   ├── TouchLayout.kt                # Shared touch geometry
│       │   ├── HotbarController.kt           # Slot tap -> selection intent
│       │   ├── PauseButton.kt
│       │   ├── VirtualJoystickController.kt
│       │   ├── TouchLookController.kt
│       │   ├── JumpButton.kt
│       │   ├── MineButton.kt
│       │   └── PlaceButton.kt
│       ├── inventory/
│       │   ├── ItemStack.kt                  # Immutable stack value + stack limit
│       │   └── HotbarSelectionSystem.kt      # Selection intent -> inventory mutation
│       ├── player/
│       │   ├── PlayerState.kt                # Authoritative feet position/velocity/yaw/pitch
│       │   ├── PlayerInventoryState.kt       # Six-slot hotbar inventory
│       │   ├── PlayerSpawnSystem.kt
│       │   ├── PlayerMovementSystem.kt
│       │   ├── PlayerLookSystem.kt
│       │   ├── PlayerCameraController.kt
│       │   ├── VoxelRaycaster.kt
│       │   └── PlayerInteractionSystem.kt    # Inventory-aware mine/place mutations
│       ├── rendering/
│       │   ├── BlockRenderPalette.kt
│       │   ├── ChunkFaceBuilder.kt
│       │   ├── ChunkMeshBuilder.kt
│       │   ├── ChunkRenderCache.kt
│       │   └── WorldRenderer.kt
│       ├── save/
│       │   ├── SaveData.kt                   # Versioned persistence schema
│       │   ├── SaveCodec.kt                  # Deterministic text codec
│       │   ├── SaveRepository.kt             # Storage boundary
│       │   ├── LocalSaveRepository.kt        # libGDX local-file adapter
│       │   └── GameSaveSystem.kt             # Capture/validate/restore
│       ├── session/
│       │   ├── GameSessionState.kt
│       │   └── PauseSystem.kt
│       ├── ui/
│       │   ├── TouchHudRenderer.kt
│       │   └── hotbar/HotbarRenderer.kt
│       ├── screens/                          # System composition only
│       ├── theme/                            # Shared colors/dimensions/theme values
│       └── world/
│           ├── Chunk.kt
│           ├── ChunkPosition.kt
│           ├── WorldModification.kt          # Player-authored generated-world override
│           ├── World.kt
│           └── generation/
│               ├── TerrainNoise.kt
│               └── WorldGenerator.kt
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

Hotbar example:

```text
input/HotbarController.kt
inventory/HotbarSelectionSystem.kt
player/PlayerInventoryState.kt
ui/hotbar/HotbarRenderer.kt
```

A button may have its own component file, but its gameplay logic must not live inside the button.

## 3. UI never directly changes gameplay state

Correct flow:

```text
MineButton
-> GameAction.MINE
-> InputState
-> PlayerInteractionSystem
-> PlayerInventoryState + World
-> chunk revision
-> renderer cache rebuild
```

Incorrect flow:

```text
MineButton -> World.setBlock(...)
HotbarController -> inventory.selectSlot(...)
PauseButton -> write save file
```

UI is responsible for presentation and intent only.

## 4. Central theme roots are mandatory

Shared visual values must come from:

```text
theme/GameColors.kt
theme/GameDimensions.kt
theme/GameTheme.kt
```

Do not scatter arbitrary UI colors, HUD sizes, joystick sizes, hotbar sizes, paddings, or common dimensions across feature files.

A new globally reused color must be added to `GameColors.kt` first.

## 5. Central configuration roots are mandatory

Gameplay constants belong in domain configuration files such as:

```text
config/WorldConfig.kt
config/PlayerConfig.kt
config/RenderingConfig.kt
```

Do not scatter magic values such as chunk size, gravity, movement speed, render distance, terrain amplitude, touch sensitivity, reach distance, or field of view across unrelated classes.

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
Button -> direct World mutation
UI -> SaveRepository file I/O
```

Lower-level world/data code must not know about higher-level UI or Android platform code.

## 7. Android-specific code stays isolated

`android/` is for Android launcher and platform integrations such as:

- Google Play services
- billing
- advertisements
- Android haptics
- Android-only permissions/lifecycle services

Core movement, blocks, chunks, terrain, inventory, save schema, combat, entities, and rendering rules belong in `core/`.

Platform-neutral libGDX local storage may be adapted behind `SaveRepository`; gameplay/UI must never access files directly.

## 8. One owner for each piece of state

Each state value must have one authoritative owner.

Examples:

- Player position/yaw/pitch -> `PlayerState`
- Blocks -> `Chunk` / `World`
- Six hotbar slots + selected slot -> `PlayerInventoryState`
- Pause state -> `GameSessionState`
- Continuous/transient input intent -> `InputState`
- Encoded local save file -> `SaveRepository`

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

A renderer must never reset gameplay data merely to declare itself synchronized.

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

Touch controls, keyboard controls, gamepads, or future platform controls resolve to platform-neutral `GameAction` values and shared `InputState` intent.

Gameplay must not care whether `MINE`, `PAUSE`, or hotbar selection came from touchscreen, mouse, keyboard, or controller.

Input components may claim touches and update intent; they may not implement movement, mining, placement, inventory mutation, save I/O, or world mutation themselves.

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

Every persistent save contains:

```text
SAVE_VERSION
generationVersion
worldSeed
player position/yaw/pitch
selected hotbar slot
non-empty hotbar stacks
player-authored world modifications
```

`GameSaveSystem` must reject incompatible versions rather than partially loading them.

`SaveCodec` owns serialization syntax. `SaveRepository` owns storage. Gameplay/UI must not parse or write save files directly.

Never change serialized save structure without considering migration/backward compatibility and updating `SAVE_VERSION` when appropriate.

## 15. World generation must be deterministic

The same seed and generation version must produce the same base terrain.

Do not use uncontrolled global random calls for persistent terrain generation.

Persistent saves store player-authored block overrides layered over regenerated base terrain; untouched generated chunks are not serialized.

## 16. Performance rules

TinyCraft targets stable mobile performance.

Do not:

- create one permanent render mesh/draw call per block;
- rebuild unchanged chunks every frame;
- render internal block faces;
- allocate avoidable temporary objects every frame;
- bind gameplay simulation speed directly to frame rate;
- use unlimited render distance/entity counts;
- serialize the complete untouched generated world.

Prefer:

- chunk meshes;
- revision-based chunk rebuilding;
- visible-face culling;
- reusable temporary math objects where appropriate;
- bounded frame delta for movement simulation;
- deterministic base terrain + compact modification saves;
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

Do not knowingly leave unresolved imports, duplicate class names, broken package paths, stale renamed APIs, or placeholder calls to nonexistent APIs on merged branches.

If a feature is intentionally incomplete, keep the incomplete portion isolated behind a compilable interface/stub and document the next step.

## 22. Tests accompany foundational logic

Pure data structures, deterministic generation, visibility/culling rules, inventory mutation, persistence codecs, raycasts, input state, and gameplay logic should gain unit tests as they are introduced. Renderer/device behavior can use integration/device tests later.

## 23. Commit/PR scope

Prefer coherent development slices. A PR should implement one architectural layer or feature slice rather than unrelated changes across the project.

## 24. Architecture principle

> **UI displays and produces actions. Systems perform gameplay. Models own state. Renderers render. Persistence captures state. Platform adapters handle external services. One layer must not absorb another layer's responsibilities.**

# Milestones

## 0.1 Foundation — complete

- Android/libGDX project scaffold
- Central theme and configuration roots
- Chunk/world data model
- Block registry
- Platform-neutral input actions
- Minimal game screen and Android launcher

## 0.2 Terrain + chunk rendering — complete

- Deterministic value-noise terrain generation
- Grass/dirt/stone/sand/water layers
- Seeded initial chunk neighborhood
- World-coordinate block access across chunks
- Visible-face extraction across chunk boundaries
- One GPU mesh per chunk
- Revision-based mesh cache invalidation
- Perspective voxel shader
- Unit coverage for terrain determinism and face culling

## 0.3 Player camera + mobile controls — complete

- Player-owned feet position, velocity, yaw and pitch
- Surface-based spawn system
- Movement relative to player yaw
- Gravity and jump action
- First-person player camera controller
- Right-side touch look
- Left virtual movement joystick
- Separate Jump, Mine and Place input components
- Shared touch layout
- Touch HUD + crosshair
- Voxel raycast targeting
- Raycast-driven mining/placement
- Tests for input, look, movement/jump and raycasts

## 0.4 Inventory + versioned save/load — complete

- Six-slot hotbar state
- Tap-to-select hotbar controller
- Immutable block item stacks with a 64-item stack limit
- Mining collects solid blocks into inventory
- Full inventory prevents destructive block loss
- Placement consumes one selected block
- Colored hotbar rendering with item counts
- Pause/resume button and pause session state
- Autosave when pausing, hiding, or disposing the game screen
- Versioned save schema and deterministic text codec
- Local save repository behind a storage interface
- Saved player pose and selected hotbar slot
- Saved hotbar contents
- Saved player-authored block modifications over deterministic terrain
- Compatibility validation using `SAVE_VERSION` + `GENERATION_VERSION`
- Round-trip tests for inventory, saves, world edits and interaction consumption

## Next milestone: 0.5 voxel collision + chunk streaming

1. Replace surface-only horizontal movement checks with player AABB voxel collision.
2. Add axis-separated collision resolution for walls, ceilings, floors, and jumping.
3. Keep step-up behavior explicit and bounded by `MAX_STEP_HEIGHT`.
4. Add player-centered chunk loading/unloading rather than a permanently fixed 3x3 world.
5. Generate newly entered chunks deterministically from the active world seed.
6. Re-apply saved world modifications when streamed chunks load.
7. Dispose GPU meshes when chunks unload.
8. Add collision and chunk-boundary/streaming tests before expanding gameplay further.
