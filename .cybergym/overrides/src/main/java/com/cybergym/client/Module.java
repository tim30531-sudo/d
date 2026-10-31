package com.cybergym.client;

/**
 * Feature catalog. Each toggle corresponds to an implementation in CyberGymClient
 * or HudOverlay. Automated combat/movement actions are opt-in.
 */
public enum Module {
    AIM_ASSIST("Aim smoothing", "Combat", false, "Visible targets only; hold physical attack"),
    KILL_AURA("Practice aura", "Combat", false, "Turn toward visible nearby players; attack only on raycast hit"),
    TRIGGER_BOT("Trigger attack", "Combat", false, "Attack valid crosshair player at configured CPS"),
    AUTO_CLICKER("Hold-to-click", "Combat", false, "Attack crosshair player while holding attack"),
    AUTO_SPRINT("Auto sprint", "Movement", false, "Sprint when forward and able"),
    AUTO_JUMP("Auto jump", "Movement", false, "Jump while holding forward on ground"),
    SAFE_WALK("Edge sneak", "Movement", false, "Sneak near an unprotected ledge"),
    ANTI_AFK("Idle head movement", "Movement", false, "Turn head slightly every 15s when idle"),
    QUICK_SWORD("Quick sword [V]", "Equipment", true, "Select sword from hotbar"),
    QUICK_BOW("Quick bow [B]", "Equipment", true, "Select bow or crossbow"),
    QUICK_TOTEM("Quick totem [G]", "Equipment", false, "Select available totem in hotbar"),
    QUICK_GAPPLE("Quick apple [H]", "Equipment", false, "Select golden apple in hotbar"),
    QUICK_PICKAXE("Quick pickaxe [J]", "Equipment", false, "Select pickaxe in hotbar"),
    AUTO_TOOL("Auto tool", "Equipment", false, "Select fastest mining tool while mining a block"),
    TARGET_HUD("Target info", "HUD", true, "Player name and client-side health"),
    RANGE_HUD("Range indicator", "HUD", true, "Crosshair target distance"),
    CPS_HUD("CPS counter", "HUD", true, "Sampled physical CPS"),
    CLICK_TRAINER("Click rhythm", "HUD", false, "Click rhythm accuracy"),
    MOVEMENT_HUD("Movement", "HUD", true, "Local speed and movement state"),
    LATENCY_HUD("Server ping", "HUD", true, "Player-list latency"),
    EQUIPMENT_HUD("Equipment", "HUD", true, "Current hand item"),
    ARRAY_LIST("Enabled features", "HUD", false, "List enabled toggles"),
    PLAYER_RADAR("Player radar", "Visual", false, "Local HUD radar of nearby players"),
    MOB_RADAR("Mob radar", "Visual", false, "Local HUD radar of nearby living mobs"),
    DIRECTION_TRACERS("Direction tracers", "Visual", false, "2D screen-space directional lines to visible targets"),
    NAME_TAGS_HUD("Nearby name tags", "Visual", false, "Nearby entities and available health in HUD"),
    LOW_HEALTH_HUD("Low health alert", "Utility", true, "Visual warning when below configured hearts"),
    AUTO_LEAVE("Low health disconnect", "Utility", false, "Disconnect when health <= selected threshold");

    public final String title;
    public final String category;
    public final boolean initiallyEnabled;
    public final String description;
    Module(String title, String category, boolean initiallyEnabled, String description) {
        this.title = title; this.category = category;
        this.initiallyEnabled = initiallyEnabled; this.description = description;
    }
    public boolean automatesCombat() {
        return this == AIM_ASSIST || this == KILL_AURA || this == TRIGGER_BOT || this == AUTO_CLICKER;
    }
}