# MC Spatial Audio

[![Latest release](https://img.shields.io/github/v/release/KirillEraser/mc-spatial-audio?include_prereleases&label=release)](https://github.com/KirillEraser/mc-spatial-audio/releases)
[![Build](https://github.com/KirillEraser/mc-spatial-audio/actions/workflows/build.yml/badge.svg)](https://github.com/KirillEraser/mc-spatial-audio/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

**Apple-native spatial audio for Minecraft on macOS.** Minecraft's sound goes through Apple's headphone spatial renderer — the same one macOS uses for movies and music — with optional head tracking on AirPods Pro / Max and other supported headphones. Minecraft keeps doing its own positioning and distance attenuation; [Sound Physics Remastered](https://modrinth.com/mod/sound-physics-remastered) works alongside it but isn't required.

> **Status: 1.0 release candidate.** Builds and automated tests pass and the alpha was tested on real hardware, but the new settings screen, reload flow and channel balance still need listening checks before 1.0.0 stable. Bug reports welcome.

## Requirements

| | |
|---|---|
| Minecraft | 1.21.1 |
| Loader | NeoForge 21.1.256+ (within 21.1) |
| OS | macOS 13 Ventura or newer, Apple Silicon or Intel |
| Java | 21 (recommended) |
| Head tracking | AirPods Pro / Max / 3rd gen, Beats with spatial audio, or other Apple-supported headphones |

## Install

1. Download `mc-spatial-audio-<version>.jar` from [Releases](https://github.com/KirillEraser/mc-spatial-audio/releases) — the **JAR asset**, not "Source code (zip)".
2. Remove any older copy, drop the JAR into your instance's `mods` folder and launch.
3. Set your headphones as the macOS sound output.
4. In game: **Mods → MC Spatial Audio → Config** → enable spatial audio → **Apply & reload**.

No restart needed for settings changes. New installs start with spatial audio **off**.

## Usage

- **Head tracking** on = Apple tracks your head; off = fixed spatialization.
- `/mcspatial status` — diagnostics (also **Copy diagnostics** in the config screen).
- `/mcspatial reload` — rebuild the audio output. Do this after switching output devices or if sound drops.
- To go back to plain OpenAL audio, turn spatial audio off and apply, or just remove the JAR.

## How it works

```text
Minecraft (+ optional Sound Physics Remastered)
  → OpenAL 48 kHz float 7.1 loopback mix
  → channel-order conversion
  → Apple Spatial Mixer → sample-rate converter → macOS output
```

A universal (arm64 + x86_64) native library is bundled inside the JAR and loaded over JNI. No helper app, no separate process, no menu-bar icon. Apple handles head-tracking and the reference frame; the mod doesn't add its own recenter timer or mess with channel gains. Details: [ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Limitations

- Always uses the **macOS default output**, ignoring Minecraft's device picker.
- Renders a horizontal **7.1 bed**, not per-sound spatial objects — elevation is limited, and UI/music get spatialized too.
- Recentering behaviour and system audio controls are up to macOS and your headphones.
- Native binaries are **ad-hoc signed**, not notarized. The mod never touches your launcher, Java runtime, Gatekeeper or SIP — don't disable those for it.
- macOS only. On other OSes the mod does nothing.

## Troubleshooting

- **No sound / crackles after changing devices** → `/mcspatial reload`.
- **Still broken** → disable spatial audio and apply, or remove the JAR.
- **Reporting a bug** → open an [issue](https://github.com/KirillEraser/mc-spatial-audio/issues) with `/mcspatial status` output, relevant `latest.log` lines, your Mac model, macOS and headphone model.

The release also ships an optional `MCSpatialProbe` app — a standalone test-tone generator for checking the spatial renderer outside Minecraft. See [INSTALL_PREBUILT.md](docs/INSTALL_PREBUILT.md) and [TESTING.md](docs/TESTING.md).

## Building from source

macOS with JDK 21, Gradle 8.14.3, CMake 3.24+ and Xcode command-line tools:

```sh
bash scripts/build-native.sh
gradle clean build
```

Output: `build/libs/mc-spatial-audio-<version>.jar`. GitHub Actions builds, tests and publishes releases ([release workflow](.github/workflows/release.yml)).

## License

[MIT](LICENSE)
