# Install the prebuilt experimental macOS mod

You do not need Gradle, CMake, Xcode, or Homebrew to install the release JAR.

## Requirements

- macOS 13 or newer.
- Minecraft 1.21.1 with NeoForge 21.1.256 (or a compatible later 21.1 version).
- Java 21, normally supplied/configured by your Minecraft launcher.
- AirPods Pro selected as the macOS default audio output for the intended tests.

## Download the correct file

From the GitHub release's Assets section, download `mc-spatial-audio-<version>.jar`.

Do NOT download `Source code (zip)` expecting a compiled mod. The JAR contains the universal Apple Silicon / Intel native audio library. `MCSpatialProbe-macos-universal.zip` is an optional independent test app, not a Minecraft mod and not a replacement for the JAR.

## Install and enable

1. Create a separate test Minecraft instance. Do not start with important worlds.
2. Put the downloaded JAR in that instance's `mods` directory.
3. Initially leave Sound Physics Remastered out so the output bridge can be tested alone.
4. Launch Minecraft once, then quit completely.
5. Open that instance's `config/mc-spatial-audio.properties` and set:

```properties
enabled=true
headTrackingRequested=true
```

6. Select AirPods Pro as the macOS sound output before relaunching Minecraft.
7. In a world, run `/mcspatial status`. Check that framesRendered increases and renderErrors stays zero.
8. Inspect the real macOS audio controls while a sound plays; keep the game camera still and turn your physical head to test tracking.
9. After basic playback succeeds, add Sound Physics Remastered's Minecraft 1.21.1 NeoForge build and repeat the test.

A head-tracking property readback of 1 does NOT prove active tracking or system-control eligibility. The mod has no custom menu-bar icon. A GitHub build is not an AirPods hardware test.

## If sound fails

Quit, set `enabled=false` in the same config, and relaunch. If needed, remove the mod JAR. Send `logs/latest.log`, `/mcspatial status` output, exact macOS version, Mac chip/model, launcher/Java version and AirPods generation.

Do not re-sign Java or your launcher, disable SIP/Gatekeeper, or change system security settings to force this prototype to load. The binary is ad-hoc signed, not Developer ID signed/notarized. Host library-validation and head-pose entitlement restrictions may still block it.

## Optional standalone probe

Unzip `MCSpatialProbe-macos-universal.zip` and select AirPods as the macOS output. Start with low volume. From the folder containing the extracted app:

```sh
./MCSpatialProbe.app/Contents/MacOS/MCSpatialProbe --fixed --seconds 32
./MCSpatialProbe.app/Contents/MacOS/MCSpatialProbe --seconds 32
```

The first command requests fixed spatialization; the second requests head tracking. This app is independently identified by macOS and success here is not proof of success for Minecraft. It has no Developer ID/notarization or authorized head-pose provisioning in this release; if macOS blocks it, report that rather than disabling protections.
