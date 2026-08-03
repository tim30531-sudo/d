package de.tim30531.deathknightscreen.client;

import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class KnightDeathAnimationScreen extends Screen {
    private final DeathScreen vanillaDeathScreen;
    private int ticks;

    public KnightDeathAnimationScreen(DeathScreen vanillaDeathScreen) {
        super(Component.literal("Death Knight"));
        this.vanillaDeathScreen = vanillaDeathScreen;
    }

    @Override public void tick() {
        ticks++;
        if (ticks >= 48 && minecraft != null) minecraft.setScreen(vanillaDeathScreen);
    }

    @Override public boolean shouldCloseOnEsc() { return false; }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        float t = ticks + partialTick;
        g.fill(0, 0, width, height, 0xFF000000);
        int cx = width / 2;
        int cy = height / 2;

        drawSmoke(g, cx, cy, t);
        drawHeart(g, cx - 48, cy - 2, t);
        drawKnight(g, cx + 46, cy + 7, t);
        drawSlash(g, cx, cy, t);

        if (t > 38) {
            int a = Math.min(255, (int)((t - 38) * 32));
            g.fill(0, 0, width, height, a << 24);
        }
    }

    private void drawSmoke(GuiGraphics g, int x, int y, float t) {
        if (t < 17) return;
        for (int i = 0; i < 9; i++) {
            float p = Math.min(1f, (t - 17 - i * 0.7f) / 15f);
            if (p <= 0) continue;
            double a = i * 0.72 + p * 1.8;
            int r = 18 + i * 5 + (int)(p * 65);
            int px = x + (int)(Math.cos(a) * r);
            int py = y + (int)(Math.sin(a) * r * 0.55) - (int)(p * 24);
            int alpha = (int)((1f - p) * 130);
            int size = 10 + (int)(p * 18);
            g.fill(px - size, py - size, px + size, py + size, (alpha << 24) | 0x0B7FA9);
        }
    }

    private void drawHeart(GuiGraphics g, int x, int y, float t) {
        float pulse = 1f + 0.05f * (float)Math.sin(t * 0.7f);
        int s = (int)(28 * pulse);
        int glow = t < 20 ? 0x901BEAFF : 0x501BEAFF;
        g.fill(x - s - 8, y - s, x + s + 8, y + s + 18, glow);

        if (t < 25) {
            g.fill(x - 22, y - 18, x - 2, y + 7, 0xFF12CFFF);
            g.fill(x + 2, y - 18, x + 22, y + 7, 0xFF12CFFF);
            g.fill(x - 29, y - 10, x + 29, y + 8, 0xFF12CFFF);
            g.fill(x - 22, y + 8, x + 22, y + 20, 0xFF0BAEE8);
            g.fill(x - 12, y + 20, x + 12, y + 31, 0xFF0789C9);
            g.fill(x - 4, y + 31, x + 4, y + 38, 0xFF066BA5);
            g.fill(x - 17, y - 15, x - 8, y - 9, 0xFFE9FCFF);
        } else {
            int d = Math.min(44, (int)((t - 25) * 4));
            g.fill(x - 31 - d, y - 17 + d / 3, x - 4 - d, y + 18 + d / 2, 0xFF0CAFE8);
            g.fill(x + 4 + d, y - 17 + d / 4, x + 31 + d, y + 18 + d / 2, 0xFF12CFFF);
            g.fill(x - 2, y - 28, x + 2, y + 31, 0xFFE9FCFF);
        }
    }

    private void drawKnight(GuiGraphics g, int targetX, int y, float t) {
        float enter = Math.min(1f, t / 12f);
        int x = (int)(width + 90 + (targetX - width - 90) * (1f - Math.pow(1f - enter, 3)));

        g.fill(x - 21, y - 53, x + 21, y - 15, 0xFF0B1019);
        g.fill(x - 28, y - 45, x + 28, y - 22, 0xFF151E2B);
        g.fill(x - 15, y - 38, x + 15, y - 31, 0xFF000000);
        g.fill(x - 9, y - 36, x - 3, y - 32, 0xFF39D9FF);
        g.fill(x + 3, y - 36, x + 9, y - 32, 0xFF39D9FF);
        g.fill(x - 22, y - 15, x + 22, y + 42, 0xFF111722);
        g.fill(x - 34, y + 31, x + 34, y + 49, 0xFF070A0F);

        float swing = t < 13 ? 0f : Math.min(1f, (t - 13) / 12f);
        float angle = 55f - swing * 132f;
        g.pose().pushPose();
        g.pose().translate(x - 20, y - 3, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(angle));
        g.fill(-5, -10, 5, 65, 0xFFEAFBFF);
        g.fill(-2, -8, 2, 64, 0xFF68E4FF);
        g.fill(-10, 53, 10, 59, 0xFF6E7C8B);
        g.fill(-3, 59, 3, 76, 0xFF3B2417);
        g.pose().popPose();
    }

    private void drawSlash(GuiGraphics g, int x, int y, float t) {
        if (t < 20 || t > 34) return;
        float p = (t - 20) / 14f;
        int alpha = (int)((1f - p) * 230);
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(-42));
        int len = 75 + (int)(p * 110);
        int w = 12 + (int)((1f - p) * 18);
        g.fill(-len, -w, len, w, (alpha << 24) | 0xBFF7FF);
        g.fill(-len, -3, len, 3, (Math.min(255, alpha + 20) << 24) | 0xFFFFFF);
        g.pose().popPose();
        if (t < 24) {
            int flash = (int)((24 - t) * 50);
            g.fill(0, 0, width, height, (Math.min(220, flash) << 24) | 0xD7FAFF);
        }
    }
}
