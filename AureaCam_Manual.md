# Aurea Cam — Professional Creator Camera Manual

Welcome to the official developer and creator manual for **Aurea Cam** ( stylized as **AureaCam** ), a professional-grade, cinematic camera application engineered specifically for vloggers, content creators, and mobile cinematographers. 

Designed on **Jetpack Compose** and **CameraX** with a modern, high-contrast Material Design 3 aesthetic, AureaCam provides advanced tools that bridge the gap between smartphones and standalone professional cinema rigs.

---

## Table of Contents
1. [Introduction & Core Philosophy](#1-introduction--core-philosophy)
2. [Visual Identity & Theme Architecture](#2-visual-identity--theme-architecture)
3. [Deep-Dive Feature Specifications](#3-deep-dive-feature-specifications)
   - [A. Dynamic Viewfinder with Animated Resizing](#a-dynamic-viewfinder-with-animated-resizing)
   - [B. Multi-Mathematical Composition Overlays](#b-multi-mathematical-composition-overlays)
   - [C. Dual-Mode Electronic Virtual Horizon](#c-dual-mode-electronic-virtual-horizon)
   - [D. Cinematic Ghost Overlay Mode (with Storage File Access)](#d-cinematic-ghost-overlay-mode-with-storage-file-access)
   - [E. Physical Tactile Button Controller](#e-physical-tactile-button-controller)
   - [F. Slate Metadata & SMPTE Timecode System](#f-slate-metadata--smpte-timecode-system)
   - [G. Manual Professional Utility Controls](#g-manual-professional-utility-controls)
   - [H. Built-in Media Gallery & Asset Manager](#h-built-in-media-gallery--asset-manager)
4. [UI/UX Layout Architecture](#4-uiux-layout-architecture)
5. [HUD Button Reference Directory](#5-hud-button-reference-directory)

---

## 1. Introduction & Core Philosophy

Smartphones have powerful sensors, but standard stock camera interfaces often oversimplify controls or hide artistic alignment tools. AureaCam's primary philosophy is **Aesthetic Empowerment**. Every element on screen is designed to give the shooter instant visual feedback about:
- **Geometry & Composition**: Using advanced mathematical matrices overlaying the scene.
- **Orientation & Levelling**: Real-time pitch and roll spirit levels that prevent tilted shots.
- **Temporal Alignment**: Hardware-accurate scene slate labelling and SMPTE timecode generation.
- **Framing Continuity (Ghost Mode)**: Aligning consecutive takes seamlessly for transitions and stop-motion videos.

---

## 2. Visual Identity & Theme Architecture

The applet enforces a **Slate Cosmic Dark Theme** designed to protect screen-adaptability in low-light environments (maximizing the user's focus on actual scene colors):

- **Background Canvas**: Pure Deep Black (`#000000`) and Obsidian Charcoal (`#1C1C1E`) to maximize contrast and eliminate edge bleed.
- **Primary Color Accents**: Vivid Golden Orange (`#FF9500` / `Orange500`) and Crimson Red (`Red500`) to highlight active recording modes, target alignments, and focus locks.
- **Typography**: Paired display typography with high tracking, utilizing clean system sans-serifs for readability, and monospace layouts (`JetBrains Mono` style) for telemetry readouts like the SMPTE timecode.
- **Aesthetic Negatives**: Generous padding (16dp to 24dp) surrounding the interactive buttons prevents accidental touches and simulates physical hardware controls.

---

## 3. Deep-Dive Feature Specifications

### A. Dynamic Viewfinder with Animated Resizing
*   **What it is**: A responsive live viewfinder that adapts to cinematic crops.
*   **How it works**: Unlike standard viewers that stretch or distort feed aspects, AureaCam calculates the target aspect ratios dynamically based on:
    - **9:16** (TikTok, Reels, Shorts)
    - **3:4** (Classic photography)
    - **1:1** (Square portrait)
    - **4:3** (Standard sensor output)
    - **16:9** (Widescreen UHD Broadcast)
    - **FULL** (Device-native display)
*   **The UX transition**: Selecting an aspect ratio triggers a 350ms smooth geometric animation using `animateDpAsState` with a `FastOutSlowInEasing` algorithm. The viewfinder container gracefully shrinks, expands, and centers itself on a dark background, giving the shooter a tactile feel of moving shutter blades.

### B. Multi-Mathematical Composition Overlays
*   **What it is**: An advanced mathematical grid overlay canvas containing ten professional compositional guidelines.
*   **How they work**: Drawn via a customized high-performance Jetpack Compose `<Canvas>` superposed on the camera feed at `0.6f` opacity.
    1.  **Rule of Thirds**: Standard dual horizontal and vertical division ticks.
    2.  **Golden Ratio (Phi Grid)**: Divisions spaced at $1 : 1.618$ to frame centers of interest.
    3.  **Golden Spiral (Fibonacci)**: Mathematically plotted logarithmic curves drawing logarithmic arcs across reciprocal grids.
    4.  **Golden Triangle**: Splits the screen diagonally from corner to corner with perpendicular vectors leading to opposite corners. Excellent for high-action diagonal compositions.
    5.  **Dynamic Symmetry**: Draws reciprocal diagonals creating a geometric armature of intersections.
    6.  **Leading Lines**: Perspective line indicators focusing from bottom corners towards center-sensor height.
    7.  **Framing**: Visual inner boundaries showing a 10% safety margin.
    8.  **Symmetry Centering**: A dynamic technical crosshair.
    9.  **Radial Symmetry**: Radiating lines dividing the frame into 8 identical geometric sectors.
    10. **Vanishing Point**: Multi-vector convergence lines that align with linear backgrounds (e.g., roads, corridors).

### C. Dual-Mode Electronic Virtual Horizon
*   **What it is**: A real-time camera leveler that uses on-board telemetry sensors to coordinate pitch (front/back tilt) and roll (sideways rotation).
*   **How it works**: Leverages `Sensor.TYPE_ROTATION_VECTOR` or a sensor fusion of `TYPE_ACCELEROMETER` and `TYPE_MAGNETIC_FIELD`. Remaps coordinates relative to system rotation, applies low-pass exponential smoothing ($\alpha = 0.12$) to eliminate physical hand jitter, and displays 2 layout configurations:
    1.  **Upright standard level**: Displays tick marks, a rotating virtual horizon line matching roll, and a secondary horizontal spirit bubble vial capsule below the center point. 
    2.  **Flat (Table-Top) Mode**: When pitch exceeds 75° (e.g., shooting flat lay shots from above), the level transforms automatically into a circular bullseye spirit level. Roll controls the X-axis of a dynamic central bubble, and pitch controls the Y-axis.
*   **Tactile Haptic Lock**: Once alignment is level within ±1.0° of tolerance, the horizon turns **Vibrant Golden Orange** and emits an immediate tactical haptic click (`performHapticFeedback`) to notify the shooter without requiring them to break visual focus.

### D. Cinematic Ghost Overlay Mode (with Storage File Access)
*   **What it is**: An on-screen translucent overlay that guides frame continuity for sequence shots, transition matches, and stop-motion projects.
*   **How it works**: Uses an `AsyncImage` loaded at a default `0.4f` opacity. The shooter has two methods to load an overlay:
    - **Immediate Previous Take**: Selecting Ghost Mode automatically superimposes the latest photo or video poster-frame captured on the phone.
    - **Custom Storage File**: Features a dedicated storage file picker integration (`ActivityResultContracts.GetContent()`). Pressing the **Select Image** button prompts the native Android Document UI, allowing the user to select *any* historical visual asset from their local internal storage or SD card.
*   **Tactile Adjustments**: An adjacent HUD vertical slider lets creators adjust opacity smoothly from `0.0f` (completely transparent) to `1.0f` (opaque solid reference image) or wipe it via the clear red reset button.

### E. Physical Tactile Button Controller
*   **What it is**: A physical hardware button integrator replicating professional camera control wheels.
*   **How it works**: Listens directly to volume button triggers to alter parameters without requiring the user to touch the screen:
    - **Single Click Volume Up**: Increments the current control parameter or shoots a picture.
    - **Single Click Volume Down**: Decrements the current control parameter or shoots a picture.
    - **Long Press Volume Up/Down**: Rotates through active modes: `ZOOM`, `FOCUS`, or `SHUTTER` parameters.
*   **Tactile Feedback**: Every key capture is visually reflected inside an interactive HUD Toast and active text indicators.

### F. Slate Metadata & SMPTE Timecode System
*   **What it is**: A cinematic organization system that tags captures with structural metadata: Scene and Take.
*   **How it works**: 
    - Users can access the **Slate Dialog Box** to enter custom Scene identifiers.
    - Captures are saved using structured titles, e.g., `Scene_01_Take_3_20260719_120000_abcd.mp4`. Filenames automatically append both a microsecond-precise timestamp and a unique 4-character random alphanumeric suffix, guaranteeing that naming collisions or file overwrites are physically impossible under any circumstance.
    - Recording videos triggers a live simulated **SMPTE Timecode generator** running on a coroutine loop. It counts time strictly in `Hours : Minutes : Seconds : Frames` (calibrated to approximate 24 frames per second).

### G. Manual Professional Utility Controls
*   **What it is**: Manual parameter controls for ISO, exposure bias, shutter speed, manual focus distance, and white balance.
*   **How it works**:
    - **Tap-To-Focus**: Clicking any point on the camera viewfinder requests focusing at specific sensor matrices, displaying a target reticle.
    - **Long-Press Exposure Lock**: Holding the viewfinder locks current exposure ratings, turning indicators golden.
    - **Exposure Slider**: Dragging horizontally adjusts exposure compensation values. The bounds of the slider are dynamically queried from the camera hardware's `ExposureState.exposureCompensationRange` at runtime, rather than claiming static claims of ±12 stops, ensuring accurate physical scale values without clipping or crashes.
    - **Manual Focus Distance Slider**: Dragging lets users focus from hyperfocal infinity to extreme macro focal points.

### H. Built-in Media Gallery & Asset Manager
*   **What it is**: A comprehensive, integrated captures gallery.
*   **How it works**: Uses an asynchronous MediaStore query (`queryMedia`) to dynamically load images and videos sorted chronologically (newest first). Features:
    - **Dynamic Permissions**: Under modern Android Scoped Storage model, clicking the gallery button checks and prompts for `READ_MEDIA_IMAGES`/`READ_MEDIA_VIDEO` (API 33+) or `READ_EXTERNAL_STORAGE` (API 29-32) dynamically, while custom ghost retrieval using `GetContent()` runs sandbox-isolated and requires no permissions at all.
    - **Visual Indicators**: Videos are overlaid with durations and an explicit Play button.
    - **Fluid Pager Viewer**: Clicking opens a full-screen high-resolution media viewer with horizontal swiping capabilities.
    - **Video Playback**: Videos can be launched directly into standard device players.
    - **Quick Delete**: Features a fast file deletion routine utilizing the `contentResolver.delete` engine. The layout instantly updates and smoothly synchronizes.

---

## 4. UI/UX Layout Architecture

AureaCam adheres to Material Design 3 guidelines with a strict mobile-first layout prioritizing ergonomic accessibility:

```
+-----------------------------------------------------------+
| [Settings]                [Overlays]             [Timer]  |  <-- TOP HUD CONTROL BAR
+-----------------------------------------------------------+
|                                                           |
|  [Mirror]                                                 |
|  [Ghost]                                                  |  <-- VIEWFINDER HUD
|  [Picker]                  VIEWFINDER SCREEN              |      SIDEBAR STRIP
|  [Clear]                                                  |
|  [Opacity]                                                |
|                                                           |
|                       [Horizon Indicator]                 |
|                                                           |
+-----------------------------------------------------------+
|                      [Manual Adjustments]                 |  <-- MANUAL SLIDER WIDGETS
+-----------------------------------------------------------+
|  [Gallery]               [SHUTTER BUTTON]         [Video] |  <-- PRIMARY SHOOTING PANEL
|                          [Scene 01 / Take 3]              |
+-----------------------------------------------------------+
```

*   **Touch Targets**: Every interactive element, icon button, and menu choice is bounded by a minimum active region of **48dp x 48dp** to satisfy modern accessibility standards.
*   **Control Strip Auto-Fade**: To offer an unobstructed view, non-essential HUD overlays (everything except the main Shutter button, active record visual indicator, and slate display tag) automatically fade out after 1.5 seconds of user inactivity. Dragging or touching any slider, dial, or control resets the inactivity timer instantly. The entire HUD is guaranteed to remain fully visible whenever any slider is being actively pressed or the zoom dial is being dragged, preventing unwanted UI disappearances. Non-interactive overlay panels also ignore touch events completely when faded out to prevent accidental clicks.

---

## 5. HUD Button Reference Directory

The following table provides size, placement, and functional parameters for AureaCam's control buttons:

| Button / Icon | Screen Placement | Target Size | Primary Function | Secondary Function |
| :--- | :--- | :--- | :--- | :--- |
| **Settings (Gear)** | Top Left | 48dp x 48dp | Opens the settings panel. | Configure Resolution, Frame rate, Audio, and Burst mode. |
| **Overlays (Grid)** | Top Center | 48dp x 48dp | Accesses composition overlays. | Toggles grid selection (10 available options). |
| **Timer (Clock)** | Top Right | 48dp x 48dp | Configures capture countdown. | Selects OFF, 3 seconds, or 10 seconds. |
| **Mirror (Cameraswitch)** | Middle Left (Front Cam) | 36dp x 36dp | Flips front preview mirroring. | Reverts viewfinder image mapping. |
| **Ghost Mode (Details)** | Middle Left | 36dp x 36dp | Activates/deactivates translucent overlay. | Uses the last capture if no custom image is selected. |
| **Select Image (Image)** | Middle Left | 36dp x 36dp | Launches Android native file picker. | Imports custom overlays from device storage. |
| **Clear Ghost (Close)** | Middle Left | 36dp x 36dp | Wipes the custom selected ghost uri. | Restores ghost mode behavior to default (latest shot). |
| **Manual Adjust (Tune)** | Middle Bottom (Left of Shutter) | 48dp x 48dp | Shows manual parameter panels. | Adjust ISO, shutter speed, and white balance. |
| **Gallery Button (Circle)** | Bottom Left | 56dp x 56dp | Opens the asset gallery page. | Displays the thumbnail of the latest captured file. |
| **Shutter Button (Circle)**| Bottom Center | 84dp x 84dp | Triggers capture or record start/stop.| Displays countdowns and burst mode progressions. |
| **Slate Toggle (Edit)** | Bottom Center (Subtext) | 48dp x 48dp | Opens scene/take editor. | Modifies scene names; resets take index back to 1. |
| **Mode Switch (Rotate)** | Bottom Right | 56dp x 56dp | Toggles photo vs video shooting mode. | Adjusts viewfinder overlays according to selected mode. |

---

### *Manual Metadata*
- **App Name**: Aurea Cam
- **Package**: `com.aistudio.aureacam`
- **Compiler target**: Jetpack Compose, CameraX, Kotlin DSL.
