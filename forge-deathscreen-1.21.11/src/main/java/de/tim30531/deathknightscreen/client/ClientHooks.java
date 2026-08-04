package de.tim30531.deathknightscreen.client;

import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraftforge.client.event.ScreenEvent;

public final class ClientHooks {
    private static boolean pendingPlayerKillAnimation;

    private ClientHooks() {
    }

    public static void register() {
        ScreenEvent.Opening.BUS.addListener(ClientHooks::onScreenOpening);
    }

    public static void armAnimation() {
        pendingPlayerKillAnimation = true;
    }

    private static void onScreenOpening(ScreenEvent.Opening event) {
        if (!pendingPlayerKillAnimation || !(event.getScreen() instanceof DeathScreen deathScreen)) {
            return;
        }

        pendingPlayerKillAnimation = false;
        event.setNewScreen(new SmoothDeathAnimationScreen(deathScreen));
    }
}
