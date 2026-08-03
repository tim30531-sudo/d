package de.tim30531.deathknightscreen.client;

import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.chat.Component;

public final class KnightDeathAnimationScreen extends Screen {
    private final DeathScreen vanillaDeathScreen;
    private int ticks;

    public KnightDeathAnimationScreen(DeathScreen vanillaDeathScreen) {
        super(Component.literal("Death Knight"));
        this.vanillaDeathScreen = vanillaDeathScreen;
    }

    @Override
    public void tick() {
        ticks++;
        if (ticks >= 82 && minecraft != null) {
            minecraft.setScreen(vanillaDeathScreen);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xFF000000);
        float t = ticks + partialTick;
        int cx = width / 2;
        int cy = height / 2 - 8;

        drawHeart(g, cx - 58, cy, t);
        drawKnight(g, cx + 58, cy + 6, t);

        if (t > 55) {
            int alpha = Math.min(255, (int)((t - 55) * 12));
            int color = (alpha << 24) | 0xB9D9FF;
            g.drawCenteredString(font, Component.literal("DEINE SEELE IST GEBROCHEN"), cx, cy + 72, color);
        }
    }

    private void drawHeart(GuiGraphics g, int x, int y, float t) {
        float shake = t > 44 && t < 60 ? (float)Math.sin(t * 2.8f) * 3f : 0f;
        int sx = x + Math.round(shake);
        int sy = y;
        if (t < 48) {
            heartShape(g, sx, sy, 0xFF1677FF, 0xFF7EC8FF);
        } else {
            int split = Math.min(24, (int)((t - 48) * 1.7f));
            heartLeft(g, sx - split, sy + split / 3);
            heartRight(g, sx + split, sy + split / 2);
            g.fill(sx - 2, sy - 27, sx + 2, sy + 24, 0xFFE8F7FF);
        }
    }

    private void heartShape(GuiGraphics g, int x, int y, int blue, int light) {
        g.fill(x - 28, y - 22, x - 4, y + 8, blue);
        g.fill(x + 4, y - 22, x + 28, y + 8, blue);
        g.fill(x - 36, y - 14, x + 36, y + 8, blue);
        g.fill(x - 28, y + 8, x + 28, y + 22, blue);
        g.fill(x - 18, y + 22, x + 18, y + 34, blue);
        g.fill(x - 8, y + 34, x + 8, y + 43, blue);
        g.fill(x - 22, y - 17, x - 8, y - 9, light);
    }

    private void heartLeft(GuiGraphics g, int x, int y) {
        g.fill(x - 34, y - 20, x - 3, y + 8, 0xFF1266D8);
        g.fill(x - 26, y + 8, x - 3, y + 22, 0xFF1266D8);
        g.fill(x - 16, y + 22, x - 3, y + 34, 0xFF1266D8);
    }

    private void heartRight(GuiGraphics g, int x, int y) {
        g.fill(x + 3, y - 20, x + 34, y + 8, 0xFF1677FF);
        g.fill(x + 3, y + 8, x + 26, y + 22, 0xFF1677FF);
        g.fill(x + 3, y + 22, x + 16, y + 34, 0xFF1677FF);
    }

    private void drawKnight(GuiGraphics g, int targetX, int y, float t) {
        int startX = width + 70;
        float enter = Math.min(1f, t / 28f);
        int x = (int)(startX + (targetX - startX) * (1f - (float)Math.pow(1f - enter, 3)));
        g.fill(x - 18, y - 45, x + 18, y - 9, 0xFF111722);
        g.fill(x - 23, y - 38, x + 23, y - 18, 0xFF202938);
        g.fill(x - 13, y - 34, x + 13, y - 29, 0xFF000000);
        g.fill(x - 8, y - 33, x - 3, y - 30, 0xFF2B8CFF);
        g.fill(x + 3, y - 33, x + 8, y - 30, 0xFF2B8CFF);
        g.fill(x - 20, y - 9, x + 20, y + 39, 0xFF151B26);
        g.fill(x - 28, y + 32, x + 28, y + 45, 0xFF090C12);

        float swing = 0f;
        if (t > 28 && t < 58) swing = Math.min(1f, (t - 28) / 24f);
        float angle = 45f - swing * 115f;
        g.pose().pushPose();
        g.pose().translate(x - 18, y - 3, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(angle));
        g.fill(-3, -7, 3, 58, 0xFFDCE7F2);
        g.fill(-7, 48, 7, 54, 0xFF6D7885);
        g.fill(-2, 54, 2, 68, 0xFF3C2418);
        g.pose().popPose();
    }
}
