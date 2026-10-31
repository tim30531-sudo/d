# CyberGym PvP 0.8 — AC Practice Guard

This mod is for private or explicitly permitted PvP testing environments.

## What the guard does
The **AC practice guard** module starts **ON**. It applies local restrictions
to **automated** attacks and optionally limits automatic aim rotation.

The setting is in **Right Shift → Settings → AC guard profile**:
- **STRICT** (default): client raycast must hit the target, line of sight
  must be clear, target stable for two consecutive ticks, reach <= 2.85
  blocks, at least 170 ms between automated attacks, near-full vanilla
  attack cooldown, and automatic rotation <= 1.8° per client tick.
- **BALANCED**: client raycast, line of sight, reach <= 3.0 blocks, at least
  120 ms, attack cooldown, rotation <= 3.0° per tick.
- **PRACTICE**: client raycast, line of sight, reach <= 3.0 blocks, capped
  by configured CPS, attack cooldown, rotation <= 5.0° per tick.

The HUD **locally held** counter reports attacks prevented by these local
rules. It is NOT an anti-cheat detection or server flag counter.

## What it does not do
- Does **not** claim to bypass Matrix, AAC, Vulcan, Verus, Hypixel, or other
  anti-cheats.
- Does **not** forge, inject, cancel, replay or spoof movement/attack packets.
- Does **not** enable server-side reach or movement powers.
- Does **not** protect other independently installed mods.
- Does **not** intercept normal physical mouse attacks.
- Does **not** change server validation.

Anti-cheat behavior varies by server configuration, latency, version and
plugins; no client can guarantee bypass. Server/online testing has not been
performed as part of the build, so server compatibility is **unverified**.

## Installation
Use **Minecraft 1.21.11**, **Java 21**, compatible **Fabric Loader** and
**Fabric API**. Put `CyberGym-PvP-Fabric-1.21.11-v0.8.0.jar` in the
Minecraft `mods` directory. Avoid keeping older CyberGym PvP JARs in that
directory (duplicate mod IDs cause Fabric startup errors).

## Test scope
CI: Java 21 + Yarn mapped Fabric source compilation, remapped production
JAR inspection, and core self-tests. This is not an actual Minecraft launch
or private-server integration test. Check Minecraft logs if any third-party
mod conflict occurs.
