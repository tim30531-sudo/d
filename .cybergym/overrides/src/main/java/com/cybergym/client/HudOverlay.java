package com.cybergym.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Batched client-state sampling: radar extraction every 200 ms, HUD rendering each frame. */
public final class HudOverlay {
    private static final int PANEL = 0xCA0B1221;
    private static final int TEXT = 0xFFE3F1FB;
    private static final int ACCENT = 0xFF39D6BA;
    private static final int PLAYER = 0xFF3FEFBE, MOB = 0xFFFFBA66, DANGER = 0xFFFF6778;
    private static boolean disabled, reported;
    private static long lastSampleMs;
    private static Object previousWorld;
    private record Contact(String name, boolean player, double dx, double dz,
                           double d2, float hp, boolean visible) {}
    private static List<Contact> contacts = List.of();
    private HudOverlay() {}
    public static void render(DrawContext context, RenderTickCounter counter) {
        try { draw(context); }
        catch (RuntimeException | LinkageError err) {
            if (!reported) {
                reported = true;
                System.err.println("[CyberGym] Disabling incompatible HUD for this session: " + err);
                err.printStackTrace();
            }
            disabled = true;
        }
    }
    private static void sample(MinecraftClient mc, Settings s) {
        long now = System.currentTimeMillis();
        if (mc.world == previousWorld && now >= lastSampleMs && now - lastSampleMs < 200) return;
        previousWorld = mc.world;
        lastSampleMs = now;
        List<Contact> scan = new ArrayList<>(s.entityBudget + 1);
        double range2 = (double)s.radarRadius * s.radarRadius;
        int inspected = 0;
        for (Entity ent : mc.world.getEntities()) {
            if (++inspected > 1200) break; // defensive budget, not a framerate guarantee
            if (ent == mc.player || !(ent instanceof LivingEntity living) || !living.isAlive()) continue;
            double d2 = ent.squaredDistanceTo(mc.player);
            if (d2 > range2 || d2 < 0.01) continue;
            String name = ent.getName().getString();
            if (name.length() > 19) name = name.substring(0, 16) + "...";
            scan.add(new Contact(name, ent instanceof PlayerEntity,
                ent.getX() - mc.player.getX(), ent.getZ() - mc.player.getZ(),
                d2, living.getHealth(), mc.player.canSee(ent)));
        }
        scan.sort(Comparator.comparingDouble(Contact::d2));
        contacts = List.copyOf(scan.subList(0, Math.min(scan.size(), s.entityBudget)));
    }
    private static void draw(DrawContext dc) {
        if (disabled) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.currentScreen != null) return;
        ModuleManager m = CyberGymClient.modules();
        Settings s = CyberGymClient.settings();
        int x = Math.min(s.overlayX, Math.max(0, dc.getScaledWindowWidth() - 240));
        int y = Math.min(s.overlayY, Math.max(0, dc.getScaledWindowHeight() - 125));
        if (m.enabled(Module.CPS_HUD)) {
            line(dc, mc, x, y, "CPS (sampled): " + CyberGymClient.clicks().cps(System.currentTimeMillis())); y += 16;
        }
        if (m.enabled(Module.CLICK_TRAINER)) {
            double accuracy = CyberGymClient.clicks().intervalAccuracy(s.trainerGoalCps);
            line(dc, mc, x, y, "Rhythm " + s.trainerGoalCps + " CPS: " +
                (accuracy < 0 ? "click to start" : String.format(Locale.ROOT, "%.0f%%", accuracy))); y += 16;
        }
        if (m.enabled(Module.MOVEMENT_HUD)) {
            double speed = mc.player.getVelocity().horizontalLength() * 20.0;
            line(dc, mc, x, y, String.format(Locale.ROOT, "Speed: %.1f b/s | %s", speed,
                mc.player.isSprinting() ? "sprint" : mc.player.isSneaking() ? "sneak" : "walk")); y += 16;
        }
        if (m.enabled(Module.LATENCY_HUD)) {
            String ping = "--";
            if (mc.getNetworkHandler() != null) {
                var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                if (entry != null) ping = Integer.toString(entry.getLatency());
            }
            line(dc, mc, x, y, "Server ping: " + ping + " ms"); y += 16;
        }
        if (m.enabled(Module.EQUIPMENT_HUD)) {
            String held = mc.player.getMainHandStack().getName().getString();
            if (held.length() > 27) held = held.substring(0, 24) + "...";
            line(dc, mc, x, y, "Held: " + held); y += 16;
        }
        PlayerEntity target = CyberGymClient.crosshairPlayer(mc);
        if (target != null) {
            if (m.enabled(Module.TARGET_HUD)) {
                line(dc, mc, x, y, target.getName().getString() + " | HP (client): " +
                    String.format(Locale.ROOT, "%.1f", target.getHealth() + target.getAbsorptionAmount())); y += 16;
            }
            if (m.enabled(Module.RANGE_HUD)) {
                line(dc, mc, x, y, String.format(Locale.ROOT, "Target range: %.2f blocks",
                    mc.player.distanceTo(target))); y += 16;
            }
        }
        if (m.enabled(Module.LOW_HEALTH_HUD) &&
            mc.player.getHealth() <= s.lowHealthHearts * 2f) {
            dc.drawTextWithShadow(mc.textRenderer, "LOW HEALTH: " +
                String.format(Locale.ROOT, "%.1f", mc.player.getHealth()) + " HP",
                Math.max(4, dc.getScaledWindowWidth()/2 - 56),
                Math.max(20, dc.getScaledWindowHeight()/2 - 40), DANGER);
        }
        if (m.enabled(Module.ARRAY_LIST)) {
            int right = dc.getScaledWindowWidth() - 7, ry = Math.max(6, s.overlayY);
            for (Module module : Module.values()) {
                if (!m.enabled(module)) continue;
                int tw = mc.textRenderer.getWidth(module.title);
                dc.fill(right - tw - 10, ry - 3, right + 3, ry + 11, PANEL);
                dc.drawTextWithShadow(mc.textRenderer, module.title, right - tw - 4, ry, TEXT);
                ry += 15;
            }
        }
        if (m.enabled(Module.PLAYER_RADAR) || m.enabled(Module.MOB_RADAR)
            || m.enabled(Module.DIRECTION_TRACERS) || m.enabled(Module.NAME_TAGS_HUD)) {
            sample(mc, s);
            drawContacts(dc, mc, m, s);
        }
        m.render(dc);
    }
    private static void drawContacts(DrawContext dc, MinecraftClient mc, ModuleManager m, Settings s) {
        double radians = Math.toRadians(mc.player.getYaw());
        double sine = Math.sin(radians), cosine = Math.cos(radians);
        boolean players = m.enabled(Module.PLAYER_RADAR), mobs = m.enabled(Module.MOB_RADAR);
        if (players || mobs) {
            int size = Math.min(s.radarSize, dc.getScaledWindowHeight() / 3);
            int left = Math.max(4, dc.getScaledWindowWidth() - size - 10), top = 14;
            int cx = left + size/2, cy = top + size/2;
            dc.fill(left - 2, top - 2, left + size + 2, top + size + 2, PANEL);
            dc.drawHorizontalLine(left, left + size, cy, 0x99FFFFFF);
            dc.drawVerticalLine(cx, top, top + size, 0x99FFFFFF);
            dc.fill(cx - 2, cy - 2, cx + 2, cy + 2, TEXT);
            for (Contact c : contacts) {
                if (c.player && !players || !c.player && !mobs) continue;
                double forward = -c.dx * sine + c.dz * cosine;
                double right = c.dx * cosine + c.dz * sine;
                int mx = cx + (int)Math.round(right / s.radarRadius * (size/2 - 4));
                int my = cy - (int)Math.round(forward / s.radarRadius * (size/2 - 4));
                dc.fill(mx - 2, my - 2, mx + 2, my + 2, c.player ? PLAYER : MOB);
            }
            dc.drawTextWithShadow(mc.textRenderer, "RADAR " + s.radarRadius + "m", left, top + size + 5, ACCENT);
        }
        if (m.enabled(Module.NAME_TAGS_HUD)) {
            int x = 6, y = Math.max(s.overlayY + 135, 150), shown = 0;
            for (Contact c : contacts) {
                if (shown >= 7 || y + 16 > dc.getScaledWindowHeight()) break;
                line(dc, mc, x, y, (c.player ? "P " : "M ") + c.name + " " +
                    String.format(Locale.ROOT, "%.0fm  %.1fHP", Math.sqrt(c.d2), c.hp));
                y += 16; shown++;
            }
        }
        if (m.enabled(Module.DIRECTION_TRACERS)) {
            int drawn = 0, cx = dc.getScaledWindowWidth() / 2;
            int sy = dc.getScaledWindowHeight() - 35;
            for (Contact c : contacts) {
                if (!c.visible || drawn >= 12) continue;
                double forward = -c.dx * sine + c.dz * cosine;
                if (forward <= 0) continue; // cull entities behind camera
                double right = c.dx * cosine + c.dz * sine;
                int tx = (int)Math.round(cx + right / s.radarRadius * dc.getScaledWindowWidth() * 0.40);
                int ty = (int)Math.round(sy - Math.min(forward / s.radarRadius, 1.0) * sy * 0.65);
                tx = Math.max(4, Math.min(dc.getScaledWindowWidth() - 4, tx));
                drawLine(dc, cx, sy, tx, ty, c.player ? PLAYER : MOB);
                drawn++;
            }
        }
    }
    private static void drawLine(DrawContext dc, int x1, int y1, int x2, int y2, int color) {
        int count = Math.min(64, Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1)));
        if (count == 0) return;
        for (int i = 0; i <= count; i++) {
            int x = x1 + (int)Math.round((x2 - x1) * (double)i / count);
            int y = y1 + (int)Math.round((y2 - y1) * (double)i / count);
            dc.fill(x, y, x + 1, y + 1, color);
        }
    }
    private static void line(DrawContext dc, MinecraftClient mc, int x, int y, String value) {
        int width = mc.textRenderer.getWidth(value);
        dc.fill(x - 3, y - 3, x + width + 7, y + 12, PANEL);
        dc.fill(x - 3, y - 3, x - 1, y + 12, ACCENT);
        dc.drawTextWithShadow(mc.textRenderer, value, x + 2, y, TEXT);
    }
}