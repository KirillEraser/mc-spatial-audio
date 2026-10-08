# MC Spatial Audio

**Apple-native spatial audio for Minecraft on macOS.** Hear Minecraft through Apple's headphone renderer, with optional head tracking on supported headphones such as AirPods Pro. Normal sound positioning and distance attenuation stay in Minecraft; Sound Physics Remastered is optional.

## Install

Requires **Minecraft 1.21.1**, **NeoForge 21.1.256+ within 21.1**, and **macOS 13+**. Java 21 is the recommended build/runtime baseline.

1. Download the **mod JAR** from [Releases](https://github.com/KirillEraser/mc-spatial-audio/releases), not the source ZIP. Remove any older copy from your test instance.
2. Put it in the instance's `mods` folder and launch Minecraft.
3. Open **Mods → MC Spatial Audio → Config**.
4. Select your headphones as the macOS output, enable spatial audio, then choose **Apply & reload**.

Head tracking can be switched off for fixed spatialization. Settings changes do not require quitting Minecraft. `/mcspatial status` shows diagnostics; `/mcspatial reload` rebuilds the audio output. If you change output devices or lose sound, apply/reload again. Disable spatial audio to return to ordinary OpenAL output.

## How it works

```text
Minecraft + optional Sound Physics Remastered
  → OpenAL 48 kHz float 7.1 mix
  → channel-order conversion
  → Apple Spatial Mixer → sample-rate converter → macOS output
```

The JAR includes a universal Apple Silicon/Intel native library. No helper app or separate process is required. Apple handles headphone rendering and the tracking reference frame; the mod does not add its own recenter timer, boost rear channels, or create a menu-bar icon.

## Scope

- Uses the **macOS default output**, not Minecraft's OpenAL device selection.
- Renders a horizontal **7.1 bed**, not individual spatial objects; elevation detail and UI/music treatment have limits.
- Automatic reference-frame/recentering behavior and system controls are governed by macOS/headphones. Listening tests cannot be replaced by a property readback.
- Native binaries are ad-hoc signed, not Developer ID signed/notarized. The mod never modifies your launcher, Java runtime, entitlements or security settings.

Head tracking and attenuation were user-tested on an M4 Pro Mac. See [testing](docs/TESTING.md) for the remaining release checks and [backend details](docs/ARCHITECTURE.md) for the channel mapping.

## Build

On macOS, use JDK 21, Gradle 8.14.3, CMake 3.24+ and Xcode command-line tools:

```sh
bash scripts/build-native.sh
gradle clean build
```

The JAR is in `build/libs/`. GitHub Actions also builds/tests and publishes downloadable releases. Original code: MIT.
