package com.direwolf20.buildinggadgets.common.network.C2S;

import com.direwolf20.buildinggadgets.common.items.AbstractGadget;
import com.direwolf20.buildinggadgets.common.network.PacketHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

public record PacketToggleRayTraceFluid() implements CustomPacketPayload {

    public static final Type<PacketToggleRayTraceFluid> TYPE =
            new Type<>(PacketHandler.PacketToggleRayTraceFluid);

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketToggleRayTraceFluid> CODEC = StreamCodec.of(
            (buf, packet) -> {}, // Write nothing
            buf -> new PacketToggleRayTraceFluid() // Read nothing, return new instance
    );

    public static void send() {
        ClientPlayNetworking.send(new PacketToggleRayTraceFluid());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketToggleRayTraceFluid payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ItemStack stack = AbstractGadget.getGadget(context.player());
            if (!stack.isEmpty()) {
                AbstractGadget.toggleRayTraceFluid(context.player(), stack);
            }
        });
    }
}