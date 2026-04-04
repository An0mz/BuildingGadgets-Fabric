package com.direwolf20.buildinggadgets.common.network.C2S;

import com.direwolf20.buildinggadgets.common.items.AbstractGadget;
import com.direwolf20.buildinggadgets.common.items.GadgetBuilding;
import com.direwolf20.buildinggadgets.common.network.PacketHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

public record PacketToggleBlockPlacement() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PacketToggleBlockPlacement> TYPE =
            new CustomPacketPayload.Type<>(PacketHandler.PacketToggleBlockPlacement);

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketToggleBlockPlacement> CODEC = StreamCodec.of(
            (buf, packet) -> {},
            buf -> new PacketToggleBlockPlacement()
    );

    public static void send() {
        ClientPlayNetworking.send(new PacketToggleBlockPlacement());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketToggleBlockPlacement payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ItemStack stack = AbstractGadget.getGadget(context.player());
            if (stack.getItem() instanceof GadgetBuilding) {
                GadgetBuilding.togglePlaceAtop(context.player(), stack);
            }
        });
    }
}