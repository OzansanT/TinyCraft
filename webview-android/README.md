# TinyCraft Android WebView

This project packages the existing TinyCraft / MiniCraft web game as a native Android APK shell without redesigning the game UI.

## Architecture

- Kotlin Activity
- Android WebView
- WebViewAssetLoader for app-local HTTPS assets
- Existing HTML/CSS/game JavaScript preserved in presentation
- Three.js r170 is copied into assets/www/vendor/three.module.js before APK compilation
- No runtime INTERNET permission

## Controls

Desktop/keyboard controls remain unchanged: WASD/arrows move, drag rotates, wheel zooms, click mines, Shift+click places, 1/2/3 select blocks.

On Android touchscreens, a long press performs the same place-block action as Shift+click. This adds no visible UI and does not change the design.

The GitHub Actions build produces app/build/outputs/apk/debug/app-debug.apk.
