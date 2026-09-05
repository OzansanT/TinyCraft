# Design Parity Map

| Web reference | Native Android implementation |
|---|---|
| `body background #101418` | Activity + ScrollView background |
| `.app max 1100px` | full available Android content width with matching outer padding |
| `.badge` | native `TextView` + `GradientDrawable` |
| `button` | native `Button` + exact panel/border colors |
| `.block-choice.active` | 2dp `#72C94A` native border |
| `.game-frame` | native `FrameLayout` border/radius |
| `<canvas>` | `GLSurfaceView` / OpenGL ES |
| HUD overlay | native `LinearLayout` overlay |
| message overlay | native `TextView` bottom-center |
| Three.js perspective camera | Android `Matrix.perspectiveM(62°)` |
| Three.js fog | native OpenGL fragment-shader linear fog 14–34 |
| Lambert blocks | native normal-based directional + ambient lighting |
| JS voxel data | Kotlin `VoxelWorld` |
| JS raycaster | native CPU ray march from inverse view-projection |

No web-runtime layer exists in the APK.
