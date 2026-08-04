package de.tim30531.deathknightscreen;

import de.tim30531.deathknightscreen.client.ClientHooks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(DeathKnightScreenMod.MODID)
public final class DeathKnightScreenMod {
    public static final String MODID = "deathknightscreen";

    public DeathKnightScreenMod(FMLJavaModLoadingContext context) {
        FMLCommonSetupEvent.getBus(context.getModBusGroup()).addListener(this::commonSetup);
        LivingDeathEvent.BUS.addListener(this::onLivingDeath);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientHooks.register();
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        ModNetwork.register();
    }

    private void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) {
            return;
        }

        var killer = victim.getKillCredit();
        if (killer instanceof Player && killer != victim) {
            ModNetwork.sendAnimation(victim);
        }
    }
}
