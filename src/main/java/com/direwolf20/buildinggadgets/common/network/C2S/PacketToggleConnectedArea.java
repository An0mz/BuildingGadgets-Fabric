package com.direwolf20.buildinggadgets.common.network.C2S;

import com.direwolf20.buildinggadgets.common.items.AbstractGadget;
import com.direwolf20.buildinggadgets.common.items.GadgetBuilding;
import com.direwolf20.buildinggadgets.common.items.GadgetDestruction;
import com.direwolf20.buildinggadgets.common.items.GadgetExchanger;
import com.direwolf20.buildinggadgets.common.network.PacketHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

public record PacketToggleConnectedArea() implements CustomPacketPayload {

    public static final Type<PacketToggleConnectedArea> TYPE =
            new Type<>(PacketHandler.PacketToggleConnectedArea);

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketToggleConnectedArea> CODEC = StreamCodec.of(
            (buf, packet) -> {}, // Write nothing
            buf -> new PacketToggleConnectedArea() // Read nothing, return new instance
    );

    public static void send() {
        ClientPlayNetworking.send(new PacketToggleConnectedArea());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketToggleConnectedArea payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ItemStack stack = AbstractGadget.getGadget(context.player());
            if (stack.getItem() instanceof GadgetExchanger || stack.getItem() instanceof GadgetBuilding || stack.getItem() instanceof GadgetDestruction) {
                AbstractGadget.toggleConnectedArea(context.player(), stack);
            }
        });
    }
}