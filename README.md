# Universal A/C Remote

<p align="center">
  <img src="docs/assets/readme/hero.svg" alt="Universal A/C Remote product overview" width="100%" />
</p>

<p align="center">
  <strong>A modern Android infrared remote for discovering, verifying, organizing, and controlling air conditioners — directly from the IR blaster built into your phone.</strong>
</p>

<p align="center">
  <a href="https://github.com/thanhnha01/universal-ac-remote/releases/latest"><strong>Download the latest APK</strong></a>
  · <a href="#how-it-works">How it works</a>
  · <a href="#features">Features</a>
  · <a href="#device-requirements">Device requirements</a>
</p>

<p align="center">
  <img alt="Latest release" src="https://img.shields.io/github/v/release/thanhnha01/universal-ac-remote?display_name=tag&style=flat-square" />
  <img alt="Android" src="https://img.shields.io/badge/Android-6.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white" />
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?style=flat-square&logo=kotlin&logoColor=white" />
  <img alt="IR" src="https://img.shields.io/badge/IR-ConsumerIrManager-087CF0?style=flat-square" />
  <img alt="ABI" src="https://img.shields.io/badge/ABI-arm64--v8a-0B1F44?style=flat-square" />
</p>

---

## Why this app exists

Universal A/C Remote solves one practical problem: your Android phone already has a built-in IR blaster, but the original A/C remote is missing, broken, inconvenient, or impossible to identify.

Instead of behaving like a generic remote-code database, the app is designed around how people actually find a working A/C remote:

- browse brands with an **A–Z catalog**,
- narrow by **series, A/C model, or remote model**,
- use a **safe Smart Scanner** when the exact model is unknown,
- save devices by **room, favorites, and recent use**,
- control them with an **adaptive remote** that only exposes functions the active profile can really transmit.

**No ESP32. No Wi-Fi bridge. No external IR hardware.** The phone itself is the remote.

---

## Download

The latest stable APK is published through GitHub Releases:

**[Download Universal A/C Remote](https://github.com/thanhnha01/universal-ac-remote/releases/latest)**

Recent release artifacts include the APK, update manifest, SHA-256 sums, catalog metadata, and source-lock information.

> Android may ask you to allow installation from the selected source. The app never uses silent installation.

---

## Features

### Fast device discovery

Search by brand, A/C model, or remote model. Or browse the full catalog using the A–Z brand index and drill into available series/model groups.

### Smart Scanner V2

When you do not know the exact model, Smart Scanner tests compatible profiles one at a time and waits for your confirmation before continuing.

<p align="center">
  <img src="docs/assets/readme/scanner-flow.svg" alt="Smart Scanner V2 workflow" width="100%" />
</p>

Scanner behavior is intentionally conservative:

- one candidate at a time,
- no automatic IR blast loop,
- explicit user confirmation after each probe,
- **Power verification last**,
- resumable scan progress,
- previously successful profiles ranked ahead on the same device.

### Adaptive Remote

The remote UI is generated from real profile capabilities. Depending on the selected profile, it can expose:

- Power
- Temperature
- Mode
- Fan speed
- Vertical swing
- Horizontal swing

Controls are hidden when the active profile cannot safely encode them.

> Consumer IR is one-way. **Last sent state** means what the app most recently transmitted — not the live state of the A/C.

### Rooms, favorites, and recents

Organize saved A/Cs by room and reach the devices you use most through Favorites, Recent devices, room filters, and device details.

### Smart `.ir` import

Import Flipper-style raw IR files directly from Android storage.

The importer validates the file, preserves the original waveform, classifies common command names, and normalizes labels such as Power, Temp ±, Fan, Swing, Timer, and Light.

It never invents timings or synthesizes commands that were not present in the file.

### Backup and restore

Export your local setup to a versioned JSON backup containing saved remotes, imported commands, rooms, favorites, verification data, recent usage, and last-sent state.

Restore is validated before replacing the local database.

### Hardware diagnostics

Diagnostics distinguish between **Ready**, **Limited**, and **Unavailable** IR states. When Android reports carrier-frequency ranges, the app shows them in kHz.

### App update flow

Stable APK updates are checked through GitHub Releases and validated with allowed HTTPS sources, manifest checks, and APK SHA-256 verification.

A database-update foundation also exists with compatibility checks, SHA-256 validation, signature-verifier hooks, atomic activation, and rollback support.

---

## How it works

```text
Compose UI
   ↓
Saved device / scanner / remote state
   ↓
AcState
   ↓
Protocol encoder or validated profile/raw command
   ↓
IrTransmission
   ↓
Android ConsumerIrManager
   ↓
Built-in phone IR blaster
```

Protocol-based remotes use a native bridge around selected IRremoteESP8266 A/C capabilities. Profile-based sources are validated and resolved before transmission.

---

## IR data sources

The unified catalog is assembled from multiple upstream projects:

- **IRremoteESP8266**
- **SmartIR**
- **Flipper IRDB** — A/C entries only
- **irplus**

Upstream inputs are pinned and tracked rather than pulling unreviewed latest content directly into every build.

See [Upstream sources](docs/UPSTREAM_SOURCES.md), [Protocol catalog](docs/PROTOCOL_CATALOG.md), and [IR protocol model](docs/IR_PROTOCOL.md).

---

## Device requirements

### Required

- Android **6.0 / API 23** or newer
- **arm64-v8a**
- built-in consumer IR emitter exposed through Android `ConsumerIrManager`

### Primary target

The initial hardware target is **OnePlus 15**.

The app can also work on other Android phones that expose a compatible built-in IR emitter through the Consumer IR API.

---

## Build from source

### Toolchain

- JDK 17
- Android SDK 35
- Android NDK 27.2.12479018
- CMake 3.22.1
- Kotlin + Jetpack Compose
- Room
- Native C++ IR bridge

### Debug build

```bash
./gradlew :app:assembleDebug
```

### Unit tests

```bash
./gradlew :app:testDebugUnitTest
```

### Lint

```bash
./gradlew :app:lintDebug
```

### Full local verification

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Release signing is configured through environment variables in CI and is not committed to the repository.

---

## Architecture

```text
app/src/main/java/com/thanhnha/universalacremote/
├── ProductionScreens.kt
├── RemoteControlScreen.kt
├── SavedRemoteStore.kt
├── ScannerSessionStore.kt
├── BackupCodec.kt
├── BackupPanel.kt
├── ImportedCommandPresentation.kt
├── ir/
│   ├── AcState.kt
│   ├── AndroidIrTransmitter.kt
│   ├── CatalogTransmitter.kt
│   ├── NativeAcEncoder.kt
│   ├── ProtocolRegistry.kt
│   ├── RemoteCapabilities.kt
│   ├── RemoteResolver.kt
│   └── UniversalAcScanner.kt
└── update/
    ├── AppUpdate.kt
    ├── DatabaseUpdate.kt
    └── UpdateScreen.kt
```

More detail:

- [System architecture](docs/ARCHITECTURE.md)
- [Product goals](docs/PRODUCT.md)
- [Testing strategy](docs/TESTING.md)
- [CI/CD](docs/CI_CD.md)
- [Update system](docs/UPDATE_SYSTEM.md)
- [UI / UX](docs/UI_UX.md)
- [Roadmap](docs/ROADMAP.md)

---

## Product principles

### Do not fake device state

Consumer IR is one-way, so the app uses **Last sent state** instead of pretending to know the live state of the A/C.

### Do not show dead controls

A feature being present in catalog metadata is not enough. A control should only be interactive when the active encoder/profile can produce a valid IR transmission for it.

### Do not brute-force IR

The scanner sends a candidate, waits for confirmation, and only then continues.

### Prefer reproducible data

IR sources, build inputs, and release artifacts are pinned and auditable.

---

## Project status

The V3 rebuild is merged into `main` and includes:

- V3 design system and navigation
- Home / Rooms / Favorites / Recents
- A–Z brand browser
- Brand → Series / Model discovery
- Smart Scanner V2 with resume
- Adaptive Remote + AcState V2 foundation
- Smart `.ir` import
- Backup / Restore
- App updater
- Database updater security foundation
- IR diagnostics
- regression fixes across the core flows

The next validation focus is physical-device testing across real A/C units and the primary OnePlus 15 target.

---

## Contributing

Before changing the repository, read [AGENTS.md](AGENTS.md).

Good pull requests should solve one clear problem, include tests for behavior changes, preserve scanner safety, avoid unsupported IR claims, and keep upstream attribution traceable.

---

## Security and trust

APK updates are verified by checksum and restricted to approved GitHub HTTPS sources.

The database updater foundation supports signed-payload verification, compatibility gating, atomic install, and rollback.

Do not submit secrets, signing keys, private remote dumps, or IR datasets without clear provenance.

---

## Acknowledgements

Universal A/C Remote builds on work from the open-source IR community, especially IRremoteESP8266, SmartIR, Flipper IRDB, and irplus.

---

<p align="center">
  <strong>Turn the Android phone already in your hand into the A/C remote you actually want to use.</strong>
</p>

<p align="center">
  <a href="https://github.com/thanhnha01/universal-ac-remote/releases/latest"><strong>Download the latest APK →</strong></a>
</p>