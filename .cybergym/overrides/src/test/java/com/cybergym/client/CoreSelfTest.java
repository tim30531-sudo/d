package com.cybergym.client;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Dependency-free unit tests run by Gradle's testCore. */
public final class CoreSelfTest {
    private static int assertions;
    private static void check(boolean pass, String explanation) {
        assertions++;
        if (!pass) throw new AssertionError(explanation);
    }
    public static void main(String[] args) throws Exception {
        ModuleManager modules = new ModuleManager();
        Settings settings = new Settings();
        for (Module m : Module.values())
            if (m.automatesCombat()) check(!modules.enabled(m), "Combat opt-in: " + m);
        check(!modules.enabled(Module.AUTO_LEAVE), "Auto-leave opt-in");
        check(!modules.enabled(Module.ANTI_AFK), "Anti-AFK opt-in");
        check(!modules.enabled(Module.AUTO_JUMP), "Auto jump opt-in");
        Properties corrupt = new Properties();
        corrupt.setProperty("maxCps", "987654321");
        corrupt.setProperty("aimDegreesPerTick", "NaN");
        corrupt.setProperty("overlayX", "-14");
        corrupt.setProperty("radarRadius", "40000");
        corrupt.setProperty("radarSize", "-1");
        corrupt.setProperty("entityBudget", "99999");
        corrupt.setProperty("lowHealthHearts", "garbage");
        corrupt.setProperty("aimCurve", "FAKE");
        corrupt.setProperty("guardPreset", "INVALID");
        corrupt.setProperty("module.AUTO_CLICKER", "garbage");
        settings.read(corrupt, modules);
        check(settings.maxCps == 12, "CPS upper bound");
        check(settings.aimDegreesPerTick == 2.0, "NaN fallback");
        check(settings.overlayX == 0, "HUD offset clamp");
        check(settings.radarRadius == 96, "Radar range clamped");
        check(settings.radarSize == 50, "Radar size clamped");
        check(settings.entityBudget == 64, "Radar entity budget clamped");
        check(settings.lowHealthHearts == 3, "Bad health threshold fallback");
        check(settings.aimCurve == Settings.AimCurve.LINEAR, "Bad aim mode fallback");
        check(settings.guardPreset == Settings.GuardPreset.STRICT, "Unknown guard preset falls back to strict");
        check(!modules.enabled(Module.AUTO_CLICKER), "Invalid state must not enable attacks");
        check(!TargetingRules.eligible(true,true,false,false,4,3), "Occluded target blocked");
        check(!TargetingRules.eligible(true,true,true,true,4,3), "Spectator blocked");
        check(!TargetingRules.eligible(false,true,false,true,4,3), "Self blocked");
        check(!TargetingRules.eligible(true,false,false,true,4,3), "Dead player blocked");
        check(!TargetingRules.eligible(true,true,false,true,16,3), "Out of range blocked");
        check(!TargetingRules.eligible(true,true,false,true,Double.NaN,3), "Nonfinite range blocked");
        check(TargetingRules.eligible(true,true,false,true,9,3), "Three block edge accepted");
        check(TargetingRules.wrappedAngle(355) == -5, "Angle wrap");
        check(TargetingRules.smoothStep(80, 2) == 2, "Linear smoothing bounded");
        for (Settings.AimCurve curve : Settings.AimCurve.values()) {
            double v = TargetingRules.smoothStep(20, 3, curve);
            check(v > 0 && v <= 3, "Curve output safe " + curve);
            check(TargetingRules.smoothStep(-20, 3, curve) == -v, "Curve mirrors angles " + curve);
            check(TargetingRules.smoothStep(Double.NaN, 2, curve) == 0, "Curve NaN input " + curve);
        }
        CpsTracker clicks = new CpsTracker();
        clicks.click(100); clicks.click(200); clicks.click(300);
        check(clicks.cps(400) == 3, "CPS");
        check(clicks.lastIntervalMs() == 100, "Click interval");
        check(clicks.cps(1300) == 0, "Click expiration");
        check(!ConfigStore.validName("../evil"), "Reject traversal");
        check(!ConfigStore.validName("abc/x"), "Reject subdirectory");
        check(ConfigStore.validName("practice-1"), "Valid profile");
        Path temp = Files.createTempDirectory("cybergym-test-");
        ConfigStore store = new ConfigStore(temp, modules, settings);
        settings.maxCps = 9; settings.aimCurve = Settings.AimCurve.BEZIER;
        settings.guardPreset = Settings.GuardPreset.BALANCED;
        settings.radarRadius = 72; modules.set(Module.PLAYER_RADAR, true);
        store.saveAs("practice-1");
        settings.maxCps = 2; settings.aimCurve = Settings.AimCurve.LINEAR;
        settings.guardPreset = Settings.GuardPreset.PRACTICE;
        modules.set(Module.PLAYER_RADAR, false);
        store.loadProfile("practice-1");
        check(settings.maxCps == 9 && modules.enabled(Module.PLAYER_RADAR), "Profile save/load");
        check(settings.aimCurve == Settings.AimCurve.BEZIER, "Curve persists");
        check(settings.guardPreset == Settings.GuardPreset.BALANCED, "Guard preset persists");
        check(settings.radarRadius == 72, "Radar radius persists");
        ConfigStore fresh = new ConfigStore(temp, modules, settings);
        fresh.load();
        check(fresh.activeName().equals("practice-1"), "Active profile persists");
        check(fresh.profiles().contains("practice-1"), "Profile list");
        check(modules.enabled(Module.ANTICHEAT_GUARD), "Guard defaults enabled");
        check(AntiCheatGuard.minimumIntervalNanos(Settings.GuardPreset.STRICT, 12) >= 170_000_000L,
            "Strict rate floor");
        check(AntiCheatGuard.minimumIntervalNanos(Settings.GuardPreset.BALANCED, 12) >= 120_000_000L,
            "Balanced rate floor");
        check(AntiCheatGuard.minimumIntervalNanos(Settings.GuardPreset.PRACTICE, 10) == 100_000_000L,
            "Practice user CPS interval");
        check(AntiCheatGuard.maximumAttackRange(Settings.GuardPreset.STRICT, 8.0) == 2.85,
            "Strict reach cap");
        check(AntiCheatGuard.maximumAttackRange(Settings.GuardPreset.PRACTICE, 8.0) == 3.0,
            "Vanilla max reach");
        check(AntiCheatGuard.maximumRotation(Settings.GuardPreset.STRICT, 6.0) == 1.8,
            "Strict aim rotation cap");
        check(AntiCheatGuard.maximumRotation(Settings.GuardPreset.BALANCED, 6.0) == 3.0,
            "Balanced aim rotation cap");
        check(AntiCheatGuard.maximumRotation(Settings.GuardPreset.PRACTICE, 6.0) == 5.0,
            "Practice aim rotation cap");
        check(AntiCheatGuard.maximumRotation(Settings.GuardPreset.STRICT, Double.NaN) == 0.0,
            "Reject invalid rotation");
        System.out.println("CyberGym v0.8 core self-test PASSED (" + assertions + " assertions)");
    }
}
