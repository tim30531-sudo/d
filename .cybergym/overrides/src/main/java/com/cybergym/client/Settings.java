package com.cybergym.client;

import java.util.Properties;

/** Local, validated settings. No networking, hidden telemetry or spoofing. */
public final class Settings {
    public enum AimCurve { LINEAR, QUADRATIC, BEZIER }
    public int maxCps = 7;
    public double aimDegreesPerTick = 2.0;
    public int aimFovDegrees = 18;
    public double maxTargetRange = 3.0;
    public boolean respectCooldown = true;
    public int trainerGoalCps = 7;
    public int overlayX = 8;
    public int overlayY = 8;
    public AimCurve aimCurve = AimCurve.LINEAR;
    public int radarRadius = 40;
    public int radarSize = 90;
    public int lowHealthHearts = 3;
    public int entityBudget = 32;

    public void reset() {
        maxCps = 7; aimDegreesPerTick = 2.0; aimFovDegrees = 18;
        maxTargetRange = 3.0; respectCooldown = true; trainerGoalCps = 7;
        overlayX = 8; overlayY = 8; aimCurve = AimCurve.LINEAR;
        radarRadius = 40; radarSize = 90; lowHealthHearts = 3; entityBudget = 32;
    }
    public void normalize() {
        maxCps = clamp(maxCps, 1, 12);
        aimDegreesPerTick = clamp(aimDegreesPerTick, 0.2, 5.0, 2.0);
        aimFovDegrees = clamp(aimFovDegrees, 2, 40);
        maxTargetRange = clamp(maxTargetRange, 1.0, 3.0, 3.0);
        trainerGoalCps = clamp(trainerGoalCps, 1, 12);
        overlayX = clamp(overlayX, 0, 150);
        overlayY = clamp(overlayY, 0, 150);
        radarRadius = clamp(radarRadius, 8, 96);
        radarSize = clamp(radarSize, 50, 140);
        lowHealthHearts = clamp(lowHealthHearts, 1, 10);
        entityBudget = clamp(entityBudget, 4, 64);
        if (aimCurve == null) aimCurve = AimCurve.LINEAR;
    }
    public Properties toProperties(ModuleManager modules) {
        normalize();
        Properties out = new Properties();
        for (Module m : Module.values()) out.setProperty("module." + m.name(), Boolean.toString(modules.enabled(m)));
        out.setProperty("maxCps", Integer.toString(maxCps));
        out.setProperty("aimDegreesPerTick", Double.toString(aimDegreesPerTick));
        out.setProperty("aimFovDegrees", Integer.toString(aimFovDegrees));
        out.setProperty("maxTargetRange", Double.toString(maxTargetRange));
        out.setProperty("respectCooldown", Boolean.toString(respectCooldown));
        out.setProperty("trainerGoalCps", Integer.toString(trainerGoalCps));
        out.setProperty("overlayX", Integer.toString(overlayX));
        out.setProperty("overlayY", Integer.toString(overlayY));
        out.setProperty("aimCurve", aimCurve.name());
        out.setProperty("radarRadius", Integer.toString(radarRadius));
        out.setProperty("radarSize", Integer.toString(radarSize));
        out.setProperty("lowHealthHearts", Integer.toString(lowHealthHearts));
        out.setProperty("entityBudget", Integer.toString(entityBudget));
        return out;
    }
    public void read(Properties values, ModuleManager modules) {
        reset(); modules.reset();
        for (Module m : Module.values()) {
            String value = values.getProperty("module." + m.name());
            if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value))
                modules.set(m, Boolean.parseBoolean(value));
        }
        maxCps = integer(values, "maxCps", maxCps);
        aimDegreesPerTick = decimal(values, "aimDegreesPerTick", aimDegreesPerTick);
        aimFovDegrees = integer(values, "aimFovDegrees", aimFovDegrees);
        maxTargetRange = decimal(values, "maxTargetRange", maxTargetRange);
        respectCooldown = booleanValue(values, "respectCooldown", respectCooldown);
        trainerGoalCps = integer(values, "trainerGoalCps", trainerGoalCps);
        overlayX = integer(values, "overlayX", overlayX);
        overlayY = integer(values, "overlayY", overlayY);
        radarRadius = integer(values, "radarRadius", radarRadius);
        radarSize = integer(values, "radarSize", radarSize);
        lowHealthHearts = integer(values, "lowHealthHearts", lowHealthHearts);
        entityBudget = integer(values, "entityBudget", entityBudget);
        try { aimCurve = AimCurve.valueOf(values.getProperty("aimCurve", "LINEAR")); }
        catch (IllegalArgumentException ex) { aimCurve = AimCurve.LINEAR; }
        normalize();
    }
    private static int integer(Properties props, String key, int fallback) {
        try { return Integer.parseInt(props.getProperty(key, "")); }
        catch (NumberFormatException ex) { return fallback; }
    }
    private static double decimal(Properties props, String key, double fallback) {
        try { return Double.parseDouble(props.getProperty(key, "")); }
        catch (NumberFormatException ex) { return fallback; }
    }
    private static boolean booleanValue(Properties props, String key, boolean fallback) {
        String s = props.getProperty(key);
        return "true".equalsIgnoreCase(s) ? true : "false".equalsIgnoreCase(s) ? false : fallback;
    }
    private static int clamp(int n, int min, int max) { return Math.max(min, Math.min(max, n)); }
    private static double clamp(double n, double min, double max, double fallback) {
        return Double.isFinite(n) ? Math.max(min, Math.min(max, n)) : fallback;
    }
}