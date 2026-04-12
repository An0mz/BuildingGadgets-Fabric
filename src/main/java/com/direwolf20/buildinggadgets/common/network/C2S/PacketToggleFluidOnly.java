package com.direwolf20.buildinggadgets.common.network.C2S;

import com.direwolf20.buildinggadgets.common.items.AbstractGadget;
import com.direwolf20.buildinggadgets.common.items.GadgetDestruction;
import com.direwolf20.buildinggadgets.common.network.PacketHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

public record PacketToggleFluidOnly() implements CustomPacketPayload {

    public static final Type<PacketToggleFluidOnly> TYPE =
            new Type<>(PacketHandler.PacketToggleFluidOnly);

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketToggleFluidOnly> CODEC = StreamCodec.of(
            (buf, packet) -> {},
            buf -> new PacketToggleFluidOnly()
    );

    public static void send() {
        ClientPlayNetworking.send(new PacketToggleFluidOnly());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketToggleFluidOnly payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ItemStack stack = AbstractGadget.getGadget(context.player());
            if (stack.getItem() instanceof GadgetDestruction) {
                GadgetDestruction.toggleFluidMode(stack);
            }
        });
    }
}