package com.direwolf20.buildinggadgets.common.network.C2S;

import com.direwolf20.buildinggadgets.common.items.AbstractGadget;
import com.direwolf20.buildinggadgets.common.items.GadgetExchanger;
import com.direwolf20.buildinggadgets.common.network.PacketHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

public record PacketUndo() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PacketUndo> TYPE =
            new CustomPacketPayload.Type<>(PacketHandler.PacketUndo);

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketUndo> CODEC = StreamCodec.of(
            (buf, packet) -> {}, // Write nothing
            buf -> new PacketUndo() // Read nothing, return new instance
    );

    public static void send() {
        ClientPlayNetworking.send(new PacketUndo());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketUndo payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ItemStack stack = AbstractGadget.getGadget(context.player());

            if (!(stack.isEmpty() || stack.getItem() instanceof GadgetExchanger)) {
                ((AbstractGadget) stack.getItem()).undo(context.player().level(), context.player(), stack);
            }
        });
    }
}