package com.direwolf20.buildinggadgets.common.network.bidirection;

import com.direwolf20.buildinggadgets.client.BuildingGadgetsClient;
import com.direwolf20.buildinggadgets.common.component.BGComponent;
import com.direwolf20.buildinggadgets.common.network.PacketHandler;
import com.direwolf20.buildinggadgets.common.network.Target;
import com.direwolf20.buildinggadgets.common.tainted.template.TemplateKey;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public record PacketRequestTemplate(UUID id) implements CustomPacketPayload {

    public static final Type<PacketRequestTemplate> TYPE =
            new Type<>(PacketHandler.PacketRequestTemplate);

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketRequestTemplate> CODEC = StreamCodec.of(
            (buf, packet) -> buf.writeUUID(packet.id),
            buf -> new PacketRequestTemplate(buf.readUUID())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void sendToTarget(Target target, UUID id) {
        if (target.flow() == PacketFlow.CLIENTBOUND) {
            Server.sendToClient(target.player(), id);
        } else {
            Client.send(id);
        }
    }

    @Environment(EnvType.CLIENT)
    public static class Client {
        @Environment(EnvType.CLIENT)
        public static void send(UUID id) {
            ClientPlayNetworking.send(new PacketRequestTemplate(id));
        }

        @Environment(EnvType.CLIENT)
        public static void handle(PacketRequestTemplate payload, ClientPlayNetworking.Context context) {
            context.client().execute(() -> {
                BuildingGadgetsClient.CACHE_TEMPLATE_PROVIDER.requestRemoteUpdate(
                        new TemplateKey(payload.id),
                        context.client().level
                );
            });
        }
    }

    public static class Server {
        public static void sendToClient(ServerPlayer player, UUID id) {
            ServerPlayNetworking.send(player, new PacketRequestTemplate(id));
        }

        public static void handle(PacketRequestTemplate payload, ServerPlayNetworking.Context context) {
            context.server().execute(() -> {
                BGComponent.TEMPLATE_PROVIDER_COMPONENT.maybeGet(context.player().level()).ifPresent(provider -> {
                    provider.requestRemoteUpdate(new TemplateKey(payload.id), context.player().level());
                });
            });
        }
    }
}
