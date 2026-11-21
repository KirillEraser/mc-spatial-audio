# MC Spatial Audio 1.0.0

Apple-native headphone spatial audio for Minecraft 1.21.1 / NeoForge on macOS, with optional head tracking on AirPods and other supported headphones. Minecraft's own positioning and distance attenuation are unchanged.

## What's new since the alpha

- In-game settings: **Mods → MC Spatial Audio → Config**.
- Spatial audio and head-tracking toggles, **Apply & reload** without restarting, reset to defaults, and **Copy diagnostics**.
- `/mcspatial status` and `/mcspatial reload` commands.
- Settings save atomically; alpha config files carry over.
- Vanilla's HRTF reset is skipped only on the mod's own output device; normal output is untouched.
- Mod-list icon, quieter logs, rewritten docs.

## Download

Install `mc-spatial-audio-<version>.jar` — not the source ZIP. It contains the universal (Apple Silicon + Intel) native library; nothing else to install. See `INSTALL.md`. The probe ZIP is an optional test-tone app.

## Requirements and limits

- Minecraft 1.21.1, NeoForge 21.1.256+ (within 21.1), macOS 13+, Java 21.
- Uses the macOS default output; run `/mcspatial reload` after switching devices.
- Horizontal 7.1 bed, not per-sound object audio.
- Native files are ad-hoc signed, not notarized. Recentering and system audio controls are handled by macOS.
- Sound Physics Remastered is optional and compatible.
