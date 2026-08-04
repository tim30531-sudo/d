package de.tim30531.deathknightscreen.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;

public final class ClientHooks {
    private static final long PENDING_TIMEOUT_NANOS = 5_000_000_000L;
    private static long pendingUntilNanos;

    private ClientHooks() {
    }

    public static void register() {
        ScreenEvent.Opening.BUS.addListener(ClientHooks::onScreenOpening);
        TickEvent.ClientTickEvent.Post.BUS.addListener(ClientHooks::onClientTick);
    }

    public static void triggerAnimation() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            Screen current = minecraft.screen;
            if (current instanceof SmoothDeathAnimationScreen) {
                pendingUntilNanos = 0L;
                return;
            }

            if (current instanceof DeathScreen deathScreen) {
                pendingUntilNanos = 0L;
                minecraft.setScreen(new SmoothDeathAnimationScreen(deathScreen));
                return;
            }

            pendingUntilNanos = System.nanoTime() + PENDING_TIMEOUT_NANOS;
        });
    }

    private static void onScreenOpening(ScreenEvent.Opening event) {
        if (!(event.getScreen() instanceof DeathScreen deathScreen)) {
            return;
        }

        long now = System.nanoTime();
        if (pendingUntilNanos == 0L || now > pendingUntilNanos) {
            pendingUntilNanos = 0L;
            return;
        }

        pendingUntilNanos = 0L;
        event.setNewScreen(new SmoothDeathAnimationScreen(deathScreen));
    }

    private static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        SmoothDeathAnimationScreen.preloadFrames(Minecraft.getInstance(), 2);
        if (pendingUntilNanos != 0L && System.nanoTime() > pendingUntilNanos) {
            pendingUntilNanos = 0L;
        }
    }
}
