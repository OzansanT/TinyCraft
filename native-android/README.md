# TinyCraft — Native Android Exact-Design Build

This is the Android-only rewrite of the MiniCraft/TinyCraft web prototype.

## Runtime stack

- Kotlin
- Android SDK Views
- `GLSurfaceView`
- OpenGL ES 2.0

## Explicitly not used

- No WebView
- No HTML
- No CSS
- No JavaScript
- No Three.js
- No libGDX
- No shared desktop/core module

## Design parity

The native interface carries across the original values and structure:

- background `#101418`
- panel `#171D22`
- secondary panel `#202830`
- border `#39444D`
- text `#EEF3F6`
- muted text `#9AA8B2`
- selected accent `#72C94A`
- sky/fog `#8FC7E8`
- same toolbar labels, block picker, HUD, message position, help copy, terrain colors and yellow player
- same 62° perspective camera, orbit pitch/yaw and distance limits
- same generated terrain formula and tree rules

## Native controls

- Tap block: mine
- Long press block: place selected block
- One-finger drag: rotate camera
- Pinch: zoom
- Two-finger drag: move without adding visible controls
- Hardware WASD / arrows: move
- Hardware 1 / 2 / 3: choose Grass / Dirt / Stone

## Build

```bash
gradle :app:assembleDebug
```

APK output:

`app/build/outputs/apk/debug/app-debug.apk`
