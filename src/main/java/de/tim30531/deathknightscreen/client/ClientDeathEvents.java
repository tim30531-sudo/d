package de.tim30531.deathknightscreen.client;

import de.tim30531.deathknightscreen.DeathKnightScreenMod;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DeathKnightScreenMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientDeathEvents {
    private ClientDeathEvents() {}

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (event.getNewScreen() instanceof DeathScreen deathScreen
                && !(event.getCurrentScreen() instanceof KnightDeathAnimationScreen)) {
            event.setNewScreen(new KnightDeathAnimationScreen(deathScreen));
        }
    }
}
