package com.cybergym.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import java.util.UUID;

/**
 * Conservative optional compatibility guard for consent-based practice servers.
 *
 * This is NOT an anti-cheat bypass. It does not alter, spoof, drop, fabricate
 * or reorder packets. The server decides whether attacks and movement are valid.
 */
public final class AntiCheatGuard {
    public enum Rejection {
        NONE, NO_WORLD, MENU, WRONG_CROSSHAIR, INVALID_TARGET, COOLDOWN,
        RATE_LIMIT, TARGET_UNSTABLE, AIM_SNAP, USING_ITEM,
        RECENT_USE, POSSIBLE_TELEPORT
    }
    private long attacksAllowed;
    private long attacksBlocked;
    private Rejection lastRejection = Rejection.NONE;
    private UUID trackedTarget;
    private int trackedTicks;
    private long lastTrackedTick = -1;
    private long lastSentNs = Long.MIN_VALUE;
    private float previousYaw;
    private float previousPitch;
    private long previousYawTick = -1;
    private float recentRotation;
    private long lastRecordedAttackTick = -1;
    private Object observedWorld;
    private Vec3d lastPosition;
    private long lastPositionTick = -1;
    private long lastItemUseTick = -1;
    private long pauseUntilTick = -1;

    /** Public for dependency-free boundary tests; events use client-tick integers. */
    public static boolean withinGraceTicks(long currentTick, long eventTick, int graceTicks) {
        return eventTick >= 0 && graceTicks > 0
            && currentTick >= eventTick && currentTick - eventTick <= graceTicks;
    }

    /**
     * Records nearby client events that can invalidate automated attacks.
     * A large position jump is treated as a possible teleport, NOT proof of
     * an anti-cheat correction. No packet interception occurs.
     */
    public void observePlayerContext(MinecraftClient mc, long tick, Settings settings) {
        if (mc == null || mc.player == null || mc.world == null) {
            resetTransient();
            return;
        }
        Vec3d position = mc.player.getPos();
        if (observedWorld != mc.world) {
            observedWorld = mc.world;
            pauseUntilTick = tick + settings.correctionPauseTicks;
        } else if (lastPosition != null && lastPositionTick == tick - 1
                && position.squaredDistanceTo(lastPosition) > 36.0) {
            pauseUntilTick = tick + settings.correctionPauseTicks;
        }
        lastPosition = position;
        lastPositionTick = tick;
        if (mc.options.useKey.isPressed() || mc.player.isUsingItem()) lastItemUseTick = tick;
    }

    public long attacksAllowed() { return attacksAllowed; }
    public long attacksBlocked() { return attacksBlocked; }
    public Rejection lastRejection() { return lastRejection; }

    public static long minimumIntervalNanos(Settings.GuardPreset preset, int configuredCps) {
        int cps = Math.max(1, Math.min(12, configuredCps));
        long userInterval = 1_000_000_000L / cps;
        return switch (preset) {
            case STRICT -> Math.max(userInterval, 170_000_000L);
            case BALANCED -> Math.max(userInterval, 120_000_000L);
            case PRACTICE -> userInterval;
        };
    }

    public static double maximumRotation(Settings.GuardPreset preset, double requested) {
        double cap = switch (preset) {
            case STRICT -> 1.8;
            case BALANCED -> 3.0;
            case PRACTICE -> 5.0;
        };
        return Double.isFinite(requested) ? Math.max(0.0, Math.min(requested, cap)) : 0.0;
    }

    public static double maximumAttackRange(Settings.GuardPreset preset, double requested) {
        double cap = switch (preset) {
            case STRICT -> 2.85;
            case BALANCED, PRACTICE -> 3.0;
        };
        return Double.isFinite(requested) ? Math.max(0.0, Math.min(requested, cap)) : 0.0;
    }

    private boolean reject(Rejection reason) {
        lastRejection = reason;
        attacksBlocked++;
        return false;
    }

    /** Should be called once per playable client tick, after screen checks. */
    public void trackView(MinecraftClient mc, long tick) {
        if (mc == null || mc.player == null) { resetTransient(); return; }
        float yaw = mc.player.getYaw(), pitch = mc.player.getPitch();
        if (previousYawTick == tick - 1) {
            double yd = TargetingRules.wrappedAngle(yaw - previousYaw);
            double pd = pitch - previousPitch;
            recentRotation = (float)Math.hypot(yd, pd);
        } else recentRotation = 0;
        previousYaw = yaw;
        previousPitch = pitch;
        previousYawTick = tick;
    }

    /**
     * Checks the actual raycast target and vanilla cooldown. Only used for
     * automated attacks; physical/manual attacks are never intercepted.
     */
    public boolean permitAutomatedAttack(MinecraftClient mc, PlayerEntity target, Settings settings,
                                          long tick, long nowNanos, boolean guardEnabled) {
        if (mc == null || mc.player == null || mc.world == null || mc.interactionManager == null)
            return reject(Rejection.NO_WORLD);
        if (mc.currentScreen != null) return reject(Rejection.MENU);
        if (target == null || !(mc.crosshairTarget instanceof EntityHitResult hit)
            || hit.getEntity() != target) {
            trackedTarget = null; trackedTicks = 0;
            return reject(Rejection.WRONG_CROSSHAIR);
        }
        Settings.GuardPreset preset = guardEnabled ? settings.guardPreset : Settings.GuardPreset.PRACTICE;
        if (!TargetingRules.eligible(target != mc.player, target.isAlive(), target.isSpectator(),
            mc.player.canSee(target), mc.player.squaredDistanceTo(target),
            maximumAttackRange(preset, settings.maxTargetRange))) return reject(Rejection.INVALID_TARGET);
        if (mc.player.isSpectator() || mc.player.isUsingItem()
            || mc.options.useKey.isPressed()) return reject(Rejection.USING_ITEM);
        if (guardEnabled && tick < pauseUntilTick)
            return reject(Rejection.POSSIBLE_TELEPORT);
        if (guardEnabled && withinGraceTicks(tick, lastItemUseTick, settings.postUseDelayTicks))
            return reject(Rejection.RECENT_USE);
        if (settings.respectCooldown
            && mc.player.getAttackCooldownProgress(0.0f) < (preset == Settings.GuardPreset.STRICT ? 0.99f : 0.95f))
            return reject(Rejection.COOLDOWN);
        if (tick == lastRecordedAttackTick || (lastSentNs != Long.MIN_VALUE
            && nowNanos - lastSentNs < minimumIntervalNanos(preset, settings.maxCps)))
            return reject(Rejection.RATE_LIMIT);

        UUID uuid = target.getUuid();
        if (tick != lastTrackedTick) {
            if (uuid.equals(trackedTarget) && lastTrackedTick == tick - 1) trackedTicks++;
            else { trackedTarget = uuid; trackedTicks = 1; }
            lastTrackedTick = tick;
        }
        if (guardEnabled && preset == Settings.GuardPreset.STRICT && trackedTicks < 2)
            return reject(Rejection.TARGET_UNSTABLE);
        if (guardEnabled && preset == Settings.GuardPreset.STRICT && recentRotation > 45.0f)
            return reject(Rejection.AIM_SNAP);

        lastSentNs = nowNanos;
        lastRecordedAttackTick = tick;
        lastRejection = Rejection.NONE;
        attacksAllowed++;
        return true;
    }

    public void resetTransient() {
        trackedTarget = null;
        trackedTicks = 0;
        lastTrackedTick = -1;
        previousYawTick = -1;
        recentRotation = 0;
        observedWorld = null;
        lastPosition = null;
        lastPositionTick = -1;
        lastItemUseTick = -1;
        pauseUntilTick = -1;
    }
}