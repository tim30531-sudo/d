package de.tim30531.deathknightscreen.client;

import de.tim30531.deathknightscreen.DeathKnightScreenMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class SmoothDeathAnimationScreen extends Screen {
    private static final int FRAME_COUNT = 80;
    private static final int FRAME_WIDTH = 768;
    private static final int FRAME_HEIGHT = 432;
    private static final long DURATION_NANOS = 2_000_000_000L;
    private static final Identifier[] FRAMES = createFrameLocations();

    private final Screen vanillaDeathScreen;
    private final long startedAtNanos;

    public SmoothDeathAnimationScreen(Screen vanillaDeathScreen) {
        super(Component.literal("Death Knight"));
        this.vanillaDeathScreen = vanillaDeathScreen;
        this.startedAtNanos = System.nanoTime();
    }

    private static Identifier[] createFrameLocations() {
        Identifier[] frames = new Identifier[FRAME_COUNT];
        for (int i = 0; i < FRAME_COUNT; i++) {
            frames[i] = Identifier.fromNamespaceAndPath(
                    DeathKnightScreenMod.MODID,
                    String.format("textures/gui/frames/frame_%03d.png", i));
        }
        return frames;
    }

    @Override
    public void tick() {
        if (System.nanoTime() - startedAtNanos >= DURATION_NANOS && minecraft != null) {
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

        long elapsed = Math.max(0L, System.nanoTime() - startedAtNanos);
        double progress = Math.min(0.999999, elapsed / (double) DURATION_NANOS);
        int frame = Math.min(FRAME_COUNT - 1, (int) (progress * FRAME_COUNT));

        graphics.blitInscribed(FRAMES[frame], 0, 0, width, height,
                FRAME_WIDTH, FRAME_HEIGHT, false, true);
    }
}
