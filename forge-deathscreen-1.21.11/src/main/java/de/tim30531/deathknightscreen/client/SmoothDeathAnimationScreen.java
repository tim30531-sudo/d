package de.tim30531.deathknightscreen.client;

import de.tim30531.deathknightscreen.DeathKnightScreenMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;

public final class SmoothDeathAnimationScreen extends Screen {
    private static final int FRAME_COUNT = 120;
    private static final int FRAME_WIDTH = 640;
    private static final int FRAME_HEIGHT = 360;
    private static final long DURATION_NANOS = 2_000_000_000L;
    private static final long SWEEP_CUE_NANOS = 960_000_000L;
    private static final long IMPACT_CUE_NANOS = 1_120_000_000L;
    private static final long SHATTER_CUE_NANOS = 1_280_000_000L;
    private static final Identifier[] FRAMES = createFrameLocations();

    private static int preloadIndex;

    private final Screen vanillaDeathScreen;
    private long startedAtNanos;
    private boolean sweepPlayed;
    private boolean impactPlayed;
    private boolean shatterPlayed;
    private boolean finished;

    public SmoothDeathAnimationScreen(Screen vanillaDeathScreen) {
        super(Component.literal("Death Knight"));
        this.vanillaDeathScreen = vanillaDeathScreen;
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

    static void preloadFrames(Minecraft minecraft, int budget) {
        if (minecraft == null || budget <= 0) {
            return;
        }

        for (int i = 0; i < budget && preloadIndex < FRAME_COUNT; i++) {
            minecraft.getTextureManager().getTexture(FRAMES[preloadIndex]);
            preloadIndex++;
        }
    }

    @Override
    protected void init() {
        super.init();
        if (startedAtNanos == 0L) {
            startedAtNanos = System.nanoTime();
        }
        preloadFrames(minecraft, FRAME_COUNT - preloadIndex);
    }

    @Override
    public void tick() {
        long elapsed = elapsedNanos();
        playSoundCues(elapsed);
        if (elapsed >= DURATION_NANOS) {
            finishAnimation();
        }
    }

    private long elapsedNanos() {
        if (startedAtNanos == 0L) {
            startedAtNanos = System.nanoTime();
        }
        return Math.max(0L, System.nanoTime() - startedAtNanos);
    }

    private void playSoundCues(long elapsed) {
        if (minecraft == null) {
            return;
        }

        if (!sweepPlayed && elapsed >= SWEEP_CUE_NANOS) {
            sweepPlayed = true;
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(
                    SoundEvents.PLAYER_ATTACK_SWEEP, 0.82F));
        }
        if (!impactPlayed && elapsed >= IMPACT_CUE_NANOS) {
            impactPlayed = true;
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(
                    SoundEvents.TOTEM_USE, 1.28F));
        }
        if (!shatterPlayed && elapsed >= SHATTER_CUE_NANOS) {
            shatterPlayed = true;
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(
                    SoundEvents.AMETHYST_BLOCK_BREAK, 0.72F));
        }
    }

    private void finishAnimation() {
        if (finished || minecraft == null) {
            return;
        }
        finished = true;
        minecraft.setScreen(vanillaDeathScreen);
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

        long elapsed = elapsedNanos();
        double progress = Math.min(0.999999, elapsed / (double) DURATION_NANOS);
        int frame = Math.min(FRAME_COUNT - 1, (int) (progress * FRAME_COUNT));

        graphics.blitInscribed(FRAMES[frame], 0, 0, width, height,
                FRAME_WIDTH, FRAME_HEIGHT, false, true);
    }
}
