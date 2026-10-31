# CyberGym PvP 0.9 – Server-validation-aware practice mode

This version **does not bypass anti-cheat software**. There is no credible
universal way to guarantee bypassing server-side checks. The changes aim to
reduce avoidable invalid automated actions in *authorized private arenas*.

## New behavior
- While a held-use or block placement input is active, automated attacks stop.
- After right-click / item use, configurable grace before automated attacks
  (default five client ticks; persisted per profile).
- After a large (>6-block) position jump or world change, configurable pause
  (default 16 client ticks). This detects possible teleports, **not**
  anti-cheat corrections.
- In conservative mode, KillAura's aim and attack require the physical attack
  key held (default ON; configurable). TriggerBot retains its intended
  crosshair-only action.
- Existing checks preserved: raycast target, 2.85/3.0-block limits, no
  through-wall hits, attack cooldown, stable crosshair, attack spacing,
  bounded aim changes.
- All actions remain vanilla client actions and **server validated**.

GUI: Right Shift → Settings → post-use delay, position jump pause, and
Aura requires held attack.

## Limits
- This is **not** a tested bypass for Vulcan, Matrix, AAC, Hypixel, Verus etc.
- Does not intercept/spoof/replay/cancel packets.
- Does not alter other installed Minecraft mods.
- Local build tests do not establish multiplayer anti-cheat compatibility.
- Any claim of detection avoidance requires controlled server tests with
  the particular plugin/version/configuration and its log evidence.
- Unmodified physical mouse clicks are never intercepted by this feature.

Built for Minecraft 1.21.11 / Java 21 / Fabric.
Remove older CyberGym PvP JARs from mods/ before installing.
