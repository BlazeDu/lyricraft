# 🎵 Lyricraft

[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.20-7F52FF.svg?logo=kotlin)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-2025.02.00-4285F4.svg?logo=android)](https://developer.android.com/jetpack/compose)
[![ExoPlayer](https://img.shields.io/badge/ExoPlayer-2.19.1-34A853.svg?logo=android)](https://exoplayer.dev/)
[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg?logo=android)](https://developer.android.com/studio/releases/platforms)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

---

**English** | [**中文**](README_CN.md)

---

## Overview

**Lyricraft** is a lightweight, pure-native Android lyrics creation tool built with **Jetpack Compose** and **ExoPlayer**. It allows users to import plain text lyrics, load local audio files, manually synchronize lyrics with timestamps via tapping, and export standard **`.lrc`** lyric files.

This is a **fully offline, local-first** application — no internet connection required, no user registration, and no privacy concerns. All data stays on your device.

## Key Features

| Feature | Description |
|---------|-------------|
| 📝 Lyrics Import | Paste multi-line plain text, automatically split by newlines |
| 🎵 Audio Loading | Load local audio files with ExoPlayer — play, pause, seek |
| ⏱️ Time Tagging | One-tap timestamp recording while playing, auto-advance to next line |
| ✏️ Real-time Editing | Modify or delete individual lyric lines |
| 💾 LRC Export | Custom save path and filename, generate standard `.lrc` files |
| 📂 File Browser | Built-in folder picker with directory navigation |
| 🔐 Permission Handling | Full support for Android 11+ scoped storage |
| 🌓 Dark Mode | Auto-switch between Material 3 light/dark themes |
| 📱 Bottom Navigation | Three tabs: Input Text / Record Time / Settings |

## Technology Stack

| Technology | Description |
|------------|-------------|
| **Kotlin** | 100% Kotlin |
| **Jetpack Compose** | Declarative UI with Material 3 |
| **ExoPlayer** | Official Google audio playback engine |
| **Activity Result API** | Modern file picker callbacks |
| **Kotlin Coroutines** | IO operations and UI updates |

## Architecture

- **Single Activity + Pure Compose**: No Fragments, no XML layouts
- **State-driven UI**: All states managed by `remember`, automatic UI updates on data changes
- **Lightweight**: No MVVM overhead — simple, clean, and maintainable

## Project Structure

```
app/src/main/java/com/storm/lyricraft/
├── MainActivity.kt                 # Entry point & global state
├── data/
│   └── LyricLine.kt                # Lyric data model
├── ui/
│   ├── theme/
│   │   ├── Color.kt               # Color definitions
│   │   ├── Type.kt                # Typography
│   │   └── Theme.kt               # Material 3 theme
│   ├── navigation/
│   │   └── TabPage.kt             # Bottom nav enumeration
│   ├── components/
│   │   ├── LyricItem.kt           # Single lyric row component
│   │   └── Dialogs.kt             # Export / Folder / Permission dialogs
│   └── screens/
│       ├── InputTextScreen.kt     # Text import page
│       ├── RecordTimeScreen.kt    # Time tagging page (core)
│       └── SettingScreen.kt       # Settings page
└── utils/
    └── TimeUtils.kt               # LRC time formatting
```

## Quick Start

### Requirements

| Tool | Version |
|------|---------|
| Android Studio | Ladybug \| 2024.2.1+ |
| Gradle | 8.2+ |
| Min SDK | API 21 (Android 5.0) |
| Target SDK | API 34 (Android 14) |

### Build & Run

```bash
# Clone the repository
git clone https://github.com/BlazeDu/Lyricraft.git

# Open in Android Studio and sync Gradle

# Run the app
./gradlew installDebug
```

## How to Use

1. **Import Lyrics** — Switch to the **"Text"** tab, paste your plain-text lyrics (one line per sentence), tap **"Import & Go to Tagging"**.
2. **Load Audio** — Go to the **"Time"** tab, tap **"Select Audio"** and choose a local audio file.
3. **Tag Timestamps** — Tap **"Play"** to start playback. At each lyric line, tap **"Record Current Time"** — the timestamp is saved and the next line is auto-selected.
4. **Export LRC** — Tap **"Export LRC"**, choose a save folder and filename, then confirm to generate the `.lrc` file.

## Time Format

Timestamps follow the standard LRC format: `[MM:SS.cc]`, for example:

```
[00:12.34]First lyric line
[00:25.67]Second lyric line
```

## Permission Handling

- **Android 10 and below**: Traditional file read/write permissions
- **Android 11+ (API 30+)**: Uses `MANAGE_EXTERNAL_STORAGE` with a one-click jump to system settings
- The app guides users through the permission grant process

## Resource Management

- `DisposableEffect` automatically releases the ExoPlayer instance when the screen is destroyed
- Coroutines are automatically cancelled — no memory leaks

## Future Plans

- [ ] Import and edit existing `.lrc` files
- [ ] Batch time adjustment / global offset
- [ ] Custom theme and font size settings
- [ ] Lyrics preview with real-time highlight sync
- [ ] Undo / Redo support
- [ ] Batch delete lyrics

## Contributing

Issues and Pull Requests are welcome!

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

Distributed under the MIT License. See [LICENSE](LICENSE) for more information.

## Acknowledgements

- [Jetpack Compose](https://developer.android.com/jetpack/compose) — Modern declarative UI toolkit
- [ExoPlayer](https://exoplayer.dev/) — Powerful Android media playback engine
- [Material 3](https://m3.material.io/) — Google's latest design system

---

**Made with ❤️ by BlazeDu**