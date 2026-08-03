package de.tim30531.deathknightscreen.client;

import de.tim30531.deathknightscreen.DeathKnightScreenMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class KnightDeathAnimationScreen extends Screen {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            DeathKnightScreenMod.MOD_ID,
            "textures/gui/death_knight_realistic.png"
    );

    private static final int FRAME_WIDTH = 512;
    private static final int FRAME_HEIGHT = 288;
    private static final int COLUMNS = 8;
    private static final int FRAME_COUNT = 32;
    private static final int SHEET_WIDTH = FRAME_WIDTH * COLUMNS;
    private static final int SHEET_HEIGHT = FRAME_HEIGHT * 4;
    private static final int TICKS_PER_FRAME = 2;

    private final DeathScreen vanillaDeathScreen;
    private int ticks;

    public KnightDeathAnimationScreen(DeathScreen vanillaDeathScreen) {
        super(Component.literal("Death Knight"));
        this.vanillaDeathScreen = vanillaDeathScreen;
    }

    @Override
    public void tick() {
        ticks++;
        if (ticks >= FRAME_COUNT * TICKS_PER_FRAME && minecraft != null) {
            minecraft.setScreen(vanillaDeathScreen);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xFF000000);

        int frame = Math.min(FRAME_COUNT - 1, ticks / TICKS_PER_FRAME);
        int sourceX = (frame % COLUMNS) * FRAME_WIDTH;
        int sourceY = (frame / COLUMNS) * FRAME_HEIGHT;

        float screenRatio = (float) width / (float) height;
        float frameRatio = (float) FRAME_WIDTH / (float) FRAME_HEIGHT;
        int renderWidth;
        int renderHeight;

        if (screenRatio > frameRatio) {
            renderWidth = width;
            renderHeight = Math.round(width / frameRatio);
        } else {
            renderHeight = height;
            renderWidth = Math.round(height * frameRatio);
        }

        int renderX = (width - renderWidth) / 2;
        int renderY = (height - renderHeight) / 2;

        graphics.blit(
                TEXTURE,
                renderX,
                renderY,
                renderWidth,
                renderHeight,
                sourceX,
                sourceY,
                FRAME_WIDTH,
                FRAME_HEIGHT,
                SHEET_WIDTH,
                SHEET_HEIGHT
        );
    }
}
