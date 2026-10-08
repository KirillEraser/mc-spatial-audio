# MC Spatial Audio — 1.0 release candidate

Apple-native headphone spatial audio for Minecraft 1.21.1 / NeoForge on macOS. The native renderer that worked in the alpha is retained; Minecraft's positioning and attenuation are unchanged.

## Polish

- In-game settings: **Mods → MC Spatial Audio → Config**.
- Spatial-audio/head-tracking toggles, safe apply/reload, defaults and diagnostic copy.
- Atomic settings persistence; existing alpha properties files remain compatible.
- Skip vanilla's playback-only HRTF reset only on our loopback device; ordinary output is unchanged.
- Channel-order, isolation, polarity and unchanged-gain regression checks.
- Quieter startup logs and concise project documentation.

Apple controls the tracking reference frame. No custom recenter timer, raw-pose integration, rear-channel boost or global attenuation workaround is introduced.

## Download

Install the `mc-spatial-audio-<version>.jar` asset. It includes the universal macOS native library; no separate process/build is needed. `INSTALL.md` explains setup, and the probe ZIP is optional. Source ZIPs are not compiled mods.

Compilation/tests, universal native packaging and JNI ABI checks gate publication. The new UI/reload behavior, stationary-head recentering, perceived channel balance and Sound Physics Remastered coexistence still need candidate listening/client checks before a validated stable release. The alpha's user report is not automatically a test of this candidate.

Requires Minecraft 1.21.1, NeoForge 21.1.256+ within 21.1, macOS 13+. Java 21 recommended. Uses the macOS default output; explicit reload is needed after device changes. Horizontal 7.1 bed, not object audio. Native files are ad-hoc signed, not Developer ID signed/notarized. macOS system controls remain platform-governed.
