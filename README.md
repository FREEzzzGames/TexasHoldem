# Texas Hold'em Android
- Fullscreen immersive mode hides Android status and navigation bars while the game is active.
- The app checks the GitHub game source on every launch.
- The last valid HTML is cached in private app storage and remains playable offline.
- Build: `gradle assembleDebug` (JDK 17, Android SDK 35).
- APK output: `app/build/outputs/apk/debug/app-debug.apk`.
- Update source: `app/src/main/assets/index.html` on `main`.
