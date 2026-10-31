package com.cybergym.client;

public final class TargetingRules {
    private TargetingRules() { }
    public static boolean eligible(boolean otherPlayer, boolean alive, boolean spectator,
                                   boolean visible, double squaredDistance, double maxRange) {
        return otherPlayer && alive && !spectator && visible
            && Double.isFinite(squaredDistance) && squaredDistance >= 0
            && Double.isFinite(maxRange) && maxRange > 0 && maxRange <= 3.0
            && squaredDistance <= maxRange * maxRange;
    }
    public static double wrappedAngle(double degrees) {
        if (!Double.isFinite(degrees)) return 0.0;
        double wrapped = (degrees + 180.0) % 360.0;
        if (wrapped < 0) wrapped += 360.0;
        return wrapped - 180.0;
    }
    public static double smoothStep(double deltaDegrees, double maxPerTick) {
        return smoothStep(deltaDegrees, maxPerTick, Settings.AimCurve.LINEAR);
    }
    /** Simple deterministic smoothing curves, independent of proprietary client implementations. */
    public static double smoothStep(double deltaDegrees, double maxPerTick, Settings.AimCurve curve) {
        if (!Double.isFinite(maxPerTick) || maxPerTick <= 0 || !Double.isFinite(deltaDegrees)) return 0.0;
        double delta = wrappedAngle(deltaDegrees);
        double magnitude = Math.min(Math.abs(delta), maxPerTick);
        if (curve == Settings.AimCurve.QUADRATIC) {
            double t = Math.min(1.0, Math.abs(delta) / 18.0);
            magnitude *= (0.20 + 0.80 * t * t);
        } else if (curve == Settings.AimCurve.BEZIER) {
            double t = Math.min(1.0, Math.abs(delta) / 18.0);
            magnitude *= (0.20 + 0.80 * (t * t * (3.0 - 2.0 * t)));
        }
        return Math.copySign(Math.min(magnitude, Math.abs(delta)), delta);
    }
}