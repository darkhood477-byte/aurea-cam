# AureaCam — Software Architecture & System Design Manual

This document provides a comprehensive technical overview of the software architecture, class relations, data streams, and pipeline integrations powering **AureaCam**.

---

## 1. High-Level Architecture Diagram

AureaCam is built following a decoupled, reactive model-view architecture entirely within Jetpack Compose and Google’s CameraX Jetpack support library. The following layout illustrates the operational streams of the application:

```
                  +----------------------------------+
                  |         Android OS Kernel        |
                  +----------------------------------+
                     /             |              \
       Camera Sensors         Device Storage        Inertial Sensors
            |                      |                       |
            v                      v                       v
     [ CameraX API ]        [ MediaStore API ]    [ SensorManager API ]
            |                      |                       |
            | (YUV/RGBA Frames)    | (URIs / Metadata)     | (Pitch/Roll Radians)
            v                      v                       v
   +------------------+   +------------------+   +-------------------+
   |  CameraPreview   |   |   MediaGallery   |   |  VirtualHorizon   |
   |   (Viewfinder)   |   |  (Asset Engine)  |   | (Telemetry State) |
   +------------------+   +------------------+   +-------------------+
            \                      |                       /
             \                     |                      /
              v                    v                     v
            +-------------------------------------------+
            |               CameraScreen                |
            |     (Central State Engine & Main UI)      |
            +-------------------------------------------+
               /           |             \            \
              v            v              v            v
       [SettingsSheet] [Composition] [ZebraStripes] [OverlayEngine]
```

---

## 2. Structural Directory & Package Architecture

The codebase is structured logically under the `com.example` namespace. Below is the directory map of the functional elements:

```
/app/src/main/java/com/example/
│
├── MainActivity.kt                  # App Entry Point, Perms Guard, Content Root
│
├── camera/                          # Camera & Creator Utilities Module
│   ├── CameraScreen.kt              # Core UI Layout, HUD Panels, Central State Hook
│   ├── CameraPreview.kt             # CameraX Engine, Lifecycle Binders, Optics Controls
│   ├── CompositionOverlay.kt        # Structural Enum Definitions for 10 Layouts
│   ├── CompositionOverlayCanvas.kt  # Mathematical Grid Drawing Superposition Canvas
│   ├── VirtualHorizon.kt            # Telemetry Listener, Orientation Math & Level UI
│   ├── ZebraStripesOverlay.kt       # Overexposure Highlighting Graphic Overlay
│   ├── SettingsSheet.kt             # Bottom Configuration Sheet (Resolutions & Timers)
│   └── MediaGallery.kt              # MediaStore Synchronizer, Chronological Grid & Pager
│
└── ui/
    └── theme/                       # Slate Cosmic Theme Color palettes & Typography
```

---

## 3. Key Core Modules & Class Interfaces

### A. App Entry Point (`MainActivity`)
- **Duty**: Controls the window environment, applies edge-to-edge system integrations (`enableEdgeToEdge()`), and enforces strict permission assertions (Camera and Audio; Storage permissions are requested dynamically only when accessing the local captures gallery, conforming to the modern Android Scoped Storage model. Notice: custom ghost image retrieval via standard GetContent() requires no storage permission at all) using Google's Accompanist permissions API.
- **Access Strategy**: Once the permission block returns successful states, it launches the unified `CameraScreen`.

### B. Viewfinder Controller & Lens Integrator (`CameraPreview`)
- **Duty**: Provides a specialized Composable wrapper around a hardware-accelerated `PreviewView`.
- **System Interactions**:
  - Connects to the local thread `ProcessCameraProvider`.
  - Configures and binds an `ImageCapture` usecase for high-resolution stills.
  - Configures and binds a `VideoCapture<Recorder>` usecase powered by the CameraX Video API for cinematic clips.
  - Exposes handles to control exposure compensation, lens focus vectors, optical zoom parameters, and torch toggles.

### C. State Controller (`CameraScreen`)
- **Duty**: Coordinates user inputs, states, and event feedback loops.
- **Key State Variables**:
  - `isGhostOverlayEnabled` / `ghostOverlayOpacity` / `ghostOverlayCustomUri`: Controls the visual state and opacity of reference layers.
  - `activeCompositionGrid`: Enum declaring which composition canvas is active.
  - `latestMediaItem`: Stores the newest file URI from the database/MediaStore to facilitate rapid preview and Ghost-continuity mode.
  - `currentScene` / `currentTake`: Maintains the active cinematic Scene and Take indexes.

### D. Mathematical Superposition Canvas (`CompositionOverlayCanvas`)
- **Duty**: Custom drawing operations utilizing Jetpack Compose `Canvas` drawing scopes.
- **Mathematical Implementations**:
  - **Fibonacci Spiral**: Calculated using rotating coordinate frames translated progressively by logarithmic scale factors $\Phi \approx 1.618$.
  - **Golden Grid / Phi Grid**: Spaced according to the reciprocal divisions of the golden ratio ($0.382$ and $0.618$ relative to viewfinder dimensions).
  - **Dynamic Symmetry**: Draws corner-to-corner vectors combined with reciprocal normals.

### E. Telemetry Fusion Engine (`VirtualHorizon`)
- **Duty**: Interfaces with Android's `SensorManager` to fetch inertial measurement unit (IMU) registers and maps coordinates.
- **Mathematical Alignments**:
  - Reads rotation matrices and extracts Euler angles (Pitch and Roll in radians).
  - Normalizes pitch and roll to human-readable angular degrees ($-180^\circ$ to $+180^\circ$).
  - Implements a low-pass exponential smoothing filter:
    $$\theta_{smoothed} = \alpha \cdot \theta_{new} + (1 - \alpha) \cdot \theta_{previous}$$
    Where $\alpha = 0.12$ to suppress noise while maintaining instantaneous alignment updates.
  - Transitions to **Bullseye Mode** if the pitch angle exceeds critical gravity pivots ($\approx 75^\circ$).

---

## 4. Unified Data & Action Streams

### A. Capture & Auto-Increment Workflow
1. User taps the **Shutter** or clicks a bound **Volume Key**.
2. If `Timer` state is set (3s/10s), the UI initiates a countdown coroutine.
3. Once the countdown expires, `CameraPreview` captures a high-resolution Jpeg or initiates Video recording.
4. On success, the file path is constructed dynamically: `Scene_{currentScene}_Take_{currentTake}.jpg`.
5. The Android `MediaStore` scanner is updated, registering the asset.
6. The `latestMediaItem` state updates, generating a fresh visual thumbnail in the bottom-left gallery hub.
7. The state engine automatically increments the `currentTake` counter to prep the shooter for the next shot.

### B. Custom Ghost Overlay Feed
1. The user taps the **Select Image (Folder)** icon.
2. The UI launches a contract picker: `ActivityResultContracts.GetContent()` filtering on `image/*`.
3. The returned local `Uri` is saved to `ghostOverlayCustomUri`.
4. The Composable Viewport superimposes a translucent image layer on top of the viewfinder:
   - Target model: `ghostOverlayCustomUri` (if selected) falling back to `latestMediaItem` (if custom Uri is empty).
   - Applied modifier: `Modifier.alpha(ghostOverlayOpacity)`.

---

## 5. CameraX Lifecycle Bindings

The application manages resource allocation in complete harmony with Android's lifecycle state machine:

```
[Activity Started] ---> processCameraProvider.getInstance(context)
                             |
                             v
                     [Bind To Lifecycle]
                             |
         +-------------------+-------------------+
         |                   |                   |
         v                   v                   v
     [Preview]        [ImageCapture]      [VideoCapture]
         |                   |                   |
         +-------------------+-------------------+
                             |
[Activity Paused]  ---> Unbinds All Use Cases Automatically
[Activity Destroyed]-> Closes Sensors & Shuts Down Threads Safely
```

---

## 6. Local Asset & Memory Architecture

- **High-Performance Viewport**: Visual items are rendered inside the media gallery using **Coil (Coroutines Image Loader)**. Coil provides asynchronous image caching, recycling, and thread dispatching.
- **Asynchronous Queries**: To prevent UI freezes and Application Not Responding (ANR) errors, the gallery's `MediaStore` scan executes inside a dedicated background thread dispatcher (`Dispatchers.IO`).
- **Memory Safety**: Whenever the camera preview binds or unbinds, allocated system frames, buffer queues, and open sensor hooks are immediately recycled or closed, preserving device memory and optimizing battery efficiency.

---

### *Manual Metadata*
- **Target OS**: Android (API Level 24+)
- **UI Engine**: Jetpack Compose (M3)
- **Primary Graphics Framework**: Canvas Graphic Operations (Vector math)
- **Telemetry System**: Sensor fusion mapping (Pitch / Roll)
