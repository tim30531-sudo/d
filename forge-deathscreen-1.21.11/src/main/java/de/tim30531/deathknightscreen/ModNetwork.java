package de.tim30531.deathknightscreen;

import de.tim30531.deathknightscreen.client.ClientHooks;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

public final class ModNetwork {
    private static final int PROTOCOL_VERSION = 2;
    private static final Identifier CHANNEL_NAME = Identifier.fromNamespaceAndPath(
            DeathKnightScreenMod.MODID, "main_channel");

    private static final SimpleChannel CHANNEL = ChannelBuilder
            .named(CHANNEL_NAME)
            .clientAcceptedVersions(Channel.VersionTest.exact(PROTOCOL_VERSION))
            .serverAcceptedVersions(Channel.VersionTest.exact(PROTOCOL_VERSION))
            .networkProtocolVersion(PROTOCOL_VERSION)
            .simpleChannel()
                .play()
                    .clientbound()
                        .addMain(TriggerAnimationPacket.class,
                                TriggerAnimationPacket.STREAM_CODEC,
                                TriggerAnimationPacket::onMessage)
            .build();

    private ModNetwork() {
    }

    public static void register() {
        CHANNEL.getName();
    }

    public static void sendAnimation(ServerPlayer player) {
        CHANNEL.send(TriggerAnimationPacket.INSTANCE, PacketDistributor.PLAYER.with(player));
    }

    public record TriggerAnimationPacket() {
        public static final TriggerAnimationPacket INSTANCE = new TriggerAnimationPacket();

        public static final StreamCodec<RegistryFriendlyByteBuf, TriggerAnimationPacket> STREAM_CODEC =
                StreamCodec.ofMember(TriggerAnimationPacket::encode, TriggerAnimationPacket::decode);

        private void encode(RegistryFriendlyByteBuf buffer) {
        }

        private static TriggerAnimationPacket decode(RegistryFriendlyByteBuf buffer) {
            return INSTANCE;
        }

        private static void onMessage(TriggerAnimationPacket message, CustomPayloadEvent.Context context) {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientHooks.triggerAnimation();
            }
        }
    }
}
