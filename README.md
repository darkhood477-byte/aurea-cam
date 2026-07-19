[README.md](https://github.com/user-attachments/files/30164259/README.md)[Uploading<div align="center">

<img src="app/src/main/res/drawable/pro_camera_apk_logo_dark.png" alt="AureaCam Logo" width="180">

# AureaCam

### *Aesthetic Empowerment for Mobile Cinematographers*

[![Android](https://img.shields.io/badge/Android-API%2024%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com/studio/releases/platforms)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9%2B-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![CameraX](https://img.shields.io/badge/CameraX-1.5.0-FF9500?logo=camera&logoColor=white)](https://developer.android.com/training/camerax)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-M3-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-1C1C1E)](LICENSE)

</div>

---

> **AureaCam** is a professional-grade, cinematic camera application built for vloggers, content creators, and mobile cinematographers. Designed on **Jetpack Compose** and **CameraX 1.5.0** with a modern, high-contrast Material Design 3 aesthetic, AureaCam bridges the gap between smartphone cameras and standalone professional cinema rigs.

---

## Table of Contents

- [What Makes AureaCam Different](#what-makes-aureacam-different)
- [Features](#features)
- [Quick Start](#quick-start)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Building from Source](#building-from-source)
- [Contributing](#contributing)
- [License](#license)

---

## What Makes AureaCam Different

Smartphone sensors are incredibly powerful — but stock camera interfaces oversimplify controls and hide the artistic alignment tools professionals need. AureaCam's philosophy is **Aesthetic Empowerment**:

- **Geometry & Composition** — Ten advanced mathematical grids overlaid in real-time
- **Orientation & Levelling** — Dual-mode electronic virtual horizon with haptic feedback
- **Temporal Alignment** — Hardware-accurate scene slate labelling and SMPTE timecode
- **Framing Continuity** — Ghost overlay mode for seamless multi-take sequences
- **Physical Tactile Control** — Full volume-button integration for hands-free shooting

---

## Features

### Viewfinder & Capture
| Feature | Description |
|---------|-------------|
| **Dynamic Aspect Ratios** | 9:16, 3:4, 1:1, 4:3, 16:9, and FULL — animated with 350ms `FastOutSlowInEasing` transitions |
| **Tap-to-Focus** | Touch any point on the viewfinder to set focus with a target reticle |
| **Exposure Lock** | Long-press to lock exposure; indicators turn golden orange |
| **Exposure Compensation** | Horizontal drag slider, hardware-limited to your device's actual ±EV range |
| **Manual Focus** | Slider from infinity to device-specific macro distance |
| **Physical Shutter** | Volume Up/Down buttons trigger capture; long-press cycles through Zoom / Focus / Shutter modes |
| **Countdown Timer** | 3s or 10s delay with coroutine-driven countdown |

### Composition Overlays *(10 Mathematical Grids)*
1. **Rule of Thirds** — Classic dual-axis division
2. **Golden Ratio (Phi Grid)** — Divisions at 1 : 1.618
3. **Golden Spiral (Fibonacci)** — Logarithmic arcs across reciprocal grids
4. **Golden Triangle** — Diagonal splits with perpendicular vectors
5. **Dynamic Symmetry** — Reciprocal diagonal armatures
6. **Leading Lines** — Perspective convergence from bottom corners
7. **Framing** — 10% safety margin boundaries
8. **Symmetry Centering** — Dynamic technical crosshair
9. **Radial Symmetry** — 8-sector geometric division
10. **Vanishing Point** — Multi-vector convergence for linear backgrounds

### Virtual Horizon *(Dual-Mode Electronic Level)*
- **Upright Mode** — Tick marks, rotating horizon line, and spirit bubble vial
- **Flat-Lay Mode** — Auto-switches to circular bullseye level when pitch exceeds 75°
- **Haptic Lock** — Vibrant golden orange + tactile click when aligned within ±1.0°
- **Sensor Fusion** — `TYPE_ROTATION_VECTOR` with exponential smoothing (α = 0.12)

### Ghost Overlay Mode
- **Previous Take** — Auto-loads your last capture as a translucent reference
- **Custom Image** — Native Android file picker (`GetContent`) for any historical asset
- **Opacity Slider** — Smooth 0.0 → 1.0 adjustment
- **Perfect for** — Stop-motion, transition matches, and sequence continuity

### Cinematic Slate & Timecode
- **Scene / Take Metadata** — Structured filenames: `Scene_01_Take_3_20260719_143052.jpg`
- **SMPTE Display Timecode** — HH:MM:SS:FF calibrated to your selected recording frame rate
- **Persistent Slate Editor** — Tap to edit scene names; reset take index when needed

### Built-in Gallery
- **Chronological Grid** — Asynchronous MediaStore query, newest first
- **Fluid Pager** — Full-screen horizontal swipe viewer
- **Video Playback** — Launch into native device player
- **Smart Thumbnails** — Video duration overlays with play indicators
- **Safe Delete** — Undo snackbar + trash support on Android 11+

---

## Quick Start

### For Users

1. **Download** the latest APK from [Releases](https://github.com/darkhood477-byte/aurea-cam/releases)
2. **Grant permissions** — Camera (required), Audio (when first recording video)
3. **Tap the shutter** or press **Volume Up/Down** to capture
4. **Swipe up** on the gallery thumbnail to browse your captures

### For Developers

```bash
# Clone the repository
git clone https://github.com/darkhood477-byte/aurea-cam.git
cd aurea-cam

# Open in Android Studio (latest stable)
# Sync Gradle, build, and deploy to device
```

**Minimum Requirements:**
- Android API 24+ (Android 7.0 Nougat)
- Camera2-capable device
- Jetpack Compose runtime

---

## Architecture

AureaCam follows a **decoupled, reactive model-view architecture** built entirely within Jetpack Compose and CameraX:

```
┌─────────────────────────────────────────┐
│           Android OS Kernel             │
└─────────────────────────────────────────┘
     /           |            Camera Sensors  Storage    Inertial Sensors
    |             |               |
    v             v               v
[CameraX API] [MediaStore]  [SensorManager]
    |             |               |
    v             v               v
┌─────────┐  ┌──────────┐  ┌─────────────┐
│ Camera  │  │  Media   │  │   Virtual   │
│Preview  │  │ Gallery  │  │   Horizon   │
│(Viewf.) │  │(Assets)  │  │ (Telemetry) │
└────┬────┘  └────┬─────┘  └──────┬──────┘
     \            |               /
      v           v              v
   ┌─────────────────────────────────┐
   │         CameraScreen            │
   │   (Central State Engine & UI)   │
   └─────────────────────────────────┘
          /      |                v       v        v
  [Settings] [Composition] [ZebraStripes]
```

### Key Modules

| Module | Responsibility |
|--------|----------------|
| `MainActivity.kt` | Entry point, edge-to-edge window, permission gating via Accompanist |
| `CameraPreview.kt` | CameraX `PreviewView` wrapper, `ImageCapture` + `VideoCapture<Recorder>` binding, zoom/focus/torch controls |
| `CameraScreen.kt` | Central state coordination: ghost overlay, composition grid, scene/take counters, latest media URI |
| `CompositionOverlayCanvas.kt` | Custom `Canvas` rendering for all 10 mathematical grids |
| `VirtualHorizon.kt` | `SensorManager` integration, Euler angle extraction, low-pass filtering, bullseye mode |
| `SettingsSheet.kt` | Bottom sheet for resolution, frame rate, timer, and quality configuration |
| `MediaGallery.kt` | `MediaStore` synchronizer, chronological grid, pager viewer, delete with undo |
| `ZebraStripesOverlay.kt` | Overexposure highlight rendering on the viewfinder |

### State Flow

```
Shutter Tap / Volume Key
        |
        v
┌─────────────────┐
│  Timer Countdown│  ← 3s/10s coroutine (if enabled)
│  (gated by      │
│   isCapturing)  │
└────────┬────────┘
         v
┌─────────────────┐
│ CameraPreview   │  ← Capture JPEG / Start Recording
│  (ImageCapture  │
│   VideoCapture) │
└────────┬────────┘
         v
┌─────────────────┐
│ MediaStore Write│  ← Scene_{scene}_Take_{take}_{timestamp}.ext
└────────┬────────┘
         v
┌─────────────────┐
│ latestMediaItem │  ← Updates thumbnail + ghost overlay fallback
│ currentTake++   │
└─────────────────┘
```

### Lifecycle Management

```
Activity.onStart()  →  bind Preview + ImageCapture + VideoCapture
                        to lifecycle-aware ProcessCameraProvider

Activity.onPause()  →  CameraX auto-handles backgrounding
                        (NO explicit unbind — preserves state & avoids black flash)

Activity.onDestroy() →  unbindAll(), close sensors, recycle buffers
```

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| **UI Framework** | Jetpack Compose (Material 3) |
| **Camera Engine** | CameraX 1.5.0 (Camera2 Interop) |
| **Image Loading** | Coil (async caching + recycling) |
| **Permissions** | Accompanist Permissions |
| **Concurrency** | Kotlin Coroutines (`Dispatchers.IO` for MediaStore) |
| **Sensors** | Android `SensorManager` (`TYPE_ROTATION_VECTOR`) |
| **Storage** | MediaStore API (Scoped Storage compliant) |
| **Build System** | Kotlin DSL |

### Theme: Slate Cosmic Dark

```kotlin
// Core palette
Background    = Color(0xFF000000)        // Pure Deep Black
Surface       = Color(0xFF1C1C1E)        // Obsidian Charcoal
Primary       = Color(0xFFFF9500)        // Vivid Golden Orange
Error         = Color(0xFFFF3B30)        // Crimson Red
Typography    = System Sans + JetBrains Mono (telemetry readouts)
```

---

## Building from Source

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK with API 34
- A physical Android device with Camera2 support (emulator camera is limited)

### Steps

```bash
# 1. Clone
git clone https://github.com/darkhood477-byte/aurea-cam.git
cd aurea-cam

# 2. Local properties (if needed)
echo "sdk.dir=/path/to/Android/Sdk" > local.properties

# 3. Build debug APK
./gradlew assembleDebug

# 4. Install to connected device
./gradlew installDebug
```

### Project Structure

```
app/src/main/java/com/aistudio/aureacam/
├── MainActivity.kt
├── camera/
│   ├── CameraScreen.kt              # Central state & HUD layout
│   ├── CameraPreview.kt             # CameraX binding & lens controls
│   ├── CompositionOverlay.kt        # Grid enum definitions
│   ├── CompositionOverlayCanvas.kt  # Mathematical grid rendering
│   ├── VirtualHorizon.kt            # IMU sensor fusion & level UI
│   ├── ZebraStripesOverlay.kt       # Overexposure highlights
│   ├── SettingsSheet.kt             # Bottom configuration panel
│   └── MediaGallery.kt              # Asset manager & viewer
└── ui/theme/
    ├── Color.kt                     # Slate Cosmic palette
    ├── Type.kt                      # Typography scales
    └── Theme.kt                     # Material 3 theme composition
```

---

## Contributing

We welcome contributions from cinematographers and developers alike.

### Reporting Issues

- **Bug?** Open an issue with device model, Android version, and reproduction steps
- **Feature request?** Describe the cinematic use case — we prioritize real-world workflows
- **Performance issue?** Include a CPU/GPU profile and memory snapshot if possible

### Code Style

- Kotlin official style guide
- Compose `@Preview` annotations for all new UI components
- State hoisting to `CameraScreen` — keep children stateless where possible
- `Dispatchers.IO` for all MediaStore and file I/O operations

### Pull Request Process

1. Fork the repo and create a feature branch: `git checkout -b feature/your-feature`
2. Add `@Preview` composables for UI changes
3. Ensure `./gradlew ktlintCheck` passes
4. Update this README if user-facing features change
5. Open a PR with a clear description and screenshots/GIFs

---

## License

```
MIT License

Copyright (c) 2026 AureaCam Contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
```

---

<div align="center">

**[⬇ Download Latest Release](https://github.com/darkhood477-byte/aurea-cam/releases)** · **[📖 Documentation](docs/)** · **[🐛 Report Bug](https://github.com/darkhood477-byte/aurea-cam/issues)**

*Crafted for the frame. Built for the story.*

</div>
 README.md…]()



## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)


1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the project directory and set `GEMINI_API_KEY` in that file to your Gemini API key (see `.env.example` for an example)
5. Remove this line from the app's `build.gradle.kts` file: `signingConfig = signingConfigs.getByName("debugConfig")`
6. Run the app on an emulator or physical device
